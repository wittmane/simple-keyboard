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

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputConnection;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.UPDATE_IMPACTED_SELECTION;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.UPDATE_WAS_EXPECTED;

public class RichInputConnectionTestsV1 {
    private final ArrayList<UpdateSelectionCall> updateSelectionCalls = new ArrayList<>();
    private final ArrayList<Integer> expectedUpdateSelectionCalls = new ArrayList<>();
    private FakeInputConnection fakeInputConnection;
    private RichInputConnection richInputConnection;

    @Before
    public void setup() {
        updateSelectionCalls.clear();
        expectedUpdateSelectionCalls.clear();
        richInputConnection = new RichInputConnection(new InputMethodService() {
            @Override
            public InputConnection getCurrentInputConnection() {
                return fakeInputConnection;
            }
        });
        final FakeInputConnection.FakeInputMethodManager fakeInputMethodManager = new FakeInputConnection.FakeInputMethodManager() {
            @Override
            public void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
                updateSelectionCalls.add(new UpdateSelectionCall(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
                expectedUpdateSelectionCalls.add(richInputConnection.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
            }

            @Override
            public void updateExtractedText(View view, int token, ExtractedText text) {

            }

            @Override
            public void processUpdates() {

            }
        };
        //TODO: probably don't do this here so alternative parameters can be used
        fakeInputConnection = new FakeInputConnection(fakeInputMethodManager);
    }

    public void setup(final String initialText, final int initialCursorStart, final int initialCursorEnd) {
        richInputConnection = null;
        updateSelectionCalls.clear();
        expectedUpdateSelectionCalls.clear();
        final FakeInputConnection.FakeInputMethodManager fakeInputMethodManager = new FakeInputConnection.FakeInputMethodManager() {
            @Override
            public void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
                if (richInputConnection != null) {
                    updateSelectionCalls.add(new UpdateSelectionCall(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
                    expectedUpdateSelectionCalls.add(richInputConnection.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
                }
            }

            @Override
            public void updateExtractedText(View view, int token, ExtractedText text) {

            }

            @Override
            public void processUpdates() {

            }
        };
        //TODO: probably don't do this here so alternative parameters can be used
        fakeInputConnection = new FakeInputConnection(fakeInputMethodManager, initialText, initialCursorStart, initialCursorEnd);

        richInputConnection = new RichInputConnection(new InputMethodService() {
            @Override
            public InputConnection getCurrentInputConnection() {
                return fakeInputConnection;
            }
        });
    }

    @Test
    public void initialTest() {
        Log.w("Test", "surrogate: " + getSurrogatePairString(0, 100));
//        Log.w("Test", "surrogate: " + (int)('\uD800') + " " + (int)('\uD800' + 1) + "");
//        final int surrogate1 = getSurrogatePair(1);
//        final int surrogate2 = getSurrogatePair(2);
//        final int surrogate3 = getSurrogatePair(3);
//        final int surrogate4 = getSurrogatePair(4);
//        Log.w("Test", "surrogate1: " + surrogate1 + " '" + new StringBuilder().appendCodePoint(surrogate1).toString() + "'");
//        Log.w("Test", "surrogate2: " + surrogate2 + " '" + new StringBuilder().appendCodePoint(surrogate2).toString() + "' " + (int)(surrogate2 - surrogate1));
//        Log.w("Test", "surrogate3: " + surrogate3 + " '" + new StringBuilder().appendCodePoint(surrogate3).toString() + "' " + (surrogate3 - surrogate2));
//        Log.w("Test", "surrogate4: " + surrogate4 + " '" + new StringBuilder().appendCodePoint(surrogate4).toString() + "' " + (surrogate4 - surrogate3));
//        Log.w("Test", "surrogate: " + getSurrogatePairCodePoint(0xD801, 0xDC01));
//        Log.w("Test", "surrogate: " + getSurrogatePair3(0) + " " + getSurrogatePair3(1) + " " + getSurrogatePair3(1023) + " " + getSurrogatePair3(1024) + " " + getSurrogatePair3(1025) + " " + getSurrogatePair3(-1) + " " + getSurrogatePair3(-1024) + " ");
        final String text = "Hello world!";
        richInputConnection.resetState(0, 0);
        richInputConnection.reloadCachesForStartingInputView();
        richInputConnection.beginBatchEdit();
        richInputConnection.commitText(text, 1);
        richInputConnection.endBatchEdit();
        assertEquals(1, updateSelectionCalls.size());
        assertEquals(new UpdateSelectionCall(0, 0, text.length(), text.length(), -1, -1), updateSelectionCalls.get(0));
        int updateResult = expectedUpdateSelectionCalls.get(0);
        boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
        boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
        assertEquals(true, !updateImpactedSelection);
        assertEquals(text, fakeInputConnection.getText());
        assertNull(fakeInputConnection.getComposingText());
    }




    //#region reset caches
    @Test
    public void resetCaches_initialWithNoText_stateSet() {
        richInputConnection.resetState(0, 0);
        richInputConnection.reloadCachesForStartingInputView();

        verifySelection(0, 0);
        verifyText("", "", "");
    }

    @Test
    public void resetCaches_initialStartOfText_stateSet() {
        testResetCachesInitial("", "", "Lorem ipsum dolor sit amet");
    }

    @Test
    public void resetCaches_initialMiddleOfText_stateSet() {
        testResetCachesInitial("Lorem ipsum", "", " dolor sit amet");
    }

    @Test
    public void resetCaches_initialEndOfText_stateSet() {
        testResetCachesInitial("Lorem ipsum dolor sit amet", "", "");
    }

    @Test
    public void resetCaches_initialSelectMiddleOfText_stateSet() {
        testResetCachesInitial("Lorem ipsum ", "dolor", " sit amet");
    }

    private void testResetCachesInitial(final String before, final String selection, final String after) {
        final int start = before.length();
        final int end = before.length() + selection.length();
        setup(before + selection + after, start, end);

        richInputConnection.resetState(start, end);
        richInputConnection.reloadCachesForStartingInputView();

        verifySelection(start, end);
        verifyText(before, selection, after);
    }
    //#endregion

    private void verifySelection(final int selectionStart, final int selectionEnd) {
        assertEquals(selectionStart, richInputConnection.getExpectedSelectionStart());
        assertEquals(selectionEnd, richInputConnection.getExpectedSelectionEnd());
    }
    private void verifyText(final String beforeCursor, final String selected, final String afterCursor) {
        assertEquals(beforeCursor, richInputConnection.getTextBeforeCursor(Integer.MAX_VALUE, 0).toString());
        assertEquals(selected, richInputConnection.getSelectedText(0).toString());
        assertEquals(afterCursor, richInputConnection.getTextAfterCursor(Integer.MAX_VALUE, 0).toString());
    }

    //#region commit text
    @Test
    public void commitText_initialText_stateUpToDate() {
        testCommitTextBasicSingleCursor("", "", "Lorem ipsum dolor sit amet");
    }

    //#region adding additional text to input connection
    @Test
    public void commitText_additionalTextToBeginning_stateUpToDate() {
        testCommitTextBasicSingleCursor("", "Lorem ipsum dolor sit amet", "test");
    }

    @Test
    public void commitText_additionalTextToEnd_stateUpToDate() {
        testCommitTextBasicSingleCursor("Lorem ipsum dolor sit amet", "", "test");
    }

    @Test
    public void commitText_additionalTextToMiddle_stateUpToDate() {
        testCommitTextBasicSingleCursor("Lorem ipsum", " dolor sit amet", "test");
    }

    @Test
    public void commitText_additionalTextSetCursorBeforeNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        testCommitTextBasicSingleCursor(initialTextBefore, " dolor sit amet", "test", -2, initialTextBefore.length() - 2);
    }

    @Test
    public void commitText_additionalTextSetCursorAfterNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String addedText = "test";
        testCommitTextBasicSingleCursor(initialTextBefore, " dolor sit amet", addedText, 3, initialTextBefore.length() + addedText.length() + 2);
    }

    @Test
    public void commitText_additionalTextSetCursorBeforeStart_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        testCommitTextBasicSingleCursor(initialTextBefore, " dolor sit amet", "test", -50, 0);
    }

    @Test
    public void commitText_additionalTextSetCursorAfterEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String addedText = "test";
        testCommitTextBasicSingleCursor(initialTextBefore, initialTextAfter, addedText, 50, initialTextBefore.length() + addedText.length() + initialTextAfter.length());
    }
    //#endregion

