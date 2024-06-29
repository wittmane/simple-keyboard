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

public class HangulPreferences {
    private static HangulPreferences sInstance;
    private String mLayoutSetName;

    private static final String LAYOUT_TWO_BULSIK = "korean_two_bulsik";
    private static final String LAYOUT_SHORT_VOWEL = "korean_short_vowel";
    private static final String LAYOUT_TEN_KEY = "korean_ten_key";

    private HangulPreferences() {

    }
    public static HangulPreferences getInstance() {
        if (sInstance == null) {
            sInstance = new HangulPreferences();
        }
        return sInstance;
    }
    public void setKeyboardLayout(final String layoutSetName) {
        mLayoutSetName = layoutSetName;
    }
    public boolean deleteWholeSyllable() {
        //TODO: (EW) get this from a user preference
        return false;
    }
    public boolean buildDoubleConsonants() {
        //TODO: (EW) get this from a user preference
        return false;
    }
    public boolean buildIotizedVowels() {
        //TODO: (EW) get this from a user preference
        final boolean setting = false;
        return setting || LAYOUT_SHORT_VOWEL.equals(mLayoutSetName)
                || LAYOUT_TEN_KEY.equals(mLayoutSetName);
    }
    public boolean buildEVowels() {
        //TODO: (EW) get this from a user preference
        final boolean setting = false;
        return setting || LAYOUT_TEN_KEY.equals(mLayoutSetName);
    }
    public boolean fullyComposeVowels() {
        return LAYOUT_TEN_KEY.equals(mLayoutSetName);
    }
    //TODO: (EW) possibly rename this since this is specifically tied to the 10 key input, but
    // sounds generic
    public boolean canCycleInputLetter() {
        return LAYOUT_TEN_KEY.equals(mLayoutSetName);
    }
}
