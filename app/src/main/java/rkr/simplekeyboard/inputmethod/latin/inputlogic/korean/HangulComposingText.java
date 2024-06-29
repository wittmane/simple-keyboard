/*
 * Copyright (C) 2024 Eli Wittman
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rkr.simplekeyboard.inputmethod.latin.inputlogic.korean;

import android.text.TextUtils;
import android.util.Log;

import rkr.simplekeyboard.inputmethod.latin.inputlogic.ComposingText;

import static rkr.simplekeyboard.inputmethod.latin.inputlogic.korean.HangulCharacters.HANGUL_LETTER_ARAEA;

/**
 * Manager for editing Korean character blocks.
 */
public class HangulComposingText implements ComposingText {
    private static final String TAG = HangulComposingText.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int NO_CODE_POINT = -1;

    /** The text for the word (series of adjacent hangul syllables or letters) that isn't actively
     *  being modified. */
    private String mCompletedCharacters;
    /** Hangul character block that is actively being modified. */
    private HangulSyllable mSyllable;
    /** Temporary character that doesn't fit in the current character block but might if modified
     *  (see {@link HangulCharacters#tryCycleChar(int, int)}, so it shouldn't be moved to a separate
     *  block yet. */
    private int mCarryOverCodePoint = NO_CODE_POINT;
    /** Settings to indicate how the Hangul syllable blocks can be built. */
    private final HangulPreferences mPrefs;

    /**
     * Create a new manager for editing Hangul character blocks.
     * This overload should only be called internally or by unit tests for dependency injection.
     * @param prefs settings that should be used to determine how the syllable blocks can be built.
     */
    public HangulComposingText(HangulPreferences prefs) {
        mPrefs = prefs;
        clear();
    }

    /**
     * Create a new manager for editing Hangul character blocks.
     */
    public HangulComposingText() {
        this(HangulPreferences.getInstance());
    }

    /**
     * Remove all of the text that is being edited.
     */
    public void clear() {
        mCompletedCharacters = "";
        mSyllable = new HangulSyllable(mPrefs);
        mCarryOverCodePoint = NO_CODE_POINT;
    }

    /**
     * Specify the keyboard layout set in use in order to control how some input should be handled.
     * @param layoutSetName the name of the keyboard layout set that is being used.
     */
    public void setKeyboardLayout(final String layoutSetName) {
        mPrefs.setKeyboardLayout(layoutSetName);
    }

    /**
     * Check if there is a temporary code point being held before completing the current block and
     * starting a new one.
     * @return whether there is a carry-over code point.
     */
    private boolean hasCarryOverCodePoint() {
        return mCarryOverCodePoint != NO_CODE_POINT;
    }

    //TODO: (EW) reconsider how fully composing vowels work - now that the whole word is kept as
    // editing backspacing is inconsistent. when working with the current syllable, the lines/dots
    // are backspaced individually, but when backspacing further into a previous syllable, the
    // standard vowels are deleted as a whole rather than broken up into lines/dots even though they
    // were probably composed that way.
    // This is also an issue with other jamo that can be formed in multiple ways (eg: double
    // consonants, e vowels, iotized vowels). For these we could keep track of how the previous
    // syllables in the composing text were formed to backspace the same way they were entered, but
    // that might still be weird/annoying to backspace all of the components multiple syllables back