    //#region changing text in the selection
    @Test
    public void commitText_selectedTextToBeginning_stateUpToDate() {
        testCommitTextBasicSelection("", "Lorem", " ipsum dolor sit amet", "test");
    }

    @Test
    public void commitText_selectedTextToEnd_stateUpToDate() {
        testCommitTextBasicSelection("Lorem ipsum dolor ", "sit amet", "", "test");
    }

    @Test
    public void commitText_selectedTextMiddle_stateUpToDate() {
        testCommitTextBasicSelection("Lorem ipsum", " dolor ", "sit amet", "test");
    }

    @Test
    public void commitText_selectedTextWhole() {
        testCommitTextBasicSelection("", "Lorem ipsum dolor sit amet", "", "test");
    }

    @Test
    public void commitText_selectedTextShorter_stateUpToDate() {
        testCommitTextBasicSelection("Lorem ipsum dolor ", "sit", " amet", "test");
    }

    @Test
    public void commitText_selectedTextSetCursorBeforeNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testCommitTextBasicSelection(initialTextBefore, selectedText, initialTextAfter, addedText, -2, initialTextBefore.length() - 2);
    }

    @Test
    public void commitText_selectedTextSetCursorAfterNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testCommitTextBasicSelection(initialTextBefore, selectedText, initialTextAfter, addedText, 3, initialTextBefore.length() + addedText.length() + 2);
    }

    @Test
    public void commitText_selectedTextSetCursorBeforeStart_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testCommitTextBasicSelection(initialTextBefore, selectedText, initialTextAfter, addedText, -50, 0);
    }

    @Test
    public void commitText_selectedTextSetCursorAfterEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testCommitTextBasicSelection(initialTextBefore, selectedText, initialTextAfter, addedText, 50, initialTextBefore.length() + addedText.length() + initialTextAfter.length());
    }
    //TODO: same as these^^^, but with longer addedText than selectedText? - at least one test case for this (this much is done)
    //#endregion

    //#region text is currently being composed
    //TODO: possibly de-duplicate logic with composeText tests
    @Test
    public void commitText_composingTextShorterSetCursorAtCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String composingText = "test";
        final String committingText = "changed";
        testCommitTextComposing(initialTextBefore, initialTextAfter, composingText, committingText, 1, initialTextBefore.length() + committingText.length());
    }

    @Test
    public void commitText_composingTextShorterSetCursorBeforeCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String composingText = "test";
        final String committingText = "changed";
        testCommitTextComposing(initialTextBefore, initialTextAfter, composingText, committingText, -3, initialTextBefore.length() - 3);
    }

    @Test
    public void commitText_composingTextShorterSetCursorAfterCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String composingText = "test";
        final String committingText = "changed";
        testCommitTextComposing(initialTextBefore, initialTextAfter, composingText, committingText, 4, initialTextBefore.length() + committingText.length() + 3);
    }

    @Test
    public void commitText_composingTextLongerSetCursorAtCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String composingText = "test";
        final String committingText = "new";
        testCommitTextComposing(initialTextBefore, initialTextAfter, composingText, committingText, 1, initialTextBefore.length() + committingText.length());
    }

    @Test
    public void commitText_composingTextLongerSetCursorBeforeCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String composingText = "test";
        final String committingText = "new";
        testCommitTextComposing(initialTextBefore, initialTextAfter, composingText, committingText, -3, initialTextBefore.length() - 3);
    }

    @Test
    public void commitText_composingTextLongerSetCursorAfterCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String composingText = "test";
        final String committingText = "new";
        testCommitTextComposing(initialTextBefore, initialTextAfter, composingText, committingText, 4, initialTextBefore.length() + committingText.length() + 3);
    }
    //#endregion

