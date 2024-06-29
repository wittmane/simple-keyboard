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

package rkr.simplekeyboard.inputmethod.latin.inputlogic;

/**
 * Manager interface for composing text. Note that #toString needs to be overridden as that is what
 * should get used for the composition entered in the input connection.
 */
public interface ComposingText {
    /**
     * Remove all of the text that is being edited.
     */
    void clear();

    /**
     * Add a code point to the composing text.
     * @param codePoint the new code point to add.
     * @return the completed text if the entered code point requires starting a separate composition.
     */
    String addCodePoint(final int codePoint);

    /**
     * Add a string to the composing text.
     * @param text a string to add.
     * @return the completed text if the entered text requires starting a separate composition.
     */
    String addString(final String text);

    /**
     * Remove the last portion of the composition.
     */
    void backspace();

    /**
     * Check if the text is empty.
     * @return whether there is no text.
     */
    boolean isEmpty();
}
