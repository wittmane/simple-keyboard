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

import java.util.Arrays;
import java.util.EmptyStackException;

import static rkr.simplekeyboard.inputmethod.latin.inputlogic.korean.HangulCharacters.FULLWIDTH_COLON;
import static rkr.simplekeyboard.inputmethod.latin.inputlogic.korean.HangulCharacters.HANGUL_LETTER_ARAEA;
import static rkr.simplekeyboard.inputmethod.latin.inputlogic.korean.HangulCharacters.HANGUL_SYLLABLE_BLOCK_END;
import static rkr.simplekeyboard.inputmethod.latin.inputlogic.korean.HangulCharacters.HANGUL_SYLLABLE_BLOCK_START;

/**
 * This is a data structure of code points that combine to form a Korean syllable block. A Hangul
 * syllable is composed of up to 3 jamo (a leading consonant, a vowel, and a trailing consonant).
 * These jamo each consist of one or more code point to build them. For example, ㅎ + ㅏ + ㄴ = 한.
 */
public class HangulSyllable {
    /** The flat array of code points used to form the syllable.
     *  This has a max of 9 because leading/trailing consonants each have a max of 2 components and
     *  vowels have a max of 5 when fully composing. */
    private final int[] mComponents = new int[9];
    /** The array of consonants and vowel that form the syllable. Check
     *  {@link #mJamoSize} to get the current number of jamo used in this syllable. */
    private final Jamo[] mJamo =  new Jamo[3];
    /** The number of jamo ({@link #mJamo}) currently used in this syllable. */
    private int mJamoSize;
    /** The settings used to determine what combinations can be used to form jamo. */
    private final HangulPreferences mPrefs;

    /**
     * This represents a leading consonant, vowel, or trailing consonant used to form a
     * {@link HangulSyllable}. This is formed from one or more component code points.
     */
    public class Jamo {
        private static final int NO_COMPONENTS_END_INDEX = -1;
        /** The index in {@link #mJamo} that this object exists */
        private final int mIndex;
        /** The index in {@link #mComponents} where the last component used to form this jamo
         *  exists. This is negative if there are no components to ensure that it will less
         *  than the previous jamo's end index to avoid incorrectly indicating that this has
         *  components. */
        private int mEndIndex = NO_COMPONENTS_END_INDEX;

        /**
         * Create a new jamo.
         * This constructor is private so it can only be built from {@link HangulSyllable} because
         * it's directly tied to the instance and the other jamo objects in
         * {@link #mJamo}.
         * @param jamoIndex the index of the jamo within the Hangul syllable.
         */
        private Jamo(final int jamoIndex) {
            mIndex = jamoIndex;
        }

        /**
         * Get the index where the first component for the jamo should be.
         * @return the first component's index.
         */
        private int getStartIndex() {
            return mIndex > 0 ? mJamo[mIndex - 1].mEndIndex + 1 : 0;
        }

        /**
         * Get the number of components that form the jamo.
         * @return the number of components in the jamo.
         */
        public int size() {
            return Math.max(0, mEndIndex + 1 - getStartIndex());
        }

        /**
         * Get the code point for the jamo built from the components.
         * @return The Hangul compatibility (U+3130 to U+318F) code point for the jamo or
         *         {@link HangulCharacters#NO_CODE_POINT} if the jamo is empty.
         */
        public int getCodePoint() {
            // the combination was validated on input, so this doesn't need validation with the
            // preferences
            return HangulCharacters.getCombination(this, mIndex);
        }

        /**
         * Get the component for the jamo at a specific index.
         * @param pieceIndex the index of the component to get.
         * @return the component for the jamo at the specified index.
         */
        public int get(final int pieceIndex) {
            final int offset = getStartIndex();
            final int index = offset + pieceIndex;
            if (index > mEndIndex || index < 0) {
                throw new IndexOutOfBoundsException("Index " + pieceIndex
                        + " out of bounds for length " + size());
            }
            return mComponents[offset + pieceIndex];
        }