//    private void testAddText(final String initialTextBefore, final String selectedText,
//                                 final String initialTextAfter, final String newText,
//                                 final int newCursorPosition, final int newCursorIndexSanitized, final boolean composeText) {
//        final int initialStartPosition = initialTextBefore.length();
//        final int initialEndPosition = initialTextBefore.length() + selectedText.length();
//        setup(initialTextBefore + selectedText + initialTextAfter, initialStartPosition, initialEndPosition);
//        richInputConnection.resetCachesUponCursorMoveAndReturnSuccess(initialStartPosition, initialEndPosition);
//
//        richInputConnection.beginBatchEdit();
//        final int composingStart;
//        final int composingEnd;
//        final String composingText;
//        if (composeText) {
//            composingStart = initialStartPosition;
//            composingEnd = initialStartPosition + newText.length();
//            composingText = newText;
//            richInputConnection.setComposingText(newText, newCursorPosition);
//        } else {
//            composingStart = -1;
//            composingEnd = -1;
//            composingText = null;
//            richInputConnection.commitText(newText, newCursorPosition);
//        }
//        richInputConnection.endBatchEdit();
//
//        verifyState(new UpdateSelectionCall(initialStartPosition, initialEndPosition,
//                newCursorIndexSanitized, newCursorIndexSanitized, composingStart, composingEnd), true);
//        verifyActualText(initialTextBefore + newText + initialTextAfter, composingText);
//    }
//    private void testAddText(final String initialTextBefore, final String selectedText,
//                              final String initialTextAfter, final String newText,
//                              final int newCursorPosition, final int newCursorIndexSanitized, final boolean composeText) {
//        testAddText(initialTextBefore,
//                selectedText, false,
//                newText, composeText,
//                initialTextAfter,
//                initialTextBefore.length(), initialTextBefore.length() + selectedText.length(),
//                newCursorPosition, newCursorIndexSanitized);
//    }

    //TODO: reduce duplicate code with testComposeTextUpdate
    private void testCommitTextComposing(final String initialTextBefore,
                                         final String initialTextAfter,
                                         final String initialComposingText,
                                         final String updatedComposingText,
                                         final int newCursorPosition, final int newCursorIndex) {
        final int compositionStart = initialTextBefore.length();
        final int compositionEnd = compositionStart + initialComposingText.length();
        final int initialTextLength = compositionEnd + initialTextAfter.length();

        //TODO: find a good way to split these into separate tests
        final Position[] positionOptions = new Position[] {
                new Position(0, 0),
                new Position(compositionStart, compositionStart),
                new Position(compositionStart + 2, compositionStart + 2),
                new Position(compositionEnd, compositionEnd),
                new Position(initialTextLength-1, initialTextLength-1),
                new Position(initialTextLength, initialTextLength),

                new Position(compositionStart - 3, compositionStart + 2),
                new Position(compositionStart + 2, compositionEnd + 3),
                new Position(compositionStart, compositionEnd),
                new Position(compositionStart + 1, compositionEnd - 1),
                new Position(compositionStart - 2, compositionEnd + 2),
        };
        for (final Position position : positionOptions) {
            Log.w("Test", "Position: " + position.start + ", " + position.end);
            testCommitTextComposing(initialTextBefore, initialComposingText, initialTextAfter, position.start, position.end, updatedComposingText, newCursorPosition, newCursorIndex);
        }
    }