    //TODO: (EW) update documentation
    /**
     * Add a consonant or vowel (or a component of one) to the composing text. This will add it to
     * the current syllable block if it fits or start a new syllable block if it doesn't fit.
     * @param codePoint the new Hangul jamo character to add.
     * @param isDoubleClick whether the code point was added as a double click, which could allow
     *                      the last added code point to be modified instead of adding this code
     *                      point separately.
     * @return the completed text if the entered code point requires starting a new syllable block.
     */
    public String addCodePoint(final int codePoint, final boolean isDoubleClick) {
        if (!HangulCharacters.isHangulInputLetter(codePoint, mPrefs)) {
            if (DEBUG) {
                Log.d(TAG, "Add non-jamo: current=" + toDebugString() + ", codePoint="
                        + (char)codePoint + "(" + codePoint + ")");
            }
            final String result = new StringBuilder()
                    .append(toString())
                    .appendCodePoint(codePoint)
                    .toString();
            clear();
            return result;
        }

        if (DEBUG) {
            Log.d(TAG, "Add jamo: current=" + toDebugString() + ", codePoint="
                    + (char)codePoint + "(" + codePoint + "), isDoubleClick=" + isDoubleClick);
        }

        // see if the last jamo should be cycled instead of added as a separate jamo or combination
        if (cycleJamo(codePoint, isDoubleClick)) {
            if (DEBUG) {
                Log.d(TAG, "Cycled input: " + toDebugString());
            }
            return "";
        }

        String result = "";
        if (hasCarryOverCodePoint()) {
            result += mSyllable;
            // since a character is being added, not cycled, the carry-over code point can be added
            // to a new working syllable (since it doesn't fit in the current working syllable) or
            // simply returned as completed text if it doesn't fit in a new syllable
            mSyllable = new HangulSyllable(mPrefs);
            if (!addJamoCodePoint(mSyllable, mCarryOverCodePoint)) {
                result += (char)mCarryOverCodePoint;
            }
            mCarryOverCodePoint = NO_CODE_POINT;
        }

        // add the code point to the current syllable if possible
        if (!addJamoCodePoint(mSyllable, codePoint)) {
            if ((mPrefs.canCycleInputLetter()) && mSyllable.anyCycleOptionFits(codePoint)) {
                // keep track of a single character that doesn't fit in the syllable in case it
                // gets cycled and then is able to fit
                mCarryOverCodePoint = codePoint;
            } else {
                // since the code point doesn't fit in the current syllable and a carry-over isn't
                // valid, a new syllable is necessary
                HangulSyllable newSyllable = new HangulSyllable(mPrefs);
                if (mSyllable.getJamoSize() == 3
                        && HangulCharacters.isValidInputJamo(1, codePoint, mPrefs)) {
                    // shift the last consonant on the current syllable to the new one for the new
                    // vowel
                    newSyllable.addJamo(mSyllable.popJamoPiece());
                    newSyllable.addJamo(codePoint);
                    result += mSyllable.toString();
                } else {
                    result += mSyllable.toString();
                    if (!newSyllable.addJamo(codePoint)) {
                        // return the new code point as completed text since it doesn't fit in the
                        // current syllable and isn't valid to start the new syllable
                        result += (char) codePoint;
                    }
                }
                mSyllable = newSyllable;
            }
        }
        mCompletedCharacters += result;
        if (DEBUG) {
            Log.d(TAG, "Jamo added: " + toDebugString());
        }
        return "";
    }

    /**
     * Add a string to the composing text.
     * @param text a string to add.
     * @return the completed text if the entered text requires starting a new word.
     */
    public String addString(final String text) {
        //TODO: (EW) this should be able to support entering a string of only valid Korean
        // characters
        final String result = toString() + text;
        clear();
        return result;
    }

    public boolean updateState(final String text) {
        if (TextUtils.isEmpty(text)) {
            clear();
            return true;
        }
        final String currentText = toString();
        if (text.length() <= currentText.length() && currentText.startsWith(text)) {
            //TODO: (EW) this can probably be done more efficiently
            while (!toString().equals(text)) {
                backspace();
            }
            return true;
        }
        //TODO: (EW) this still needs to be tested
        for (int i = 0; i < text.codePointCount(0, text.length()); i++) {
            final int codePoint = text.codePointAt(i);
            if (!HangulCharacters.isHangulInputLetter(codePoint, mPrefs)
                    && !HangulCharacters.isSyllable(codePoint)) {
                return false;
            }
        }
        clear();
        mCompletedCharacters = text;
        trySetLastCodePointAsCurrentSyllable(NO_CODE_POINT);
        return true;
    }

    /**
     * Try using the new input to cycle the last entered code point.
     * @param codePoint the new Hangul jamo character to add.
     * @param isDoubleClick whether the code point was added as a double click, which could allow
     *                      the last added code point to be modified instead of adding this code
     *                      point separately.
     * @return whether the last input was cycled
     */
    private boolean cycleJamo(final int codePoint, final boolean isDoubleClick) {
        if (!mPrefs.canCycleInputLetter() || mSyllable.getJamoSize() == 0) {
            return false;
        }
        if (isDoubleClick) {
            int cycledLetter = HangulCharacters.tryCycleChar(hasCarryOverCodePoint()
                    ? mCarryOverCodePoint : mSyllable.peekJamoPiece(), codePoint);
            if (cycledLetter > 0) {
                if (!hasCarryOverCodePoint()) {
                    mSyllable.popJamoPiece();
                }
                if (addJamoCodePoint(mSyllable, cycledLetter)) {
                    mCarryOverCodePoint = NO_CODE_POINT;
                } else {
                    mCarryOverCodePoint = cycledLetter;
                }
                return true;
            }
        }

        //TODO: consider merging the dots into a regular vowel
        // this would make backspacing remove whole vowel rather than breaking up dots
        // this is probably more consistent with the way cycled extra dots don't all need to be backspaced through
        // this is basically how gboard behaves, so that might mean this is what most users expect
        // similar to the backspacing through built syllables, users might find extra backspacing annoying

        // try cycling 1 and 2 vowel dots instead of just adding extra dots that don't build a
        // valid character
        if (codePoint == HANGUL_LETTER_ARAEA && !hasCarryOverCodePoint()
                && mPrefs.fullyComposeVowels()
                && HangulCharacters.canCycleFullyComposedVowel(mSyllable.peekJamo())) {
            mSyllable.popJamoPiece();
            return true;
        }

        return false;
    }