        /**
         * Add a new component to the jamo.
         * This doesn't do any validation, but it's expected that the caller will do the validation
         * since this is private. Other methods assume that the components are validated, so if
         * invalid code points are added, other things will behave incorrectly.
         * @param codePoint the code point to add to the jamo.
         * @return whether the component was successfully added.
         */
        private boolean push(final int codePoint) {
            final boolean addingJamo = size() == 0;
            final int nextIndex = getComponentSize();
            if (nextIndex >= mComponents.length) {
                // an unexpected number of components was attempted to be added
                return false;
            }
            mComponents[nextIndex] = codePoint;
            mEndIndex = nextIndex;
            if (addingJamo) {
                mJamoSize++;
            }
            return true;
        }

        /**
         * Remove the last component of the jamo.
         * @return the component that was removed.
         */
        private int pop() {
            if (size() == 0) {
                // nothing to remove
                throw new EmptyStackException();
            }
            int piece = mComponents[mEndIndex];
            mComponents[mEndIndex] = 0;
            mEndIndex--;
            if (size() == 0) {
                mJamoSize--;
                mEndIndex = NO_COMPONENTS_END_INDEX;
            }
            return piece;
        }

        /**
         * Get the last component of the jamo.
         * @return the last component of the jamo.
         */
        private int peek() {
            if (size() == 0) {
                // nothing to remove
                throw new EmptyStackException();
            }
            return mComponents[mEndIndex];
        }
    }

    /**
     * Create a blank Hangul syllable.
     * @param prefs the settings used to determine what combinations can be used to form jamo.
     */
    public HangulSyllable(final HangulPreferences prefs) {
        mPrefs = prefs;
    }

    /**
     * Create a fully formed Hangul syllable.
     * @param syllableCodePoint the Hangul syllable code point to use to populate the jamo. This
     *                          needs to be a Hangul syllable (U+AC00 to U+D7AF).
     * @param prefs the settings used to determine what combinations can be used to form jamo.
     */
    public HangulSyllable(final int syllableCodePoint, final HangulPreferences prefs) {
        if (!HangulCharacters.isSyllable(syllableCodePoint)) {
            throw new IllegalArgumentException("The code point " + syllableCodePoint + " ("
                    + syllableCodePoint + ") is not a valid Hangul syllable ("
                    + HANGUL_SYLLABLE_BLOCK_START + " to " + HANGUL_SYLLABLE_BLOCK_END + ")");
        }

        mPrefs = prefs;

        // break up the syllable into separate jamo code points
        final int[] positionalJamoCodePointList =
                HangulCharacters.decomposeSyllable(syllableCodePoint);
        if (positionalJamoCodePointList == null) {
            throw new IllegalArgumentException("Invalid syllable code point: "
                    + (char)syllableCodePoint + "(" + syllableCodePoint + ")");
        }
        for (int positionalJamoCodePoint : positionalJamoCodePointList) {
            final int jamoCodePoint =
                    HangulCharacters.positionalToCompatibilityJamo(positionalJamoCodePoint);
            // check if this jamo should be split into multiple components
            final int[] jamoPieces = HangulCharacters.getCompositeJamoPieces(jamoCodePoint);
            if (jamoPieces != null && jamoPieces.length > 0) {
                // add all of the components for the jamo
                for (int i = 0; i < jamoPieces.length; i++) {
                    // only the first component creates the jamo. the others should be added to the
                    // same jamo.
                    push(jamoPieces[i], i == 0);
                }
            } else {
                // add the new jamo
                push(jamoCodePoint, true);
            }
        }
    }

    /**
     * Get the total number of consonants and vowels in the syllable. This will at most be 3
     * (leading consonant, vowel, and trailing consonant). Components of each jamo aren't counted.
     * @return the number of jamo in the syllable.
     */
    public int getJamoSize() {
        return mJamoSize;
    }