//    private void testAddTextUpdate(final String initialTextBefore, final String initialComposingText,
//                                   final String initialTextAfter,
//                                   final int initialCursorStart, final int initialCursorEnd,
//                                   final String newText,
//                                   final int newCursorPosition, final int newCursorIndexSanitized, final boolean composeText) {
////        final int setupInitialPosition = initialTextBefore.length();
////        setup(initialTextBefore + initialTextAfter, setupInitialPosition, setupInitialPosition);
////        richInputConnection.resetCachesUponCursorMoveAndReturnSuccess(setupInitialPosition, setupInitialPosition);
////        if (initialComposingText != null) {
////            richInputConnection.beginBatchEdit();
////            richInputConnection.setComposingText(initialComposingText, 1);
////            richInputConnection.endBatchEdit();
////        }
////        moveCursor(initialCursorStart, initialCursorEnd);
////
////        richInputConnection.beginBatchEdit();
////        final int composingStart;
////        final int composingEnd;
////        final String composingText;
////        if (composeText) {
////            composingStart = setupInitialPosition;
////            composingEnd = setupInitialPosition + newText.length();
////            composingText = newText;
////            richInputConnection.setComposingText(newText, newCursorPosition);
////        } else {
////            composingStart = -1;
////            composingEnd = -1;
////            composingText = null;
////            richInputConnection.commitText(newText, newCursorPosition);
////        }
////        richInputConnection.endBatchEdit();
////
////        verifyState(new UpdateSelectionCall(initialCursorStart, initialCursorEnd,
////                newCursorIndexSanitized, newCursorIndexSanitized, composingStart, composingEnd), true);
////        verifyActualText(initialTextBefore + newText + initialTextAfter, composingText);
//        testAddText(initialTextBefore,
//                initialComposingText, true,
//                newText, composeText,
//                initialTextAfter,
//                initialCursorStart, initialCursorEnd,
//                newCursorPosition, newCursorIndexSanitized);
//    }
    //#endregion

    //#region compose text
    //TODO: possibly de-duplicate logic with commitText tests
    @Test
    public void composeText_initialText_stateUpToDate() {
        testComposeTextInitialSingleCursor("", "", "Lorem ipsum dolor sit amet");
    }

    //#region adding additional text to input connection (starting composition)
    @Test
    public void composeText_additionalTextToBeginning_stateUpToDate() {
        testComposeTextInitialSingleCursor("", "Lorem ipsum dolor sit amet", "test");
    }

    @Test
    public void composeText_additionalTextToEnd_stateUpToDate() {
        testComposeTextInitialSingleCursor("Lorem ipsum dolor sit amet", "", "test");
    }

    @Test
    public void composeText_additionalTextToMiddle_stateUpToDate() {
        testComposeTextInitialSingleCursor("Lorem ipsum", " dolor sit amet", "test");
    }

    @Test
    public void composeText_additionalTextSetCursorBeforeNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        testComposeTextInitialSingleCursor(initialTextBefore, " dolor sit amet", "test", -2, initialTextBefore.length() - 2);
    }

    @Test
    public void composeText_additionalTextSetCursorAfterNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String addedText = "test";
        testComposeTextInitialSingleCursor(initialTextBefore, " dolor sit amet", addedText, 3, initialTextBefore.length() + addedText.length() + 2);
    }

    @Test
    public void composeText_additionalTextSetCursorBeforeStart_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        testComposeTextInitialSingleCursor(initialTextBefore, " dolor sit amet", "test", -50, 0);
    }

    @Test
    public void composeText_updateTextSetCursorAfterEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String addedText = "test";
        testComposeTextInitialSingleCursor(initialTextBefore, initialTextAfter, addedText, 50, initialTextBefore.length() + addedText.length() + initialTextAfter.length());
    }
    //#endregion

    //#region changing text in the selection (starting composition)
    //TODO: reduce duplicate code with commitText
    @Test
    public void composeText_selectedTextToBeginning_stateUpToDate() {
        testComposeTextInitialSelection("", "Lorem", " ipsum dolor sit amet", "test");
    }

    @Test
    public void composeText_selectedTextToEnd_stateUpToDate() {
        testComposeTextInitialSelection("Lorem ipsum dolor ", "sit amet", "", "test");
    }

    @Test
    public void composeText_selectedTextMiddle_stateUpToDate() {
        testComposeTextInitialSelection("Lorem ipsum", " dolor ", "sit amet", "test");
    }

    @Test
    public void composeText_selectedTextWhole() {
        testComposeTextInitialSelection("", "Lorem ipsum dolor sit amet", "", "test");
    }

    @Test
    public void composeText_selectedTextShorter_stateUpToDate() {
        testComposeTextInitialSelection("Lorem ipsum dolor ", "sit", " amet", "test");
    }

    @Test
    public void composeText_selectedTextSetCursorBeforeNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testComposeTextInitialSelection(initialTextBefore, selectedText, initialTextAfter, addedText, -2, initialTextBefore.length() - 2);
    }

    @Test
    public void composeText_selectedTextSetCursorAfterNewText_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testComposeTextInitialSelection(initialTextBefore, selectedText, initialTextAfter, addedText, 3, initialTextBefore.length() + addedText.length() + 2);
    }

    @Test
    public void composeText_selectedTextSetCursorBeforeStart_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testComposeTextInitialSelection(initialTextBefore, selectedText, initialTextAfter, addedText, -50, 0);
    }

    @Test
    public void composeText_selectedTextSetCursorAfterEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String selectedText = " dolor ";
        final String initialTextAfter = "sit amet";
        final String addedText = "test";
        testComposeTextInitialSelection(initialTextBefore, selectedText, initialTextAfter, addedText, 50, initialTextBefore.length() + addedText.length() + initialTextAfter.length());
    }
    //#endregion

    //#region text is currently being composed (update composing text)
    // composing text length, new cursor position, old cursor position
    @Test
    public void composeText_longerTextSetCursorAtCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String initialComposingText = "test";
        final String updatedComposingText = "changed";
        testComposeTextUpdate(initialTextBefore, initialTextAfter, initialComposingText, updatedComposingText, 1, initialTextBefore.length() + updatedComposingText.length());
    }

    @Test
    public void composeText_longerTextSetCursorBeforeCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String initialComposingText = "test";
        final String updatedComposingText = "changed";
        testComposeTextUpdate(initialTextBefore, initialTextAfter, initialComposingText, updatedComposingText, -3, initialTextBefore.length() - 3);
    }

    @Test
    public void composeText_longerTextSetCursorAfterCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String initialComposingText = "test";
        final String updatedComposingText = "changed";
        testComposeTextUpdate(initialTextBefore, initialTextAfter, initialComposingText, updatedComposingText, 4, initialTextBefore.length() + updatedComposingText.length() + 3);
    }

    @Test
    public void composeText_shorterTextSetCursorAtCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String initialComposingText = "test";
        final String updatedComposingText = "new";
        testComposeTextUpdate(initialTextBefore, initialTextAfter, initialComposingText, updatedComposingText, 1, initialTextBefore.length() + updatedComposingText.length());
    }

    @Test
    public void composeText_shorterTextSetCursorBeforeCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String initialComposingText = "test";
        final String updatedComposingText = "new";
        testComposeTextUpdate(initialTextBefore, initialTextAfter, initialComposingText, updatedComposingText, -3, initialTextBefore.length() - 3);
    }

    @Test
    public void composeText_shorterTextSetCursorAfterCompositionEnd_stateUpToDate() {
        final String initialTextBefore = "Lorem ipsum";
        final String initialTextAfter = " dolor sit amet";
        final String initialComposingText = "test";
        final String updatedComposingText = "new";
        testComposeTextUpdate(initialTextBefore, initialTextAfter, initialComposingText, updatedComposingText, 4, initialTextBefore.length() + updatedComposingText.length() + 3);
    }
    //#endregion


