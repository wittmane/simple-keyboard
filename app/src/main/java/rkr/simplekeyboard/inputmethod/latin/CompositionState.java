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

public class CompositionState {
    public final int cursorStart;//relative to composition start
    public final Integer cursorEnd;//relative to composition start
    public final String compositionText;//null when text isn't certain
    public CompositionState(final int cursorStart, final Integer cursorEnd,
                            final String compositionText) {
        this.cursorStart = cursorStart;
        this.cursorEnd = cursorEnd;
        this.compositionText = compositionText;
    }
    @Override
    public String toString() {
        return "{ compositionText: \"" + compositionText
                + "\", cursorStart: " + cursorStart
                + ", cursorEnd: " + cursorEnd + " }";
    }
}