    /**
     * Get the total number of components used to build the whole syllable (all of the jamos).
     * @return the number of components in the syllable.
     */
    private int getComponentSize() {
        if (mJamoSize == 0) {
            return 0;
        }
        return mJamo[mJamoSize - 1].mEndIndex + 1;
    }

    /**
     * Get the jamo at a specific index.
     * @param index the index of the jamo to return.
     * @return the jamo at the specified index.
     */
    public Jamo getJamo(final int index) {
        if (index < 0 || index > mJamoSize) {
            return null;
        }
        return mJamo[index];
    }

    /**
     * Add a code point to the last jamo in the syllable. If there are no jamos, it will be added to
     * a new one. This will fail if the code point can't be added to the jamo to create a valid
     * Hangul syllable.
     * @param codePoint the Hangul compatibility jamo (U+3130 to U+318F) code point to add.
     * @return whether the code point was successfully added to the jamo.
     */
    public boolean addJamoPiece(final int codePoint) {
        if (mJamoSize == 0) {
            return addJamo(codePoint);
        }
        final int jamoIndex = mJamoSize - 1;
        final Jamo curGroup = mJamo[jamoIndex];
        // validate whether the code point can be added as a component to the jamo
        final int combinedJamo =
                HangulCharacters.getCombination(curGroup, codePoint, jamoIndex, mPrefs);
        if (combinedJamo < 0
                || !HangulCharacters.isValidInputJamo(jamoIndex, combinedJamo, mPrefs)) {
            return false;
        }
        return push(codePoint, false);
    }

    /**
     * Add a code point as a new jamo at the end of the syllable. This will fail if the new jamo
     * doesn't create a valid Hangul syllable.
     * @param codePoint the Hangul compatibility jamo (U+3130 to U+318F) code point for the jamo.
     * @return whether the jamo was successfully added to the syllable.
     */
    public boolean addJamo(final int codePoint) {
        if (mJamoSize > 2 || !HangulCharacters.isValidInputJamo(mJamoSize, codePoint, mPrefs)) {
            return false;
        }
        // only allow an end consonant if the vowel is complete
        if (mJamoSize == 2) {
            if (!isVowelComplete()) {
                return false;
            }
        }
        return push(codePoint, true);
    }

    /**
     * Check if the vowel is complete (ie: not just dots) and can be used to build a
     * pre-composed Hangul syllable.
     * @return whether the vowel is complete.
     */
    private boolean isVowelComplete() {
        if (mJamoSize != 2) {
            return false;
        }
        final int pieceCount = mJamo[1].size();
        return mJamo[1].get(0) != HANGUL_LETTER_ARAEA || (pieceCount != 1
                && (pieceCount != 2 || mJamo[1].get(1) != HANGUL_LETTER_ARAEA));
    }

    /**
     * Add a jamo component to the syllable.
     * This doesn't do any validation, but it's expected that the caller will do the validation
     * since this is private. Other methods assume that the components are validated, so if invalid
     * code points are added, other things will behave incorrectly.
     * @param codePoint the code point to add.
     * @param newJamo true if this should create a new jamo, false if the code point should be added
     *                as a component of the last jamo.
     * @return whether the code point was successfully added.
     */
    private boolean push(final int codePoint, final boolean newJamo) {
        final int jamoIndex;
        if (newJamo) {
            jamoIndex = mJamoSize;
            if (jamoIndex >= mJamo.length) {
                // an unexpected number of jamos was attempted to be added
                return false;
            }

            if (mJamo[jamoIndex] == null) {
                mJamo[jamoIndex] = new Jamo(jamoIndex);
            }
        } else {
            jamoIndex = mJamoSize - 1;
        }

        return mJamo[jamoIndex].push(codePoint);
    }

    /**
     * Remove the last component from the last jamo. This will reduce the number of jamo in the
     * syllable if all of the components are removed from the last jamo.
     * @return the code point that was removed.
     */
    public int popJamoPiece() {
        if (mJamoSize < 1) {
            // nothing to remove
            throw new EmptyStackException();
        }
        return mJamo[mJamoSize - 1].pop();
    }

