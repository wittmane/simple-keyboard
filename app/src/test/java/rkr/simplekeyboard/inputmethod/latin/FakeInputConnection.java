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

import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FakeInputConnection implements InputConnection {
    private static final int INVALID_CURSOR_POSITION = -1;

    public static final int EXTRACT_TEXT_FROM_START = 1;
    public static final int EXTRACT_TEXT_FROM_END = 2;
    public static final int EXTRACT_TEXT_CENTERED_ON_SELECTION = 3;
    public static final int EXTRACT_TEXT_CENTERED_ON_SELECTION_START = 4;
    public static final int EXTRACT_TEXT_CENTERED_ON_SELECTION_END = 5;

    private final SpannableStringBuilder mText = new SpannableStringBuilder();
    private int mCurrentCursorStart = 0;
    private int mCurrentCursorEnd = 0;
    private int mCurrentComposingStart = INVALID_CURSOR_POSITION;
    private int mCurrentComposingEnd = INVALID_CURSOR_POSITION;

    private int mBatchLevel = 0;
    private String mEditInitialText = "";
    private int mEditInitialCursorStart = 0;
    private int mEditInitialCursorEnd = 0;
    private int mEditInitialComposingStart = INVALID_CURSOR_POSITION;
    private int mEditInitialComposingEnd = INVALID_CURSOR_POSITION;

    private HashMap<Integer, ExtractedTextRequest> mExtractedTextMonitorRequests = new HashMap<>();

    private boolean mAllowGettingText = true;

    private final FakeInputMethodManager mManager;
    private final VariableBehaviorSettings mSettings;

    private List<GetTextCall> mGetTextBeforeCursorCalls = new ArrayList<>();
    private List<GetSelectedTextCall> mGetSelectedTextCalls = new ArrayList<>();
    private List<GetTextCall> mGetTextAfterCursorCalls = new ArrayList<>();
    private List<GetExtractedTextCall> mGetExtractedTextCalls = new ArrayList<>();

    public String getDebugState() {
        return "text=\"" + mText + "\", selectionStart=" + mCurrentCursorStart
                + ", selectionEnd=" + mCurrentCursorEnd
                + ", compositionStart=" + mCurrentComposingStart
                + ", compositionEnd=" + mCurrentComposingEnd;
    }

    public String getSettingsDebug() {
        return "FakeInputConnect settings:"
                + "\n  mGetTextLimit=" + mSettings.getTextLimit
                + "\n  mSetComposingRegionSupported=" + mSettings.setComposingRegionSupported
                + "\n  mGetSelectedTextSupported=" + mSettings.getSelectedTextSupported
                + "\n  mDeleteAroundComposingText=" + mSettings.deleteAroundComposingText
                + "\n  mKeepEmptyComposingPosition=" + mSettings.keepEmptyComposingPosition
                + "\n  textModifiers=" + Arrays.toString(mSettings.textModifiers)
                + "\n  mAllowGettingText=" + mAllowGettingText
                + "\n  mAllowExtractingText=" + mSettings.allowExtractingText
                + "\n  mExtractTextLimit=" + mSettings.extractTextLimit
                + "\n  mExtractTextLimitedTarget=" + mSettings.extractTextLimitedTarget
                + "\n  mPartialTextMonitorUpdates=" + mSettings.partialTextMonitorUpdates
                + "\n  mExtractMonitorTextLimit=" + mSettings.extractMonitorTextLimit
                + "\n  mUpdateSelectionAfterExtractedText="
                + mSettings.updateSelectionAfterExtractedText
                + "\n  mDropSelectionAfterExtractedTextMonitorRequest="
                + mSettings.dropSelectionAfterExtractedTextMonitorRequest
                + "\n  sendSelectionUpdateWhenNotChanged="
                + mSettings.sendSelectionUpdateWhenNotChanged
                + "\n  sendExtractedTextUpdateBasedOnNetTextChange="
                + mSettings.sendExtractedTextUpdateBasedOnNetTextChange;
    }

    //TODO: (EW) remove - only used in old tests
    public FakeInputConnection(final FakeInputMethodManager manager) {
        this(manager, null, 0, 0, Integer.MAX_VALUE,
                true, true, false, false,
                true, Integer.MAX_VALUE, true, true);
    }

    public FakeInputConnection(final FakeInputMethodManager manager, final String initialText,
                               final int initialCursorStart, final int initialCursorEnd) {
        this(manager, initialText, initialCursorStart, initialCursorEnd,
                new VariableBehaviorSettings());
    }

    public FakeInputConnection(final FakeInputMethodManager manager, final String initialText,
                               final int initialCursorStart, final int initialCursorEnd,
                               final VariableBehaviorSettings settings) {
        mManager = manager;
        mSettings = new VariableBehaviorSettings(settings);

        if (initialText != null) {
            mText.append(initialText);
            if (initialCursorStart > initialCursorEnd || initialCursorStart < 0
                    || initialCursorEnd > initialText.length()) {
                throw new IllegalArgumentException("initialText=\"" + initialText
                        + "\", initialCursorStart=" + initialCursorStart
                        + ", initialCursorEnd=" + initialCursorEnd);
            }
            mCurrentCursorStart = initialCursorStart;
            mCurrentCursorEnd = initialCursorEnd;
        } else if (initialCursorStart != 0 || initialCursorEnd != 0) {
            throw new IllegalArgumentException("initialCursorStart=" + initialCursorStart
                    + ", initialCursorEnd=" + initialCursorEnd);
        }
    }

    //TODO: (EW) remove - only used in old tests
    public FakeInputConnection(final FakeInputMethodManager manager, final String initialText,
                               final int initialCursorStart, final int initialCursorEnd,
                               final int getTextLimit, final boolean setComposingRegionSupported,
                               final boolean getSelectedTextSupported,
                               final boolean deleteAroundComposingText,
                               final boolean keepEmptyComposingPosition,
                               final boolean partialTextMonitorUpdates,
                               final int extractMonitorTextLimit,
                               final boolean updateSelectionAfterExtractedText,
                               final boolean dropSelectionAfterExtractedTextMonitorRequest) {
        this(manager, initialText, initialCursorStart, initialCursorEnd,
                new VariableBehaviorSettings()
                        .setGetTextLimit(getTextLimit)
                        .supportSetComposingRegion(setComposingRegionSupported)
                        .supportGetSelectedText(getSelectedTextSupported)
                        .forceDeletingAroundComposingText(deleteAroundComposingText)
                        .forceKeepingEmptyComposingPosition(keepEmptyComposingPosition)
                        .setExtractedTextMonitorInfo(partialTextMonitorUpdates, extractMonitorTextLimit)
                        .sendUpdatesForSelectionAfterUpdatesForExtractedText(updateSelectionAfterExtractedText)
                        .forceDroppingSelectionAfterExtractedTextMonitorRequest(dropSelectionAfterExtractedTextMonitorRequest)
        );
    }


    public static class VariableBehaviorSettings {
        //TODO: (EW) make private
        int getTextLimit = Integer.MAX_VALUE;
        //TODO: (EW) make private
        boolean setComposingRegionSupported = true;
        private boolean getSelectedTextSupported = true;
        private boolean deleteAroundComposingText = false;
        private boolean keepEmptyComposingPosition = false;
        //TODO: (EW) make private
        FakeInputConnection.TextModifier[] textModifiers = new TextModifier[0];
        //TODO: (EW) make private
        boolean allowExtractingText = true;
        //TODO: (EW) make private
        int extractTextLimit = Integer.MAX_VALUE;
        private int extractTextLimitedTarget = EXTRACT_TEXT_CENTERED_ON_SELECTION;
        //TODO: (EW) I don't think it's valid for an editor to just ignore this flag if it does
        // support extracting text normally, so this probably should be removed. it's useful for now
        // to skip that update to simplify/isolate the test for delayed updates and works to
        // simulate the case where we don't request these updates yet. especially once everything is
        // developed, it might be better to not test this fake case or find a better way to test
        // something similar.
        private boolean allowExtractedTextMonitor = true;
        //TODO: maybe the default should be full updates to fit limitReturnedText better, rather
        // than match what the default EditText does
        //TODO: (EW) make private
        boolean partialTextMonitorUpdates = true;
        int extractMonitorTextLimit = Integer.MAX_VALUE;
        //TODO: (EW) make private
        boolean updateSelectionAfterExtractedText = true;
        private boolean dropSelectionAfterExtractedTextMonitorRequest = true;
        //TODO: (EW) I'm not sure if using this is valid since the framework itself seems to block
        // duplicate updates
        private boolean sendSelectionUpdateWhenNotChanged = false;
        //TODO: (EW) this probably should be false to match the framework as a default
        private boolean sendExtractedTextUpdateBasedOnNetTextChange = true;

        public VariableBehaviorSettings() {
        }

        public VariableBehaviorSettings(VariableBehaviorSettings copy) {
            getTextLimit = copy.getTextLimit;
            setComposingRegionSupported = copy.setComposingRegionSupported;
            getSelectedTextSupported = copy.getSelectedTextSupported;
            deleteAroundComposingText = copy.deleteAroundComposingText;
            keepEmptyComposingPosition = copy.keepEmptyComposingPosition;
            textModifiers = copy.textModifiers;
            allowExtractingText = copy.allowExtractingText;
            extractTextLimit = copy.extractTextLimit;
            extractTextLimitedTarget = copy.extractTextLimitedTarget;
            allowExtractedTextMonitor = copy.allowExtractedTextMonitor;
            partialTextMonitorUpdates = copy.partialTextMonitorUpdates;
            extractMonitorTextLimit = copy.extractMonitorTextLimit;
            updateSelectionAfterExtractedText = copy.updateSelectionAfterExtractedText;
            dropSelectionAfterExtractedTextMonitorRequest =
                    copy.dropSelectionAfterExtractedTextMonitorRequest;
            sendSelectionUpdateWhenNotChanged = copy.sendSelectionUpdateWhenNotChanged;
            sendExtractedTextUpdateBasedOnNetTextChange =
                    copy.sendExtractedTextUpdateBasedOnNetTextChange;
        }

        public VariableBehaviorSettings blockBaseExtractText() {
            return allowExtractingText(false);
        }

        public VariableBehaviorSettings allowExtractingText(final boolean allow) {
            allowExtractingText = allow;
            return this;
        }

        public VariableBehaviorSettings limitExtractedText(final int textLimit,
                                                           final int contentToExtract) {
            extractTextLimit = textLimit;
            extractTextLimitedTarget = contentToExtract;
            return this;
        }

        public VariableBehaviorSettings blockExtractedTextMonitor() {
            return allowExtractedTextMonitor(false);
        }

        public VariableBehaviorSettings allowExtractedTextMonitor(boolean allow) {
            allowExtractedTextMonitor = allow;
            return this;
        }

        public VariableBehaviorSettings limitReturnedText(
                final int getTextLimit,
                final boolean partialTextMonitorUpdates,
                final int extractMonitorTextLimit) {
            return setGetTextLimit(getTextLimit)
                    .setExtractedTextMonitorInfo(partialTextMonitorUpdates,
                            extractMonitorTextLimit);
        }

        public VariableBehaviorSettings setGetTextLimit(int limit) {
            getTextLimit = limit;
            return this;
        }

        public VariableBehaviorSettings setExtractedTextMonitorInfo(
                boolean partialTextMonitorUpdates, int extractMonitorTextLimit) {
            this.partialTextMonitorUpdates = partialTextMonitorUpdates;
            this.extractMonitorTextLimit = extractMonitorTextLimit;
            return this;
        }

        public VariableBehaviorSettings updateSelectionBeforeExtractedText() {
            return sendUpdatesForSelectionAfterUpdatesForExtractedText(false);
        }

        public VariableBehaviorSettings updateSelectionAfterExtractedText() {
            return sendUpdatesForSelectionAfterUpdatesForExtractedText(true);
        }

        public VariableBehaviorSettings sendUpdatesForSelectionAfterUpdatesForExtractedText(boolean updateSelectionAfterExtractedText) {
            this.updateSelectionAfterExtractedText = updateSelectionAfterExtractedText;
            return this;
        }

        public VariableBehaviorSettings setComposingRegionNotSupported() {
            return supportSetComposingRegion(false);
        }

        public VariableBehaviorSettings supportSetComposingRegion(boolean methodSupported) {
            setComposingRegionSupported = methodSupported;
            return this;
        }

        public VariableBehaviorSettings getSelectedTextNotSupported() {
            return supportGetSelectedText(false);
        }

        public VariableBehaviorSettings supportGetSelectedText(boolean methodSupported) {
            getSelectedTextSupported = methodSupported;
            return this;
        }

        //TODO: maybe flip this default since that's how the default EditText works
        public VariableBehaviorSettings deleteAroundComposingText() {
            return forceDeletingAroundComposingText(true);
        }
        public VariableBehaviorSettings deleteThroughComposingText() {
            return forceDeletingAroundComposingText(false);
        }
        public VariableBehaviorSettings forceDeletingAroundComposingText(
                boolean deleteAroundComposingText) {
            this.deleteAroundComposingText = deleteAroundComposingText;
            return this;
        }

        //TODO: (EW) this doesn't seem to be used yet
        public VariableBehaviorSettings keepEmptyComposingPosition() {
            return forceKeepingEmptyComposingPosition(true);
        }
        public VariableBehaviorSettings forceKeepingEmptyComposingPosition(
                boolean keepEmptyComposingPosition) {
            this.keepEmptyComposingPosition = keepEmptyComposingPosition;
            return this;
        }

        public VariableBehaviorSettings setTextModifier(final TextModifier textModifier) {
            return setTextModifiers(textModifier == null
                    ? null
                    : new TextModifier[] {textModifier});
        }

        public VariableBehaviorSettings setTextModifiers(final TextModifier[] textModifiers) {
            this.textModifiers = textModifiers == null ? new TextModifier[0] : textModifiers;
            return this;
        }

        public VariableBehaviorSettings forceDroppingSelectionAfterExtractedTextMonitorRequest(
                boolean dropSelectionAfterExtractedTextMonitorRequest) {
            this.dropSelectionAfterExtractedTextMonitorRequest =
                    dropSelectionAfterExtractedTextMonitorRequest;
            return this;
        }

        public VariableBehaviorSettings sendSelectionUpdateWhenNotChanged(
                boolean sendSelectionUpdateWhenNotChanged) {
            this.sendSelectionUpdateWhenNotChanged = sendSelectionUpdateWhenNotChanged;
            return this;
        }

        public VariableBehaviorSettings sendExtractedTextUpdateBasedOnNetTextChange(
                boolean sendExtractedTextUpdateBasedOnNetTextChange) {
            this.sendExtractedTextUpdateBasedOnNetTextChange =
                    sendExtractedTextUpdateBasedOnNetTextChange;
            return this;
        }
    }

    public VariableBehaviorSettings getSettings() {
        return mSettings;
    }

    private CharSequence modifyText(CharSequence text, boolean isComposing) {
        for (TextModifier textModifier : mSettings.textModifiers) {
            if (isComposing) {
                text = textModifier.modifyComposingText(text);
            } else {
                text = textModifier.modifyCommittingText(text);
            }
        }
        return text;
    }

    //TODO: merge with mGetTextLimit (maybe also take -1 to return null instead of empty string)
    //TODO: is blocking getting text entirely even a valid thing for the text editor to do?
    public void allowGettingText(final boolean allow) {
        mAllowGettingText = allow;
    }

    private boolean hasComposingText() {
        return mCurrentComposingStart != INVALID_CURSOR_POSITION
                && mCurrentComposingEnd != INVALID_CURSOR_POSITION;
    }
    private int getNewCursorPosition(final int newCursorPosition,
                                     final int spanStart, final int spanEnd) {
        if (newCursorPosition > 0) {
            return Math.min(spanEnd - 1 + newCursorPosition, mText.length());
        }
        return Math.max(spanStart + newCursorPosition, 0);
    }
    public void setText(CharSequence text, int start, int end) {
        Log.w("Fake", "setText(\"" + text + "\", " + start + ", " + end + "): " + getDebugState());
        if (start > end) {
            throw new IllegalArgumentException("start (" + start + ") > end (" + end + ")");
        }
        if (start != end) {
            mText.delete(start, end);
        }
        //TODO: verify if this if is even necessary - does insert work at the end?
        if (start < mText.length()) {
            mText.insert(start, text);
        } else {
            mText.append(text);
        }
        int[] selection = updateRangeForReplace(start, end, text.length(),
                mCurrentCursorStart, mCurrentCursorEnd);
        mCurrentCursorStart = selection[0];
        mCurrentCursorEnd = selection[1];
        if (mCurrentComposingStart != INVALID_CURSOR_POSITION
                && mCurrentComposingEnd != INVALID_CURSOR_POSITION) {
            int[] composition = updateRangeForReplace(start, end, text.length(),
                    mCurrentComposingStart, mCurrentComposingEnd);
            mCurrentComposingStart = composition[0];
            mCurrentComposingEnd = composition[1];
            // Documentation doesn't clearly indicate whether deleting the last character of the
            // composing text should clear the composing position, but due to some wording of the
            // documentation of setComposingText, there is room for interpretation that passing an
            // empty string could keep the composing position (so additional composing text would be
            // added in the same place), so we'll use a setting here too to allow testing the case
            // where an implementation might keep the composing position once the whole composing
            // text has been deleted just to be safe.
            if (!mSettings.keepEmptyComposingPosition
                    && mCurrentComposingStart == mCurrentComposingEnd) {
                mCurrentComposingStart = INVALID_CURSOR_POSITION;
                mCurrentComposingEnd = INVALID_CURSOR_POSITION;
            }
        }
        Log.w("Fake", "setText: finished: " + getDebugState());
    }
    private int[] updateRangeForReplace(final int replaceStart, final int replaceEnd,
                                        final int newLength, int rangeStart, int rangeEnd) {
        if (replaceEnd <= rangeStart) {
            rangeStart += newLength - (replaceEnd - replaceStart);
            rangeEnd += newLength - (replaceEnd - replaceStart);
        } else if (rangeStart <= replaceStart && rangeEnd >= replaceEnd
                && (replaceStart < replaceEnd || replaceEnd < rangeEnd)) {
            rangeEnd += newLength - (replaceEnd - replaceStart);
        } else if (replaceStart <= rangeStart) {
            rangeStart = replaceStart + newLength;
            if (replaceEnd <= rangeEnd) {
                rangeEnd += newLength - (replaceEnd - replaceStart);
            } else {
                rangeEnd = replaceStart + newLength;
            }
        } else if (replaceStart < rangeEnd) {
            rangeEnd = replaceStart;
        }
        return new int[] { rangeStart, rangeEnd };
    }
    public void deleteText(int start, int end) {
        setText("", start, end);
    }

    // Get n characters of text before the current cursor position.
    //
    // This method may fail either if the input connection has become invalid (such as its process
    // crashing) or the editor is taking too long to respond with the text (it is given a couple
    // seconds to return). In either case, null is returned. This method does not affect the text
    // in the editor in any way, nor does it affect the selection or composing spans.
    //
    // If GET_TEXT_WITH_STYLES is supplied as flags, the editor should return a SpannableString with
    // all the spans set on the text.
    //
    // IME authors: please consider this will trigger an IPC round-trip that will take some time.
    // Assume this method consumes a lot of time. Also, please keep in mind the Editor may choose to
    // return less characters than requested even if they are available for performance reasons. If
    // you are using this to get the initial text around the cursor, you may consider using
    // EditorInfo#getInitialTextBeforeCursor(int, int), EditorInfo#getInitialSelectedText(int), and
    // EditorInfo#getInitialTextAfterCursor(int, int) to prevent IPC costs.
    //
    // Editor authors: please be careful of race conditions in implementing this call. An IME can
    // make a change to the text and use this method right away; you need to make sure the returned
    // value is consistent with the result of the latest edits. Also, you may return less than n
    // characters if performance dictates so, but keep in mind IMEs are relying on this for many
    // functions: you should not, for example, limit the returned value to the current line, and
    // specifically do not return 0 characters unless the cursor is really at the start of the text.
    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        Log.w("Fake", "getTextBeforeCursor: n=" + n + ", flags=" + flags);
        mGetTextBeforeCursorCalls.add(new GetTextCall(n, flags));
        if (n < 0) {
            throw new IllegalArgumentException("n = " + n);
        }
        if (!mAllowGettingText) {
            return "";
        }
        return mText.subSequence(
                Math.max(0, mCurrentCursorStart - Math.min(n, mSettings.getTextLimit)),
                mCurrentCursorStart);
    }

    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        Log.w("Fake", "getTextAfterCursor: n=" + n + ", flags=" + flags);
        mGetTextAfterCursorCalls.add(new GetTextCall(n, flags));
        if (n < 0) {
            throw new IllegalArgumentException("n = " + n);
        }
        if (!mAllowGettingText) {
            return "";
        }
        final int end = Math.min(mText.length(),
                mCurrentCursorEnd + Math.min(Math.min(n, mSettings.getTextLimit),
                        Integer.MAX_VALUE - mCurrentCursorEnd));
