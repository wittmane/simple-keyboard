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

//import android.inputmethodservice.InputMethodService;
//import android.text.TextUtils;
//import android.view.KeyEvent;
//import android.view.View;
//import android.view.inputmethod.ExtractedText;
//import android.view.inputmethod.InputConnection;
//
//import org.junit.Test;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.MethodSource;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Stream;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertNotEquals;
//import static org.junit.Assert.assertTrue;
//import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.UPDATE_IMPACTED_SELECTION;
//import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.UPDATE_WAS_EXPECTED;

public class RichInputConnectionTestsV2 {
//    private final ArrayList<UpdateSelectionCall> updateSelectionCalls = new ArrayList<>();
//    private final ArrayList<Integer> expectedUpdateSelectionCalls = new ArrayList<>();
//    private FakeInputConnection fakeInputConnection;
//    private RichInputConnection richInputConnection;
//
//    @BeforeEach
//    public void setup() {
//        System.out.println("Base setup");
//        updateSelectionCalls.clear();
//        expectedUpdateSelectionCalls.clear();
//        richInputConnection = new RichInputConnection(new InputMethodService() {
//            @Override
//            public InputConnection getCurrentInputConnection() {
//                return fakeInputConnection;
//            }
//        });
//        final FakeInputConnection.FakeInputMethodManager fakeInputMethodManager = new FakeInputConnection.FakeInputMethodManager() {
//            @Override
//            public void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
//                updateSelectionCalls.add(new UpdateSelectionCall(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
//                expectedUpdateSelectionCalls.add(richInputConnection.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
//            }
//
//            @Override
//            public void updateExtractedText(View view, int token, ExtractedText text) {
//                if (richInputConnection != null) {
//                    richInputConnection.onUpdateExtractedText(token, text);
//                }
//            }
//
//            @Override
//            public void processUpdates() {
//            }
//        };
//        //TODO: probably don't do this here so alternative parameters can be used
//        fakeInputConnection = new FakeInputConnection(fakeInputMethodManager);
//    }
//
//    public void setup(final FakeInputConnectionSettings settings, final String initialText,
//                      final int initialCursorStart, final int initialCursorEnd) {
//        richInputConnection = null;
//        updateSelectionCalls.clear();
//        expectedUpdateSelectionCalls.clear();
//        final FakeInputConnection.FakeInputMethodManager fakeInputMethodManager = new FakeInputConnection.FakeInputMethodManager() {
//            @Override
//            public void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
//                if (richInputConnection != null) {
//                    updateSelectionCalls.add(new UpdateSelectionCall(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
//                    expectedUpdateSelectionCalls.add(richInputConnection.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
//                }
//            }
//
//            @Override
//            public void updateExtractedText(View view, int token, ExtractedText text) {
//                if (richInputConnection != null) {
//                    richInputConnection.onUpdateExtractedText(token, text);
//                }
//            }
//
//            @Override
//            public void processUpdates() {
//            }
//        };
//        if (settings == null) {
//            fakeInputConnection = new FakeInputConnection(fakeInputMethodManager, initialText,
//                    initialCursorStart, initialCursorEnd);
//        } else {
//            fakeInputConnection = new FakeInputConnection(fakeInputMethodManager, initialText,
//                    initialCursorStart, initialCursorEnd,
//                    settings.getTextLimit, settings.setComposingRegionSupported,
//                    settings.getSelectedTextSupported,
//                    settings.deleteAroundComposingText, settings.keepEmptyComposingPosition,
//                    true, Integer.MAX_VALUE, true, true);
//            if (settings.textModifier != null) {
//                fakeInputConnection.getSettings().setTextModifier(settings.textModifier);
//            }
//        }
//
//        richInputConnection = new RichInputConnection(new InputMethodService() {
//            @Override
//            public InputConnection getCurrentInputConnection() {
//                return fakeInputConnection;
//            }
//        });
//    }
//
//    @Test
//    public void debuggingTest() {
//        System.out.println("debuggingTest");
//        System.out.println("smile: " + new StringBuilder().appendCodePoint(0x1F642));
//        System.out.println(getSurrogatePairString(0, 1024));
////        for (int i = 0; i < 4000; i++) {
////            System.out.println(i + ": " + getSurrogatePairString(i));
////        }
//        for (int i = 0; i < simplePrintableSurrogatePairIndices.length; i++) {
//            System.out.println(i + ": " + getSurrogatePairString(simplePrintableSurrogatePairIndices[i]));
//        }
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(5),
////                new ComposedState("", "example", "", 6, 6),
////                new AddText("a", false, -1),
////                new CommittedState("a", 0, 0));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(5),
////                new ComposedState("", "example", "", 0, 0),
////                new AddText("a", false, 2),
////                new CommittedState("a", 1, 1));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(5),
////                new ComposedState("", "a", "Lorem ipsum dolor sit amet", 26, 26),
////                new AddText("a", false, -1),
////                new CommittedState("aLorem ipsum dolor sit amet", 0, 0));
////        final ActionTestCase<DeleteTextBefore> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(false, false),
////                new ComposedState("", "test", "", 3, 3),
////                new DeleteTextBefore(2),
////                new ComposedState("", "tt", "", 1, 1));
////        final ActionTestCase<DeleteTextBefore> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(false, false),
////                new ComposedState("", "test", "", 3, 3),
////                new DeleteTextBefore(2),
////                new ComposedState("", "tt", "", 1, 1));
////        final ActionTestCase<DeleteTextBefore> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(false, false),
////                new CommittedState("Lorem ipsum dolor sit amet", 26, 26),
////                new DeleteTextBefore(1),
////                new CommittedState("Lorem ipsum dolor sit ame", 25, 25));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(false, false),
////                new CommittedState("Lorem ipsum dolor sit amet", 26, 26),
////                new AddText("a", false, -1),
////                new CommittedState("Lorem ipsum dolor sit ameta", 25, 25));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(false, false),
////                new CommittedState("Lorem ipsum dolor sit ameta", 26, 27),
////                new AddText("a", true, -1),
////                new ComposedState("Lorem ipsum dolor sit amet", "a", "", 25, 25));
////        final ActionTestCase<SendDeleteKey> params = new ActionTestCase<>("delete surrogate pair",
////                new CommittedState("Lorem ipsum dolor" + getSurrogatePairString(0), " sit amet"),
////                new SendDeleteKey(),
////                new CommittedState("Lorem ipsum dolor", " sit amet"));
////        final ActionTestCase<SendDeleteKey> params = new ActionTestCase<>("delete selection of surrogate pairs through the beginning of the composition",
////                new ComposedState("Lorem ipsum " + getSurrogatePairString(0), getSurrogatePairString(1) + "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION, ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.BEGINNING_OF_COMPOSITION));
////        final ActionTestCase<DeleteTextBefore> params = new ActionTestCase<>("delete composing text",
////                new FakeInputConnectionSettings(true, true),
////                new ComposedState("qw", "e", "rty", ComposedTextPosition.END_OF_COMPOSITION),
////                new DeleteTextBefore(1),
////                new CommittedState("qw", "rty"));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                (FakeInputConnectionSettings) null,
////                true,
////                new CommittedState("", "example", ""),
////                false,
////                new AddText("a", false, -1),
////                new CommittedState("a", 0, 0));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                (FakeInputConnectionSettings) null,
////                true,
////                new ComposedState("Lorem ipsum d", "o", "lor sit amet"),
////                false,
////                new AddText("Lorem ipsum dolor sit amet", false, 3),
////                new CommittedState("Lorem ipsum dLorem ipsum dolor sit ametlo", "r sit amet"));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                (FakeInputConnectionSettings) null,
////                true,
////                new ComposedState("Lorem ipsum d", "o", "lor sit amet"),
////                false,
////                new AddText("", false, 12),
////                new CommittedState("Lorem ipsum dlor sit ame", "t"));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                (FakeInputConnectionSettings) null,
////                false,
////                new CommittedState("Lorem ipsum d", "o", "lor sit amet"),
////                true,
////                new AddText("", false, 12),
////                new CommittedState("Lorem ipsum dlor sit ame", "t"));
////        final ActionTestCase<AddText> params = new ActionTestCase<>("debugging test",
////                (FakeInputConnectionSettings) null,
////                false,
////                new ComposedState("Lorem ipsum d", "o", "lor sit amet", 12, 15),
////                true,
////                new AddText("", false, -11),
////                new CommittedState("Lo", "rem ipsum dlor sit amet"));
////        final ActionTestCase<SendDeleteKey> params = new ActionTestCase<>("debugging test",
////                (FakeInputConnectionSettings) null,
////                false,
////                new CommittedState("Lorem ipsum", " ", "dolor sit amet"),
////                true,
////                new SendDeleteKey(),
////                new CommittedState("Lorem ipsum", "dolor sit amet"));
////        final ActionTestCase<DeleteTextBefore> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(false, false),
////                false,
////                new ComposedState("Lorem ips", "um do", "lor sit amet", 12),
////                true,
////                new DeleteTextBefore(1),
////                new ComposedState("Lorem ips", "umdo", "lor sit amet", 11));
////        final ActionTestCase<DeleteTextBefore> params = new ActionTestCase<>("debugging test",
////                new FakeInputConnectionSettings(false, false),
////                false,
////                new ComposedState("Lo", "rem ips", "um dolor sit amet", 12),
////                true,
////                new DeleteTextBefore(1),
////                new ComposedState("Lo", "rem ips", "umdolor sit amet", 11));
////        final ActionTestCase<AddText> params = new ActionTestCase<>(
////                    "input connection adds extra characters with the new cursor position after the committed text",
////                    new FakeInputConnectionSettings(new FakeInputConnection.DoubleTextModifier()),
////                    true,
////                    new CommittedState("Lorem ipsum dol", "or sit amet"),
////                    true,
////                    new AddText("asdf", true, 1),
////                    new ComposedState("Lorem ipsum dol", "aassddff", "or sit amet"),
////                    false);
////        final ActionTestCase<AddText> params = new ActionTestCase<>(
////                    "finalize whole composed normal text with nothing",
////                    new FakeInputConnectionSettings(),
////                    false,
////                    new ComposedState("", "Lorem ipsum dolor sit amet", ""),
////                    true,
////                    new AddText("", false, 1),
////                    new CommittedState("", ""),
////                    true);
////        final ActionTestCase<AddText> params = new ActionTestCase<>(
////                    "debug",
////                    new FakeInputConnectionSettings(5),
////                    false,
////                    new ComposedState("", "example", "", 0),
////                    true,
////                    new AddText("a", false, 2),
////                    new CommittedState("a", ""),
////                    true);
//        final ActionTestCase<DeleteTextBefore> params = new ActionTestCase<>(
//                    "debug",
//                    new FakeInputConnectionSettings(false, false),
//                    false,
//                    new ComposedState("Lorem ipsum dolor sit amet", "a", "", 26),
//                    true,
//                    new DeleteTextBefore(27),
//                    new ComposedState("", "a", "", 0),
//                    true);
//        System.out.println("Debug test: " + params);
//        testAction2(params);
////        testAction(params);
//
////        final Named<String> stringOption = new Named<>("multiple regular characters",
////                "asdf");
////        final String before = "Lorem ips";
////        final String selection = "um dolor";
////        final String after = " sit amet";
////        final int chars = stringOption.data.codePointCount(0, stringOption.data.length());
////        final int expectedUnicodeSteps = stringOption.data.length();
////        final UnicodeStepParams params = new UnicodeStepParams(stringOption + " before cursor end extending past selection",
////                before + selection + stringOption.data.substring(0, getCharCountBeforeCodePoint(stringOption.data, -1)),
////                stringOption.data.substring(stringOption.data.length() - getCharCountAfterCodePoint(stringOption.data, -1)),
////                after,
////                -chars, false, -expectedUnicodeSteps);
////        testGetUnicodeSteps(params.text.left, params.text.center, params.text.right, params.chars, params.rightSidePointer, params.expectedUnicodeSteps);
//    }
//
////    @Test
////    public void exampleTest() {
////        System.out.println("Base exampleTest");
////    }
////
////    @Nested
////    @DisplayName("Given a normal input connection")
////    public class NormalInputConnection {
////        @BeforeEach
////        public void setup() {
////            System.out.println("Case1 setup");
////        }
////
////        @Nested
////        @DisplayName("When Action1")
////        public class Action1 {
////            @BeforeEach
////            public void setup() {
////                System.out.println("Case1 Action1 setup");
////            }
////
////            @Test
////            @DisplayName("Then Result1")
////            public void exampleTest1() {
////                System.out.println("Case1 Action1 Result1");
////            }
////
////            @Test
////            @DisplayName("Then Result2")
////            public void exampleTest2() {
////                System.out.println("Case1 Action1 Result2");
////            }
////        }
////    }
//
//    // given: text in input connection, existing composed text, text selection
//    // when: commit/compose text, new text, cursor position
//    // then: is state expected, was text added to input connection, maybe check some other methods to verify internal state although maybe those should have separate testing
//    // this case has multiple givens and whens, but the nested class structure only supports
//    // parameterized test methods (I think), so this may not work great. we can break things into a
//    // few groups of general whens and thens and just have the individual tests have multiple
//    // specific values for each when and then. it's possible this structure isn't the best for these
//    // types of parameterized tests, but it still might be helpful for simple grouping purposes.
//    // it might be worth looking into other options too, such as dynamic tests
//
//
//    //#region specific tests
//
//    //#region reset caches
//
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("resetCaches_parameters")
//    public void resetCaches(final Named<ThreePartText> params) {
//        System.out.println("ParameterizedTest resetCaches");
//        testResetCachesInitial(params.data.left, params.data.center, params.data.right);
//    }
//    private void testResetCachesInitial(final String before, final String selection, final String after) {
//        final int start = before.length();
//        final int end = before.length() + selection.length();
//        setup(null, before + selection + after, start, end);
//
//        richInputConnection.resetState(start, end);
//        richInputConnection.reloadCachesForStartingInputView();
//
//        //TODO: these rely on getExpectedSelectionStart, getExpectedSelectionEnd, getTextBeforeCursor, getSelectedText, getTextAfterCursor
//        // is this appropriate? should these tests be expanded to cover all functions (not just named resetCaches)?
//        // should this test less (just getExpectedSelectionStart, getExpectedSelectionEnd)?
//        // should this be split into test for each of these and not have a specific test for resetCachesUponCursorMoveAndReturnSuccess?
//        verifySelection(start, end);
//        verifyText(before, selection, after);
//    }
//
//    private static Stream<Named<ThreePartText>> resetCaches_parameters() {
//        final List<Named<ThreePartText>> list = new ArrayList<>();
//
//        list.add(new Named<>("no text",
//                new ThreePartText("", "", "")));
//        list.add(new Named<>("cursor at beginning of text",
//                new ThreePartText("", "", "Lorem ipsum dolor sit amet")));
//        list.add(new Named<>("cursor in middle of text",
//                new ThreePartText("Lorem ipsum", "", " dolor sit amet")));
//        list.add(new Named<>("cursor at end of text",
//                new ThreePartText("Lorem ipsum dolor sit amet", "", "")));
//        list.add(new Named<>("cursor selection in middle of text",
//                new ThreePartText("Lorem ipsum ", "dolor", " sit amet")));
//        list.add(new Named<>("cursor selection at beginning of text",
//                new ThreePartText("", "Lorem", " ipsum dolor sit amet")));
//        list.add(new Named<>("cursor selection at end of text",
//                new ThreePartText("Lorem ipsum dolor sit ", "amet", "")));
//        list.add(new Named<>("cursor selection around whole text",
//                new ThreePartText("", "Lorem ipsum dolor sit amet", "")));
//
//        //TODO: add surrogate pairs
//
//        return list.stream();
//    }
//
//    private void verifySelection(final int selectionStart, final int selectionEnd) {
//        assertEquals(selectionStart, richInputConnection.getExpectedSelectionStart());
//        assertEquals(selectionEnd, richInputConnection.getExpectedSelectionEnd());
//    }
//    private void verifyText(final String beforeCursor, final String selected, final String afterCursor) {
//        assertEquals(beforeCursor, richInputConnection.getTextBeforeCursor(Integer.MAX_VALUE, 0).toString());
//        CharSequence selectedText = richInputConnection.getSelectedText(0);
//        //TODO: (EW) I'm not sure that null necessarily means the same as actually no selection
//        assertEquals(selected, selectedText == null ? "" : selectedText.toString());
//        assertEquals(afterCursor, richInputConnection.getTextAfterCursor(Integer.MAX_VALUE, 0).toString());
//    }
//    private void verifyTextCache(final String beforeCursor, final String selected, final String afterCursor) {
//        // block the rich input connection from getting the real text so it's forced to only return
//        // whatever it might have cached
//        fakeInputConnection.allowGettingText(false);
//
//        String cachedBefore = "";
//        for (int i = 0; i < beforeCursor.length(); i++) {
//            String tempBefore = richInputConnection.getTextBeforeCursor(i, 0).toString();
//            if (tempBefore.length() < i) {
//                // cache must have run out - the previously returned value must be the full cache
//                break;
//            }
//            cachedBefore = tempBefore;
//        }
//        assertEquals(beforeCursor.substring(beforeCursor.length() - cachedBefore.length()), cachedBefore);
//
//        CharSequence cachedSelected = richInputConnection.getSelectedText(0);
//        if (isEmpty(selected)) {
//            assertTrue(isEmpty(cachedSelected));
//        } else if (!isEmpty(cachedSelected)) {
//            assertEquals(selected, cachedSelected.toString());
//        }
//
//        String cachedAfter = "";
//        for (int i = 0; i < afterCursor.length(); i++) {
//            String tempAfter = richInputConnection.getTextAfterCursor(i, 0).toString();
//            if (tempAfter.length() < i) {
//                // cache must have run out - the previously returned value must be the full cache
//                break;
//            }
//            cachedAfter = tempAfter;
//        }
//        assertEquals(afterCursor.substring(0, cachedAfter.length()), cachedAfter);
//
//        fakeInputConnection.allowGettingText(false);
//    }
//    private static boolean isEmpty(final CharSequence s) {
//        return s == null || s.length() == 0;
//    }
////    private static boolean isEmpty(final String s) {
////        return s == null || s.isEmpty();
////    }
//    //#endregion
//
//    //#region get unicode steps
//
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("getUnicodeSteps_parameters")
//    public void getUnicodeSteps(final UnicodeStepParams params) {
//        System.out.println("ParameterizedTest getUnicodeSteps");
//        testGetUnicodeSteps(params.text.left, params.text.center, params.text.right, params.chars, params.rightSidePointer, params.expectedUnicodeSteps);
//    }
//    private void testGetUnicodeSteps(final String before, final String selection, final String after,
//                                     final int chars, final boolean rightSidePointer, final int expectedUnicodeSteps) {
//        final int start = before.length();
//        final int end = before.length() + selection.length();
//        printState(new CommittedState(before + selection + after, start, end), 0);
//        setup(null, before + selection + after, start, end);
//        richInputConnection.resetState(start, end);
//        richInputConnection.reloadCachesForStartingInputView();
//
//        final int unicodeSteps = richInputConnection.getUnicodeSteps(chars, rightSidePointer);
//
//        assertEquals(expectedUnicodeSteps, unicodeSteps);
//    }
//
//    private static Stream<UnicodeStepParams> getUnicodeSteps_parameters() {
//        final List<UnicodeStepParams> list = new ArrayList<>();
//
//        final String before = "Lorem ips";
//        final String selection = "um dolor";
//        final String after = " sit amet";
//        for (final Named<String> stringOption : getUnicodeStepStringOptions()) {
//            //TODO: these names seem flipped, but I think getUnicodeSteps is actually named incorrectly
//            final int chars = stringOption.data.codePointCount(0, stringOption.data.length());
//            final int expectedUnicodeSteps = stringOption.data.length();
//
//            list.add(new UnicodeStepParams(stringOption + " before cursor start",
//                    before + stringOption.data, selection, after,
//                    -chars, false, -expectedUnicodeSteps));
//            list.add(new UnicodeStepParams(stringOption + " after cursor start",
//                    before, stringOption.data + selection, after,
//                    chars, false, expectedUnicodeSteps));
//
//            if (expectedUnicodeSteps > 1) {
//                list.add(new UnicodeStepParams(stringOption + " after cursor start extending past selection",
//                        before,
//                        stringOption.data.substring(0, getCharCountBeforeCodePoint(stringOption.data, 1)),
//                        stringOption.data.substring(stringOption.data.length() - getCharCountAfterCodePoint(stringOption.data, 1)) + selection + after,
//                        chars, false, expectedUnicodeSteps));
//            }
//
//            list.add(new UnicodeStepParams(stringOption + " after cursor start without selection",
//                    before, "", stringOption.data + selection + after,
//                    chars, false, expectedUnicodeSteps));
//
//            list.add(new UnicodeStepParams(stringOption + " before cursor end without selection",
//                    before + selection + stringOption.data, "", after,
//                    -chars, true, -expectedUnicodeSteps));
//
//
//            if (expectedUnicodeSteps > 1) {
//                list.add(new UnicodeStepParams(stringOption + " before cursor end extending past selection",
//                        before + selection + stringOption.data.substring(0, getCharCountBeforeCodePoint(stringOption.data, -1)),
//                        stringOption.data.substring(stringOption.data.length() - getCharCountAfterCodePoint(stringOption.data, -1)),
//                        after,
//                        -chars, true, -expectedUnicodeSteps));
//            }
//
//            list.add(new UnicodeStepParams(stringOption + " before cursor end",
//                    before, selection + stringOption.data, after,
//                    -chars, true, -expectedUnicodeSteps));
//            list.add(new UnicodeStepParams(stringOption + " after cursor end",
//                    before, selection, stringOption.data + after,
//                    chars, true, expectedUnicodeSteps));
//        }
//
//        return list.stream();
//    }
//    private static List<Named<String>> getUnicodeStepStringOptions() {
//        final List<Named<String>> list = new ArrayList<>();
//
//        list.add(new Named<>("single regular character",
//                "a"));
//        list.add(new Named<>("single surrogate pair",
//                getSurrogatePairString(0)));
//        list.add(new Named<>("multiple regular characters",
//                "asdf"));
//        list.add(new Named<>("multiple surrogate pairs",
//                getSurrogatePairString(0, 4)));
//        list.add(new Named<>("mixed surrogate pairs and normal characters",
//                "a" + getSurrogatePairString(0) + "b" + getSurrogatePairString(1)));
//        list.add(new Named<>("no character",
//                ""));
//
//        return list;
//    }
//
//    private static class UnicodeStepParams {
//        final String name;
//        final ThreePartText text;
//        final int chars;
//        final boolean rightSidePointer;
//        final int expectedUnicodeSteps;
//        public UnicodeStepParams(final String name, final ThreePartText text, final int chars, final boolean rightSidePointer, final int expectedUnicodeSteps) {
//            this.name = name;
//            this.text = text;
//            this.chars = chars;
//            this.rightSidePointer = rightSidePointer;
//            this.expectedUnicodeSteps = expectedUnicodeSteps;
//        }
//        public UnicodeStepParams(final String name, final String before, final String selection, final String after, final int chars, final boolean rightSidePointer, final int expectedUnicodeSteps) {
//            this(name, new ThreePartText(before, selection, after), chars, rightSidePointer, expectedUnicodeSteps);
//        }
//
//        @Override
//        public String toString() {
//            return name;
//        }
//    }
//    //#endregion
//
//    //#region commit/compose tests
//
//    // test cases:
//    // start composing text
//    //    single cursor     -> insert composing text
//    //    selection         -> replace with composing text
//    // update composing text
//    //    single cursor     -> update composing text
//    //    selection         -> update composing text
//    // commit text without a composition
//    //    single cursor     -> insert text
//    //    selection         -> replace text
//    // commit text with a composition
//    //    single cursor     -> replace composing text
//    //    selection         -> replace composing text
//
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("commit_addNewText_parameters")
//    public void commit_addNewText(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest commit_addNewText");
//        testAction2(params);
//    }
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("commit_replaceSelectedText_parameters")
//    public void commit_replaceSelectedText(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest commit_replaceSelectedText");
//        testAction2(params);
//    }
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("commit_commitOverComposingTextWithoutSelection_parameters")
//    public void commit_commitOverComposingTextWithoutSelection(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest commit_commitOverComposingTextWithoutSelection");
//        testAction2(params);
//    }
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("commit_commitOverComposingTextWithSelection_parameters")
//    public void commit_commitOverComposingTextWithSelection(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest commit_commitOverComposingTextWithSelection");
//        testAction2(params);
//    }
//
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("compose_newText_parameters")
//    public void compose_newText(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest compose_newText");
//        testAction2(params);
//    }
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("compose_replaceSelectedText_parameters")
//    public void compose_replaceSelectedText(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest compose_replaceSelectedText");
//        testAction2(params);
//    }
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("compose_replaceComposingTextWithoutSelection_parameters")
//    public void compose_replaceComposingTextWithoutSelection(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest compose_replaceComposingTextWithoutSelection");
//        testAction2(params);
//    }
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("compose_replaceComposingTextWithSelection_parameters")
//    public void compose_replaceComposingTextWithSelection(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest compose_replaceComposingTextWithSelection");
//        testAction2(params);
//    }
//
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("commit_parameters")
//    public void commit(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest commit");
//        testAction2(params);
//    }
//
//    //@DisplayName("Test with @MethodSource")
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("compose_parameters")
//    public void compose(final ActionTestCase<AddText> params) {
//        System.out.println("ParameterizedTest compose");
//        testAction2(params);
//    }
//
//    //#region commit/compose parameters
//    //#region commit parameters
//    private static Stream<ActionTestCase<AddText>> commit_addNewText_parameters() {
//        return getAddTextParams(false, false, false);
//    }
//    private static Stream<ActionTestCase<AddText>> commit_replaceSelectedText_parameters() {
//        return getAddTextParams(false, false, true);
//    }
//    private static Stream<ActionTestCase<AddText>> commit_commitOverComposingTextWithoutSelection_parameters() {
//        return getAddTextParams(false, true, false);
//    }
//    private static Stream<ActionTestCase<AddText>> commit_commitOverComposingTextWithSelection_parameters() {
//        return getAddTextParams(false, true, true);
//    }
//    //#endregion
//
//    //#region compose parameters
//    private static Stream<ActionTestCase<AddText>> compose_newText_parameters() {
//        return getAddTextParams(true, false, false);
//    }
//    private static Stream<ActionTestCase<AddText>> compose_replaceSelectedText_parameters() {
//        return getAddTextParams(true, false, true);
//    }
//    private static Stream<ActionTestCase<AddText>> compose_replaceComposingTextWithoutSelection_parameters() {
//        return getAddTextParams(true, true, false);
//    }
//    private static Stream<ActionTestCase<AddText>> compose_replaceComposingTextWithSelection_parameters() {
//        return getAddTextParams(true, true, true);
//    }
//    //#endregion
//
//    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
//                                                   final Named<TestSettings> settingsOption, final int initialCursorStart,
//                                                   final int initialCursorEnd, final int initialCompositionStart,
//                                                   final int initialCompositionEnd, final Named<String> addedTextOption) {
//        if (newCursorPositionOption.newCursorPosition > 1
//                && settingsOption.data.inputConnectionSettings.getTextLimit < Integer.MAX_VALUE
//                && settingsOption.data.initialCursorPositionKnown) {
//            final int maxKnownIndex = Math.max(
//                    initialCursorEnd + settingsOption.data.inputConnectionSettings.getTextLimit,
//                    initialCompositionEnd);
//            final int replacingTextLength = initialCompositionStart == -1 && initialCompositionEnd == -1
//                    ? initialCursorEnd - initialCursorStart
//                    : initialCompositionEnd - initialCompositionStart;
//            final int changedTextLength = addedTextOption.data.length() - replacingTextLength;
//            if (maxKnownIndex + changedTextLength < newCursorPositionOption.newCursorIndex) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
//                                                   final Named<TestSettings> settingsOption,
//                                                   final int initialCursorEnd, final int initialCompositionStart,
//                                                   final int initialCompositionEnd, final Named<String> addedTextOption) {
//        //TODO: clean this up. initialCursorEnd is getting passed as the initialCursorStart, even
//        // though it may not be, but it isn't actually relevant for the composing case
//        return isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, initialCursorEnd,
//                initialCursorEnd, initialCompositionStart, initialCompositionEnd, addedTextOption);
//    }
//
//    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
//                                                   final Named<TestSettings> settingsOption,
//                                                   final int initialCursorPosition, final Named<String> addedTextOption) {
//        return isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
//                initialCursorPosition, initialCursorPosition, -1, -1, addedTextOption);
//    }
//
//    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
//                                                   final Named<TestSettings> settingsOption,
//                                                   final int initialCursorStart, final int initialCursorEnd,
//                                                   final Named<String> addedTextOption) {
//        return isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, initialCursorStart,
//                initialCursorEnd, -1, -1, addedTextOption);
//    }
//
//    private static NewCursorPositionParams getNewCursorPosition(final String left,
//                                                                final int newTextLength,
//                                                                final String right,
//                                                                final AddingTextPosition referencePoint,
//                                                                final int codePointOffset) {
//        final String offsetName;
//        if (codePointOffset < 0) {
//            offsetName = Math.abs(codePointOffset) + " before the ";
//        } else if (codePointOffset > 0) {
//            offsetName = Math.abs(codePointOffset) + " after the ";
//        } else {
//            offsetName = "the ";
//        }
//        final String referenceName;
//        final int newCursorPosition;
//        final int newCursorIndex;
//        switch (referencePoint) {
//            case BEGINNING_OF_FULL_TEXT:
//                referenceName = "beginning of the full text";
//                if (codePointOffset <= 0) {
//                    newCursorPosition = -left.length() + codePointOffset;
//                    //TODO: should the index allow going negative?
//                    newCursorIndex = 0;
//                } else {
//                    newCursorPosition = -getCharCountAfterCodePoint(left, codePointOffset);
//                    newCursorIndex = getCharCountBeforeCodePoint(left, codePointOffset);
//                }
//                break;
//            case BEGINNING_OF_NEW_TEXT:
//                referenceName = "new text";
//                if (codePointOffset > 0) {
//                    throw new IllegalArgumentException("can't reference inside the new text");
//                }
//                //TODO: should this handle going past the end of the full text?
//                if (codePointOffset < 0) {
//                    newCursorPosition = -getCharCountAfterCodePoint(left, codePointOffset);
//                    newCursorIndex = getCharCountBeforeCodePoint(left, codePointOffset);
//                } else {
//                    newCursorPosition = 0;
//                    newCursorIndex = left.length();
//                }
//                break;
//            case END_OF_NEW_TEXT:
//                referenceName = "new text";
//                if (codePointOffset < 0) {
//                    throw new IllegalArgumentException("can't reference inside the new text");
//                }
//                //TODO: should this handle going past the end of the full text?
//                newCursorPosition = 1 + getCharCountBeforeCodePoint(right, codePointOffset);
//                newCursorIndex = left.length() + newTextLength + getCharCountBeforeCodePoint(right, codePointOffset);
//                break;
//            case END_OF_FULL_TEXT:
//                referenceName = "end of the full text";
//                if (codePointOffset < 0) {
//                    newCursorPosition = 1 + getCharCountBeforeCodePoint(right, codePointOffset);
//                    newCursorIndex = left.length() + newTextLength + getCharCountBeforeCodePoint(right, codePointOffset);
//                } else {
//                    newCursorPosition = 1 + right.length() + codePointOffset;
//                    //TODO: should the index allow going past the end of the text?
//                    newCursorIndex = left.length() + newTextLength + right.length();
//                }
//                break;
//            default:
//                throw new IllegalArgumentException("unknown enum: " + referencePoint);
//        }
//        return new NewCursorPositionParams(offsetName + referenceName, newCursorPosition,
//                newCursorIndex);
//    }
//
//    private static NewCursorPositionParams[] getNewCursorPositionOptions(
//            final String left, final int newTextLength, final String right,
//            final RelativePosition[] positions) {
//        final List<NewCursorPositionParams> list = new ArrayList<>();
//        for (final RelativePosition position : positions) {
//            final NewCursorPositionParams newItem = getNewCursorPosition(left, newTextLength, right, position.referencePoint, position.codePointOffset);
//            // only add this if it is unique
//            boolean itemExists = false;
//            for (int i = 0; i < list.size(); i++) {
//                final NewCursorPositionParams existingItem = list.get(i);
//                if (existingItem.newCursorPosition == newItem.newCursorPosition
//                        && existingItem.newCursorIndex == newItem.newCursorIndex) {
//                    // update the name of the existing case
//                    list.set(i, new NewCursorPositionParams(
//                            existingItem.name + " / " + newItem.name,
//                            existingItem.newCursorPosition, existingItem.newCursorIndex));
//                    itemExists = true;
//                    break;
//                }
//            }
//            if (!itemExists) {
//                list.add(newItem);
//            }
//        }
//        //TODO: probably don't make this an array
//        return list.toArray(new NewCursorPositionParams[0]);
//    }
//
//    //#region test case sources
//    private static AddTextOption[] getAddTextOptions() {
//        final Named<String> initialNoText =
//                new Named<>("no text", "");
//        final Named<String> initialNormalText =
//                new Named<>("normal text", "Lorem ipsum dolor sit amet");
//        final Named<String> initialSurrogatePairs =
//                new Named<>("surrogate pairs", getSurrogatePairString(0, 26));
//        final Named<String> initialSurrogatePairsAndNormalText =
//                new Named<>("surrogate pairs and normal text",
//                        getAlternatingSurrogatePairString(0, 'a', 26));
//
//        final Named<String> addingNothing =
//                new Named<>("nothing", "");
//        final Named<String> addingNormalCharacter =
//                new Named<>("normal character", "a");
//        final Named<String> addingSurrogatePair =
//                new Named<>("surrogate pair", getSurrogatePairString(0));
//        final Named<String> addingNormalCharactersAndSurrogatePair =
//                new Named<>("normal characters and a surrogate pair",
//                        "test" + getSurrogatePairString(1));
//
//        //TODO: see if we can reduce these options a bit more
//        return new AddTextOption[] {
//                new AddTextOption(initialNoText, addingNothing),
//                new AddTextOption(initialNoText, addingNormalCharacter),
//                new AddTextOption(initialNoText, addingSurrogatePair),
//                new AddTextOption(initialNormalText, addingNothing),
//                new AddTextOption(initialNormalText, addingNormalCharacter),
//                new AddTextOption(initialNormalText, addingSurrogatePair),
//                new AddTextOption(initialNormalText, addingNormalCharactersAndSurrogatePair),
//                new AddTextOption(initialSurrogatePairs, addingNormalCharacter),
//                new AddTextOption(initialSurrogatePairs, addingSurrogatePair),
//                new AddTextOption(initialSurrogatePairsAndNormalText, addingNormalCharactersAndSurrogatePair),
//
////                new AddTextOption(initialNoText, addingNormalCharactersAndSurrogatePair),
////                new AddTextOption(initialSurrogatePairs, addingNothing),
////                new AddTextOption(initialSurrogatePairs, addingNormalCharactersAndSurrogatePair),
////                new AddTextOption(initialSurrogatePairsAndNormalText, addingNothing),
////                new AddTextOption(initialSurrogatePairsAndNormalText, addingNormalCharacter),
////                new AddTextOption(initialSurrogatePairsAndNormalText, addingSurrogatePair),
//        };
//    }
//
//    private static NewCursorPositionParams[] getInsertTextNewCursorPositions(final String leftHalf,
//                                                                             final int addedTextLength,
//                                                                             final String rightHalf) {
//        return getNewCursorPositionOptions(leftHalf, addedTextLength, rightHalf,
//                new RelativePosition[] {
//                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, -1),
//                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, 1),
//                        new RelativePosition(AddingTextPosition.BEGINNING_OF_NEW_TEXT, 0),
//                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 1),
//                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, 0)
//                });
//    }
//    private static NewCursorPositionParams[] getReplaceSelectionNewCursorPositions(final String leftOfSelection,
//                                                                                   final int addedTextLength,
//                                                                                   final String rightOfSelection) {
//        return getNewCursorPositionOptions(leftOfSelection, addedTextLength, rightOfSelection,
//                new RelativePosition[] {
//                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, 0),
//                        new RelativePosition(AddingTextPosition.BEGINNING_OF_NEW_TEXT, -1),
//                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 0),
//                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, -1),
//                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, 1)
//                });
//    }
//    private static NewCursorPositionParams[] getUpdateCompositionNewCursorPositions(final String leftOfComposed,
//                                                                                    final int addedTextLength,
//                                                                                    final String rightOfComposed) {
//        return getNewCursorPositionOptions(leftOfComposed, addedTextLength, rightOfComposed,
//                new RelativePosition[] {
//                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, 2),
//                        new RelativePosition(AddingTextPosition.BEGINNING_OF_NEW_TEXT, -2),
//                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 0),
//                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 2),
//                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, -2),
//                });
//    }
//    //#endregion
//
//    // test cases:
//    // start composing text
//    //    single cursor     -> insert composing text
//    //    selection         -> replace with composing text
//    // update composing text
//    //    single cursor     -> update composing text
//    //    selection         -> update composing text
//    // commit text without a composition
//    //    single cursor     -> insert text
//    //    selection         -> replace text
//    // commit text with a composition
//    //    single cursor     -> replace composing text
//    //    selection         -> replace composing text
//    //
//    // settingsOptions (default input connection, limited get text length), cursorPositionKnown (committed initially only)
//    // surrounding text, changing text (probably don't need as many combinations of these 2)
//    // newCursorPositionParams
//    // initialCursorPositions (doesn't really matter for initially composed, so only bother testing a couple)
//    private static Stream<ActionTestCase<AddText>> commit_parameters() {
//        final List<ActionTestCase<AddText>> list = new ArrayList<>();
//
//        final AddTextOption[] addTextOptions = getAddTextOptions();
//
//        // committed text
//        for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(true)) {
//            final boolean useBatch = !settingsOption.data.initialCursorPositionKnown;
//            for (final AddTextOption addTextOption : addTextOptions) {
//                final Named<String> initialTextOption = addTextOption.initialText;
//                final int codePointCount = initialTextOption.data.codePointCount(0, initialTextOption.data.length());
//                final Named<String> addedTextOption = addTextOption.addingText;
//
//                list.add(new ActionTestCase<>(
//                        "add " + addedTextOption.name + " to the end of " + initialTextOption.name,
//                        settingsOption,
//                        useBatch,
//                        new CommittedState(initialTextOption.data, ""),
//                        new AddText(addedTextOption.data, false, 1),
//                        new CommittedState(initialTextOption.data + addedTextOption.data, "")));
//
//                if (codePointCount == 0) {
//                    continue;
//                }
//
//                list.add(new ActionTestCase<>(
//                        "add " + addedTextOption.name + " to the beginning of " + initialTextOption.name,
//                        settingsOption,
//                        useBatch,
//                        new CommittedState("", initialTextOption.data),
//                        new AddText(addedTextOption.data, false, 1),
//                        new CommittedState(addedTextOption.data, initialTextOption.data)));
//
//                if (codePointCount < 2) {
//                    continue;
//                }
//
//                final String leftHalf = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                final String rightHalf = getSubstring(initialTextOption.data, codePointCount / 2);
//                final NewCursorPositionParams[] insertTextNewCursorPositions =
//                        getInsertTextNewCursorPositions(leftHalf, addedTextOption.data.length(), rightHalf);
//                for (final NewCursorPositionParams newCursorPositionOption : insertTextNewCursorPositions) {
//                    if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, leftHalf.length(), addedTextOption)) {
//                        //TODO: is it possible to write a useful test for this? skipping for now
//                        continue;
//                    }
//                    list.add(new ActionTestCase<>(
//                            "add " + addedTextOption.name + " to the middle of "
//                                    + initialTextOption.name + " and move the cursor to "
//                                    + newCursorPositionOption.name,
//                            settingsOption,
//                            useBatch,
//                            new CommittedState(leftHalf, rightHalf),
//                            new AddText(addedTextOption.data, false,
//                                    newCursorPositionOption.newCursorPosition),
//                            new CommittedState(leftHalf + addedTextOption.data + rightHalf,
//                                    newCursorPositionOption.newCursorIndex)));
//                }
//
//                if (codePointCount < 3) {
//                    continue;
//                }
//
//                final String leftOfSelection = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                final String selection = getSubstring(initialTextOption.data, codePointCount / 2, codePointCount / 2 + 1);
//                final String rightOfSelection = getSubstring(initialTextOption.data, codePointCount / 2 + 1);
//                final NewCursorPositionParams[] replaceSelectionNewCursorPositions =
//                        getReplaceSelectionNewCursorPositions(leftOfSelection, addedTextOption.data.length(), rightOfSelection);
//                for (final NewCursorPositionParams newCursorPositionOption : replaceSelectionNewCursorPositions) {
//                    if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
//                            leftOfSelection.length(), leftOfSelection.length() + selection.length(),
//                            addedTextOption)) {
//                        //TODO: is it possible to write a useful test for this? skipping for now
//                        continue;
//                    }
//                    list.add(new ActionTestCase<>(
//                            "replace the selected middle of " + initialTextOption.name
//                                    + " with " + addedTextOption.name
//                                    + " and move the cursor to " + newCursorPositionOption.name,
//                            settingsOption,
//                            useBatch,
//                            new CommittedState(leftOfSelection, selection, rightOfSelection),
//                            new AddText(addedTextOption.data, false,
//                                    newCursorPositionOption.newCursorPosition),
//                            new CommittedState(leftOfSelection + addedTextOption.data + rightOfSelection,
//                                    newCursorPositionOption.newCursorIndex)));
//                }
//            }
//        }
//
//        // composed text
//        for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(false)) {
//            for (final AddTextOption addTextOption : addTextOptions) {
//                final Named<String> initialTextOption = addTextOption.initialText;
//                final int codePointCount = initialTextOption.data.codePointCount(0, initialTextOption.data.length());
//                final Named<String> addedTextOption = addTextOption.addingText;
//                if (codePointCount == 0) {
//                    // skip since part of the initial text is used as the composing text
//                    continue;
//                }
//
//                final String leftHalf = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                final String rightHalf = getSubstring(initialTextOption.data, codePointCount / 2);
//
//                list.add(new ActionTestCase<>(
//                        "finalize whole composed " + initialTextOption.name
//                                + " with " + addedTextOption.name,
//                        settingsOption,
//                        new ComposedState("", initialTextOption.data, ""),
//                        new AddText(addedTextOption.data, false, 1),
//                        new CommittedState(addedTextOption.data)));
//
//                list.add(new ActionTestCase<>(
//                        "finalize composed " + initialTextOption.name
//                                + " at the beginning of the text with " + addedTextOption.name,
//                        settingsOption,
//                        new ComposedState("", leftHalf, rightHalf),
//                        new AddText(addedTextOption.data, false, 1),
//                        new CommittedState(addedTextOption.data, rightHalf)));
//
//                list.add(new ActionTestCase<>(
//                        "finalize composed " + initialTextOption.name
//                                + " at the end of the text with " + addedTextOption.name,
//                        settingsOption,
//                        new ComposedState(leftHalf, rightHalf, ""),
//                        new AddText(addedTextOption.data, false, 1),
//                        new CommittedState(leftHalf + addedTextOption.data)));
//
//                if (codePointCount < 3) {
//                    continue;
//                }
//
//                //TODO: reduce duplicate code from above
//                final String leftOfComposed = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                final String composed = getSubstring(initialTextOption.data, codePointCount / 2, codePointCount / 2 + 1);
//                final String rightOfComposed = getSubstring(initialTextOption.data, codePointCount / 2 + 1);
//                final NewCursorPositionParams[] updateCompositionNewCursorPositions =
//                        getUpdateCompositionNewCursorPositions(leftOfComposed, addedTextOption.data.length(), rightOfComposed);
//                final InitialCursorPositionParams[] initialCursorPositionOptions = new InitialCursorPositionParams[]{
//                        new InitialCursorPositionParams("at the beginning of the text", 0),
////                        new InitialCursorPositionParams("at the end of the composition", leftOfComposed.length() + composed.length()),
//                        new InitialCursorPositionParams("at the end of the text", initialTextOption.data.length()),
//                        new InitialCursorPositionParams("selecting text",
//                                getCharCountBeforeCodePoint(leftOfComposed, -1),
//                                leftOfComposed.length() + composed.length() + getCharCountBeforeCodePoint(rightOfComposed, 1)),
//                };
//                for (final NewCursorPositionParams newCursorPositionOption : updateCompositionNewCursorPositions) {
//                    for (final InitialCursorPositionParams initialCursorPositionOption : initialCursorPositionOptions) {
//                        if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
//                                initialCursorPositionOption.end, leftOfComposed.length(),
//                                leftOfComposed.length() + composed.length(), addedTextOption)) {
//                            //TODO: is it possible to write a useful test for this? skipping for now
//                            continue;
//                        }
//                        list.add(new ActionTestCase<>(
//                                "finalize composed " + initialTextOption.name
//                                        + " in the middle of the text with " + addedTextOption.name
//                                        + " with the cursor " + initialCursorPositionOption.name,
//                                settingsOption,
//                                new ComposedState(leftOfComposed, composed, rightOfComposed,
//                                        initialCursorPositionOption),
//                                new AddText(addedTextOption.data, false,
//                                        newCursorPositionOption.newCursorPosition),
//                                new CommittedState(leftOfComposed + addedTextOption.data + rightOfComposed,
//                                        newCursorPositionOption.newCursorIndex)));
//                    }
//                }
//            }
//        }
//
//        return list.stream();
//    }
//    //TODO: consider reducing duplicate code with commit_parameters
//    private static Stream<ActionTestCase<AddText>> compose_parameters() {
//        final List<ActionTestCase<AddText>> list = new ArrayList<>();
//
//        final AddTextOption[] addTextOptions = getAddTextOptions();
//
//        // committed text
//        for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(true)) {
//            final boolean useBatch = !settingsOption.data.initialCursorPositionKnown;
//            for (final AddTextOption addTextOption : addTextOptions) {
//                final Named<String> initialTextOption = addTextOption.initialText;
//                final int codePointCount = initialTextOption.data.codePointCount(0, initialTextOption.data.length());
//                final Named<String> addedTextOption = addTextOption.addingText;
//                //TODO: try to get rid of this indent
//                if (addedTextOption.data.length() != 0) {
//                    list.add(new ActionTestCase<>(
//                            "add " + addedTextOption.name + " to the end of " + initialTextOption.name,
//                            settingsOption,
//                            useBatch,
//                            new CommittedState(initialTextOption.data, ""),
//                            new AddText(addedTextOption.data, true, 1),
//                            new ComposedState(initialTextOption.data, addedTextOption.data, "")));
//
//                    if (codePointCount == 0) {
//                        continue;
//                    }
//
//                    list.add(new ActionTestCase<>(
//                            "add " + addedTextOption.name + " to the beginning of " + initialTextOption.name,
//                            settingsOption,
//                            useBatch,
//                            new CommittedState("", initialTextOption.data),
//                            new AddText(addedTextOption.data, true, 1),
//                            new ComposedState("", addedTextOption.data, initialTextOption.data)));
//
//                    if (codePointCount < 2) {
//                        continue;
//                    }
//
//                    final String leftHalf = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                    final String rightHalf = getSubstring(initialTextOption.data, codePointCount / 2);
//                    final NewCursorPositionParams[] insertTextNewCursorPositions =
//                            getInsertTextNewCursorPositions(leftHalf, addedTextOption.data.length(), rightHalf);
//                    for (final NewCursorPositionParams newCursorPositionOption : insertTextNewCursorPositions) {
//                        if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, leftHalf.length(), addedTextOption)) {
//                            //TODO: is it possible to write a useful test for this? skipping for now
//                            continue;
//                        }
//                        list.add(new ActionTestCase<>(
//                                "add " + addedTextOption.name + " to the middle of "
//                                        + initialTextOption.name + " and move the cursor to "
//                                        + newCursorPositionOption.name,
//                                settingsOption,
//                                useBatch,
//                                new CommittedState(leftHalf, rightHalf),
//                                new AddText(addedTextOption.data, true,
//                                        newCursorPositionOption.newCursorPosition),
//                                new ComposedState(leftHalf, addedTextOption.data, rightHalf,
//                                        newCursorPositionOption.newCursorIndex)));
//                    }
//
//                    if (codePointCount < 3) {
//                        continue;
//                    }
//
//                    final String leftOfSelection = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                    final String selection = getSubstring(initialTextOption.data, codePointCount / 2, codePointCount / 2 + 1);
//                    final String rightOfSelection = getSubstring(initialTextOption.data, codePointCount / 2 + 1);
//                    final NewCursorPositionParams[] replaceSelectionNewCursorPositions =
//                            getReplaceSelectionNewCursorPositions(leftOfSelection, addedTextOption.data.length(), rightOfSelection);
//                    for (final NewCursorPositionParams newCursorPositionOption : replaceSelectionNewCursorPositions) {
//                        if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, leftOfSelection.length(),
//                                leftOfSelection.length() + selection.length(), addedTextOption)) {
//                            //TODO: is it possible to write a useful test for this? skipping for now
//                            continue;
//                        }
//                        list.add(new ActionTestCase<>(
//                                "replace the selected middle of " + initialTextOption.name
//                                        + " with " + addedTextOption.name
//                                        + " and move the cursor to " + newCursorPositionOption.name,
//                                settingsOption,
//                                useBatch,
//                                new CommittedState(leftOfSelection, selection, rightOfSelection),
//                                new AddText(addedTextOption.data, true,
//                                        newCursorPositionOption.newCursorPosition),
//                                new ComposedState(leftOfSelection, addedTextOption.data, rightOfSelection,
//                                        newCursorPositionOption.newCursorIndex)));
//                    }
//                } else {
//                    // compose nothing tests
//
//                    list.add(new ActionTestCase<>(
//                            "add " + addedTextOption.name + " to the end of " + initialTextOption.name,
//                            settingsOption,
//                            useBatch,
//                            new CommittedState(initialTextOption.data, ""),
//                            new AddText(addedTextOption.data, true, 1),
//                            new CommittedState(initialTextOption.data, "")));
//
//                    if (codePointCount < 2) {
//                        continue;
//                    }
//
//                    final String leftHalf = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                    final String rightHalf = getSubstring(initialTextOption.data, codePointCount / 2);
//                    final NewCursorPositionParams newCursorPositionOption1 =
//                            getNewCursorPosition(leftHalf, addedTextOption.data.length(), rightHalf,
//                                    AddingTextPosition.BEGINNING_OF_NEW_TEXT, -1);
//                    if (!isNewCursorInKnownSpace(newCursorPositionOption1, settingsOption, leftHalf.length(), addedTextOption)) {
//                        //TODO: is it possible to write a useful test for this? skipping for now
//                    } else {
//                        list.add(new ActionTestCase<>(
//                                "add " + addedTextOption.name + " to the middle of "
//                                        + initialTextOption.name + " and move the cursor to "
//                                        + newCursorPositionOption1.name,
//                                settingsOption,
//                                useBatch,
//                                new CommittedState(leftHalf, rightHalf),
//                                new AddText(addedTextOption.data, true,
//                                        newCursorPositionOption1.newCursorPosition),
//                                new CommittedState(leftHalf + rightHalf,
//                                        newCursorPositionOption1.newCursorIndex)));
//                    }
//
//                    if (codePointCount < 3) {
//                        continue;
//                    }
//
//                    final String leftOfSelection = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                    final String selection = getSubstring(initialTextOption.data, codePointCount / 2, codePointCount / 2 + 1);
//                    final String rightOfSelection = getSubstring(initialTextOption.data, codePointCount / 2 + 1);
//                    final NewCursorPositionParams newCursorPositionOption2 =
//                            getNewCursorPosition(leftOfSelection, addedTextOption.data.length(), rightOfSelection,
//                                    AddingTextPosition.END_OF_NEW_TEXT, 1);
//                    if (!isNewCursorInKnownSpace(newCursorPositionOption2, settingsOption, leftOfSelection.length(),
//                            leftOfSelection.length() + selection.length(), addedTextOption)) {
//                        //TODO: is it possible to write a useful test for this? skipping for now
//                    } else {
//                        list.add(new ActionTestCase<>(
//                                "replace the selected middle of " + initialTextOption.name
//                                        + " with " + addedTextOption.name
//                                        + " and move the cursor to " + newCursorPositionOption2.name,
//                                settingsOption,
//                                useBatch,
//                                new CommittedState(leftOfSelection, selection, rightOfSelection),
//                                new AddText(addedTextOption.data, true,
//                                        newCursorPositionOption2.newCursorPosition),
//                                new CommittedState(leftOfSelection + rightOfSelection,
//                                        newCursorPositionOption2.newCursorIndex)));
//                    }
//                }
//            }
//        }
//
//        // composed text
//        for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(false)) {
//            for (final AddTextOption addTextOption : addTextOptions) {
//                final Named<String> initialTextOption = addTextOption.initialText;
//                final int codePointCount = initialTextOption.data.codePointCount(0, initialTextOption.data.length());
//                final Named<String> addedTextOption = addTextOption.addingText;
//                if (codePointCount == 0) {
//                    // skip since part of the initial text is used as the composing text
//                    continue;
//                }
//                //TODO: try to get rid of this indent
//                if (addedTextOption.data.length() != 0) {
//                    final String leftHalf = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                    final String rightHalf = getSubstring(initialTextOption.data, codePointCount / 2);
//
//                    list.add(new ActionTestCase<>(
//                            "replace whole composed " + initialTextOption.name
//                                    + " with " + addedTextOption.name,
//                            settingsOption,
//                            new ComposedState("", initialTextOption.data, ""),
//                            new AddText(addedTextOption.data, true, 1),
//                            new ComposedState("", addedTextOption.data, "")));
//
//                    list.add(new ActionTestCase<>(
//                            "replace composed " + initialTextOption.name
//                                    + " at the beginning of the text with " + addedTextOption.name,
//                            settingsOption,
//                            new ComposedState("", leftHalf, rightHalf),
//                            new AddText(addedTextOption.data, true, 1),
//                            new ComposedState("", addedTextOption.data, rightHalf)));
//
//                    list.add(new ActionTestCase<>(
//                            "replace composed " + initialTextOption.name
//                                    + " at the end of the text with " + addedTextOption.name,
//                            settingsOption,
//                            new ComposedState(leftHalf, rightHalf, ""),
//                            new AddText(addedTextOption.data, true, 1),
//                            new ComposedState(leftHalf, addedTextOption.data, "")));
//
//                    if (codePointCount < 3) {
//                        continue;
//                    }
//
//                    //TODO: reduce duplicate code from above
//                    final String leftOfComposed = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                    final String composed = getSubstring(initialTextOption.data, codePointCount / 2, codePointCount / 2 + 1);
//                    final String rightOfComposed = getSubstring(initialTextOption.data, codePointCount / 2 + 1);
//                    final NewCursorPositionParams[] updateCompositionNewCursorPositions =
//                            getUpdateCompositionNewCursorPositions(leftOfComposed, addedTextOption.data.length(), rightOfComposed);
//                    final InitialCursorPositionParams[] initialCursorPositionOptions = new InitialCursorPositionParams[]{
//                            new InitialCursorPositionParams("at the beginning of the text", 0),
////                        new InitialCursorPositionParams("at the end of the composition", leftOfComposed.length() + composed.length()),
//                            new InitialCursorPositionParams("at the end of the text", initialTextOption.data.length()),
//                            new InitialCursorPositionParams("selecting text",
//                                    getCharCountBeforeCodePoint(leftOfComposed, -1),
//                                    leftOfComposed.length() + composed.length() + getCharCountBeforeCodePoint(rightOfComposed, 1)),
//                    };
//                    for (final NewCursorPositionParams newCursorPositionOption : updateCompositionNewCursorPositions) {
//                        for (final InitialCursorPositionParams initialCursorPositionOption : initialCursorPositionOptions) {
//                            //TODO: my guess is that this isn't working - I think the currently broken tests should be skipped - this isn't taking in initialCursorPositionOption
//                            if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
//                                    initialCursorPositionOption.end, leftOfComposed.length(),
//                                    leftOfComposed.length() + composed.length(), addedTextOption)) {
//                                //TODO: is it possible to write a useful test for this? skipping for now
//                                continue;
//                            }
//                            list.add(new ActionTestCase<>(
//                                    "replace composed " + initialTextOption.name
//                                            + " in the middle of the text with " + addedTextOption.name
//                                            + " with the cursor " + initialCursorPositionOption.name,
//                                    settingsOption,
//                                    new ComposedState(leftOfComposed, composed, rightOfComposed,
//                                            initialCursorPositionOption),
//                                    new AddText(addedTextOption.data, true,
//                                            newCursorPositionOption.newCursorPosition),
//                                    new ComposedState(leftOfComposed, addedTextOption.data, rightOfComposed,
//                                            newCursorPositionOption.newCursorIndex)));
//                        }
//                    }
//                } else {
//                    // compose nothing tests
//
//                    list.add(new ActionTestCase<>(
//                            "replace whole composed " + initialTextOption.name
//                                    + " with " + addedTextOption.name,
//                            settingsOption,
//                            new ComposedState("", initialTextOption.data, ""),
//                            new AddText(addedTextOption.data, true, 1),
//                            new CommittedState("")));
//
//                    if (codePointCount < 3) {
//                        continue;
//                    }
//
//                    //TODO: reduce duplicate code from above
//                    final String leftOfComposed = getSubstring(initialTextOption.data, 0, codePointCount / 2);
//                    final String composed = getSubstring(initialTextOption.data, codePointCount / 2, codePointCount / 2 + 1);
//                    final String rightOfComposed = getSubstring(initialTextOption.data, codePointCount / 2 + 1);
//                    final NewCursorPositionParams newCursorPositionOption = getNewCursorPosition(leftOfComposed, addedTextOption.data.length(), rightOfComposed,
//                            AddingTextPosition.END_OF_NEW_TEXT, 1);
//                    final InitialCursorPositionParams initialCursorPositionOption =
//                            new InitialCursorPositionParams("at the end of the composition",
//                                    leftOfComposed.length() + composed.length());
//                    if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
//                            initialCursorPositionOption.end, leftOfComposed.length(),
//                            leftOfComposed.length() + composed.length(), addedTextOption)) {
//                        //TODO: is it possible to write a useful test for this? skipping for now
//                        continue;
//                    }
//                    list.add(new ActionTestCase<>(
//                            "replace composed " + initialTextOption.name
//                                    + " in the middle of the text with " + addedTextOption.name
//                                    + " with the cursor " + initialCursorPositionOption.name,
//                            settingsOption,
//                            new ComposedState(leftOfComposed, composed, rightOfComposed,
//                                    initialCursorPositionOption),
//                            new AddText(addedTextOption.data, true,
//                                    newCursorPositionOption.newCursorPosition),
//                            new CommittedState(leftOfComposed + rightOfComposed,
//                                    newCursorPositionOption.newCursorIndex)));
//                }
//            }
//        }
//
//        return list.stream();
//    }
//
//    private static Stream<ActionTestCase<AddText>> getAddTextParams(final boolean isComposingText,
//                                                                    final boolean hasComposedText,
//                                                                    final boolean hasSelection) {
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getAddTextSettingsOptions();
//        final List<SurroundingTextParams> surroundingTextParams = getInitialSurroundingTextParams();
//        final List<ChangingTextParams> changingTextParams = (hasComposedText || hasSelection)
//                ? getChangingTextSimpleParams()
//                : getAddingTextParams();
//        final List<ActionTestCase<AddText>> list = new ArrayList<>();
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            for (final SurroundingTextParams surroundingText : surroundingTextParams) {
//                for (final ChangingTextParams changingText : changingTextParams) {
//                    final int changedTextLength = changingText.update.length() - changingText.initial.length();
//                    final List<NewCursorPositionParams> newCursorPositionParams =
//                            getCursorPositionParams(surroundingText, changingText.update);
//                    if (hasComposedText) {
//                        final List<InitialCursorPositionParams> initialCursorPositions = hasSelection
//                                ? getSelectionCursorPositionParams(surroundingText, changingText.initial)
//                                : getSingleCursorPositionParams(surroundingText, changingText.initial);
//                        for (final NewCursorPositionParams cursorPosition : newCursorPositionParams) {
//                            for (final InitialCursorPositionParams initialCursorPosition : initialCursorPositions) {
//                                final String name = getName(surroundingText, changingText, initialCursorPosition, cursorPosition, settingsOption);
//                                final ComposedState state = new ComposedState(surroundingText, changingText.initial, initialCursorPosition);
//                                //TODO: reduce duplicate code from above
//                                if (cursorPosition.newCursorIndex > 1 && settingsOption.data.getTextLimit < Integer.MAX_VALUE) {
//                                    final int maxKnownIndex = state.getCursorEnd() >= state.getCompositionEnd()
//                                            ? state.getCursorEnd() + settingsOption.data.getTextLimit
//                                            : Math.max(state.getCursorEnd() + settingsOption.data.getTextLimit, state.getCompositionEnd());
//                                    if (maxKnownIndex + changedTextLength < cursorPosition.newCursorIndex) {
//                                        //TODO: is it possible to write a useful test for this? skipping for now
//                                        continue;
//                                    }
//                                }
//                                list.add(createAddTextTestCase(name, settingsOption.data, true, state, surroundingText, changingText.update, isComposingText, cursorPosition));
//                            }
//                        }
//                    } else {
//                        for (final boolean cursorPositionKnown : new boolean[] {false, true}) {
//                            for (final NewCursorPositionParams cursorPosition : newCursorPositionParams) {
//                                final String name = getName(surroundingText, changingText, cursorPosition, settingsOption);
//                                final CommittedState state = new CommittedState(surroundingText, changingText.initial);
//                                //TODO: reduce duplicate code from above
//                                if (cursorPosition.newCursorIndex > 1 && settingsOption.data.getTextLimit < Integer.MAX_VALUE) {
//                                    final int maxKnownIndex = state.getCursorEnd() >= state.getCompositionEnd()
//                                            ? state.getCursorEnd() + settingsOption.data.getTextLimit
//                                            : Math.max(state.getCursorEnd() + settingsOption.data.getTextLimit, state.getCompositionEnd());
//                                    if (maxKnownIndex + changedTextLength < cursorPosition.newCursorIndex) {
//                                        //TODO: is it possible to write a useful test for this? skipping for now
//                                        continue;
//                                    }
//                                }
//                                list.add(createAddTextTestCase(name, settingsOption.data, cursorPositionKnown, state, surroundingText, changingText.update, isComposingText, cursorPosition));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return list.stream();
//    }
//
//    private static ActionTestCase<AddText> createAddTextTestCase(final String testName,
//                                                                 final FakeInputConnectionSettings settings,
//                                                                 final boolean cursorPositionKnown,
//                                                                 final State initialState,
//                                                                 final SurroundingTextParams surroundingText,
//                                                                 final String newText,
//                                                                 final boolean composeNewText,
//                                                                 final NewCursorPositionParams newCursorPosition) {
//        final State expectedState;
//        //TODO: handle keepEmptyComposingPosition setting
//        if (composeNewText && !isEmpty(newText)) {
//            expectedState = new ComposedState(surroundingText.before, newText, surroundingText.after,
//                    newCursorPosition.newCursorIndex, newCursorPosition.newCursorIndex);
//        } else {
//            expectedState = new CommittedState(surroundingText.before + newText + surroundingText.after,
//                    newCursorPosition.newCursorIndex, newCursorPosition.newCursorIndex);
//        }
//        return new ActionTestCase<>(testName + " :: cursor position " + (cursorPositionKnown ? "known" : "unknown"),
//                settings, !cursorPositionKnown, initialState, cursorPositionKnown,
//                new AddText(newText, composeNewText, newCursorPosition.newCursorPosition),
//                expectedState);
//    }
//    //#endregion
//
//    //#region parameter component builders
//    //TODO: copied from getDeleteSelectedSettingsOptions - make sure this isn't just a duplicate
//    private static List<Named<FakeInputConnectionSettings>> getAddTextSettingsOptions() {
//        final List<Named<FakeInputConnectionSettings>> list = new ArrayList<>();
//        list.add(new Named<>("default input connection",
//                new FakeInputConnectionSettings()));
//        list.add(new Named<>("limited get text length",
//                new FakeInputConnectionSettings(5)));
//        return list;
//    }
//    private static List<Named<TestSettings>> getAddTextSettingsOptions(final boolean testUnknownPosition) {
//        //TODO: are all of these combinations valuable and are there other settings worth testing here?
//        final List<Named<TestSettings>> list = new ArrayList<>();
//        list.add(new Named<>("default input connection with known cursor",
//                new TestSettings(new FakeInputConnectionSettings(), true)));
//        list.add(new Named<>("limited get text length with known cursor",
//                new TestSettings(new FakeInputConnectionSettings(5), true)));
//        if (testUnknownPosition) {
//            list.add(new Named<>("default input connection with unknown cursor",
//                    new TestSettings(new FakeInputConnectionSettings(), false)));
//            list.add(new Named<>("limited get text length with unknown cursor",
//                    new TestSettings(new FakeInputConnectionSettings(5), false)));
//        }
//        return list;
//    }
//
//    //#region surrounding text
//    private static List<SurroundingTextParams> getInitialSurroundingTextParams() {
//        final List<SurroundingTextParams> list = new ArrayList<>();
//        list.add(new SurroundingTextParams("no initial text",
//                "", ""));
//        list.add(new SurroundingTextParams("cursor before existing text",
//                "", "Lorem ipsum dolor sit amet"));
//        list.add(new SurroundingTextParams("cursor after existing text",
//                "Lorem ipsum dolor sit amet", ""));
//        list.add(new SurroundingTextParams("cursor inside existing text",
//                "Lorem ipsum", " dolor sit amet"));
//
//        list.add(new SurroundingTextParams("cursor before existing text with surrogate pairs",
//                "", getSurrogatePairString(0, 10)));
//        list.add(new SurroundingTextParams("cursor after existing text with surrogate pairs",
//                getSurrogatePairString(0, 10), ""));
//        list.add(new SurroundingTextParams("cursor inside existing text with all surrogate pairs",
//                getSurrogatePairString(10, 4), getSurrogatePairString(20, 6)));
//        list.add(new SurroundingTextParams("cursor inside existing text with surrogate pairs on inside edges",
//                "Lorem ipsum" + getSurrogatePairString(0), getSurrogatePairString(1) + " dolor sit amet"));
//        list.add(new SurroundingTextParams("cursor inside existing text with surrogate pairs just off of the inside edges",
//                "Lorem ipsu" + getSurrogatePairString(0) + "m", " " + getSurrogatePairString(1) + "dolor sit amet"));
//
//        return list;
//    }
//    //#endregion
//
//    //#region adding/changing text
//    private static List<Named<String>> getTextOptions() {
//        final List<Named<String>> list = new ArrayList<>();
//        list.add(new Named<>("single character",
//                "a"));
//        list.add(new Named<>("text",
//                "test"));
//
//        list.add(new Named<>("single surrogate pair",
//                getSurrogatePairString(30)));
//        list.add(new Named<>("multiple surrogate pairs",
//                getSurrogatePairString(30, 3)));
//
//        list.add(new Named<>("mixed surrogate pairs and normal text",
//                getSurrogatePairString(30) + "a" + getSurrogatePairString(40) + "b"
//                        + getSurrogatePairString(50)));
//        return list;
//    }
//
//    private static List<ChangingTextParams> getAddingTextParams() {
//        final List<ChangingTextParams> list = new ArrayList<>();
//        for (final Named<String> textOption : getTextOptions()) {
//            list.add(new ChangingTextParams(textOption.name, textOption.data));
//        }
//        return list;
//    }
//
//    private static List<ChangingTextParams> getChangingTextSimpleParams() {
//        final List<ChangingTextParams> list = new ArrayList<>();
//        list.add(new ChangingTextParams("unchanging single character",
//                "a", "a"));
//        list.add(new ChangingTextParams("changing single character",
//                "a", "b"));
//        list.add(new ChangingTextParams("single character to multiple characters",
//                "a", "test"));
//        list.add(new ChangingTextParams("single character to nothing",
//                "a", ""));
//        list.add(new ChangingTextParams("multiple characters to single character",
//                "example", "a"));
//        list.add(new ChangingTextParams("changing multiple characters",
//                "foo", "bar"));
//        list.add(new ChangingTextParams("unchanging multiple characters",
//                "text", "text"));
//        list.add(new ChangingTextParams("multiple characters to nothing",
//                "text", ""));
//
//        list.add(new ChangingTextParams("unchanging single surrogate pair",
//                getSurrogatePairString(30), getSurrogatePairString(30)));
//        list.add(new ChangingTextParams("changing single surrogate pair",
//                getSurrogatePairString(30), getSurrogatePairString(31)));
//        list.add(new ChangingTextParams("single character to single surrogate pair",
//                "a", getSurrogatePairString(30)));
//        list.add(new ChangingTextParams("single surrogate pair to single character",
//                getSurrogatePairString(30), "a"));
//        list.add(new ChangingTextParams("single surrogate pair to nothing",
//                getSurrogatePairString(30), ""));
//
//        list.add(new ChangingTextParams("text to surrogate pairs with the same length",
//                "abcd", getSurrogatePairString(30, 2)));
//        list.add(new ChangingTextParams("surrogate pairs to text with the same length",
//                getSurrogatePairString(30, 2), "abcd"));
//        list.add(new ChangingTextParams("text to surrogate pairs with the same code point length",
//                "ab", getSurrogatePairString(30, 2)));
//        list.add(new ChangingTextParams("surrogate pairs to text with the same code point length",
//                getSurrogatePairString(30, 2), "ab"));
//        list.add(new ChangingTextParams("surrogate pairs to nothing",
//                getSurrogatePairString(30, 2), ""));
//
//        return list;
//    }
//    //#endregion
//
//    //#region initial cursor position
//    private static List<InitialCursorPositionParams> getSingleCursorPositionParams(final SurroundingTextParams surroundingText, final String composingText) {
//        final List<InitialCursorPositionParams> initialCursorPositionParams = new ArrayList<>();
//
//        final int composingStart = surroundingText.before.length();
//        final int composingEnd = composingStart + composingText.length();
//        final int fullLength = composingEnd + surroundingText.after.length();
//
//        final int beforeCodePointCount = surroundingText.before.codePointCount(0, surroundingText.before.length());
//        final int composingCodePointCount = composingText.codePointCount(0, composingText.length());
//        final int afterCodePointCount = surroundingText.after.codePointCount(0, surroundingText.after.length());
//
//
//        if (beforeCodePointCount > 0) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor at the beginning of the text",
//                    0));
//        }
//        if (beforeCodePointCount > 1) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor after the beginning of the text",
//                    getCharCountBeforeCodePoint(surroundingText.before, 1)));
//        }
//        if (beforeCodePointCount > 2) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor before the beginning of the composing text",
//                    getCharCountBeforeCodePoint(surroundingText.before, -1)));
//        }
//        if (composingCodePointCount > 0) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor at beginning of the composing text",
//                    composingStart));
//        }
//        if (composingCodePointCount > 2) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor after the beginning of the composing text",
//                    composingStart + getCharCountBeforeCodePoint(composingText, 1)));
//        }
//        if (composingCodePointCount > 1) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor before the end of the composing text",
//                    composingStart + getCharCountBeforeCodePoint(composingText, -1)));
//        }
//        initialCursorPositionParams.add(new InitialCursorPositionParams("cursor at end of the composing text", composingEnd));
//        if (afterCodePointCount > 2) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor after end of the composing text",
//                    composingEnd + getCharCountBeforeCodePoint(surroundingText.after, 1)));
//        }
//        if (afterCodePointCount > 1) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor before the end of the text",
//                    composingEnd + getCharCountBeforeCodePoint(surroundingText.after, -1)));
//        }
//        if (afterCodePointCount > 0) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "cursor at end of the text", fullLength));
//        }
//
//        return initialCursorPositionParams;
//    }
//    private static List<InitialCursorPositionParams> getSelectionCursorPositionParams(final SurroundingTextParams surroundingText, final String composingText) {
//        final List<InitialCursorPositionParams> initialCursorPositionParams = new ArrayList<>();
//
//        final int composingStart = surroundingText.before.length();
//        final int composingEnd = composingStart + composingText.length();
//        final int fullLength = composingEnd + surroundingText.after.length();
//
//        final int beforeCodePointCount = surroundingText.before.codePointCount(0, surroundingText.before.length());
//        final int composingCodePointCount = composingText.codePointCount(0, composingText.length());
//        final int afterCodePointCount = surroundingText.after.codePointCount(0, surroundingText.after.length());
//
//        if (beforeCodePointCount > 0) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "select to the start of the composing text",
//                    0,
//                    composingStart));
//            if (composingCodePointCount > 1) {
//                initialCursorPositionParams.add(new InitialCursorPositionParams(
//                        "select to just after the start of the composing text",
//                        0,
//                        composingStart + getCharCountBeforeCodePoint(composingText, 1)));
//            }
//            if (composingCodePointCount > 2) {
//                initialCursorPositionParams.add(new InitialCursorPositionParams(
//                        "select to just before the end of the composing text",
//                        0,
//                        composingStart + getCharCountBeforeCodePoint(composingText, -1)));
//            }
//            if (composingText.length() > 0) {
//                initialCursorPositionParams.add(new InitialCursorPositionParams(
//                        "select to the end of the composing text",
//                        0,
//                        composingEnd));
//            }
//        }
//
//        if (composingCodePointCount > 0) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "select the whole composing text",
//                    composingStart,
//                    composingEnd));
//        }
//        if (composingCodePointCount > 2) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "select part of the composing text",
//                    composingStart + getCharCountBeforeCodePoint(composingText, 1),
//                    composingStart + getCharCountBeforeCodePoint(composingText, -1)));
//        }
//
//        if (afterCodePointCount > 0) {
//            if (composingCodePointCount > 0) {
//                initialCursorPositionParams.add(new InitialCursorPositionParams(
//                        "select from the start of the composing text",
//                        composingStart,
//                        fullLength));
//            }
//            if (composingCodePointCount > 2) {
//                initialCursorPositionParams.add(new InitialCursorPositionParams(
//                        "select from just after the start of the composing text",
//                        composingStart + getCharCountBeforeCodePoint(composingText, 1),
//                        fullLength));
//            }
//            if (composingCodePointCount > 1) {
//                initialCursorPositionParams.add(new InitialCursorPositionParams(
//                        "select from just before the end of the composing text",
//                        composingStart + getCharCountBeforeCodePoint(composingText, -1),
//                        fullLength));
//            }
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "select from the end of the composing text",
//                    composingEnd,
//                    fullLength));
//        }
//
//        if (beforeCodePointCount > 2 && afterCodePointCount > 2) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "select part of the whole text",
//                    getCharCountBeforeCodePoint(surroundingText.before, 1),
//                    composingEnd + getCharCountBeforeCodePoint(surroundingText.after, -1)));
//        }
//        if (beforeCodePointCount > 0 && afterCodePointCount > 0) {
//            initialCursorPositionParams.add(new InitialCursorPositionParams(
//                    "select the whole text",
//                    0,
//                    fullLength));
//        }
//
//        return initialCursorPositionParams;
//    }
//    //#endregion
//
//    //#region newCursorPosition and expected new index
//    private static List<NewCursorPositionParams> getCursorPositionParams(
//            final SurroundingTextParams surroundingText, final String newText) {
//        final int newTextStartPosition = surroundingText.before.length();
//        final int newTextEndPosition = newTextStartPosition + newText.length();
//        final int fullTextLength = newTextEndPosition + surroundingText.after.length();
//
//        final int beforeCodePointCount = surroundingText.before.codePointCount(0, surroundingText.before.length());
//        final int afterCodePointCount = surroundingText.after.codePointCount(0, surroundingText.after.length());
//
//        final List<NewCursorPositionParams> list = new ArrayList<>();
//
//        list.add(new NewCursorPositionParams("cursor before full text",
//                -surroundingText.before.length() - 1,
//                0));
//        if (beforeCodePointCount > 0) {
//            list.add(new NewCursorPositionParams("cursor at beginning of full text",
//                    -surroundingText.before.length(),
//                    0));
//        }
//        if (beforeCodePointCount > 1) {
//            final int offset = -getCharCountAfterCodePoint(surroundingText.before, 1);
//            list.add(new NewCursorPositionParams("cursor just after beginning of full text",
//                    offset,
//                    newTextStartPosition + offset));
//        }
//        if (beforeCodePointCount > 2) {
//            final int offset = -getCharCountAfterCodePoint(surroundingText.before, -1);
//            list.add(new NewCursorPositionParams("cursor just before new text",
//                    offset,
//                    newTextStartPosition + offset));
//        }
//        list.add(new NewCursorPositionParams("cursor at beginning of new text",
//                0,
//                surroundingText.before.length()));
//        list.add(new NewCursorPositionParams("cursor at end of new text",
//                1,
//                newTextEndPosition));
//        if (afterCodePointCount > 2) {
//            final int offset = getCharCountBeforeCodePoint(surroundingText.after, 1);
//            list.add(new NewCursorPositionParams("cursor just after new text",
//                    offset + 1,
//                    newTextEndPosition + offset));
//        }
//        if (afterCodePointCount > 1) {
//            final int offset = getCharCountBeforeCodePoint(surroundingText.after, -1);
//            list.add(new NewCursorPositionParams("cursor just before end of full text",
//                    offset + 1,
//                    newTextEndPosition + offset));
//        }
//        if (afterCodePointCount > 0) {
//            list.add(new NewCursorPositionParams("cursor at end of full text",
//                    surroundingText.after.length() + 1,
//                    fullTextLength));
//        }
//        list.add(new NewCursorPositionParams("cursor after end of full text",
//                surroundingText.after.length() + 2,
//                fullTextLength));
//
//        return list;
//    }
//    //#endregion
//    //#endregion
//
//    //#region parameter component builder classes
//    private static class SurroundingTextParams {
//        public final String name;
//        public final String before;
//        public final String after;
//        public SurroundingTextParams(final String name, final String before, final String after) {
//            this.name = name;
//            this.before = before;
//            this.after = after;
//        }
//    }
//
//    private static class ChangingTextParams {
//        public final String name;
//        public final String initial;
//        public final String update;
//        public ChangingTextParams(final String name, final String initial, final String update) {
//            this.name = name;
//            this.initial = initial;
//            this.update = update;
//        }
//        public ChangingTextParams(final String name, final String update) {
//            this(name, "", update);
//        }
//    }
//
//    private static class InitialCursorPositionParams {
//        public final String name;
//        public final int start;
//        public final int end;
//        public InitialCursorPositionParams(final String name, final int position) {
//            this(name, position, position);
//        }
//        public InitialCursorPositionParams(final String name, final int start, final int end) {
//            this.name = name;
//            this.start = start;
//            this.end = end;
//        }
//    }
//
//    private static class NewCursorPositionParams {
//        public final String name;
//        public final int newCursorPosition;
//        public final int newCursorIndex;
//        public NewCursorPositionParams(final String name,
//                                       final int newCursorPosition, final int newCursorIndex) {
//            this.name = name;
//            this.newCursorPosition = newCursorPosition;
//            this.newCursorIndex = newCursorIndex;
//        }
//    }
//    //#endregion
//    //#endregion
//
//    //#region finish composing tests
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("finishComposing_parameters")
//    public void finishComposing(final ActionTestCase<FinishComposing> params) {
//        System.out.println("ParameterizedTest commit_addNewText");
//        testAction2(params);
//    }
//
//    private static Stream<ActionTestCase<FinishComposing>> finishComposing_parameters() {
//        //TODO: consider not creating as many tests - this seem like overkill
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getDeleteSelectedSettingsOptions();//TODO: rename since this is being reused
//        final List<SurroundingTextParams> surroundingTextParams = getInitialSurroundingTextParams();
//        final List<Named<String>> composedTextOptions = getTextOptions();
//        final List<ActionTestCase<FinishComposing>> list = new ArrayList<>();
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            for (final SurroundingTextParams surroundingText : surroundingTextParams) {
//                for (final Named<String> composedTextOption : composedTextOptions) {
//                    final String composedText = composedTextOption.data;
//                    final List<InitialCursorPositionParams> initialCursorPositions = new ArrayList<>();
//                    initialCursorPositions.addAll(getSingleCursorPositionParams(surroundingText, composedText));
//                    initialCursorPositions.addAll(getSelectionCursorPositionParams(surroundingText, composedText));
//                    for (final InitialCursorPositionParams initialCursorPosition : initialCursorPositions) {
//                        final String name = getName(surroundingText, composedTextOption, initialCursorPosition);
//                        list.add(new ActionTestCase<>(name + " :: " + settingsOption.name,
//                                settingsOption.data,
//                                false,
//                                new ComposedState(surroundingText.before, composedText, surroundingText.after, initialCursorPosition),
//                                true,
//                                new FinishComposing(),
//                                new CommittedState(surroundingText.before + composedText + surroundingText.after, initialCursorPosition)));
//                    }
//                }
//            }
//        }
//        return list.stream();
//    }
//    //#endregion
//
//    //#region delete text tests
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("deleteTextBeforeCursor_committed_parameters")
//    public void deleteTextBeforeCursor_committed(final ActionTestCase<DeleteTextBefore> params) {
//        System.out.println("ParameterizedTest deleteTextBeforeCursor_committed");
//        testAction2(params);
//    }
//    //TODO: (EW) 24 tests are failing because the composed text can't be cached to be able to add back - validate if those tests are appropriate
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("deleteTextBeforeCursor_composed_parameters")
//    public void deleteTextBeforeCursor_composed(final ActionTestCase<DeleteTextBefore> params) {
//        System.out.println("ParameterizedTest deleteTextBeforeCursor_composed");
//        testAction2(params);
//    }
//
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("deleteSelectedText_committed_parameters")
//    public void deleteSelectedText_committed(final ActionTestCase<DeleteSelected> params) {
//        System.out.println("ParameterizedTest deleteSelectedText_committed");
//        testAction2(params);
//    }
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("deleteSelectedText_composed_parameters")
//    public void deleteSelectedText_composed(final ActionTestCase<DeleteSelected> params) {
//        System.out.println("ParameterizedTest deleteSelectedText_composed");
//        testAction2(params);
//    }
//
//    //#region delete parameters
//    //#region delete text before parameters
//    private static Stream<ActionTestCase<DeleteTextBefore>> deleteTextBeforeCursor_committed_parameters() {
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getDeleteBeforeSettingsOptions();
//        final List<Named<ThreePartText>> deleteParams = getDeletingCommittedTextParams();
//        final List<ActionTestCase<DeleteTextBefore>> list = new ArrayList<>();
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            for (final boolean cursorPositionKnown : new boolean[] {false, true}) {
//                final boolean useBatch = !cursorPositionKnown;
//                final String nameExtra = " (cursor position " + (cursorPositionKnown ? "known" : "unknown") + ")";
//                for (final Named<ThreePartText> param : deleteParams) {
//                    final int initialPosition = param.data.left.length() + param.data.center.length();
//                    final int updatedPosition = param.data.left.length();
//                    final String initialText = param.data.left + param.data.center + param.data.right;
//                    final String updatedText = param.data.left + param.data.right;
//                    list.add(new ActionTestCase<>("delete " + param.name + nameExtra,
//                            settingsOption.data,
//                            useBatch,
//                            new CommittedState(initialText, initialPosition, initialPosition),
//                            cursorPositionKnown,
//                            new DeleteTextBefore(param.data.center.length()),
//                            new CommittedState(updatedText, updatedPosition, updatedPosition)));
//                    final int rightCodePointCount = param.data.right.codePointCount(0, param.data.right.length());
//                    if (rightCodePointCount > 0) {
//                        list.add(new ActionTestCase<>("delete " + param.name + " and select to the end" + nameExtra,
//                                settingsOption.data,
//                                useBatch,
//                                new CommittedState(initialText, initialPosition, initialPosition + param.data.right.length()),
//                                cursorPositionKnown,
//                                new DeleteTextBefore(param.data.center.length()),
//                                new CommittedState(updatedText, updatedPosition, updatedPosition + param.data.right.length())));
//                    }
//                    if (rightCodePointCount > 1) {
//                        list.add(new ActionTestCase<>("delete " + param.name + " and select one code point" + nameExtra,
//                                settingsOption.data,
//                                useBatch,
//                                new CommittedState(initialText, initialPosition, initialPosition + getCharCountBeforeCodePoint(param.data.right, 1)),
//                                cursorPositionKnown,
//                                new DeleteTextBefore(param.data.center.length()),
//                                new CommittedState(updatedText, updatedPosition, updatedPosition + getCharCountBeforeCodePoint(param.data.right, 1))));
//                    }
//                    if (param.data.left.length() == 0) {
//                        list.add(new ActionTestCase<>("delete " + param.name + " and request deleting past the start" + nameExtra,
//                                settingsOption.data,
//                                useBatch,
//                                new CommittedState(initialText, initialPosition, initialPosition),
//                                cursorPositionKnown,
//                                new DeleteTextBefore(param.data.center.length() + 1),
//                                new CommittedState(updatedText, updatedPosition, updatedPosition)));
//                    }
//                }
//            }
//        }
//
//        //TODO: add tests for deleting part of surrogate pair
//
//        return list.stream();
//    }
//
//    private static Stream<ActionTestCase<DeleteTextBefore>> deleteTextBeforeCursor_composed_parameters() {
//        final List<ActionTestCase<DeleteTextBefore>> list = new ArrayList<>();
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getDeleteBeforeSettingsOptions();
//        final List<DeletingComposedTextParams> params = getDeletingComposedTextParams();
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            for (final DeletingComposedTextParams param : params) {
//                //TODO: clean up how names are built
//                final String name = param.name + " :: " + settingsOption.name;
//                final int deleteBeforeLength = param.deleteEnd - param.deleteStart;
//                final State initialState = new ComposedState(
//                        param.initialLeftText, param.initialComposedText, param.initialRightText,
//                        param.deleteEnd, param.deleteEnd);
//                final State expectedState = deleteTextFromComposed(param.initialLeftText, param.initialComposedText,
//                        param.initialRightText, param.deleteStart, param.deleteEnd,
//                        param.deleteStart, param.deleteStart);
//                list.add(new ActionTestCase<>(name,
//                        settingsOption.data,
//                        false,
//                        initialState,
//                        true,
//                        new DeleteTextBefore(deleteBeforeLength),
//                        settingsOption.data.getTextLimit < Integer.MAX_VALUE
//                                && expectedState.getCursorEnd() + settingsOption.data.getTextLimit < expectedState.getCompositionEnd()
//                                && !settingsOption.data.setComposingRegionSupported
//                                && initialState.getCursorStart() > initialState.getCompositionStart()
//                                ? new CommittedState(expectedState.getText(), expectedState.getCursorStart(), expectedState.getCursorEnd())
//                                : expectedState));
//                final String initialText = param.initialLeftText + param.initialComposedText + param.initialRightText;
//                final int rightCodePointCount = initialText.codePointCount(param.deleteEnd, initialText.length());
//                if (rightCodePointCount > 0) {
//                    list.add(new ActionTestCase<>(name + " :: and select to the end",
//                            settingsOption.data,
//                            false,
//                            new ComposedState(param.initialLeftText,
//                                    param.initialComposedText, param.initialRightText,
//                                    param.deleteEnd, initialText.length()),
//                            true,
//                            new DeleteTextBefore(deleteBeforeLength),
//                            deleteTextFromComposed(param.initialLeftText, param.initialComposedText,
//                                    param.initialRightText, param.deleteStart, param.deleteEnd,
//                                    param.deleteStart, initialText.length() - deleteBeforeLength)));
//                }
//                if (rightCodePointCount > 1) {
//                    final int cursorLength = getCharCountBeforeCodePoint(initialText.substring(param.deleteEnd), 1);
//                    list.add(new ActionTestCase<>(name + " :: and select one code point",
//                            settingsOption.data,
//                            false,
//                            new ComposedState(param.initialLeftText,
//                                    param.initialComposedText, param.initialRightText,
//                                    param.deleteEnd, param.deleteEnd + cursorLength),
//                            true,
//                            new DeleteTextBefore(deleteBeforeLength),
//                            deleteTextFromComposed(param.initialLeftText, param.initialComposedText,
//                                    param.initialRightText, param.deleteStart, param.deleteEnd,
//                                    param.deleteStart, param.deleteStart + cursorLength)));
//                }
//                if (param.deleteStart == 0) {
//                    list.add(new ActionTestCase<>(name + " :: and request deleting past the start",
//                            settingsOption.data,
//                            false,
//                            initialState,
//                            true,
//                            new DeleteTextBefore(deleteBeforeLength + 1),
//                            settingsOption.data.getTextLimit < Integer.MAX_VALUE
//                                    && expectedState.getCursorEnd() + settingsOption.data.getTextLimit < expectedState.getCompositionEnd()
//                                    && !settingsOption.data.setComposingRegionSupported
//                                    && initialState.getCursorStart() > initialState.getCompositionStart()
//                                    ? new CommittedState(expectedState.getText(), expectedState.getCursorStart(), expectedState.getCursorEnd())
//                                    : expectedState));
//                }
//            }
//        }
//
//        //TODO: add tests for deleting part of surrogate pair
//
//        return list.stream();
//    }
//
//    private static List<Named<FakeInputConnectionSettings>> getDeleteBeforeSettingsOptions() {
//        final List<Named<FakeInputConnectionSettings>> list = new ArrayList<>();
//        list.add(new Named<>("setComposingRegion not supported and delete through the composing text and unlimited get text length",
//                new FakeInputConnectionSettings(false, false)));
//        list.add(new Named<>("setComposingRegion not supported and delete around the composing text and unlimited get text length",
//                new FakeInputConnectionSettings(false, true)));
//        list.add(new Named<>("setComposingRegion supported and delete through the composing text and unlimited get text length",
//                new FakeInputConnectionSettings(true, false)));
//        list.add(new Named<>("setComposingRegion supported and delete around the composing text and unlimited get text length",
//                new FakeInputConnectionSettings(true, true)));
//        list.add(new Named<>("setComposingRegion not supported and delete through the composing text and limited get text length",
//                new FakeInputConnectionSettings(5, false, false)));
//        list.add(new Named<>("setComposingRegion not supported and delete around the composing text and limited get text length",
//                new FakeInputConnectionSettings(5, false, true)));
//        list.add(new Named<>("setComposingRegion supported and delete through the composing text and limited get text length",
//                new FakeInputConnectionSettings(5, true, false)));
//        list.add(new Named<>("setComposingRegion supported and delete around the composing text and limited get text length",
//                new FakeInputConnectionSettings(5, true, true)));
//        return list;
//    }
//    //#endregion
//
//    //#region delete selected text parameters
//    private static Stream<ActionTestCase<DeleteSelected>> deleteSelectedText_committed_parameters() {
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getDeleteSelectedSettingsOptions();
//        final List<Named<ThreePartText>> deleteParams = getDeletingCommittedTextParams();
//        final List<ActionTestCase<DeleteSelected>> list = new ArrayList<>();
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            for (final boolean cursorPositionKnown : new boolean[] {false, true}) {
//                final boolean useBatch = !cursorPositionKnown;
//                final String nameExtra = " (cursor position " + (cursorPositionKnown ? "known" : "unknown") + ")";
//                for (final Named<ThreePartText> param : deleteParams) {
//                    final int initialPositionEnd = param.data.left.length() + param.data.center.length();
//                    final int initialPositionStart = param.data.left.length();
//                    final int updatedPosition = param.data.left.length();
//                    list.add(new ActionTestCase<>("select " + param.name + " :: " + settingsOption.name + nameExtra,
//                            settingsOption.data,
//                            useBatch,
//                            new CommittedState(param.data.left + param.data.center + param.data.right, initialPositionStart, initialPositionEnd),
//                            cursorPositionKnown,
//                            new DeleteSelected(),
//                            new CommittedState(param.data.left + param.data.right, updatedPosition, updatedPosition)));
//                }
//            }
//        }
//        return list.stream();
//    }
//
//    private static Stream<ActionTestCase<DeleteSelected>> deleteSelectedText_composed_parameters() {
//        final List<ActionTestCase<DeleteSelected>> list = new ArrayList<>();
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getDeleteSelectedSettingsOptions();
//        final List<DeletingComposedTextParams> params = getDeletingComposedTextParams();
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            for (final DeletingComposedTextParams param : params) {
//                final State initialState = new ComposedState(
//                        param.initialLeftText, param.initialComposedText, param.initialRightText,
//                        param.deleteStart, param.deleteEnd);
//                final State expectedState = deleteTextFromComposed(
//                        param.initialLeftText, param.initialComposedText, param.initialRightText,
//                        param.deleteStart, param.deleteEnd,
//                        param.deleteStart, param.deleteStart);
//                list.add(new ActionTestCase<>(param.name + " :: "+ settingsOption.name,
//                        settingsOption.data, false, initialState, true, new DeleteSelected(),
//                        expectedState));
//            }
//        }
//        return list.stream();
//    }
//
//    private static List<Named<FakeInputConnectionSettings>> getDeleteSelectedSettingsOptions() {
//        final List<Named<FakeInputConnectionSettings>> list = new ArrayList<>();
//        list.add(new Named<>("default input connection",
//                new FakeInputConnectionSettings()));
//        list.add(new Named<>("limited get text length",
//                new FakeInputConnectionSettings(5)));
//        return list;
//    }
//    //#endregion
//    //#endregion
//
//    //#region parameter component builders
//    private static List<Named<ThreePartText>> getDeletingCommittedTextParams() {
//        final List<Named<ThreePartText>> list = new ArrayList<>();
//
//        list.add(new Named<>("one from the end",
//                new ThreePartText("Lorem ipsum dolor sit ame", "t", "")));
//        list.add(new Named<>("multiple from the end",
//                new ThreePartText("Lorem ipsum dolor sit ", "amet", "")));
//        list.add(new Named<>("one in the middle",
//                new ThreePartText("Lorem ipsum dolo", "r", " sit amet")));
//        list.add(new Named<>("multiple in the middle",
//                new ThreePartText("Lorem ipsum ", "dolor", " sit amet")));
//        list.add(new Named<>("one to the beginning",
//                new ThreePartText("", "L", "orem ipsum dolor sit amet")));
//        list.add(new Named<>("multiple to the beginning",
//                new ThreePartText("", "Lorem", " ipsum dolor sit amet")));
//        list.add(new Named<>("all",
//                new ThreePartText("", "Lorem ipsum dolor sit amet", "")));
//
//        //TODO: add surrogate pairs, delete nothing
//
//        return list;
//    }
//
//    private static List<DeletingComposedTextParams> getDeletingComposedTextParams() {
//        final List<DeletingComposedTextParams> list = new ArrayList<>();
//
//        final List<SurroundingTextParams> surroundingTextParams = getInitialSurroundingTextParams();
//        final List<ChangingTextParams> addingTextParams = getAddingTextParams();
//        for (final SurroundingTextParams surroundingText : surroundingTextParams) {
//            for (final ChangingTextParams addingText : addingTextParams) {
//                //TODO: possibly split getSelectionCursorPositionParams into something for this specifically since the name isn't completely accurate
//                final List<InitialCursorPositionParams> initialCursorPositions = getSelectionCursorPositionParams(surroundingText, addingText.update);
//
//                for (final InitialCursorPositionParams initialCursorPosition : initialCursorPositions) {
//                    //TODO: fix how the name is built
//                    list.add(new DeletingComposedTextParams(getName(surroundingText, addingText, initialCursorPosition),
//                            surroundingText.before, addingText.update, surroundingText.after,
//                            initialCursorPosition.start, initialCursorPosition.end));
//                }
//            }
//        }
//
//        return list;
//    }
//
//    private static State deleteTextFromComposed(final String leftText, final String composedText,
//                                                final String rightText, final int deleteStart,
//                                                final int deleteEnd, final int newCursorStart,
//                                                final int newCursorEnd) {
//        final int[] edges = new int[] {
//                0,
//                leftText.length(),
//                leftText.length() + composedText.length(),
//                leftText.length() + composedText.length() + rightText.length()
//        };
//        final String[] textPieces = new String[] { leftText, composedText, rightText };
//        for (int i = 0; i < textPieces.length; i++) {
//            final int pieceStart = edges[i];
//            final int pieceEnd = edges[i + 1];
//            if (deleteEnd <= pieceStart || deleteStart >= pieceEnd) {
//                // not deleting from this piece, so keep it all
//                continue;
//            }
//            if (deleteStart > pieceStart && deleteEnd < pieceEnd) {
//                // deleting the middle of this piece
//                textPieces[i] = textPieces[i].substring(0, deleteStart - pieceStart)
//                        + textPieces[i].substring(deleteEnd - pieceStart);
//            } else if (deleteEnd >= pieceEnd) {
//                // deleting to the end of the piece (including possibly the whole piece)
//                textPieces[i] = textPieces[i].substring(0, Math.max(0, deleteStart - pieceStart));
//            } else {
//                // deleting part of the beginning of the piece
//                textPieces[i] = textPieces[i].substring(deleteEnd - pieceStart);
//            }
//        }
//        final State updatedState;
//        if (textPieces[1].length() == 0) {
//            //TODO: handle keepEmptyComposingPosition setting and add test cases for it
//            updatedState = new CommittedState(textPieces[0] + textPieces[2], newCursorStart, newCursorEnd);
//        } else {
//            updatedState = new ComposedState(textPieces[0], textPieces[1], textPieces[2], newCursorStart, newCursorEnd);
//        }
//        return updatedState;
//    }
//    //#endregion
//
//    //TODO: rename better
//    private static class ThreePartText {
//        public final String left;
//        public final String center;
//        public final String right;
//        public ThreePartText(final String left, final String center, final String right) {
//            this.left = left;
//            this.center = center;
//            this.right = right;
//        }
//    }
//    //TODO: rename better
//    private static class TwoPartText {
//        public final String left;
//        public final String right;
//        public TwoPartText(final String left, final String right) {
//            this.left = left;
//            this.right = right;
//        }
//    }
//
//    //TODO: try to make this more reusable
//    private static class DeletingComposedTextParams {
//        public final String name;
//        public final String initialLeftText;
//        public final String initialComposedText;
//        public final String initialRightText;
//        public final int deleteStart;
//        public final int deleteEnd;
//        public DeletingComposedTextParams(final String name, final String initialLeftText, final String initialComposedText, final String initialRightText, final int deleteStart, final int deleteEnd) {
//            this.name = name;
//            this.initialLeftText = initialLeftText;
//            this.initialComposedText = initialComposedText;
//            this.initialRightText = initialRightText;
//            this.deleteStart = deleteStart;
//            this.deleteEnd = deleteEnd;
//        }
//    }
//    //#endregion
//
//    //#region sendKeyEvent
//    //#region KEYCODE_DEL
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("sendKeyEvent_delete_committed_parameters")
//    public void sendKeyEvent_delete_committed(final ActionTestCase<SendDeleteKey> params) {
//        System.out.println("ParameterizedTest sendKeyEvent_delete_committed");
//        testAction2(params);
//    }
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("sendKeyEvent_delete_composed_parameters")
//    public void sendKeyEvent_delete_composed(final ActionTestCase<SendDeleteKey> params) {
//        System.out.println("ParameterizedTest sendKeyEvent_delete_composed");
//        testAction2(params);
//    }
//
//    private static Stream<ActionTestCase<SendDeleteKey>> sendKeyEvent_delete_committed_parameters() {
//        final List<ActionTestCase<SendDeleteKey>> list = new ArrayList<>();
//
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getDeleteSelectedSettingsOptions();//TODO: rename since this is being reused
//        final boolean cursorPositionKnown = true;
//        final boolean useBatch = false;
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            list.add(new ActionTestCase<>("delete nothing at beginning of text",
//                    settingsOption,
//                    useBatch,
//                    new CommittedState("", "Lorem ipsum dolor sit amet"),
//                    cursorPositionKnown,
//                    new SendDeleteKey(),
//                    new CommittedState("", "Lorem ipsum dolor sit amet")));
//
//            list.add(new ActionTestCase<>("delete single character",
//                    settingsOption,
//                    useBatch,
//                    new CommittedState("Lorem ipsum dolor", " sit amet"),
//                    cursorPositionKnown,
//                    new SendDeleteKey(),
//                    new CommittedState("Lorem ipsum dolo", " sit amet")));
//
//            list.add(new ActionTestCase<>("delete surrogate pair",
//                    settingsOption,
//                    useBatch,
//                    new CommittedState("Lorem ipsum dolor" + getSurrogatePairString(0), " sit amet"),
//                    cursorPositionKnown,
//                    new SendDeleteKey(),
//                    new CommittedState("Lorem ipsum dolor", " sit amet")));
//
//            list.add(new ActionTestCase<>("delete selected text",
//                    settingsOption,
//                    useBatch,
//                    new CommittedState("Lorem ipsum ", "dolor", " sit amet"),
//                    cursorPositionKnown,
//                    new SendDeleteKey(),
//                    new CommittedState("Lorem ipsum ", " sit amet")));
//        }
//
//        return list.stream();
//    }
//
//    private static Stream<ActionTestCase<SendDeleteKey>> sendKeyEvent_delete_composed_parameters() {
//        final List<ActionTestCase<SendDeleteKey>> list = new ArrayList<>();
//
//        final List<Named<FakeInputConnectionSettings>> settingsOptions = getDeleteSelectedSettingsOptions();//TODO: rename since this is being reused
//        final boolean cursorPositionKnown = true;
//        final boolean useBatch = false;
//        for (final Named<FakeInputConnectionSettings> settingsOption : settingsOptions) {
//            list.add(new ActionTestCase<>("delete nothing at beginning of text",
//                    settingsOption,
//                    useBatch,
//                    new ComposedState("", "Lorem ipsum dolor sit amet", "",
//                            ComposedTextPosition.BEGINNING_OF_TEXT),
//                    cursorPositionKnown,
//                    new SendDeleteKey(),
//                    new ComposedState("", "Lorem ipsum dolor sit amet", "",
//                            ComposedTextPosition.BEGINNING_OF_TEXT)));
//
//            final Named<String>[] singleCodePointOptions = new Named[]{
//                    new Named<>("single character", "a"),
//                    new Named<>("surrogate pair", getSurrogatePairString(0)),
//            };
//            for (final Named<String> singleCodePointOption : singleCodePointOptions) {
//                final String codePointType = singleCodePointOption.name;
//                final String codePointString = singleCodePointOption.data;
//                list.add(new ActionTestCase<>("delete " + codePointType + " before composing text",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum" + codePointString + " ", "dolor", " sit amet",
//                                ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lorem ipsum ", "dolor", " sit amet",
//                                ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION)));
//
//                list.add(new ActionTestCase<>("delete " + codePointType + " in composing text",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum ", "dolo" + codePointString + "r", " sit amet",
//                                ComposedTextPosition.BEFORE_END_OF_COMPOSITION),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lorem ipsum ", "dolor", " sit amet",
//                                ComposedTextPosition.BEFORE_END_OF_COMPOSITION)));
//
//                list.add(new ActionTestCase<>("delete " + codePointType + " after composing text",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum ", "dolor", " sit ame" + codePointString + "t",
//                                ComposedTextPosition.BEFORE_END_OF_TEXT),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lorem ipsum ", "dolor", " sit amet",
//                                ComposedTextPosition.BEFORE_END_OF_TEXT)));
//            }
//
//            final Named<String>[] simpleSelectionCodePointsOptions = new Named[]{
//                    new Named<>("regular characters", "abc"),
//                    new Named<>("surrogate pairs", getSurrogatePairString(0, 3)),
//                    new Named<>("mixed surrogate pairs and regular characters", getSurrogatePairString(1) + "a" + getSurrogatePairString(1)),
//            };
//            for (final Named<String> selectionCodePointsOption : simpleSelectionCodePointsOptions) {
//                final String selectionType = selectionCodePointsOption.name;
//                final String selectionString = selectionCodePointsOption.data;
//
//                list.add(new ActionTestCase<>("delete selection of " + selectionType + " before composition",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("L" + selectionString + "o", "rem ipsum dolor", " sit amet",
//                                ComposedTextPosition.AFTER_BEGINNING_OF_TEXT, ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lo", "rem ipsum dolor", " sit amet",
//                                ComposedTextPosition.AFTER_BEGINNING_OF_TEXT)));
//
//                list.add(new ActionTestCase<>("delete selection of " + selectionType + " in composition",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum dol", "o" + selectionString + "r", " sit amet",
//                                ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION, ComposedTextPosition.BEFORE_END_OF_COMPOSITION),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lorem ipsum dol", "or", " sit amet",
//                                ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION)));
//
//                list.add(new ActionTestCase<>("delete selection of " + selectionType + " after composition",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum ", "dolor sit am", "e" + selectionString + "t",
//                                ComposedTextPosition.AFTER_END_OF_COMPOSITION, ComposedTextPosition.BEFORE_END_OF_TEXT),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lorem ipsum ", "dolor sit am", "et",
//                                ComposedTextPosition.AFTER_END_OF_COMPOSITION)));
//            }
//
//            final Named<TwoPartText>[] splitSelectionCodePointsOptions = new Named[]{
//                    new Named<>("regular characters", new TwoPartText("a", "b")),
//                    new Named<>("surrogate pairs", new TwoPartText(getSurrogatePairString(0), getSurrogatePairString(1))),
//                    new Named<>("mixed surrogate pairs and regular characters", new TwoPartText(getSurrogatePairString(0), "b"))
//            };
//            for (final Named<TwoPartText> selectionCodePointsOption : splitSelectionCodePointsOptions) {
//                final String selectionType = selectionCodePointsOption.name;
//                final String selectionStringA = selectionCodePointsOption.data.left;
//                final String selectionStringB = selectionCodePointsOption.data.right;
//
//                list.add(new ActionTestCase<>("delete selection of " + selectionType + " through the beginning of the composition",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum " + selectionStringA, selectionStringB + "dolor", " sit amet",
//                                ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION, ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lorem ipsum ", "dolor", " sit amet",
//                                ComposedTextPosition.BEGINNING_OF_COMPOSITION)));
//
//                list.add(new ActionTestCase<>("delete selection of " + selectionType + " past the end of the composition",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum ", "dolor" + selectionStringA, selectionStringB + " sit amet",
//                                ComposedTextPosition.BEFORE_END_OF_COMPOSITION, ComposedTextPosition.AFTER_END_OF_COMPOSITION),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new ComposedState("Lorem ipsum ", "dolor", " sit amet",
//                                ComposedTextPosition.END_OF_COMPOSITION)));
//            }
//
//            final Named<ThreePartText>[] composedSelectionCodePointsOptions = new Named[]{
//                    new Named<>("regular characters", new ThreePartText("a", "bc", "d")),
//                    new Named<>("surrogate pairs", new ThreePartText(getSurrogatePairString(0),
//                            getSurrogatePairString(1, 2), getSurrogatePairString(3))),
//                    new Named<>("mixed surrogate pairs and regular characters", new ThreePartText(
//                            getSurrogatePairString(0), "b" + getSurrogatePairString(2), "d")),
//            };
//            for (final Named<ThreePartText> selectionCodePointsOption : composedSelectionCodePointsOptions) {
//                final String selectionType = selectionCodePointsOption.name;
//                //TODO: handle keepEmptyComposingPosition setting and add test cases for it
//                list.add(new ActionTestCase<>("delete selection of " + selectionType + " covering the whole composition",
//                        settingsOption,
//                        useBatch,
//                        new ComposedState("Lorem ipsum dolor" + selectionCodePointsOption.data.left,
//                                selectionCodePointsOption.data.center,
//                                selectionCodePointsOption.data.right + " sit amet",
//                                ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION, ComposedTextPosition.AFTER_END_OF_COMPOSITION),
//                        cursorPositionKnown,
//                        new SendDeleteKey(),
//                        new CommittedState("Lorem ipsum dolor", " sit amet")));
//            }
//        }
//
//        return list.stream();
//    }
////    private static Stream<ActionTestCase<SendDeleteKey>> sendKeyEvent_delete_composed_parameters() {
////        final List<ActionTestCase<SendDeleteKey>> list = new ArrayList<>();
////
////        list.add(new ActionTestCase<>("delete nothing at beginning of text",
////                new ComposedState("", "Lorem ipsum dolor sit amet", "",
////                        ComposedTextPosition.BEGINNING_OF_TEXT),
////                new SendDeleteKey(),
////                new ComposedState("", "Lorem ipsum dolor sit amet", "",
////                        ComposedTextPosition.BEGINNING_OF_TEXT)));
////
////        list.add(new ActionTestCase<>("delete single character before composing text",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsu ", "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION)));
////
////        list.add(new ActionTestCase<>("delete single character in composing text",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_END_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsum ", "dolr", " sit amet",
////                        ComposedTextPosition.BEFORE_END_OF_COMPOSITION)));
////
////        list.add(new ActionTestCase<>("delete single character after composing text",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_END_OF_TEXT),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsum ", "dolor", " sit amt",
////                        ComposedTextPosition.BEFORE_END_OF_TEXT)));
////
////        list.add(new ActionTestCase<>("delete selection before composition",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.AFTER_BEGINNING_OF_TEXT, ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new ComposedState("L ", "dolor", " sit amet",
////                        ComposedTextPosition.AFTER_BEGINNING_OF_TEXT)));
////        list.add(new ActionTestCase<>("delete selection in composition",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION, ComposedTextPosition.BEFORE_END_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsum ", "dr", " sit amet",
////                        ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION)));
////        list.add(new ActionTestCase<>("delete selection after composition",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.AFTER_END_OF_COMPOSITION, ComposedTextPosition.BEFORE_END_OF_TEXT),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsum ", "dolor", " t",
////                        ComposedTextPosition.AFTER_END_OF_COMPOSITION)));
////
////        list.add(new ActionTestCase<>("delete selection through the beginning of the composition",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION, ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsum", "olor", " sit amet",
////                        ComposedTextPosition.BEGINNING_OF_COMPOSITION)));
////        list.add(new ActionTestCase<>("delete selection past the end of the composition",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_END_OF_COMPOSITION, ComposedTextPosition.AFTER_END_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new ComposedState("Lorem ipsum ", "dolo", "sit amet",
////                        ComposedTextPosition.END_OF_COMPOSITION)));
////
////        //TODO: handle keepEmptyComposingPosition setting and add test cases for it
////        list.add(new ActionTestCase<>("delete selection covering the whole composition",
////                new ComposedState("Lorem ipsum ", "dolor", " sit amet",
////                        ComposedTextPosition.BEFORE_BEGINNING_OF_COMPOSITION, ComposedTextPosition.AFTER_END_OF_COMPOSITION),
////                new SendDeleteKey(),
////                new CommittedState("Lorem ipsum", "sit amet")));
////        //TODO: add test cases (surrogate pairs, settings?, others?)
////
////        return list.stream();
////    }
//    //#endregion
//
//    //#region KEYCODE_DPAD_LEFT/KEYCODE_DPAD_RIGHT
//    //TODO: add tests
//    // is this even necessary? the only reason this ever gets called is when there is no cursor
//    // position, so the only thing we'd really test is if it gets forwarded to the fake, which
//    // doesn't seem particularly valuable.
//    //#endregion
//
//    //#region KEYCODE_0 - KEYCODE_9
//    //TODO: add tests (or remove this whole case if possible)
//    //#endregion
//    //#endregion
//    //#endregion
//
//    //#region setSelection
//    @ParameterizedTest(name = "{index}: {0}")
//    @MethodSource("setSelection_parameters")
//    public void setSelection(final ActionTestCase<SetSelection> params) {
//        System.out.println("ParameterizedTest sendKeyEvent_delete_committed");
//        testAction2(params);
//    }
//
//    private static Stream<ActionTestCase<SetSelection>> setSelection_parameters() {
//        final List<ActionTestCase<SetSelection>> list = new ArrayList<>();
//
//        final String fullText = "Lorem ipsum dolor sit amet";
//        final String left = fullText.substring(0, fullText.length() / 3);
//        final String middle = fullText.substring(left.length(), fullText.length() * 2 / 3);
//        final String right = fullText.substring(left.length() + middle.length());
//
//        list.add(new ActionTestCase<>("move cursor to beginning of text",
//                new CommittedState(fullText, ""),
//                new SetSelection(0, 0),
//                new CommittedState("", fullText)));
//
//        list.add(new ActionTestCase<>("move cursor to end of text",
//                new CommittedState("", fullText),
//                new SetSelection(fullText.length(), fullText.length()),
//                new CommittedState(fullText, "")));
//
//        list.add(new ActionTestCase<>("select middle of text",
//                new CommittedState(fullText, ""),
//                new SetSelection(left.length(), left.length() + middle.length()),
//                new CommittedState(left, middle, right)));
//
//        list.add(new ActionTestCase<>("select part of composition and committed text",
//                new ComposedState(left, middle, right, ComposedTextPosition.END_OF_TEXT),
//                new SetSelection(left.length() + 1, fullText.length() - 1),
//                new ComposedState(left, middle, right,
//                        ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION,
//                        ComposedTextPosition.BEFORE_END_OF_TEXT)));
//
//        return list.stream();
//    }
//    //#endregion
//
//    //#region debug printing
//    private static String getDebugInfo(final State state, final int indentLength) {
//        return getDebugInfo(state, indentLength, null);
//    }
//    private static String getDebugInfo(final State state, final int indentLength, final String prefix) {
//        String indent = getIndent(indentLength);
//        String result;
//        //TODO: don't use instanceof
//        if (state instanceof ComposedState) {
//            final ComposedState composedState = (ComposedState) state;
//            result = indent + (prefix != null ? prefix + "TextBefore" : "textBefore") + "=\"" + composedState.textBefore + "\"\n"
//                    + indent + (prefix != null ? prefix + "ComposedText" : "composedText") + "=\"" + composedState.composedText + "\"\n"
//                    + indent + (prefix != null ? prefix + "TextAfter" : "textAfter") + "=\"" + composedState.textAfter + "\"\n";
//        } else {
//            result = indent + (prefix != null ? prefix + "Text" : "text") + "=\"" + state.getText() + "\"\n";
//        }
//        return result
//                + indent + (prefix != null ? prefix + "CursorStart" : "cursorStart") + "=" + state.getCursorStart() + "\n"
//                + indent + (prefix != null ? prefix + "CursorEnd" : "cursorEnd") + "=" + state.getCursorEnd() + "\n";
//    }
//    private static String getIndent(final int length) {
//        return repeatChar(' ', length);
//    }
//    private static String repeatChar(final char character, final int length) {
//        char[] indentArray = new char[length];
//        Arrays.fill(indentArray, character);
//        return new String(indentArray);
//    }
//
//    private static void printState(final State state, final int indentLength) {
//        String indent = getIndent(indentLength);
//        System.out.println(indent + "\"" + state.getText() + "\"");
//        if (state.getCompositionStart() >= 0) {
//            System.out.println(indent
//                    + getIndent(1 + state.getText().codePointCount(0, state.getCompositionStart()))
//                    + repeatChar('_', state.getText().codePointCount(state.getCompositionStart(), state.getCompositionEnd()))
//                    + getIndent(state.getText().codePointCount(state.getCompositionEnd(), state.getText().length()) + 1));
//        }
//        System.out.println(indent
//                + getIndent(1 + state.getText().codePointCount(0, state.getCursorStart()) - 1) + ">"
//                + getIndent(state.getText().codePointCount(state.getCursorStart(), state.getCursorEnd()))
//                + "<" + getIndent(-1 + state.getText().codePointCount(state.getCursorEnd(), state.getText().length()) + 1));
//    }
//
//    private static void printDebugInfo(final String name, final State initialState, final State updatedState) {
//        printDebugInfo(name, initialState, null, updatedState);
//    }
//    private static void printDebugInfo(final String name, final State initialState, final String actionInfo, final State updatedState) {
//        final int indentLength = 2;
//        System.out.println(name + ":\n"
//                + getDebugInfo(initialState, indentLength, "initial")
//                + (actionInfo != null ? getIndent(indentLength) + actionInfo + "\n" : "")
//                + getDebugInfo(updatedState, indentLength, "updated"));
//        System.out.println("initialState:");
//        printState(initialState, indentLength);
//        System.out.println("updatedState:");
//        printState(updatedState, indentLength);
//    }
//    //#endregion
//
//    //TODO: delete
//    //#region refactor helpers
//    private static <T extends Action> void verifyActionParamsDeleteBefore(final List<ActionTestCase<T>> expected, final List<ActionTestCase<T>> actual) {
//        if (actual.size() != expected.size()) {
//            throw new RuntimeException(actual.size() + " != " + expected.size());
//        }
//        for (int i = 0; i < expected.size(); i++) {
//            if (!stateEquals(actual.get(i).initialState, expected.get(i).initialState)) {
//                throw new RuntimeException(expected.get(i).testName + "\n" + "initialState:\n"
//                        + getDebugInfo(expected.get(i).initialState, 2, "expected")
//                        + getDebugInfo(actual.get(i).initialState, 2, "actual"));
//            }
//            if (!stringEquals(actual.get(i).actionParams.getDetails(), expected.get(i).actionParams.getDetails())) {
//                throw new RuntimeException(expected.get(i).testName + "\n" + "deleteBeforeLength:\n"
//                        + getIndent(2) + "expected: " + expected.get(i).actionParams.getDetails() + "\n"
//                        + getIndent(2) + "actual: " + actual.get(i).actionParams.getDetails() + "\n");
//            }
//            if (!stateEquals(actual.get(i).expectedState, expected.get(i).expectedState)) {
//                throw new RuntimeException(expected.get(i).testName + "\n" + "expectedState:\n"
//                        + getDebugInfo(expected.get(i).expectedState, 2, "expected")
//                        + getDebugInfo(actual.get(i).expectedState, 2, "actual"));
//            }
//        }
//    }
//
//    private static boolean stateEquals(final State a, final State b) {
//        return stringEquals(a.getText(), b.getText())
//                && stringEquals(a.getComposedText(), b.getComposedText())
//                && a.getCursorStart() == b.getCursorStart()
//                && a.getCursorEnd() == b.getCursorEnd()
//                && a.getCompositionStart() == b.getCompositionStart()
//                && a.getCompositionEnd() == b.getCompositionEnd();
//    }
//    private static boolean stringEquals(final String a, final String b) {
//        if (a == null) {
//            return b == null;
//        }
//        return a.equals(b);
//    }
//    //#endregion
//
//    //#region general testing helpers
//    //#region surrogate pairs
//    private static String getSurrogatePairString(final int index) {
//        return getSurrogatePairString(index, 1);
//    }
//
//    private static String getSurrogatePairString(final int index, final int length) {
//        final StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < length; i++) {
//            sb.appendCodePoint(getSurrogatePair(index + i));
//        }
//        return sb.toString();
//    }
//
//    private static int[] simplePrintableSurrogatePairIndices = new int[] {
//            0, 1, 4, 5, 6, 7, 9, 13, 17, 26, 34, 54, 1024, 1027, 1028, 1030, 1032, 1035, 1040, 1049, 1057, 1075, 1077, 1085, 1090, 2072, 2080, 2100
//    };
//
//    private static String getAlternatingSurrogatePairString(final int surrogatePairIndex, final int charIndex, final int length) {
//        final StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < length; i += 2) {
//            sb.appendCodePoint(getSurrogatePair(surrogatePairIndex + i));
//            if (sb.length() < length) {
//                sb.append((char) (charIndex + i + 1));
//            }
//        }
//        return sb.toString();
//    }
//
//    private static int getSurrogatePair(int index) {
//        // high surrogates: U+D800 to U+DBFF (1024 characters)
//        // low surrogates:  U+DC00 to U+DFFF (1024 characters)
//        final int charCount = 1024;
//        if (index < 0 || index >= charCount * charCount) {
//            throw new IllegalArgumentException("invalid index: " + index);
//        }
//        // iterate through both high and low surrogate pairs at the same time to avoid the same high
//        // or low surrogate from appearing nearby when iterating
//        final int hiIndex = (index + (index / charCount)) % charCount;
//        final int loIndex = index % charCount;
//        return getSurrogatePairCodePoint(HI_SURROGATE_START + hiIndex, LO_SURROGATE_START + loIndex);
//    }
//
//    final static int HI_SURROGATE_START = 0xD800;
//    final static int LO_SURROGATE_START = 0xDC00;
//    final static int SURROGATE_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;
//    private static int getSurrogatePairCodePoint(final int lead, final int trail) {
//        return (lead << 10) + trail + SURROGATE_OFFSET;
//    }
//    //#endregion
//
//    private static int getCharCountBeforeCodePoint(final String text, final int codePointOffset) {
//        return codePointOffset < 0
//                ? text.offsetByCodePoints(text.length(), codePointOffset)
//                : text.offsetByCodePoints(0, codePointOffset);
//    }
//
//    private static int getCharCountAfterCodePoint(final String text, final int codePointOffset) {
//        return text.length() - getCharCountBeforeCodePoint(text, codePointOffset);
//    }
//
//    private static String getSubstring(final String text, int codePointStart, int codePointEnd) {
//        if (codePointStart < 0) {
//            codePointStart = text.codePointCount(0, text.length()) + codePointStart;
//        }
//        if (codePointEnd < 0) {
//            codePointEnd = text.codePointCount(0, text.length()) + codePointEnd;
//        }
//        return text.substring(
//                text.offsetByCodePoints(0, codePointStart),
//                text.offsetByCodePoints(0, codePointEnd));
//    }
//    private static String getSubstring(final String text, final int codePointStart) {
//        return getSubstring(text, codePointStart, text.codePointCount(0, text.length()));
//    }
//
//    private static class Named<T> {
//        final String name;
//        final T data;
//        public Named(final String name, final T data) {
//            this.name = name;
//            this.data = data;
//
//        }
//
//        @Override
//        public String toString() {
//            return name;
//        }
//    }
//
//    public static String getName(final SurroundingTextParams surroundingText,
//                                 final ChangingTextParams changingText,
//                                 final InitialCursorPositionParams initialCursorPosition) {
//        return surroundingText.name + " :: " + changingText.name + " :: " + initialCursorPosition.name;
//    }
//    public static <T> String getName(final SurroundingTextParams surroundingText,
//                                     final Named<T> changingText,
//                                     final InitialCursorPositionParams initialCursorPosition) {
//        return surroundingText.name + " :: " + changingText.name + " :: " + initialCursorPosition.name;
//    }
//    public static String getName(final SurroundingTextParams surroundingText,
//                                 final ChangingTextParams changingText,
//                                 final NewCursorPositionParams cursorPosition) {
//        return surroundingText.name + " :: " + changingText.name + " :: " + cursorPosition.name;
//    }
//    public static String getName(final SurroundingTextParams surroundingText,
//                                 final ChangingTextParams changingText,
//                                 final NewCursorPositionParams cursorPosition,
//                                 final Named<FakeInputConnectionSettings> settingsOption) {
//        return surroundingText.name + " :: " + changingText.name + " :: " + cursorPosition.name + " :: " + settingsOption.name;
//    }
//    private static String getName(final SurroundingTextParams surroundingText,
//                                  final ChangingTextParams changingText,
//                                  final InitialCursorPositionParams initialCursorPosition,
//                                  final NewCursorPositionParams cursorPosition) {
//        return surroundingText.name + " :: " + changingText.name + " :: " + initialCursorPosition.name + " :: " + cursorPosition.name;
//    }
//    private static String getName(final SurroundingTextParams surroundingText,
//                                  final ChangingTextParams changingText,
//                                  final InitialCursorPositionParams initialCursorPosition,
//                                  final NewCursorPositionParams cursorPosition,
//                                  final Named<FakeInputConnectionSettings> settingsOption) {
//        return surroundingText.name + " :: " + changingText.name + " :: " + initialCursorPosition.name + " :: " + cursorPosition.name + " :: " + settingsOption.name;
//    }
//
//    private static class FakeInputConnectionSettings {
//        final int getTextLimit;
//        final boolean setComposingRegionSupported;
//        final boolean getSelectedTextSupported = true;
//        final boolean deleteAroundComposingText;
//        final boolean keepEmptyComposingPosition;
//        final FakeInputConnection.TextModifier textModifier;
//        public FakeInputConnectionSettings(final int getTextLimit,
//                                           final boolean setComposingRegionSupported,
//                                           final boolean deleteAroundComposingText,
//                                           final boolean keepEmptyComposingPosition,
//                                           final FakeInputConnection.TextModifier textModifier) {
//            this.getTextLimit = getTextLimit;
//            this.setComposingRegionSupported = setComposingRegionSupported;
//            this.deleteAroundComposingText = deleteAroundComposingText;
//            this.keepEmptyComposingPosition = keepEmptyComposingPosition;
//            this.textModifier = textModifier;
//        }
//        public FakeInputConnectionSettings() {
//            this(Integer.MAX_VALUE, true, false, false, null);
//        }
//        public FakeInputConnectionSettings(final int getTextLimit) {
//            this(getTextLimit, true, false, false, null);
//        }
//        //TODO: support keepEmptyComposingPosition and add test cases
//        public FakeInputConnectionSettings(final boolean setComposingRegionSupported, final boolean deleteAroundComposingText) {
//            this(Integer.MAX_VALUE, setComposingRegionSupported, deleteAroundComposingText, false, null);
//        }
//        public FakeInputConnectionSettings(final int getTextLimit, final boolean setComposingRegionSupported, final boolean deleteAroundComposingText) {
//            this(getTextLimit, setComposingRegionSupported, deleteAroundComposingText, false, null);
//        }
//        public FakeInputConnectionSettings(final FakeInputConnection.TextModifier textModifier) {
//            this(Integer.MAX_VALUE, true, false, false, textModifier);
//        }
//    }
//
//    private static class TestSettings {
//        public final FakeInputConnectionSettings inputConnectionSettings;
//        public final boolean initialCursorPositionKnown;
//        public TestSettings(final FakeInputConnectionSettings inputConnectionSettings,
//                            final boolean initialCursorPositionKnown) {
//            this.inputConnectionSettings = inputConnectionSettings;
//            this.initialCursorPositionKnown = initialCursorPositionKnown;
//        }
//    }
//
//    private static class AddTextOption {
//        public final Named<String> initialText;
//        public final Named<String> addingText;
//        public AddTextOption(final Named<String> initialText, final Named<String> addingText) {
//            this.initialText = initialText;
//            this.addingText = addingText;
//        }
//    }
//
//    private interface State {
//        String getText();
//        int getCursorStart();
//        int getCursorEnd();
//        int getCompositionStart();
//        int getCompositionEnd();
//        String getComposedText();
//    }
//
//    private static class CommittedState implements State {
//        public final String text;
//        private final int cursorStart;
//        private final int cursorEnd;
//        public CommittedState(final String text,
//                              final int cursorStart, final int cursorEnd) {
//            this.cursorStart = cursorStart;
//            this.cursorEnd = cursorEnd;
//            this.text = text;
//            if (cursorStart > cursorEnd || cursorStart < 0 || cursorEnd > text.length()) {
//                throw new IllegalArgumentException("Invalid cursor position: "
//                        + cursorStart + "," + cursorEnd
//                        + " text: \"" + text + "\"");
//            }
//        }
//        public CommittedState(final String text, final int cursorPosition) {
//            this(text, cursorPosition, cursorPosition);
//        }
//        public CommittedState(final String text) {
//            this(text, text.length());
//        }
//        public CommittedState(final String text, final InitialCursorPositionParams cursorPosition) {
//            this(text, cursorPosition.start, cursorPosition.end);
//        }
//        public CommittedState(final SurroundingTextParams surroundingText,
//                              final String selectedText) {
//            this(surroundingText.before + selectedText + surroundingText.after,
//                    surroundingText.before.length(),
//                    surroundingText.before.length() + selectedText.length());
//        }
//        public CommittedState(final String beforeText, final String selectedText,
//                              final String afterText) {
//            this(beforeText + selectedText + afterText,
//                    beforeText.length(),
//                    beforeText.length() + selectedText.length());
//        }
//        public CommittedState(final String beforeText, final String afterText) {
//            this(beforeText, "", afterText);
//        }
//
//        public String getText() {
//            return text;
//        }
//        public int getCursorStart() {
//            return cursorStart;
//        }
//        public int getCursorEnd() {
//            return cursorEnd;
//        }
//        public int getCompositionStart() {
//            return -1;
//        }
//        public int getCompositionEnd() {
//            return -1;
//        }
//        public String getComposedText() {
//            return null;
//        }
//    }
//
//    private static class ComposedState implements State {
//        private final String textBefore;
//        private final String composedText;
//        private final String textAfter;
//        private final int cursorStart;
//        private final int cursorEnd;
//        public ComposedState(final String textBefore, final String composedText,
//                             final String textAfter, final int cursorStart, final int cursorEnd) {
//            this.cursorStart = cursorStart;
//            this.cursorEnd = cursorEnd;
//            this.textBefore = textBefore;
//            this.composedText = composedText;
//            this.textAfter = textAfter;
//            if (cursorStart > cursorEnd || cursorStart < 0
//                    || cursorEnd > textBefore.length() + composedText.length() + textAfter.length()) {
//                throw new IllegalArgumentException("Invalid cursor position: "
//                        + cursorStart + "," + cursorEnd
//                        + " text: \"" + textBefore + composedText + textAfter + "\"");
//            }
//        }
//        public ComposedState(final String textBefore, final String composedText,
//                             final String textAfter, final int cursorPosition) {
//            this(textBefore, composedText, textAfter, cursorPosition, cursorPosition);
//        }
//        public ComposedState(final String textBefore, final String composedText,
//                             final String textAfter) {
//            this(textBefore, composedText, textAfter, textBefore.length() + composedText.length());
//        }
//        public ComposedState(final String textBefore, final String composedText,
//                             final String textAfter,
//                             final InitialCursorPositionParams cursorPosition) {
//            this(textBefore, composedText, textAfter, cursorPosition.start, cursorPosition.end);
//        }
//        public ComposedState(final String textBefore, final String composedText,
//                             final String textAfter,
//                             final ComposedTextPosition cursorPosition) {
//            this(textBefore, composedText, textAfter, cursorPosition, cursorPosition);
//        }
//        public ComposedState(final String textBefore, final String composedText,
//                             final String textAfter,
//                             final ComposedTextPosition cursorStart,
//                             final ComposedTextPosition cursorEnd) {
//            this(textBefore, composedText, textAfter,
//                    getAbsolutePosition(textBefore, composedText, textAfter, cursorStart),
//                    getAbsolutePosition(textBefore, composedText, textAfter, cursorEnd));
//        }
//        public ComposedState(final SurroundingTextParams surroundingText,
//                             final ChangingTextParams changingText,
//                             final InitialCursorPositionParams initialCursorPosition) {
//            this(surroundingText.before, changingText.initial, surroundingText.after,
//                    initialCursorPosition);
//        }
//        public ComposedState(final SurroundingTextParams surroundingText,
//                             final String composedText,
//                             final InitialCursorPositionParams initialCursorPosition) {
//            this(surroundingText.before, composedText, surroundingText.after,
//                    initialCursorPosition);
//        }
//
//        public String getText() {
//            return textBefore + composedText + textAfter;
//        }
//        public int getCursorStart() {
//            return cursorStart;
//        }
//        public int getCursorEnd() {
//            return cursorEnd;
//        }
//        public int getCompositionStart() {
//            return textBefore.length();
//        }
//        public int getCompositionEnd() {
//            return textBefore.length() + composedText.length();
//        }
//        public String getComposedText() {
//            return composedText;
//        }
//    }
//    private enum ComposedTextPosition {
//        BEGINNING_OF_TEXT,
//        AFTER_BEGINNING_OF_TEXT,
//        BEFORE_BEGINNING_OF_COMPOSITION,
//        BEGINNING_OF_COMPOSITION,
//        AFTER_BEGINNING_OF_COMPOSITION,
//        BEFORE_END_OF_COMPOSITION,
//        END_OF_COMPOSITION,
//        AFTER_END_OF_COMPOSITION,
//        BEFORE_END_OF_TEXT,
//        END_OF_TEXT
//    }
//    private static int getAbsolutePosition(final String textBefore, final String composedText,
//                                    final String textAfter, final ComposedTextPosition position) {
//        switch (position) {
//            case BEGINNING_OF_TEXT:
//                return 0;
//            case AFTER_BEGINNING_OF_TEXT:
//                return getCharCountBeforeCodePoint(textBefore, 1);
//            case BEFORE_BEGINNING_OF_COMPOSITION:
//                return getCharCountBeforeCodePoint(textBefore, -1);
//            case BEGINNING_OF_COMPOSITION:
//                return textBefore.length();
//            case AFTER_BEGINNING_OF_COMPOSITION:
//                return textBefore.length() + getCharCountBeforeCodePoint(composedText, 1);
//            case BEFORE_END_OF_COMPOSITION:
//                return textBefore.length() + getCharCountBeforeCodePoint(composedText, -1);
//            case END_OF_COMPOSITION:
//                return textBefore.length() + composedText.codePointCount(0, composedText.length());
//            case AFTER_END_OF_COMPOSITION:
//                return textBefore.length() + composedText.length()
//                        + getCharCountBeforeCodePoint(textAfter, 1);
//            case BEFORE_END_OF_TEXT:
//                return textBefore.length() + composedText.length()
//                        + getCharCountBeforeCodePoint(textAfter, -1);
//            case END_OF_TEXT:
//                return textBefore.length() + composedText.length() + composedText.length();
//            default:
//                throw new IllegalArgumentException("Invalid enum: " + position);
//        }
//    }
//
//    private enum AddingTextPosition {
//        BEGINNING_OF_FULL_TEXT,
//        BEGINNING_OF_NEW_TEXT,
//        END_OF_NEW_TEXT,
//        END_OF_FULL_TEXT
//    }
//
//    private static class RelativePosition {
//        final AddingTextPosition referencePoint;
//        final int codePointOffset;
//        public RelativePosition(final AddingTextPosition referencePoint,
//                                final int codePointOffset) {
//            this.referencePoint = referencePoint;
//            this.codePointOffset = codePointOffset;
//        }
//    }
//
//    private void setUpState(final FakeInputConnectionSettings settings, final State state, final boolean cursorPositionKnown) {
//        if (state.getCompositionStart() >= 0) {
//            final int setupInitialPosition = state.getCompositionStart();
//            final String textBefore = state.getText().substring(0, state.getCompositionStart());
//            final String composedText = state.getText().substring(state.getCompositionStart(), state.getCompositionEnd());
//            final String textAfter = state.getText().substring(state.getCompositionEnd());
//            setup(settings, textBefore + textAfter, setupInitialPosition, setupInitialPosition);
//            //TODO: handle cursorPositionKnown? it shouldn't make a difference because the input
//            // connection should send an update with the selection or should we block that to
//            // simulate a broken input connection? that doesn't really seem valid to test.
//            // we could just start a batch and expect the general test handling to release the batch
//            // at some point (this might get confusing since the committed initial state doesn't
//            // need to do this - and probably shouldn't).
//            richInputConnection.resetState(setupInitialPosition, setupInitialPosition);
//            richInputConnection.reloadCachesForStartingInputView();
//            richInputConnection.beginBatchEdit();
//            richInputConnection.setComposingText(composedText, 1);
//            richInputConnection.endBatchEdit();
//            moveCursor(state.getCursorStart(), state.getCursorEnd());
//        } else {
//            setup(settings, state.getText(), state.getCursorStart(), state.getCursorEnd());
//            if (cursorPositionKnown) {
//                richInputConnection.resetState(state.getCursorStart(), state.getCursorEnd());
//                richInputConnection.reloadCachesForStartingInputView();
//            }
//        }
//        System.out.println(richInputConnection.getDebugState());
//        System.out.println("Finished setting up state\n");
//    }
//
//    private void moveCursor(final int start, final int end) {
//        richInputConnection.setSelection(start, end, true);
//    }
//
//    private void verifyState(final UpdateSelectionCall lastUpdateSelectionCall, final boolean lastUpdateExpected) {
//        assertNotEquals(0, updateSelectionCalls.size());
//        assertEquals(lastUpdateSelectionCall, updateSelectionCalls.get(updateSelectionCalls.size() - 1));
//        assertNotEquals(0, expectedUpdateSelectionCalls.size());
//        int updateResult = expectedUpdateSelectionCalls.get(expectedUpdateSelectionCalls.size() - 1);
//        boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
//        boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
//        assertEquals(lastUpdateExpected, !updateImpactedSelection);
//    }
//
//    private void verifyActualText(final String text, final String composingText) {
//        assertEquals(text, fakeInputConnection.getText());
//        assertEquals(composingText, fakeInputConnection.getComposingText());
//    }
//
//    private static class UpdateSelectionCall {
//        public final int oldSelStart;
//        public final int oldSelEnd;
//        public final int newSelStart;
//        public final int newSelEnd;
//        public final int candidatesStart;
//        public final int candidatesEnd;
//
//        public UpdateSelectionCall(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
//            this.oldSelStart = oldSelStart;
//            this.oldSelEnd = oldSelEnd;
//            this.newSelStart = newSelStart;
//            this.newSelEnd = newSelEnd;
//            this.candidatesStart = candidatesStart;
//            this.candidatesEnd = candidatesEnd;
//        }
//
//        @Override
//        public boolean equals(final Object o) {
//            if (!(o instanceof UpdateSelectionCall)) {
//                return false;
//            }
//            final UpdateSelectionCall other = (UpdateSelectionCall)o;
//            return oldSelStart == other.oldSelStart
//                    && oldSelEnd == other.oldSelEnd
//                    && newSelStart == other.newSelStart
//                    && newSelEnd == other.newSelEnd
//                    && candidatesStart == other.candidatesStart
//                    && candidatesEnd == other.candidatesEnd;
//        }
//
//        @Override
//        public int hashCode() {
//            final int prime = 31;
//            int hash = 1;
//            hash = hash * prime + oldSelStart;
//            hash = hash * prime + oldSelEnd;
//            hash = hash * prime + newSelStart;
//            hash = hash * prime + newSelEnd;
//            hash = hash * prime + candidatesStart;
//            hash = hash * prime + candidatesEnd;
//            return hash;
//        }
//
//        @Override
//        public String toString() {
//            return "oss=" + oldSelStart + ", ose=" + oldSelEnd
//                    + ", nss=" + newSelStart + ", nse=" + newSelEnd
//                    + ", cs=" + candidatesStart + ", ce=" + candidatesEnd;
//        }
//    }
//
//    //#region generic action testing
//    public <T extends Action> void testAction(final ActionTestCase<T> testCase) {
//        testAction(testCase.settings, testCase.initialState, testCase.actionParams, testCase.expectedState);
//    }
//
//    public void testAction(final FakeInputConnectionSettings settings, final State initialState, final Action action, final State expectedState) {
//        printDebugInfo(action.getName(), initialState, action.getDetails(), expectedState);
//
//        setUpState(settings, initialState, true);
//        System.out.println("\n" + fakeInputConnection.getSettingsDebug() + "\n");
//
//        richInputConnection.beginBatchEdit();
//        action.doAction(richInputConnection);
//        richInputConnection.endBatchEdit();
//
//        verifyState(new UpdateSelectionCall(initialState.getCursorStart(), initialState.getCursorEnd(),
//                expectedState.getCursorStart(), expectedState.getCursorEnd(),
//                expectedState.getCompositionStart(), expectedState.getCompositionEnd()), true);
//        verifyActualText(expectedState.getText(), expectedState.getComposedText());
//    }
//
//    //TODO: if the update isn't going to be expected, we might need to also test with a batch
//    public <T extends Action> void testAction2(final ActionTestCase<T> testCase) {
//        System.out.println(testCase);
//        if (testCase.useBatch) {
//            testActionInBatch(testCase.settings, testCase.initialState, testCase.cursorPositionKnown, testCase.actionParams, testCase.expectedState, testCase.isExpectedChange);
//        } else {
//            testActionNoBatch(testCase.settings, testCase.initialState, testCase.cursorPositionKnown, testCase.actionParams, testCase.expectedState, testCase.isExpectedChange);
//        }
//    }
//
//    public void testActionNoBatch(final FakeInputConnectionSettings settings, final State initialState, final boolean cursorPositionKnown,
//                                  final Action action, final State expectedState, final boolean isExpectedUpdate) {
//        printDebugInfo(action.getName(), initialState, action.getDetails(), expectedState);
//
//        setUpState(settings, initialState, cursorPositionKnown);
//        System.out.println("\n" + fakeInputConnection.getSettingsDebug() + "\nNo batch");
//        updateSelectionCalls.clear();
//        expectedUpdateSelectionCalls.clear();
//
//        action.doAction(richInputConnection);
//
//        fakeInputConnection.forceUpdateSelectionCall();
//
//        assertNotEquals(0, updateSelectionCalls.size());
//        final UpdateSelectionCall lastCall = updateSelectionCalls.get(updateSelectionCalls.size() - 1);
//        assertEquals(expectedState.getCursorStart(), lastCall.newSelStart);
//        assertEquals(expectedState.getCursorEnd(), lastCall.newSelEnd);
//        assertEquals(expectedState.getCompositionStart(), lastCall.candidatesStart);
//        assertEquals(expectedState.getCompositionEnd(), lastCall.candidatesEnd);
//
//        assertNotEquals(0, expectedUpdateSelectionCalls.size());
//        if (isExpectedUpdate) {
//            for (final int updateResult : expectedUpdateSelectionCalls) {
//                boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
//                boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
//                assertTrue(!updateImpactedSelection);
//            }
//        } else {
//            boolean allExpected = true;
//            for (final int updateResult : expectedUpdateSelectionCalls) {
//                boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
//                boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
//                allExpected = allExpected && !updateImpactedSelection;
//            }
//            assertFalse(allExpected);
//        }
//
//        verifyTextCache(expectedState.getText().substring(0, expectedState.getCursorStart()),
//                expectedState.getText().substring(expectedState.getCursorStart(), expectedState.getCursorEnd()),
//                expectedState.getText().substring(expectedState.getCursorEnd()));
//
//        verifyActualText(expectedState.getText(), expectedState.getComposedText());
//    }
//    public void testActionInBatch(final FakeInputConnectionSettings settings,
//                                  final State initialState, final boolean cursorPositionKnown,
//                                  final Action action, final State expectedState,
//                                  final boolean isExpectedUpdate) {
//        printDebugInfo(action.getName(), initialState, action.getDetails(), expectedState);
//
//        setUpState(settings, initialState, cursorPositionKnown);
//        System.out.println("\n" + fakeInputConnection.getSettingsDebug() + "\nUse Batch");
//        updateSelectionCalls.clear();
//        expectedUpdateSelectionCalls.clear();
//
//        richInputConnection.beginBatchEdit();
//
//        action.doAction(richInputConnection);
//
//        assertEquals(0, updateSelectionCalls.size());
//        assertEquals(0, expectedUpdateSelectionCalls.size());
//
//        if (isExpectedUpdate) {
//            verifyTextCache(expectedState.getText().substring(0, expectedState.getCursorStart()),
//                    expectedState.getText().substring(expectedState.getCursorStart(), expectedState.getCursorEnd()),
//                    expectedState.getText().substring(expectedState.getCursorEnd()));
//        }
//
//        verifyActualText(expectedState.getText(), expectedState.getComposedText());
//
//        richInputConnection.endBatchEdit();
//
//        if (!isExpectedUpdate) {
//            verifyTextCache(expectedState.getText().substring(0, expectedState.getCursorStart()),
//                    expectedState.getText().substring(expectedState.getCursorStart(), expectedState.getCursorEnd()),
//                    expectedState.getText().substring(expectedState.getCursorEnd()));
//        }
//
//        assertEquals(1, updateSelectionCalls.size());
//        final UpdateSelectionCall call = updateSelectionCalls.get(0);
//        assertEquals(expectedState.getCursorStart(), call.newSelStart);
//        assertEquals(expectedState.getCursorEnd(), call.newSelEnd);
//        assertEquals(expectedState.getCompositionStart(), call.candidatesStart);
//        assertEquals(expectedState.getCompositionEnd(), call.candidatesEnd);
//
//        assertEquals(1, expectedUpdateSelectionCalls.size());
//        int updateResult = expectedUpdateSelectionCalls.get(0);
//        boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
//        boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
//        assertEquals(isExpectedUpdate, !updateImpactedSelection);
//
//        //TODO: is is valuable to test the text cache twice? if not, which should be kept
//        verifyTextCache(expectedState.getText().substring(0, expectedState.getCursorStart()),
//                expectedState.getText().substring(expectedState.getCursorStart(), expectedState.getCursorEnd()),
//                expectedState.getText().substring(expectedState.getCursorEnd()));
//    }
//
//    //#region test case classes
//    private static class ActionTestCase<T extends Action> {
//        final String testName;
//        final FakeInputConnectionSettings settings;
//        final boolean useBatch;
//        final State initialState;
//        final boolean cursorPositionKnown;
//        final T actionParams;
//        final State expectedState;
//        final boolean isExpectedChange;
//
//        public ActionTestCase(final String testName,
//                              final FakeInputConnectionSettings settings,
//                              final boolean useBatch,
//                              final State initialState,
//                              final boolean cursorPositionKnown,
//                              final T actionParams,
//                              final State expectedState,
//                              final boolean isExpectedChange) {
//            this.testName = testName;
//            this.settings = settings;
//            this.useBatch = useBatch;
//            this.initialState = initialState;
//            this.cursorPositionKnown = cursorPositionKnown;
//            this.actionParams = actionParams;
//            this.expectedState = expectedState;
//            this.isExpectedChange = isExpectedChange;
//        }
//
//        public ActionTestCase(final String testName,
//                              final FakeInputConnectionSettings settings,
//                              final boolean useBatch,
//                              final State initialState,
//                              final boolean cursorPositionKnown,
//                              final T actionParams,
//                              final State expectedState) {
//            this(testName, settings, useBatch, initialState, cursorPositionKnown, actionParams,
//                    expectedState,
//                    cursorPositionKnown
//                            || (settings.getSelectedTextSupported && actionParams instanceof AddText
//                                    && TextUtils.isEmpty(expectedState.getComposedText())));
//        }
//
//        public ActionTestCase(final String testName,
//                              final Named<TestSettings> settings,
//                              final boolean useBatch,
//                              final State initialState,
//                              final T actionParams,
//                              final State expectedState) {
//            //TODO: update the name better
//            this(testName + " (fake settings: " + settings.name + "; " + (useBatch ? "" : "don't ")
//                            + "use batch)", settings.data.inputConnectionSettings, useBatch,
//                    initialState, settings.data.initialCursorPositionKnown, actionParams,
//                    expectedState);
//        }
//        public ActionTestCase(final String testName,
//                              final Named<FakeInputConnectionSettings> settings,
//                              final boolean useBatch,
//                              final State initialState,
//                              final boolean cursorPositionKnown,
//                              final T actionParams,
//                              final State expectedState) {
//            //TODO: update the name better
//            this(testName + " (fake settings: " + settings.name + "; " + (useBatch ? "" : "don't ")
//                            + "use batch; initial cursor position "
//                            + (cursorPositionKnown ? "known" : "unknown") + ")", settings.data,
//                    useBatch, initialState, cursorPositionKnown, actionParams, expectedState);
//        }
//        public ActionTestCase(final String testName,
//                              final Named<TestSettings> settings,
//                              final State initialState,
//                              final T actionParams,
//                              final State expectedState) {
//            this(testName, settings, false, initialState, actionParams, expectedState);
//        }
//
//        public ActionTestCase(final String testName,
//                              final State initialState,
//                              final T actionParams,
//                              final State expectedState) {
//            this(testName, (FakeInputConnectionSettings) null, false, initialState, true,
//                    actionParams, expectedState, true);
//        }
//
//        @Override
//        public String toString() {
//            return testName;
//        }
//    }
//
//    public static abstract class Action {
//        private final String mName;
//        private final String mDetails;
//
//        protected Action(final String name, final String details) {
//            mName = name;
//            mDetails = details;
//        }
//
//        public String getName() {
//            return mName;
//        }
//
//        public String getDetails() {
//            return mDetails;
//        }
//
//        public abstract void doAction(RichInputConnection richInputConnection);
//    }
//
//    //#region specific actions
//    private static class AddText extends Action {
//        final String newText;
//        final boolean composeNewText;
//        final int newCursorPosition;
//        public AddText(final String newText, final boolean composeNewText, final int newCursorPosition) {
//            super("AddText",
//                    "new" + (composeNewText ? "Composing" : "Committing") + "Text=\"" + newText
//                            + "\", newCursorPosition=" + newCursorPosition);
//            this.newText = newText;
//            this.composeNewText = composeNewText;
//            this.newCursorPosition = newCursorPosition;
//        }
//
//        @Override
//        public void doAction(final RichInputConnection richInputConnection) {
//            if (composeNewText) {
//                richInputConnection.setComposingText(newText, newCursorPosition);
//            } else {
//                richInputConnection.commitText(newText, newCursorPosition);
//            }
//        }
//    }
//
//    private static class FinishComposing extends Action {
//        //TODO: consider using a single instance to avoid creating unnecessary objects
//        public FinishComposing() {
//            super("FinishComposingText", null);
//        }
//
//        @Override
//        public void doAction(final RichInputConnection richInputConnection) {
//            richInputConnection.finishComposingText();
//        }
//    }
//
//    private static class DeleteSelected extends Action {
//        //TODO: consider using a single instance to avoid creating unnecessary objects
//        public DeleteSelected() {
//            super("DeleteSelectedText", null);
//        }
//
//        @Override
//        public void doAction(final RichInputConnection richInputConnection) {
//            richInputConnection.deleteSelectedText();
//        }
//    }
//
//    private static class DeleteTextBefore extends Action {
//        final int beforeLength;
//        public DeleteTextBefore(final int beforeLength) {
//            super("DeleteTextBefore", "deleteBeforeLength=" + beforeLength);
//            this.beforeLength = beforeLength;
//        }
//
//        @Override
//        public void doAction(final RichInputConnection richInputConnection) {
//            richInputConnection.deleteTextBeforeCursor(beforeLength);
//        }
//    }
//
//    private static abstract class SendKey extends Action {
//        final int keyCode;
//        protected SendKey(final String name, final int keyCode) {
//            super(name, null);
//            this.keyCode = keyCode;
//        }
//
//        @Override
//        public void doAction(final RichInputConnection richInputConnection) {
//            richInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
//            richInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
//        }
//    }
//
//    private static class SendDeleteKey extends SendKey {
//        //TODO: consider using a single instance to avoid creating unnecessary objects
//        public SendDeleteKey() {
//            super("SendDeleteKey", KeyEvent.KEYCODE_DEL);
//        }
//    }
//
//    private static class SetSelection extends Action {
//        final int start;
//        final int end;
//        public SetSelection(final int start, final int end) {
//            super("SetSelection", "start=" + start + ", end=" + end);
//            this.start = start;
//            this.end = end;
//        }
//
//        @Override
//        public void doAction(final RichInputConnection richInputConnection) {
//            richInputConnection.setSelection(start, end, true);
//        }
//    }
//    //#endregion
//    //#endregion
//    //#endregion
//    //#endregion
}