    /**
     * Get the last component of the last jamo.
     * @return the last component of the last jamo.
     */
    public int peekJamoPiece() {
        if (mJamoSize == 0) {
            // nothing left
            throw new EmptyStackException();
        }
        return mJamo[mJamoSize - 1].peek();
    }

    /**
     * Get the last jamo of the syllable.
     * @return the last jamo of the syllable.
     */
    public Jamo peekJamo() {
        if (mJamoSize < 1) {
            throw new EmptyStackException();
        }
        return mJamo[mJamoSize - 1];
    }

    /**
     * Check if any of the options that can be cycled through from pressing a key multiple times is
     * capable of being added to form a valid syllable.
     * @param codePoint the code point that is entered and is used as a key to look up the options
     *                  that can be cycled through.
     * @return whether there are any options that can be used to form a valid syllable.
     */
    public boolean anyCycleOptionFits(final int codePoint) {
        if (mJamoSize == 0) {
            // validate codePoint is a leading consonant jamo
            return HangulCharacters.isValidInputJamo(0, codePoint, mPrefs);
        }
        if (mJamoSize == 1) {
            if (!mPrefs.buildDoubleConsonants() || mJamo[0].size() > 1) {
                return false;
            }
            // validate codePoint cycle options contains the one jamo and it isn't already a double
            return HangulCharacters.cycleOptionCanCombine(mJamo[0].get(0), 0, codePoint, mPrefs);
        }
        if (mJamoSize == 2) {
            // all cycle options have at least one that can be used as a final jamo
            return isVowelComplete() && HangulCharacters.isValidInputJamo(2, codePoint, mPrefs);
        }
        if (mJamo[2].size() > 1) {
            // there can't be more than 2 components to the final jamo
            return false;
        }
        // validate that codePoint cycle options contains at least one jamo that can combine with
        // the current trailing consonant
        return HangulCharacters.cycleOptionCanCombine(mJamo[2].get(0), 2, codePoint, mPrefs);
    }

    /**
     * Returns a string representation of the syllable.
     * @return a string representation of the syllable.
     */
    @Override
    public String toString() {
        int[] finalJamo = new int[mJamoSize];
        for (int i = 0; i < finalJamo.length; i++) {
            Jamo jamo = mJamo[i];
            int combination;
            if (jamo == null) {
                combination = 0;
            } else {
                combination = jamo.getCodePoint();
            }
            if (combination < 0) {
                throw new RuntimeException("The jamo '" + Arrays.toString(finalJamo)
                        + "' is not valid (" + toDebugString() + ")");
            }
            finalJamo[i] = combination;
        }
        if (finalJamo.length == 0) {
            return "";
        }
        if (finalJamo.length == 1) {
            return String.valueOf((char)finalJamo[0]);
        }
        if (finalJamo.length == 2 && (finalJamo[1] == HANGUL_LETTER_ARAEA
                || finalJamo[1] == FULLWIDTH_COLON)) {
            return String.valueOf((char)finalJamo[0]) + (char) finalJamo[1];
        }
        for (int i = 0; i < finalJamo.length; i++) {
            finalJamo[i] = HangulCharacters.compatibilityToPositionalJamo(finalJamo[i], i);
        }
        return "" + (char)HangulCharacters.composeSyllable(finalJamo);
    }

    /**
     * Get a string that represents the internal composition of the syllable.
     * @return a debug string representing the working text.
     */
    public String toDebugString() {
        final StringBuilder result = new StringBuilder();
        result.append("[");
        int componentIndex = 0;
        for (int jamoIndex = 0; jamoIndex < mJamoSize; jamoIndex++) {
            while (componentIndex <= mJamo[jamoIndex].mEndIndex) {
                result.append((char) mComponents[componentIndex++]);
            }
            if (jamoIndex + 1 < mJamoSize) {
                result.append(",");
            }
        }
        result.append("]");
        return result.toString();
    }
}
