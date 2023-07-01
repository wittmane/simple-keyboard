/*
 * Copyright (C) 2023 Eli Wittman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin;

import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.testLog;

import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

//TODO: (EW) support a limit on how much text is kept in the cache
//TODO: (EW) does this need to handle multiple threads? there was an old comment saying all access
// to mCommittedTextBeforeComposingText should be done on the main thread instead of calling
// #toString() on it. it probably would be reasonable to synchronize things to make it thread-safe
// since it's all in this class. we're already calling subSequence (which seems to return a string),
// so that may be just as good as the original code (other than the comment saying to only call on
// the main thread).
//TODO: (EW) remove the debugging logging (or at least hide it behind a constant and use a debug log
// level)
public class EditorState {
    private static final String TAG = EditorState.class.getSimpleName();

    /** Value to set for an absolute selection/composition position to indicate that it doesn't
     *  actually contain the position that it is meant to hold. */
    public static final int UNKNOWN_POSITION = -1;
    /** Value to set for length variables to indicate that it doesn't actually contain the length
     *  that it is meant to hold. */
    public static final int UNKNOWN_LENGTH = -1;
    /** Placeholder character to use when a character isn't cached. This character shouldn't ever be
     *  returned in any public methods.
     *  This is using one of the characters defined by unicode as being a noncharacter, which is
     *  reserved for internal (private) use, so it shouldn't conflict with any text actually in the
     *  editor. */
    public static final char NONCHARACTER_CODEPOINT_PLACEHOLDER = '\uFFFF';

    /** Output flag for {@link #compareTextInCache} indicating some portion of the reference text
     * didn't match what was in the cache. */
    public static final int TEXT_DIFFERENT = 0;
    /** Output flag for {@link #compareTextInCache} indicating that none of the reference text was
     *  cached so nothing could actually be compared. */
    public static final int TEXT_UNKNOWN = 1;
    /** Output flag for {@link #compareTextInCache} indicating that all of the text in the cache
     *  matched the reference text, but not all of the text from the reference was actually
     *  cached. */
    public static final int TEXT_KNOWN_MATCHED = 2;
    /** Output flag for {@link #compareTextInCache} indicating that all of the reference text was
     *  cached and they completely matched. */
    public static final int TEXT_MATCHED = 3;

    /** The cached text. Note that this may contain {@link #NONCHARACTER_CODEPOINT_PLACEHOLDER} in
     *  gaps between cached text. These placeholder characters should only be used internally and
     *  not returned in any public function. */
    private final RelativeCache mTextCache;

    /** The cursor start absolute position. This will be {@link #UNKNOWN_POSITION} when the absolute
     *  position isn't known. */
    private int mSelectionStart = UNKNOWN_POSITION;
    /** The length of the selection. The length is tracked rather than the selection end for
     *  relative tracking in case we enter text (can know there is no selection) but don't yet know
     *  where the cursor is. This will be {@link #UNKNOWN_LENGTH} when the length is unknown. */
    private int mSelectionLength = UNKNOWN_LENGTH;

    // there shouldn't ever be a composition when entering a text field, so no composition is the
    // default state
    /** The composition start position relative to the selection start. This will be null when the
     *  relative position isn't known. This should be reset to null when the composition ends to
     *  avoid indicating that there is a zero length composition. */
    private Integer mCompositionRelativeStartIndex = null;
    //TODO: (EW) there is at least one case where we could know that there is a composition but not
    // know the length of the composition (replace text partially in the composition with an unknown
    // replacement length), and currently we're just falling into a completely unknown state. this
    // one probably isn't valuable enough to try to keep the state, but if there are others,
    // redesigning this might be relevant.
    /** The length of the composition. The length is tracked rather than the composition end for
     *  relative tracking in case we lose the relative position of the composition but can still
     *  know the length of the composition. This will be {@link #UNKNOWN_LENGTH} when the length is
     *  unknown, which also indicates that we don't know whether there even is a composition. */
    private int mCompositionLength = 0;

    public EditorState() {
        mTextCache = new RelativeCache();
    }

    public EditorState(EditorState copy) {
        mTextCache = new RelativeCache(copy.mTextCache);
        mSelectionStart = copy.mSelectionStart;
        mSelectionLength = copy.mSelectionLength;
        mCompositionRelativeStartIndex = copy.mCompositionRelativeStartIndex;
        mCompositionLength = copy.mCompositionLength;
    }

    //#region debug
    public String getDebugStateInternal() {
        return "mSelectionStart: " + mSelectionStart
                + ", mSelectionLength: " + mSelectionLength
                + ", mCompositionRelativeStartIndex=" + mCompositionRelativeStartIndex
                + ", mCompositionLength=" + mCompositionLength
                + ", " + mTextCache.getDebugStateInternal();
    }

    public String getDebugState() {
        StringBuilder sb = new StringBuilder();
        if (areSelectionAbsolutePositionsKnown()) {
            sb.append("sel: ")
                    .append(getSelectionStart())
                    .append(" - ");
            if (isSelectionLengthKnown()) {
                sb.append(getSelectionEnd());
            } else {
                sb.append("?");
            }
        } else {
            if (isSelectionLengthKnown()) {
                sb.append("selLength: ").append(mSelectionLength);
            } else {
                sb.append("sel: unknown");
            }
        }
        sb.append(", ");
        if (isCompositionUnknown()) {
            sb.append("compos: unknown");
        } else if (mCompositionRelativeStartIndex == null) {
            sb.append("composLength: ")
                    .append(mCompositionLength);
        } else if (isAbsoluteSelectionStartKnown()) {
            sb.append("compos: ")
                    .append(mCompositionRelativeStartIndex + mSelectionStart)
                    .append(" - ")
                    .append(mCompositionRelativeStartIndex + mCompositionLength + mSelectionStart);
        } else {
            sb.append("relCompos: ")
                    .append(mCompositionRelativeStartIndex)
                    .append(" - ")
                    .append(mCompositionRelativeStartIndex + mCompositionLength);
        }
        sb.append(", beforeSelection: \"").append(getTextBeforeCursor()).append("\"");
        if (isSelectionLengthKnown()) {
            sb.append(", inSelection: \"").append(getSelectedText()).append("\"");
            sb.append(", afterSelection: \"").append(getTextAfterCursor()).append("\"");
        }
        return sb.toString();
    }

    public void printDebugState() {
        testLog(TAG, "printDebugState: " + getDebugStateInternal());
        int spaceBefore;
        if (isAbsoluteSelectionStartKnown()) {
            spaceBefore = mSelectionStart - mTextCache.mIndex;
        } else if (!isCompositionUnknown() && hasComposition()
                && isCompositionRelativePositionKnown() && mCompositionRelativeStartIndex < 0) {
            spaceBefore = Math.max(0, -mCompositionRelativeStartIndex - mTextCache.mIndex);
        } else {
            spaceBefore = Math.max(0, -mTextCache.mIndex);
        }
        int spaceAfter = 0;
        if (isSelectionLengthKnown()) {
            if (mSelectionLength + mTextCache.mIndex > mTextCache.mStringBuilder.length()) {
                spaceAfter = mSelectionLength + mTextCache.mIndex - mTextCache.mStringBuilder.length();
            }
        } else {
            if (mTextCache.mIndex > mTextCache.mStringBuilder.length()) {
                spaceAfter = mTextCache.mIndex - mTextCache.mStringBuilder.length();
            }
        }
        if (!isCompositionUnknown() && hasComposition() && isCompositionRelativePositionKnown()) {
            spaceAfter = Math.max(spaceAfter,
                    mCompositionRelativeStartIndex + mTextCache.mIndex
                            - mTextCache.mStringBuilder.length());
        }
        String indent = repeatChar(' ', 2);
        String state = indent + "\"" + repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER, spaceBefore)
                + mTextCache.mStringBuilder
                + repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER, spaceAfter) + "\"";
        if (isCompositionUnknown()) {
            state += "\n" + indent + " " + repeatChar('?',
                    spaceBefore + mTextCache.mStringBuilder.length() + spaceAfter);
        } else if (hasComposition()) {
            if (isCompositionRelativePositionKnown()) {
                state += "\n" + indent + " " + repeatChar(' ',
                        spaceBefore + mTextCache.mIndex + mCompositionRelativeStartIndex)
                        + repeatChar('_', mCompositionLength);
            } else {
                state += "\n" + indent + "?" + repeatChar('_', mCompositionLength) + "?";
            }
        }
        state += "\n" + indent + repeatChar(' ',
                spaceBefore + mTextCache.mIndex) + ">";
        if (isSelectionLengthKnown()) {
            state += repeatChar(' ', mSelectionLength) + "<";
        }
        testLog(TAG, "state: \n" + state);
    }
    //#endregion

    //#region basic data accessors
    /**
     * Get the absolution position of the selection start.
     * @return The absolution position of the selection start or {@link #UNKNOWN_POSITION} if it
     *         isn't known.
     */
    public int getSelectionStart() {
        return mSelectionStart;
    }

    /**
     * Get the absolution position of the selection end.
     * @return The absolution position of the selection end or {@link #UNKNOWN_POSITION} if it isn't
     *         known.
     */
    public int getSelectionEnd() {
        if (!isAbsoluteSelectionStartKnown() || !isSelectionLengthKnown()) {
            return UNKNOWN_POSITION;
        }
        return mSelectionStart + mSelectionLength;
    }

    /**
     * Get the length of the selection.
     * @return The length of the selection or {@link #UNKNOWN_LENGTH} if it isn't known.
     */
    public int getSelectionLength() {
        return mSelectionLength;
    }

    /**
     * Check if the absolute position of the selection start is known.
     * @return Whether the absolute position of the selection start is known.
     */
    public boolean isAbsoluteSelectionStartKnown() {
        return mSelectionStart != UNKNOWN_POSITION;
    }

    /**
     * Check if the absolute position of the selection (start and end) is known.
     * @return Whether the absolute position of the selection (start and end) is known.
     */
    public boolean areSelectionAbsolutePositionsKnown() {
        return isAbsoluteSelectionStartKnown() && isSelectionLengthKnown();
    }

    /**
     * Check if the length of the selection is known (which determines whether we have a relative
     * selection end).
     * @return Whether the length of the selection is known.
     */
    public boolean isSelectionLengthKnown() {
        return mSelectionLength != UNKNOWN_LENGTH;
    }

    /**
     * Check if the tracked absolute positions match a specified selection.
     * @param start The selection start to compare.
     * @param end The selection end to compare.
     * @param onlyValidateKnown Whether to only validate the absolute positions that are known.
     *                          Since there are cases that the start and/or end absolute positions
     *                          aren't known, if this is true, this will only verify known absolute
     *                          positions. This means that returning true doesn't necessarily mean
     *                          that the absolution selection position is tracked to be matching,
     *                          but simply that it isn't known to be different. If this is set to
     *                          false, returning true doesn't necessarily mean that the tracked
     *                          absolute selection is different since it also could just be unknown.
     * @return Whether the tracked absolute positions are the same.
     */
    public boolean selectionMatches(final int start, final int end,
                                    final boolean onlyValidateKnown) {
        if (onlyValidateKnown && (start < 0 || end < 0 || !isAbsoluteSelectionStartKnown())) {
            // nothing to verify
            return true;
        }
        // it's possible for the text field to have the selection start and end positions flipped,
        // but methods like getTextBeforeCursor still work as if the positions were normal, so we
        // will just compare these as if they were in the normal order
        final int selectionStart = Math.min(start, end);
        final int selectionEnd = Math.max(start, end);

        if (getSelectionStart() != selectionStart) {
            return false;
        }
        if ((isSelectionLengthKnown() || !onlyValidateKnown) && getSelectionEnd() != selectionEnd) {
            return false;
        }
        return true;
    }

    /**
     * Get the absolution position of the composition start.
     * @return The absolution position of the composition start or {@link #UNKNOWN_POSITION}
     *         if it isn't known, there is no composition, or the state of the composition isn't
     *         known.
     */
    public int getCompositionStart() {
        if (!isAbsoluteSelectionStartKnown() || isCompositionUnknown() || !hasComposition()
                || mCompositionRelativeStartIndex == null) {
            return UNKNOWN_POSITION;
        }
        return mSelectionStart + mCompositionRelativeStartIndex;
    }

    /**
     * Get the absolution position of the composition end.
     * @return The absolution position of the composition end or {@link #UNKNOWN_POSITION} if it
     *         isn't known, there is no composition, or the state of the composition isn't known.
     */
    public int getCompositionEnd() {
        if (!isAbsoluteSelectionStartKnown() || isCompositionUnknown() || !hasComposition()
                || mCompositionRelativeStartIndex == null) {
            return UNKNOWN_POSITION;
        }
        return mSelectionStart + mCompositionRelativeStartIndex + mCompositionLength;
    }

    /**
     * Check if the absolute position of the composition (start and end) is known.
     * @return Whether the absolute position of the composition (start and end) is known.
     */
    public boolean areCompositionAbsolutePositionsKnown() {
        return !isCompositionUnknown() && hasComposition() && isAbsoluteSelectionStartKnown();
    }

    /**
     * Check if there is no information about whether there even is a composition.
     * @return Whether there is no information about the composition state.
     */
    public boolean isCompositionUnknown() {
        return mCompositionLength == UNKNOWN_LENGTH;
    }

    /**
     * Check if there currently is a composing region. Note that {@link #isCompositionUnknown}
     * should be checked first to make sure the state is even known.
     * @return Whether there currently is a composing region.
     */
    public boolean hasComposition() {
        if (isCompositionUnknown()) {
            Log.e(TAG, "Checking if there is a composition when the composition state is unknown");
            throw new RuntimeException("Checking if there is a composition when the composition state is unknown");
        }
        //TODO: (EW) it might not be necessary to bother checking the start index here
        return mCompositionLength > 0 || mCompositionRelativeStartIndex != null;
    }

    /**
     * Check if the composition relative start/end position is known.
     * @return Whether the composition relative positions are known.
     */
    public boolean isCompositionRelativePositionKnown() {
        // if we have a relative start position, we should have a length, so that's not necessary to
        // check
        return mCompositionRelativeStartIndex != null;
    }

    /**
     * Get the position of the composition start from the selection start.
     * @return The relative position of the composition start.
     */
    public int getRelativeCompositionStart() {
        if (isCompositionUnknown() || mCompositionRelativeStartIndex == null) {
            Log.e(TAG, "Requesting the composition start position when the composition state is unknown");
            throw new RuntimeException("Requesting the composition start position when the composition state is unknown");
        }
        return mCompositionRelativeStartIndex;
    }

    /**
     * Get the position of the composition end from the selection start.
     * @return The relative position of the composition end.
     */
    public int getRelativeCompositionEnd() {
        if (isCompositionUnknown() || mCompositionRelativeStartIndex == null) {
            Log.e(TAG, "Requesting the composition end position when the composition state is unknown");
            throw new RuntimeException("Requesting the composition end position when the composition state is unknown");
        }
        return mCompositionRelativeStartIndex + mCompositionLength;
    }

    /**
     * Get the length of the composition.
     * @return The length of the composition or {@link #UNKNOWN_LENGTH} if it isn't known.
     */
    public int getCompositionLength() {
        return mCompositionLength;
    }

    /**
     * Check if the relative composition start and end positions are known.
     * @return Whether the relative composition position is known.
     */
    public boolean isRelativeCompositionPositionKnown() {
        return mCompositionRelativeStartIndex != null;
    }

    /**
     * Check if the cache contains any text immediately before the selection start.
     * @return Whether any text immediately before the selection is cached.
     */
    public boolean hasCachedTextRightBeforeCursor() {
        //TODO: maybe validate that if there is only 1 character, it isn't half of a surrogate pair
        return mTextCache.charAt(-1) != NONCHARACTER_CODEPOINT_PLACEHOLDER;
    }

    /**
     * Get the cached text immediately before the selection. Note that if there any gaps in the
     * cached text, only the text up to the first gap will be returned.
     * @return The cached text before the cursor or null if there is no cached text.
     */
    public CharSequence getTextBeforeCursor() {
        int startIndex = 0;
        while (mTextCache.charAt(startIndex - 1) != NONCHARACTER_CODEPOINT_PLACEHOLDER) {
            startIndex--;
        }
        return mTextCache.subSequence(startIndex, 0, true);
    }

    /**
     * Get the text in the selection.
     * @return The text in the selection if the whole text is in the cache or null if it isn't in
     *         the cache or the length of the selection isn't known.
     */
    public CharSequence getSelectedText() {
        if (!isSelectionLengthKnown()) {
            return null;
        }
        return mTextCache.subSequence(0, mSelectionLength, true);
    }

    /**
     * Get the cached text immediately after the selection. Note that if there any gaps in the
     * cached text, only the text up to the first gap will be returned.
     * @return The cached text after the cursor or an empty string if there is no cached text.
     */
    public CharSequence getTextAfterCursor() {
        int endIndex = mSelectionLength;
        while (mTextCache.charAt(endIndex) != NONCHARACTER_CODEPOINT_PLACEHOLDER) {
            endIndex++;
        }
        return mTextCache.subSequence(mSelectionLength, endIndex, true);
    }

    /**
     * Check if the whole composing text is in the cache.
     * @return Whether the whole composing text is in the cache.
     */
    public boolean isFullCompositionCached() {
        if (isCompositionUnknown() || mCompositionRelativeStartIndex == null) {
            // technically we might have it fully cached, but we don't know where the composition is
            // to be certain (if there even is a composition)
            return false;
        }
        if (!hasComposition()) {
            // we have the full composition in the sense that there is no composition, so we're not
            // missing anything from in cache for it
            return true;
        }
        return mTextCache.isRangeCached(mCompositionRelativeStartIndex,
                mCompositionRelativeStartIndex + mCompositionLength);
    }

    /**
     * Get the cached text in the composition.
     * @return The composition or null if there is no composition or it isn't fully cached.
     */
    public CharSequence getComposedText() {
        if (isCompositionUnknown()) {
            Log.e(TAG, "Requesting the composition when its state is unknown");
            throw new RuntimeException("Requesting the composition when its state is unknown");
        }
        if (!hasComposition()) {
            return null;
        }
        if (mCompositionRelativeStartIndex == null) {
            Log.e(TAG, "Requesting the composition when its position is unknown");
            throw new RuntimeException("Requesting the composition when its position is unknown");
        }
        return mTextCache.subSequence(mCompositionRelativeStartIndex,
                mCompositionRelativeStartIndex + mCompositionLength, true);
    }

    /**
     * Check if some text matches what is in the cache.
     * @param text The text to compare to the cache.
     * @param offsetFromCursorStart The relative position from the cursor start where the text is
     *                              expected in the cache.
     * @return {@link #TEXT_DIFFERENT}, {@link #TEXT_UNKNOWN}, {@link #TEXT_KNOWN_MATCHED}, or
     *         {@link #TEXT_MATCHED} depending on how much of the text is in the cache and
     *         whether what was cached actually matches.
     */
    public int compareTextInCache(final CharSequence text,
                                  final int offsetFromCursorStart) {
        if (TextUtils.isEmpty(text)) {
            return TEXT_MATCHED;
        }
        int matchedChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char cachedChar = mTextCache.charAt(offsetFromCursorStart + i);
            if (cachedChar != NONCHARACTER_CODEPOINT_PLACEHOLDER) {
                if (cachedChar != text.charAt(i)) {
                    return TEXT_DIFFERENT;
                } else {
                    matchedChars++;
                }
            }
        }
        if (matchedChars == 0) {
            return TEXT_UNKNOWN;
        }
        if (matchedChars < text.length()) {
            return TEXT_KNOWN_MATCHED;
        }
        return TEXT_MATCHED;
    }

    /**
     * Get the maximum relative position from the selection start that is known to exist.
     * @return The maximum relative position from the selection start that is known to exist.
     */
    public int getMaxKnownRelativePosition() {
        int maxKnownPositionFromCursorStart = Math.max(0, mTextCache.maxKnownPosition());
        if (mSelectionLength > maxKnownPositionFromCursorStart) {
            maxKnownPositionFromCursorStart = mSelectionLength;
        }
        if (!isCompositionUnknown() && hasComposition() && mCompositionRelativeStartIndex != null
                && mCompositionRelativeStartIndex + mCompositionLength > maxKnownPositionFromCursorStart) {
            maxKnownPositionFromCursorStart = mCompositionRelativeStartIndex + mCompositionLength;
        }
        return maxKnownPositionFromCursorStart;
    }

    /**
     * Get the number of characters in the cache.
     * @return The number of characters in the cache.
     */
    public int getCachedCharCount() {
        return mTextCache.charCount();
    }
    //#endregion

    //#region actions (public modifiers)
    /**
     * Reset internal tracking state for everything.
     */
    public void reset() {
        mTextCache.clear();
        invalidateSelection(true, true);
        // setting the state to have no composition because the composition is finished when leaving
        // a text field or switching IMEs, and in the case of the app thinking we're not connected
        // (probably a framework bug), that probably would similarly end the composition
        finishComposingText();
    }

    /**
     * Set the absolute position for the selection. Note that this will clear the cached text if the
     * current selection position is unknown since the position of that text isn't known.
     * @param start The new absolute start position for the selection.
     * @param end The new absolute end position for the selection.
     */
    public void setSelection(int start, int end) {
        if (start > end) {
            // it's possible for the text field to track the selection start and end positions
            // flipped, so we need to handle that, but methods like getTextBeforeCursor still work
            // as if the positions were in the normal order, so we should just track these in the
            // normal position. we could keep track of some isSelectionFlipped flag, but currently
            // there doesn't seem to be any value to do so.
            int temp = start;
            start = end;
            end = temp;
        }
        if (start < 0) {
            throw new IllegalArgumentException(
                    "setSelection called with an invalid start position: " + start);
        }
        if (isAbsoluteSelectionStartKnown()) {
            shiftSelection(start - mSelectionStart);
        } else {
            // we don't know where the cursor position is relative to the composing text, so we
            // don't know where the existing cached text is relative to the new position, so the
            // only safe option is to clear whatever cache we have
            testLog(TAG, "setSelection: invalidateTextCache unknown cursor position");
            invalidateTextCache();
            if (!isCompositionUnknown() && hasComposition()) {
                // we still know there is a composition, but we don't know where it is
                invalidateComposition(false);
            }
            mSelectionStart = start;
        }
        mSelectionLength = end - start;
    }

    /**
     * Shift the position of the selection.
     * @param cursorStartShift The number of characters to shift the start position of the
     *                         selection.
     * @param cursorEndShift The number of characters to shift the end position of the selection
     */
    public void shiftSelection(int cursorStartShift, int cursorEndShift) {
        shiftSelection(cursorStartShift);
        if (isSelectionLengthKnown()) {
            mSelectionLength += cursorEndShift - cursorStartShift;
            if (mSelectionLength < 0) {
                Log.e(TAG, "Invalid new selection length");
                // not sure how to handle this, so just revert to an unknown state
                invalidateTextCache();
                invalidateSelection(true, true);
                invalidateComposition(false);
                //TODO: (EW) probably remove - keeping for now to catch any of these issues before
                // release
                throw new IllegalArgumentException("Invalid new selection length");
            }
        }
    }

    /**
     * Shift the position of the selection.
     * @param shift The number of characters to shift both the start and end of the selection.
     */
    public void shiftSelection(int shift) {
        mTextCache.shift(shift);
        if (isAbsoluteSelectionStartKnown()) {
            if (mSelectionStart + shift < 0) {
                Log.e(TAG, "Shifting selection past 0");
                shift = -mSelectionStart;
                //TODO: (EW) probably remove - keeping for now to catch any of these issues before
                // release
                throw new IllegalArgumentException("Shifting selection past 0");
            }
            mSelectionStart += shift;
        }
        if (!isCompositionUnknown() && hasComposition()
                && mCompositionRelativeStartIndex != null) {
            mCompositionRelativeStartIndex -= shift;
        }
    }

    /**
     * Set the length of the selection.
     * @param length The new length for the selection.
     */
    public void setSelectionLength(int length) {
        if (length < 0) {
            Log.e(TAG, "Length is less than 0: " + length);
            length = UNKNOWN_LENGTH;
            //TODO: (EW) probably remove - keeping for now to catch any of these issues before
            // release
            throw new IllegalArgumentException("Length is less than 0");
        }
        mSelectionLength = length;
    }

    /**
     * Set the absolute position for the composition.
     * @param start The new absolute start position for the composition.
     * @param end The new absolute end position for the composition.
     */
    public void setComposingRegion(int start, int end) {
        //TODO: (EW) should we handle out of order positions the same as setting the selection?
        int composingStart = Math.min(start, end);
        int composingEnd = Math.max(start, end);
        if (composingStart < composingEnd && isAbsoluteSelectionStartKnown()) {
            mCompositionRelativeStartIndex = composingStart - mSelectionStart;
        } else {
            mCompositionRelativeStartIndex = null;
        }
        mCompositionLength = composingEnd - composingStart;
    }

    /**
     * Remove the composing region.
     */
    public void finishComposingText() {
        mCompositionRelativeStartIndex = null;
        mCompositionLength = 0;
    }

    /**
     * Set text as composing and set the cursor position. If there is an existing composition, that
     * text will be replaced with the new text. If there is no composition, the new text will
     * replace the current selection.
     * @param text The new composing text.
     * @param newCursorPosition The new cursor position around the text. If > 0, this is relative to
     *                          the end of the text - 1; if <= 0, this is relative to the start of
     *                          the text. This means a value of 1 will always advance you to the
     *                          position after the full text being inserted. Note that this means
     *                          you can't position the cursor within the text.
     */
    public void setComposingText(CharSequence text, int newCursorPosition) {
        testLog(TAG, "setComposingText: text=\"" + text + "\", newCursorPosition=" + newCursorPosition);
        int replacementLength;
        // temporarily internally move the cursor to the beginning of where the new text is going to
        // be placed to make sure the replacement doesn't mess with relative positions and for
        // consistency between cases. it's going to move relative to that position regardless, so
        // this just simplifies things. also determine how much text needs to be replaced.
        if (isCompositionUnknown()) {
            // we don't know if there is a composition, so we don't know where this text will be
            // placed
            testLog(TAG, "setComposingText: invalidateTextCache - unknown composition state");
            invalidateTextCache();
            invalidateSelection(true, true);
            replacementLength = 0;
        } else if (hasComposition()) {
            if (mCompositionRelativeStartIndex != null) {
                shiftSelection(mCompositionRelativeStartIndex);
                replacementLength = mCompositionLength;
            } else {
                // we don't know where the current composition is, so we don't know how to shift the
                // cached text
                testLog(TAG, "setComposingText: invalidateTextCache - unknown composition position");
                invalidateTextCache();
                invalidateSelection(true, true);
                replacementLength = 0;
            }
        } else {
            if (isSelectionLengthKnown()) {
                replacementLength = mSelectionLength;
            } else {
                // we don't know if there is a cursor selection, so we have to clear the cache after
                // the cursor start since we can't determine what text after the cursor start might
                // be getting removed
                testLog(TAG, "setComposingTextInternal: invalidateTextCacheAfterRelative 0 - unknown selection length");
                invalidateTextCacheAfterRelative(-1);
                replacementLength = 0;
            }
        }
        mTextCache.replace(0, replacementLength, text);
        // get the new position to move the cursor relative to start position of where the text is
        // getting entered
        int newCursorPositionFromTextStart = newCursorPosition > 0
                ? text.length() + newCursorPosition - 1
                : newCursorPosition;
        // move the cursor to where it was actually requested to go
        shiftSelection(newCursorPositionFromTextStart);
        mSelectionLength = 0;
        mCompositionRelativeStartIndex = -newCursorPositionFromTextStart;
        mCompositionLength = text.length();

        //TODO: Documentation indicates that an empty string can be passed and tells editor
        // authors that it should be handled normally, replacing the current composing text
        // with an empty string. This could be interpreted as the composing region should
        // still be retained as part of treating it "normally", but the default TextView
        // updates the composing position to -1.
        // This means that potentially a TextView could try to reuse this composition
        // position even if the cursor position changes, which would cause an unexpected
        // location to add new composed text. We'll probably need to call
        // finishComposingText if this state occurs, but we'll determine this difference in
        // implementation in onUpdateSelection, which is possibly dangerous to call
        // something to update the InputConnection because it could cause an infinite loop.
        // It might be better to queue up this call to be done the next time
        // setComposingText is called.
        if (mCompositionLength == 0) {
            finishComposingText();
        }

        testLog(TAG, "setComposingTextInternal: debug state: " + getDebugState());
    }

    /**
     * Replace the selected text (inserts for an empty selection) and place the cursor at the end
     * of the new text. This doesn't impact the composition unless the selection overlaps the
     * composition. If the selection is entirely in the composition, the new text is added as part
     * of the composition, but if any part of the selection is outside of the composition, the new
     * text will not be part of the composition.
     * @param text The text to enter.
     */
    public void enterText(final CharSequence text) {
        testLog(TAG, "enterText: init: " + getDebugStateInternal());
        if (TextUtils.isEmpty(text)) {
            return;
        }
        replaceTextRelative(0, mSelectionLength, text);
        if (mSelectionLength > 0) {
            shiftSelection(text.length());
        }
        mSelectionLength = 0;
    }

    /**
     * Delete text before the selection.
     * @param length The number of characters to delete.
     */
    public void deleteTextBeforeCursor(int length) {
        deleteTextRelative(-length, 0);
    }

    /**
     * Delete the text in the selection.
     */
    public void deleteSelectedText() {
        if (!isSelectionLengthKnown()) {
            //TODO: (EW) probably should actually handle this case (maybe shouldn't be called, but
            // should be handled for completeness)
            throw new RuntimeException("Trying to delete the selection with an unknown selection");
        }
        deleteTextRelative(0, mSelectionLength);
    }

    /**
     * Delete text after the selection.
     * @param length The number of characters to delete.
     */
    public void deleteTextAfterCursor(int length) {
        if (!isSelectionLengthKnown()) {
            //TODO: (EW) probably should actually handle this
            throw new RuntimeException("Trying to delete after the cursor with an unknown cursor length");
        }
        deleteTextRelative(mSelectionLength, mSelectionLength + length);
    }
    //#endregion

    //#region text cache management
    //#region invalidate

    /**
     * Clear the tracked state of the selection.
     * @param invalidatePosition Whether the absolute position of the selection should be cleared.
     * @param invalidateLength Whether the length of the selection should be cleared.
     */
    public void invalidateSelection(final boolean invalidatePosition,
                                    final boolean invalidateLength) {
        if (invalidatePosition) {
            mSelectionStart = UNKNOWN_POSITION;
        }
        if (invalidateLength) {
            mSelectionLength = UNKNOWN_LENGTH;
        }
    }

    /**
     * Clear the tracked state of the composition. This always clears the relative position of the
     * composition and optionally also the length of the composition.
     * @param invalidateLength Whether the length of the composition should be cleared (also
     *                         indicates that we don't know if there even is a composition).
     */
    public void invalidateComposition(final boolean invalidateLength) {
        mCompositionRelativeStartIndex = null;
        if (invalidateLength) {
            mCompositionLength = UNKNOWN_LENGTH;
        }
    }

    /**
     * Clear the entire text cache.
     * @return The number of characters that were removed.
     */
    public int invalidateTextCache() {
        return mTextCache.clear();
    }

    /**
     * Clear the text cache before a specific position.
     * @param absolutePosition The absolute position to clear text before.
     * @return The number of characters that were removed.
     */
    public int invalidateTextCacheBeforeAbsolute(final int absolutePosition) {
        if (!isAbsoluteSelectionStartKnown()) {
            Log.e(TAG, "Can't invalidate cache before " + absolutePosition
                    + " because the absolute position of the text cache is unknown");
            return 0;
        }
        return invalidateTextCacheBeforeRelative(absolutePosition - mSelectionStart);
    }

    /**
     * Clear the text cache before a specific position.
     * @param positionFromCursorStart The position (relative to the selection start) to clear text
     *                                before.
     * @return The number of characters that were removed.
     */
    public int invalidateTextCacheBeforeRelative(final int positionFromCursorStart) {
        testLog(TAG, "invalidateTextCacheBeforeRelative(" + positionFromCursorStart
                + "): mHoleyTextCache=\"" + mTextCache.mStringBuilder
                + "\", mHoleyTextCacheCursorStartIndex=" + mTextCache.mIndex
                + ", discarding=\"" + mTextCache.subSequence(mTextCache.minKnownPosition(), positionFromCursorStart, false) + "\"");
        return mTextCache.clearBefore(positionFromCursorStart);
    }

    /**
     * Clear the text cache after a specific position.
     * @param absolutePosition The absolute position to clear text after.
     * @return The number of characters that were removed.
     */
    public int invalidateTextCacheAfterAbsolute(final int absolutePosition) {
        if (!isAbsoluteSelectionStartKnown()) {
            Log.e(TAG, "Can't invalidate cache after " + absolutePosition
                    + " because the absolute position of the text cache is unknown");
            return 0;
        }
        return invalidateTextCacheAfterRelative(absolutePosition - mSelectionStart);
    }

    /**
     * Clear the text cache after a specific position.
     * @param positionFromCursorStart The position (relative to the selection start) to clear text
     *                                after.
     * @return The number of characters that were removed.
     */
    public int invalidateTextCacheAfterRelative(final int positionFromCursorStart) {
        testLog(TAG, "invalidateTextCacheAfterRelative(" + positionFromCursorStart
                + "): mHoleyTextCache=\"" + mTextCache.mStringBuilder
                + "\", mHoleyTextCacheCursorStartIndex=" + mTextCache.mIndex
                + ", discarding=\""
                + mTextCache.subSequence(positionFromCursorStart + 1, mTextCache.maxKnownPosition() + 1, false)
                + "\"");
        return mTextCache.clearAfter(positionFromCursorStart);
    }

    /**
     * Clear the text cache in a specific range.
     * @param startPositionFromCursorStart Range start (relative to selection start, inclusive).
     * @param endPositionFromCursorStart Range end (relative to selection start, inclusive).
     * @return The number of characters that were removed.
     */
    public int invalidateTextCacheRangeRelative(final int startPositionFromCursorStart,
                                                final int endPositionFromCursorStart) {
        testLog(TAG, "invalidateTextCacheBetweenRelative(" + startPositionFromCursorStart + ", "
                + endPositionFromCursorStart + "): mHoleyTextCache=\"" + mTextCache.mStringBuilder
                + "\", mHoleyTextCacheCursorStartIndex=" + mTextCache.mIndex
                + ", discarding=\""
                + mTextCache.subSequence(startPositionFromCursorStart,
                endPositionFromCursorStart + 1, false)
                + "\"");
        return mTextCache.clearRange(startPositionFromCursorStart, endPositionFromCursorStart);
    }
    //#endregion

    //#region update
    /**
     * Update text in the cache. Add/replace the text at a specific position in the cache. This
     * doesn't impact selection or composition positions.
     * @param text The text to set in the cache.
     * @param absoluteOffset The absolute start position of the updated text.
     */
    public void updateTextCache(final CharSequence text, final int absoluteOffset) {
        if (!isAbsoluteSelectionStartKnown()) {
            return;
        }
        updateTextCacheRelative(text, absoluteOffset - mSelectionStart);
    }

    /**
     * Update text in the cache. Add/replace the text at a specific position in the cache. This
     * doesn't impact selection or composition positions.
     * @param text The text to set in the cache.
     * @param offsetFromCursorStart The start position (relative to the selection start) of the
     *                              updated text.
     */
    public void updateTextCacheRelative(final CharSequence text, final int offsetFromCursorStart) {
        mTextCache.replace(offsetFromCursorStart, offsetFromCursorStart + text.length(),
                text);
    }
    //#endregion

    //#region unexpected change reconciliation
    /**
     * Replace some text. This should not be used for normal IME actions since it isn't clear what
     * should happen to the composition or selection positions when text is replaced overlapping the
     * start/end positions, and in such cases the only option is to clear cached values. This should
     * only be used to reconcile unexpected changes in the editor since there is no better option.
     * In these cases it is expected that the composing region, selection length, and/or other
     * cached text will need to be set again since it will fall into an unknown state.
     * @param absoluteStart The absolute start position of the text to replace.
     * @param absoluteEnd The absolute end position of the text to replace.
     * @param text The text to replace what is in the specified range.
     */
    public void replaceTextAbsolute(final int absoluteStart, final int absoluteEnd,
                                    final CharSequence text) {
        if (absoluteStart > absoluteEnd) {
            throw new IllegalArgumentException("Positions out of order");
        }
        if (!isAbsoluteSelectionStartKnown()) {
            // not sure where the text is in the cache that needs to change, so reset everything to
            // an unknown state
            invalidateSelection(true, mSelectionLength > 0);
            if (!isCompositionUnknown() && hasComposition()) {
                invalidateComposition(true);
            }
            mTextCache.clear();
            return;
        }
        final int startOffsetFromCursorStart = absoluteStart - mSelectionStart;
        final int endOffsetFromCursorStart = absoluteEnd - mSelectionStart;
        if (!isCompositionUnknown() && hasComposition()) {
            if (!isRelativeCompositionPositionKnown()) {
                // we don't know what of the composition may be replaced, so we can't know if there
                // even is a composition anymore
                mCompositionLength = UNKNOWN_LENGTH;
            } else if (startOffsetFromCursorStart < mCompositionRelativeStartIndex + mCompositionLength
                    && endOffsetFromCursorStart > mCompositionRelativeStartIndex) {
                // replacing some of the composition
                if (startOffsetFromCursorStart >= mCompositionRelativeStartIndex
                        && endOffsetFromCursorStart <= mCompositionRelativeStartIndex + mCompositionLength) {
                    // replacement is entirely within the composition, so the composition length
                    // needs to shift for the change of text
                    mCompositionLength += text.length() - (absoluteEnd - absoluteStart);
                } else {
                    // replacement is through the edge of the composition and there is no
                    // indication of how that impacts the composition, so we need to reset to an
                    // unknown state
                    invalidateComposition(true);
                }
            }
        }
        if ((startOffsetFromCursorStart < 0 && endOffsetFromCursorStart > 0)
                || (startOffsetFromCursorStart == 0 && endOffsetFromCursorStart == 0)) {
            // text is changing around the selection start with no indication of how that shifts the
            // selection start, so everything using that position as a reference needs to be reset
            // to an unknown state
            invalidateSelection(true, mSelectionLength > 0);
            invalidateComposition(false);
            mTextCache.clear();
            return;
        }
        if (!isCompositionUnknown() && hasComposition() && isRelativeCompositionPositionKnown()) {
            // shift the composition start if text is being replaced between that and the cursor
            // start
            if (startOffsetFromCursorStart >= mCompositionRelativeStartIndex + mCompositionLength
                    && endOffsetFromCursorStart <= 0) {
                mCompositionRelativeStartIndex -= text.length() - (absoluteEnd - absoluteStart);
            } else if (startOffsetFromCursorStart >= 0
                    && endOffsetFromCursorStart <= mCompositionRelativeStartIndex) {
                mCompositionRelativeStartIndex += text.length() - (absoluteEnd - absoluteStart);
            }
        }
        mTextCache.replace(startOffsetFromCursorStart, endOffsetFromCursorStart, text);
    }
    //#endregion

    /**
     * Delete text. The selection and composition positions will shift accordingly as text in or
     * before them are deleted (as much as the state of those is known).
     * @param relativeStartIndex The start index to delete (relative to the selection start),
     *                           exclusive.
     * @param relativeEndIndex The end index to delete (relative to the selection start), exclusive.
     */
    private void deleteTextRelative(int relativeStartIndex, int relativeEndIndex) {
        if (relativeStartIndex > relativeEndIndex) {
            throw new IllegalArgumentException("relativeStartIndex (" + relativeStartIndex
                    + ") > " + "relativeEndIndex (" + relativeEndIndex + ")");
        }
        testLog(TAG, "deleteTextRelative(" + relativeStartIndex + ", " + relativeEndIndex
                + "): init: " + getDebugStateInternal());
        printDebugState();
        if (relativeStartIndex == relativeEndIndex) {
            // nothing to delete
            return;
        }
        //TODO: (EW) should this validate that we don't delete past the text start (either error out
        // or cap to avoid buggy state)?
        // if there is any logic that relies on this doing what was requested, it would need to
        // handle this, so it might be better to have an expectation on the caller to validate
        // instead of doing it here

        replaceTextRelative(relativeStartIndex, relativeEndIndex - relativeStartIndex, null);
        printDebugState();
        testLog(TAG, "deleteTextRelative(" + relativeStartIndex + ", " + relativeEndIndex
                + "): final: " + getDebugStateInternal());
    }

    /**
     * Replace text at a specified position. This will shift the absolute positions for the
     * selection and composition. The new text will only be part of the composition if it is
     * entirely within the existing composition region (this doesn't include inserting at a single
     * point at either end of the composition). If the selection start or end is within the replaced
     * range, the position will shift to the end of the new text. Inserting text at the selection
     * start will shift the selection to stay after the new text.
     * @param relativeStartIndex The start index for the text to replace.
     * @param replaceLength The length of the text to replaced. This can be 0 to just insert the
     *                      text, or it could even be {@link #UNKNOWN_LENGTH} if it isn't clear what
     *                      should get replaced.
     * @param newText The new text to replace the existing text at the specified position.
     */
    private void replaceTextRelative(final int relativeStartIndex, final int replaceLength,
                                     final CharSequence newText) {
        final int newTextLength = newText == null ? 0 : newText.length();

        Integer selectionStartShift = getShiftForReplace(0, relativeStartIndex, replaceLength,
                newTextLength, true, true);
        if (selectionStartShift == null) {
            // we don't know how this impacts the selection start, so everything using that
            // position as a reference needs to be reset to an unknown state
            invalidateSelection(true, mSelectionLength > 0);
            if (!isCompositionUnknown() && hasComposition()) {
                invalidateComposition(!isCompositionRelativePositionKnown()
                        || mCompositionRelativeStartIndex + mCompositionLength > relativeStartIndex);
            }
            mTextCache.clear();
            return;
        }

        // replace the text
        mTextCache.replace(relativeStartIndex,
                replaceLength != UNKNOWN_LENGTH ? (relativeStartIndex + replaceLength) : 0,
                newText);
        testLog(TAG, "replaceText: mTextCache.replace: " + mTextCache.getDebugStateInternal());

        // update the selection
        testLog(TAG, "replaceText: selectionStartShift=" + selectionStartShift);
        mTextCache.shift(selectionStartShift);
        if (isAbsoluteSelectionStartKnown()) {
            mSelectionStart += selectionStartShift;
        }
        if (isSelectionLengthKnown()) {
            Integer selectionEndShift = getShiftForReplace(mSelectionLength, relativeStartIndex, replaceLength,
                    newTextLength, true, mSelectionLength == 0);
            if (selectionEndShift == null) {
                // we don't know how this impacts the selection end, so reset to an unknown state
                invalidateSelection(false, true);
            } else {
                mSelectionLength += selectionEndShift - selectionStartShift;
            }
        }

        // update the composition state if there is a known composition
        if (!isCompositionUnknown() && hasComposition()) {
            if (!isCompositionRelativePositionKnown()) {
                // we don't know how this may impact the composition since we don't even know where
                // it is, so reset to an unknown state
                testLog(TAG, "replaceText: invalidateComposition - unknown relative composition");
                invalidateComposition(true);
            } else {
                Integer compositionStartShift = getShiftForReplace(mCompositionRelativeStartIndex,
                        relativeStartIndex, replaceLength, newTextLength, true, true);
                Integer compositionEndShift = getShiftForReplace(mCompositionRelativeStartIndex + mCompositionLength,
                        relativeStartIndex, replaceLength, newTextLength, false, false);
                testLog(TAG, "replaceText: compositionStartShift=" + compositionStartShift + ", compositionEndShift=" + compositionEndShift);
                if (compositionStartShift == null || compositionEndShift == null) {
                    invalidateComposition(true);
                } else {
                    int newCompositionStart = mCompositionRelativeStartIndex + compositionStartShift - selectionStartShift;
                    int newCompositionEnd = mCompositionRelativeStartIndex + mCompositionLength + compositionEndShift - selectionStartShift;
                    testLog(TAG, "replaceText: newCompositionStart=" + newCompositionStart + ", newCompositionEnd=" + newCompositionEnd);
                    if (newCompositionStart >= newCompositionEnd) {
                        finishComposingText();
                    } else {
                        mCompositionRelativeStartIndex = newCompositionStart;
                        mCompositionLength = newCompositionEnd - newCompositionStart;
                    }
                }
            }
        }
    }

    /**
     * Determine the absolute position shift for a position for when some range of text is getting
     * replaced.
     * @param relativePosition The starting index (relative to the selection start) of the
     *                         position to determine the shift for.
     * @param replaceRelativeStartIndex The start index for the text that is getting replaced.
     * @param replaceLength The length of the existing text that is getting replaced. Note that this
     *                      can be 0 for a simple insertion, and it can even be
     *                      {@link #UNKNOWN_LENGTH} if it isn't clear what is getting replaced.
     * @param newTextLength The length of the new text that will replace the existing text.
     * @param shiftRightForReplace True if the position should shift to the end of the new text if
     *                             the position is within the range of the text that is getting
     *                             replaced. False if the position should shift to the beginning of
     *                             the new text in that case.
     * @param shiftRightForInsertion True if the position should shift to the end of the new text if
     *                               text is being inserted (replace length is 0) at the position
     *                               (meaning the text gets inserted before the position). False if
     *                               it should be kept at its original location in this case
     *                               (meaning text gets inserted after the position).
     * @return The shift for the absolute position for the specified position for when text gets
     *         replaced. If it can't be determined how much the position should shift, this will
     *         return null.
     */
    private static Integer getShiftForReplace(final int relativePosition,
                                              final int replaceRelativeStartIndex,
                                              final int replaceLength,
                                              final int newTextLength,
                                              final boolean shiftRightForReplace,
                                              final boolean shiftRightForInsertion) {
        if (relativePosition < replaceRelativeStartIndex) {
            // the position is before the text that is getting replaced, so its absolute position
            // doesn't shift
            return 0;
        }
        if (relativePosition == replaceRelativeStartIndex && (replaceLength > 0
                || (replaceLength == 0 && !shiftRightForInsertion))) {
            // the position is at the beginning of the text that is getting replaced, but it isn't
            // shifting with the replacement
            return 0;
        }
        if (replaceLength == UNKNOWN_LENGTH) {
            // we don't know what is getting replaced to know how this will impact the position
            return null;
        }
        if (replaceRelativeStartIndex + replaceLength < relativePosition
                || (replaceRelativeStartIndex + replaceLength == relativePosition
                && replaceLength > 0)) {
            // the position is after or at the end of the replaced text, so it shifts by the change
            // in text length
            return newTextLength - replaceLength;
        }
        // the position is in the text that is getting replaced
        if (shiftRightForReplace) {
            // the position shifts by the difference of the number of characters originally before
            // the position and the new text length to move it to the end of the new text
            return newTextLength - (relativePosition - replaceRelativeStartIndex);
        }
        // the position shifts by how far away the replace start is to move it to the beginning of
        // the new text
        return replaceRelativeStartIndex - relativePosition;
    }
    //#endregion

    //TODO: (EW) maybe put in a better place to share
    /**
     * Create a string of a particular length filled with a specific character.
     * @param character The character to fill in the string.
     * @param length The number of times the character should be in the string.
     * @return The string of the repeated character.
     */
    static String repeatChar(final char character, final int length) {
        //TODO: is this the most efficient way to do this?
        char[] indentArray = new char[length];
        Arrays.fill(indentArray, character);
        return new String(indentArray);
    }

    /**
     * A container for tracking text around a position that can shift. This supports leaving gaps
     * between regions of tracked text.
     */
    private static class RelativeCache {
        //TODO: (EW) consider making this a SpannableStringBuilder
        /** The container for the tracked text. */
        private final StringBuilder mStringBuilder = new StringBuilder();

        /** The index in {@link #mStringBuilder} that all actions are done relative to. */
        private int mIndex;

        /**
         * Create a relative cache.
         */
        public RelativeCache() {
        }

        /**
         * Create a relative cache.
         * @param copy An existing cache to copy.
         */
        public RelativeCache(final RelativeCache copy) {
            mStringBuilder.append(copy.mStringBuilder.toString());
            mIndex = copy.mIndex;
        }

        /**
         * Convert a position relative to {@link #mIndex} into an index in {@link #mStringBuilder}.
         * @param offsetFromIndex A position relative to the index to convert.
         * @return The corresponding index for the string builder.
         */
        private int indexInStringBuilder(final int offsetFromIndex) {
            return mIndex + offsetFromIndex;
        }

        /**
         * Get the min position that has a character set. Note that position still may have a
         * placeholder character. This simply marks the furthest position that either had a
         * character set or wasn't cleared.
         * @return The min position.
         */
        public int minKnownPosition() {
            return -mIndex;
        }

        /**
         * Get the max position that has a character set. Note that position still may have a
         * placeholder character. This simply marks the furthest position that either had a
         * character set or wasn't cleared.
         * @return The max position.
         */
        public int maxKnownPosition() {
            return mStringBuilder.length() - mIndex - 1;
        }

        /**
         * Get a character at a position relative to the current index in the cache.
         * {@link #NONCHARACTER_CODEPOINT_PLACEHOLDER} is returned if the requested position isn't
         * in the cache.
         * @param position The position relative to the current index. No value is considered out of
         *                 bounds since this cache is fundamentally built to support gaps in what
         *                 could be considered the normal range of tracked text.
         * @return The character at the specified position or a placeholder character if the
         *         position isn't in the cache.
         */
        public char charAt(final int position) {
            final int sbIndex = indexInStringBuilder(position);
            if (sbIndex < 0 || sbIndex >= mStringBuilder.length()) {
                return NONCHARACTER_CODEPOINT_PLACEHOLDER;
            }
            return mStringBuilder.charAt(indexInStringBuilder(position));
        }

        /**
         * Get a new character sequence that is a subsequence of the cached text. Since this cache
         * supports gaps, the requested range may contain gaps, and due to that, there is no real
         * defined range for the cache, so any range of text is valid. Nothing is considered out of
         * bounds. It just may not be cached.
         * @param start The start position (relative to the current index), inclusive.
         * @param end The end position (relative to the current index), inclusive.
         * @param skipIncomplete Whether to return null if any portion of the requested range isn't
         *                       cached, rather than returning a sequence with
         *                       {@link #NONCHARACTER_CODEPOINT_PLACEHOLDER}s placed where the text
         *                       wasn't cached.
         * @return A character sequence from the cache.
         */
        public CharSequence subSequence(final int start, final int end, boolean skipIncomplete) {
            if (start >= end) {
                return "";
            }
            int sbStartIndex = indexInStringBuilder(start);
            int sbEndIndex = indexInStringBuilder(end);
            if (skipIncomplete) {
                if (!isRangeCached(start, end)) {
                    return null;
                }
                return mStringBuilder.subSequence(sbStartIndex, sbEndIndex);
            }
            final StringBuilder sb = new StringBuilder();
            if (sbStartIndex < 0) {
                sb.append(repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER,
                        Math.min(0, sbEndIndex) - sbStartIndex));
            }
            if (sbEndIndex > 0 && sbStartIndex < mStringBuilder.length()) {
                sb.append(mStringBuilder.subSequence(
                        Math.max(0, sbStartIndex),
                        Math.min(mStringBuilder.length(), sbEndIndex)));
            }
            if (sbEndIndex > mStringBuilder.length()) {
                sb.append(repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER,
                        sbEndIndex - Math.max(mStringBuilder.length(), sbStartIndex)));
            }
            return sb;
        }

        /**
         * Check if a particular range of text is fully cached.
         * @param start The start position (relative to the current index), inclusive.
         * @param end The end position (relative to the current index), inclusive.
         * @return Whether the text is cached.
         */
        public boolean isRangeCached(final int start, final int end) {
            int sbStartIndex = indexInStringBuilder(start);
            int sbEndIndex = indexInStringBuilder(end);
            if (sbStartIndex < 0 || sbEndIndex > mStringBuilder.length()) {
                return false;
            }
            for (int i = sbStartIndex; i < sbEndIndex; i++) {
                if (mStringBuilder.charAt(i) == NONCHARACTER_CODEPOINT_PLACEHOLDER) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Shift the current index in the cache.
         * @param shift The amount to shift the current index.
         */
        public void shift(final int shift) {
            if (mStringBuilder.length() >= 0) {
                mIndex += shift;
            }
        }

        /**
         * Clear all text from the cache.
         * @return The number of characters (excluding placeholder characters) that were removed.
         */
        public int clear() {
            int removingCharCount = charCount();
            mStringBuilder.setLength(0);
            mIndex = 0;
            return removingCharCount;
        }

        /**
         * Clear all text in the cache that is before a particular position.
         * @param position The position relative to the current index to remove text before.
         * @return The number of characters (excluding placeholder characters) that were removed.
         */
        public int clearBefore(final int position) {
            final int sbIndex = indexInStringBuilder(position);
            if (sbIndex >= mStringBuilder.length()) {
                return clear();
            } else if (sbIndex > 0) {
                int removingCharCount = charCount(0, sbIndex);
                mStringBuilder.delete(0, sbIndex);
                mIndex -= sbIndex;
                return removingCharCount;
            }
            return 0;
        }

        /**
         * Clear all text in the cache that is after a particular position.
         * @param position The position relative to the current index to remove text after.
         * @return The number of characters (excluding placeholder characters) that were removed.
         */
        public int clearAfter(final int position) {
            final int sbIndex = indexInStringBuilder(position);
            if (sbIndex < 0) {
                return clear();
            } else if (sbIndex + 1 < mStringBuilder.length()) {
                int removingCharCount = charCount(sbIndex + 1, mStringBuilder.length());
                mStringBuilder.setLength(sbIndex + 1);
                if (mStringBuilder.length() == 0) {
                    mIndex = 0;
                }
                return removingCharCount;
            }
            return 0;
        }

        //TODO: (EW) should this make the end range exclusive to be consistent with most others (and
        // see if there are other cases like this that should be fixed too)?
        // could consider changing invalidateTextCacheAfterRelative to be exclusive since it's
        // mostly called with text end positions that are already exclusive, but the name might need
        // to change to avoid being confusing, but then it is less symmetrical with invalidating
        // before a position.
        /**
         * Clear a range of text.
         * @param start Range start to clear (inclusive).
         * @param end Range end to clear (inclusive).
         * @return The number of characters (excluding placeholder characters) that were removed.
         */
        public int clearRange(final int start, final int end) {
            if (start > end) {
                Log.e(TAG, "Invalid range to clear: " + start + " - " + end);
                //return 0;
                //TODO: (EW) probably remove - keeping for now to catch any of these issues before
                // release
                throw new IllegalArgumentException("Invalid range to clear: " + start + " - " + end);
            }
            final int sbStartIndex = indexInStringBuilder(start);
            final int sbEndIndex = indexInStringBuilder(end);
            if (sbStartIndex < 0) {
                return clearBefore(end + 1);
            } else if (sbEndIndex >= mStringBuilder.length()) {
                return clearAfter(sbStartIndex - 1);
            } else {
                int removingCharCount = charCount(sbStartIndex, sbEndIndex + 1);
                mStringBuilder.replace(sbStartIndex, sbEndIndex + 1,
                        repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER, end - start));
                return removingCharCount;
            }
        }

        /**
         * Replace some text in the cache. This doesn't impact the absolute position of the current
         * index even if the length of the text before the current index changes. In such a case,
         * the text around the current index would effectively change. If such a change should
         * change the index, it's the caller's responsibility to call {@link #shift} as appropriate.
         * @param start The start position (relative to the index) to replace text, inclusive.
         * @param end The end position (relative to the index) to replace text, exclusive.
         * @param text The text to insert as a replacement in the specified position or null to
         *             simply delete text.
         */
        private void replace(final int start, final int end, final CharSequence text) {
            testLog(TAG, "replace(" + start + ", " + end + ", \"" + text + "\"): "
                    + getDebugStateInternal());
            int sbStartIndex = indexInStringBuilder(start);
            int sbEndIndex = indexInStringBuilder(end);
            int newTextLength = text == null ? 0 : text.length();

            // if the range to replace text is neither overlapping nor adjacent to the existing
            // cache, we'll need to add placeholder characters in the space between the new and
            // existing text
            if (sbEndIndex < 0) {
                final int gapLength = -(sbEndIndex);
                mStringBuilder.insert(0, repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER, gapLength));
                // shift the relative position of the index since the beginning of the cache (its
                // reference point) changed
                testLog(TAG, "replace: mIndex+=" + gapLength);
                mIndex += gapLength;
                sbStartIndex += gapLength;
                sbEndIndex += gapLength;
            } else if (sbStartIndex > mStringBuilder.length()) {
                final int gapLength = sbStartIndex - mStringBuilder.length();
                mStringBuilder.append(repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER, gapLength));
            }

            if (newTextLength > 0) {
                mStringBuilder.replace(Math.max(0, sbStartIndex),
                        Math.min(mStringBuilder.length(), sbEndIndex),
                        text.toString());
            } else {
                mStringBuilder.delete(Math.max(0, sbStartIndex),
                        Math.min(mStringBuilder.length(), sbEndIndex));
            }
            if (mStringBuilder.length() == 0) {
                mIndex = 0;
            } else if (sbStartIndex < 0) {
                // shift the relative position of the index based on how much of the text change was
                // before the original cached text since that is now part of the cache
                mIndex += -sbStartIndex;
            }
            testLog(TAG, "replace: final " + getDebugStateInternal());
        }

        /**
         * Get the number of characters in the cache (excluding placeholder characters).
         * @return The number of characters in the cache.
         */
        public int charCount() {
            return charCount(0, mStringBuilder.length());
        }

        /**
         * Get the number of characters in the specified range in the cache (excluding placeholder
         * characters).
         * @param start The start position to count characters, inclusive.
         * @param end The start position to count characters, exclusive.
         * @return The number of characters in the cache.
         */
        private int charCount(final int start, final int end) {
            int count = 0;
            for (int i = start; i < end; i++) {
                if (mStringBuilder.charAt(i) != NONCHARACTER_CODEPOINT_PLACEHOLDER) {
                    count++;
                }
            }
            return count;
        }

        public String getDebugStateInternal() {
            return "mIndex=" + mIndex + ", mStringBuilder=\"" + mStringBuilder.toString() + "\"";
        }
    }
}
