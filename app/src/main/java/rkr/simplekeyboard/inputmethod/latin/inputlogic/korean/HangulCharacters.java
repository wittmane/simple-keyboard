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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HangulCharacters {
    public static final int NO_CODE_POINT = -1;

    // Hangul Compatibility Jamo (U+3130 to U+318F) (94 codepoints)
    // These are used for building combinations and entered from the keyboard because these are not
    // linked to the position of the jamo (leading/trailing consonant).
    /** Hangul compatibility jamo ㄱ */
    public static final int HANGUL_LETTER_KIYEOK = 0x3131;
    /** Hangul compatibility jamo ㄲ */
    public static final int HANGUL_LETTER_SSANGKIYEOK = 0x3132;
    /** Hangul compatibility jamo ㄳ */
    public static final int HANGUL_LETTER_KIYEOK_SIOS = 0x3133;
    /** Hangul compatibility jamo ㄴ */
    public static final int HANGUL_LETTER_NIEUN = 0x3134;
    /** Hangul compatibility jamo ㄵ */
    public static final int HANGUL_LETTER_NIEUN_CIEUC = 0x3135;
    /** Hangul compatibility jamo ㄶ */
    public static final int HANGUL_LETTER_NIEUN_HIEUH = 0x3136;
    /** Hangul compatibility jamo ㄷ */
    public static final int HANGUL_LETTER_TIKEUT = 0x3137;
    /** Hangul compatibility jamo ㄸ */
    public static final int HANGUL_LETTER_SSANGTIKEUT = 0x3138;
    /** Hangul compatibility jamo ㄹ */
    public static final int HANGUL_LETTER_RIEUL = 0x3139;
    /** Hangul compatibility jamo ㄺ */
    public static final int HANGUL_LETTER_RIEUL_KIYEOK = 0x313A;
    /** Hangul compatibility jamo ㄻ */
    public static final int HANGUL_LETTER_RIEUL_MIEUM = 0x313B;
    /** Hangul compatibility jamo ㄼ */
    public static final int HANGUL_LETTER_RIEUL_PIEUP = 0x313C;
    /** Hangul compatibility jamo ㄽ */
    public static final int HANGUL_LETTER_RIEUL_SIOS = 0x313D;
    /** Hangul compatibility jamo ㄾ */
    public static final int HANGUL_LETTER_RIEUL_THIEUTH = 0x313E;
    /** Hangul compatibility jamo ㄿ */
    public static final int HANGUL_LETTER_RIEUL_PHIEUPH = 0x313F;
    /** Hangul compatibility jamo ㅀ */
    public static final int HANGUL_LETTER_RIEUL_HIEUH = 0x3140;
    /** Hangul compatibility jamo ㅁ */
    public static final int HANGUL_LETTER_MIEUM = 0x3141;
    /** Hangul compatibility jamo ㅂ */
    public static final int HANGUL_LETTER_PIEUP = 0x3142;
    /** Hangul compatibility jamo ㅃ */
    public static final int HANGUL_LETTER_SSANGPIEUP = 0x3143;
    /** Hangul compatibility jamo ㅄ */
    public static final int HANGUL_LETTER_PIEUP_SIOS = 0x3144;
    /** Hangul compatibility jamo ㅅ */
    public static final int HANGUL_LETTER_SIOS = 0x3145;
    /** Hangul compatibility jamo ㅆ */
    public static final int HANGUL_LETTER_SSANGSIOS = 0x3146;
    /** Hangul compatibility jamo ㅇ */
    public static final int HANGUL_LETTER_IEUNG = 0x3147;
    /** Hangul compatibility jamo ㅈ */
    public static final int HANGUL_LETTER_CIEUC = 0x3148;
    /** Hangul compatibility jamo ㅉ */
    public static final int HANGUL_LETTER_SSANGCIEUC = 0x3149;
    /** Hangul compatibility jamo ㅊ */
    public static final int HANGUL_LETTER_CHIEUCH = 0x314A;
    /** Hangul compatibility jamo ㅋ */
    public static final int HANGUL_LETTER_KHIEUKH = 0x314B;
    /** Hangul compatibility jamo ㅌ */
    public static final int HANGUL_LETTER_THIEUTH = 0x314C;
    /** Hangul compatibility jamo ㅍ */
    public static final int HANGUL_LETTER_PHIEUPH = 0x314D;
    /** Hangul compatibility jamo ㅎ */
    public static final int HANGUL_LETTER_HIEUH = 0x314E;
    /** Hangul compatibility jamo ㅏ */
    public static final int HANGUL_LETTER_A = 0x314F;
    /** Hangul compatibility jamo ㅐ */
    public static final int HANGUL_LETTER_AE = 0x3150;
    /** Hangul compatibility jamo ㅑ */
    public static final int HANGUL_LETTER_YA = 0x3151;
    /** Hangul compatibility jamo ㅒ */
    public static final int HANGUL_LETTER_YAE = 0x3152;
    /** Hangul compatibility jamo ㅓ */
    public static final int HANGUL_LETTER_EO = 0x3153;
    /** Hangul compatibility jamo ㅔ */
    public static final int HANGUL_LETTER_E = 0x3154;
    /** Hangul compatibility jamo ㅕ */
    public static final int HANGUL_LETTER_YEO = 0x3155;
    /** Hangul compatibility jamo ㅖ */
    public static final int HANGUL_LETTER_YE = 0x3156;
    /** Hangul compatibility jamo ㅗ */
    public static final int HANGUL_LETTER_O = 0x3157;
    /** Hangul compatibility jamo ㅘ */
    public static final int HANGUL_LETTER_WA = 0x3158;
    /** Hangul compatibility jamo ㅙ */
    public static final int HANGUL_LETTER_WAE = 0x3159;
    /** Hangul compatibility jamo ㅚ */
    public static final int HANGUL_LETTER_OE = 0x315A;
    /** Hangul compatibility jamo ㅛ */
    public static final int HANGUL_LETTER_YO = 0x315B;
    /** Hangul compatibility jamo ㅜ */
    public static final int HANGUL_LETTER_U = 0x315C;
    /** Hangul compatibility jamo ㅝ */
    public static final int HANGUL_LETTER_WEO = 0x315D;
    /** Hangul compatibility jamo ㅞ */
    public static final int HANGUL_LETTER_WE = 0x315E;
    /** Hangul compatibility jamo ㅟ */
    public static final int HANGUL_LETTER_WI = 0x315F;
    /** Hangul compatibility jamo ㅠ */
    public static final int HANGUL_LETTER_YU = 0x3160;
    /** Hangul compatibility jamo ㅡ */
    public static final int HANGUL_LETTER_EU = 0x3161;
    /** Hangul compatibility jamo ㅢ */
    public static final int HANGUL_LETTER_YI = 0x3162;
    /** Hangul compatibility jamo ㅣ */
    public static final int HANGUL_LETTER_I = 0x3163;
    /** Hangul compatibility jamo ㆍ */
    public static final int HANGUL_LETTER_ARAEA = 0x318D;

    /** Colon (：) to use to represent 2 Hangul Araea (ㆍ) characters when fully composing vowels */
    public static final int FULLWIDTH_COLON = 0xFF1A; //：

    // Hangul Jamo (U+1100 to U+11FF) (256 codepoints)
    // These are used for constructing and deconstructing the syllable characters because they
    // directly map to the pre-composed Hangul syllables using a simple algorithm.
    // Leading consonants (CHOSEONG) (U+1100 to U+1112)
    /** Hangul leading consonant ᄀ */
    public static final int HANGUL_CHOSEONG_KIYEOK = 0x1100;
    /** Hangul leading consonant ᄂ */
    public static final int HANGUL_CHOSEONG_NIEUN = 0x1102;
    /** Hangul leading consonant ᄃ */
    public static final int HANGUL_CHOSEONG_TIKEUT = 0x1103;
    /** Hangul leading consonant ᄆ */
    public static final int HANGUL_CHOSEONG_MIEUM = 0x1106;
    /** Hangul leading consonant ᄉ */
    public static final int HANGUL_CHOSEONG_SIOS = 0x1109;
    /** Hangul leading consonant ᄒ */
    public static final int HANGUL_CHOSEONG_HIEUH = 0x1112;
    // Vowels (JUNGSEONG) (U+1161 to U+1175)
    /** Hangul vowel ᅡ */
    public static final int HANGUL_JUNGSEONG_A = 0x1161;
    /** Hangul vowel ᅵ */
    public static final int HANGUL_JUNGSEONG_I = 0x1175;
    // Trailing consonants (JONGSEONG) (U+11A8 to U+11C2)
    /** Hangul trailing consonant ᆨ */
    public static final int HANGUL_JONGSEONG_KIYEOK = 0x11A8;
    /** Hangul trailing consonant ᆯ */
    public static final int HANGUL_JONGSEONG_RIEUL = 0x11AF;
    /** Hangul trailing consonant ᆹ */
    public static final int HANGUL_JONGSEONG_PIEUP_SIOS = 0x11B9;
    /** Hangul trailing consonant ᆾ */
    public static final int HANGUL_JONGSEONG_CHIEUCH = 0x11BE;
    /** Hangul trailing consonant ᇂ */
    public static final int HANGUL_JONGSEONG_HIEUH = 0x11C2;

    // Hangul Syllables (U+AC00 to U+D7AF) (11172 codepoints)
    /** Hangul Syllable 가 */
    public static int HANGUL_SYLLABLE_BLOCK_START = 0xAC00; //가
    /** Hangul Syllable 힣 */
    public static int HANGUL_SYLLABLE_BLOCK_END = 0xD7A3; //힣

    // Hangul Jamo Extended-A (U+A960 to U+A97F) (29 codepoints)

    // Hangul Jamo Extended-B (U+D7B0 to U+D7FF) (72 codepoints)

    // Enclosed CJK Letters and Months (U+3200 to U+32FF) (254 codepoints)

    // Halfwidth and Fullwidth Forms (U+FF00 to U+FFEF) (225 codepoints)
    // Korean (U+FFA0 to U+FFDC) (52 codepoints)

    // Constants for the composition/decomposition algorithm
    /** The base value for the pre-composed Hangul syllables for use in the
     *  composition/decomposition algorithm */
    private static final int SYLLABLE_BASE =  HANGUL_SYLLABLE_BLOCK_START;
    /** The base value for the leading consonants for use in composing/decomposing pre-composed
     *  Hangul syllables */
    private static final int LEADING_CONSONANT_BASE =  HANGUL_CHOSEONG_KIYEOK;
    /** The base value for the medial vowels for use in composing/decomposing pre-composed Hangul
     *  syllables */
    private static final int VOWEL_BASE =  HANGUL_JUNGSEONG_A;
    /** The base value for the medial vowels for use in composing/decomposing pre-composed Hangul
     *  syllables. This is one less than the beginning of the range of trailing consonants, which
     *  starts at U+11A8, to effectively include a blank value as the first trailing consonant. */
    private static final int TRAILING_CONSONANT_BASE = HANGUL_JONGSEONG_KIYEOK - 1;
    /** The number of leading consonants that can be used in pre-composed Hangul syllables */
    private static final int LEADING_CONSONANT_COUNT =
            HANGUL_CHOSEONG_HIEUH - HANGUL_CHOSEONG_KIYEOK + 1;
    /** The number of medial vowels that can be used in pre-composed Hangul syllables */
    private static final int VOWEL_COUNT =  HANGUL_JUNGSEONG_I - HANGUL_JUNGSEONG_A + 1;
    /** One more than the number of trailing consonants that can be used in pre-composed Hangul
     *  syllables. This effectively includes a blank trailing consonant in the number to provide a
     *  count for how many syllables can be formed from any given leading consonant and vowel
     *  pair. */
    private static final int TRAILING_CONSONANT_COUNT =
            HANGUL_JONGSEONG_HIEUH - HANGUL_JONGSEONG_KIYEOK + 2;
    /** The number of pre-composed Hangul syllables starting with the same leading consonant,
     *  counting both the syllables with and without trailing consonants for each possible trailing
     *  consonant */
    private static final int SYLLABLES_PER_LEADING_CONSONANT_COUNT =
            VOWEL_COUNT * TRAILING_CONSONANT_COUNT;
    /** The total number of pre-composed Hangul syllables */
    private static final int SYLLABLE_COUNT =
            LEADING_CONSONANT_COUNT * SYLLABLES_PER_LEADING_CONSONANT_COUNT;

    /**
     * Check if a code point is a general Hangul letter used for input. This is looking for Hangul
     * Compatibility Jamo (U+3130 to U+318F) (although not all of them).
     * @param codePoint the code point to check.
     * @param prefs the preferences to determine what code points are usable.
     * @return whether the entered code point is a Hangul input letter.
     */
    public static boolean isHangulInputLetter(final int codePoint, final HangulPreferences prefs) {
        return (codePoint >= HANGUL_LETTER_KIYEOK && codePoint <= HANGUL_LETTER_I)
                || (codePoint == HANGUL_LETTER_ARAEA && prefs.fullyComposeVowels());
    }

    /**
     * Check if the code point is a Hangul syllable (U+AC00 to U+D7AF)
     * @param codePoint the code point to check.
     * @return whether the code point is a Hangul syllable
     */
    public static boolean isSyllable(final int codePoint) {
        return codePoint >= HANGUL_SYLLABLE_BLOCK_START && codePoint <= HANGUL_SYLLABLE_BLOCK_END;
    }

    /**
     * Break up a Hangul syllable code point into an array of Hangul jamo code points.
     * @param syllable the Hangul syllable (U+AC00 to U+D7AF) to decompose.
     * @return an array of Hangul jamo (U+1100 to U+11FF) code points that compose the syllable.
     */
    public static int[] decomposeSyllable(final int syllable) {
        if (!isSyllable(syllable)) {
            return null;
        }

        // the Hangul syllable code points are grouped first by the leading consonant, then by the
        // vowel, and finally by the trailing consonant, so the syllable index can be divided to
        // determine the index of the consonants and vowel
        final int syllableIndex = syllable - SYLLABLE_BASE;
        final int leadingConsonantIndex = syllableIndex / SYLLABLES_PER_LEADING_CONSONANT_COUNT;
        final int vowelIndex =
                (syllableIndex % SYLLABLES_PER_LEADING_CONSONANT_COUNT) / TRAILING_CONSONANT_COUNT;
        final int trailingConsonantIndex = syllableIndex % TRAILING_CONSONANT_COUNT;

        final boolean hasTrailingConsonant = trailingConsonantIndex > 0;

        final int[] jamoSequence = new int[hasTrailingConsonant ? 3 : 2];

        // use the indices to build the list of jamo code points
        jamoSequence[0] = LEADING_CONSONANT_BASE + leadingConsonantIndex;
        jamoSequence[1] = VOWEL_BASE + vowelIndex;
        if (hasTrailingConsonant) {
            jamoSequence[2] = TRAILING_CONSONANT_BASE + trailingConsonantIndex;
        }

        return jamoSequence;
    }

    /**
     * Build a Hangul syllable code point from an array of Hangul jamo code points.
     * @param jamoSequence an array of Hangul jamo (U+1100 to U+11FF) code points that compose a
     *                     syllable.
     * @return the composed Hangul syllable (U+AC00 to U+D7AF).
     */
    public static int composeSyllable(final int[] jamoSequence) {
        // validate the input can build a pre-composed Hangul syllable
        if (jamoSequence.length < 2 || jamoSequence.length > 3
                || jamoSequence[0] < HANGUL_CHOSEONG_KIYEOK
                || jamoSequence[0] > HANGUL_CHOSEONG_HIEUH
                || jamoSequence[1] < HANGUL_JUNGSEONG_A
                || jamoSequence[1] > HANGUL_JUNGSEONG_I
                || (jamoSequence.length == 3
                && (jamoSequence[2] < HANGUL_JONGSEONG_KIYEOK
                || jamoSequence[2] > HANGUL_JONGSEONG_HIEUH))) {
            return NO_CODE_POINT;
        }

        // determine the index of each consonant and vowel (a blank trailing consonant is treated as
        // the first index)
        final int leadingConsonantIndex = jamoSequence[0] - LEADING_CONSONANT_BASE;
        final int vowelIndex = jamoSequence[1] - VOWEL_BASE;
        final int trailingConsonantIndex;
        if (jamoSequence.length == 3) {
            trailingConsonantIndex = jamoSequence[2] - TRAILING_CONSONANT_BASE;
        } else {
            trailingConsonantIndex = 0;
        }

        // the Hangul syllable code points are grouped first by the leading consonant, then by the
        // vowel, and finally by the trailing consonant, so the syllable can be built by adding the
        // indices multiplied by how many adjacent code points share the same consonant/vowel
        return SYLLABLE_BASE
                + leadingConsonantIndex * SYLLABLES_PER_LEADING_CONSONANT_COUNT
                + vowelIndex * TRAILING_CONSONANT_COUNT
                + trailingConsonantIndex;
    }

    /** Information to map Hangul Compatibility Jamo (U+3130 to U+318F) to Hangul Jamo (U+1100 to
     *  U+11FF). Rather than pairing each character individually, sets of adjacent code points in
     *  one list can be grouped to correspond with the same sized set of adjacent code points in the
     *  other list. The innermost array has 3 items: the compatibility code point for the starting
     *  point of the group, the corresponding positional code point for the starting point of the
     *  group, and the size of the group. The middle array holds the list of the groups for a
     *  consonant/vowel, and they are ordered based on the groups' starting point to support a
     *  binary search. The outermost array has 3 inner arrays for mapping the leading consonants,
     *  vowels, and trailing consonants. */
    private static final int[][][] sCompatibilityPositionalConvertInfo = new int[][][] {
            new int[][]{
                    new int[]{ HANGUL_LETTER_KIYEOK, HANGUL_CHOSEONG_KIYEOK, 2 }, // ㄱ - ㄲ
                    new int[]{ HANGUL_LETTER_NIEUN, HANGUL_CHOSEONG_NIEUN, 1 }, // ㄴ
                    new int[]{ HANGUL_LETTER_TIKEUT, HANGUL_CHOSEONG_TIKEUT, 3 }, // ㄷ - ㄹ
                    new int[]{ HANGUL_LETTER_MIEUM, HANGUL_CHOSEONG_MIEUM, 3 }, // ㅁ - ㅃ
                    new int[]{ HANGUL_LETTER_SIOS, HANGUL_CHOSEONG_SIOS, 10 } // ㅅ - ㅎ
            },
            new int[][]{
                    new int[]{ HANGUL_LETTER_A, HANGUL_JUNGSEONG_A, VOWEL_COUNT } // ㅏ - ㅣ
            },
            new int[][]{
                    new int[]{ HANGUL_LETTER_KIYEOK, HANGUL_JONGSEONG_KIYEOK, 7 }, // ㄱ - ㄷ
                    new int[]{ HANGUL_LETTER_RIEUL, HANGUL_JONGSEONG_RIEUL, 10 }, // ㄹ - ㅂ
                    new int[]{ HANGUL_LETTER_PIEUP_SIOS, HANGUL_JONGSEONG_PIEUP_SIOS, 5 }, // ㅄ - ㅈ
                    new int[]{ HANGUL_LETTER_CHIEUCH, HANGUL_JONGSEONG_CHIEUCH, 5 } // ㅊ - ㅎ
            }
    };

    /**
     * Convert a positional jamo (U+1100 to U+11FF) code point to or from a compatibility jamo
     * (U+3130 to U+318F). Note that not all of those values can be converted. It only works for
     * jamo that are used for building the pre-composed Hangul syllables.
     * @param codePoint the code point to convert.
     * @param jamoIndex the index of the syllable's jamo where the code point is used.
     * @param toPositional true to convert from compatibility to positional jamo, false to convert
     *                     from positional to compatibility jamo.
     * @return the converted code point.
     */
    private static int convertJamo(final int codePoint, final int jamoIndex,
                                   final boolean toPositional) {
        final int curFormatIndex;
        final int newFormatIndex;
        if (toPositional) {
            curFormatIndex = 0;
            newFormatIndex = 1;
        } else {
            curFormatIndex = 1;
            newFormatIndex = 0;
        }
        // get the list of mapping groups for the consonant/vowel
        final int[][] convertInfo = sCompatibilityPositionalConvertInfo[jamoIndex];

        // do a binary search for the group that contains the code point
        int min = 0;
        int max = convertInfo.length - 1;
        int index;
        while (min <= max) {
            // select the middle group
            index = (max + 1 - min) / 2 + min;

            final int groupStart = convertInfo[index][curFormatIndex];
            final int groupEnd = groupStart + convertInfo[index][2] - 1;

            if (codePoint < groupStart) {
                // set the previous group as the last group that might contain the code point
                max = index - 1;
            } else if (codePoint > groupEnd) {
                // set the next group as the first group that might contain the code point
                min = index + 1;
            } else {
                // get the position in the group
                final int offset = codePoint - groupStart;
                // get the corresponding code point in the related group
                return convertInfo[index][newFormatIndex] + offset;
            }
        }

        // the code point isn't in any of the groups, so it can't be converted
        return -1;
    }

    /**
     * Convert a compatibility jamo (U+3130 to U+318F) to a positional jamo (U+1100 to U+11FF) code
     * point. Note that this only actually works for U+3130 to U+3163.
     * @param codePoint the compatibility jamo code point to convert.
     * @param jamoIndex the index of the syllable's jamo where the code point is used.
     * @return the converted positional jamo code point.
     */
    public static int compatibilityToPositionalJamo(final int codePoint, final int jamoIndex) {
        return convertJamo(codePoint, jamoIndex, true);
    }

    /**
     * Convert a positional jamo (U+1100 to U+11FF) to a compatibility jamo (U+3130 to U+318F) code
     * point. Note that this only actually works for U+1100 to U+1112 (leading consonants),
     * U+1161 to U+1175 (vowels), and U+11A8 to U+11C2 (trailing consonants).
     * @param codePoint the positional jamo code point to convert.
     * @return the converted compatibility jamo code point.
     */
    public static int positionalToCompatibilityJamo(final int codePoint) {
        final int jamoIndex;
        if (codePoint >= HANGUL_CHOSEONG_KIYEOK && codePoint <= HANGUL_CHOSEONG_HIEUH) {
            jamoIndex = 0;
        } else if (codePoint >= HANGUL_JUNGSEONG_A && codePoint <= HANGUL_JUNGSEONG_I) {
            jamoIndex = 1;
        } else if (codePoint >= HANGUL_JONGSEONG_KIYEOK && codePoint <= HANGUL_JONGSEONG_HIEUH) {
            jamoIndex = 2;
        } else {
            return -1;
        }
        return convertJamo(codePoint, jamoIndex, false);
    }

    private static final int PREF_REQ_NONE = 0;
    private static final int PREF_REQ_E = 1;
    private static final int PREF_REQ_IOTIZE = 2;
    private static final int PREF_REQ_FULLY_COMPOSE = 3;
    private static final int PREF_REQ_DOUBLE_CONSONANT = 4;

    /** Map of jamo to a pair of jamos that combine to form it. These are combinations that are
     *  always valid (not dependent on user settings). */
    private static final Map<Integer, IntPair> sJamoToStdCombinationMap;
    /** Map of a pair of jamo code points to the jamo code point that they combine to form and the
     *  setting flag to determine when the combination should be allowed. */
    private static final Map<IntPair, IntPair> sCombinationToJamoMap;
    static {
        Map<Integer, IntPair> jamoToStdCombinationMap = new HashMap<>();
        // standard consonant combinations
        jamoToStdCombinationMap.put(HANGUL_LETTER_KIYEOK_SIOS,
                new IntPair(HANGUL_LETTER_KIYEOK, HANGUL_LETTER_SIOS)); // ㄱ + ㅅ = ㄳ
        jamoToStdCombinationMap.put(HANGUL_LETTER_NIEUN_CIEUC,
                new IntPair(HANGUL_LETTER_NIEUN, HANGUL_LETTER_CIEUC)); // ㄴ + ㅈ = ㄵ
        jamoToStdCombinationMap.put(HANGUL_LETTER_NIEUN_HIEUH,
                new IntPair(HANGUL_LETTER_NIEUN, HANGUL_LETTER_HIEUH)); // ㄴ + ㅎ = ㄶ
        jamoToStdCombinationMap.put(HANGUL_LETTER_RIEUL_KIYEOK,
                new IntPair(HANGUL_LETTER_RIEUL, HANGUL_LETTER_KIYEOK)); // ㄹ + ㄱ = ㄺ
        jamoToStdCombinationMap.put(HANGUL_LETTER_RIEUL_MIEUM,
                new IntPair(HANGUL_LETTER_RIEUL, HANGUL_LETTER_MIEUM)); // ㄹ + ㅁ = ㄻ
        jamoToStdCombinationMap.put(HANGUL_LETTER_RIEUL_PIEUP,
                new IntPair(HANGUL_LETTER_RIEUL, HANGUL_LETTER_PIEUP)); // ㄹ + ㅂ = ㄼ
        jamoToStdCombinationMap.put(HANGUL_LETTER_RIEUL_SIOS,
                new IntPair(HANGUL_LETTER_RIEUL, HANGUL_LETTER_SIOS)); // ㄹ + ㅅ = ㄽ
        jamoToStdCombinationMap.put(HANGUL_LETTER_RIEUL_THIEUTH,
                new IntPair(HANGUL_LETTER_RIEUL, HANGUL_LETTER_THIEUTH)); // ㄹ + ㅌ = ㄾ
        jamoToStdCombinationMap.put(HANGUL_LETTER_RIEUL_PHIEUPH,
                new IntPair(HANGUL_LETTER_RIEUL, HANGUL_LETTER_PHIEUPH)); // ㄹ + ㅍ = ㄿ
        jamoToStdCombinationMap.put(HANGUL_LETTER_RIEUL_HIEUH,
                new IntPair(HANGUL_LETTER_RIEUL, HANGUL_LETTER_HIEUH)); // ㄹ + ㅎ = ㅀ
        jamoToStdCombinationMap.put(HANGUL_LETTER_PIEUP_SIOS,
                new IntPair(HANGUL_LETTER_PIEUP, HANGUL_LETTER_SIOS)); // ㅂ + ㅅ = ㅄ

        // standard vowel combinations
        jamoToStdCombinationMap.put(HANGUL_LETTER_WA,
                new IntPair(HANGUL_LETTER_O, HANGUL_LETTER_A)); // ㅗ + ㅏ = ㅘ
        jamoToStdCombinationMap.put(HANGUL_LETTER_WAE,
                new IntPair(HANGUL_LETTER_O, HANGUL_LETTER_AE)); // ㅗ + ㅐ = ㅙ
        jamoToStdCombinationMap.put(HANGUL_LETTER_OE,
                new IntPair(HANGUL_LETTER_O, HANGUL_LETTER_I)); // ㅗ + ㅣ = ㅚ
        jamoToStdCombinationMap.put(HANGUL_LETTER_WEO,
                new IntPair(HANGUL_LETTER_U, HANGUL_LETTER_EO)); // ㅜ + ㅓ = ㅝ
        jamoToStdCombinationMap.put(HANGUL_LETTER_WE,
                new IntPair(HANGUL_LETTER_U, HANGUL_LETTER_E)); // ㅜ + ㅔ = ㅞ
        jamoToStdCombinationMap.put(HANGUL_LETTER_WI,
                new IntPair(HANGUL_LETTER_U, HANGUL_LETTER_I)); // ㅜ + ㅣ = ㅟ
        jamoToStdCombinationMap.put(HANGUL_LETTER_YI,
                new IntPair(HANGUL_LETTER_EU, HANGUL_LETTER_I)); // ㅡ + ㅣ = ㅢ

        sJamoToStdCombinationMap = Collections.unmodifiableMap(jamoToStdCombinationMap);

        Map<IntPair, IntPair> combinationsToJamoMap = new HashMap<>();

        // add the default decomposition as a way to combine the jamo
        for (Map.Entry<Integer, IntPair> entry : sJamoToStdCombinationMap.entrySet()) {
            combinationsToJamoMap.put(entry.getValue(), new IntPair(entry.getKey(), PREF_REQ_NONE));
        }

        //TODO: (EW) consider only combining double consonants as a double tap (and maybe replace
        // the character rather than list as combination)

        // add 1 to get the double consonant
        final int[] doubleConsonants = {
                HANGUL_LETTER_KIYEOK, // ㄱ + ㄱ = ㄲ
                HANGUL_LETTER_TIKEUT, // ㄷ + ㄷ = ㄸ
                HANGUL_LETTER_PIEUP, // ㅂ + ㅂ = ㅃ
                HANGUL_LETTER_SIOS, // ㅅ + ㅅ = ㅆ
                HANGUL_LETTER_CIEUC // ㅈ + ㅈ = ㅉ
        };
        for (int consonant : doubleConsonants) {
            combinationsToJamoMap.put(new IntPair(consonant, consonant),
                    new IntPair(consonant + 1, PREF_REQ_DOUBLE_CONSONANT));
        }

        // add 1 to get the combined vowel
        final int[] vowelsToCombineWithE = {
                HANGUL_LETTER_A, //ㅏ + ㅣ = ㅐ
                HANGUL_LETTER_YA, //ㅑ + ㅣ = ㅒ
                HANGUL_LETTER_EO, //ㅓ + ㅣ = ㅔ
                HANGUL_LETTER_YEO, //ㅕ + ㅣ = ㅖ
                HANGUL_LETTER_WA, //ㅘ + ㅣ = ㅙ
                HANGUL_LETTER_WEO //ㅝ + ㅣ = ㅞ
        };
        for (int vowel : vowelsToCombineWithE) {
            combinationsToJamoMap.put(new IntPair(vowel, HANGUL_LETTER_I),
                    new IntPair(vowel + 1, PREF_REQ_E));
        }

        //TODO: (EW) consider only combining these as a double tap (and maybe replace the character
        // rather than list as combination)
        final int[] vowelsToIotize = {
                HANGUL_LETTER_A, // add 2 to get the combined vowel
                HANGUL_LETTER_AE, // add 2 to get the combined vowel
                HANGUL_LETTER_EO, // add 2 to get the combined vowel
                HANGUL_LETTER_E, // add 2 to get the combined vowel
                HANGUL_LETTER_O, // add 4 to get the combined vowel
                HANGUL_LETTER_U // add 4 to get the combined vowel
        };
        for (int vowel : vowelsToIotize) {
            final int iotizedVowel = vowel + (vowel < HANGUL_LETTER_O ? 2 : 4);
            combinationsToJamoMap.put(new IntPair(vowel, vowel),
                    new IntPair(iotizedVowel, PREF_REQ_IOTIZE));
        }

        // basic vowel combinations formed with dots
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_ARAEA, HANGUL_LETTER_ARAEA),
                new IntPair(FULLWIDTH_COLON, PREF_REQ_FULLY_COMPOSE)); // ㆍㆍ = ：
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_I, HANGUL_LETTER_ARAEA),
                new IntPair(HANGUL_LETTER_A, PREF_REQ_FULLY_COMPOSE)); // ㅣㆍ = ㅏ
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_A, HANGUL_LETTER_ARAEA),
                new IntPair(HANGUL_LETTER_YA, PREF_REQ_FULLY_COMPOSE)); // ㅣㆍㆍ = ㅑ
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_ARAEA, HANGUL_LETTER_I),
                new IntPair(HANGUL_LETTER_EO, PREF_REQ_FULLY_COMPOSE)); // ㆍㅣ = ㅓ
        combinationsToJamoMap.put(new IntPair(FULLWIDTH_COLON, HANGUL_LETTER_I),
                new IntPair(HANGUL_LETTER_YEO, PREF_REQ_FULLY_COMPOSE)); // ㆍㆍㅣ = ㅕ
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_EU, HANGUL_LETTER_ARAEA),
                new IntPair(HANGUL_LETTER_U, PREF_REQ_FULLY_COMPOSE)); // ㅡㆍ = ㅜ
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_U, HANGUL_LETTER_ARAEA),
                new IntPair(HANGUL_LETTER_YU, PREF_REQ_FULLY_COMPOSE)); // ㅡㆍㆍ = ㅠ
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_ARAEA, HANGUL_LETTER_EU),
                new IntPair(HANGUL_LETTER_O, PREF_REQ_FULLY_COMPOSE)); // ㆍㅡ = ㅗ
        combinationsToJamoMap.put(new IntPair(FULLWIDTH_COLON, HANGUL_LETTER_EU),
                new IntPair(HANGUL_LETTER_YO, PREF_REQ_FULLY_COMPOSE)); // ㆍㆍㅡ = ㅛ
        // slightly odd vowel combinations that make sense when formed with dots
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_YU, HANGUL_LETTER_I),
                new IntPair(HANGUL_LETTER_WEO, PREF_REQ_FULLY_COMPOSE)); // ㅡㆍㆍㅣ = ㅝ
        combinationsToJamoMap.put(new IntPair(HANGUL_LETTER_OE, HANGUL_LETTER_ARAEA),
                new IntPair(HANGUL_LETTER_WA, PREF_REQ_FULLY_COMPOSE)); // ㆍㅡㅣㆍ = ㅘ

        sCombinationToJamoMap = Collections.unmodifiableMap(combinationsToJamoMap);
    }

    /**
     * Build a single jamo code point from a combination of code points if possible.
     * @param jamo the jamo containing a list of component code points.
     * @param jamoIndex the index of the syllable's jamo where the code point will be used.
     * @return the combined compatibility jamo (U+3130 to U+318F) code point or
     *         {@link #NO_CODE_POINT} if there wasn't a valid combination.
     */
    public static int getCombination(final HangulSyllable.Jamo jamo, final int jamoIndex) {
        final int size = jamo.size();
        if (size < 1) {
            return NO_CODE_POINT;
        }
        int result = jamo.get(0);
        for (int i = 1; i < size; i++) {
            result = getCombination(result, jamo.get(i), jamoIndex, null);
            if (result < 0) {
                return NO_CODE_POINT;
            }
        }
        return result;
    }

    /**
     * Build a single jamo code point from a combination of an existing jamo's components and an
     * additional code point if possible.
     * @param jamo the jamo to try to add a new component to.
     * @param newCodePoint the new component to try to add to the jamo.
     * @param jamoIndex the index of the syllable's jamo where the code point will be used.
     * @param prefs the settings used to determine what combinations can be used. This is only used
     *              to validate the new code point. The existing components of the jamo are assumed
     *              to be valid.
     * @return the combined compatibility jamo (U+3130 to U+318F) code point or
     *         {@link #NO_CODE_POINT} if there wasn't a valid combination.
     */
    public static int getCombination(final HangulSyllable.Jamo jamo, final int newCodePoint,
                                     final int jamoIndex, final HangulPreferences prefs) {
        if (jamo.size() == 0) {
            return newCodePoint;
        }
        final int currentCombination = getCombination(jamo, jamoIndex);
        if (currentCombination == NO_CODE_POINT) {
            return NO_CODE_POINT;
        }
        return getCombination(currentCombination, newCodePoint, jamoIndex, prefs);
    }

    /**
     * Check if a combination is allowed based on the preferences.
     * @param prefRequirement preference requirement flag indicating which preference should be
     *                        checked.
     * @param prefs the settings used to determine what combinations can be used.
     * @return whether the combination is allowed.
     */
    private static boolean combinationAllowed(final int prefRequirement,
                                              final HangulPreferences prefs) {
        switch (prefRequirement) {
            case PREF_REQ_E:
                return prefs.buildEVowels();
            case PREF_REQ_IOTIZE:
                return prefs.buildIotizedVowels();
            case PREF_REQ_FULLY_COMPOSE:
                return prefs.fullyComposeVowels();
            case PREF_REQ_DOUBLE_CONSONANT:
                return prefs.buildDoubleConsonants();
        }
        return true;
    }

    /**
     * Get the combination of two code points if it exists and is allowed.
     * @param codePointA the first of two code points to combine.
     * @param codePointB the second of two code points to combine.
     * @param jamoIndex the index of the syllable's jamo where the code point will be used.
     * @param prefs the settings used to determine what combinations can be used. If this is null,
     *              all combinations will be allowed.
     * @return the combined compatibility jamo (U+3130 to U+318F) code point or
     *         {@link #NO_CODE_POINT} if there wasn't a valid combination.
     */
    private static int getCombination(final int codePointA, final int codePointB,
                                      final int jamoIndex, final HangulPreferences prefs) {
        if (jamoIndex == 0 && (codePointA != codePointB
                || (prefs != null && !prefs.buildDoubleConsonants()))) {
            return NO_CODE_POINT;
        }

        final IntPair pair = new IntPair(codePointA, codePointB);
        if (sCombinationToJamoMap.containsKey(pair)) {
            final IntPair combination = sCombinationToJamoMap.get(pair);
            if (prefs == null || combinationAllowed(combination.getValue1(), prefs)) {
                return combination.getValue0();
            }
        }

        return NO_CODE_POINT;
    }

    /** The list of cycling options for the 10-key keyboard layout. The first value in the inner
     *  array is the value that the key enters by default. Note that the outer array needs to be
     *  sorted based on the first value for each inner array to support a binary search in
     *  {@link #getCycleCharOptions(int)}. */
    private static final int[][] sCycleChars = {
            new int[] { HANGUL_LETTER_KIYEOK, HANGUL_LETTER_KHIEUKH, HANGUL_LETTER_SSANGKIYEOK },
            new int[] { HANGUL_LETTER_NIEUN, HANGUL_LETTER_RIEUL },
            new int[] { HANGUL_LETTER_TIKEUT, HANGUL_LETTER_THIEUTH, HANGUL_LETTER_SSANGTIKEUT },
            new int[] { HANGUL_LETTER_PIEUP, HANGUL_LETTER_PHIEUPH, HANGUL_LETTER_SSANGPIEUP },
            new int[] { HANGUL_LETTER_SIOS, HANGUL_LETTER_HIEUH, HANGUL_LETTER_SSANGSIOS },
            new int[] { HANGUL_LETTER_IEUNG, HANGUL_LETTER_MIEUM },
            new int[] { HANGUL_LETTER_CIEUC, HANGUL_LETTER_CHIEUCH, HANGUL_LETTER_SSANGCIEUC }
    };

    /**
     * Get the 10-key keyboard layout cycling options for a code point that was entered.
     * @param cycleCodePoint the default code point entered by the key to look up the options.
     * @return the code points that can be cycled by pressing the key multiple times.
     */
    private static int[] getCycleCharOptions(final int cycleCodePoint) {
        // do a binary search to find the list of options that repeatedly pressing a key will cycle
        // through
        int min = 0;
        int max = sCycleChars.length - 1;
        int index;
        while (min <= max) {
            // select the middle list
            index = (max + 1 - min) / 2 + min;

            // the first input in the option list is the code point entered to cycle the options
            final int optionInputCodePoint = sCycleChars[index][0];

            if (cycleCodePoint < optionInputCodePoint) {
                max = index - 1;
            } else if (cycleCodePoint > optionInputCodePoint) {
                min = index + 1;
            } else {
                return sCycleChars[index];
            }
        }

        // the entered code point isn't used to cycle characters, so there are no options to return
        return null;
    }

    /**
     * Get the next cycle option if existing code point can be cycled with the specified cycling
     * code point.
     * @param existingCodePoint the existing code point to cycle.
     * @param newCodePoint the default code point entered by the key to look up the cycle options.
     * @return the next cycle option if it can be cycled or {@link #NO_CODE_POINT}.
     */
    public static int tryCycleChar(final int existingCodePoint, final int newCodePoint) {
        final int[] options = getCycleCharOptions(newCodePoint);
        if (options == null) {
            return NO_CODE_POINT;
        }

        for (int i = 0; i < options.length; i++) {
            // find where the current code point is in the list of options
            if (options[i] == existingCodePoint) {
                // get the next option in the list (loop back to the beginning if at the end)
                return options[i + 1 < options.length ? i + 1 : 0];
            }
        }

        return NO_CODE_POINT;
    }

    /**
     * Check if any of the cycle options are valid to combine with a particular jamo.
     * @param existingCodePoint the existing jamo's code point.
     * @param jamoIndex the index of the syllable's jamo where the code point is used.
     * @param cycleCodePoint the default code point entered by the key to look up the cycle options.
     * @param prefs the settings used to determine what combinations can be used.
     * @return whether any valid combination exists.
     */
    public static boolean cycleOptionCanCombine(final int existingCodePoint, final int jamoIndex,
                                                final int cycleCodePoint,
                                                final HangulPreferences prefs) {
        final int[] options = getCycleCharOptions(cycleCodePoint);
        if (options == null) {
            return false;
        }
        for (int i = 0; i < options.length; i++) {
            if (getCombination(existingCodePoint, options[i], jamoIndex, prefs) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a fully composed vowel can be cycled by entering an additional dot (U+318D).
     * @param vowel the current vowel jamo.
     * @return whether the vowel can be cycled.
     */
    public static boolean canCycleFullyComposedVowel(final HangulSyllable.Jamo vowel) {
        if (vowel == null) {
            return false;
        }
        final int size = vowel.size();
        if (size < 2) {
            return false;
        }
        if (vowel.get(size - 1) != HANGUL_LETTER_ARAEA) {
            return false;
        }
        final int vowelCodePoint = vowel.getCodePoint();
        return vowelCodePoint == HANGUL_LETTER_YA || vowelCodePoint == HANGUL_LETTER_YU
                || vowelCodePoint == FULLWIDTH_COLON;
    }

    /**
     * Check if a jamo code point is valid for a particular jamo index in the syllable.
     * @param jamoIndex the index of the syllable's jamo where the code point is be used.
     * @param jamoCodePoint the compatibility jamo (U+3130 to U+318F) code point to check.
     * @param prefs the settings used to determine what can be used to build jamos.
     * @return whether the code point is valid for the jamo.
     */
    public static boolean isValidInputJamo(final int jamoIndex, final int jamoCodePoint,
                                           final HangulPreferences prefs) {
        if (jamoIndex == 1 && prefs.fullyComposeVowels()
                && (jamoCodePoint == HANGUL_LETTER_ARAEA || jamoCodePoint == FULLWIDTH_COLON)) {
            return true;
        }
        return compatibilityToPositionalJamo(jamoCodePoint, jamoIndex) >= 0;
    }

    /**
     * Split a jamo code point into multiple pieces if it can be broken up. This only uses standard
     * combinations (no need to check preferences).
     * @param codePoint the compatibility jamo (U+3130 to U+318F) code point to split.
     * @return an array of jamo pieces or null if it can't be broken up.
     */
    public static int[] getCompositeJamoPieces(final int codePoint) {
        if (!sJamoToStdCombinationMap.containsKey(codePoint)) {
            return null;
        }
        IntPair pieces = sJamoToStdCombinationMap.get(codePoint);
        return new int[]{pieces.getValue0(), pieces.getValue1()};
    }

    /**
     * A pair of integers.
     */
    private static class IntPair {
        private final int value0;
        private final int value1;

        public IntPair(final int value0, final int value1) {
            this.value0 = value0;
            this.value1 = value1;
        }

        public int getValue0() {
            return value0;
        }

        public int getValue1() {
            return value1;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int hash = 1;
            hash = hash * prime + value0;
            hash = hash * prime + value1;
            return hash;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof IntPair
                    && ((IntPair) o).value0 == value0 && ((IntPair) o).value1 == value1;
        }

        @Override
        public String toString() {
            return value0 + "," + value1;
        }
    }
}