    /**
     * Try to add a consonant or vowel (or a component of one) to a syllable. This will add it as a
     * combination to the last letter in the syllable if possible, and if not, it will just add it
     * as a separate consonant/vowel if possible.
     * @param syllable the Hangul syllable to attempt to update.
     * @param codePoint the jamo (Korean consonant/vowel) (or a component of one) to add to the
     *                  syllable.
     * @return whether the code point was added to the syllable.
     */
    private boolean addJamoCodePoint(final HangulSyllable syllable, final int codePoint) {
        if (syllable.addJamoPiece(codePoint)) {
            return true;
        }
        return syllable.addJamo(codePoint);
    }

    /**
     * Remove the last consonant or vowel (or a component of one) from the syllable.
     */
    public void backspace() {
        int result;

        // remove the last input piece
        if (hasCarryOverCodePoint()) {
            result = mCarryOverCodePoint;
            mCarryOverCodePoint = NO_CODE_POINT;
        } else if (mSyllable.getJamoSize() > 0) {
            result = mSyllable.popJamoPiece();
        } else {
            final int lastCodePoint = getLastCompletedCodePoint();
            removeLastCompletedCodePoint();

            if (HangulCharacters.isSyllable(lastCodePoint)) {
                mSyllable = new HangulSyllable(lastCodePoint, mPrefs);
                result = mSyllable.popJamoPiece();
            } else {
                result = lastCodePoint;
            }
        }

        // update the current syllable to use the previous syllable if necessary
        if (!mCompletedCharacters.isEmpty()) {
            if (mSyllable.getJamoSize() == 0) {
                trySetLastCodePointAsCurrentSyllable(NO_CODE_POINT);
            } else if (mSyllable.getJamoSize() == 1 && mSyllable.getJamo(0).size() == 1) {
                trySetLastCodePointAsCurrentSyllable(mSyllable.getJamo(0).get(0));
            }
        }
    }

    /**
     * Take the last code point from {@link #mCompletedCharacters} and set it as {@link #mSyllable}
     * if the code point is a valid syllable or start of a syllable. If a jamo code point is passed,
     * the previous syllable will only be set as current if the jamo can be added to it.
     * @param jamoToAppend a code point for a jamo to that needs to be able to be added to the
     *                    previous syllable. This can be set to {@link #NO_CODE_POINT} to just use
     *                    the previous syllable.
     */
    private void trySetLastCodePointAsCurrentSyllable(final int jamoToAppend) {
        final HangulSyllable syllable;
        final int lastCodePoint = getLastCompletedCodePoint();
        if (HangulCharacters.isSyllable(lastCodePoint)) {
            syllable = new HangulSyllable(lastCodePoint, mPrefs);
        } else {
            syllable = new HangulSyllable(mPrefs);
            if (!syllable.addJamo(lastCodePoint)) {
                // the previous code point isn't an initial consonant, so there is nothing to do
                return;
            }
        }
        if (jamoToAppend != NO_CODE_POINT && !addJamoCodePoint(syllable, jamoToAppend)) {
            // the syllables can't be combined, so there is nothing to do
            return;
        }

        mSyllable = syllable;

        // remove the last code point to not duplicate it
        removeLastCompletedCodePoint();
    }

    /**
     * Get the most recently added code point from {@link #mCompletedCharacters}.
     * @return the code point found right before the current syllable.
     */
    private int getLastCompletedCodePoint() {
        final int codePointCount =
                mCompletedCharacters.codePointCount(0, mCompletedCharacters.length());
        return mCompletedCharacters.codePointAt(codePointCount - 1);
    }

    /**
     * Remove the most recently added code point from {@link #mCompletedCharacters}.
     */
    private void removeLastCompletedCodePoint() {
        final int codePointCount =
                mCompletedCharacters.codePointCount(0, mCompletedCharacters.length());
        final int offset = mCompletedCharacters.offsetByCodePoints(0, codePointCount - 1);
        mCompletedCharacters = mCompletedCharacters.substring(0, offset);
    }

    /**
     * Check if the text is empty.
     * @return whether there is no text.
     */
    public boolean isEmpty() {
        // the carry-over code point doesn't need to be checked because it should only be populated
        // when the syllable is partially filled
        return mSyllable.getJamoSize() == 0 && mCompletedCharacters.isEmpty();
    }

    /**
     * Returns a string representation of the composing text.
     * @return a string representation of the composing text.
     */
    @Override
    public String toString() {
        String result = mCompletedCharacters + mSyllable.toString();
        if (hasCarryOverCodePoint()) {
            result += (char)mCarryOverCodePoint;
        }
        return result;
    }

    /**
     * Get a string that represents the internal composition of the text.
     * @return a debug string representing the composing text.
     */
    public String toDebugString() {
        String result = "";
        if (!mCompletedCharacters.isEmpty()) {
            result += mCompletedCharacters + "+";
        }
        result += mSyllable.toDebugString();
        if (hasCarryOverCodePoint()) {
            result += "+" + (char)mCarryOverCodePoint;
        }
        return result;
    }
}