//    @Test
//    public void moveCursor_beforeComposingText_stateUpdates() {
//        final String initialTextBefore = "Lorem ipsum";
//        final String initialTextAfter = " dolor sit amet";
//        final String initialComposingText = "test";
//        final String addedText = "changed";
//
//
//
//
//        final int initialPosition = textBefore.length();
//        setup(textBefore + textAfter, initialPosition, initialPosition);
//        richInputConnection.resetCachesUponCursorMoveAndReturnSuccess(initialPosition, initialPosition);
//        richInputConnection.beginBatchEdit();
//        richInputConnection.setComposingText(initialComposingText, 1);
//        richInputConnection.endBatchEdit();
//        fakeInputConnection.setSelection(initialTextBefore, initialCursorEnd);
//
//        final Position[] positionOptions = new Position[] {
//                new Position(0, 0),
//                new Position(initialTextBefore.length(), initialTextBefore.length()),
//                new Position(initialTextBefore.length() + 2, initialTextBefore.length() + 2),
//                new Position(initialTextBefore.length() + initialComposingText.length(), initialTextBefore.length() + initialComposingText.length()),
//                new Position(initialTextBefore.length() + initialComposingText.length() + initialTextAfter.length(), initialTextBefore.length() + initialComposingText.length() + initialTextAfter.length()),
//
//                new Position(initialTextBefore.length() - 3, initialTextBefore.length() + 2),
//                new Position(initialTextBefore.length() + 2, initialTextBefore.length() + initialComposingText.length() + 3),
//                new Position(initialTextBefore.length(), initialTextBefore.length() + initialComposingText.length()),
//                new Position(initialTextBefore.length() + 1, initialTextBefore.length() + initialComposingText.length() - 1),
//                new Position(initialTextBefore.length() - 2, initialTextBefore.length() + initialComposingText.length() + 2),
//        };
//        final String[] newTextOptions = new String[] {"z", "asdf", "changed"};
//        final int[] newCursorPositionOptions = new int[] {-3, }
//
//        testComposeTextUpdate(initialTextBefore, initialTextAfter, initialComposingText, 0, 0, addedText, 1, );
//    }