//        Log.w("Fake", "getTextAfterCursor end=" + end + ", n=" + n
//                + ", n2=" + Math.min(n, mSettings.getTextLimit));
        return mText.subSequence(mCurrentCursorEnd, end);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        Log.w("Fake", "getSelectedText: flags=" + flags);
        mGetSelectedTextCalls.add(new GetSelectedTextCall(flags));
        if (!mSettings.getSelectedTextSupported || !mAllowGettingText) {
            return null;
        }
        return mText.subSequence(mCurrentCursorStart, mCurrentCursorEnd);
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        // currently unused - no need to implement the fake version
        return 0;
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest extractedTextRequest, int flags) {
        mGetExtractedTextCalls.add(new GetExtractedTextCall(extractedTextRequest, flags));
        if (flags == InputConnection.GET_EXTRACTED_TEXT_MONITOR) {
            if (mExtractedTextMonitorRequests.containsKey(extractedTextRequest.token)) {
                mExtractedTextMonitorRequests.replace(extractedTextRequest.token, extractedTextRequest);
            } else {
                mExtractedTextMonitorRequests.put(extractedTextRequest.token, extractedTextRequest);
            }
        }
        if (!mSettings.allowExtractingText) {
            return null;
        }
        final ExtractedText result = new ExtractedText();
        // the default implementation doesn't respect extractedTextRequest.hintMaxChars or
        // extractedTextRequest.hintMaxLines here for some reason, so we'll do the same and return
        // the full text unless there is a get text limit
        int endOffset;
        if (mSettings.extractTextLimit == Integer.MAX_VALUE) {
            result.startOffset = 0;
            endOffset = mText.length();
        } else {
            switch (mSettings.extractTextLimitedTarget) {
                case EXTRACT_TEXT_FROM_START:
                    result.startOffset = 0;
                    endOffset = Math.min(mText.length(), mSettings.extractTextLimit);
                    break;
                case EXTRACT_TEXT_FROM_END:
                    result.startOffset = Math.max(0, mText.length() - mSettings.extractTextLimit);
                    endOffset = mText.length();
                    break;
                case EXTRACT_TEXT_CENTERED_ON_SELECTION_START:
                    result.startOffset = Math.max(0,
                            mCurrentCursorStart - mSettings.extractTextLimit);
                    endOffset = Math.min(mText.length(),
                            mCurrentCursorStart + mSettings.extractTextLimit);
                    break;
                case EXTRACT_TEXT_CENTERED_ON_SELECTION_END:
                    result.startOffset = Math.max(0,
                            mCurrentCursorEnd - mSettings.extractTextLimit);
                    endOffset = Math.min(mText.length(),
                            mCurrentCursorEnd + mSettings.extractTextLimit);
                    break;
                case EXTRACT_TEXT_CENTERED_ON_SELECTION:
                default:
                    result.startOffset = Math.max(0,
                            mCurrentCursorStart - mSettings.extractTextLimit);
                    endOffset = Math.min(mText.length(),
                            mCurrentCursorEnd + mSettings.extractTextLimit);
                    break;
            }
        }
        result.text = mText.subSequence(result.startOffset, endOffset);
        result.selectionStart = mCurrentCursorStart - result.startOffset;
        result.selectionEnd = mCurrentCursorEnd - result.startOffset;
        result.partialStartOffset = -1;
        result.partialEndOffset = -1;

        // for some reason the default implementation drops the selection, but still reports the
        // original selection in the request (at least in some cases)
        if (flags == InputConnection.GET_EXTRACTED_TEXT_MONITOR
                && mSettings.dropSelectionAfterExtractedTextMonitorRequest) {
            mCurrentCursorStart = mCurrentCursorEnd;
            //TODO: (EW) this should call updateSelection, but it should be triggered after this
            // function returns. currently this doesn't really matter because the real code should
            // avoid hitting the case where this changes the cursor position.
        }

        return result;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        // Documentation warns IME authors to avoid deleting only half of a surrogate pair, but it
        // doesn't describe how the input connection should handle this. The default TextView seems
        // to just allow deleting half of a surrogate, so this fake will do the same and not add any
        // special handling for this case.

        handleEditStart();
        final int beforeEnd;
        final int afterStart;
        if (mSettings.deleteAroundComposingText && hasComposingText()) {
            // this is how some default implementations behave
            beforeEnd = Math.min(mCurrentCursorStart, mCurrentComposingStart);
            afterStart = Math.max(mCurrentCursorEnd, mCurrentComposingEnd);
        } else {
            // this is how the documentation seems to indicate it should behave
            beforeEnd = mCurrentCursorStart;
            afterStart = mCurrentCursorEnd;
        }
        deleteText(afterStart, Math.min(mText.length(), afterStart + afterLength));
        final int beforeStart = Math.max(0, beforeEnd - beforeLength);
        deleteText(beforeStart, beforeEnd);
        handleEditEnd();
        return true;
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        // currently unused - no need to implement the fake version
        return false;
    }

    // Replace the currently composing text with the given text, and set the new cursor position.
    // Any composing text set previously will be removed automatically.
    //
    // If there is any composing span currently active, all characters that it comprises are
    // removed. The passed text is added in its place, and a composing span is added to this text.
    // If there is no composing span active, the passed text is added at the cursor position
    // (removing selected characters first if any), and a composing span is added on the new text.
    // Finally, the cursor is moved to the location specified by newCursorPosition.
    //
    // This is usually called by IMEs to add or remove or change characters in the composing span.
    // Calling this method will cause the editor to call
    // InputMethodService.onUpdateSelection(int, int, int, int, int, int) on the current IME after
    // the batch input is over.
    //
    // Editor authors: please keep in mind the text may be very similar or completely different than
    // what was in the composing span at call time, or there may not be a composing span at all.
    // Please note that although it's not typical use, the string may be empty. Treat this normally,
    // replacing the currently composing text with an empty string. Also, be careful with the cursor
    // position. IMEs rely on this working exactly as described above. Since this changes the
    // contents of the editor, you need to make the changes known to the input method by calling
    // InputMethodManager#updateSelection(View, int, int, int, int), but be careful to wait until
    // the batch edit is over if one is in progress. Note that this method can set the cursor
    // position on either edge of the composing text or entirely outside it, but the IME may also go
    // on to move the cursor position to within the composing text in a subsequent call so you
    // should make no assumption at all: the composing text and the selection are entirely
    // independent.
    //
    // newCursorPosition - The new cursor position around the text. If > 0, this is relative to the
    // end of the text - 1; if <= 0, this is relative to the start of the text. So a value of 1 will
    // always advance you to the position after the full text being inserted. Note that this means
    // you can't position the cursor within the text, because the editor can make modifications to
    // the text you are providing so it is not possible to correctly specify locations there.
    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        //Note: newCursorPosition is in characters, not code points

        handleEditStart();
        text = modifyText(text, true);
        final int start;
        final int end;
        if (hasComposingText()) {
            start = mCurrentComposingStart;
            end = mCurrentComposingEnd;
        } else {
            start = mCurrentCursorStart;
            end = mCurrentCursorEnd;
        }
        setText(text, start, end);
        mCurrentComposingStart = start;
        mCurrentComposingEnd = start + text.length();
        final int textStart = mCurrentComposingStart;
        // Documentation indicates that an empty string can be passed and tells editor
        // authors that it should be handled normally, replacing the current composing text
        // with an empty string. This could be interpreted as the composing region should
        // still be retained as part of treating it "normally", but the default TextView
        // updates the composing position to -1. We'll check a setting to allow testing both cases.
        if (!mSettings.keepEmptyComposingPosition
                && mCurrentComposingStart == mCurrentComposingEnd) {
            mCurrentComposingStart = INVALID_CURSOR_POSITION;
            mCurrentComposingEnd = INVALID_CURSOR_POSITION;
        }
        mCurrentCursorStart =
                getNewCursorPosition(newCursorPosition, textStart, textStart + text.length());
        mCurrentCursorEnd = mCurrentCursorStart;
        handleEditEnd();
        Log.w("Fake", "setComposingText: finished: " + getDebugState());
        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        if (!mSettings.setComposingRegionSupported) {
            // In Build.VERSION_CODES.N and later, false is returned when the target application
            // does not implement this method.
            return false;
        }
        handleEditStart();
        // Since this does not change the contents of the text, editors should not call
        // InputMethodManager#updateSelection, so calling handleEditStart/handleEditEnd would be
        // inappropriate.
        if (start < end) {
            mCurrentComposingStart = start;
            mCurrentComposingEnd = end;
        } else if (start > end) {
            mCurrentComposingStart = end;
            mCurrentComposingEnd = start;
        } else {
            mCurrentComposingStart = INVALID_CURSOR_POSITION;
            mCurrentComposingEnd = INVALID_CURSOR_POSITION;
        }
        handleEditEnd();
        return true;
    }

    // Have the text editor finish whatever composing text is currently active. This simply leaves
    // the text as-is, removing any special composing styling or other state that was around it. The
    // cursor position remains unchanged.
    // Be aware that this call may be expensive with some editors.
    @Override
    public boolean finishComposingText() {
        // The documentation doesn't call out either way whether this should trigger an update, but
        // the default EditText does, so this fake will match that. Also, since the composing
        // position is part of what is sent in the update call, it seems likely that this is what
        // should be happening. A flag for testing to skip the batch could be added, but unless
        // there is evidence that some text fields do this (not just one buggy one), that doesn't
        // seem necessary.
        // The documentation states that this simply leaves the text as-is, so we don't need a
        // special test case for modifying the composing text.
        handleEditStart();
        mCurrentComposingStart = INVALID_CURSOR_POSITION;
        mCurrentComposingEnd = INVALID_CURSOR_POSITION;
        handleEditEnd();
        return true;
    }

    // Commit text to the text box and set the new cursor position.
    //
    // This method removes the contents of the currently composing text and replaces it with the
    // passed CharSequence, and then moves the cursor according to newCursorPosition. If there is no
    // composing text when this method is called, the new text is inserted at the cursor position,
    // removing text inside the selection if any. This behaves like calling
    // setComposingText(text, newCursorPosition) then finishComposingText().
    //
    // Calling this method will cause the editor to call
    // InputMethodService.onUpdateSelection(int, int, int, int, int, int) on the current IME after
    // the batch input is over. Editor authors, for this to happen you need to make the changes
    // known to the input method by calling
    // InputMethodManager#updateSelection(View, int, int, int, int), but be careful to wait until
    // the batch edit is over if one is in progress.
    //
    // newCursorPosition - The new cursor position around the text, in Java characters. If > 0, this
    // is relative to the end of the text - 1; if <= 0, this is relative to the start of the text.
    // So a value of 1 will always advance the cursor to the position after the full text being
    // inserted. Note that this means you can't position the cursor within the text, because the
    // editor can make modifications to the text you are providing so it is not possible to
    // correctly specify locations there.
    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        //Note: newCursorPosition is in characters, not code points

        text = modifyText(text, false);
        handleEditStart();
        final int textStart;
        final int textInitialEnd;
        if (hasComposingText()) {
            textStart = mCurrentComposingStart;
            textInitialEnd = mCurrentComposingEnd;
            mCurrentComposingStart = INVALID_CURSOR_POSITION;
            mCurrentComposingEnd = INVALID_CURSOR_POSITION;
        } else {
            textStart = mCurrentCursorStart;
            textInitialEnd = mCurrentCursorEnd;
        }
        setText(text, textStart, textInitialEnd);
        mCurrentCursorStart =
                getNewCursorPosition(newCursorPosition, textStart, textStart + text.length());
        mCurrentCursorEnd = mCurrentCursorStart;
        handleEditEnd();
        return true;
    }

    @Override
    public boolean commitCompletion(CompletionInfo text) {
        // currently unused - no need to implement the fake version
        return false;
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        // currently unused - no need to implement the fake version
        return false;
    }

    @Override
    public boolean setSelection(int start, int end) {
        // Documentation doesn't describe how placing the cursor in the middle of a surrogate pair
        // should be handled, but the default TextView seems to just allow it, so this fake will do
        // the same and not add any special handling.

        handleEditStart();
        if (Math.min(start, end) < 0 || Math.max(start, end) > mText.length()) {
            //TODO: this possibly shouldn't crash - it might be valid to set a selection not knowing the actual size (maybe < 0 could still error)
            throw new IllegalArgumentException("start=" + start + ", end=" + end + ", mText.length=" + mText.length() + ", mText=\"" + mText + "\"");
        }
        if (end < start) {
            mCurrentCursorStart = end;
            mCurrentCursorEnd = start;
        } else {
            mCurrentCursorStart = start;
            mCurrentCursorEnd = end;
        }
        handleEditEnd();
        return true;
    }

    @Override
    public boolean performEditorAction(int editorAction) {
        // This isn't currently necessary for unit testing. It can be implemented later if a test
        // case using this is added.
        return false;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        // currently unused - no need to implement the fake version
        return false;
    }

    @Override
    public boolean beginBatchEdit() {
        handleEditStart();
        mBatchLevel++;
        return true;
    }

    @Override
    public boolean endBatchEdit() {
        if (mBatchLevel < 1) {
            throw new RuntimeException("batch level is " + mBatchLevel);
        }
        mBatchLevel--;
        return handleEditEnd();
    }

    private void handleEditStart() {
        if (mBatchLevel == 0) {
            mEditInitialText = mText.toString();
            Log.w("Fake", "handleEditStart: initialText=\"" + mEditInitialText + "\"");
            mEditInitialCursorStart = mCurrentCursorStart;
            mEditInitialCursorEnd = mCurrentCursorEnd;
            mEditInitialComposingStart = mCurrentComposingStart;
            mEditInitialComposingEnd = mCurrentComposingEnd;
        }
    }

    private boolean handleEditEnd() {
        if (mBatchLevel > 0) {
            return true;
        }

        //TODO: maybe add a configuration option to test both calling onUpdateExtractedText before and after onUpdateSelection
        if (mSettings.updateSelectionAfterExtractedText) {
            // basic EditText calls onUpdateExtractedText before onUpdateSelection
            updateExtractedText();
            updateSelection();
        } else {
            updateSelection();
            updateExtractedText();
        }
        // process the updates together to simulate the asynchronous nature of separate processes to
        // prevent responses to one update impacting the other update
        mManager.processUpdates();

        return false;
    }

    private void updateExtractedText() {
        Log.w("Fake", "updateExtractedText: allowExtractedTextMonitor=" + mSettings.allowExtractedTextMonitor);
        if (!mSettings.allowExtractedTextMonitor) {
            Log.w("Fake", "updateExtractedText: skipping update due to functionality being disabled");
            return;
        }
        if (mExtractedTextMonitorRequests.size() == 0) {
            return;
        }

        if (!mSettings.sendExtractedTextUpdateBasedOnNetTextChange) {
            //TODO: (EW) implement to support sending updates based on modification action positions rather than net text change
            throw new RuntimeException("Not implemented yet");
        }

        //TODO: (EW) mEditInitialText should be a Spannable so we can verify if spans changed to factor in the changed range
        int startMatchLength = 0;
        for (int i = 0; i < Math.min(mText.length(), mEditInitialText.length()); i++) {
            if (mText.charAt(i) == mEditInitialText.charAt(i)) {
                startMatchLength++;
            } else {
                break;
            }
        }
        int endMatchLength = 0;
        for (int i = 1; i <= Math.min(mText.length(), mEditInitialText.length()); i++) {
            if (mText.charAt(mText.length() - i) == mEditInitialText.charAt(mEditInitialText.length() - i)) {
                endMatchLength++;
            } else {
                break;
            }
        }
        Log.w("Fake", "updateExtractedText: initialText=\"" + mEditInitialText
                + "\", updatedText=\"" + mText + "\", startMatchLength=" + startMatchLength
                + ", endMatchLength=" + endMatchLength);

        if (startMatchLength == endMatchLength && endMatchLength == mEditInitialText.length()
                && mEditInitialText.length() == mText.length()) {
            Log.w("Fake", "updateExtractedText: skipping update due to unchanged text");
            if (!mEditInitialText.equals(mText.toString())) {
                throw new RuntimeException("text not actually equal");
            }
            return;
        }

        for (final ExtractedTextRequest extractedTextRequest : mExtractedTextMonitorRequests.values()) {
            final ExtractedText extractedText = new ExtractedText();

            if (mSettings.partialTextMonitorUpdates) {
                final int initialTextMatchOverlap =
                        startMatchLength - (mEditInitialText.length() - endMatchLength);
                final int newTextMatchOverlap =
                        startMatchLength - (mText.length() - endMatchLength);
                if (initialTextMatchOverlap > 0 || newTextMatchOverlap > 0) {
                    if (initialTextMatchOverlap == newTextMatchOverlap) {
                        // I think this means they are the same full text - verify that
                        //TODO: (EW) if this is in fact due to no change, this shouldn't be able to
                        // happen anymore due to the early exit, so this case could be removed
                        if (!mText.toString().equals(mEditInitialText)) {
                            //TODO: can this happen? what to do?
                            throw new RuntimeException("initialText=\"" + mEditInitialText
                                    + "\", updatedText=\"" + mText + "\"");
                        }
                        //TODO: no change - maybe skip (default tracks where the specific
                        // modification actions occur rather than the net difference - maybe do that
                        // too)
                        continue;
                    } else if (initialTextMatchOverlap > newTextMatchOverlap) {
                        extractedText.text = mText.subSequence(
                                mEditInitialText.length() - endMatchLength,
                                mText.length() - (mEditInitialText.length() - startMatchLength));
                        extractedText.partialStartOffset =
                                mEditInitialText.length() - endMatchLength;
                        extractedText.partialEndOffset = startMatchLength;
                    } else {
                        extractedText.text = mText.subSequence(
                                mText.length() - endMatchLength,
                                startMatchLength);
                        extractedText.partialStartOffset = mText.length() - endMatchLength;
                        extractedText.partialEndOffset =
                                mEditInitialText.length() - (mText.length() - startMatchLength);
                    }
                } else {
                    extractedText.text =
                            mText.subSequence(startMatchLength, mText.length() - endMatchLength);
                    extractedText.partialStartOffset = startMatchLength;
                    extractedText.partialEndOffset = mEditInitialText.length() - endMatchLength;
                }
                extractedText.startOffset = 0;
                extractedText.selectionStart = mCurrentCursorStart;
                extractedText.selectionEnd = mCurrentCursorEnd;
            } else {
                // ignoring extractedTextRequest.hintMaxLines because it's not necessary for the
                // current tests and it would just complicate things for no reason. handling for it
                // can be added later if a test requires it.

                //TODO: add an option to send minimal updates (only the changing text)

                // documentation doesn't clearly indicate what part of the text should be returned
                // when it is limited. The existence of startOffset at least implies that
                // selectionStart (and probably selectionEnd) should be within the range of the
                // returned text, so we'll aim to return the requested length of text centered
                // around the cursor (and extending it if necessary to include both the selection
                // start and end).
                //TODO: if the change is outside of the range of text to return, should we skip sending the update?
                final int maxRequestedChars = Math.min(extractedTextRequest.hintMaxChars,
                        mSettings.extractMonitorTextLimit);
                final int center = (mCurrentCursorStart + mCurrentCursorEnd) / 2;
                //TODO: these names aren't clear
                final int maxLeft = Math.min(mCurrentCursorStart, startMatchLength);
                final int minRight = Math.max(mCurrentCursorEnd, mText.length() - endMatchLength);
                int left;
                int right;
                //TODO: make sure to prevent out of bounds errors
                if (minRight - maxLeft >= maxRequestedChars) {
                    // we're returning more than the requested (or intentionally limited) characters
                    // to ensure both the cursor position (which might be a requirement) and the
                    // text change (to allow the test to verify the text cache matches) are in the
                    // returned text
                    left = maxLeft;
                    right = minRight;
                } else if (maxLeft >= center - maxRequestedChars / 2 && minRight <= center + maxRequestedChars / 2) {
                    left = center - maxRequestedChars / 2;
                    right = center + maxRequestedChars / 2;
                } else {
                    final int remainingChars = maxRequestedChars - (minRight - maxLeft);
                    // extend the shorter side (trying to center around the cursor position) to
                    // match the requested number of characters
                    if (center - maxLeft > minRight - center) {
                        left = maxLeft;
                        right = minRight + remainingChars;
                    } else {
                        left = maxLeft + remainingChars;
                        right = minRight;
                    }
                }
                // fix positions if the centered range extends past the bounds of the text
                //TODO: see if this can be simplified
                if (left < 0) {
                    right += -left;
                    left = 0;
                }
                if (right > mText.length()) {
                    left -= right - mText.length();
                    right = mText.length();
                }
                if (left < 0) {
                    left = 0;
                }

                extractedText.text = mText.subSequence(left, right);
                extractedText.partialStartOffset = -1;
                extractedText.partialEndOffset = -1;
                extractedText.startOffset = left;
                extractedText.selectionStart = mCurrentCursorStart - extractedText.startOffset;
                extractedText.selectionEnd = mCurrentCursorEnd - extractedText.startOffset;
            }

            mManager.updateExtractedText(null, extractedTextRequest.token, extractedText);
        }
    }

    private void updateSelection() {
        if (mSettings.sendSelectionUpdateWhenNotChanged
                || mEditInitialCursorStart != mCurrentCursorStart
                || mEditInitialCursorEnd != mCurrentCursorEnd
                || mEditInitialComposingStart != mCurrentComposingStart
                || mEditInitialComposingEnd != mCurrentComposingEnd) {
            mManager.updateSelection(mEditInitialCursorStart, mEditInitialCursorEnd,
                    mCurrentCursorStart, mCurrentCursorEnd,
                    mCurrentComposingStart, mCurrentComposingEnd);
        } else {
            Log.w("Fake", "updateSelection: skipping update due to unchanged positions");
        }
    }

    public void forceUpdateSelectionCall() {
        mManager.updateSelection(mCurrentCursorStart, mCurrentCursorEnd,
                mCurrentCursorStart, mCurrentCursorEnd,
                mCurrentComposingStart, mCurrentComposingEnd);
    }

    @Override
    public boolean sendKeyEvent(KeyEvent keyEvent) {
        //TODO: verify that this should trigger an update on its own (documentation doesn't call out either way)
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            handleEditStart();
            // Currently this only gets called for KeyEvent.KEYCODE_DPAD_LEFT,
            // KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DEL, and
            // KeyEvent.KEYCODE_0 - KeyEvent.KEYCODE_9, so that's all we'll handle here for now.
            final int codePoint;
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (mCurrentCursorStart < mCurrentCursorEnd) {
                        mCurrentCursorEnd = mCurrentCursorStart;
                    } else {
                        final int codePointBeforeCursor =
                                Character.codePointBefore(mText, mCurrentCursorStart);
                        final int lengthToMove =
                                Character.isSupplementaryCodePoint(codePointBeforeCursor) ? 2 : 1;
                        mCurrentCursorStart -= lengthToMove;
                        mCurrentCursorEnd -= lengthToMove;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (mCurrentCursorStart < mCurrentCursorEnd) {
                        mCurrentCursorStart = mCurrentCursorEnd;
                    } else {
                        final int codePointAtCursor =
                                Character.codePointAt(mText, mCurrentCursorStart);
                        final int lengthToMove =
                                Character.isSupplementaryCodePoint(codePointAtCursor) ? 2 : 1;
                        mCurrentCursorStart += lengthToMove;
                        mCurrentCursorEnd += lengthToMove;
                    }
                    break;
                case KeyEvent.KEYCODE_DEL:
                    if (mCurrentCursorStart < mCurrentCursorEnd) {
                        deleteText(mCurrentCursorStart, mCurrentCursorEnd);
                    } else if (mCurrentCursorStart > 0) {
                        final int codePointBeforeCursor =
                                Character.codePointBefore(mText, mCurrentCursorStart);
                        final int lengthToDelete =
                                Character.isSupplementaryCodePoint(codePointBeforeCursor) ? 2 : 1;
                        deleteText(mCurrentCursorStart - lengthToDelete, mCurrentCursorStart);
                    }
                    break;
                case KeyEvent.KEYCODE_0:
                case KeyEvent.KEYCODE_1:
                case KeyEvent.KEYCODE_2:
                case KeyEvent.KEYCODE_3:
                case KeyEvent.KEYCODE_4:
                case KeyEvent.KEYCODE_5:
                case KeyEvent.KEYCODE_6:
                case KeyEvent.KEYCODE_7:
                case KeyEvent.KEYCODE_8:
                case KeyEvent.KEYCODE_9:
                    // keyEvent = codePoint - '0' + KeyEvent.KEYCODE_0
                    codePoint = '0' + keyEvent.getKeyCode() - KeyEvent.KEYCODE_0;
                    Log.w("Fake", getDebugState());
                    setText(modifyText(new StringBuilder().appendCodePoint(codePoint), true),
                            mCurrentCursorStart, mCurrentCursorEnd);
                    mCurrentCursorStart = mCurrentCursorEnd;
                    Log.w("Fake", getDebugState());
                    break;
                case KeyEvent.KEYCODE_A:
                case KeyEvent.KEYCODE_B:
                case KeyEvent.KEYCODE_C:
                case KeyEvent.KEYCODE_D:
                case KeyEvent.KEYCODE_E:
                case KeyEvent.KEYCODE_F:
                case KeyEvent.KEYCODE_G:
                case KeyEvent.KEYCODE_H:
                case KeyEvent.KEYCODE_I:
                case KeyEvent.KEYCODE_J:
                case KeyEvent.KEYCODE_K:
                case KeyEvent.KEYCODE_L:
                case KeyEvent.KEYCODE_M:
                case KeyEvent.KEYCODE_N:
                case KeyEvent.KEYCODE_O:
                case KeyEvent.KEYCODE_P:
                case KeyEvent.KEYCODE_Q:
                case KeyEvent.KEYCODE_R:
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_T:
                case KeyEvent.KEYCODE_U:
                case KeyEvent.KEYCODE_V:
                case KeyEvent.KEYCODE_W:
                case KeyEvent.KEYCODE_X:
                case KeyEvent.KEYCODE_Y:
                case KeyEvent.KEYCODE_Z:
                    codePoint = 'a' + keyEvent.getKeyCode() - KeyEvent.KEYCODE_A;
                    Log.w("Fake", getDebugState());
                    setText(modifyText(new StringBuilder().appendCodePoint(codePoint), true),
                            mCurrentCursorStart, mCurrentCursorEnd);
                    mCurrentCursorStart = mCurrentCursorEnd;
                    Log.w("Fake", getDebugState());
                    break;
            }
            handleEditEnd();
        }
        return true;
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        // currently unused - no need to implement the fake version
        return false;
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        // currently unused - no need to implement the fake version
        return false;
    }

    @Override
    public boolean performPrivateCommand(String s, Bundle bundle) {
        // currently unused - no need to implement the fake version
        return false;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        // currently unused - no need to implement the fake version
        return false;
    }

    @Override
    public Handler getHandler() {
        // currently unused - no need to implement the fake version
        return null;
    }

    @Override
    public void closeConnection() {
        // currently unused - no need to implement the fake version
    }

    @Override
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
        // currently unused - no need to implement the fake version
        return false;
    }

    public String getText() {
        return mText.toString();
    }

    public String getComposingText() {
        if (!hasComposingText()) {
            return null;
        }
        return mText.subSequence(mCurrentComposingStart, mCurrentComposingEnd).toString();
    }

    public int getSelectionStart() {
        return mCurrentCursorStart;
    }

    public int getSelectionEnd() {
        return mCurrentCursorEnd;
    }

    public int getCompositionStart() {
        return mCurrentComposingStart;
    }

    public int getCompositionEnd() {
        return mCurrentComposingEnd;
    }

    public interface FakeInputMethodManager {
        void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                             int candidatesStart, int candidatesEnd);
        void updateExtractedText (View view, int token, ExtractedText text);
        void processUpdates();
    }

    public void resetCalls() {
        mGetTextBeforeCursorCalls.clear();
        mGetSelectedTextCalls.clear();
        mGetTextAfterCursorCalls.clear();
        mGetExtractedTextCalls.clear();
    }

    public GetTextCall[] getGetTextBeforeCursorCalls() {
        return mGetTextBeforeCursorCalls.toArray(new GetTextCall[0]);
    }

    public GetSelectedTextCall[] getGetSelectedTextCalls() {
        return mGetSelectedTextCalls.toArray(new GetSelectedTextCall[0]);
    }

    public GetTextCall[] getGetTextAfterCursorCalls() {
        return mGetTextAfterCursorCalls.toArray(new GetTextCall[0]);
    }

    public GetExtractedTextCall[] getGetExtractedTextCalls() {
        return mGetExtractedTextCalls.toArray(new GetExtractedTextCall[0]);
    }

    public static class GetTextCall {
        final int n;
        final int flags;
        public GetTextCall(final int n, final int flags) {
            this.n = n;
            this.flags = flags;
        }
    }

    public static class GetSelectedTextCall {
        final int flags;
        public GetSelectedTextCall(final int flags) {
            this.flags = flags;
        }
    }

    public static class GetExtractedTextCall {
        public final ExtractedTextRequest extractedTextRequest;
        public final int flags;
        public GetExtractedTextCall(ExtractedTextRequest extractedTextRequest, int flags) {
            this.extractedTextRequest = extractedTextRequest;
            this.flags = flags;
        }
    }

    public interface TextModifier {
        CharSequence modifyComposingText(CharSequence text);
        CharSequence modifyCommittingText(CharSequence text);
    }

    public static class DoubleTextModifier extends RepeatTextModifier {
        public DoubleTextModifier() {
            super(2);
        }

        @Override
        public String toString() {
            return "DoubleTextModifier";
        }
    }

    public static class RepeatTextModifier implements TextModifier {
        final int count;
        public RepeatTextModifier(int count) {
            this.count = count;
        }

        public CharSequence modifyComposingText(CharSequence text) {
            return repeatText(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return repeatText(text);
        }

        public CharSequence repeatText(CharSequence text) {
            StringBuilder sb = new StringBuilder();
            for (int codePoint : text.codePoints().toArray()) {
                for (int i = 0; i < count; i++) {
                    sb.appendCodePoint(codePoint);
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "RepeatTextModifier(" + count + ")";
        }
    }

    public static class HalveTextModifier implements TextModifier {
        public CharSequence modifyComposingText(CharSequence text) {
            return halveText(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return halveText(text);
        }

        public static CharSequence halveText(CharSequence text) {
            StringBuilder sb = new StringBuilder();
            boolean keep = false;
            for (int codePoint : text.codePoints().toArray()) {
                if (keep) {
                    sb.appendCodePoint(codePoint);
                }
                keep = !keep;
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "HalveTextModifier";
        }
    }

    public static class FlipTextModifier implements TextModifier {
        public CharSequence modifyComposingText(CharSequence text) {
            return changeText(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return changeText(text);
        }

        public static CharSequence changeText(CharSequence text) {
            StringBuilder sb = new StringBuilder();
            int[] codePoints = text.codePoints().toArray();
            for (int i = codePoints.length - 1; i >= 0; i--) {
                int codePoint = codePoints[i];
                if (codePoints.length == 1) {
                    sb.appendCodePoint(codePoint + 1);
                } else {
                    sb.appendCodePoint(codePoint);
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "FlipTextModifier";
        }
    }

    public static class DoubleCapsTextModifier implements TextModifier {
        public CharSequence modifyComposingText(CharSequence text) {
            return doubleCaps(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return doubleCaps(text);
        }

        public static CharSequence doubleCaps(CharSequence text) {
            StringBuilder sb = new StringBuilder();
            for (int codePoint : text.codePoints().toArray()) {
                sb.appendCodePoint(codePoint);
                if (codePoint >= 'A' && codePoint <= 'Z') {
                    sb.appendCodePoint(codePoint);
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "DoubleCapsTextModifier";
        }
    }

    public static class IncrementNumberTextModifier implements TextModifier {
        public CharSequence modifyComposingText(CharSequence text) {
            return incrementNumbers(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return incrementNumbers(text);
        }

        public static CharSequence incrementNumbers(CharSequence text) {
            StringBuilder sb = new StringBuilder();
            for (int codePoint : text.codePoints().toArray()) {
                if (codePoint >= '0' && codePoint <= '8') {
                    sb.appendCodePoint(codePoint + 1);
                } else if (codePoint == '9') {
                    sb.appendCodePoint('0');
                } else {
                    sb.appendCodePoint(codePoint);
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "IncrementNumberTextModifier";
        }
    }

    public static class AlterSpecificTextModifier implements TextModifier {
        private final CharSequence original;
        private final CharSequence update;
        private final boolean onlyWholeText;
        public AlterSpecificTextModifier(CharSequence original, CharSequence update, boolean onlyWholeText) {
            this.original = original;
            this.update = update;
            this.onlyWholeText = onlyWholeText;
        }
        public CharSequence modifyComposingText(CharSequence text) {
            return alterText(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return alterText(text);
        }

        public CharSequence alterText(CharSequence text) {
            if (onlyWholeText) {
                if (text != null && text.toString().equals(original.toString())) {
                    return update;
                }
                return text;
            }
            SpannableStringBuilder sb = new SpannableStringBuilder();
            for (int i = 0; i + original.length() <= text.length(); i++) {
                if (text.subSequence(i, i + original.length()).toString().equals(original.toString())) {
                    sb.append(update);
                    i += original.length() - 1;
                } else {
                    sb.append(text.subSequence(i, i + 1));
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "AlterSpecificTextModifier(\"" + original + "\", \"" + update + "\", "
                    + onlyWholeText + ")";
        }
    }

    public static class ExtraTextTextModifier implements TextModifier {
        final CharSequence extraText;
        public ExtraTextTextModifier(final CharSequence extraText) {
            this.extraText = extraText;
        }

        public CharSequence modifyComposingText(CharSequence text) {
            return addExtraText(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return addExtraText(text);
        }

        public CharSequence addExtraText(CharSequence text) {
            StringBuilder sb = new StringBuilder();
            sb.append(text);
            sb.append(extraText);
            return sb;
        }

        @Override
        public String toString() {
            return "ExtraTextTextModifier(" + extraText + ")";
        }
    }

    public static class BlockCharactersTextModifier implements TextModifier {
        final char[] charsToBlock;
        public BlockCharactersTextModifier(final char[] charsToBlock) {
            this.charsToBlock = charsToBlock;
        }

        public CharSequence modifyComposingText(CharSequence text) {
            return blockCharacters(text);
        }

        public CharSequence modifyCommittingText(CharSequence text) {
            return blockCharacters(text);
        }

        public CharSequence blockCharacters(CharSequence text) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char curChar = text.charAt(i);
                boolean blockChar = false;
                for (char charToBlock : charsToBlock) {
                    if (curChar == charToBlock) {
                        blockChar = true;
                        break;
                    }
                }
                if (blockChar) {
                    continue;
                }
                sb.append(curChar);
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "BlockCharactersTextModifier(" + Arrays.toString(charsToBlock) + ")";
        }
    }
}