//    private void testMoveCursor(final String textBeforeComposition, final String composingText, final String textAfterComposition,
//                                       final int cursorStart, final int cursorEnd,
//                                       final String addedText, final int newCursorPosition,
//                                       final int newCursorIndexSanitized) {
//        final int initialPosition = textBeforeComposition.length();
//        setup(textBeforeComposition + textAfterComposition, initialPosition, initialPosition);
//        richInputConnection.resetCachesUponCursorMoveAndReturnSuccess(initialPosition, initialPosition);
//        richInputConnection.beginBatchEdit();
//        richInputConnection.setComposingText(composingText, 1);
//        richInputConnection.endBatchEdit();
//
//        fakeInputConnection.setSelection(cursorStart, cursorEnd);
//
//        verifyState(new UpdateSelectionCall(initialPosition, initialPosition,
//                newCursorIndexSanitized, newCursorIndexSanitized, initialPosition, initialPosition + addedText.length()), false);
//
//    }

    private static class Position {
        public final int start;
        public final int end;
        public Position(final int start, final int end) {
            this.start = start;
            this.end = end;
        }
    }


    private void testComposeTextUpdate(final String initialTextBefore,
                                       final String initialTextAfter,
                                       final String initialComposingText,
                                       final String updatedComposingText,
                                       final int newCursorPosition, final int newCursorIndex) {
        final int compositionStart = initialTextBefore.length();
        final int compositionEnd = compositionStart + initialComposingText.length();
        final int initialTextLength = compositionEnd + initialTextAfter.length();

        //TODO: find a good way to split these into separate tests
        final Position[] positionOptions = new Position[] {
                new Position(0, 0),
                new Position(compositionStart, compositionStart),
                new Position(compositionStart + 2, compositionStart + 2),
                new Position(compositionEnd, compositionEnd),
                new Position(initialTextLength-1, initialTextLength-1),// this also catches those vvv issues
                new Position(initialTextLength, initialTextLength),// this caught issues for *SetCursorAfterCompositionEnd

                new Position(compositionStart - 3, compositionStart + 2),
                new Position(compositionStart + 2, compositionEnd + 3),
                new Position(compositionStart, compositionEnd),
                new Position(compositionStart + 1, compositionEnd - 1),
                new Position(compositionStart - 2, compositionEnd + 2),
        };
        for (final Position position : positionOptions) {
            Log.w("Test", "Position: " + position.start + ", " + position.end);
            testComposeTextUpdate(initialTextBefore, initialComposingText, initialTextAfter, position.start, position.end, updatedComposingText, newCursorPosition, newCursorIndex);
        }
    }
    //#endregion

    //#region add text helpers

    // test cases:
    // start composing text
    //    single cursor     -> insert composing text
    //    selection         -> replace with composing text
    // update composing text
    //    single cursor     -> update composing text
    //    selection         -> update composing text
    // commit text without a composition
    //    single cursor     -> insert text
    //    selection         -> replace text
    // commit text with a composition
    //    single cursor     -> replace composing text
    //    selection         -> replace composing text
    //TODO: clean up the wrapper tags for this and possibly make one for each of these^^^ test cases for better readability
    private void testAddText(final String textBefore,
                             final String initialText, final boolean initialTextComposed,
                             final String textAfter,
                             final int initialCursorStart, final int initialCursorEnd,
                             final String newText, final boolean composeNewText,
                             final int newCursorPosition, final int newCursorIndex) {
        final int setupInitialPosition = textBefore.length();
        setup(textBefore + textAfter, setupInitialPosition, setupInitialPosition);
        richInputConnection.resetState(setupInitialPosition, setupInitialPosition);
        richInputConnection.reloadCachesForStartingInputView();
        if (initialText != null) {
            richInputConnection.beginBatchEdit();
            if (initialTextComposed) {
                richInputConnection.setComposingText(initialText, 1);
            } else {
                richInputConnection.commitText(initialText, 1);
            }
            richInputConnection.endBatchEdit();
        }
        //TODO: is there a simple way to skip this call if there is no initial text? or is it better to keep for consistency between test cases?
        fakeInputConnection.setSelection(initialCursorStart, initialCursorEnd);

        richInputConnection.beginBatchEdit();
        final int composingStart;
        final int composingEnd;
        final String composingText;
        if (composeNewText) {
            composingStart = setupInitialPosition;
            composingEnd = setupInitialPosition + newText.length();
            composingText = newText;
            richInputConnection.setComposingText(newText, newCursorPosition);
        } else {
            composingStart = -1;
            composingEnd = -1;
            composingText = null;
            richInputConnection.commitText(newText, newCursorPosition);
        }
        richInputConnection.endBatchEdit();

        boolean lastUpdateExpected = newCursorPosition - 1 <= textAfter.length();
        verifyState(new UpdateSelectionCall(initialCursorStart, initialCursorEnd,
                newCursorIndex, newCursorIndex, composingStart, composingEnd), lastUpdateExpected);
        verifyActualText(textBefore + newText + textAfter, composingText);
    }

    //#region start composing text
    private void testComposeTextInitialSingleCursor(final String textBefore, final String textAfter,
                                                    final String newText) {
        testComposeTextInitialSelection(textBefore, "", textAfter, newText);
    }

    private void testComposeTextInitialSingleCursor(final String textBefore, final String textAfter,
                                                    final String newText,
                                                    final int newCursorPosition,
                                                    final int newCursorIndex) {
        testComposeTextInitialSelection(textBefore, "", textAfter,
                newText,newCursorPosition, newCursorIndex);
    }

    private void testComposeTextInitialSelection(final String textBefore, final String selectedText,
                                                 final String textAfter, final String newText) {
        testComposeTextInitialSelection(textBefore, selectedText, textAfter,
                newText, 1, textBefore.length() + newText.length());
    }

    private void testComposeTextInitialSelection(final String textBefore, final String selectedText,
                                                 final String textAfter, final String newText,
                                                 final int newCursorPosition,
                                                 final int newCursorIndex) {
        testAddText(textBefore, selectedText, false, textAfter,
                textBefore.length(), textBefore.length() + selectedText.length(),
                newText, true,
                newCursorPosition, newCursorIndex);
    }
    //#endregion

    //#region update composing text
    private void testComposeTextUpdate(final String textBefore, final String initialComposingText,
                                       final String textAfter,
                                       final int initialCursorStart, final int initialCursorEnd,
                                       final String newText,
                                       final int newCursorPosition, final int newCursorIndex) {
        testAddText(textBefore, initialComposingText, true, textAfter,
                initialCursorStart, initialCursorEnd,
                newText, true,
                newCursorPosition, newCursorIndex);
    }
    //#endregion

    //#region commit text without a composition
    private void testCommitTextBasicSingleCursor(final String textBefore, final String textAfter,
                                                 final String addedText) {
        testCommitTextBasicSelection(textBefore, "", textAfter, addedText);
    }

    private void testCommitTextBasicSingleCursor(final String textBefore, final String textAfter,
                                                 final String addedText,
                                                 final int newCursorPosition,
                                                 final int newCursorIndex) {
        testCommitTextBasicSelection(textBefore, "", textAfter,
                addedText, newCursorPosition, newCursorIndex);
    }

    private void testCommitTextBasicSelection(final String textBefore, final String selectedText,
                                              final String textAfter, final String addedText) {
        testCommitTextBasicSelection(textBefore, selectedText, textAfter,
                addedText, 1, textBefore.length() + addedText.length());
    }

    private void testCommitTextBasicSelection(final String textBefore, final String selectedText,
                                              final String textAfter, final String newText,
                                              final int newCursorPosition,
                                              final int newCursorIndex) {
        testAddText(textBefore, selectedText, false, textAfter,
                textBefore.length(), textBefore.length() + selectedText.length(),
                newText, false,
                newCursorPosition, newCursorIndex);
    }
    //#endregion

    //#region commit text with a composition
    private void testCommitTextComposing(final String textBefore, final String initialComposingText,
                                         final String textAfter,
                                         final int initialCursorStart, final int initialCursorEnd,
                                         final String newText,
                                         final int newCursorPosition, final int newCursorIndex) {
        testAddText(textBefore, initialComposingText, true, textAfter,
                initialCursorStart, initialCursorEnd,
                newText, false,
                newCursorPosition, newCursorIndex);
    }
    //#endregion
    //#endregion

    private void verifyState(final UpdateSelectionCall lastUpdateSelectionCall, final boolean lastUpdateExpected) {
        assertNotEquals(0, updateSelectionCalls.size());
        assertEquals(lastUpdateSelectionCall, updateSelectionCalls.get(updateSelectionCalls.size() - 1));
        assertNotEquals(0, expectedUpdateSelectionCalls.size());
        int updateResult = expectedUpdateSelectionCalls.get(expectedUpdateSelectionCalls.size() - 1);
        boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
        boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
        assertEquals(lastUpdateExpected, !updateImpactedSelection);
    }

    private void verifyActualText(final String text, final String composingText) {
        assertEquals(text, fakeInputConnection.getText());
        assertEquals(composingText, fakeInputConnection.getComposingText());
    }

    private static String getSurrogatePairString(final int index, final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.appendCodePoint(getSurrogatePair(index + i));
        }
        return sb.toString();
    }

//    private static int getSurrogatePair(final int index) {
//        // high: U+D800 to U+DBFF
//        // low:  U+DC00 to U+DFFF
//        return ("" + (char)('\uD800' + index) + (char)('\uDC00' + index)).codePointAt(0);
//    }
//
//    private static int getSurrogatePair2(final int index) {
//        // high: U+D800 to U+DBFF 55296 - 56319 (1024)
//        // low:  U+DC00 to U+DFFF 56320 - 57343 (1024)
////        return ("" + (char)('\uD800' + index) + (char)('\uDC00' + index)).codePointAt(0);
//        return 66561 + 1025 * index;
//    }

    private static int getSurrogatePair(int index) {
        // high surrogates: U+D800 to U+DBFF (1024 characters)
        // low surrogates:  U+DC00 to U+DFFF (1024 characters)
        index = index % 1024;
        if (index < 0) {
            index += 1024;
        }
        return getSurrogatePairCodePoint(HI_SURROGATE_START + index, LO_SURROGATE_START + index);
    }

    final static int HI_SURROGATE_START = 0xD800;
    final static int LO_SURROGATE_START = 0xDC00;
    final static int SURROGATE_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;
    private static int getSurrogatePairCodePoint(final int lead, final int trail) {
        return (lead << 10) + trail + SURROGATE_OFFSET;
    }

    private static class UpdateSelectionCall {
        public final int oldSelStart;
        public final int oldSelEnd;
        public final int newSelStart;
        public final int newSelEnd;
        public final int candidatesStart;
        public final int candidatesEnd;

        public UpdateSelectionCall(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
            this.oldSelStart = oldSelStart;
            this.oldSelEnd = oldSelEnd;
            this.newSelStart = newSelStart;
            this.newSelEnd = newSelEnd;
            this.candidatesStart = candidatesStart;
            this.candidatesEnd = candidatesEnd;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof UpdateSelectionCall)) {
                return false;
            }
            final UpdateSelectionCall other = (UpdateSelectionCall)o;
            return oldSelStart == other.oldSelStart
                    && oldSelEnd == other.oldSelEnd
                    && newSelStart == other.newSelStart
                    && newSelEnd == other.newSelEnd
                    && candidatesStart == other.candidatesStart
                    && candidatesEnd == other.candidatesEnd;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int hash = 1;
            hash = hash * prime + oldSelStart;
            hash = hash * prime + oldSelEnd;
            hash = hash * prime + newSelStart;
            hash = hash * prime + newSelEnd;
            hash = hash * prime + candidatesStart;
            hash = hash * prime + candidatesEnd;
            return hash;
        }

        @Override
        public String toString() {
            return "oss=" + oldSelStart + ", ose=" + oldSelEnd
                    + ", nss=" + newSelStart + ", nse=" + newSelEnd
                    + ", cs=" + candidatesStart + ", ce=" + candidatesEnd;
        }
    }
}
