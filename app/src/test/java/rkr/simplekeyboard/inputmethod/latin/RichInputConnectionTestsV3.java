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

import static android.view.inputmethod.InputConnection.GET_TEXT_WITH_STYLES;

import android.inputmethodservice.InputMethodService;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.AlterSpecificTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.BlockCharactersTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.DoubleCapsTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.DoubleTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.ExtraTextTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.FlipTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.HalveTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.IncrementNumberTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.RepeatTextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.TextModifier;
import rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.VariableBehaviorSettings;
import rkr.simplekeyboard.inputmethod.latin.RichInputConnection.UpdatesReceivedHandler;
import rkr.simplekeyboard.inputmethod.latin.RichInputConnectionTestsV3.GetTextAroundCursorTest.GetTextAroundCursorTestCase;
import rkr.simplekeyboard.inputmethod.latin.RichInputConnection.LoadAndValidateCacheResult;
import rkr.simplekeyboard.inputmethod.latin.utils.RangeList.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static rkr.simplekeyboard.inputmethod.latin.EditorState.UNKNOWN_POSITION;
import static rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.EXTRACT_TEXT_CENTERED_ON_SELECTION;
import static rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.EXTRACT_TEXT_CENTERED_ON_SELECTION_END;
import static rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.EXTRACT_TEXT_CENTERED_ON_SELECTION_START;
import static rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.EXTRACT_TEXT_FROM_END;
import static rkr.simplekeyboard.inputmethod.latin.FakeInputConnection.EXTRACT_TEXT_FROM_START;
import static rkr.simplekeyboard.inputmethod.latin.EditorState.NONCHARACTER_CODEPOINT_PLACEHOLDER;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.FULL_REQUEST_COMPLETED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.SELECTION_LOADED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.SELECTION_UPDATED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.TEXT_CORRECTED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.TEXT_LOADED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.TEXT_REMOVED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.TEXT_REQUESTED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.TEXT_UPDATED;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.UPDATE_IMPACTED_SELECTION;
import static rkr.simplekeyboard.inputmethod.latin.RichInputConnection.UPDATE_WAS_EXPECTED;

@RunWith(Enclosed.class)
public class RichInputConnectionTestsV3 {
    //TODO: add flag for debug printing

    //#region test classes
    @RunWith(JUnit4.class)
    public static class DebuggingTest {
        @Test
        public void debuggingTest() {
            System.out.println("debuggingTest");
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "input connection adds extra characters with the new cursor position after the committed text",
//                    new VariableBehaviorSettings(new FakeInputConnection.DoubleTextModifier()),
//                    true,
//                    new CommittedState("Lorem ipsum dol", "or sit amet"),
//                    true,
//                    new AddText("asdf", true, 1),
//                    new ComposedState("Lorem ipsum dol", "aassddff", "or sit amet"),
//                    false);
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettings(),
//                    true,
//                    new CommittedState("Lorem ipsum d", "olor sit amet"),
//                    false,
//                    new AddText("a", true, 0),
//                    new ComposedState("Lorem ipsum d", "a", "olor sit amet", 13),
//                    false);
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettings(5),
//                    true,
//                    new CommittedState("Lorem ipsum d", "olor sit amet"),
//                    false,
//                    new AddText("test\uD801\uDC01", true, 0),
//                    new ComposedState("Lorem ipsum d", "test\uD801\uDC01", "olor sit amet", 13),
//                    false);
//            //TODO: this is failing when newCursorPosition is 0 because
//            // mExtractedTextMonitorRequests is empty because reloadTextCache isn't called to set
//            // the monitor. it does work when newCursorPosition is less (eg: -2) because the cursor
//            // isn't in the cached text, so it reloads. figure out if reloadTextCache is called in a
//            // real case when there is an unknown cursor position. if it is, rewrite the test setup
//            // to match, and if it isn't, see if there is a place to add that call or something else
//            // to add the monitor and update the test setup to match.
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettings(5),
//                    true,
//                    new CommittedState("Lorem ipsum d", "olor sit amet"),
//                    false,
//                    new AddText("testxy", true, 0),
//                    new ComposedState("Lorem ipsum d", "testxy", "olor sit amet", 13),
//                    false);
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettings(3, 3, false),
//                    false,
//                    new CommittedState("Lorem ipsum d", "olor sit amet"),
//                    true,
//                    new AddText("", false, -14),
//                    new CommittedState("Lorem ipsum dolor sit amet", 0),
//                    false);
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettings(),
//                    false,
//                    new CommittedState("Lorem ipsum d", "olor sit amet"),
//                    true,
//                    new AddText("", false, -14),
//                    new CommittedState("Lorem ipsum dolor sit amet", 0),
//                    false);
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettings(false),
//                    true,
//                    new CommittedState(""),
//                    false,
//                    new AddText("", false, 1),
//                    new CommittedState("", 0),
//                    false);
//            final ActionTestCase<SendDeleteKey> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettings(5, 5, false),
//                    false,
//                    new ComposedState("\uD800\uDC00\uD801\uDC01", "\uD802\uDC02\uD803\uDC03\uD804\uDC04\uD805\uDC05\uD806\uDC06\uD807\uDC07\uD808\uDC08", "\uD809\uDC09\uD80A\uDC0A\uD80B\uDC0B\uD80C\uDC0C\uD80D\uDC0D\uD80E\uDC0E\uD80F\uDC0F\uD810\uDC10\uD811\uDC11\uD812\uDC12\uD813\uDC13\uD814\uDC14\uD815\uDC15\uD816\uDC16\uD817\uDC17\uD818\uDC18\uD819\uDC19", 24),
//                    true,
//                    new SendDeleteKey(),
//                    new ComposedState("\uD800\uDC00\uD801\uDC01", "\uD802\uDC02\uD803\uDC03\uD804\uDC04\uD805\uDC05\uD806\uDC06\uD807\uDC07\uD808\uDC08", "\uD809\uDC09\uD80A\uDC0A\uD80C\uDC0C\uD80D\uDC0D\uD80E\uDC0E\uD80F\uDC0F\uD810\uDC10\uD811\uDC11\uD812\uDC12\uD813\uDC13\uD814\uDC14\uD815\uDC15\uD816\uDC16\uD817\uDC17\uD818\uDC18\uD819\uDC19", 22),
//                    true);
//            final ActionTestCase<DeleteTextBefore> testCase = new ActionTestCase<>(
//                    "debug",
//                    new VariableBehaviorSettingsBuilder()
//                            .setComposingRegionNotSupported()
//                            .limitReturnedText(5, false, 5)
//                            .build(),
//                    false,
//                    new CommittedState("Lorem ipsum dolor sit ame", "t"),
//                    true,
//                    new DeleteTextBefore(25),
//                    new CommittedState("t", 0),
//                    true);
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "input connection changes characters with the new cursor position after the composed text",
//                    new VariableBehaviorSettingsBuilder()
//                            .setTextModifier(new FakeInputConnection.IncrementNumberTextModifier()).build(),
//                    true,
//                    new ComposedState("Lorem ipsum dol", "asdf", "or sit amet"),
//                    true,
//                    new AddText("as1df", true, 1),
//                    new ComposedState("Lorem ipsum dol", "as2df", "or sit amet"),
//                    true, false);
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "input connection changes characters with the new cursor position after the composed text",
//                    new VariableBehaviorSettingsBuilder()
//                            .setTextModifier(new FakeInputConnection.DoubleTextModifier()).build(),
//                    true,
//                    new CommittedState("Lorem ipsum dol", "or sit amet"),
//                    true,
//                    new AddText("asdf", false, 6),
//                    new CommittedState("Lorem ipsum dolaassddffor sit amet", 28),
//                    false);
//            final ActionTestCase<SendUnicodeCharKey> testCase = new ActionTestCase<>(
//                    "replace composed " + "normal text"
//                            + " at the beginning of the text with " + "normal character",
//                    new Named<>("limited get text length with unknown cursor",
//                            new TestSettings(new VariableBehaviorSettingsBuilder()
//                                    .limitReturnedText(5, true, 5).blockBaseExtractText().build(),
//                                    false)),
//                    new ComposedState("Lorem ips", "um dolor", " sit amet",
//                            getCharCountBeforeCodePoint("Lorem ips", 1)),
//                    new SendUnicodeCharKey(new KeyEventInfo(KeyEvent.KEYCODE_0, '0', "KEYCODE_0")),
//                    new ComposedState(getSubstring("Lorem ips", 0, 1)
//                            + '0' + getSubstring("Lorem ips", 1),
//                            "um dolor", " sit amet",
//                            getCharCountBeforeCodePoint("L", 1) + 1));
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "replace composed " + "normal text"
//                            + " at the beginning of the text with " + "normal character",
//                    new Named<>("default input connection with known cursor",
//                            new TestSettings(new VariableBehaviorSettingsBuilder().build(), true)),
//                    new ComposedState("", "Lorem ipsum d", "olor sit amet"),
//                    new AddText("a", true, 1),
//                    new ComposedState("", "a", "olor sit amet"));
//            final ActionTestCase<AddText> testCase = new ActionTestCase<>(
//                    "add normal character to the middle of normal text and move the cursor to 1 after the beginning of the full text",
//                    new Named<>("default input connection with unknown cursor",
//                            new TestSettings(new VariableBehaviorSettingsBuilder().blockBaseExtractText().build(), false)),
//                    new CommittedState("Lorem ipsum d", "olor sit amet"),
//                    new AddText("a", true, -12),
//                    new ComposedState("Lorem ipsum d", "a", "olor sit amet", 1));
//            final ActionTestCase<DeleteTextBefore> testCase = new ActionTestCase<>(
//                    "",
//                    new Named<>("",
//                            new TestSettings(new VariableBehaviorSettingsBuilder()
//                                    .setComposingRegionNotSupported()
//                                    .getSelectedTextNotSupported()
//                                    .limitReturnedText(5, true, 5)
//                                    .build(),
//                                    true)),
//                    new ComposedState("", "Lo", "rem ipsum dolor sit amet", 3),
//                    new DeleteTextBefore(2),
//                    new ComposedState("", "L", "em ipsum dolor sit amet", 1));
            final ActionTestCase<SetSelection> testCase = new ActionTestCase<>(
                    "",
                    new Named<>("",
                            new TestSettings(new VariableBehaviorSettings()
                                    .setComposingRegionNotSupported()
                                    .getSelectedTextNotSupported()
                                    .limitReturnedText(5, true, 5),
                                    true)),
                    new CommittedState("Lorem ipsum dolor sit amet"),
                    new SetSelection(2, 24),
                    new CommittedState("Lo", "rem ipsum dolor sit am", "et"));
            final RichInputConnectionManager manager = new RichInputConnectionManager();
            manager.startRunAction(testCase);
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("change text before cursor and composition",
//                    new Named<>("limited get text length and limited full updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(5, false, false, 5),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 1, 4),
//                    new ComposedState("a123efghijklmnopqrstuvwxyz", 5, 10, 15, 20));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("debug",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, true, Integer.MAX_VALUE, false),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                    new ExternalActionSetText("123", 6, 6),
//                    new ComposedState("abcdef123ghijklmnopqrstuvwxyz", 18, 23, 5, 13));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor and cursor updated before text",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, true, Integer.MAX_VALUE, false),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 1, 1),
//                    new ComposedState("a123bcdefghijklmnopqrstuvwxyz", 8, 13, 18, 23));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and limited full updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, false, 5, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 16, 19),
//                    new ComposedState("abcdefghijklmnop123tuvwxyz", 5, 10, 15, 20));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, true, Integer.MAX_VALUE, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                    new ExternalActionSetText("123", 6, 9),
//                    new ComposedState("abcdef123jklmnopqrstuvwxyz", 15, 20, 5, 10));
//            manager.runExternalAction(testCase);
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("full updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    Integer.MAX_VALUE, true, true, Integer.MAX_VALUE, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                    new ExternalActionSetText("123", 16, 16),
//                    new ComposedState("abcdefghijklmnop123qrstuvwxyz", 15, 23, 5, 10));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("full updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    Integer.MAX_VALUE, true, true, Integer.MAX_VALUE, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                    new ExternalActionSetText("123", 11, 11),
//                    new ComposedState("abcdefghijk123lmnopqrstuvwxyz", 18, 23, 5, 10));
//            manager.startRunAction(testCase);
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, true, Integer.MAX_VALUE, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                    new ExternalActionSetText("123", 1, 1),
//                    new ComposedState("a123bcdefghijklmnopqrstuvwxyz", 18, 23, 8, 13));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("full updates with known cursor and cursor updated before text",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    Integer.MAX_VALUE, true, true, Integer.MAX_VALUE, false),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 1, 1),
//                    new ComposedState("a123bcdefghijklmnopqrstuvwxyz", 8, 13, 18, 23));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("full updates with known cursor and cursor updated before text",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    Integer.MAX_VALUE, true, true, Integer.MAX_VALUE, false),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 6, 6),
//                    new ComposedState("abcdef123ghijklmnopqrstuvwxyz", 5, 13, 18, 23));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, true, Integer.MAX_VALUE, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 6, 6),
//                    new ComposedState("abcdef123ghijklmnopqrstuvwxyz", 5, 13, 18, 23));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and limited full updates with known cursor and cursor updated before text",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, false, 5, false),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 1, 4),
//                    new ComposedState("a123efghijklmnopqrstuvwxyz", 5, 10, 15, 20));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor and cursor updated before text",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, true, Integer.MAX_VALUE, false),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                    new ExternalActionSetText("123", 1, 1),
//                    new ComposedState("a123bcdefghijklmnopqrstuvwxyz", 18, 23, 8, 13));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    5, false, true, Integer.MAX_VALUE, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                    new ExternalActionSetText("123", 9, 11),
//                    new ComposedState("abcdefghi123lmnopqrstuvwxyz", 16, 21, 5, 9));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettings(
//                                    3, false, true, Integer.MAX_VALUE, true),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 1, 2),
//                    new ComposedState("a123cdefghijklmnopqrstuvwxyz", 7, 12, 17, 22));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettingsBuilder()
//                                    .limitReturnedText(5, true, Integer.MAX_VALUE)
//                                    .blockBaseExtractText()
//                                    .updateSelectionBeforeExtractedText()
//                                    .build(),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionSetText("123", 11, 11),
//                    new ComposedState("abcdefghijk123lmnopqrstuvwxyz", 5, 10, 18, 23));
//            final ExternalActionTestCase testCase = new ExternalActionTestCase("debug",
//                    new Named<>("limited get text length and partial updates with known cursor",
//                            new TestSettings(new VariableBehaviorSettingsBuilder()
//                                    .limitReturnedText(5, true, Integer.MAX_VALUE)
//                                    .blockBaseExtractText()
////                                    .updateSelectionBeforeExtractedText()
//                                    .build(),
//                                    true)),
//                    new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                    new ExternalActionBatch<>(new ExternalActionSetText[] {
//                            new ExternalActionSetText("456", 22, 23),
//                            new ExternalActionSetText("1", 1, 4)}),
//                    new ComposedState("a1efghijklmnopqrstuv456xyz", 3, 8, 13, 18));
//            manager.runExternalAction(testCase);

            //testAccurateCache
//            if (!testCase.isExpectedChange) {
//                // the expected state should be wrong, so there is no use testing that
                manager.finishAction(testCase);
//            }
            manager.verifyTextCache(testCase);
            manager.verifyCompositionCache(testCase);

            //testUpdatedText
            manager.verifyActualText(testCase);

//            //testUpdateSelectionCall
//            manager.finishAction(testCase);
//            manager.verifyUpdateSelectionCall(testCase);
//
//            //testExpectedUpdateSelectionCalls
//            manager.finishAction(testCase);
//            manager.verifyExpectedUpdateSelectionCalls(testCase);
        }
    }

    public static abstract class ActionTestBase<T extends Action> {
        protected final ActionTestCase<T> testCase;
        protected final RichInputConnectionManager manager;

        public ActionTestBase(final ActionTestCase<T> testCase) {
            this.testCase = testCase;
            manager = new RichInputConnectionManager();
        }

        @Before
        public void setup() {
            manager.startRunAction(testCase);
        }

        @Test
        public void testAccurateGeneralCache() {
            if (!testCase.isExpectedTextChange) {
                // the expected state should be wrong, so there is no use testing that
                manager.finishAction(testCase);
            }
            manager.verifyTextCache(testCase);
        }

        @Test
        public void testAccurateCompositionCache() {
            if (!testCase.isExpectedTextChange) {
                // the expected state should be wrong, so there is no use testing that
                manager.finishAction(testCase);
            }
            manager.verifyCompositionCache(testCase);
        }

        @Test
        public void testUpdatedText() {
            manager.verifyActualText(testCase);
        }

        @Test
        public void testUpdateSelectionCall() {
            manager.finishAction(testCase);
            manager.verifyUpdateSelectionCall(testCase);
        }

        @Test
        public void testExpectedUpdateSelectionCalls() {
            manager.finishAction(testCase);
            manager.verifyExpectedUpdateSelectionCalls(testCase);
        }
    }

    @RunWith(Parameterized.class)
    public static class SetSelectionTest extends ActionTestBase<SetSelection> {
        public SetSelectionTest(final ActionTestCase<SetSelection> testCase) {
            super(testCase);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<ActionTestCase<SetSelection>> list = new GroupedList<>();

            final SplitText text = new SplitText("Lorem ipsum dolor sit amet");

            for (final Named<TestSettings> settingsOption : getSetSelectionSettingsOptions(true)) {
                final List<GenericTestStateWithAction<SetSelection>> cases = new ArrayList<>();
                cases.add(new GenericTestStateWithAction<>("move cursor to beginning of text",
                        new CommittedState(text.full, ""),
                        new SetSelection(0, 0),
                        new CommittedState("", text.full)));

                cases.add(new GenericTestStateWithAction<>("move cursor to end of text",
                        new CommittedState("", text.full),
                        new SetSelection(text.full.length(), text.full.length()),
                        new CommittedState(text.full, "")));

                cases.add(new GenericTestStateWithAction<>("select middle of text",
                        new CommittedState(text.full, ""),
                        new SetSelection(text.triLeft.length(),
                                text.triLeft.length() + text.triCenter.length()),
                        new CommittedState(text.triLeft, text.triCenter, text.triRight)));

                cases.add(new GenericTestStateWithAction<>("select part of composition and committed text",
                        new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                ComposedTextPosition.END_OF_TEXT),
                        new SetSelection(text.triLeft.length() + 1, text.full.length() - 1),
                        new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                ComposedTextPosition.AFTER_BEGINNING_OF_COMPOSITION,
                                ComposedTextPosition.BEFORE_END_OF_TEXT)));
                for (int i = 0; i < cases.size(); i++) {
                    GenericTestStateWithAction<SetSelection> testState = cases.get(i);
                    list.add(new ActionTestCase<>(testState.titleInfo,
                            settingsOption,
                            testState.initialState,
                            testState.action,
                            testState.expectedState));
                }
            }
            //TODO: (EW) why is this test case separated out?
            for (final Named<TestSettings> settingsOption : getSetSelectionSettingsOptions(false)) {
                String title = "move cursor to beginning of text from middle";
                list.add(new ActionTestCase<>(title,
                        settingsOption,
                        new CommittedState(text.biLeft, text.biRight),
                        new SetSelection(0, 0),
                        new CommittedState("", text.full), true));
            }

            return buildParameters(list);
        }
    }

    @RunWith(Parameterized.class)
    public static class CommitTextTest extends ActionTestBase<AddText> {
        public CommitTextTest(final ActionTestCase<AddText> testCase) {
            super(testCase);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<ActionTestCase<AddText>> list = new GroupedList<>();

            final AddTextOption[] addTextOptions = getAddTextOptions();

            // committed text
            for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(true)) {
                final boolean useBatch = !settingsOption.data.initialCursorPositionKnown;
                for (final AddTextOption addTextOption : addTextOptions) {
                    final Named<String> initialTextOption = addTextOption.initialText;
                    final SplitText initialText = new SplitText(initialTextOption.data);
                    final Named<String> addedTextOption = addTextOption.addingText;

                    list.add(new ActionTestCase<>(
                            "add " + addedTextOption.name + " to the end of " + initialTextOption.name,
                            settingsOption,
                            useBatch,
                            new CommittedState(initialText.full, ""),
                            new AddText(addedTextOption.data, false, 1),
                            new CommittedState(initialText.full + addedTextOption.data, "")));

                    if (initialText.codePointCount == 0) {
                        continue;
                    }

                    list.add(new ActionTestCase<>(
                            "add " + addedTextOption.name + " to the beginning of "
                                    + initialTextOption.name,
                            settingsOption,
                            useBatch,
                            new CommittedState("", initialText.full),
                            new AddText(addedTextOption.data, false, 1),
                            new CommittedState(addedTextOption.data, initialText.full)));

                    if (initialText.codePointCount < 2) {
                        continue;
                    }

                    final NewCursorPositionParams[] insertTextNewCursorPositions =
                            getInsertTextNewCursorPositions(initialText.biLeft,
                                    addedTextOption.data.length(), initialText.biRight);
                    for (final NewCursorPositionParams newCursorPositionOption : insertTextNewCursorPositions) {
                        if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
                                initialText.biLeft.length(), addedTextOption)) {
                            //TODO: is it possible to write a useful test for this? skipping for now
                            continue;
                        }
                        list.add(new ActionTestCase<>(
                                "add " + addedTextOption.name + " to the middle of "
                                        + initialTextOption.name + " and move the cursor to "
                                        + newCursorPositionOption.name,
                                settingsOption,
                                useBatch,
                                new CommittedState(initialText.biLeft, initialText.biRight),
                                new AddText(addedTextOption.data, false,
                                        newCursorPositionOption.newCursorPosition),
                                new CommittedState(initialText.biLeft + addedTextOption.data + initialText.biRight,
                                        newCursorPositionOption.newCursorIndexSanitized)));
                    }

                    if (initialText.codePointCount < 3) {
                        continue;
                    }

                    final NewCursorPositionParams[] replaceSelectionNewCursorPositions =
                            getReplaceSelectionNewCursorPositions(initialText.triLeft,
                                    addedTextOption.data.length(), initialText.triRight);
                    for (final NewCursorPositionParams newCursorPositionOption : replaceSelectionNewCursorPositions) {
                        if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
                                initialText.triLeft.length(),
                                initialText.triLeft.length() + initialText.triCenter.length(),
                                addedTextOption)) {
                            //TODO: is it possible to write a useful test for this? skipping for now
                            continue;
                        }
                        list.add(new ActionTestCase<>(
                                "replace the selected middle of " + initialTextOption.name
                                        + " with " + addedTextOption.name
                                        + " and move the cursor to " + newCursorPositionOption.name,
                                settingsOption,
                                useBatch,
                                new CommittedState(initialText.triLeft, initialText.triCenter, initialText.triRight),
                                new AddText(addedTextOption.data, false,
                                        newCursorPositionOption.newCursorPosition),
                                new CommittedState(initialText.triLeft + addedTextOption.data + initialText.triRight,
                                        newCursorPositionOption.newCursorIndexSanitized),
                                // GET_EXTRACTED_TEXT_MONITOR is loading the selection to correct
                                // the position before the update, so we don't also need to check if
                                // the position is within the text
                                settingsOption.data.initialCursorPositionKnown));
                    }
                }
            }

            // composed text
            for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(false)) {
                for (final AddTextOption addTextOption : addTextOptions) {
                    final Named<String> initialTextOption = addTextOption.initialText;
                    final SplitText initialText = new SplitText(initialTextOption.data);
                    final Named<String> addedTextOption = addTextOption.addingText;
                    if (initialText.codePointCount == 0) {
                        // skip since part of the initial text is used as the composing text
                        continue;
                    }

                    list.add(new ActionTestCase<>(
                            "finalize whole composed " + initialTextOption.name
                                    + " with " + addedTextOption.name,
                            settingsOption,
                            new ComposedState("", initialText.full, ""),
                            new AddText(addedTextOption.data, false, 1),
                            new CommittedState(addedTextOption.data)));

                    list.add(new ActionTestCase<>(
                            "finalize composed " + initialTextOption.name
                                    + " at the beginning of the text with " + addedTextOption.name,
                            settingsOption,
                            new ComposedState("", initialText.biLeft, initialText.biRight),
                            new AddText(addedTextOption.data, false, 1),
                            new CommittedState(addedTextOption.data, initialText.biRight)));

                    list.add(new ActionTestCase<>(
                            "finalize composed " + initialTextOption.name
                                    + " at the end of the text with " + addedTextOption.name,
                            settingsOption,
                            new ComposedState(initialText.biLeft, initialText.biRight, ""),
                            new AddText(addedTextOption.data, false, 1),
                            new CommittedState(initialText.biLeft + addedTextOption.data)));

                    if (initialText.codePointCount < 3) {
                        continue;
                    }

                    //TODO: reduce duplicate code from above
                    final NewCursorPositionParams[] updateCompositionNewCursorPositions =
                            getUpdateCompositionNewCursorPositions(initialText.triLeft,
                                    addedTextOption.data.length(), initialText.triRight);
                    final InitialCursorPositionParams[] initialCursorPositionOptions =
                            getInitialCursorPositions(initialText.triLeft,
                                    initialText.triCenter, initialText.triRight);
                    for (final NewCursorPositionParams newCursorPositionOption : updateCompositionNewCursorPositions) {
                        for (final InitialCursorPositionParams initialCursorPositionOption : initialCursorPositionOptions) {
                            if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
                                    initialCursorPositionOption.end, initialText.triLeft.length(),
                                    initialText.triLeft.length() + initialText.triCenter.length(), addedTextOption)) {
                                //TODO: is it possible to write a useful test for this? skipping for now
                                continue;
                            }
                            list.add(new ActionTestCase<>(
                                    "finalize composed " + initialTextOption.name
                                            + " in the middle of the text with " + addedTextOption.name
                                            + " with the cursor " + initialCursorPositionOption.name,
                                    settingsOption,
                                    new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                            initialCursorPositionOption),
                                    new AddText(addedTextOption.data, false,
                                            newCursorPositionOption.newCursorPosition),
                                    new CommittedState(initialText.triLeft + addedTextOption.data + initialText.triRight,
                                            newCursorPositionOption.newCursorIndexSanitized)));
                        }
                    }
                }
            }

            final String before = "Lorem ipsum dol";
            final String after = "or sit amet";
            final NewCursorPositionRelative[] newCursorOptions = new NewCursorPositionRelative[] {
                    NewCursorPositionRelative.before(5),
                    NewCursorPositionRelative.before(0),
                    NewCursorPositionRelative.after(0),
                    NewCursorPositionRelative.after(5),
            };
            for (final Named<TestSettingsBuilder> settingsOption : getAddTextSettingsBuilderOptions(true)) {
                for (final NewCursorPositionRelative newCursorPositionOption : newCursorOptions) {
                    list.add(getTextModifierCommitTestCase(
                            "input connection adds extra characters with the new cursor position "
                                    + newCursorPositionOption + " the composed text",
                            settingsOption, new DoubleTextModifier(),
                            before, after, "asdf", "aassddff",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown
                                    && !newCursorPositionOption.cursorAfterText));
                    list.add(getTextModifierCommitTestCase(
                            "input connection adds fewer characters with the new cursor position "
                                    + newCursorPositionOption + " the composed text",
                            settingsOption, new HalveTextModifier(),
                            before, after, "asdf", "sf",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown
                                    && !newCursorPositionOption.cursorAfterText));
                    list.add(getTextModifierCommitTestCase(
                            "input connection changes characters with the new cursor position "
                                    + newCursorPositionOption + " the composed text",
                            settingsOption, new FlipTextModifier(),
                            before, after, "asdf", "fdsa",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown));
                }
            }

            return buildParameters(list);
        }
    }

    //TODO: (EW) do we have a test case for trying to compose text when the composition state is
    // unknown? it should throw away any text in the cache since it doesn't know where the text is
    // even getting entered
    @RunWith(Parameterized.class)
    public static class ComposeTextTest extends ActionTestBase<AddText> {
        public ComposeTextTest(final ActionTestCase<AddText> testCase) {
            super(testCase);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<ActionTestCase<AddText>> list = new GroupedList<>();

            final AddTextOption[] addTextOptions = getAddTextOptions();

            // committed text
            for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(true)) {
                final boolean useBatch = !settingsOption.data.initialCursorPositionKnown;
                for (final AddTextOption addTextOption : addTextOptions) {
                    final Named<String> initialTextOption = addTextOption.initialText;
                    final SplitText initialText = new SplitText(initialTextOption.data);
                    final Named<String> addedTextOption = addTextOption.addingText;
                    //TODO: try to get rid of this indent
                    if (addedTextOption.data.length() != 0) {
                        list.add(new ActionTestCase<>(
                                "add " + addedTextOption.name + " to the end of " + initialTextOption.name,
                                settingsOption,
                                useBatch,
                                new CommittedState(initialText.full, ""),
                                new AddText(addedTextOption.data, true, 1),
                                new ComposedState(initialText.full, addedTextOption.data, "")));

                        if (initialText.codePointCount == 0) {
                            continue;
                        }

                        list.add(new ActionTestCase<>(
                                "add " + addedTextOption.name + " to the beginning of " + initialTextOption.name,
                                settingsOption,
                                useBatch,
                                new CommittedState("", initialText.full),
                                new AddText(addedTextOption.data, true, 1),
                                new ComposedState("", addedTextOption.data, initialText.full)));

                        if (initialText.codePointCount < 2) {
                            continue;
                        }

                        final NewCursorPositionParams[] insertTextNewCursorPositions =
                                getInsertTextNewCursorPositions(initialText.biLeft, addedTextOption.data.length(), initialText.biRight);
                        for (final NewCursorPositionParams newCursorPositionOption : insertTextNewCursorPositions) {
                            if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, initialText.biLeft.length(), addedTextOption)) {
                                //TODO: is it possible to write a useful test for this? skipping for now
                                continue;
                            }
                            list.add(new ActionTestCase<>(
                                    "add " + addedTextOption.name + " to the middle of "
                                            + initialTextOption.name + " and move the cursor to "
                                            + newCursorPositionOption.name,
                                    settingsOption,
                                    useBatch,
                                    new CommittedState(initialText.biLeft, initialText.biRight),
                                    new AddText(addedTextOption.data, true,
                                            newCursorPositionOption.newCursorPosition),
                                    new ComposedState(initialText.biLeft, addedTextOption.data, initialText.biRight,
                                            newCursorPositionOption.newCursorIndexSanitized)));
                        }

                        if (initialText.codePointCount < 3) {
                            continue;
                        }

                        final NewCursorPositionParams[] replaceSelectionNewCursorPositions =
                                getReplaceSelectionNewCursorPositions(initialText.triLeft, addedTextOption.data.length(), initialText.triRight);
                        for (final NewCursorPositionParams newCursorPositionOption : replaceSelectionNewCursorPositions) {
                            if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, initialText.triLeft.length(),
                                    initialText.triLeft.length() + initialText.triCenter.length(), addedTextOption)) {
                                //TODO: is it possible to write a useful test for this? skipping for now
                                continue;
                            }
                            list.add(new ActionTestCase<>(
                                    "replace the selected middle of " + initialTextOption.name
                                            + " with " + addedTextOption.name
                                            + " and move the cursor to " + newCursorPositionOption.name,
                                    settingsOption,
                                    useBatch,
                                    new CommittedState(initialText.triLeft, initialText.triCenter, initialText.triRight),
                                    new AddText(addedTextOption.data, true,
                                            newCursorPositionOption.newCursorPosition),
                                    new ComposedState(initialText.triLeft, addedTextOption.data, initialText.triRight,
                                            newCursorPositionOption.newCursorIndexSanitized),
                                    settingsOption.data.initialCursorPositionKnown
                                            && (newCursorPositionOption.newCursorIndexRaw <= (initialText.triLeft + addedTextOption.data + initialText.triRight).length())));
                        }
                    } else {
                        // compose nothing tests

                        list.add(new ActionTestCase<>(
                                "add " + addedTextOption.name + " to the end of " + initialTextOption.name,
                                settingsOption,
                                useBatch,
                                new CommittedState(initialText.full, ""),
                                new AddText(addedTextOption.data, true, 1),
                                new CommittedState(initialText.full, "")));

                        if (initialText.codePointCount < 2) {
                            continue;
                        }

                        final NewCursorPositionParams newCursorPositionOption1 =
                                getNewCursorPosition(initialText.biLeft,
                                        addedTextOption.data.length(), initialText.biRight,
                                        AddingTextPosition.BEGINNING_OF_NEW_TEXT, -1);
                        if (!isNewCursorInKnownSpace(newCursorPositionOption1, settingsOption,
                                initialText.biLeft.length(), addedTextOption)) {
                            //TODO: is it possible to write a useful test for this? skipping for now
                        } else {
                            list.add(new ActionTestCase<>(
                                    "add " + addedTextOption.name + " to the middle of "
                                            + initialTextOption.name + " and move the cursor to "
                                            + newCursorPositionOption1.name,
                                    settingsOption,
                                    useBatch,
                                    new CommittedState(initialText.biLeft, initialText.biRight),
                                    new AddText(addedTextOption.data, true,
                                            newCursorPositionOption1.newCursorPosition),
                                    new CommittedState(initialText.biLeft + initialText.biRight,
                                            newCursorPositionOption1.newCursorIndexSanitized)));
                        }

                        if (initialText.codePointCount < 3) {
                            continue;
                        }

                        final NewCursorPositionParams newCursorPositionOption2 =
                                getNewCursorPosition(initialText.triLeft,
                                        addedTextOption.data.length(), initialText.triRight,
                                        AddingTextPosition.END_OF_NEW_TEXT, 1);
                        if (!isNewCursorInKnownSpace(newCursorPositionOption2, settingsOption,
                                initialText.triLeft.length(),
                                initialText.triLeft.length() + initialText.triCenter.length(),
                                addedTextOption)) {
                            //TODO: is it possible to write a useful test for this? skipping for now
                        } else {
                            list.add(new ActionTestCase<>(
                                    "replace the selected middle of " + initialTextOption.name
                                            + " with " + addedTextOption.name
                                            + " and move the cursor to " + newCursorPositionOption2.name,
                                    settingsOption,
                                    useBatch,
                                    new CommittedState(initialText.triLeft, initialText.triCenter, initialText.triRight),
                                    new AddText(addedTextOption.data, true,
                                            newCursorPositionOption2.newCursorPosition),
                                    new CommittedState(initialText.triLeft + initialText.triRight,
                                            newCursorPositionOption2.newCursorIndexSanitized)));
                        }
                    }
                }
            }

            // composed text
            for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(false)) {
                for (final AddTextOption addTextOption : addTextOptions) {
                    final Named<String> initialTextOption = addTextOption.initialText;
                    final SplitText initialText = new SplitText(initialTextOption.data);
                    final Named<String> addedTextOption = addTextOption.addingText;
                    if (initialText.codePointCount == 0) {
                        // skip since part of the initial text is used as the composing text
                        continue;
                    }
                    //TODO: try to get rid of this indent
                    if (addedTextOption.data.length() != 0) {
                        list.add(new ActionTestCase<>(
                                "replace whole composed " + initialTextOption.name
                                        + " with " + addedTextOption.name,
                                settingsOption,
                                new ComposedState("", initialTextOption.data, ""),
                                new AddText(addedTextOption.data, true, 1),
                                new ComposedState("", addedTextOption.data, "")));

                        list.add(new ActionTestCase<>(
                                "replace composed " + initialTextOption.name
                                        + " at the beginning of the text with " + addedTextOption.name,
                                settingsOption,
                                new ComposedState("", initialText.biLeft, initialText.biRight),
                                new AddText(addedTextOption.data, true, 1),
                                new ComposedState("", addedTextOption.data, initialText.biRight)));

                        list.add(new ActionTestCase<>(
                                "replace composed " + initialTextOption.name
                                        + " at the end of the text with " + addedTextOption.name,
                                settingsOption,
                                new ComposedState(initialText.biLeft, initialText.biRight, ""),
                                new AddText(addedTextOption.data, true, 1),
                                new ComposedState(initialText.biLeft, addedTextOption.data, "")));

                        if (initialText.codePointCount < 3) {
                            continue;
                        }

                        //TODO: reduce duplicate code from above
                        final NewCursorPositionParams[] updateCompositionNewCursorPositions =
                                getUpdateCompositionNewCursorPositions(initialText.triLeft, addedTextOption.data.length(), initialText.triRight);
                        final InitialCursorPositionParams[] initialCursorPositionOptions =
                                getInitialCursorPositions(initialText.triLeft, initialText.triCenter, initialText.triRight);
                        for (final NewCursorPositionParams newCursorPositionOption : updateCompositionNewCursorPositions) {
                            for (final InitialCursorPositionParams initialCursorPositionOption : initialCursorPositionOptions) {
                                if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
                                        initialCursorPositionOption.end, initialText.triLeft.length(),
                                        initialText.triLeft.length() + initialText.triCenter.length(), addedTextOption)) {
                                    //TODO: is it possible to write a useful test for this? skipping for now
                                    continue;
                                }
                                list.add(new ActionTestCase<>(
                                        "replace composed " + initialTextOption.name
                                                + " in the middle of the text with " + addedTextOption.name
                                                + " with the cursor " + initialCursorPositionOption.name,
                                        settingsOption,
                                        new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                                initialCursorPositionOption),
                                        new AddText(addedTextOption.data, true,
                                                newCursorPositionOption.newCursorPosition),
                                        new ComposedState(initialText.triLeft, addedTextOption.data, initialText.triRight,
                                                newCursorPositionOption.newCursorIndexSanitized)));
                            }
                        }
                    } else {
                        // compose nothing tests

                        list.add(new ActionTestCase<>(
                                "replace whole composed " + initialTextOption.name
                                        + " with " + addedTextOption.name,
                                settingsOption,
                                new ComposedState("", initialText.full, ""),
                                new AddText(addedTextOption.data, true, 1),
                                new CommittedState("")));

                        if (initialText.codePointCount < 3) {
                            continue;
                        }

                        //TODO: reduce duplicate code from above
                        final NewCursorPositionParams newCursorPositionOption =
                                getNewCursorPosition(initialText.triLeft, addedTextOption.data.length(), initialText.triRight,
                                AddingTextPosition.END_OF_NEW_TEXT, 1);
                        final InitialCursorPositionParams initialCursorPositionOption =
                                new InitialCursorPositionParams("at the end of the composition",
                                        initialText.triLeft.length() + initialText.triCenter.length());
                        if (!isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
                                initialCursorPositionOption.end, initialText.triLeft.length(),
                                initialText.triLeft.length() + initialText.triCenter.length(), addedTextOption)) {
                            //TODO: is it possible to write a useful test for this? skipping for now
                            continue;
                        }
                        list.add(new ActionTestCase<>(
                                "replace composed " + initialTextOption.name
                                        + " in the middle of the text with " + addedTextOption.name
                                        + " with the cursor " + initialCursorPositionOption.name,
                                settingsOption,
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialCursorPositionOption),
                                new AddText(addedTextOption.data, true,
                                        newCursorPositionOption.newCursorPosition),
                                new CommittedState(initialText.triLeft + initialText.triRight,
                                        newCursorPositionOption.newCursorIndexSanitized)));
                    }
                }
            }

            final String before = "Lorem ipsum dol";
            final String after = "or sit amet";
            final NewCursorPositionRelative[] newCursorOptions = new NewCursorPositionRelative[] {
                    NewCursorPositionRelative.before(5),
                    NewCursorPositionRelative.before(0),
                    NewCursorPositionRelative.after(0),
                    NewCursorPositionRelative.after(5),
            };
            for (final Named<TestSettingsBuilder> settingsOption : getAddTextSettingsBuilderOptions(true)) {
                for (final NewCursorPositionRelative newCursorPositionOption : newCursorOptions) {
                    list.add(getTextModifierComposeTestCase(
                            "input connection adds extra characters with the new cursor position "
                                    + newCursorPositionOption + " the composed text",
                            settingsOption, new DoubleTextModifier(),
                            before, after, "asdf", "aassddff",
                            newCursorPositionOption,
                            false));
                    list.add(getTextModifierComposeTestCase(
                            "input connection adds fewer characters with the new cursor position "
                                    + newCursorPositionOption + " the composed text",
                            settingsOption, new HalveTextModifier(),
                            before, after, "asdf", "sf",
                            newCursorPositionOption,
                            false));
                    list.add(getTextModifierComposeTestCase(
                            "input connection changes characters with the new cursor position "
                                    + newCursorPositionOption + " the composed text",
                            settingsOption, new FlipTextModifier(),
                            before, after, "asdf", "fdsa",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown));//if the cursor is initially unknown, the first onUpdateSelection call will be treated as unexpected

                    list.add(getTextModifierComposeTestCase(
                            "input connection changes some characters in existing composition with the new cursor position "
                                    + newCursorPositionOption + " the composed text (longer)",
                            settingsOption, new IncrementNumberTextModifier(),
                            before, "asdf", after, "as1df", "as2df",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown));
                    list.add(getTextModifierComposeTestCase(
                            "input connection adds extra characters in existing composition with the new cursor position "
                                    + newCursorPositionOption + " the composed text",
                            settingsOption, new DoubleCapsTextModifier(),
                            before, "asdf", after, "asAdf", "asAAdf",
                            newCursorPositionOption,
                            false));

                    //TODO: are these different enough to be valuable or should they just get deleted?
                    list.add(getTextModifierComposeTestCase(
                            "input connection changes some characters in existing composition with the new cursor position "
                                    + newCursorPositionOption + " the composed text (shorter)",
                            settingsOption, new IncrementNumberTextModifier(),
                            before, "asXXdf", after, "as1df", "as2df",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown));
                    list.add(getTextModifierComposeTestCase(
                            "input connection changes some characters in existing composition with the new cursor position "
                                    + newCursorPositionOption + " the composed text (same)",
                            settingsOption, new IncrementNumberTextModifier(),
                            before, "asXdf", after, "as1df", "as2df",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown
                                    || (newCursorPositionOption.distanceFromText == 0 && newCursorPositionOption.cursorAfterText)));

                    //TODO: 60 tests fail when DEBUG_PREVIOUS_TEXT is true (that assumes we won't be
                    // incorrect too many times in a row, but with the forced delays, we're going to
                    // be wrong until everything processes). maybe that debug check needs to be
                    // fixed (not sure if it can be). see if there is any better handling for
                    // something, but this may just have to be like this.
                    list.add(getTextModifierComposeTestCase(
                            "input connection changes some characters in existing composition with the new cursor position "
                                    + newCursorPositionOption + " the composed text (longer, shift to same)",
                            settingsOption, new IncrementNumberTextModifier(),
                            before, "asdf", after, before.length() + 5, "as1df", "as2df",
                            newCursorPositionOption,
                            settingsOption.data.initialCursorPositionKnown));


                    //TODO: the lextra length didn't seem useful - probably just delete
//                    list.add(getTextModifierComposeTestCase(
//                            "input connection changes some characters in existing composition with the new cursor position " + newCursorPositionOption.name + " the composed text (shorter, long text)",
//                            settingsOption, new FakeInputConnection.IncrementNumberTextModifier(),
//                            before, "longtextasXXdf", after, "longtextas1df", "longtextas2df",
//                            newCursorPositionOption.data,
//                            settingsOption.data.initialCursorPositionKnown));
//                    list.add(getTextModifierComposeTestCase(
//                            "input connection changes some characters in existing composition with the new cursor position " + newCursorPositionOption.name + " the composed text (same)",
//                            settingsOption, new FakeInputConnection.IncrementNumberTextModifier(),
//                            before, "longtextasXdf", after, "longtextas1df", "longtextas2df",
//                            newCursorPositionOption.data,
//                            settingsOption.data.initialCursorPositionKnown
//                                    || (newCursorPositionOption.data.distanceFromText == 0 && newCursorPositionOption.data.cursorAfterText)));
                }
            }

            return buildParameters(list);
        }
    }

    @RunWith(Parameterized.class)
    public static class FinishComposingTest extends ActionTestBase<FinishComposing> {
        public FinishComposingTest(final ActionTestCase<FinishComposing> testCase) {
            super(testCase);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<ActionTestCase<FinishComposing>> list = new GroupedList<>();

            final AddTextOption[] textOptions = getFinishComposingTextOptions();

            for (final Named<TestSettings> settingsOption : getDeleteSelectedSettingsOptions()) {
                //TODO: batch probably doesn't matter, and cursor should always be known unless
                // there is no composition or the composition was started as part of the ongoing
                // batch, which would be a strange case
                final boolean useBatch = !settingsOption.data.initialCursorPositionKnown;
                for (final AddTextOption textOption : textOptions) {
                    final TwoPartText twoPartText = new TwoPartText(textOption.initialText.data);
                    final String leftOfComposed = twoPartText.left;
                    final String composed = textOption.addingText.data;
                    final String rightOfComposed = twoPartText.right;
                    for (final InitialCursorPositionParams initialCursorPosition : getInitialCursorPositions(leftOfComposed, composed, rightOfComposed)) {
                        list.add(new ActionTestCase<>("finish composing " + textOption.addingText.name
                                + " in " + textOption.initialText.name + " with the cursor " + initialCursorPosition.name,
                                settingsOption,
                                new ComposedState(leftOfComposed, composed, rightOfComposed, initialCursorPosition),
                                new FinishComposing(),
                                new CommittedState(leftOfComposed + composed + rightOfComposed, initialCursorPosition)));
                    }
                }
            }
            //TODO: add extra tests for no composition, compose and finish in single batch

            return buildParameters(list);
        }
    }

    @RunWith(Parameterized.class)
    public static class DeleteTextBeforeCursorTest extends ActionTestBase<DeleteTextBefore> {
        public DeleteTextBeforeCursorTest(final ActionTestCase<DeleteTextBefore> testCase) {
            super(testCase);
        }

        //TODO: (EW) use TwoPartText and ThreePartText instead of a bunch of getSubstring calls
        // to match and consolidate with getDeleteBeforeSelectionTestCases
        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            //TODO: try to reduce the number of test cases
            final GroupedList<ActionTestCase<DeleteTextBefore>> list = new GroupedList<>();

            for (GenericTestStateWithSettingsAndDeletingText testCase : getDeleteBeforeSelectionTestCases(true, true)) {
                if (testCase.expectedState instanceof ComposedState
                        && !((ComposedState)testCase.expectedState).composedText.equals(((ComposedState)testCase.initialState).composedText)
                        && testCase.settingsOption.data.inputConnectionSettings.getTextLimit < Integer.MAX_VALUE
                        && !testCase.settingsOption.data.inputConnectionSettings.setComposingRegionSupported) {

                }

                list.add(new ActionTestCase<>("delete " + testCase.titleInfo,
                        testCase.settingsOption,
                        testCase.initialState,
                        new DeleteTextBefore(testCase.textToDelete.length()),
                        testCase.expectedState));
                //TODO: (EW) this doesn't need to be restricted to composed text, but that's what we
                // tested before, and we probably don't need as many tests for deleting past the
                // start if we included all of the composed cases. try to test a variety of things
                // without creating too many cases.
                if (testCase.initialState.getTextBeforeCursor().equals(testCase.textToDelete)
                        && testCase.initialState instanceof ComposedState) {
                    list.add(new ActionTestCase<>(
                            "delete " + testCase.titleInfo + " and request deleting past the start",
                            testCase.settingsOption,
                            testCase.initialState,
                            new DeleteTextBefore(testCase.textToDelete.length() + 1),
                            testCase.expectedState));
                }
            }

            //TODO: add test for deleting negative?

            return buildParameters(list);
        }
    }

    @RunWith(Parameterized.class)
    public static class DeleteSelectedTextTest extends ActionTestBase<DeleteSelected> {
        public DeleteSelectedTextTest(final ActionTestCase<DeleteSelected> testCase) {
            super(testCase);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<ActionTestCase<DeleteSelected>> list = new GroupedList<>();

            for (GenericTestStateWithSettings testCase : getDeleteSelectionTestCases(true)) {
                list.add(new ActionTestCase<>("delete " + testCase.titleInfo,
                        testCase.settingsOption,
                        testCase.initialState,
                        new DeleteSelected(),
                        testCase.expectedState));
            }

            return buildParameters(list);
        }
    }

    @RunWith(Parameterized.class)
    public static class SendKeyEventDeleteTest extends ActionTestBase<SendDeleteKey> {
        public SendKeyEventDeleteTest(final ActionTestCase<SendDeleteKey> testCase) {
            super(testCase);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<ActionTestCase<SendDeleteKey>> list = new GroupedList<>();

            for (GenericTestStateWithSettings testCase : getDeleteSelectionTestCases(false)) {
                list.add(new ActionTestCase<>("delete selected " + testCase.titleInfo,
                        testCase.settingsOption,
                        testCase.initialState,
                        new SendDeleteKey(),
                        testCase.expectedState));
            }

            for (GenericTestStateWithSettingsAndDeletingText testCase : getDeleteBeforeSelectionTestCases(false, false)) {
                if (codePointCount(testCase.textToDelete) != 1) {
                    continue;
                }
                list.add(new ActionTestCase<>("delete before the cursor " + testCase.titleInfo,
                        testCase.settingsOption,
                        testCase.initialState,
                        new SendDeleteKey(),
                        testCase.expectedState));
            }

            return buildParameters(list);
        }
    }

    //TODO: see if there is anything duplicative from DeleteTextBeforeCursorTest that should be reduced
    private static List<GenericTestStateWithSettings> getDeleteSelectionTestCases(
            final boolean includeZeroLengthSelection) {
        final List<GenericTestStateWithSettings> list = new ArrayList<>();

        final List<Named<ThreePartText>> textOptions = getDeletingTextOptions();

        //TODO: (EW) these tests aren't grouped well (a lot or it is coming from the text options

        //committed
        for (final Named<TestSettings> settingsOption : getDeleteSelectedSettingsOptions()) {
            for (final Named<ThreePartText> textOption : textOptions) {
                final String leftText = textOption.data.left;
                final String textToDelete = textOption.data.center;
                final String rightText = textOption.data.right;

                if (!includeZeroLengthSelection && isEmpty(textToDelete)) {
                    continue;
                }

                list.add(new GenericTestStateWithSettings(textOption.name,
                        settingsOption,
                        new CommittedState(leftText, textToDelete, rightText),
                        new CommittedState(leftText, rightText)));
            }
        }
        //composed
        for (final Named<TestSettings> settingsOption : getDeleteSelectedSettingsOptions()) {
            for (final Named<ThreePartText> textOption : textOptions) {
                final String leftText = textOption.data.left;
                final String textToDelete = textOption.data.center;
                final String rightText = textOption.data.right;
                final int initialSelectionStart = leftText.length();
                final int initialSelectionEnd = leftText.length() + textToDelete.length();

                if (!includeZeroLengthSelection && isEmpty(textToDelete)) {
                    continue;
                }

                if (!includeZeroLengthSelection && codePointCount(textToDelete) == 1) {
                    //TODO: (EW) these were skipped in SendKeyEventDeleteTest possibly mainly as a
                    // simple way to split cases for single deleting before text and selection.
                    // these may be valid to add, but they also may just be unnecessary cases.
                    continue;
                }

                if (!isEmpty(rightText)) {
                    final ThreePartText splitRight = new ThreePartText(rightText);
                    list.add(new GenericTestStateWithSettings(
                            textOption.name + " before the composition",
                            settingsOption,
                            new ComposedState(leftText + textToDelete + splitRight.left,
                                    splitRight.center,
                                    splitRight.right,
                                    initialSelectionStart, initialSelectionEnd),
                            new ComposedState(leftText + splitRight.left,
                                    splitRight.center,
                                    splitRight.right,
                                    initialSelectionStart)));
                }
                if (!isEmpty(leftText) && !isEmpty(rightText)) {
                    final TwoPartText splitLeft = new TwoPartText(leftText);
                    final TwoPartText splitRight = new TwoPartText(rightText);
                    list.add(new GenericTestStateWithSettings(
                            textOption.name + " in the composition",
                            settingsOption,
                            new ComposedState(splitLeft.left,
                                    splitLeft.right + textToDelete + splitRight.left,
                                    splitRight.right,
                                    initialSelectionStart, initialSelectionEnd),
                            new ComposedState(splitLeft.left,
                                    splitLeft.right + splitRight.left,
                                    splitRight.right,
                                    initialSelectionStart)));
                }

                if (!isEmpty(leftText)) {
                    final ThreePartText splitLeft = new ThreePartText(leftText);
                    list.add(new GenericTestStateWithSettings(
                            textOption.name + " after the composition",
                            settingsOption,
                            new ComposedState(splitLeft.left,
                                    splitLeft.center,
                                    splitLeft.right + textToDelete + rightText,
                                    initialSelectionStart, initialSelectionEnd),
                            new ComposedState(splitLeft.left,
                                    splitLeft.center,
                                    splitLeft.right + rightText,
                                    initialSelectionStart)));
                }
                //TODO: (EW) only left or right needs to be populated. these were skipped before in
                // a more straightforward way, but now here it seems weird.
                if (codePointCount(textToDelete) > 1 && !isEmpty(leftText) && !isEmpty(rightText)) {
                    final TwoPartText toDelete = new TwoPartText(textToDelete);

                    final TwoPartText splitRight = new TwoPartText(rightText);
                    list.add(new GenericTestStateWithSettings(
                            textOption.name + " through the beginning of the composition",
                            settingsOption,
                            new ComposedState(leftText + toDelete.left,
                                    toDelete.right + splitRight.left,
                                    splitRight.right,
                                    initialSelectionStart, initialSelectionEnd),
                            new ComposedState(leftText, splitRight.left, splitRight.right,
                                    initialSelectionStart)));

                    final TwoPartText splitLeft = new TwoPartText(leftText);
                    list.add(new GenericTestStateWithSettings(
                            textOption.name + " through the end of the composition",
                            settingsOption,
                            new ComposedState(splitLeft.left,
                                    splitLeft.right + toDelete.left,
                                    toDelete.right + rightText,
                                    initialSelectionStart, initialSelectionEnd),
                            new ComposedState(splitLeft.left, splitLeft.right, rightText,
                                    initialSelectionStart)));
                }
                if (!isEmpty(leftText) && !isEmpty(rightText)) {
                    if (codePointCount(textToDelete) > 2) {
                        final ThreePartText toDelete = new ThreePartText(textToDelete);
                        list.add(new GenericTestStateWithSettings(
                                textOption.name + " through the whole composition",
                                settingsOption,
                                new ComposedState(leftText + toDelete.left,
                                        toDelete.center,
                                        toDelete.right + rightText,
                                        initialSelectionStart, initialSelectionEnd),
                                new CommittedState(leftText, rightText)));
                    }
                }
            }
        }

        return list;
    }

    private static List<GenericTestStateWithSettingsAndDeletingText> getDeleteBeforeSelectionTestCases(
            final boolean allowSelection, final boolean canDropComposition) {
        final List<GenericTestStateWithSettingsAndDeletingText> list = new ArrayList<>();

        final List<Named<ThreePartText>> textOptions = getDeletingTextOptions();

        //committed
        for (final Named<TestSettings> settingsOption : getDeleteBeforeSettingsOptions(false, false)) {
            for (final Named<ThreePartText> textOption : textOptions) {
                final String leftText = textOption.data.left;
                final String textToDelete = textOption.data.center;
                final String rightText = textOption.data.right;

                list.add(new GenericTestStateWithSettingsAndDeletingText(textOption.name,
                        settingsOption, textToDelete,
                        new CommittedState(leftText + textToDelete, rightText),
                        new CommittedState(leftText, rightText)));

                final int rightCodePointCount = codePointCount(rightText);

                if (allowSelection && rightCodePointCount > 0) {
                    list.add(new GenericTestStateWithSettingsAndDeletingText(
                            textOption.name + " and select to the end",
                            settingsOption, textToDelete,
                            new CommittedState(leftText + textToDelete, rightText, ""),
                            new CommittedState(leftText, rightText, "")));
                    //TODO: (EW) both cases may be valuable, but we don't need both for every case
                    if (rightCodePointCount > 1) {
                        list.add(new GenericTestStateWithSettingsAndDeletingText(
                                textOption.name + " and select one code point",
                                settingsOption, textToDelete,
                                new CommittedState(leftText + textToDelete,
                                        getSubstring(rightText, 0, 1),
                                        getSubstring(rightText, 1)),
                                new CommittedState(leftText,
                                        getSubstring(rightText, 0, 1),
                                        getSubstring(rightText, 1))));
                    }
                }
            }
        }
        //composed
        for (final Named<TestSettings> settingsOption : getDeleteBeforeSettingsOptions(canDropComposition, true)) {
            for (final Named<ThreePartText> textOption : textOptions) {
                final String leftText = textOption.data.left;
                final String textToDelete = textOption.data.center;
                final String rightText = textOption.data.right;
                final int initialSelectionStart = leftText.length() + textToDelete.length();
                final int expectedSelectionStart = leftText.length();

                //TODO: (EW) add tests with spans

                //TODO: should tests be added here for a selection?

                //TODO: (EW) I think all of these cases are valuable, but we don't need all
                // combinations. see if there is a good way to keep all sub-cases and just combine
                // them with a couple other sub-cases, rather than all. maybe a flag (for all tests)
                // could be included to run all combinations for occasionally testing very
                // thoroughly, but normally having a smaller number of tests.

                if (!isEmpty(rightText)) {
                    final ThreePartText splitRight = new ThreePartText(rightText);
                    list.add(new GenericTestStateWithSettingsAndDeletingText(
                            textOption.name + " before the composition",
                            settingsOption, textToDelete,
                            new ComposedState(leftText + textToDelete + splitRight.left,
                                    splitRight.center,
                                    splitRight.right,
                                    initialSelectionStart),
                            new ComposedState(leftText + splitRight.left,
                                    splitRight.center,
                                    splitRight.right,
                                    expectedSelectionStart)));
                }

                //TODO: (EW) only left or right needs to be populated for most cases. these were
                // skipped before in a more straightforward way, but now here it seems weird.
                if (!isEmpty(leftText) && !isEmpty(rightText)) {
                    final TwoPartText splitLeft = new TwoPartText(leftText);
                    final TwoPartText splitRight = new TwoPartText(rightText);
                    final TwoPartText splitLeftNearStart = new TwoPartText(leftText, 1);
                    final TwoPartText splitLeftNearEnd = new TwoPartText(leftText, -1);
                    final TwoPartText splitRightNearStart = new TwoPartText(rightText, 1);
                    final TwoPartText splitRightNearEnd = new TwoPartText(rightText, -1);

                    list.add(new GenericTestStateWithSettingsAndDeletingText(
                            textOption.name + " right before the composition",
                            settingsOption, textToDelete,
                            new ComposedState(leftText + textToDelete,
                                    splitRight.left,
                                    splitRight.right,
                                    initialSelectionStart),
                            new ComposedState(leftText,
                                    splitRight.left,
                                    splitRight.right,
                                    expectedSelectionStart)));

                    final int getTextLimit = settingsOption.data.inputConnectionSettings.getTextLimit;
                    // check if we should shift the length of the composition to add test cases to
                    // both be over and under the get text limit
                    final boolean shiftCompositionForTextLimit = canDropComposition
                            && !settingsOption.data.inputConnectionSettings.setComposingRegionSupported
                            && getTextLimit < Integer.MAX_VALUE && textToDelete.length() > 0;

                    //TODO: (EW) consider making these split cases less deterministic (ie if we find
                    // a way to avoid dropping the composition in some cases, it might be nice to
                    // have that not break tests. since dropping the composition isn't the desired
                    // outcome, but only the last resort when we haven't found a way around it, it
                    // seems weird to specifically expect that in the tests)

                    if (shiftCompositionForTextLimit) {
                        if (splitRightNearEnd.left.length() > getTextLimit) {
                            // deleting text may cause us to remove text after the selection
                            // from the cache, and if we don't have the fully cached
                            // composition, we may have to stop composing
                            list.add(getDeleteBeginningOfCompositionCase(
                                    textOption.name + " at the beginning of long composition",
                                    settingsOption, leftText,
                                    textToDelete, splitRightNearEnd, true));
                        }
                        if (splitRightNearStart.left.length() <= getTextLimit) {
                            list.add(getDeleteBeginningOfCompositionCase(
                                    textOption.name + " at the beginning of the short composition",
                                    settingsOption, leftText,
                                    textToDelete, splitRightNearStart, false));
                        }
                    } else {
                        list.add(getDeleteBeginningOfCompositionCase(
                                textOption.name + " at the beginning of the composition",
                                settingsOption, leftText, textToDelete, splitRight, false));
                    }

                    if (shiftCompositionForTextLimit) {
                        if (splitRightNearEnd.left.length() > getTextLimit) {
                            // deleting text may cause us to remove text after the selection
                            // from the cache, and if we don't have the fully cached
                            // composition, we may have to stop composing
                            list.add(getDeleteInCompositionCase(
                                    textOption.name + " in the long composition",
                                    settingsOption, splitLeftNearStart,
                                    textToDelete, splitRightNearEnd, true));
                        }
                        if (splitRightNearStart.left.length() <= getTextLimit) {
                            list.add(getDeleteInCompositionCase(
                                    textOption.name + " in the short composition",
                                    settingsOption, splitLeftNearEnd,
                                    textToDelete, splitRightNearStart, false));
                        }
                    } else {
                        list.add(getDeleteInCompositionCase(
                                textOption.name + " in the composition",
                                settingsOption, splitLeft, textToDelete, splitRight, false));
                    }

                    list.add(new GenericTestStateWithSettingsAndDeletingText(
                            textOption.name + " at the end of the composition",
                            settingsOption, textToDelete,
                            new ComposedState(splitLeft.left,
                                    splitLeft.right + textToDelete,
                                    rightText,
                                    initialSelectionStart),
                            new ComposedState(splitLeft.left,
                                    splitLeft.right,
                                    rightText,
                                    expectedSelectionStart)));

                    if (codePointCount(textToDelete) > 1) {

                        if (shiftCompositionForTextLimit) {
                            if (splitRightNearEnd.left.length() > getTextLimit) {
                                // deleting text may cause us to remove text after the selection
                                // from the cache, and if we don't have the fully cached
                                // composition, we may have to stop composing
                                list.add(getDeleteThroughCompositionStartCase(
                                        textOption.name + " through the beginning of the long composition",
                                        settingsOption, leftText, textToDelete, splitRightNearEnd, true));
                            }
                            if (splitRightNearStart.left.length() <= getTextLimit) {
                                list.add(getDeleteThroughCompositionStartCase(
                                        textOption.name + " through the beginning of the short composition",
                                        settingsOption, leftText, textToDelete, splitRightNearStart, false));
                            }
                        } else {
                            list.add(getDeleteThroughCompositionStartCase(
                                    textOption.name + " through the beginning of the composition",
                                    settingsOption, leftText, textToDelete, splitRight, false));
                        }

                        list.add(getDeleteThroughCompositionEndCase(
                                textOption.name + " through the end of the composition",
                                settingsOption, splitLeft, textToDelete, rightText, false));
                    }

                    if (codePointCount(textToDelete) > 2) {
                        final ThreePartText toDelete = new ThreePartText(textToDelete);
                        list.add(new GenericTestStateWithSettingsAndDeletingText(
                                textOption.name + " through the whole composition",
                                settingsOption, textToDelete,
                                new ComposedState(leftText + toDelete.left,
                                        toDelete.center,
                                        toDelete.right + rightText,
                                        initialSelectionStart),
                                new CommittedState(leftText, rightText)));
                    }

                    list.add(new GenericTestStateWithSettingsAndDeletingText(
                            textOption.name + " right after the composition",
                            settingsOption, textToDelete,
                            new ComposedState(splitLeft.left,
                                    splitLeft.right,
                                    textToDelete + rightText,
                                    initialSelectionStart),
                            new ComposedState(splitLeft.left,
                                    splitLeft.right,
                                    rightText,
                                    expectedSelectionStart)));
                }

                if (!isEmpty(leftText)) {
                    final ThreePartText splitLeft = new ThreePartText(leftText);
                    list.add(new GenericTestStateWithSettingsAndDeletingText(
                            textOption.name + " after the composition",
                            settingsOption, textToDelete,
                            new ComposedState(splitLeft.left,
                                    splitLeft.center,
                                    splitLeft.right + textToDelete + rightText,
                                    initialSelectionStart),
                            new ComposedState(splitLeft.left,
                                    splitLeft.center,
                                    splitLeft.right + rightText,
                                    expectedSelectionStart)));
                }
            }
        }

        return list;
    }
    private static GenericTestStateWithSettingsAndDeletingText getDeleteBeginningOfCompositionCase(
            final String titleInfo, final Named<TestSettings> settingsOption,
            final String leftText, final String textToDelete, final TwoPartText splitRight,
            final boolean dropComposition) {
        final int initialSelectionStart = leftText.length() + textToDelete.length();
        final int expectedSelectionStart = leftText.length();
        return new GenericTestStateWithSettingsAndDeletingText(
                titleInfo,
                settingsOption, textToDelete,
                new ComposedState(leftText,
                        textToDelete + splitRight.left,
                        splitRight.right,
                        initialSelectionStart),
                dropComposition
                        ? new CommittedState(leftText + splitRight.full,
                                expectedSelectionStart)
                        : new ComposedState(leftText,
                                splitRight.left,
                                splitRight.right,
                                expectedSelectionStart));
    }
    private static GenericTestStateWithSettingsAndDeletingText getDeleteInCompositionCase(
            final String titleInfo, final Named<TestSettings> settingsOption,
            final TwoPartText splitLeft, final String textToDelete, final TwoPartText splitRight,
            final boolean dropComposition) {
        final int initialSelectionStart = splitLeft.full.length() + textToDelete.length();
        final int expectedSelectionStart = splitLeft.full.length();
        return new GenericTestStateWithSettingsAndDeletingText(
                titleInfo,
                settingsOption, textToDelete,
                new ComposedState(splitLeft.left,
                        splitLeft.right + textToDelete + splitRight.left,
                        splitRight.right,
                        initialSelectionStart),
                dropComposition
                        ? new CommittedState(splitLeft.full + splitRight.full,
                                expectedSelectionStart)
                        : new ComposedState(splitLeft.left,
                                splitLeft.right + splitRight.left,
                                splitRight.right,
                                expectedSelectionStart));
    }
    private static GenericTestStateWithSettingsAndDeletingText getDeleteThroughCompositionStartCase(
            final String titleInfo, final Named<TestSettings> settingsOption,
            final String leftText, final String textToDelete, final TwoPartText splitRight,
            final boolean dropComposition) {
        final int initialSelectionStart = leftText.length() + textToDelete.length();
        final int expectedSelectionStart = leftText.length();
        final TwoPartText toDelete = new TwoPartText(textToDelete);
        return new GenericTestStateWithSettingsAndDeletingText(
                titleInfo,
                settingsOption, textToDelete,
                new ComposedState(leftText + toDelete.left,
                        toDelete.right + splitRight.left,
                        splitRight.right,
                        initialSelectionStart),
                dropComposition
                        ? new CommittedState(leftText + splitRight.full, expectedSelectionStart)
                        : new ComposedState(leftText, splitRight.left, splitRight.right,
                                expectedSelectionStart));
    }
    private static GenericTestStateWithSettingsAndDeletingText getDeleteThroughCompositionEndCase(
            final String titleInfo, final Named<TestSettings> settingsOption,
            final TwoPartText splitLeft, final String textToDelete, final String rightText,
            final boolean dropComposition) {
        final int initialSelectionStart = splitLeft.full.length() + textToDelete.length();
        final int expectedSelectionStart = splitLeft.full.length();
        final TwoPartText toDelete = new TwoPartText(textToDelete);
        return new GenericTestStateWithSettingsAndDeletingText(
                titleInfo,
                settingsOption, textToDelete,
                new ComposedState(splitLeft.left,
                        splitLeft.right + toDelete.left,
                        toDelete.right + rightText,
                        initialSelectionStart),
                dropComposition
                        ? new CommittedState(splitLeft.full + rightText, expectedSelectionStart)
                        : new ComposedState(splitLeft.left, splitLeft.right, rightText,
                                expectedSelectionStart));
    }

    @RunWith(Parameterized.class)
    public static class SendKeyEventUnicodeCharTest extends ActionTestBase<SendKey> {
        public SendKeyEventUnicodeCharTest(final ActionTestCase<SendKey> testCase) {
            super(testCase);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<Grouped<ActionTestCase<SendKey>>> list = new GroupedList<>();

            final KeyEventInfo[] keyEvents = new KeyEventInfo[] {
                    new KeyEventInfo(KeyEvent.KEYCODE_0, '0', "KEYCODE_0"),
                    new KeyEventInfo(KeyEvent.KEYCODE_9, '9', "KEYCODE_9"),
                    new KeyEventInfo(KeyEvent.KEYCODE_A, 'a', "KEYCODE_A"),
                    new KeyEventInfo(KeyEvent.KEYCODE_Z, 'z', "KEYCODE_Z")
            };

            final Named<String> initialNoText =
                    new Named<>("no text", "");
            final Named<String> initialNormalText =
                    new Named<>("normal text", "Lorem ipsum dolor sit amet");
            final Named<String> initialSurrogatePairs =
                    new Named<>("surrogate pairs", getSurrogatePairString(0, 26));
            final Named<String>[] initialTextOptions = new Named[] {
                    initialNoText,
                    initialNormalText,
                    initialSurrogatePairs,
            };

            int caseIndex = 0;
            for (final Named<TestSettings> settingsOption : getAddTextSettingsOptions(true)) {
                for (final Named<String> initialTextOption : initialTextOptions) {
                    KeyEventInfo keyEventInfo = keyEvents[caseIndex++ % keyEvents.length];
                    final List<GenericTestState> cases = new ArrayList<>();
                    if (initialTextOption.data.length() == 0) {
                        // no initial text
                        cases.add(new GenericTestState(null,
                                new CommittedState(initialTextOption.data),
                                new CommittedState("" + keyEventInfo.unicodeChar)));
                    } else {
                        final SplitText initialText = new SplitText(initialTextOption.data);

                        //TODO: (EW) consider using ComposedTextPosition

                        // composed text with a single cursor
                        cases.add(new GenericTestState("the cursor at the beginning",
                                new CommittedState("", initialTextOption.data),
                                new CommittedState("" + keyEventInfo.unicodeChar, initialTextOption.data)));
                        cases.add(new GenericTestState("the cursor in the middle",
                                new CommittedState(initialText.biLeft, initialText.biRight),
                                new CommittedState(initialText.biLeft + keyEventInfo.unicodeChar,
                                        initialText.biRight)));
                        cases.add(new GenericTestState("the cursor at the end",
                                new CommittedState(initialTextOption.data),
                                new CommittedState(initialTextOption.data + keyEventInfo.unicodeChar)));

                        // composed text with a selection
                        cases.add(new GenericTestState("a selection at the beginning",
                                new CommittedState("",
                                        getSubstring(initialTextOption.data, 0, 1),
                                        getSubstring(initialTextOption.data, 1)),
                                new CommittedState("" + keyEventInfo.unicodeChar, getSubstring(initialTextOption.data, 1))));
                        cases.add(new GenericTestState("a selection in the middle",
                                new CommittedState(initialText.biLeft+ initialText.biRight,
                                        initialText.biLeft.length(),
                                        initialText.biLeft.length() + getCharCountBeforeCodePoint(initialText.biRight, 1)),
                                new CommittedState(initialText.biLeft + keyEventInfo.unicodeChar,
                                        getSubstring(initialText.biRight, 1))));
                        cases.add(new GenericTestState("a selection at the end",
                                new CommittedState(getSubstring(initialTextOption.data, 0, -1),
                                        getSubstring(initialTextOption.data, -1), ""),
                                new CommittedState(getSubstring(initialTextOption.data, 0, -1) + keyEventInfo.unicodeChar)));
                        cases.add(new GenericTestState("a selection of the whole text",
                                new CommittedState("", initialTextOption.data, ""),
                                new CommittedState("" + keyEventInfo.unicodeChar)));

                        // cursor before the composition
                        cases.add(new GenericTestState("the cursor before the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, 1)),
                                new ComposedState(getSubstring(initialText.triLeft, 0, 1)
                                        + keyEventInfo.unicodeChar + getSubstring(initialText.triLeft, 1),
                                        initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, 1) + 1)));
                        cases.add(new GenericTestState("a selection before the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, 1),
                                        getCharCountBeforeCodePoint(initialText.triLeft, 2)),
                                new ComposedState(getSubstring(initialText.triLeft, 0, 1)
                                        + keyEventInfo.unicodeChar + getSubstring(initialText.triLeft, 2),
                                        initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, 1) + 1)));
                        cases.add(new GenericTestState("the cursor immediately before the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight, initialText.triLeft.length()),
                                new ComposedState(initialText.triLeft + keyEventInfo.unicodeChar,
                                        initialText.triCenter, initialText.triRight, initialText.triLeft.length() + 1)));
                        cases.add(new GenericTestState("a selection immediately before the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, -1),
                                        initialText.triLeft.length()),
                                new ComposedState(getSubstring(initialText.triLeft, 0, -1) + keyEventInfo.unicodeChar,
                                        initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, -1) + 1)));

                        // cursor in the composition - adds to composition
                        cases.add(new GenericTestState("a selection at the beginning of the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length(),
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, 1)),
                                new ComposedState(initialText.triLeft,
                                        keyEventInfo.unicodeChar
                                                + getSubstring(initialText.triCenter, 1),
                                        initialText.triRight,
                                        initialText.triLeft.length() + 1)));
                        cases.add(new GenericTestState("the cursor in the middle of the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, 1)),
                                new ComposedState(initialText.triLeft,
                                        getSubstring(initialText.triCenter, 0, 1)
                                                + keyEventInfo.unicodeChar
                                                + getSubstring(initialText.triCenter, 1),
                                        initialText.triRight,
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, 1) + 1)));
                        cases.add(new GenericTestState("a selection in the middle of the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, 1),
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, -1)),
                                new ComposedState(initialText.triLeft,
                                        getSubstring(initialText.triCenter, 0, 1)
                                                + keyEventInfo.unicodeChar
                                                + getSubstring(initialText.triCenter, -1),
                                        initialText.triRight,
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, 1) + 1)));
                        cases.add(new GenericTestState("a selection at the end of the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, -1),
                                        initialText.triLeft.length() + initialText.triCenter.length()),
                                new ComposedState(initialText.triLeft,
                                        getSubstring(initialText.triCenter, 0, -1) + keyEventInfo.unicodeChar,
                                        initialText.triRight,
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, -1) + 1)));
                        cases.add(new GenericTestState("a selection of the whole composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length(),
                                        initialText.triLeft.length() + initialText.triCenter.length()),
                                new ComposedState(initialText.triLeft,
                                        "" + keyEventInfo.unicodeChar,
                                        initialText.triRight)));

                        // cursor after the composition
                        cases.add(new GenericTestState("the cursor immediately after the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight),
                                new ComposedState(initialText.triLeft, initialText.triCenter,
                                        keyEventInfo.unicodeChar + initialText.triRight,
                                        initialText.triLeft.length() + initialText.triCenter.length() + 1)));
                        cases.add(new GenericTestState("a selection immediately after the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length() + initialText.triCenter.length(),
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triLeft, 1)),
                                new ComposedState(initialText.triLeft, initialText.triCenter,
                                        keyEventInfo.unicodeChar + getSubstring(initialText.triRight, 1),
                                        initialText.triLeft.length() + initialText.triCenter.length() + 1)));
                        cases.add(new GenericTestState("the cursor after the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triRight, -1)),
                                new ComposedState(initialText.triLeft, initialText.triCenter,
                                        getSubstring(initialText.triRight, 0, -1)
                                                + keyEventInfo.unicodeChar
                                                + getSubstring(initialText.triRight, -1),
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triRight, -1) + 1)));
                        cases.add(new GenericTestState("a selection after the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triRight, -2),
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triRight, -1)),
                                new ComposedState(initialText.triLeft, initialText.triCenter,
                                        getSubstring(initialText.triRight, 0, -2)
                                                + keyEventInfo.unicodeChar + getSubstring(initialText.triRight, -1),
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triRight, -2) + 1)));

                        // selection through the composition - new text not composed
                        cases.add(new GenericTestState("a selection through the beginning of the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, -1),
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, 1)),
                                new ComposedState(
                                        getSubstring(initialText.triLeft, 0, -1)
                                                + keyEventInfo.unicodeChar,
                                        getSubstring(initialText.triCenter, 1),
                                        initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, -1) + 1)));
                        cases.add(new GenericTestState("a selection through the end of the composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, -1),
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triRight, 1)),
                                new ComposedState(initialText.triLeft,
                                        getSubstring(initialText.triCenter, 0, -1),
                                        keyEventInfo.unicodeChar + getSubstring(initialText.triRight, 1),
                                        initialText.triLeft.length()
                                                + getCharCountBeforeCodePoint(initialText.triCenter, -1) + 1)));
                        cases.add(new GenericTestState("a selection through the whole composition",
                                new ComposedState(initialText.triLeft, initialText.triCenter, initialText.triRight,
                                        getCharCountBeforeCodePoint(initialText.triLeft, -1),
                                        initialText.triLeft.length() + initialText.triCenter.length()
                                                + getCharCountBeforeCodePoint(initialText.triRight, 1)),
                                new CommittedState(getSubstring(initialText.triLeft, 0, -1)
                                        + keyEventInfo.unicodeChar,
                                        getSubstring(initialText.triRight, 1))));
                    }
                    for (int i = 0; i < cases.size(); i++) {
                        GenericTestState testState = cases.get(i);
                        list.add(new Grouped<>(testState.titleInfo, new ActionTestCase<>("send " + keyEventInfo.keyName
                                + (TextUtils.isEmpty(testState.titleInfo) ? "" : (" with " + testState.titleInfo))
                                + " in " + initialTextOption.name,
                                settingsOption,
                                testState.initialState,
                                new SendUnicodeCharKey(keyEventInfo),
                                testState.expectedState)));
                    }
                }
            }

            // unexpected changes
            final SplitText text = new SplitText("Lorem ipsum dolor sit amet");
            for (final Named<TestSettingsBuilder> settingsOption : getAddTextSettingsBuilderOptions(true)) {

                KeyEventInfo keyEventInfo = keyEvents[caseIndex++ % keyEvents.length];
                final List<GenericTestStateWithTextModifier> cases = new ArrayList<>();
                cases.add(new GenericTestStateWithTextModifier(
                        "adds extra characters",
                        new DoubleTextModifier(),
                        new CommittedState(text.biLeft, text.biRight),
                        new CommittedState(text.biLeft + keyEventInfo.unicodeChar + keyEventInfo.unicodeChar, text.biRight),
                        false, false));
                cases.add(new GenericTestStateWithTextModifier(
                        "adds extra characters with a selection",
                        new DoubleTextModifier(),
                        new CommittedState(text.triLeft, text.triCenter, text.triRight),
                        new CommittedState(
                                text.triLeft + keyEventInfo.unicodeChar + keyEventInfo.unicodeChar,
                                text.triRight),
                        false, false));
                final String extraText = "long extra text";
                cases.add(new GenericTestStateWithTextModifier(
                        "adds many extra characters",
                        new ExtraTextTextModifier(extraText),
                        new CommittedState(text.biLeft, text.biRight),
                        new CommittedState(text.biLeft + keyEventInfo.unicodeChar + extraText,
                                text.biRight),
                        false, false));
                cases.add(new GenericTestStateWithTextModifier(
                        "adds many extra characters with a selection",
                        new ExtraTextTextModifier(extraText),
                        new CommittedState(text.triLeft, text.triCenter, text.triRight),
                        new CommittedState(text.triLeft + keyEventInfo.unicodeChar + extraText,
                                text.triRight),
                        false, false));
                if (keyEventInfo.unicodeChar >= '0' && keyEventInfo.unicodeChar <= '8') {
                    cases.add(new GenericTestStateWithTextModifier(
                            "changes character",
                            new IncrementNumberTextModifier(),
                            new CommittedState(text.biLeft, text.biRight),
                            new CommittedState(text.biLeft + ((char)(keyEventInfo.unicodeChar + 1)),
                                    text.biRight),
                            true, false));
                    cases.add(new GenericTestStateWithTextModifier(
                            "changes character with a selection",
                            new IncrementNumberTextModifier(),
                            new CommittedState(text.triLeft, text.triCenter, text.triRight),
                            new CommittedState(text.triLeft + ((char)(keyEventInfo.unicodeChar + 1)),
                                    text.triRight),
                            true, false));
                }

                cases.add(new GenericTestStateWithTextModifier(
                        "adds extra characters in the composition",
                        new DoubleTextModifier(),
                        new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1)),
                        new ComposedState(text.triLeft,
                                getSubstring(text.triCenter, 0, 1)
                                        + keyEventInfo.unicodeChar + keyEventInfo.unicodeChar
                                        + getSubstring(text.triCenter, 1), text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1) + 2),
                        false, false));
                cases.add(new GenericTestStateWithTextModifier(
                        "adds extra characters in the composition with a selection",
                        new DoubleTextModifier(),
                        new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1),
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 3)),
                        new ComposedState(text.triLeft,
                                getSubstring(text.triCenter, 0, 1)
                                        + keyEventInfo.unicodeChar + keyEventInfo.unicodeChar
                                        + getSubstring(text.triCenter, 3), text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1) + 2),
                        false, false));
                cases.add(new GenericTestStateWithTextModifier(
                        "adds many extra characters in the composition",
                        new ExtraTextTextModifier(extraText),
                        new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1)),
                        new ComposedState(text.triLeft,
                                getSubstring(text.triCenter, 0, 1)
                                        + keyEventInfo.unicodeChar + extraText
                                        + getSubstring(text.triCenter, 1), text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1) + 1
                                        + extraText.length()),
                        false, false));
                cases.add(new GenericTestStateWithTextModifier(
                        "adds many extra characters in the composition with a selection",
                        new ExtraTextTextModifier(extraText),
                        new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1),
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 3)),
                        new ComposedState(text.triLeft,
                                getSubstring(text.triCenter, 0, 1)
                                        + keyEventInfo.unicodeChar + extraText
                                        + getSubstring(text.triCenter, 3), text.triRight,
                                text.triLeft.length()
                                        + getCharCountBeforeCodePoint(text.triCenter, 1) + 1
                                        + extraText.length()),
                        false, false));
                if (keyEventInfo.unicodeChar >= '0' && keyEventInfo.unicodeChar <= '8') {
                    cases.add(new GenericTestStateWithTextModifier(
                            "changes character in the composition",
                            new IncrementNumberTextModifier(),
                            new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                    text.triLeft.length()
                                            + getCharCountBeforeCodePoint(text.triCenter, 1)),
                            new ComposedState(text.triLeft,
                                    getSubstring(text.triCenter, 0, 1)
                                            + ((char)(keyEventInfo.unicodeChar + 1))
                                            + getSubstring(text.triCenter, 1), text.triRight,
                                    text.triLeft.length()
                                            + getCharCountBeforeCodePoint(text.triCenter, 1) + 1),
                            true, false));
                    cases.add(new GenericTestStateWithTextModifier(
                            "changes character in the composition with a selection",
                            new IncrementNumberTextModifier(),
                            new ComposedState(text.triLeft, text.triCenter, text.triRight,
                                    text.triLeft.length()
                                            + getCharCountBeforeCodePoint(text.triCenter, 1),
                                    text.triLeft.length()
                                            + getCharCountBeforeCodePoint(text.triCenter, 3)),
                            new ComposedState(text.triLeft,
                                    getSubstring(text.triCenter, 0, 1)
                                            + ((char)(keyEventInfo.unicodeChar + 1))
                                            + getSubstring(text.triCenter, 3), text.triRight,
                                    text.triLeft.length()
                                            + getCharCountBeforeCodePoint(text.triCenter, 1) + 1),
                            true, false));
                }

                for (int i = 0; i < cases.size(); i++) {
                    GenericTestStateWithTextModifier testState = cases.get(i);
                    for (boolean useBatch : new boolean[] {true, false}) {
                        list.add(new Grouped<>(testState.titleInfo, new ActionTestCase<>("send "
                                + keyEventInfo.keyName + (TextUtils.isEmpty(testState.titleInfo)
                                        ? ""
                                        : (" where the input connection " + testState.titleInfo)),
                                new Named<>(settingsOption.name, new TestSettings(
                                        new VariableBehaviorSettings(
                                                settingsOption.data.inputConnectionSettingsBuilder)
                                                .setTextModifier(testState.textModifier),
                                        settingsOption.data.initialCursorPositionKnown)),
                                useBatch,
                                testState.initialState,
                                new SendUnicodeCharKey(keyEventInfo),
                                testState.expectedState, testState.isExpectedChange,
                                testState.isExpectedTextChange)));
                    }
                }
            }

            return buildParameters(list);
        }
    }

    //TODO: add KEYCODE_DPAD_LEFT/KEYCODE_DPAD_RIGHT tests
    // is this even necessary? the only reason this ever gets called is when there is no cursor
    // position, so the only thing we'd really test is if it gets forwarded to the fake, which
    // doesn't seem particularly valuable.

    @RunWith(Parameterized.class)
    public static class GetUnicodeStepsTest {
        private final UnicodeStepParams testCase;

        public GetUnicodeStepsTest(final UnicodeStepParams testCase) {
            this.testCase = testCase;
        }

        @Test
        public void runTest() {
            System.out.println("Parameterized case: " + testCase);

            final String before = testCase.text.left;
            final String selection = testCase.text.center;
            final String after = testCase.text.right;
            final int chars = testCase.chars;
            final boolean rightSidePointer = testCase.rightSidePointer;
            final int expectedUnicodeSteps = testCase.expectedUnicodeSteps;

            final RichInputConnectionManager manager = new RichInputConnectionManager();

            final int start = before.length();
            final int end = before.length() + selection.length();
            final State state = new CommittedState(before + selection + after, start, end);
            printState(new CommittedState(before + selection + after, start, end), 0);
            manager.setUpState(null, state, true, false);

            final int unicodeSteps = manager.richInputConnection.getUnicodeSteps(chars, rightSidePointer);

            assertEquals("unicode steps", expectedUnicodeSteps, unicodeSteps);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<Grouped<UnicodeStepParams>> list = new GroupedList<>();

            final Named<String>[] requestedTextOptions = new Named[] {
                    new Named<>("single regular character", "a"),
                    new Named<>("single surrogate pair", getSurrogatePairString(0)),
                    new Named<>("multiple regular characters", "asdf"),
                    new Named<>("multiple surrogate pairs", getSurrogatePairString(0, 4)),
                    new Named<>("mixed surrogate pairs and normal characters",
                            "a" + getSurrogatePairString(0) + "b" + getSurrogatePairString(1)),
                    new Named<>("no character", "")
            };

            final SplitText text = new SplitText("Lorem ipsum dolor sit amet");

            for (final Named<String> requestedText : requestedTextOptions) {
                //TODO: these names seem flipped, but I think getUnicodeSteps is actually named incorrectly
                final int chars = codePointCount(requestedText.data);
                final int expectedUnicodeSteps = requestedText.data.length();

                final List<UnicodeStepCase> cases = new ArrayList<>();

                cases.add(UnicodeStepCase.beforeSelectionStart(
                        new CommittedState(text.triLeft + requestedText.data, text.triCenter, text.triRight)));
                cases.add(UnicodeStepCase.afterSelectionStart("within selection",
                        new CommittedState(text.triLeft, requestedText.data + text.triCenter, text.triRight)));

                if (chars > 1) {
                    final TwoPartText splitRequestedText = new TwoPartText(requestedText.data);
                    cases.add(UnicodeStepCase.afterSelectionStart("extending past selection",
                            new CommittedState(text.biLeft, splitRequestedText.left,
                                    splitRequestedText.right + text.biRight)));
                }

                cases.add(UnicodeStepCase.afterSelectionStart("no selection",
                        new CommittedState(text.biLeft, requestedText.data + text.biRight)));
                cases.add(UnicodeStepCase.beforeSelectionEnd("no selection",
                        new CommittedState(text.biLeft + requestedText.data, text.biRight)));

                if (chars > 1) {
                    final TwoPartText splitRequestedText = new TwoPartText(requestedText.data);
                    cases.add(UnicodeStepCase.beforeSelectionEnd("extending past selection",
                            new CommittedState(text.biLeft + splitRequestedText.left,
                                    splitRequestedText.right, text.biRight)));
                }

                cases.add(UnicodeStepCase.beforeSelectionEnd("within selection",
                        new CommittedState(text.triLeft, text.triCenter + requestedText.data, text.triRight)));
                cases.add(UnicodeStepCase.afterSelectionEnd(
                        new CommittedState(text.triLeft, text.triCenter, requestedText.data + text.triRight)));

                for (UnicodeStepCase unicodeStepCase : cases) {
                    list.add(new Grouped<>(unicodeStepCase.titleInfo,
                            new UnicodeStepParams(requestedText + " " + unicodeStepCase.titleInfo,
                                    unicodeStepCase.state,
                                    unicodeStepCase.after ? chars : -chars, unicodeStepCase.rightSidePointer,
                                    unicodeStepCase.after ? expectedUnicodeSteps : -expectedUnicodeSteps)));
                }
            }

            //TODO: add tests (maybe) for unknown cursor position, composed state, and limited text

            return buildParameters(list);
        }

        private static class UnicodeStepCase {
            final String titleInfo;
            final State state;
            final boolean after;
            final boolean rightSidePointer;
            public UnicodeStepCase(final String titleBase, final String titleExtra,
                                   final State state,
                                   final boolean after, final boolean rightSidePointer) {
                this.titleInfo = titleBase
                        + (TextUtils.isEmpty(titleExtra) ? "" : (" (" + titleExtra + ")"));
                this.state = state;
                this.after = after;
                this.rightSidePointer = rightSidePointer;
            }

            public static UnicodeStepCase beforeSelectionStart(final State state) {
                return new UnicodeStepCase("before selection start", null, state, false, false);
            }
            public static UnicodeStepCase beforeSelectionEnd(final State state) {
                return beforeSelectionEnd(null, state);
            }
            public static UnicodeStepCase beforeSelectionEnd(final String titleExtra, final State state) {
                return new UnicodeStepCase("before selection end", titleExtra, state, false, true);
            }
            public static UnicodeStepCase afterSelectionStart(final State state) {
                return afterSelectionStart(null, state);
            }
            public static UnicodeStepCase afterSelectionStart(final String titleExtra, final State state) {
                return new UnicodeStepCase("after selection start", titleExtra, state, true, false);
            }
            public static UnicodeStepCase afterSelectionEnd(final State state) {
                return new UnicodeStepCase("after selection end", null, state, true, true);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class ExternalModificationTest {
        private final ExternalActionTestCase testCase;
        private final RichInputConnectionManager manager;

        public ExternalModificationTest(final ExternalActionTestCase testCase) {
            this.testCase = testCase;
            manager = new RichInputConnectionManager();
        }

        @Before
        public void setup() {
            System.out.println(testCase);
            manager.runExternalAction(testCase);
        }

        @Test
        public void testAccurateGeneralCache() {
            manager.verifyTextCache(testCase);
        }

        @Test
        public void testAccurateCompositionCache() {
            manager.verifyCompositionCache(testCase);
        }

        //TODO: is this valuable? I don't think it is
        @Test
        public void testUpdatedText() {
            manager.verifyActualText(testCase);
        }

        //TODO: is this valuable?
        @Test
        public void testUpdateSelectionCall() {
            manager.verifyUpdateSelectionCall(testCase);
        }

        //TODO: is this valuable?
//        @Test
//        public void testExpectedUpdateSelectionCalls() {
//            manager.verifyExpectedUpdateSelectionCalls(testCase);
//        }

        //TODO: see if there is a reasonable way to reduce duplicate code with DeleteTextBeforeCursorTest
        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final GroupedList<ExternalActionTestCase> list = new GroupedList<>();

            for (final Named<TestSettings> settingsOption : getExternalActionSettingsOptions()) {

                //TODO: add tests
                // test full extracted text update, large extracted text update, and edited extracted text update - done
                // test both with cache text around cursor available and not
                // change before unknown cursor position
                // change in unknown cursor position
                // change after unknown cursor position
                // shift cursor position
                // change composition position
                // change composition length

                //TODO: we might need cases without a composition to test unknown cursor positions

                final String text = "abcdefghijklmnopqrstuvwxyz";

                //TODO: rename class
                final EditPositions[] positions = new EditPositions[] {
                        new EditPositions("before cursor and composition", 1, 5, 10, 15, 20),
                        new EditPositions("before composition and cursor", 1, 15, 20, 5, 10),
                        new EditPositions("in cursor before composition", 6, 5, 10, 15, 20),
                        new EditPositions("in composition before cursor", 6, 15, 20, 5, 10),
                        new EditPositions("between cursor and composition", 11, 5, 10, 15, 20),
                        new EditPositions("between composition and cursor", 11, 15, 20, 5, 10),
                        new EditPositions("in composition after cursor", 16, 5, 10, 15, 20),
                        new EditPositions("in cursor after composition", 16, 15, 20, 5, 10),
                        new EditPositions("after cursor and composition", 21, 5, 10, 15, 20),
                        new EditPositions("after composition and cursor", 21, 15, 20, 5, 10),
                };
                final SimpleChange[] changes = new SimpleChange[] {
                        new SimpleChange("change text", "123", 3),
                        new SimpleChange("insert text", "123", 0),
                        new SimpleChange("change to more text", "123", 1),
                        new SimpleChange("change to less text", "1", 3),
                        new SimpleChange("delete text", "", 3),
                };
                for (final EditPositions position : positions) {
                    for (final SimpleChange change : changes) {
                        list.add(createTestCase(text, position, change, settingsOption));
                    }
                }


                final EditPositions[] positions2 = new EditPositions[] {
                        new EditPositions("through beginning of cursor before composition", 4,
                                5, 10, 15, 20),
                        new EditPositions("through beginning of composition before cursor", 4,
                                15, 20, 5, 10),
                        new EditPositions("through end of cursor before composition", 9,
                                5, 10, 15, 20),
                        new EditPositions("through end of composition before cursor", 9,
                                15, 20, 5, 10),
                        new EditPositions("through beginning of composition after cursor", 14,
                                5, 10, 15, 20),
                        new EditPositions("through beginning of cursor after composition", 14,
                                15, 20, 5, 10),
                        new EditPositions("through end of composition after cursor", 19,
                                5, 10, 15, 20),
                        new EditPositions("through end of cursor after composition", 19,
                                15, 20, 5, 10),
                };
                final SimpleChange[] changes2 = new SimpleChange[] {
                        new SimpleChange("change to more text", "123", 2),
                        new SimpleChange("change to less text", "12", 3),
                };
                for (final EditPositions position : positions2) {
                    for (final SimpleChange change : changes2) {
                        list.add(createTestCase(text, position, change, settingsOption));
                    }
                }


                final Named<ExternalActionSetText>[] beforeSets = new Named[] {
                        new Named<>("more", new ExternalActionSetText("123", 2, 3)),
                        new Named<>("less", new ExternalActionSetText("1", 1, 4)),
                };
                final Named<ExternalActionSetText>[] afterSets = new Named[] {
                        new Named<>("more", new ExternalActionSetText("456", 22, 23)),
                        new Named<>("less", new ExternalActionSetText("4", 21, 24)),
                };
                for (final Named<ExternalActionSetText> beforeSet : beforeSets) {
                    for (final Named<ExternalActionSetText> afterSet : afterSets) {
                        list.add(createTestCase("insert " + beforeSet.name + " before cursor and " + afterSet.name + " after composition",
                                text, 5, 10, 15, 20,
                                new ExternalActionSetText[] { afterSet.data, beforeSet.data},
                                settingsOption));
                        list.add(createTestCase("insert " + beforeSet.name + " before composition and " + afterSet.name + " after cursor",
                                text, 15, 20, 5, 10,
                                new ExternalActionSetText[] { afterSet.data, beforeSet.data},
                                settingsOption));
                    }
                }
            }

//            final List<ExternalActionTestCase> alt = explicitParameters();
//            if (alt.size() != list.size()) {
//                throw new RuntimeException("alt size: " + alt.size() + ", list size: " + list.size());
//            }
//            for (int i = 0; i < list.size(); i++) {
//                final State expectedState = list.get(i).expectedState;
//                final State altExpectedState = alt.get(i).expectedState;
//                if (!list.get(i).testName.equals(alt.get(i).testName)
//                        || !expectedState.getText().equals(altExpectedState.getText())
//                        || expectedState.getCursorStart() != altExpectedState.getCursorStart()
//                        || expectedState.getCursorEnd() != altExpectedState.getCursorEnd()
//                        || expectedState.getCompositionStart() != altExpectedState.getCompositionStart()
//                        || expectedState.getCompositionEnd() != altExpectedState.getCompositionEnd()) {
//                    throw new RuntimeException(i + "\nalt: " + alt.get(i).testName + "\n" + getDebugInfo(altExpectedState, 2)
//                            + "\nsimplified: " + list.get(i).testName + "\n" + getDebugInfo(expectedState, 2));
//                }
//            }

            return buildParameters(list);
        }
//        public static Collection parameters() {
//            final List<ExternalActionTestCase> list = new ArrayList<>();
//
//            for (final Named<TestSettings> settingsOption : getExternalActionSettingsOptions()) {
//
//                //TODO: add tests
//                // test full extracted text update, large extracted text update, and edited extracted text update
//                // test both with cache text around cursor available and not
//                // change before unknown cursor position
//                // change in unknown cursor position
//                // change after unknown cursor position
//                // change before and after composition and cursor
//                // change before and in composition
//                // change in and after composition
//                // change before and in cursor
//                // change in and after cursor
//
//                //TODO: reduce duplicative code
//                list.add(new ExternalActionTestCase("change text before cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 1, 4),
//                        new ComposedState("a123efghijklmnopqrstuvwxyz", 5, 10, 15, 20)));
//                list.add(new ExternalActionTestCase("insert text before cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 1, 1),
//                        new ComposedState("a123bcdefghijklmnopqrstuvwxyz", 5 + 3, 10 + 3, 15 + 3, 20 + 3)));
//                list.add(new ExternalActionTestCase("change to more text before cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 1, 2),
//                        new ComposedState("a123cdefghijklmnopqrstuvwxyz", 5 + 2, 10 + 2, 15 + 2, 20 + 2)));
//                list.add(new ExternalActionTestCase("delete text before cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionDeleteText(1, 4),
//                        new ComposedState("aefghijklmnopqrstuvwxyz", 5 - 3, 10 - 3, 15 - 3, 20 - 3)));
//
//                list.add(new ExternalActionTestCase("change text before composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 1, 4),
//                        new ComposedState("a123efghijklmnopqrstuvwxyz", 15, 20, 5, 10)));
//                list.add(new ExternalActionTestCase("insert text before composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 1, 1),
//                        new ComposedState("a123bcdefghijklmnopqrstuvwxyz", 15 + 3, 20 + 3, 5 + 3, 10 + 3)));
//                list.add(new ExternalActionTestCase("change to more text before composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 1, 2),
//                        new ComposedState("a123cdefghijklmnopqrstuvwxyz", 15 + 2, 20 + 2, 5 + 2, 10 + 2)));
//                list.add(new ExternalActionTestCase("delete text before composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionDeleteText(1, 4),
//                        new ComposedState("aefghijklmnopqrstuvwxyz", 15 - 3, 20 - 3, 5 - 3, 10 - 3)));
//
//                list.add(new ExternalActionTestCase("change text in cursor before composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 5 + 1, 5 + 4),
//                        new ComposedState("abcdef123jklmnopqrstuvwxyz", 5, 10, 15, 20)));
//                list.add(new ExternalActionTestCase("insert text in cursor before composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 5 + 1, 5 + 1),
//                        new ComposedState("abcdef123ghijklmnopqrstuvwxyz", 5, 10 + 3, 15 + 3, 20 + 3)));
//                list.add(new ExternalActionTestCase("change to more text in cursor before composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 5 + 1, 5 + 2),
//                        new ComposedState("abcdef123hijklmnopqrstuvwxyz", 5, 10 + 2, 15 + 2, 20 + 2)));
//                list.add(new ExternalActionTestCase("delete text in cursor before composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionDeleteText(5 + 1, 5 + 4),
//                        new ComposedState("abcdefjklmnopqrstuvwxyz", 5, 10 - 3, 15 - 3, 20 - 3)));
//
//                list.add(new ExternalActionTestCase("change text in composition before cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 5 + 1, 5 + 4),
//                        new ComposedState("abcdef123jklmnopqrstuvwxyz", 15, 20, 5, 10)));
//                list.add(new ExternalActionTestCase("insert text in composition before cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 5 + 1, 5 + 1),
//                        new ComposedState("abcdef123ghijklmnopqrstuvwxyz", 15 + 3, 20 + 3, 5, 10 + 3)));
//                list.add(new ExternalActionTestCase("change to more text in composition before cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 5 + 1, 5 + 2),
//                        new ComposedState("abcdef123hijklmnopqrstuvwxyz", 15 + 2, 20 + 2, 5, 10 + 2)));
//                list.add(new ExternalActionTestCase("delete text in composition before cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionDeleteText(5 + 1, 5 + 4),
//                        new ComposedState("abcdefjklmnopqrstuvwxyz", 15 - 3, 20 - 3, 5, 10 - 3)));
//
//                list.add(new ExternalActionTestCase("change text between cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 10 + 1, 10 + 4),
//                        new ComposedState("abcdefghijk123opqrstuvwxyz", 5, 10, 15, 20)));
//                list.add(new ExternalActionTestCase("insert text between cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 10 + 1, 10 + 1),
//                        new ComposedState("abcdefghijk123lmnopqrstuvwxyz", 5, 10, 15 + 3, 20 + 3)));
//                list.add(new ExternalActionTestCase("change to more text between cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 10 + 1, 10 + 2),
//                        new ComposedState("abcdefghijk123mnopqrstuvwxyz", 5, 10, 15 + 2, 20 + 2)));
//                list.add(new ExternalActionTestCase("delete text between cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionDeleteText(10 + 1, 10 + 4),
//                        new ComposedState("abcdefghijkopqrstuvwxyz", 5, 10, 15 - 3, 20 - 3)));
//
//                list.add(new ExternalActionTestCase("change text between composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 10 + 1, 10 + 4),
//                        new ComposedState("abcdefghijk123opqrstuvwxyz", 15, 20, 5, 10)));
//                list.add(new ExternalActionTestCase("insert text between composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 10 + 1, 10 + 1),
//                        new ComposedState("abcdefghijk123lmnopqrstuvwxyz", 15 + 3, 20 + 3, 5, 10)));
//                list.add(new ExternalActionTestCase("change to more text between composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 10 + 1, 10 + 2),
//                        new ComposedState("abcdefghijk123mnopqrstuvwxyz", 15 + 2, 20 + 2, 5, 10)));
//                list.add(new ExternalActionTestCase("delete text between composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionDeleteText(10 + 1, 10 + 4),
//                        new ComposedState("abcdefghijkopqrstuvwxyz", 15 - 3, 20 - 3, 5, 10)));
//
//                list.add(new ExternalActionTestCase("change text in composition after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 15 + 1, 15 + 4),
//                        new ComposedState("abcdefghijklmnop123tuvwxyz", 5, 10, 15, 20)));
//                list.add(new ExternalActionTestCase("insert text in composition after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 15 + 1, 15 + 1),
//                        new ComposedState("abcdefghijklmnop123qrstuvwxyz", 5, 10, 15, 20 + 3)));
//                list.add(new ExternalActionTestCase("change to more text in composition after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 15 + 1, 15 + 2),
//                        new ComposedState("abcdefghijklmnop123rstuvwxyz", 5, 10, 15, 20 + 2)));
//                list.add(new ExternalActionTestCase("delete text in composition after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionDeleteText(15 + 1, 15 + 4),
//                        new ComposedState("abcdefghijklmnoptuvwxyz", 5, 10, 15, 20 - 3)));
//
//                list.add(new ExternalActionTestCase("change text in cursor after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 15 + 1, 15 + 4),
//                        new ComposedState("abcdefghijklmnop123tuvwxyz", 15, 20, 5, 10)));
//                list.add(new ExternalActionTestCase("insert text in cursor after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 15 + 1, 15 + 1),
//                        new ComposedState("abcdefghijklmnop123qrstuvwxyz", 15, 20 + 3, 5, 10)));
//                list.add(new ExternalActionTestCase("change to more text in cursor after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 15 + 1, 15 + 2),
//                        new ComposedState("abcdefghijklmnop123rstuvwxyz", 15, 20 + 2, 5, 10)));
//                list.add(new ExternalActionTestCase("delete text in cursor after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionDeleteText(15 + 1, 15 + 4),
//                        new ComposedState("abcdefghijklmnoptuvwxyz", 15, 20 - 3, 5, 10)));
//
//                list.add(new ExternalActionTestCase("change text after cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 20 + 1, 20 + 4),
//                        new ComposedState("abcdefghijklmnopqrstu123yz", 5, 10, 15, 20)));
//                list.add(new ExternalActionTestCase("insert text after cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 20 + 1, 20 + 1),
//                        new ComposedState("abcdefghijklmnopqrstu123vwxyz", 5, 10, 15, 20)));
//                list.add(new ExternalActionTestCase("change to more text after cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("123", 20 + 1, 20 + 2),
//                        new ComposedState("abcdefghijklmnopqrstu123wxyz", 5, 10, 15, 20)));
//                list.add(new ExternalActionTestCase("delete text after cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionDeleteText(20 + 1, 20 + 4),
//                        new ComposedState("abcdefghijklmnopqrstuyz", 5, 10, 15, 20)));
//
//                list.add(new ExternalActionTestCase("change text after composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 20 + 1, 20 + 4),
//                        new ComposedState("abcdefghijklmnopqrstu123yz", 15, 20, 5, 10)));
//                list.add(new ExternalActionTestCase("insert text after composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 20 + 1, 20 + 1),
//                        new ComposedState("abcdefghijklmnopqrstu123vwxyz", 15, 20, 5, 10)));
//                list.add(new ExternalActionTestCase("change to more text after composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("123", 20 + 1, 20 + 2),
//                        new ComposedState("abcdefghijklmnopqrstu123wxyz", 15, 20, 5, 10)));
//                list.add(new ExternalActionTestCase("delete text after composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionDeleteText(20 + 1, 20 + 4),
//                        new ComposedState("abcdefghijklmnopqrstuyz", 15, 20, 5, 10)));
//
//
//                list.add(new ExternalActionTestCase("change through cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("IJKLMNOPQ", 8, 17),
//                        new ComposedState("abcdefghIJKLMNOPQrstuvwxyz", 5, 8, 17, 20)));
//                list.add(new ExternalActionTestCase("change through composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("IJKLMNOPQ", 8, 17),
//                        new ComposedState("abcdefghIJKLMNOPQrstuvwxyz", 17, 20, 5, 8)));
//
//                list.add(new ExternalActionTestCase("insert less through cursor and composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionSetText("IJKOPQ", 8, 17),
//                        new ComposedState("abcdefghIJKOPQrstuvwxyz", 5, 8, 14, 17)));
//                list.add(new ExternalActionTestCase("insert less through composition and cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionSetText("IJKOPQ", 8, 17),
//                        new ComposedState("abcdefghIJKOPQrstuvwxyz", 14, 17, 5, 8)));
//
//
//                list.add(new ExternalActionTestCase("insert more before cursor and more after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("456", 22, 23),
//                                new ExternalActionSetText("123", 2, 3)
//                        }),
//                        new ComposedState("ab123defghijklmnopqrstuv456xyz", 7, 12, 17, 22)));
//                list.add(new ExternalActionTestCase("insert more before composition and more after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("456", 22, 23),
//                                new ExternalActionSetText("123", 2, 3)
//                        }),
//                        new ComposedState("ab123defghijklmnopqrstuv456xyz", 17, 22, 7, 12)));
//
//                list.add(new ExternalActionTestCase("insert more before cursor and less after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("4", 21, 24),
//                                new ExternalActionSetText("123", 2, 3)
//                        }),
//                        new ComposedState("ab123defghijklmnopqrstu4yz", 7, 12, 17, 22)));
//                list.add(new ExternalActionTestCase("insert more before composition and less after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("4", 21, 24),
//                                new ExternalActionSetText("123", 2, 3)
//                        }),
//                        new ComposedState("ab123defghijklmnopqrstu4yz", 17, 22, 7, 12)));
//
//                list.add(new ExternalActionTestCase("insert less before cursor and more after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("456", 22, 23),
//                                new ExternalActionSetText("1", 1, 4)
//                        }),
//                        new ComposedState("a1efghijklmnopqrstuv456xyz", 3, 8, 13, 18)));
//                list.add(new ExternalActionTestCase("insert less before composition and more after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("456", 22, 23),
//                                new ExternalActionSetText("1", 1, 4)
//                        }),
//                        new ComposedState("a1efghijklmnopqrstuv456xyz", 13, 18, 3, 8)));
//
//                list.add(new ExternalActionTestCase("insert less before cursor and less after composition",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 5, 10, 15, 20),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("4", 21, 24),
//                                new ExternalActionSetText("1", 1, 4)
//                        }),
//                        new ComposedState("a1efghijklmnopqrstu4yz", 3, 8, 13, 18)));
//                list.add(new ExternalActionTestCase("insert less before composition and less after cursor",
//                        settingsOption,
//                        new ComposedState("abcdefghijklmnopqrstuvwxyz", 15, 20, 5, 10),
//                        new ExternalActionBatch<>(new ExternalAction[] {
//                                new ExternalActionSetText("4", 21, 24),
//                                new ExternalActionSetText("1", 1, 4)
//                        }),
//                        new ComposedState("a1efghijklmnopqrstu4yz", 13, 18, 3, 8)));
//            }
//
//            final Object[][] array = new Object[list.size()][];
//            for (int i = 0; i < list.size(); i++) {
//                array[i] = new Object[] { list.get(i) };
//            }
//            return Arrays.asList(array);
//        }

        //TODO: this probably needs a clarifying name
        private static ExternalActionTestCase createTestCase(final String text,
                                                             final EditPositions position,
                                                             final SimpleChange change,
                                                             final Named<TestSettings> settingsOption) {
            //TODO: refactor to call into the overload to reduce duplicate code
            final int updatedCursorStart = getUpdatedPosition(position.cursorStart,
                    position.editStart, change.deleteLength, change.addedText.length(), true);
            final int updatedCursorEnd = getUpdatedPosition(position.cursorEnd,
                    position.editStart, change.deleteLength, change.addedText.length(), false);
            final int updatedCompositionStart = getUpdatedPosition(position.compositionStart,
                    position.editStart, change.deleteLength, change.addedText.length(), true);
            final int updatedCompositionEnd = getUpdatedPosition(position.compositionEnd,
                    position.editStart, change.deleteLength, change.addedText.length(), false);

            final String insertedText = change.addedText;
            final int changeEnd = position.editStart + change.deleteLength;
            return new ExternalActionTestCase(change.name + " " + position.name,
                    settingsOption,
                    new ComposedState(text,
                            position.cursorStart, position.cursorEnd,
                            position.compositionStart, position.compositionEnd),
                    new ExternalActionSetText(insertedText, position.editStart, changeEnd),
                    new ComposedState(text.substring(0, position.editStart) + insertedText + text.substring(changeEnd),
                            updatedCursorStart, updatedCursorEnd,
                            updatedCompositionStart, updatedCompositionEnd));
        }
        private static ExternalActionTestCase createTestCase(final String name,
                                                             final String text,
                                                             final int initialCursorStart,
                                                             final int initialCursorEnd,
                                                             final int initialCompositionStart,
                                                             final int initialCompositionEnd,
                                                             final ExternalActionSetText[] externalActions,
                                                             final Named<TestSettings> settingsOption) {
            String updatedText = text;
            int updatedCursorStart = initialCursorStart;
            int updatedCursorEnd = initialCursorEnd;
            int updatedCompositionStart = initialCompositionStart;
            int updatedCompositionEnd = initialCompositionEnd;
            for (final ExternalActionSetText externalAction : externalActions) {
                final int deleteLength = externalAction.end - externalAction.start;
                updatedCursorStart = getUpdatedPosition(updatedCursorStart,
                        externalAction.start, deleteLength, externalAction.text.length(), true);
                updatedCursorEnd = getUpdatedPosition(updatedCursorEnd,
                        externalAction.start, deleteLength, externalAction.text.length(), false);
                updatedCompositionStart = getUpdatedPosition(updatedCompositionStart,
                        externalAction.start, deleteLength, externalAction.text.length(), true);
                updatedCompositionEnd = getUpdatedPosition(updatedCompositionEnd,
                        externalAction.start, deleteLength, externalAction.text.length(), false);
                updatedText = updatedText.substring(0, externalAction.start) + externalAction.text + updatedText.substring(externalAction.end);
            }

            return new ExternalActionTestCase(name,
                    settingsOption,
                    new ComposedState(text,
                            initialCursorStart, initialCursorEnd,
                            initialCompositionStart, initialCompositionEnd),
                    new ExternalActionBatch<>(externalActions),
                    new ComposedState(updatedText,
                            updatedCursorStart, updatedCursorEnd,
                            updatedCompositionStart, updatedCompositionEnd));
        }

        private static int getUpdatedPosition(final int initialPosition, final int editStart,
                                              final int deleteLength, final int addLength,
                                              final boolean shiftRight) {
            if (initialPosition < editStart) {
                return initialPosition;
            }
            if (initialPosition <= editStart + deleteLength) {
                if (shiftRight) {
                    return editStart + addLength;
                }
                return editStart;
            }
            return initialPosition - deleteLength + addLength;
        }

        //TODO: name better
        private static class SimpleChange {
            public final String name;
            public final String addedText;
            public final int deleteLength;
            public SimpleChange(final String name, final String addedText, final int deleteLength) {
                this.name = name;
                this.addedText = addedText;
                this.deleteLength = deleteLength;
            }
        }

        //TODO: name better
        private static class EditPositions {
            public final String name;
            public final int editStart;
            public final int cursorStart;
            public final int cursorEnd;
            public final int compositionStart;
            public final int compositionEnd;
            public EditPositions(final String name, final int editStart,
                                 final int cursorStart, final int cursorEnd,
                                 final int compositionStart, final int compositionEnd) {
                this.name = name;
                this.editStart = editStart;
                this.cursorStart = cursorStart;
                this.cursorEnd = cursorEnd;
                this.compositionStart = compositionStart;
                this.compositionEnd = compositionEnd;
            }
        }

//        private static ExternalActionTestCase changeTextCase(final String name,
//                                                             final Named<TestSettings> settingsOption,
//                                                             final String[] textParts,
//                                                             final int selectedPart,
//                                                             final int composedPart,
//                                                             final int editedPart) {
//            final StringBuilder sb = new StringBuilder();
//            int cursorStart = 0;
//            int cursorEnd = 0;
//            int compositionStart = 0;
//            int compositionEnd = 0;
//            int editStart = 0;
//            int editEnd = 0;
//            for (int i = 0; i < textParts.length; i++) {
//                if (i == selectedPart) {
//                    cursorStart = sb.length();
//                    cursorEnd = cursorStart + textParts[i].length();
//                }
//                if (i == composedPart) {
//                    compositionStart = sb.length();
//                    compositionEnd = compositionStart + textParts[i].length();
//                }
//                if (i == editedPart) {
//                    editStart = sb.length();
//                    editEnd = editStart + textParts[i].length();
//                }
//                sb.append(textParts[i]);
//            }
//            final String fullText = sb.toString();
//
//            final CharSequence editedText = shiftText(getSubstring(fullText, editStart + 1, editEnd - 1));
//
//            return new ExternalActionTestCase(name, settingsOption,
//                    new ComposedState(fullText, cursorStart, cursorEnd, compositionStart, compositionEnd),
//                    new ExternalActionSetText(editedText, editStart + 1, editEnd - 1),
//                    new ComposedState(fullText.substring(0, editStart + 1) + editedText + fullText.substring(editEnd - 1),
//                            cursorStart, cursorEnd, compositionStart, compositionEnd));
//        }
//        private static ExternalActionTestCase insertTextCase(final String name,
//                                                             final Named<TestSettings> settingsOption,
//                                                             final String[] textParts,
//                                                             final int selectedPart,
//                                                             final int composedPart,
//                                                             final int editedPart) {
//            final StringBuilder sb = new StringBuilder();
//            int cursorStart = 0;
//            int cursorEnd = 0;
//            int compositionStart = 0;
//            int compositionEnd = 0;
//            int editStart = 0;
//            int editEnd = 0;
//            for (int i = 0; i < textParts.length; i++) {
//                if (i == selectedPart) {
//                    cursorStart = sb.length();
//                    cursorEnd = cursorStart + textParts[i].length();
//                }
//                if (i == composedPart) {
//                    compositionStart = sb.length();
//                    compositionEnd = compositionStart + textParts[i].length();
//                }
//                if (i == editedPart) {
//                    editStart = sb.length();
//                    editEnd = editStart + textParts[i].length();
//                }
//                sb.append(textParts[i]);
//            }
//            final String fullText = sb.toString();
//
//            final CharSequence editedText = "123";
//            final int insertPosition = (editStart + editEnd) / 2;
//
//            return new ExternalActionTestCase(name, settingsOption,
//                    new ComposedState(fullText, cursorStart, cursorEnd, compositionStart, compositionEnd),
//                    new ExternalActionSetText(editedText, insertPosition, insertPosition),
//                    new ComposedState(fullText.substring(0, insertPosition) + editedText + fullText.substring(insertPosition),
//                            cursorStart, cursorEnd, compositionStart, compositionEnd));
//        }
    }

    private static class DelayedUpdatesTestCase {
        private final String mTestName;
        private final VariableBehaviorSettings mSettings;
        private final State mInitialState;
        private final List<Runnable> mSteps = new ArrayList<>();
        final RichInputConnectionManager mManager = new RichInputConnectionManager();
        private int mActionIndex = 0;

        public DelayedUpdatesTestCase(String testName, VariableBehaviorSettings settings, State initialState) {
            mTestName = testName;
            mSettings = settings;
            mInitialState = initialState;
        }
        public void performInternalAction(Consumer<RichInputConnection> richInputConnectionConsumer) {
            mSteps.add(() -> {
                System.out.println("\nPerforming Action " + ++mActionIndex + " (internal)");
                richInputConnectionConsumer.accept(mManager.richInputConnection);
            });
        }
        public void performExternalAction(Consumer<FakeInputConnection> fakeInputConnectionConsumer) {
            mSteps.add(() -> {
                System.out.println("\nPerforming Action " + ++mActionIndex + " (external)");
                fakeInputConnectionConsumer.accept(mManager.fakeInputConnection);
            });
        }
        public void processNextUpdate(boolean expectUpdateForSelection, ProcessUpdateVerifier verifier) {
            mSteps.add(() -> {
                //TODO: (EW) see if there is a way to automatically identify which action the update is associated with
                System.out.println("\nSending update");

                // start tracking calls based on the updates that are about to be processed
                mManager.fakeInputConnection.resetCalls();
                mManager.allUpdatesReceivedCallCount = 0;

                // process the update
                RichInputConnectionManager.UpdateMessage message = mManager.processNextPendingMessage();
                // make sure it was the update the test was expecting
                if (expectUpdateForSelection) {
                    assertTrue(message instanceof RichInputConnectionManager.UpdateSelectionMessage);
                } else {
                    assertTrue(message instanceof RichInputConnectionManager.UpdateExtractedTextMessage);
                }

                verify(verifier);
            });
        }
        public void waitForUpdateTimer(ProcessUpdateVerifier verifier) {
            mSteps.add(() -> {
                System.out.println("\nWaiting for update timer");
                // start tracking calls based on the updates that are about to be processed
                mManager.fakeInputConnection.resetCalls();
                mManager.allUpdatesReceivedCallCount = 0;

                //TODO: (EW) trigger the timer. for now just calling the method the timer calls directly
                //TODO: (EW) handle verifying when the timer isn't running (or this direct call quits early)
                mManager.richInputConnection.checkLostUpdates();
                // faking the handler call for now too
                mManager.allUpdatesReceivedCallCount++;

                verify(verifier);
            });
        }
        private void verify(ProcessUpdateVerifier verifier) {
            if (verifier != null) {
                if (verifier.verifyUpToDate != null) {
                    assertEquals(verifier.verifyUpToDate ? 1 : 0, mManager.allUpdatesReceivedCallCount);
                }

                if (verifier.verifyStateReloaded != null) {
                    if (verifier.verifyStateReloaded) {
                        assertNotEquals("getExtractedText call count", 0, mManager.fakeInputConnection.getGetExtractedTextCalls().length);
                    } else {
                        assertEquals("getTextBeforeCursor call count", 0, mManager.fakeInputConnection.getGetTextBeforeCursorCalls().length);
                        assertEquals("getSelectedTextCalls call count", 0, mManager.fakeInputConnection.getGetSelectedTextCalls().length);
                        assertEquals("getTextAfterCursor call count", 0, mManager.fakeInputConnection.getGetTextAfterCursorCalls().length);
                        assertEquals("getExtractedText call count", 0, mManager.fakeInputConnection.getGetExtractedTextCalls().length);
                    }
                }

                //TODO: (EW) it might be better to have an option to verify a specific state in case
                // we intentionally want to verify it retaining an old state
                if (verifier.verifySelectionPositionIsCurrent != null) {
                    assertEquals("selection start",
                            mManager.fakeInputConnection.getSelectionStart(),
                            mManager.richInputConnection.getExpectedSelectionStart());
                    assertEquals("selection end",
                            mManager.fakeInputConnection.getSelectionEnd(),
                            mManager.richInputConnection.getExpectedSelectionEnd());
                }
                if (verifier.verifyCompositionPositionIsCurrent != null) {
                    CompositionState compositionState =
                            mManager.richInputConnection.getCompositionState();
                    assertNotNull("composition state is null", compositionState);
                    if (mManager.fakeInputConnection.getCompositionStart() != UNKNOWN_POSITION) {
                        assertEquals("composition start",
                                mManager.fakeInputConnection.getCompositionStart(),
                                mManager.richInputConnection.getExpectedSelectionStart() - compositionState.cursorStart);
                        assertEquals("composition end",
                                mManager.fakeInputConnection.getCompositionEnd(),
                                mManager.richInputConnection.getExpectedSelectionStart() - compositionState.cursorStart + compositionState.compositionText.length());
                    } else {
                        assertNull("composition text", compositionState.compositionText);
                    }
                }
                if (verifier.verifyUnknownComposition != null) {
                    CompositionState compositionState =
                            mManager.richInputConnection.getCompositionState();
                    assertNull("composition state", compositionState);
                }
                if (verifier.verifyUpdateApplied != null) {
                    //TODO: (EW) implement
                }
                if (verifier.verifyTextCacheIsCurrent != null) {
                    mManager.verifyTextCache(
                            mManager.fakeInputConnection.getText().substring(0,
                                    mManager.fakeInputConnection.getSelectionStart()),
                            mManager.fakeInputConnection.getText().substring(
                                    mManager.fakeInputConnection.getSelectionStart(),
                                    mManager.fakeInputConnection.getSelectionEnd()),
                            mManager.fakeInputConnection.getText().substring(
                                    mManager.fakeInputConnection.getSelectionEnd()),
                            false);
                }
            }
        }

        @Override
        public String toString() {
            return mTestName;
        }
    }

    private static class ProcessUpdateVerifier {
        Boolean verifyUpToDate = null;
        Boolean verifyStateReloaded = null;
        Boolean verifySelectionPositionIsCurrent = null;
        Boolean verifyCompositionPositionIsCurrent = null;
        Boolean verifyUnknownComposition = null;
        Boolean verifySelectionUpdated = null;
        Boolean verifyIsExpected = null;
        Boolean verifyUpdateApplied = null;
        Boolean verifyTextCacheIsCurrent = null;
        ProcessUpdateVerifier verifyUpToDate() {
            verifyUpToDate = true;
            return this;
        }
        ProcessUpdateVerifier verifyWaitingForUpdates() {
            verifyUpToDate = false;
            return this;
        }
        ProcessUpdateVerifier verifyStateReloaded(boolean reloaded) {
            verifyStateReloaded = reloaded;
            return this;
        }
        ProcessUpdateVerifier verifySelectionPositionIsCurrent() {
            verifySelectionPositionIsCurrent = true;
            return this;
        }
        ProcessUpdateVerifier verifyCompositionPositionIsCurrent() {
            verifyCompositionPositionIsCurrent = true;
            return this;
        }
        ProcessUpdateVerifier verifyUnknownComposition() {
            verifyUnknownComposition = true;
            return this;
        }
        ProcessUpdateVerifier verifyUpdateApplied(boolean applied) {
            verifyUpdateApplied = applied;
            return this;
        }
        ProcessUpdateVerifier verifyTextCacheIsCurrent() {
            verifyTextCacheIsCurrent = true;
            return this;
        }

        //TODO: (EW) probably replace these with something better
        ProcessUpdateVerifier verifySelectionUpdated(boolean selectionUpdated) {
            verifySelectionUpdated = selectionUpdated;
            return this;
        }
        ProcessUpdateVerifier verifyIsExpected(boolean isExpected) {
            verifyIsExpected = isExpected;
            return this;
        }
    }

    @RunWith(Parameterized.class)
    public static class DelayedUpdatesTest {
        protected final DelayedUpdatesTestCase testCase;

        public DelayedUpdatesTest(final DelayedUpdatesTestCase testCase) {
            this.testCase = testCase;
        }

        @Before
        public void setup() {
            testCase.mManager.setUpState(testCase.mSettings, testCase.mInitialState, true, false);
            testCase.mManager.updateSelectionCalls.clear();
            testCase.mManager.expectedUpdateSelectionCalls.clear();
            testCase.mManager.expectedUpdateExtractedTextCalls.clear();
            testCase.mManager.delayUpdates = true;
        }

        @Test
        public void testUpdatesManaged() {
            for (Runnable step : testCase.mSteps) {
                step.run();
            }
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {

            VariableBehaviorSettings settings = new VariableBehaviorSettings()
                    .updateSelectionBeforeExtractedText()
                    .blockExtractedTextMonitor();
            CommittedState initialState = new CommittedState("Lorem ipsum ", "dolor sit amet");


            DelayedUpdatesTestCase[] testCases = new DelayedUpdatesTestCase[] {
                    delayedUpdatesWithNoChange_onlyUpdateSelection(),
                    delayedUpdatesWithNoChange_updateSelectionBeforeExtractedText(),
                    delayedUpdatesWithNoChange_updateSelectionAfterExtractedText(),

                    delayedUpdatesWithModifiedInputWithOldUpdateMatchCurrentExpected_onlyUpdateSelection(),
                    delayedUpdatesWithModifiedInputWithOldUpdateNotMatchCurrentExpectedAndMultipleOutOfDateUpdates_onlyUpdateSelection(),
                    delayedUpdatesWithModifiedInputWithOldUpdateNotMatchCurrentExpectedAndMultipleOutOfDateUpdatesAndAnActionBeforeTheCurrentState_onlyUpdateSelection(),
                    delayedUpdatesWithSkippedChanges_onlyUpdateSelection(),
                    delayedUpdatesWithExternalActionBeforeImeActionsWithExternalActionMatchingImeAction_onlyUpdateSelection(),
                    delayedUpdatesWithExternalActionBeforeImeActionsWithExternalActionNotMatchingImeAction_onlyUpdateSelection(),
                    delayedUpdatesWithSkippedAndModifiedChangesWithCoincidentallyMatchingUpdates_onlyUpdateSelection(),
                    delayedUpdatesWithSkippedAndModifiedChangesWithNoMatchingUpdates_onlyUpdateSelection(),
                    delayedUpdatesWithExternalActionBeforeImeActionsAndAdditionalExternalActionDoneBeforeAllUpdatesReceived_onlyUpdateSelection(),
                    delayedUpdatesWithExternalActionBeforeImeActionsAndAdditionalExternalActionsDoneBeforeAllUpdatesReceived_onlyUpdateSelection(),
                    delayedUpdatesWithExternalActionBacktrackingCursorPositions_onlyUpdateSelection(),

                    delayedUpdatesWithTextChange_updateSelectionBeforeExtractedText()
            };

            List<Object[]> list = new ArrayList<>(testCases.length);
            for (DelayedUpdatesTestCase testCase : testCases) {
                list.add(new Object[] { testCase });
            }
            return list;
        }

        // #1 delayed updates with no change
        public static DelayedUpdatesTestCase delayedUpdatesWithNoChange_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithNoChange_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // even though this is an old update that doesn't match the current state, it is an
            // exact match with an old missing update (action 1), so it should be able to be taken
            // as the missing update and not need to do anything from the update call.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));

            // send the update for action 2
            // this is now the the update for the current state (action 2), which matches our
            // internal state and what we expected all along, so this should be expected in every
            // way.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));

            return testCase;
        }
        public static DelayedUpdatesTestCase delayedUpdatesWithNoChange_updateSelectionBeforeExtractedText() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithNoChange_updateSelectionBeforeExtractedText",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // even though this is an old update that doesn't match the current state, it is an
            // exact match with an old missing update (action 1), so it should be able to be taken
            // as the missing update and not need to do anything from the update call.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));
            // the update matches what is expected for the position, but since it's out-of-date, the
            // text can't be updated from this. the text will need to be reloaded later.
            testCase.processNextUpdate(false, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyUpdateApplied(false)
                    .verifyStateReloaded(false));

            // send the update for action 2
            // this is now the the update for the current state (action 2), which matches our
            // internal state and what we expected all along, so this should be expected in every
            // way.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));
            // the update matches what is expected for the position, and since it's the last
            // expected update, the text can be updated from this. also, since this is the last
            // update we're waiting for, it should try to reload text from the previous update that
            // got skipped due to being out-of-date.
            //TODO: (EW) should the extra update only happen when there is a composition? if the
            // update was somewhere after the cursor, this still probably wouldn't reload it.
            testCase.processNextUpdate(false, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyUpdateApplied(true)
                    .verifyTextCacheIsCurrent());

            return testCase;
        }
        public static DelayedUpdatesTestCase delayedUpdatesWithNoChange_updateSelectionAfterExtractedText() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithNoChange_updateSelectionAfterExtractedText",
                    new VariableBehaviorSettings()
                            .updateSelectionAfterExtractedText(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // the update matches what is expected for the position for an old update, but since
            // it's out-of-date, the text can't be updated from this. the text will need to be
            // reloaded later.
            testCase.processNextUpdate(false, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyUpdateApplied(false)
                    .verifyStateReloaded(false));
            // even though this is an old update that doesn't match the current state, it is an
            // exact match with an old missing update (action 1), so it should be able to be taken
            // as the missing update and not need to do anything from the update call.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));

            // send the update for action 2
            // the update matches what is expected for the position, and since it's the last
            // expected extracted text update, the text can be updated from this.
            testCase.processNextUpdate(false, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyUpdateApplied(true));
            // this is now the the update for the current state (action 2), which matches our
            // internal state and what we expected all along, so this should be expected in every
            // way. also, since this is the last update we're waiting for, it should try to reload
            // text from the previous extracted text update that got skipped due to being
            // out-of-date.
            //TODO: (EW) should the extra update only happen when there is a composition? if the
            // update was somewhere after the cursor, this still probably wouldn't reload it.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false)
                    .verifyTextCacheIsCurrent());

            return testCase;
        }

        // #2 delayed updates with modified input (incorrectly matching expected and actual)
        public static DelayedUpdatesTestCase delayedUpdatesWithModifiedInputWithOldUpdateMatchCurrentExpected_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithModifiedInputWithOldUpdateMatchCurrentExpected_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor()
                            .setTextModifier(new DoubleTextModifier()),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character (editor unexpectedly updates to 2 characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character (editor unexpectedly updates to 2 more characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // due to the modified text, this doesn't match the expected update for action 1. it
            // does happen to match the cursor position for our current expected state. at this
            // point we can't tell if the first action was skipped and this is the update for action
            // 2 (although given that it matches our current state, if the previous action was
            // skipped, it should be different, so that action must have been modified too), an
            // external action happened before our action and we're only being notified now, or the
            // actual case that the action was modified.
            // the current cursor position should be loaded to check if this is an out-of-date
            // update (still can't confirm an update is up-to-date since an old update still may
            // happen to have the same cursor position as the current state).
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // we should be able to see that the last update was unexpected and we updated to this
            // state at that update (since it was out-of-date). we haven't done any actions since
            // the last update, so at least in some sense this must be an unexpected update. if we
            // didn't have a composition, an argument could be made that this isn't unexpected since
            // we already figured out this was the state, but our composition state is unknown, so
            // this still is new info about that. if we can validate that the selection position is
            // still current and matches this, there is a good chance that this update is
            // up-to-date, so it should be safe to take the composition position to update internal
            // state. even if this was technically an out-of-date update, there would be additional
            // updates to come that will eventually put the state in the right position, which
            // should be good enough.
            //TODO: (EW) duplicate this test case but commit the text instead of composing to see
            // what the expected should be.
            //TODO: (EW) duplicate this test case but add additional updates to make this one
            // technically out-of-date to make sure taking the update and eventually updating the
            // state to be correct is fine. it might also need to try performing an action while
            // we're in an out-of-date state (maybe this should be an additional additional test
            // case).
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #3 delayed updates with modified input (incorrectly matching expected and actual) and multiple out-of-date updates
        public static DelayedUpdatesTestCase delayedUpdatesWithModifiedInputWithOldUpdateNotMatchCurrentExpectedAndMultipleOutOfDateUpdates_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithModifiedInputWithOldUpdateNotMatchCurrentExpectedAndMultipleOutOfDateUpdates_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor()
                            .setTextModifier(new DoubleTextModifier()),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character (editor unexpectedly updates to 2 characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character (editor unexpectedly updates to 2 more characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // action 3 - compose 1 more character (editor unexpectedly updates to 2 more characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("abc", 1));

            // send the update for action 1
            // due to the modified text, this doesn't match the expected update for action 1.
            // similar to test #2 we can't tell what specific sort of action occurred to trigger
            // this update, but in any case, this is an unexpected update. the current cursor
            // position should be loaded to check if this is an out-of-date update.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // we should be able to see that the last update was unexpected and we updated to this
            // state at that update (since it was out-of-date). we haven't done any actions since
            // the last update. we should reload the cursor position to verify if this is
            // up-to-date. from that, we can see that this is out-of-date and we already have the
            // correct state, so we can essentially ignore this update (this update didn't change
            // any internal state either from a relevant update or simply as a trigger to check the
            // current state).
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 3
            // we should be able to see that the last update was out-of-date and had no impact an we
            // previously updated to this state from another out-of-date update and we haven't done
            // any actions since. reloading the cursor position should indicate this is probably an
            // up-to-date update, so we should be able to update the internal composition state (see
            // test #2)
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #4 delayed updates with modified input (incorrectly matching expected and actual) and multiple out-of-date updates with an action before the current update
        public static DelayedUpdatesTestCase delayedUpdatesWithModifiedInputWithOldUpdateNotMatchCurrentExpectedAndMultipleOutOfDateUpdatesAndAnActionBeforeTheCurrentState_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithModifiedInputWithOldUpdateNotMatchCurrentExpectedAndMultipleOutOfDateUpdatesAndAnActionBeforeTheCurrentState_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor()
                            .setTextModifier(new DoubleTextModifier()),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character (editor unexpectedly updates to 2 characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character (editor unexpectedly updates to 2 more characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // action 3 - compose 1 more character (editor unexpectedly updates to 2 more characters)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("abc", 1));

            // send the update for action 1
            // same as test #3
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // same as test #3
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // action 4 - compose 1 more character (using the last entered composition since
            // RichInputConnection lost track of where the composition is) (editor unexpectedly
            // updates to 2 more characters)
            //TODO: (EW) is this an appropriate test? if we don't know if there even is a
            // composition now, should continue building off of the last composition? if that got
            // committed, this could add duplicate text. it might be safer to end the composition
            // and start composing starting here. it would be clunky having only part of the word
            // composed, but that may be better than duplicate text. also, in the case of an
            // external action moving the cursor, normally that would end the composition, and the
            // user would expect to start composing where the cursor is, not back where the current
            // composition is, so we would need to and the composition and start again.
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("abcd", 1));

            // send the update for action 3
            //TODO: (EW) maybe if we flag that previous update as out-of-date, we could ignore this
            // update as we expected something more to come. still, we got here from out-of-date and
            // unexpected changes, so it might be worth going the simpler, greedy route to update
            // here. if it wasn't for the modified last action, we might at least be able to retain
            // the composition position and just ignore this update
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 3
            // see action 2 update from test #2
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #5 delayed updates with skipped changes
        public static DelayedUpdatesTestCase delayedUpdatesWithSkippedChanges_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithSkippedChanges_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor()
                            .setTextModifier(new BlockCharactersTextModifier(new char[] {'a'})),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character (editor unexpectedly skips)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character (editor unexpectedly skips the first char again)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 2 (action 1 had no effect, so it doesn't send an update)
            // this is an

            // even though this is an old update that doesn't match the current state, it is an
            // exact match with an old missing update (action 1), so it should be able to be taken
            // as the missing update and not need to do anything from the update call.

            // this is an exact match with action 1 that we don't have an update for yet, but this
            // is technically incorrect since that change was skipped, but there isn't really a way
            // to tell at this point. now it will just look like the second update was skipped.
            //TODO: (EW) should we really have an assert for something technically incorrect?
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));

            //TODO: (EW) we'll need some timer to reload the selection if we don't get the next
            // update soon (since it won't ever come in). that should be able to see that the
            // current state matches the last update, which means the composition position from that
            // update is also probably correct, so we can keep that rather than set it to unknown.
            testCase.waitForUpdateTimer(new ProcessUpdateVerifier()
                    .verifyUpToDate()
//                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #6 delayed updates with an external action before IME actions (external matches IME action)
        public static DelayedUpdatesTestCase delayedUpdatesWithExternalActionBeforeImeActionsWithExternalActionMatchingImeAction_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithExternalActionBeforeImeActionsWithExternalActionMatchingImeAction_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 1,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 1));

            // action 2 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 3 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // the external action shifted the cursor position the same as the first action, but it
            // doesn't have a composition, so this update doesn't quite match the update we
            // expected. we should reload the cursor position to verify if this is up-to-date, and
            // seeing that it isn't and the actual cursor position doesn't match our expected one,
            // our internal composition state should become unknown.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // this doesn't match our expected state, which we should know came from checking during
            // the previous unexpected update, and due to that unexpected update, there isn't a
            // point in checking the older updates we expected since the unexpected change should
            // have changed those actions at least slightly. we should reload the cursor position to
            // verify if this is up-to-date, which will find that the update isn't, but our current
            // cursor position (updated during a previous update) is still correct, so this update
            // should be ignored.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 3
            // we should be able to see that the last update was out-of-date and had no impact an we
            // previously updated to this state from another out-of-date update and we haven't done
            // any actions since. reloading the cursor position should indicate this is probably an
            // up-to-date update, so we should be able to update the internal composition state (see
            // test #2 and #5)
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #7 delayed updates with an external action before IME actions (external doesn't match IME action)
        public static DelayedUpdatesTestCase delayedUpdatesWithExternalActionBeforeImeActionsWithExternalActionNotMatchingImeAction_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithExternalActionBeforeImeActionsWithExternalActionNotMatchingImeAction_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 2,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 2));

            // action 2 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 3 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // the external action's cursor position the same as our current expected state, but it
            // doesn't have a composition and we're expecting a different update first. that action
            // could have been skipped, which would affect subsequent actions, so we shouldn't
            // expect them to match what we originally thought they would be, or the action could be
            // modified and this is just the update for that, but in either case this is unexpected.
            // there is a small chance that the previous action's update was skipped without
            // actually skipping the action, but that's would just be the editor misbehaving, which
            // probably isn't worth trying to handle well, so this should be considered unexpected.
            // we should reload the cursor position to verify if this is up-to-date, and seeing that
            // it isn't and the actual cursor position doesn't match our expected one, our internal
            // composition state should become unknown.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // this doesn't match our expected state, which we should know came from checking during
            // the previous unexpected update, and due to that unexpected update, there isn't a
            // point in checking the older updates we expected since the unexpected change should
            // have changed those actions at least slightly. we should reload the cursor position to
            // verify if this is up-to-date, which will find that the update isn't, but our current
            // cursor position (updated during a previous update) is still correct, so this update
            // should be ignored.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 3
            // we should be able to see that the last update was out-of-date and had no impact an we
            // previously updated to this state from another out-of-date update and we haven't done
            // any actions since. reloading the cursor position should indicate this is probably an
            // up-to-date update, so we should be able to update the internal composition state (see
            // test #2 and #5)
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #8 delayed updates with skipped and modified changes (incorrectly matching expected and actual)
        public static DelayedUpdatesTestCase delayedUpdatesWithSkippedAndModifiedChangesWithCoincidentallyMatchingUpdates_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithSkippedAndModifiedChangesWithCoincidentallyMatchingUpdates_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor()
                            .setTextModifiers(new TextModifier[] {
                                    new BlockCharactersTextModifier(new char[] {'a'}),
                                    new DoubleTextModifier()
                            }),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character (editor unexpectedly skips)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character (editor unexpectedly skips the first char and doubles the second char)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 2 (action 1 had no effect, so it doesn't send an update)
            // the update matches our current expected state, but we were expecting a different
            // update first. that action could have been skipped, which would affect subsequent
            // actions, so we shouldn't expect them to match what we originally thought they would
            // be, or the action could be modified and this is just the update for that, but in
            // either case this is at least sort of unexpected. we should reload the cursor position
            // to verify if this is up-to-date, and since it is (and the expected composition and
            // composition update match) we can keep the composition state.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            //TODO: (EW) although it doesn't really matter in this case since we already updated to
            // the correct state, we still might want to trigger the timer for a complete test since
            // there was a missing update (maybe since we already found an up-to-date update that
            // isn't necessary).
            testCase.waitForUpdateTimer(new ProcessUpdateVerifier()
                    .verifyUpToDate()
                    .verifyStateReloaded(false)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #9 delayed updates with skipped and modified changes (no matching)
        public static DelayedUpdatesTestCase delayedUpdatesWithSkippedAndModifiedChangesWithNoMatchingUpdates_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithSkippedAndModifiedChangesWithNoMatchingUpdates_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor()
                            .setTextModifiers(new TextModifier[] {
                                    new BlockCharactersTextModifier(new char[] {'a'}),
                                    new RepeatTextModifier(3)
                            }),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - compose 1 character (editor unexpectedly skips)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 2 - compose 1 more character (editor unexpectedly skips the first char and doubles the second char)
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 2 (action 1 had no effect, so it doesn't send an update)
            // the update doesn't match the update we were expecting or our current expected state.
            // the first action could have been skipped, which would affect subsequent
            // actions, so it wouldn't be surprising that this subsequent update is different than
            // we originally thought it would be, or the first action could be modified and this is
            // just the update for that, but in either case this is unexpected. we should reload the
            // cursor position to verify if this is up-to-date, and since it is, it should be
            // reasonably safe to use this update's composition state for our internal state. if
            // this was actually out-of-date, we should get additional updates that eventually put
            // us in the right state.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            //TODO: (EW) although it might not really matter in this case since we this update seems
            // to be up-to-date, we still might want to trigger the timer for a complete test since
            // there was a missing update (maybe since we already found an up-to-date update that
            // isn't necessary).
            testCase.waitForUpdateTimer(new ProcessUpdateVerifier()
                    .verifyUpToDate()
                    .verifyStateReloaded(false)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #10 delayed updates with an external action before IME actions and additional external action is done before all updates are received
        public static DelayedUpdatesTestCase delayedUpdatesWithExternalActionBeforeImeActionsAndAdditionalExternalActionDoneBeforeAllUpdatesReceived_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithExternalActionBeforeImeActionsAndAdditionalExternalActionDoneBeforeAllUpdatesReceived_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 2,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 2));

            // action 2 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 3 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // the external action's cursor position the same as our current expected state, but it
            // doesn't have a composition and we're expecting a different update first. that action
            // could have been skipped, which would affect subsequent actions, so we shouldn't
            // expect them to match what we originally thought they would be, or the action could be
            // modified and this is just the update for that, but in either case this is unexpected.
            // there is a small chance that the previous action's update was skipped without
            // actually skipping the action, but that's would just be the editor misbehaving, which
            // probably isn't worth trying to handle well, so this should be considered unexpected.
            // we should reload the cursor position to verify if this is up-to-date, and seeing that
            // it isn't and the actual cursor position doesn't match our expected one, our internal
            // composition state should become unknown.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // this doesn't match our expected state, which we should know came from checking during
            // the previous unexpected update, and due to that unexpected update, there isn't a
            // point in checking the older updates we expected since the unexpected change should
            // have changed those actions at least slightly. we should reload the cursor position to
            // verify if this is up-to-date, which will find that the update isn't, but our current
            // cursor position (updated during a previous update) is still correct, so this update
            // should be ignored.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // action 4 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 2,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 2));

            // send the update for action 3
            //

            // we should be able to see that the last update was out-of-date and had no impact and
            // we previously updated to this state from another out-of-date update and we haven't
            // done any actions since. reloading the cursor position should indicate this is an
            // out-to-date update.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 4
            // we should be able to see that the last update was unexpected and out-of-date but we
            // did update to the current state at that point. reloading the cursor position should
            // indicate this is probably an up-to-date update that also matches our expected state,
            // so we can update the composition to match this update.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #11 delayed updates with an external action before IME actions and additional external actions are done before all updates are received
        public static DelayedUpdatesTestCase delayedUpdatesWithExternalActionBeforeImeActionsAndAdditionalExternalActionsDoneBeforeAllUpdatesReceived_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithExternalActionBeforeImeActionsAndAdditionalExternalActionsDoneBeforeAllUpdatesReceived_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 2,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 2));

            // action 2 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 3 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // send the update for action 1
            // the external action's cursor position the same as our current expected state, but it
            // doesn't have a composition and we're expecting a different update first. that action
            // could have been skipped, which would affect subsequent actions, so we shouldn't
            // expect them to match what we originally thought they would be, or the action could be
            // modified and this is just the update for that, but in either case this is unexpected.
            // there is a small chance that the previous action's update was skipped without
            // actually skipping the action, but that's would just be the editor misbehaving, which
            // probably isn't worth trying to handle well, so this should be considered unexpected.
            // we should reload the cursor position to verify if this is up-to-date, and seeing that
            // it isn't and the actual cursor position doesn't match our expected one, our internal
            // composition state should become unknown.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // this doesn't match our expected state, which we should know came from checking during
            // the previous unexpected update, and due to that unexpected update, there isn't a
            // point in checking the older updates we expected since the unexpected change should
            // have changed those actions at least slightly. we should reload the cursor position to
            // verify if this is up-to-date, which will find that the update isn't, but our current
            // cursor position (updated during a previous update) is still correct, so this update
            // should be ignored.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // action 4 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 2,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 2));

            // action 5 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 2,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 2));

            // send the update for action 3
            // we should be able to see that the last update was out-of-date and had no impact and
            // we previously updated to this state from another out-of-date update and we haven't
            // done any actions since. reloading the cursor position should indicate this is an
            // out-to-date update, but the current state didn't match our expected state.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 4
            // we should be able to see that the last update was out-of-date and had no impact and
            // we previously updated to this state from another out-of-date update and we haven't
            // done any actions since. reloading the cursor position should indicate this is an
            // out-to-date update but we already updated to the current position.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 5
            // we should be able to see that the last update was unexpected and out-of-date but we
            // (previously) did update to the current state. reloading the cursor position should
            // indicate this is probably an up-to-date update that also matches our expected state,
            // so we can update the composition to match this update.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #12 delayed updates with an external actions backtracking cursor positions
        public static DelayedUpdatesTestCase delayedUpdatesWithExternalActionBacktrackingCursorPositions_onlyUpdateSelection() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithExternalActionBacktrackingCursorPositions_onlyUpdateSelection",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .blockExtractedTextMonitor(),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - external cursor move
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() + 4,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() + 4));

            // action 2 - compose 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("a", 1));

            // action 3 - compose 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.setComposingText("ab", 1));

            // action 4 - external cursor move back to same position as after action 2
            testCase.performExternalAction(fakeInputConnection -> fakeInputConnection.setSelection(
                    testCase.mManager.fakeInputConnection.getSelectionStart() - 1,
                    testCase.mManager.fakeInputConnection.getSelectionEnd() - 1));

            // send the update for action 1
            // this action is from an external source and doesn't match the update we expect, but it
            // isn't up-to-date.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyUnknownComposition());

            // send the update for action 2
            // this action is from the IME, but it won't look the same as what we expected because
            // of the unexpected action before it that sent the update late. this update is
            // technically out-of-date but due to the last action changing, based on the cursor
            // position, this looks up-to-date as far as we can tell..
            //TODO: (EW) should we really have an assert for updating the composition when that is technically incorrect?
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent());
            //TODO: (EW) this will update to an incorrect composition position due to appearing
            // up-to-date. is there anything we can do about it? it would be better to leave it as
            // unknown, but to do that, I think we would never be able to update the composition
            // from this call.
            // skipping the assert for now because it seems weird to assert that it's in the wrong
            // position.
            //TODO: (EW) consider duplicating this test and adding an action at this point. if we're
            // working off of an out-of-date composition, composing something based off of the
            // existing composition would give unexpected results to the user, but I'm not sure how
            // to handle this.

            // send the update for action 3
            // this action is from the IME, but it won't look the same as what we expected because
            // of the unexpected action before it that sent the update late. reloading the cursor
            // position should indicate this is an out-to-date update but we already updated to the
            // current position.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent());
            //TODO: (EW) skipping the assert for now (see above)

            // send the update for action 4
            // we should be able to see that the last update was unexpected and out-of-date but we
            // (previously) did update to the current state. reloading the cursor position should
            // indicate this is probably an up-to-date update that also matches our expected
            // selection state, so we can update the composition to match this update.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(true)
                    .verifyIsExpected(false)
                    .verifyStateReloaded(true)
                    .verifySelectionPositionIsCurrent()
                    .verifyCompositionPositionIsCurrent());

            return testCase;
        }

        // #1.1
        public static DelayedUpdatesTestCase delayedUpdatesWithTextChange_updateSelectionBeforeExtractedText() {
            DelayedUpdatesTestCase testCase = new DelayedUpdatesTestCase(
                    "delayedUpdatesWithTextChange_updateSelectionBeforeExtractedText",
                    new VariableBehaviorSettings()
                            .updateSelectionBeforeExtractedText()
                            .setTextModifier(new AlterSpecificTextModifier("a", "x", true)),
                    new CommittedState("Lorem ipsum ", "dolor sit amet"));

            // action 1 - commit 1 character
            testCase.performInternalAction(richInputConnection -> richInputConnection.commitText("a", 1));

            // action 2 - commit 1 more character
            testCase.performInternalAction(richInputConnection -> richInputConnection.commitText("b", 1));

            // send the update for action 1
            // even though this is an old update that doesn't match the current state, it is an
            // exact match with an old missing update (action 1), so it should be able to be taken
            // as the missing update and not need to do anything from the update call.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));
            // the update matches what is expected for the position, but since it's out-of-date, the
            // text can't be updated from this. the text will need to be reloaded later.
            testCase.processNextUpdate(false, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyUpdateApplied(false)
                    .verifyStateReloaded(false));

            // send the update for action 2
            // this is now the the update for the current state (action 2), which matches our
            // internal state and what we expected all along, so this should be expected in every
            // way.
            testCase.processNextUpdate(true, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyStateReloaded(false));
            //TODO: (EW) this probably should reload the text since it couldn't update from a previous out-of-date update
            testCase.processNextUpdate(false, new ProcessUpdateVerifier()
                    .verifySelectionUpdated(false)
                    .verifyIsExpected(true)
                    .verifyUpdateApplied(false)
                    .verifyCompositionPositionIsCurrent()
                    .verifyTextCacheIsCurrent());

            return testCase;
        }

        //TODO: (EW) add tests for when we can't get the extracted text to verify the current cursor position
        //TODO: (EW) add tests for just shifting around the composing region
        //TODO: (EW) maybe add a test for a batch that has no net selection change
        //TODO: (EW) add tests where either the selection or extracted update isn't sent due to no
        // change to make sure we handle missing updates well since we're probably not going to have
        // tracking of all of what changed (at least text changes)
        //TODO: (EW) add tests for actions between selection/extracted text updates (not both received yet)
    }

    @RunWith(Parameterized.class)
    public static class GetTextAroundCursorTest {
//        private static class GetTextAroundCursorTestCase {
//            final String testBaseName;
//            final String testNameExtras;
//            final VariableBehaviorSettings settings;
//            final boolean cursorPositionKnown;
//            final State initialState;
//            final State updatedState;
//            public GetTextAroundCursorTestCase(String testBaseName, String testNameExtras,
//                                               VariableBehaviorSettings settings,
//                                               boolean cursorPositionKnown, State initialState,
//                                               State updatedState) {
//                this.testBaseName = testBaseName;
//                this.testNameExtras = testNameExtras;
//                this.settings = settings;
//                this.cursorPositionKnown = cursorPositionKnown;
//                this.initialState = initialState;
//                this.updatedState = updatedState;
//            }
//        }
        private final GetTextAroundCursorTestCase testCase;
        private final RichInputConnectionManager manager;

        public GetTextAroundCursorTest(final GetTextAroundCursorTestCase testCase) {
            this.testCase = testCase;
            manager = new RichInputConnectionManager();
        }

        @Before
        public void setup() {
            System.out.println(testCase);
            manager.runGetTextAroundCursorSetup(testCase);
        }

        @Test
        public void testResult() {
            LoadAndValidateCacheResult result = manager.richInputConnection.getTextAroundCursor(
                    testCase.start, testCase.end, 0, testCase.useCache, false, false, false);
            assertEquals("result text", testCase.expectedText,
                    result.requestedText != null ? result.requestedText.toString() : null);
            assertEquals("result update flags", testCase.expectedUpdateFlags, result.updateFlags);
        }

        @Test
        public void testUpdatedCache() {
            manager.richInputConnection.getTextAroundCursor(
                    testCase.start, testCase.end, 0, testCase.useCache, false, false, false);

            manager.verifyTextCache(testCase.expectedCachedTextBefore,
                    testCase.expectedCachedSelection, testCase.expectedCachedTextAfter, true);
        }

        @Test
        public void testSingleTextLoadCalls() {
            manager.fakeInputConnection.resetCalls();
            manager.richInputConnection.getTextAroundCursor(
                    testCase.start, testCase.end, 0, testCase.useCache, false, false, false);
            assertFalse("multiple getExtractedText calls",
                    manager.fakeInputConnection.getGetExtractedTextCalls().length > 1);
            assertFalse("multiple getTextBeforeCursor calls",
                    manager.fakeInputConnection.getGetTextBeforeCursorCalls().length > 1);
            assertFalse("multiple getSelectedText calls",
                    manager.fakeInputConnection.getGetSelectedTextCalls().length > 1);
            assertFalse("multiple getTextAfterCursorCalls calls",
                    manager.fakeInputConnection.getGetTextAfterCursorCalls().length > 1);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
//            final GroupedList<ExternalActionTestCase> list = new GroupedList<>();
            final SplitText initialText = new SplitText("Lorem ipsum dolor sit amet");
            final SplitText updatedText = new SplitText("abcdefghijklmnopqrstuvwxyz");
            final int initialTriLeftLeftLength = initialText.triLeft.length() / 2;
            final int initialTriLeftRightLength =
                    initialText.triLeft.length() - initialTriLeftLeftLength;

            //TODO: (EW) are these necessary? - created on accident - copy over to new structure or delete
            GetTextAroundCursorTestCase[] testCases = new GetTextAroundCursorTestCase[]{
                    new GetTextAroundCursorTestCase(
                            "partially cached text with changes everywhere - cached load requires fresh load and finds full updated text",
                            initialText.triLeft, initialText.triCenter, initialText.triRight,
                            new Range[]{
                                    new Range(initialTriLeftLeftLength + 1,
                                            initialText.triLeft.length()
                                                    + initialText.triCenter.length() / 2),
                                    new Range(initialText.triLeft.length()
                                            + initialText.triCenter.length(),
                                            initialText.triLeft.length()
                                                    + initialText.triCenter.length()
                                                    + initialText.triRight.length() / 2)
                            },
                            updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                            -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                            updatedText.full,
                            TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | TEXT_CORRECTED | FULL_REQUEST_COMPLETED,
                            updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                            new VariableBehaviorSettings()
                                    .blockBaseExtractText(),
                            true),
                    new GetTextAroundCursorTestCase(
                            "partially cached text with changes everywhere and limited text return - cached load requires fresh load and finds incomplete text",
                            initialText.triLeft, initialText.triCenter, initialText.triRight,
                            new Range[]{
                                    new Range(Math.max(0, initialText.triLeft.length() - 3),
                                            initialText.triLeft.length()
                                                    + initialText.triCenter.length()
                                                    + Math.min(3, initialText.triRight.length()))
                            },
                            updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                            -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                            null,
                            TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | TEXT_CORRECTED,
                            updatedText.triLeft.substring(
                                    Math.max(0, updatedText.triLeft.length() - 5)),
                            updatedText.triCenter,
                            updatedText.triRight.substring(0,
                                    Math.min(5, updatedText.triRight.length())),
                            new VariableBehaviorSettings()
                                    .blockBaseExtractText()
                                    .limitReturnedText(5, false, 5),
                            true),

            };

            VariableBehaviorSettings[] settingsBuilders =
                    new VariableBehaviorSettings[] {
                            new VariableBehaviorSettings(),
                            new VariableBehaviorSettings()
                                    .blockBaseExtractText()
                    };
            List<GetTextAroundCursorTestCase> testCases2 = new ArrayList<>();
            for (VariableBehaviorSettings settingsBuilder : settingsBuilders) {
                //full text cached with changes everywhere
                //  reload cache indicates changes
                //  cached request gets old text and indicates no update
                //partial text cached with no changes and can load all
                //  either request indicates update
                //partial text cached with no changes and can't load all
                //  either request indicates incomplete request and still caches what it gets
                //missing text in/after selection with changes everywhere
                //  either request indicates changes everywhere

                //something with
                //  unknown cursor
                //  changing the cursor position


                // full text cached with changes everywhere
                testCases2.add(new GetTextAroundCursorTestCase(
                        "fully cached text with changed text - fresh load finds changes",
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new Range[]{new Range(0, initialText.full.length() - 1)},
                        updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                        -initialText.triLeft.length(), initialText.triRight.length() + 1, false,
                        updatedText.full,
                        //TODO: (EW) TEXT_REMOVED probably doesn't make sense since the whole text
                        // is actually fully corrected
                        TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | TEXT_REMOVED | TEXT_CORRECTED | FULL_REQUEST_COMPLETED,
                        updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                        settingsBuilder, true));
                testCases2.add(new GetTextAroundCursorTestCase(
                        "fully cached text with changed text - cached load returns old text",
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new Range[]{new Range(0, initialText.full.length() - 1)},
                        updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                        -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                        initialText.full,
                        FULL_REQUEST_COMPLETED,
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        settingsBuilder, true));

                //partial text cached with no changes and can load all
                testCases2.add(new GetTextAroundCursorTestCase(
                        "partially cached text with no change - cached load requires fresh load and finds full text",
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new Range[]{
                                new Range(initialTriLeftLeftLength,
                                        initialText.triLeft.length()
                                                + initialText.triCenter.length() / 2 - 1),
                                new Range(initialText.triLeft.length()
                                        + initialText.triCenter.length(),
                                        initialText.triLeft.length()
                                                + initialText.triCenter.length()
                                                + initialText.triRight.length() / 2 - 1)
                        },
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                        initialText.full,
                        TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | FULL_REQUEST_COMPLETED,
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        settingsBuilder, true));

                //partial text cached with no changes and can't load all
                testCases2.add(new GetTextAroundCursorTestCase(
                        "partially cached text with no change and limited text return - cached load requires fresh load finds incomplete text but still updates cache",
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new Range[]{
                                new Range(Math.max(0, initialText.triLeft.length() - 3),
                                        initialText.triLeft.length()
                                                + initialText.triCenter.length()
                                                + Math.min(3, initialText.triRight.length()) - 1)
                        },
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                        null,
                        TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED,
                        initialText.triLeft.substring(
                                Math.max(0, initialText.triLeft.length() - 5)),
                        initialText.triCenter,
                        initialText.triRight.substring(0,
                                Math.min(5, initialText.triRight.length())),
                        new VariableBehaviorSettings(settingsBuilder)
                                .limitReturnedText(5, false, 5),
                        true));

                //missing text in/after selection with changes everywhere
                testCases2.add(new GetTextAroundCursorTestCase(
                        "missing text after selection with changes everywhere - cached load requires fresh load and corrects full text",
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new Range[]{
                                new Range(0,
                                        initialText.triLeft.length()
                                                + initialText.triCenter.length()
                                                + (initialText.triRight.length() / 2) - 1)
                        },
                        updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                        -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                        updatedText.full,
                        //TODO: (EW) TEXT_REMOVED probably doesn't make sense since we end up fully
                        // correcting the text
                        TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | TEXT_REMOVED | TEXT_CORRECTED | FULL_REQUEST_COMPLETED,
                        updatedText.triLeft, updatedText.triCenter, updatedText.triRight,
                        settingsBuilder, true));

                //unknown cursor
                testCases2.add(new GetTextAroundCursorTestCase(
                        "partially cached text with no change and unknown cursor - cached load requires fresh load and finds full text",
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new Range[]{
                                new Range(initialTriLeftLeftLength,
                                        initialText.triLeft.length() - 1),
                        },
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                        initialText.full,
                        settingsBuilder.allowExtractingText
                                ? TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | TEXT_REMOVED | SELECTION_LOADED | SELECTION_UPDATED | FULL_REQUEST_COMPLETED
                                : TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | SELECTION_UPDATED | FULL_REQUEST_COMPLETED,
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        settingsBuilder, false));

                if (settingsBuilder.allowExtractingText) {
                    testCases2.add(new GetTextAroundCursorTestCase(
                            "partially cached text with no change, unknown cursor, and extracted text gets some far text - cached load requires some fresh load and finds full text",
                            initialText.triLeft, initialText.triCenter, initialText.triRight,
                            new Range[]{
                                    new Range(0, initialText.triLeft.length() - 1),
                            },
                            initialText.triLeft, initialText.triCenter, initialText.triRight,
                            -initialTriLeftRightLength, initialText.triRight.length() + 1, true,
                            initialText.full.substring(initialTriLeftLeftLength),
                            TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | TEXT_REMOVED | SELECTION_LOADED | SELECTION_UPDATED | FULL_REQUEST_COMPLETED,
                            initialText.triLeft.substring(initialTriLeftLeftLength),
                            initialText.triCenter, initialText.triRight,
                            new VariableBehaviorSettings(settingsBuilder)
                                    .limitReturnedText(initialTriLeftRightLength, false,0)
                                    .limitExtractedText(initialText.triRight.length()
                                                    - initialTriLeftRightLength,
                                            EXTRACT_TEXT_FROM_END),
                            false));
                    testCases2.add(new GetTextAroundCursorTestCase(
                            "text with no change, unknown cursor, extracted text gets some far text, and can't load full text - cache updated",
                            initialText.triLeft, initialText.triCenter, initialText.triRight,
                            new Range[0],
                            initialText.triLeft, initialText.triCenter, initialText.triRight,
                            -initialText.triLeft.length(), initialText.triRight.length() + 1, true,
                            null,
                            TEXT_REQUESTED | TEXT_LOADED | TEXT_UPDATED | SELECTION_LOADED | SELECTION_UPDATED,
                            initialText.triLeft.substring(initialTriLeftLeftLength),
                            initialText.triCenter, initialText.triRight,
                            new VariableBehaviorSettings(settingsBuilder)
                                    .limitReturnedText(initialTriLeftRightLength, false,0)
                                    .limitExtractedText(initialText.triRight.length()
                                                    - initialTriLeftRightLength,
                                            EXTRACT_TEXT_FROM_END),
                            false));
                }

                testCases2.add(new GetTextAroundCursorTestCase(
                        "fully cached text with limited text - fresh load fails to find full text",
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new Range[]{new Range(0, initialText.full.length() - 1)},
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        -initialText.triLeft.length(), initialText.triRight.length() + 1, false,
                        null,
                        TEXT_REQUESTED | TEXT_LOADED,
                        initialText.triLeft, initialText.triCenter, initialText.triRight,
                        new VariableBehaviorSettings(settingsBuilder)
                                .limitReturnedText(5, false,0)
                                .limitExtractedText(5, EXTRACT_TEXT_FROM_START),
                        true));
            }

            List<Object[]> list = new ArrayList<>(testCases.length);
            for (GetTextAroundCursorTestCase testCase : testCases2) {
                list.add(new Object[] { testCase });
            }
            return list;
        }
        public static class GetTextAroundCursorTestCase {
            private final String testName;
//            private final String initialTextBefore;
//            private final String initialSelection;
//            private final String initialTextAfter;
//            private final Range[] initialCachedRanges;
//            private final String updatedTextBefore;
//            private final String updatedSelection;
//            private final String updatedTextAfter;
            private final State initialState;
            private final Range[] initialCachedRanges;
            private final State updatedState;
            private final boolean cursorPositionKnown;
            private final int start;
            private final int end;
            private final boolean useCache;
            private final String expectedText;
            private final int expectedUpdateFlags;
            private final String expectedCachedTextBefore;
            private final String expectedCachedSelection;
            private final String expectedCachedTextAfter;
            private final VariableBehaviorSettings settings;
            public GetTextAroundCursorTestCase(String testName, String initialTextBefore,
                                               String initialSelection, String initialTextAfter,
                                               Range[] initialCachedRanges,
                                               String updatedTextBefore, String updatedSelection,
                                               String updatedTextAfter, //boolean cursorPositionKnown,
                                               int start, int end, boolean useCache,
                                               String expectedText, int expectedUpdateFlags,
                                               String expectedCachedTextBefore,
                                               String expectedCachedSelection,
                                               String expectedCachedTextAfter,
                                               VariableBehaviorSettings settings,
                                               final boolean cursorPositionKnown) {
                this.testName = testName;
                initialState = new CommittedState(initialTextBefore, initialSelection, initialTextAfter);
                this.initialCachedRanges = initialCachedRanges;
                updatedState = new CommittedState(updatedTextBefore, updatedSelection, updatedTextAfter);
//                this.initialTextBefore = initialTextBefore;
//                this.initialSelection = initialSelection;
//                this.initialTextAfter = initialTextAfter;
//                this.initialCachedRanges = initialCachedRanges;
//                this.updatedTextBefore = updatedTextBefore;
//                this.updatedSelection = updatedSelection;
//                this.updatedTextAfter = updatedTextAfter;
//                this.cursorPositionKnown = cursorPositionKnown;
                this.cursorPositionKnown = cursorPositionKnown;
                this.start = start;
                this.end = end;
                this.useCache = useCache;
                this.expectedText = expectedText;
                this.expectedUpdateFlags = expectedUpdateFlags;
                this.expectedCachedTextBefore = expectedCachedTextBefore;
                this.expectedCachedSelection = expectedCachedSelection;
                this.expectedCachedTextAfter = expectedCachedTextAfter;
                this.settings = settings;
            }

            @Override
            public String toString() {
                return testName;
            }
        }
    }

    public static abstract class GetTextTestBase<T extends GetTextTestCaseBase<?>> {
        protected final T testCase;
        protected final RichInputConnectionManager manager;

        public GetTextTestBase(final T testCase) {
            this.testCase = testCase;
            manager = new RichInputConnectionManager();
        }

        protected abstract CharSequence getText();

        @Before
        public void setup() {
            System.out.println(testCase);
            manager.runGetTextAroundCursorSetup(testCase);
        }

        @Test
        public void testResult() {
            CharSequence text = getText();
            //TODO: (EW) handle spans
            assertEquals("result text",
                    testCase.expectedResponseText != null ? testCase.expectedResponseText.toString() : null,
                    text != null ? text.toString() : null);
        }

        @Test
        public void testUpdatedCache() {
            getText();

            manager.verifyTextCache(testCase.expectedCachedTextBefore,
                    testCase.expectedCachedSelectedText, testCase.expectedCachedTextAfter, true);
        }
    }

    @RunWith(Parameterized.class)
    public static class GetTextBeforeCursorTest
            extends GetTextTestBase<GetTextAdjacentToCursorTestCase> {

        public GetTextBeforeCursorTest(final GetTextAdjacentToCursorTestCase testCase) {
            super(testCase);
        }

        protected CharSequence getText() {
            return manager.richInputConnection.getTextBeforeCursor(testCase.requestedLength,
                    testCase.requestedFlags);
        }

        @Test
        public void testSingleTextLoadCalls() {
            manager.fakeInputConnection.resetCalls();
            getText();
            assertFalse("multiple getExtractedText calls",
                    manager.fakeInputConnection.getGetExtractedTextCalls().length > 1);
            assertFalse("multiple getTextBeforeCursor calls",
                    manager.fakeInputConnection.getGetTextBeforeCursorCalls().length > 1);
//            assertFalse("multiple getSelectedText calls",
//                    manager.fakeInputConnection.getGetSelectedTextCalls().length > 1);
//            assertFalse("multiple getTextAfterCursorCalls calls",
//                    manager.fakeInputConnection.getGetTextAfterCursorCalls().length > 1);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final SplitTextRecursive initialText = new SplitTextRecursive("Lorem ipsum dolor sit amet");
            final SplitTextRecursive updatedText = new SplitTextRecursive("abcdefghijklmnopqrstuvwxyz");

            VariableBehaviorSettings[] settingsBuilders =
                    new VariableBehaviorSettings[] {
                            new VariableBehaviorSettings(),
                            new VariableBehaviorSettings()
                                    .blockBaseExtractText()
                    };
            List<GetTextAdjacentToCursorTestCase> testCases = new ArrayList<>();
            for (VariableBehaviorSettings settingsBuilder : settingsBuilders) {
                //TODO: (EW) add more tests (try to cover cases from GetTextAroundCursorTest to
                // hopefully be able to stop testing getTextAroundCursor directly):
                // x fully cached with changes and request styles to force no cache
                // x fully cached with changes returns old text
                // x partially cached text with no changes loads all
                // x partially cached text with limited text (either more or less that what is in the cache)
                //   not sure how to test multiple loads with finding changes only in later ones)
                //   partially cached with unknown cursor loads selection and requires fresh load (selection and after cursor only)
                //   partially cached with unknown cursor and extracted text gets far adjacent text and limited text finds full text (after cursor only)
                //   unknown cursor, extracted text gets some far text, and can't load full text still caches loaded text (selection and after cursor only)
                // x fully cached with limited text and request styles to force no cache loads limited text
                // are any of the skipped cases still relevant as a reference (and testing unknown cursors) even if they don't have a special outcome


                //partial text cached with no changes and can load all
                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with no change - requires fresh load and finds full text")
                                .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                                .setInitialCachedRanges(new Range[]{
                                        new Range(initialText.triLeft().length() / 2,
                                                initialText.triLeft().length() - 1)
                                })
                                .setCursorPositionKnown(true)
                                .setRequestedLength(initialText.triLeft().length())
                                .setExpectedResponse(initialText.triLeft())
                                .setExpectedCache(initialText.triLeft(), null, "")
                                .setSettings(settingsBuilder));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "not cached text with no change with limited text return - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{})
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triLeft().length())
                        .setExpectedResponse(initialText.triLeft().biRight())
                        .setExpectedCache(initialText.triLeft().biRight(), null, "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triLeft().biRight().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with limited text return (limit is more than cached) - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeft().triLeft().length() + initialText.triLeft().triCenter().length(),
                                        initialText.triLeft().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triLeft().length())
                        .setExpectedResponse(initialText.triLeft().triCenterRight())
                        .setExpectedCache(initialText.triLeft().triCenterRight(), null, "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triLeft().triCenterRight().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with limited text return (cached is more than limit) - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeft().triLeft().length(),
                                        initialText.triLeft().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triLeft().length())
                        .setExpectedResponse(initialText.triLeft().triCenterRight())
                        .setExpectedCache(initialText.triLeft().triCenterRight(), null, "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triLeft().triRight().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with limited text return (cached is more than limit) and text changes - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeft().triLeft().length(),
                                        initialText.triLeft().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setRequestedLength(initialText.triLeft().length())
                        .setExpectedResponse(updatedText.triLeft().triRight())
                        .setExpectedCache(updatedText.triLeft().triRight(), null, "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triLeft().triRight().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "fully cached text with changes - gets old text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setRequestedLength(initialText.triLeft().length())
                        .setExpectedResponse(initialText.triLeft())
                        .setExpectedCache(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)));

                //TODO: (EW) test with spanned text
                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "fully cached text with changes and requests styles (to force no cache) - gets full updated text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setRequestedLength(initialText.triLeft().length())
                        .setRequestedFlags(GET_TEXT_WITH_STYLES)
                        .setExpectedResponse(updatedText.triLeft())
                        .setExpectedCache(updatedText.triLeft(), null, "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "fully cached text with changes and requests styles (to force no cache) with limited text return - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triLeft().length())
                        .setRequestedFlags(GET_TEXT_WITH_STYLES)
                        .setExpectedResponse(initialText.triLeft().biRight())
                        .setExpectedCache(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triLeft().biRight().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "cached text adjacent to limited text return - gets full text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.triLeft().biLeft().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triLeft().length())
                        .setExpectedResponse(initialText.triLeft())
                        .setExpectedCache(initialText.triLeft(), null, "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triLeft().biRight().length())));
            }

            List<Object[]> list = new ArrayList<>();
            for (GetTextAdjacentToCursorTestCase testCase : testCases) {
                list.add(new Object[] { testCase });
            }
            return list;
        }
    }
    @RunWith(Parameterized.class)
    public static class GetSelectedTextTest extends GetTextTestBase<GetSelectedTextTestCase> {

        public GetSelectedTextTest(final GetSelectedTextTestCase testCase) {
            super(testCase);
        }

        protected CharSequence getText() {
            return manager.richInputConnection.getSelectedText(testCase.requestedFlags);
        }

        @Test
        public void testSingleTextLoadCalls() {
            manager.fakeInputConnection.resetCalls();
            getText();
            assertFalse("multiple getExtractedText calls",
                    manager.fakeInputConnection.getGetExtractedTextCalls().length > 1);
//            assertFalse("multiple getTextBeforeCursor calls",
//                    manager.fakeInputConnection.getGetTextBeforeCursorCalls().length > 1);
            assertFalse("multiple getSelectedText calls (" + manager.fakeInputConnection.getGetSelectedTextCalls().length + ")",
                    manager.fakeInputConnection.getGetSelectedTextCalls().length > 1);
//            assertFalse("multiple getTextAfterCursor calls (" + manager.fakeInputConnection.getGetTextAfterCursorCalls().length + ")",
//                    manager.fakeInputConnection.getGetTextAfterCursorCalls().length > 1);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final SplitTextRecursive initialText = new SplitTextRecursive("Lorem ipsum dolor sit amet");
            final SplitTextRecursive updatedText = new SplitTextRecursive("abcdefghijklmnopqrstuvwxyz");

            VariableBehaviorSettings[] settingsBuilders =
                    new VariableBehaviorSettings[] {
                            new VariableBehaviorSettings(),
                            new VariableBehaviorSettings()
                                    .blockBaseExtractText()
                    };
            List<GetSelectedTextTestCase> testCases = new ArrayList<>();
            for (VariableBehaviorSettings settingsBuilder : settingsBuilders) {

                testCases.add(new GetSelectedTextTestCase(
                        "not cached text with no change - fresh load")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setCursorPositionKnown(true)
                        .setExpectedResponse(initialText.triCenter())
                        .setExpectedCache("", initialText.triCenter(), "")
                        .setSettings(settingsBuilder));

                testCases.add(new GetSelectedTextTestCase(
                        "partially cached cached text get selected text not supported - returns null")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeft().length(),
                                        initialText.triLeft().length() + (initialText.triCenter().length() / 2) - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setExpectedResponse(null)
                        .setExpectedCache("", null, "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .getSelectedTextNotSupported()));

                testCases.add(new GetSelectedTextTestCase(
                        "fully cached text with changes - gets old text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setExpectedResponse(initialText.triCenter())
                        .setExpectedCache(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)));

                //TODO: (EW) test with spanned text
                testCases.add(new GetSelectedTextTestCase(
                        "fully cached text with changes and requests styles (to force no cache) - gets full updated text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setRequestedFlags(GET_TEXT_WITH_STYLES)
                        .setExpectedResponse(updatedText.triCenter())
                        .setExpectedCache("", updatedText.triCenter(), "")
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)));

                testCases.add(new GetSelectedTextTestCase(
                        "partially cached with unknown cursor - loads selection and requires fresh load")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.triLeft().length() - 1)
                        })
                        .setCursorPositionKnown(false)
                        .setExpectedResponse(initialText.triCenter())
                        .setExpectedCache(initialText.triLeft(),
                                initialText.triCenter(),
                                settingsBuilder.allowExtractingText ? initialText.triRight() : "")
                        .setSettings(settingsBuilder.allowExtractingText
                                ? new VariableBehaviorSettings(settingsBuilder).getSelectedTextNotSupported()
                                : settingsBuilder));

                if (settingsBuilder.allowExtractingText) {
                    testCases.add(new GetSelectedTextTestCase(
                            "unknown cursor and extracted text gets other text and get selected text not supported - still caches loaded text")
                            .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                            .setCursorPositionKnown(false)
                            .setExpectedResponse(null)
                            .setExpectedCache("", null, initialText.triRight())
                            .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                    .limitExtractedText(initialText.triRight().length(), EXTRACT_TEXT_FROM_END)
                                    .getSelectedTextNotSupported()));
                }

                //TODO: (EW) consider adding tests for no selection
            }

            List<Object[]> list = new ArrayList<>();
            for (GetSelectedTextTestCase testCase : testCases) {
                list.add(new Object[] { testCase });
            }
            return list;
        }
    }
    @RunWith(Parameterized.class)
    public static class GetTextAfterCursorTest extends GetTextTestBase<GetTextAdjacentToCursorTestCase> {

        public GetTextAfterCursorTest(final GetTextAdjacentToCursorTestCase testCase) {
            super(testCase);
        }

        protected CharSequence getText() {
            return manager.richInputConnection.getTextAfterCursor(testCase.requestedLength,
                    testCase.requestedFlags);
        }

        @Test
        public void testSingleTextLoadCalls() {
            manager.fakeInputConnection.resetCalls();
            getText();
            assertFalse("multiple getExtractedText calls",
                    manager.fakeInputConnection.getGetExtractedTextCalls().length > 1);
//            assertFalse("multiple getTextBeforeCursor calls",
//                    manager.fakeInputConnection.getGetTextBeforeCursorCalls().length > 1);
            assertFalse("multiple getSelectedText calls (" + manager.fakeInputConnection.getGetSelectedTextCalls().length + ")",
                    manager.fakeInputConnection.getGetSelectedTextCalls().length > 1);
            assertFalse("multiple getTextAfterCursor calls (" + manager.fakeInputConnection.getGetTextAfterCursorCalls().length + ")",
                    manager.fakeInputConnection.getGetTextAfterCursorCalls().length > 1);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final SplitTextRecursive initialText = new SplitTextRecursive("Lorem ipsum dolor sit amet");
            final SplitTextRecursive updatedText = new SplitTextRecursive("abcdefghijklmnopqrstuvwxyz");

            VariableBehaviorSettings[] settingsBuilders =
                    new VariableBehaviorSettings[] {
                            new VariableBehaviorSettings(),
                            new VariableBehaviorSettings()
                                    .blockBaseExtractText()
                    };
            List<GetTextAdjacentToCursorTestCase> testCases = new ArrayList<>();
            for (VariableBehaviorSettings settingsBuilder : settingsBuilders) {

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with no change - requires fresh load and finds full text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeftCenter().length(),
                                        initialText.triLeftCenter().length() + initialText.triRight().length() / 2 - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(initialText.triRight())
                        .setExpectedCache("", null, initialText.triRight())
                        .setSettings(settingsBuilder));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "not cached text with no change with limited text return - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{})
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(initialText.triRight().biLeft())
                        .setExpectedCache("", null, initialText.triRight().biLeft())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triRight().biLeft().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with limited text return (limit is more than cached) - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeftCenter().length(),
                                        initialText.triLeftCenter().length() + initialText.triRight().triLeft().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(initialText.triRight().triLeftCenter())
                        .setExpectedCache("", null, initialText.triRight().triLeftCenter())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triRight().triLeftCenter().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with limited text return (cached is more than limit) - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeftCenter().length(),
                                        initialText.triLeftCenter().length() + initialText.triRight().triLeftCenter().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(initialText.triRight().triLeftCenter())
                        .setExpectedCache("", null, initialText.triRight().triLeftCenter())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triRight().triLeft().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached text with limited text return (cached is more than limit) and text changes - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.triLeftCenter().length(),
                                        initialText.triLeftCenter().length() + initialText.triRight().triLeftCenter().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(updatedText.triRight().triLeft())
                        .setExpectedCache("", null, updatedText.triRight().triLeft())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triRight().triLeft().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "fully cached text with changes - gets old text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(initialText.triRight())
                        .setExpectedCache(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)));

                //TODO: (EW) test with spanned text
                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "fully cached text with changes and requests styles (to force no cache) - gets full updated text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedText(updatedText.triLeft(), updatedText.triCenter(), updatedText.triRight())
                        .setRequestedLength(initialText.triRight().length())
                        .setRequestedFlags(GET_TEXT_WITH_STYLES)
                        .setExpectedResponse(updatedText.triRight())
                        .setExpectedCache("", null, updatedText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "fully cached text with changes and requests styles (to force no cache) with limited text return - gets less text than requested")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triRight().length())
                        .setRequestedFlags(GET_TEXT_WITH_STYLES)
                        .setExpectedResponse(initialText.triRight().biLeft())
                        .setExpectedCache(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triRight().biLeft().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "cached text adjacent to limited text return - gets full text")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(initialText.length() - initialText.triRight().biRight().length(), initialText.length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(initialText.triRight())
                        .setExpectedCache("", null, initialText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .setGetTextLimit(initialText.triRight().biLeft().length())));

                testCases.add(new GetTextAdjacentToCursorTestCase(
                        "partially cached with unknown cursor - loads selection and requires fresh load")
                        .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setInitialCachedRanges(new Range[]{
                                new Range(0, initialText.triLeft().length() - 1)
                        })
                        .setCursorPositionKnown(false)
                        .setRequestedLength(initialText.triRight().length())
                        .setExpectedResponse(initialText.triRight())
                        .setExpectedCache(settingsBuilder.allowExtractingText ? "" : initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .limitExtractedText(0, EXTRACT_TEXT_CENTERED_ON_SELECTION)));

                if (settingsBuilder.allowExtractingText) {
                    testCases.add(new GetTextAdjacentToCursorTestCase(
                            "unknown cursor and extracted text gets far adjacent text and limited text - finds full text")
                            .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                            .setCursorPositionKnown(false)
                            .setRequestedLength(initialText.triRight().length())
                            .setExpectedResponse(initialText.triRight())
                            .setExpectedCache("", null, initialText.triRight())
                            .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                    .setGetTextLimit(initialText.triRight().biLeft().length())
                                    .limitExtractedText(initialText.triRight().biRight().length(), EXTRACT_TEXT_FROM_END)));

                    testCases.add(new GetTextAdjacentToCursorTestCase(
                            "unknown cursor and extracted text gets far other text and limited text - still caches loaded text")
                            .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                            .setCursorPositionKnown(false)
                            .setRequestedLength(initialText.triRight().length())
                            .setExpectedResponse(initialText.triRight().biLeft())
                            .setExpectedCache(initialText.triLeft(), null, initialText.triRight().biLeft())
                            .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                    .setGetTextLimit(initialText.triRight().biLeft().length())
                                    .limitExtractedText(initialText.triLeft().length(), EXTRACT_TEXT_FROM_START)));
                } else  {
                    testCases.add(new GetTextAdjacentToCursorTestCase(
                            "partially cached with unknown cursor and get selection and extract text not supported - requires fresh load")
                            .setInitialText(initialText.triLeft(), initialText.triCenter(), initialText.triRight())
                            .setInitialCachedRanges(new Range[]{
                                    new Range(0, initialText.triLeft().length() - 1)
                            })
                            .setCursorPositionKnown(false)
                            .setRequestedLength(initialText.triRight().length())
                            .setExpectedResponse(initialText.triRight())
                            .setExpectedCache(initialText.triLeft(), null, "")
                            .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                    .getSelectedTextNotSupported()));
                }
            }

            List<Object[]> list = new ArrayList<>();
            for (GetTextAdjacentToCursorTestCase testCase : testCases) {
                list.add(new Object[] { testCase });
            }
            return list;
        }
    }


    @RunWith(Parameterized.class)
    public static class GetComposedTextTest extends GetTextTestBase<GetComposedTextTestCase> {

        public GetComposedTextTest(final GetComposedTextTestCase testCase) {
            super(testCase);
        }

        protected CharSequence getText() {
            //TODO: (EW) should this also test the cursor positions? verifyCompositionCache doesn't
            // either. one, if not both, should. it might be fine for this to skip it for testing
            // just the text (specifically intending to just indirectly test getTextAroundCursor
            // since this is really just messing with what text is cached or accessible for calling)
            // as long as it's tested with the normal action test cases, which might test it more
            // thoroughly.
            CompositionState state = manager.richInputConnection.getCompositionState();
            return state == null ? null : state.compositionText;
        }

        @Test
        public void testSingleTextLoadCalls() {
            manager.fakeInputConnection.resetCalls();
            getText();
            int extractedTextCallCount = manager.fakeInputConnection.getGetExtractedTextCalls().length;
            assertFalse("multiple getExtractedText calls (" + extractedTextCallCount + ")",
                    extractedTextCallCount > 1);
            int textBeforeCallCount = manager.fakeInputConnection.getGetTextBeforeCursorCalls().length;
            assertFalse("multiple getTextBeforeCursor calls (" + textBeforeCallCount + ")",
                    textBeforeCallCount > 1);
            int selectedTextCallCount = manager.fakeInputConnection.getGetSelectedTextCalls().length;
            assertFalse("multiple getSelectedText calls (" + selectedTextCallCount + ")",
                    selectedTextCallCount > 1);
            int textAfterCallCount = manager.fakeInputConnection.getGetTextAfterCursorCalls().length;
            assertFalse("multiple getTextAfterCursor calls (" + textAfterCallCount + ")",
                    textAfterCallCount > 1);
        }

        @Parameters(name = "{index}: {0}")
        public static Collection<Object[]> parameters() {
            final SplitTextRecursive initialText = new SplitTextRecursive("Lorem ipsum dolor sit amet");
            final SplitTextRecursive updatedText = new SplitTextRecursive("abcdefghijklmnopqrstuvwxyz");

            ComposedState selectionWellBeforeCompositionState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeft().triLeft().length(),
                    initialText.triLeft().triLeftCenter().length());
            ComposedState selectionRightBeforeCompositionState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeft().biLeft().length(),
                    initialText.triLeft().length());
            ComposedState selectionInCompositionInitialState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeft().length() + initialText.triCenter().triLeft().length(),
                    initialText.triLeft().length() + initialText.triCenter().triLeftCenter().length());
            ComposedState selectionRightAfterCompositionState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeftCenter().length(),
                    initialText.triLeftCenter().length() + initialText.triRight().biLeft().length());
            ComposedState selectionWellAfterCompositionState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeftCenter().length() + initialText.triRight().triLeft().length(),
                    initialText.triLeftCenter().length() + initialText.triRight().triLeftCenter().length());
            ComposedState selectionThroughCompositionStartState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeft().biLeft().length(),
                    initialText.triLeft().length() + initialText.triCenter().biLeft().length());
            ComposedState selectionThroughCompositionEndState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeft().length() + initialText.triCenter().biLeft().length(),
                    initialText.triLeftCenter().length() + initialText.triRight().biLeft().length());
            ComposedState selectionThroughWholeCompositionState = new ComposedState(initialText.triLeft().toString(), initialText.triCenter().toString(), initialText.triRight().toString(),
                    initialText.triLeft().biLeft().length(),
                    initialText.triLeftCenter().length() + initialText.triRight().biLeft().length());
            Named<ComposedState>[] composedInitialStateOptions = new Named[] {
                    new Named<>("selection well before the composition", selectionWellBeforeCompositionState),
                    new Named<>("selection right before the composition", selectionRightBeforeCompositionState),
                    new Named<>("selection in the composition", selectionInCompositionInitialState),
                    new Named<>("selection right after the composition", selectionRightAfterCompositionState),
                    new Named<>("selection well after the composition", selectionWellAfterCompositionState),
                    new Named<>("selection through the composition start", selectionThroughCompositionStartState),
                    new Named<>("selection through the composition end", selectionThroughCompositionEndState),
                    new Named<>("selection through the whole composition", selectionThroughWholeCompositionState)
            };
            ComposedState selectionInCompositionUpdatedState = new ComposedState(
                    updatedText.triLeft().toString(), updatedText.triCenter().toString(), updatedText.triRight().toString(),
                    updatedText.triLeft().length() + updatedText.triCenter().triLeft().length(),
                    updatedText.triLeft().length() + updatedText.triCenter().triLeftCenter().length());

            VariableBehaviorSettings[] settingsBuilders =
                    new VariableBehaviorSettings[] {
                            new VariableBehaviorSettings(),
                            new VariableBehaviorSettings()
                                    .blockBaseExtractText()
                    };
            List<GetComposedTextTestCase> testCases = new ArrayList<>();
            for (VariableBehaviorSettings settingsBuilder : settingsBuilders) {

                //TODO: (EW) group to allow combining loops
                for (Named<ComposedState> stateOption : composedInitialStateOptions) {
                    ComposedState state = stateOption.data;
                    testCases.add(new GetComposedTextTestCase(
                            "fully cached text with no change (" + stateOption.name + ") - returns full text")
                            .setInitialState(state)
                            .setInitialCachedRanges(new Range[] {
                                    new Range(0,
                                            initialText.length() - 1)
                            })
                            .setCursorPositionKnown(true)
                            .setExpectedResponse(initialText.triCenter())
                            .setExpectedCache(state.getTextBeforeCursor(),
                                    state.getSelectedText(),
                                    state.getTextAfterCursor())
                            .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                    .setGetTextLimit(1)
                                    .limitExtractedText(1, EXTRACT_TEXT_FROM_START)));
                }
                for (Named<ComposedState> stateOption : composedInitialStateOptions) {
                    ComposedState state = stateOption.data;
                    testCases.add(new GetComposedTextTestCase(
                            "not cached text with no change (" + stateOption.name + ") - returns full text")
                            .setInitialState(state)
                            .setInitialCachedRanges(new Range[] {
                                    new Range(0,
                                            initialText.length() - 1)
                            })
                            .setCursorPositionKnown(true)
                            .setExpectedResponse(initialText.triCenter())
                            .setExpectedCache(state.getTextBeforeCursor(),
                                    state.getSelectedText(),
                                    state.getTextAfterCursor())
                            .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                    .limitExtractedText(5, EXTRACT_TEXT_CENTERED_ON_SELECTION_START)));
                }
                testCases.add(new GetComposedTextTestCase(
                        "partially cached text with no change - returns full text")
                        .setInitialState(selectionInCompositionInitialState)
                        .setInitialCachedRanges(new Range[] {
                                new Range(0,
                                        selectionInCompositionInitialState.getTextBeforeCursor().length()
                                                + selectionInCompositionInitialState.getSelectedText().length() - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setExpectedResponse(initialText.triCenter())
                        .setExpectedCache(selectionInCompositionInitialState.getTextBeforeCursor(),
                                selectionInCompositionInitialState.getSelectedText(),
                                selectionInCompositionInitialState.getTextAfterCursor().substring(0,
                                        Math.max(0, selectionInCompositionInitialState.getCompositionEnd() - selectionInCompositionInitialState.getCursorEnd())))
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .limitExtractedText(2, EXTRACT_TEXT_CENTERED_ON_SELECTION_END)));
                testCases.add(new GetComposedTextTestCase(
                        "partially cached text with changes - returns full updated text")
                        .setInitialState(selectionInCompositionInitialState)
                        .setInitialCachedRanges(new Range[] {
                                new Range(0,
                                        (selectionInCompositionInitialState.getCursorEnd()
                                                + selectionInCompositionInitialState.getCompositionEnd()) / 2 - 1)
                        })
                        .setCursorPositionKnown(true)
                        .setUpdatedState(selectionInCompositionUpdatedState)
                        .setExpectedResponse(updatedText.triCenter())
                        .setExpectedCache(selectionInCompositionUpdatedState.getTextBeforeCursor().substring(Math.max(0,
                                        selectionInCompositionUpdatedState.getTextBeforeCursor().length() - (selectionInCompositionUpdatedState.getCursorStart() - selectionInCompositionUpdatedState.getCompositionStart()))),
                                selectionInCompositionUpdatedState.getSelectedText(),
                                selectionInCompositionUpdatedState.getTextAfterCursor().substring(0,
                                        Math.max(0, selectionInCompositionUpdatedState.getCompositionEnd() - selectionInCompositionUpdatedState.getCursorEnd())))
                        .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                .limitExtractedText(2, EXTRACT_TEXT_CENTERED_ON_SELECTION_END)));
                if (!settingsBuilder.allowExtractingText) {
                    testCases.add(new GetComposedTextTestCase(
                            "partially cached text with changes and selected text not supported - returns no text")
                            .setInitialState(selectionInCompositionInitialState)
                            .setInitialCachedRanges(new Range[]{
                                    new Range(0,
                                            selectionInCompositionInitialState.getTextBeforeCursor().length() - 1),
                                    new Range(selectionInCompositionInitialState.getCursorEnd(),
                                            (selectionInCompositionInitialState.getCursorEnd()
                                                    + selectionInCompositionInitialState.getCompositionEnd()) / 2 - 1)
                            })
                            .setCursorPositionKnown(true)
                            .setUpdatedState(selectionInCompositionUpdatedState)
                            .setExpectedResponse(null)
                            .setExpectedCache(selectionInCompositionUpdatedState.getTextBeforeCursor().substring(Math.max(0,
                                            selectionInCompositionUpdatedState.getTextBeforeCursor().length() - (selectionInCompositionUpdatedState.getCursorStart() - selectionInCompositionUpdatedState.getCompositionStart()))),
                                    null,
                                    selectionInCompositionUpdatedState.getTextAfterCursor().substring(0,
                                            Math.max(0, selectionInCompositionUpdatedState.getCompositionEnd() - selectionInCompositionUpdatedState.getCursorEnd())))
                            .setSettings(new VariableBehaviorSettings(settingsBuilder)
                                    .getSelectedTextNotSupported()));
                }
            }

            List<Object[]> list = new ArrayList<>();
            for (GetComposedTextTestCase testCase : testCases) {
                list.add(new Object[] { testCase });
            }
            return list;
        }
    }

    private static class GetTextAdjacentToCursorTestCase
            extends GetTextTestCaseBase<GetTextAdjacentToCursorTestCase> {
        int requestedLength;
        int requestedFlags;
        public GetTextAdjacentToCursorTestCase(String testName) {
            super(testName);
        }
        public GetTextAdjacentToCursorTestCase setRequestedLength(int length) {
            requestedLength = length;
            return this;
        }
        public GetTextAdjacentToCursorTestCase setRequestedFlags(int flags) {
            requestedFlags = flags;
            return this;
        }
    }
    private static class GetSelectedTextTestCase
            extends GetTextTestCaseBase<GetSelectedTextTestCase> {
        int requestedFlags;
        public GetSelectedTextTestCase(String testName) {
            super(testName);
        }
        public GetSelectedTextTestCase setRequestedFlags(int flags) {
            requestedFlags = flags;
            return this;
        }
    }
    private static class GetComposedTextTestCase
            extends GetTextTestCaseBase<GetComposedTextTestCase> {
        public GetComposedTextTestCase(String testName) {
            super(testName);
        }
    }
    private static class GetTextTestCaseBase<T extends GetTextTestCaseBase<?>> {
        final String testName;
        State initialState;
        Range[] initialCachedRanges;
        State updatedState;
        boolean cursorPositionKnown;
        CharSequence expectedResponseText;
        String expectedCachedTextBefore;
        String expectedCachedSelectedText;
        String expectedCachedTextAfter;
        VariableBehaviorSettings settings;

        public GetTextTestCaseBase(String testName) {
            this.testName = testName;
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T setInitialText(CharSequence initialTextBefore,
                                CharSequence initialSelection,
                                CharSequence initialTextAfter) {
            //TODO: (EW) allow CharSequence
            initialState = new CommittedState(initialTextBefore.toString(),
                    initialSelection.toString(), initialTextAfter.toString());
            return self();
        }
        public T setInitialState(State state) {
            initialState = state;
            return self();
        }
        public T setInitialCachedRanges(Range[] initialCachedRanges) {
            this.initialCachedRanges = initialCachedRanges;
            return self();
        }
        public T setUpdatedText(CharSequence textBefore, CharSequence selection,
                                CharSequence textAfter) {
            //TODO: (EW) allow CharSequence
            updatedState = new CommittedState(textBefore.toString(), selection.toString(),
                    textAfter.toString());
            return self();
        }
        public T setUpdatedState(State state) {
            updatedState = state;
            return self();
        }
        public T setCursorPositionKnown(boolean isKnown) {
            cursorPositionKnown = isKnown;
            return self();
        }
        public T setExpectedResponse(CharSequence text) {
            expectedResponseText = text;
            return self();
        }
        public T setExpectedCache(CharSequence textBefore, CharSequence selection,
                                  CharSequence textAfter) {
            //TODO: (EW) allow CharSequence
            expectedCachedTextBefore = textBefore.toString();
            expectedCachedSelectedText = selection == null ? null : selection.toString();
            expectedCachedTextAfter = textAfter.toString();
            return self();
        }
        public T setSettings(VariableBehaviorSettings settings) {
            this.settings = new VariableBehaviorSettings(settings);
            return self();
        }

        @Override
        public String toString() {
            return testName;
        }
    }

    //TODO: (EW) add tests for getSelectedText and getTextAfterCursor
    //#endregion

    //#region shared test case components
    private static List<Named<TestSettings>> getSetSelectionSettingsOptions(final boolean testUnknownPosition) {
        final List<Named<TestSettings>> list = new ArrayList<>();
        if (testUnknownPosition) {
            list.add(new Named<>("default input connection with known cursor",
                    new TestSettings(new VariableBehaviorSettings(), true)));
            list.add(new Named<>("limited get text length with known cursor and update selection before extracted text",
                    new TestSettings(new VariableBehaviorSettings()
                            .limitReturnedText(5, true, 5)
                            .blockBaseExtractText()
                            .updateSelectionBeforeExtractedText(),
                            true)));
        } else {
            list.add(new Named<>("limited get text length with unknown cursor and update selection before extracted text",
                    new TestSettings(new VariableBehaviorSettings()
                            .limitReturnedText(5, true, 5)
                            .blockBaseExtractText()
                            .updateSelectionBeforeExtractedText(),
                            false)));
        }
        return list;
    }

    //TODO: (EW) set up some way to ensure we don't have duplicate test cases/settings
    private static List<Named<TestSettings>> getAddTextSettingsOptions(final boolean testUnknownPosition) {
        //TODO: are all of these combinations valuable and are there other settings worth testing here?
        //TODO: we probably need to limit the extract updates (or update to be more explicit if it's already working)
        final List<Named<TestSettings>> list = new ArrayList<>();
        final int limitedGetTextLimit = 5;
        final int limitedExtractMonitorTextLimit = 5;
        list.add(new Named<>("default input connection with known cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true),
                        true)));
        list.add(new Named<>("limited get text length with known cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .limitReturnedText(limitedGetTextLimit, true, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText(),
                        true)));
        list.add(new Named<>("limited get text length with known cursor and update selection before extracted text",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, true, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText()
                        .updateSelectionBeforeExtractedText(),
                        true)));
        if (testUnknownPosition) {
            list.add(new Named<>("default input connection with unknown cursor",
                    new TestSettings(new VariableBehaviorSettings()
                            .sendSelectionUpdateWhenNotChanged(true)
                            .blockBaseExtractText(),
                            false)));
            list.add(new Named<>("limited get text length with unknown cursor",
                    new TestSettings(new VariableBehaviorSettings()
                            .sendSelectionUpdateWhenNotChanged(true)
                            .limitReturnedText(5, true, 5).blockBaseExtractText(),
                            false)));
            list.add(new Named<>("limited get text length with unknown cursor and update selection before extracted text",
                    new TestSettings(new VariableBehaviorSettings()
                            .sendSelectionUpdateWhenNotChanged(true)
                            .limitReturnedText(5, true, 5)
                            .blockBaseExtractText()
                            .updateSelectionBeforeExtractedText(),
                            false)));
        }
        return list;
    }
    private static List<Named<TestSettingsBuilder>> getAddTextSettingsBuilderOptions(final boolean testUnknownPosition) {
        //TODO: are all of these combinations valuable and are there other settings worth testing here?
        //TODO: we probably need to limit the extract updates (or update to be more explicit if it's already working)
        //TODO: test different extract options and order
        final List<Named<TestSettingsBuilder>> list = new ArrayList<>();
        final int limitedGetTextLimit = 5;
        final int limitedExtractMonitorTextLimit = 5;
        list.add(new Named<>("default input connection with known cursor",
                new TestSettingsBuilder(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true),
                        true)));
        list.add(new Named<>("limited get text length with known cursor",
                new TestSettingsBuilder(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, true, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText(),
                        true)));
        list.add(new Named<>("limited get text length with known cursor and update selection before extracted text",
                new TestSettingsBuilder(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, true, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText()
                        .updateSelectionBeforeExtractedText(),
                        true)));
        if (testUnknownPosition) {
            list.add(new Named<>("default input connection with unknown cursor",
                    new TestSettingsBuilder(new VariableBehaviorSettings()
                            .sendSelectionUpdateWhenNotChanged(true).
                            blockBaseExtractText(),
                            false)));
            list.add(new Named<>("limited get text length with unknown cursor",
                    new TestSettingsBuilder(new VariableBehaviorSettings()
                            .sendSelectionUpdateWhenNotChanged(true)
                            .limitReturnedText(5, true, 5).blockBaseExtractText(),
                            false)));
            list.add(new Named<>("limited get text length with unknown cursor and update selection before extracted text",
                    new TestSettingsBuilder(new VariableBehaviorSettings()
                            .sendSelectionUpdateWhenNotChanged(true)
                            .limitReturnedText(5, true, 5)
                            .blockBaseExtractText()
                            .updateSelectionBeforeExtractedText(),
                            false)));
        }
        return list;
    }
    //TODO: rename - also used for finish composing - also maybe merge with getAddTextSettingsOptions
    private static List<Named<TestSettings>> getDeleteSelectedSettingsOptions() {
        final List<Named<TestSettings>> list = new ArrayList<>();
        final int limitedGetTextLimit = 5;
        final int limitedExtractMonitorTextLimit = 5;
        //TODO: does this also need unknown cursor positions (not for finishing composition except
        // for maybe edge case where there is no composition)
        list.add(new Named<>("default input connection with known cursor",
                new TestSettings(new VariableBehaviorSettings(), true)));
        list.add(new Named<>("limited get text length with known cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .limitReturnedText(limitedGetTextLimit, true, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText(),
                        true)));
        list.add(new Named<>("limited get text length with known cursor and update selection before extracted text",
                new TestSettings(new VariableBehaviorSettings()
                        .limitReturnedText(limitedGetTextLimit, true, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText()
                        .updateSelectionBeforeExtractedText(),
                        true)));
        return list;
    }
    private static List<Named<TestSettings>> getDeleteBeforeSettingsOptions(
            final boolean includeSetComposingRegionNotSupported, final boolean composingText) {
        final List<Named<TestSettings>> list = new ArrayList<>();
        final int limitedGetTextLimit = 5;
        final int limitedExtractMonitorTextLimit = 5;
        //TODO: does this also need unknown cursor positions (not for finishing composition except for maybe edge case where there is no composition)
        //TODO: do we need all of these combinations? at least some probably could be skipped
        //TODO: should the limited text cases use partial updates (or as an additional case)?
        //TODO: (EW) update names when !includeSetComposingRegionNotSupported to not bother mentioning setComposingRegion
        //TODO: (EW) some delete before text tests previously used getDeleteSelectedSettingsOptions.
        // are there any settings there that would be useful here (it uses partial updates and
        // blocks extracting text)?
        if (includeSetComposingRegionNotSupported) {
            list.add(new Named<>("setComposingRegion not supported and delete through the composing text and unlimited get text length",
                    new TestSettings(new VariableBehaviorSettings()
                            .setComposingRegionNotSupported(),
                            true)));
            if (composingText) {
                list.add(new Named<>("setComposingRegion not supported and delete around the composing text and unlimited get text length",
                        new TestSettings(new VariableBehaviorSettings()
                                .setComposingRegionNotSupported()
                                .deleteAroundComposingText(),
                                true)));
            }
        }
        list.add(new Named<>("setComposingRegion supported and delete through the composing text and unlimited get text length",
                new TestSettings(new VariableBehaviorSettings(),
                        true)));
        if (composingText) {
            list.add(new Named<>("setComposingRegion supported and delete around the composing text and unlimited get text length",
                    new TestSettings(new VariableBehaviorSettings()
                            .deleteAroundComposingText(),
                            true)));
        }
        if (includeSetComposingRegionNotSupported) {
            list.add(new Named<>("setComposingRegion not supported and delete through the composing text and limited get text length",
                    new TestSettings(new VariableBehaviorSettings()
                            .setComposingRegionNotSupported()
                            .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit),
                            true)));
            if (composingText) {
                list.add(new Named<>("setComposingRegion not supported and delete around the composing text and limited get text length",
                        new TestSettings(new VariableBehaviorSettings()
                                .setComposingRegionNotSupported()
                                .deleteAroundComposingText()
                                .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit),
                                true)));
            }
        }
        list.add(new Named<>("setComposingRegion supported and delete through the composing text and limited get text length",
                new TestSettings(new VariableBehaviorSettings()
                        .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit),
                        true)));
        if (composingText) {
            list.add(new Named<>("setComposingRegion supported and delete around the composing text and limited get text length",
                    new TestSettings(new VariableBehaviorSettings()
                            .deleteAroundComposingText()
                            .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit),
                            true)));
        }

        if (includeSetComposingRegionNotSupported) {
            list.add(new Named<>("setComposingRegion not supported and delete through the composing text and limited get text length and cursor updated before text",
                    new TestSettings(new VariableBehaviorSettings()
                            .setComposingRegionNotSupported()
                            .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                            .updateSelectionBeforeExtractedText(),
                            true)));
            if (composingText) {
                list.add(new Named<>("setComposingRegion not supported and delete around the composing text and limited get text length and cursor updated before text",
                        new TestSettings(new VariableBehaviorSettings()
                                .setComposingRegionNotSupported()
                                .deleteAroundComposingText()
                                .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                                .updateSelectionBeforeExtractedText(),
                                true)));
            }
        }
        list.add(new Named<>("setComposingRegion supported and delete through the composing text and limited get text length and cursor updated before text",
                new TestSettings(new VariableBehaviorSettings()
                        .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                        .updateSelectionBeforeExtractedText(),
                        true)));
        if (composingText) {
            list.add(new Named<>("setComposingRegion supported and delete around the composing text and limited get text length and cursor updated before text",
                    new TestSettings(new VariableBehaviorSettings()
                            .deleteAroundComposingText()
                            .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                            .updateSelectionBeforeExtractedText(),
                            true)));
        }
        return list;
    }
    private static List<Named<TestSettings>> getExternalActionSettingsOptions() {
        // test full extracted text update, large extracted text update, and edited extracted text update
        // test both with cache text around cursor available and not
        // test both limited and full text
        final int limitedGetTextLimit = 5;
        final int limitedExtractMonitorTextLimit = 5;
        final List<Named<TestSettings>> list = new ArrayList<>();
        list.add(new Named<>("full updates with known cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(Integer.MAX_VALUE, false, Integer.MAX_VALUE),
                        true)));
        list.add(new Named<>("limited get text length and partial updates with known cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, true, Integer.MAX_VALUE)
                        .blockBaseExtractText(),
                        true)));
        list.add(new Named<>("limited get text length and limited full updates with known cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText(),
                        true)));
        list.add(new Named<>("partial updates with known cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(Integer.MAX_VALUE, true, Integer.MAX_VALUE),
                        true)));

        //TODO: tests fail when using the composition cache
        list.add(new Named<>("limited get text length and partial updates with unknown cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, true, Integer.MAX_VALUE)
                        .blockBaseExtractText(),
                        false)));
        //TODO: tests fail when using the composition cache
        list.add(new Named<>("limited get text length and limited full updates with unknown cursor",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText(),
                        false)));

        //TODO: see if all of these are necessary
        list.add(new Named<>("full updates with known cursor and cursor updated before text",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(Integer.MAX_VALUE, false, Integer.MAX_VALUE)
                        .updateSelectionBeforeExtractedText(),
                        true)));
        list.add(new Named<>("limited get text length and partial updates with known cursor and cursor updated before text",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, true, Integer.MAX_VALUE)
                        .blockBaseExtractText()
                        .updateSelectionBeforeExtractedText(),
                        true)));
        list.add(new Named<>("limited get text length and limited full updates with known cursor and cursor updated before text",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText()
                        .updateSelectionBeforeExtractedText(),
                        true)));
        list.add(new Named<>("partial updates with known cursor and cursor updated before text",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(Integer.MAX_VALUE, true, Integer.MAX_VALUE)
                        .updateSelectionBeforeExtractedText(),
                        true)));

        //TODO: tests fail when using the composition cache
        list.add(new Named<>("limited get text length and partial updates with unknown cursor and cursor updated before text",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, true, Integer.MAX_VALUE)
                        .blockBaseExtractText()
                        .updateSelectionBeforeExtractedText(),
                        false)));
        //TODO: tests fail when using the composition cache
        list.add(new Named<>("limited get text length and limited full updates with unknown cursor and cursor updated before text",
                new TestSettings(new VariableBehaviorSettings()
                        .sendSelectionUpdateWhenNotChanged(true)
                        .limitReturnedText(limitedGetTextLimit, false, limitedExtractMonitorTextLimit)
                        .blockBaseExtractText()
                        .updateSelectionBeforeExtractedText(),
                        false)));

        return list;
    }

    private static AddTextOption[] getAddTextOptions() {
        final Named<String> initialNoText =
                new Named<>("no text", "");
        final Named<String> initialNormalText =
                new Named<>("normal text", "Lorem ipsum dolor sit amet");
        final Named<String> initialSurrogatePairs =
                new Named<>("surrogate pairs", getSurrogatePairString(0, 26));
        final Named<String> initialSurrogatePairsAndNormalText =
                new Named<>("surrogate pairs and normal text",
                        getAlternatingSurrogatePairString(0, 'a', 26));

        final Named<String> addingNothing =
                new Named<>("nothing", "");
        final Named<String> addingNormalCharacter =
                new Named<>("normal character", "a");
        final Named<String> addingSurrogatePair =
                new Named<>("surrogate pair", getSurrogatePairString(0));
        final Named<String> addingNormalCharactersAndSurrogatePair =
                new Named<>("normal characters and a surrogate pair",
                        "test" + getSurrogatePairString(1));

        //TODO: see if we can reduce these options a bit more
        return new AddTextOption[] {
                new AddTextOption(initialNoText, addingNothing),
                new AddTextOption(initialNoText, addingNormalCharacter),
                new AddTextOption(initialNoText, addingSurrogatePair),
                new AddTextOption(initialNormalText, addingNothing),
                new AddTextOption(initialNormalText, addingNormalCharacter),
                new AddTextOption(initialNormalText, addingSurrogatePair),
                new AddTextOption(initialNormalText, addingNormalCharactersAndSurrogatePair),
                new AddTextOption(initialSurrogatePairs, addingNormalCharacter),
                new AddTextOption(initialSurrogatePairs, addingSurrogatePair),
                new AddTextOption(initialSurrogatePairsAndNormalText, addingNormalCharactersAndSurrogatePair),

//                new AddTextOption(initialNoText, addingNormalCharactersAndSurrogatePair),
//                new AddTextOption(initialSurrogatePairs, addingNothing),
//                new AddTextOption(initialSurrogatePairs, addingNormalCharactersAndSurrogatePair),
//                new AddTextOption(initialSurrogatePairsAndNormalText, addingNothing),
//                new AddTextOption(initialSurrogatePairsAndNormalText, addingNormalCharacter),
//                new AddTextOption(initialSurrogatePairsAndNormalText, addingSurrogatePair),
        };
    }

    private static AddTextOption[] getFinishComposingTextOptions() {
        final Named<String> initialNoText =
                new Named<>("no text", "");
        final Named<String> initialNormalText =
                new Named<>("normal text", "Lorem ipsum dolor sit amet");
        final Named<String> initialSurrogatePairs =
                new Named<>("surrogate pairs", getSurrogatePairString(0, 26));
        final Named<String> initialSurrogatePairsAndNormalText =
                new Named<>("surrogate pairs and normal text",
                        getAlternatingSurrogatePairString(0, 'a', 26));

        final Named<String> composedNormalCharacter =
                new Named<>("normal character", "a");
        final Named<String> composedSurrogatePair =
                new Named<>("surrogate pair", getSurrogatePairString(0));
        final Named<String> compsedNormalCharactersAndSurrogatePair =
                new Named<>("normal characters and a surrogate pair",
                        "test" + getSurrogatePairString(1));

        return new AddTextOption[] {
                new AddTextOption(initialNoText, composedNormalCharacter),
                new AddTextOption(initialNoText, composedSurrogatePair),
                new AddTextOption(initialNormalText, composedNormalCharacter),
                new AddTextOption(initialNormalText, composedSurrogatePair),
                new AddTextOption(initialSurrogatePairs, composedSurrogatePair),
                new AddTextOption(initialSurrogatePairsAndNormalText, compsedNormalCharactersAndSurrogatePair)
        };
    }

    private static List<Named<ThreePartText>> getDeletingTextOptions() {
        final List<Named<ThreePartText>> list = new ArrayList<>();
        final Named<String>[] uniformTextOptions = new Named[] {
                new Named<>("normal character", "Lorem ipsum dolor sit amet"),
                new Named<>("surrogate pair", getSurrogatePairString(0, 26))
        };
        final String alternatingText = getAlternatingSurrogatePairString(0, 'a', 26);
        final int middleIndex = 11;
        final int multipleCodePointLength = 3;
        for (final Named<String> textOption : uniformTextOptions) {
            list.add(new Named<>("one " + textOption.name + " from the beginning",
                    new ThreePartText(
                            "",
                            getSubstring(textOption.data, 0, 1),
                            getSubstring(textOption.data, 1)
                    )));
            list.add(new Named<>("multiple " + textOption.name + "s from the beginning",
                    new ThreePartText(
                            "",
                            getSubstring(textOption.data, 0, multipleCodePointLength),
                            getSubstring(textOption.data, multipleCodePointLength)
                    )));
            list.add(new Named<>("one " + textOption.name + " from the middle",
                    new ThreePartText(
                            getSubstring(textOption.data, 0, middleIndex),
                            getSubstring(textOption.data, middleIndex, middleIndex + 1),
                            getSubstring(textOption.data, middleIndex + 1)
                    )));
            list.add(new Named<>("multiple " + textOption.name + "s from the middle",
                    new ThreePartText(
                            getSubstring(textOption.data, 0, middleIndex),
                            getSubstring(textOption.data, middleIndex, middleIndex + multipleCodePointLength),
                            getSubstring(textOption.data, middleIndex + multipleCodePointLength)
                    )));
            list.add(new Named<>("one " + textOption.name + " from the end",
                    new ThreePartText(
                            getSubstring(textOption.data, 0, -1),
                            getSubstring(textOption.data, -1),
                            ""
                    )));
            list.add(new Named<>("multiple " + textOption.name + "s from the end",
                    new ThreePartText(
                            getSubstring(textOption.data, 0, -multipleCodePointLength),
                            getSubstring(textOption.data, -multipleCodePointLength),
                            ""
                    )));
        }
        list.add(new Named<>("multiple mixed characters from the middle",
                new ThreePartText(
                        getSubstring(alternatingText, 0, middleIndex),
                        getSubstring(alternatingText, middleIndex, middleIndex + multipleCodePointLength),
                        getSubstring(alternatingText, middleIndex + multipleCodePointLength)
                )));
        list.add(new Named<>("nothing from the middle",
                new ThreePartText(
                        getSubstring(alternatingText, 0, middleIndex),
                        "",
                        getSubstring(alternatingText, middleIndex)
                )));
        return list;
    }

    private static NewCursorPositionParams[] getInsertTextNewCursorPositions(final String leftHalf,
                                                                             final int addedTextLength,
                                                                             final String rightHalf) {
        return getNewCursorPositionOptions(leftHalf, addedTextLength, rightHalf,
                new RelativePosition[] {
                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, -1),
                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, 1),
                        new RelativePosition(AddingTextPosition.BEGINNING_OF_NEW_TEXT, 0),
                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 1),
                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, 0)
                });
    }
    private static NewCursorPositionParams[] getReplaceSelectionNewCursorPositions(final String leftOfSelection,
                                                                                   final int addedTextLength,
                                                                                   final String rightOfSelection) {
        return getNewCursorPositionOptions(leftOfSelection, addedTextLength, rightOfSelection,
                new RelativePosition[] {
                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, 0),
                        new RelativePosition(AddingTextPosition.BEGINNING_OF_NEW_TEXT, -1),
                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 0),
                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, -1),
                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, 1)
                });
    }
    private static NewCursorPositionParams[] getUpdateCompositionNewCursorPositions(final String leftOfComposed,
                                                                                    final int addedTextLength,
                                                                                    final String rightOfComposed) {
        return getNewCursorPositionOptions(leftOfComposed, addedTextLength, rightOfComposed,
                new RelativePosition[] {
                        new RelativePosition(AddingTextPosition.BEGINNING_OF_FULL_TEXT, 2),
                        new RelativePosition(AddingTextPosition.BEGINNING_OF_NEW_TEXT, -2),
                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 0),
                        new RelativePosition(AddingTextPosition.END_OF_NEW_TEXT, 2),
                        new RelativePosition(AddingTextPosition.END_OF_FULL_TEXT, -2),
                });
    }

    private static InitialCursorPositionParams[] getInitialCursorPositions(final String leftOfComposed,
                                                                           final String composed,
                                                                           final String rightOfComposed) {
        final int selectionStart;
        if (!isEmpty(leftOfComposed)) {
            selectionStart = getCharCountBeforeCodePoint(leftOfComposed, -1);
        } else if (codePointCount(composed) >= 3) {
            selectionStart = getCharCountBeforeCodePoint(composed, 1);
        } else {
            selectionStart = 0;
        }
        final int selectionEnd;
        if (!isEmpty(rightOfComposed)) {
            selectionEnd = leftOfComposed.length() + composed.length() + getCharCountBeforeCodePoint(rightOfComposed, 1);
        } else if (codePointCount(composed) >= 3) {
            selectionEnd = leftOfComposed.length() + getCharCountBeforeCodePoint(composed, -1);
        } else {
            selectionEnd = leftOfComposed.length() + composed.length();
        }
        return new InitialCursorPositionParams[]{
                new InitialCursorPositionParams("at the beginning of the text", 0),
//                new InitialCursorPositionParams("at the end of the composition", leftOfComposed.length() + composed.length()),
                new InitialCursorPositionParams("at the end of the text", leftOfComposed.length() + composed.length() + rightOfComposed.length()),
                new InitialCursorPositionParams("selecting text",
                        selectionStart,
                        selectionEnd),
        };
    }
    //#endregion

    //#region shared test case helpers
    private static NewCursorPositionParams getNewCursorPosition(final String left,
                                                                final int newTextLength,
                                                                final String right,
                                                                final AddingTextPosition referencePoint,
                                                                final int codePointOffset) {
        final String offsetName;
        if (codePointOffset < 0) {
            offsetName = Math.abs(codePointOffset) + " before the ";
        } else if (codePointOffset > 0) {
            offsetName = Math.abs(codePointOffset) + " after the ";
        } else {
            offsetName = "the ";
        }
        final String referenceName;
        final int newCursorPosition;
        final int newCursorIndex;
        switch (referencePoint) {
            case BEGINNING_OF_FULL_TEXT:
                referenceName = "beginning of the full text";
                if (codePointOffset <= 0) {
                    newCursorPosition = -left.length() + codePointOffset;
                    newCursorIndex = codePointOffset;
                } else if (codePointOffset <= codePointCount(left)) {
                    newCursorPosition = -getCharCountAfterCodePoint(left, codePointOffset);
                    newCursorIndex = getCharCountBeforeCodePoint(left, codePointOffset);
                } else {
                    throw new IllegalArgumentException("can't reference inside the new text");
                }
                break;
            case BEGINNING_OF_NEW_TEXT:
                referenceName = "beginning of the new text";
                if (codePointOffset > 0) {
                    throw new IllegalArgumentException("can't reference inside the new text");
                }
                if (codePointOffset < 0) {
                    newCursorPosition = -getCharCountAfterCodePoint(left, codePointOffset);
                    newCursorIndex = codePointCount(left) >= -codePointOffset
                            ? getCharCountBeforeCodePoint(left, codePointOffset)
                            : (codePointOffset + codePointCount(left));
                } else {
                    newCursorPosition = 0;
                    newCursorIndex = left.length();
                }
                break;
            case END_OF_NEW_TEXT:
                referenceName = "end of the new text";
                if (codePointOffset < 0) {
                    throw new IllegalArgumentException("can't reference inside the new text");
                }
                newCursorPosition = 1 + getCharCountBeforeCodePoint(right, codePointOffset);
                newCursorIndex = left.length() + newTextLength
                        + (codePointOffset <= codePointCount(right)
                        ? getCharCountBeforeCodePoint(right, codePointOffset)
                        : (right.length() + codePointOffset - 1 - codePointCount(right)));
                break;
            case END_OF_FULL_TEXT:
                referenceName = "end of the full text";
                if (-codePointOffset > codePointCount(right)) {
                    throw new IllegalArgumentException("can't reference inside the new text");
                }
                if (codePointOffset < 0) {
                    newCursorPosition = 1 + getCharCountBeforeCodePoint(right, codePointOffset);
                    newCursorIndex = left.length() + newTextLength + getCharCountBeforeCodePoint(right, codePointOffset);
                } else {
                    newCursorPosition = 1 + right.length() + codePointOffset;
                    newCursorIndex = left.length() + newTextLength + right.length() + codePointOffset;
                }
                break;
            default:
                throw new IllegalArgumentException("unknown enum: " + referencePoint);
        }
        int newCursorIndexSanitized = Math.max(0,
                Math.min(newCursorIndex,
                        left.length() + newTextLength + right.length()));
        return new NewCursorPositionParams(offsetName + referenceName, newCursorPosition,
                newCursorIndexSanitized, newCursorIndex);
    }

    private static NewCursorPositionParams[] getNewCursorPositionOptions(
            final String left, final int newTextLength, final String right,
            final RelativePosition[] positions) {
        final List<NewCursorPositionParams> list = new ArrayList<>();
        for (final RelativePosition position : positions) {
            final NewCursorPositionParams newItem = getNewCursorPosition(left, newTextLength, right, position.referencePoint, position.codePointOffset);
            // only add this if it is unique
            boolean itemExists = false;
            for (int i = 0; i < list.size(); i++) {
                final NewCursorPositionParams existingItem = list.get(i);
                if (existingItem.newCursorPosition == newItem.newCursorPosition
                        && existingItem.newCursorIndexRaw == newItem.newCursorIndexRaw) {
                    // update the name of the existing case
                    list.set(i, new NewCursorPositionParams(
                            existingItem.name + " / " + newItem.name,
                            existingItem.newCursorPosition,
                            existingItem.newCursorIndexSanitized,
                            existingItem.newCursorIndexRaw));
                    itemExists = true;
                    break;
                }
            }
            if (!itemExists) {
                list.add(newItem);
            }
        }
        //TODO: probably don't make this an array
        return list.toArray(new NewCursorPositionParams[0]);
    }

    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
                                                   final Named<TestSettings> settingsOption, final int initialCursorStart,
                                                   final int initialCursorEnd, final int initialCompositionStart,
                                                   final int initialCompositionEnd, final Named<String> addedTextOption) {
        if (newCursorPositionOption.newCursorPosition > 1
                && settingsOption.data.inputConnectionSettings.getTextLimit < Integer.MAX_VALUE
                && settingsOption.data.initialCursorPositionKnown) {
            final int maxKnownIndex = Math.max(
                    initialCursorEnd + settingsOption.data.inputConnectionSettings.getTextLimit,
                    initialCompositionEnd);
            final int replacingTextLength = initialCompositionStart == -1 && initialCompositionEnd == -1
                    ? initialCursorEnd - initialCursorStart
                    : initialCompositionEnd - initialCompositionStart;
            final int changedTextLength = addedTextOption.data.length() - replacingTextLength;
            if (maxKnownIndex + changedTextLength < newCursorPositionOption.newCursorIndexSanitized) {
                return false;
            }
        }
        return true;
    }
    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
                                                   final Named<TestSettings> settingsOption,
                                                   final int initialCursorEnd, final int initialCompositionStart,
                                                   final int initialCompositionEnd, final Named<String> addedTextOption) {
        //TODO: clean this up. initialCursorEnd is getting passed as the initialCursorStart, even
        // though it may not be, but it isn't actually relevant for the composing case
        return isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, initialCursorEnd,
                initialCursorEnd, initialCompositionStart, initialCompositionEnd, addedTextOption);
    }
    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
                                                   final Named<TestSettings> settingsOption,
                                                   final int initialCursorPosition, final Named<String> addedTextOption) {
        return isNewCursorInKnownSpace(newCursorPositionOption, settingsOption,
                initialCursorPosition, initialCursorPosition, -1, -1, addedTextOption);
    }
    private static boolean isNewCursorInKnownSpace(final NewCursorPositionParams newCursorPositionOption,
                                                   final Named<TestSettings> settingsOption,
                                                   final int initialCursorStart, final int initialCursorEnd,
                                                   final Named<String> addedTextOption) {
        return isNewCursorInKnownSpace(newCursorPositionOption, settingsOption, initialCursorStart,
                initialCursorEnd, -1, -1, addedTextOption);
    }

    private static int getAbsolutePosition(final String textBefore, final String composedText,
                                           final String textAfter, final ComposedTextPosition position) {
        switch (position) {
            case BEGINNING_OF_TEXT:
                return 0;
            case AFTER_BEGINNING_OF_TEXT:
                return getCharCountBeforeCodePoint(textBefore, 1);
            case BEFORE_BEGINNING_OF_COMPOSITION:
                return getCharCountBeforeCodePoint(textBefore, -1);
            case BEGINNING_OF_COMPOSITION:
                return textBefore.length();
            case AFTER_BEGINNING_OF_COMPOSITION:
                return textBefore.length() + getCharCountBeforeCodePoint(composedText, 1);
            case BEFORE_END_OF_COMPOSITION:
                return textBefore.length() + getCharCountBeforeCodePoint(composedText, -1);
            case END_OF_COMPOSITION:
                return textBefore.length() + codePointCount(composedText);
            case AFTER_END_OF_COMPOSITION:
                return textBefore.length() + composedText.length()
                        + getCharCountBeforeCodePoint(textAfter, 1);
            case BEFORE_END_OF_TEXT:
                return textBefore.length() + composedText.length()
                        + getCharCountBeforeCodePoint(textAfter, -1);
            case END_OF_TEXT:
                return textBefore.length() + composedText.length() + composedText.length();
            default:
                throw new IllegalArgumentException("Invalid enum: " + position);
        }
    }

    //TODO: name better
    private static class NewCursorPositionRelative {
        public final int distanceFromText;
        public final boolean cursorAfterText;
        public NewCursorPositionRelative(final int distanceFromText, final boolean cursorAfterText) {
            if (distanceFromText < 0) {
                throw new IllegalArgumentException("distanceFromText can't be negative: " + distanceFromText);
            }
            this.distanceFromText = distanceFromText;
            this.cursorAfterText = cursorAfterText;
        }

        public static NewCursorPositionRelative before(final int distanceBeforeText) {
            return new NewCursorPositionRelative(distanceBeforeText, false);
        }

        public static NewCursorPositionRelative after(final int distanceBeforeText) {
            return new NewCursorPositionRelative(distanceBeforeText, true);
        }

        @Override
        public String toString() {
            return (distanceFromText == 0 ? "right" : "well") + " "
                    + (cursorAfterText ? "after" : "before");
        }
    }

    private static ActionTestCase<AddText> getTextModifierComposeTestCase(final String testName,
                                                                          final Named<TestSettingsBuilder> settingsOption,
                                                                          final TextModifier textModifier,
                                                                          final String before,
                                                                          final String after,
                                                                          final String originalText,
                                                                          final String modifiedText,
                                                                          final NewCursorPositionRelative cursorPosition,
                                                                          final boolean isExpectedSelectionChange) {
        return getTextModifierComposeTestCase(testName, settingsOption, textModifier,
                new CommittedState(before, after), before, after, originalText,
                modifiedText, cursorPosition, isExpectedSelectionChange);
    }

    private static ActionTestCase<AddText> getTextModifierComposeTestCase(final String testName,
                                                                          final Named<TestSettingsBuilder> settingsOption,
                                                                          final TextModifier textModifier,
                                                                          final String before,
                                                                          final String composed,
                                                                          final String after,
                                                                          final String originalText,
                                                                          final String modifiedText,
                                                                          final NewCursorPositionRelative cursorPosition,
                                                                          final boolean isExpectedSelectionChange) {
        return getTextModifierComposeTestCase(testName, settingsOption, textModifier,
                new ComposedState(before, composed, after), before, after, originalText,
                modifiedText, cursorPosition, isExpectedSelectionChange);
    }
    private static ActionTestCase<AddText> getTextModifierComposeTestCase(final String testName,
                                                                          final Named<TestSettingsBuilder> settingsOption,
                                                                          final TextModifier textModifier,
                                                                          final String before,
                                                                          final String composed,
                                                                          final String after,
                                                                          final int initialCursorPosition,
                                                                          final String originalText,
                                                                          final String modifiedText,
                                                                          final NewCursorPositionRelative cursorPosition,
                                                                          final boolean isExpectedSelectionChange) {
        return getTextModifierComposeTestCase(testName, settingsOption, textModifier,
                new ComposedState(before, composed, after, initialCursorPosition), before, after, originalText,
                modifiedText, cursorPosition, isExpectedSelectionChange);
    }

    private static ActionTestCase<AddText> getTextModifierComposeTestCase(final String testName,
                                                                          final Named<TestSettingsBuilder> settingsOption,
                                                                          final TextModifier textModifier,
                                                                          final State initialState,
                                                                          final String before,
                                                                          final String after,
                                                                          final String originalText,
                                                                          final String modifiedText,
                                                                          final NewCursorPositionRelative cursorPosition,
                                                                          final boolean isExpectedSelectionChange) {
        return getTextModifierTestCase(testName, settingsOption, textModifier,
                initialState, new ComposedState(before, modifiedText, after,
                        before.length() + (cursorPosition.cursorAfterText
                                ? modifiedText.length() + cursorPosition.distanceFromText
                                : -cursorPosition.distanceFromText)),
                originalText, true, cursorPosition, isExpectedSelectionChange);
    }

    private static ActionTestCase<AddText> getTextModifierCommitTestCase(final String testName,
                                                                         final Named<TestSettingsBuilder> settingsOption,
                                                                         final TextModifier textModifier,
                                                                         final String before,
                                                                         final String after,
                                                                         final String originalText,
                                                                         final String modifiedText,
                                                                         final NewCursorPositionRelative cursorPosition,
                                                                         final boolean isExpectedSelectionChange) {
        return getTextModifierCommitTestCase(testName, settingsOption, textModifier,
                new CommittedState(before, after), before, after, originalText,
                modifiedText, cursorPosition, isExpectedSelectionChange);
    }

    private static ActionTestCase<AddText> getTextModifierCommitTestCase(final String testName,
                                                                         final Named<TestSettingsBuilder> settingsOption,
                                                                         final TextModifier textModifier,
                                                                         final String before,
                                                                         final String composed,
                                                                         final String after,
                                                                         final String originalText,
                                                                         final String modifiedText,
                                                                         final NewCursorPositionRelative cursorPosition,
                                                                         final boolean isExpectedSelectionChange) {
        return getTextModifierCommitTestCase(testName, settingsOption, textModifier,
                new ComposedState(before, composed, after), before, after, originalText,
                modifiedText, cursorPosition, isExpectedSelectionChange);
    }

    private static ActionTestCase<AddText> getTextModifierCommitTestCase(final String testName,
                                                                         final Named<TestSettingsBuilder> settingsOption,
                                                                         final TextModifier textModifier,
                                                                         final State initialState,
                                                                         final String before,
                                                                         final String after,
                                                                         final String originalText,
                                                                         final String modifiedText,
                                                                         final NewCursorPositionRelative cursorPosition,
                                                                         final boolean isExpectedSelectionChange) {
        return getTextModifierTestCase(testName, settingsOption, textModifier,
                initialState, new CommittedState(before + modifiedText + after,
                        before.length() + (cursorPosition.cursorAfterText
                                ? modifiedText.length() + cursorPosition.distanceFromText
                                : -cursorPosition.distanceFromText)),
                originalText, false, cursorPosition, isExpectedSelectionChange);
    }

    private static ActionTestCase<AddText> getTextModifierTestCase(final String testName,
                                                                   final Named<TestSettingsBuilder> settingsOption,
                                                                   final TextModifier textModifier,
                                                                   final State initialState,
                                                                   final State expectedState,
                                                                   final String originalText,
                                                                   final boolean composeNewText,
                                                                   final NewCursorPositionRelative cursorPosition,
                                                                   final boolean isExpectedSelectionChange) {
        return new ActionTestCase<>(testName,
                new Named<>(settingsOption.name, new TestSettings(
                        new VariableBehaviorSettings(
                                settingsOption.data.inputConnectionSettingsBuilder)
                                .setTextModifier(textModifier),
                        settingsOption.data.initialCursorPositionKnown)),
                true,//TODO: (EW) allow this to be false for some cases
                initialState,
                new AddText(originalText, composeNewText,
                        cursorPosition.cursorAfterText
                                ? 1 + cursorPosition.distanceFromText
                                : -cursorPosition.distanceFromText),
                expectedState,
                isExpectedSelectionChange, false);
    }

    private static <T extends Groupable> Collection<Object[]> buildParameters(
            GroupedList<T> testCases) {
        List<Object[]> list = new ArrayList<>(testCases.size());
        for (T testCase : testCases) {
            list.add(new Object[] {
                    testCase instanceof Grouped ? ((Grouped<?>)testCase).data : testCase
            });
        }
        return list;
    }
    //#endregion

    //#region debug printing
    private static String getDebugInfo(final State state, final int indentLength) {
        return getDebugInfo(state, indentLength, null);
    }
    private static String getDebugInfo(final State state, final int indentLength, final String prefix) {
        String indent = getIndent(indentLength);
        String result;
        //TODO: don't use instanceof
        if (state instanceof ComposedState) {
            final ComposedState composedState = (ComposedState) state;
            result = indent + (prefix != null ? prefix + "TextBefore" : "textBefore") + "=\"" + composedState.textBefore + "\"\n"
                    + indent + (prefix != null ? prefix + "ComposedText" : "composedText") + "=\"" + composedState.composedText + "\"\n"
                    + indent + (prefix != null ? prefix + "TextAfter" : "textAfter") + "=\"" + composedState.textAfter + "\"\n";
        } else {
            result = indent + (prefix != null ? prefix + "Text" : "text") + "=\"" + state.getText() + "\"\n";
        }
        return result
                + indent + (prefix != null ? prefix + "CursorStart" : "cursorStart") + "=" + state.getCursorStart() + "\n"
                + indent + (prefix != null ? prefix + "CursorEnd" : "cursorEnd") + "=" + state.getCursorEnd() + "\n";
    }
    private static String getIndent(final int length) {
        return repeatChar(' ', length);
    }
    private static String repeatChar(final char character, final int length) {
        char[] indentArray = new char[length];
        Arrays.fill(indentArray, character);
        return new String(indentArray);
    }

    private static void printState(final State state, final int indentLength) {
        String indent = getIndent(indentLength);
        System.out.println(indent + "\"" + state.getText() + "\"");
        if (state.getCompositionStart() >= 0) {
            System.out.println(indent
                    + getIndent(1 + state.getText().codePointCount(0, state.getCompositionStart()))
                    + repeatChar('_', state.getText().codePointCount(state.getCompositionStart(), state.getCompositionEnd()))
                    + getIndent(state.getText().codePointCount(state.getCompositionEnd(), state.getText().length()) + 1));
        }
        System.out.println(indent
                + getIndent(1 + state.getText().codePointCount(0, state.getCursorStart()) - 1) + ">"
                + getIndent(state.getText().codePointCount(state.getCursorStart(), state.getCursorEnd()))
                + "<" + getIndent(-1 + state.getText().codePointCount(state.getCursorEnd(), state.getText().length()) + 1));
    }

    private static void printDebugInfo(final String name, final State initialState,
                                       final State updatedState) {
        printDebugInfo(name, initialState, null, updatedState);
    }
    private static void printDebugInfo(final String name, final State initialState,
                                       final String actionInfo, final State updatedState) {
        final int indentLength = 2;
        System.out.println(name + ":\n"
                + getDebugInfo(initialState, indentLength, "initial")
                + (actionInfo != null ? getIndent(indentLength) + actionInfo + "\n" : "")
                + getDebugInfo(updatedState, indentLength, "updated"));
        System.out.println("initialState:");
        printState(initialState, indentLength);
        System.out.println("updatedState:");
        printState(updatedState, indentLength);
    }
    //#endregion

    //#region generic helpers
    private static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }

    private static int codePointCount(final String s) {
        return s.codePointCount(0, s.length());
    }

    //#region surrogate pairs
    private static String getSurrogatePairString(final int index) {
        return getSurrogatePairString(index, 1);
    }

    private static String getSurrogatePairString(final int index, final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.appendCodePoint(getSurrogatePair(index + i));
        }
        return sb.toString();
    }

    private static int[] simplePrintableSurrogatePairIndices = new int[] {
            0, 1, 4, 5, 6, 7, 9, 13, 17, 26, 34, 54, 1024, 1027, 1028, 1030, 1032, 1035, 1040, 1049, 1057, 1075, 1077, 1085, 1090, 2072, 2080, 2100
    };

    private static String getAlternatingSurrogatePairString(final int surrogatePairIndex, final int charIndex, final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i += 2) {
            sb.appendCodePoint(getSurrogatePair(surrogatePairIndex + i));
            if (sb.length() < length) {
                sb.append((char) (charIndex + i + 1));
            }
        }
        return sb.toString();
    }

    private static int getSurrogatePair(int index) {
        // high surrogates: U+D800 to U+DBFF (1024 characters)
        // low surrogates:  U+DC00 to U+DFFF (1024 characters)
        final int charCount = 1024;
        if (index < 0 || index >= charCount * charCount) {
            throw new IllegalArgumentException("invalid index: " + index);
        }
        // iterate through both high and low surrogate pairs at the same time to avoid the same high
        // or low surrogate from appearing nearby when iterating
        final int hiIndex = (index + (index / charCount)) % charCount;
        final int loIndex = index % charCount;
        return getSurrogatePairCodePoint(HI_SURROGATE_START + hiIndex, LO_SURROGATE_START + loIndex);
    }

    final static int HI_SURROGATE_START = 0xD800;
    final static int LO_SURROGATE_START = 0xDC00;
    final static int SURROGATE_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;
    private static int getSurrogatePairCodePoint(final int lead, final int trail) {
        return (lead << 10) + trail + SURROGATE_OFFSET;
    }
    //#endregion

    private static int getCharCountBeforeCodePoint(final String text, final int codePointOffset) {
        return codePointOffset < 0
                ? text.offsetByCodePoints(text.length(), codePointOffset)
                : text.offsetByCodePoints(0, codePointOffset);
    }

    private static int getCharCountAfterCodePoint(final String text, final int codePointOffset) {
        return text.length() - getCharCountBeforeCodePoint(text, codePointOffset);
    }

    private static String getSubstring(final String text, int codePointStart, int codePointEnd) {
        if (codePointStart < 0) {
            codePointStart = codePointCount(text) + codePointStart;
        }
        if (codePointEnd < 0) {
            codePointEnd = codePointCount(text) + codePointEnd;
        }
        return text.substring(
                text.offsetByCodePoints(0, codePointStart),
                text.offsetByCodePoints(0, codePointEnd));
    }
    private static CharSequence getSubSequence(final CharSequence text, int codePointStart,
                                               int codePointEnd) {
        if (codePointStart < 0) {
            codePointStart = codePointCount(text.toString()) + codePointStart;
        }
        if (codePointEnd < 0) {
            codePointEnd = codePointCount(text.toString()) + codePointEnd;
        }
        return text.subSequence(
                text.toString().offsetByCodePoints(0, codePointStart),
                text.toString().offsetByCodePoints(0, codePointEnd));
    }
    private static String getSubstring(final String text, final int codePointStart) {
        return getSubstring(text, codePointStart, codePointCount(text));
    }

    private static String[] divideString(final String text, final int pieceCount) {
        final String[] pieces = new String[pieceCount];
        final float pieceLength = codePointCount(text) / (1f * pieceCount);
        for (int i = 0; i < pieceCount; i++) {

            pieces[i] = getSubstring(text, Math.round(pieceLength * i),
                    Math.round(pieceLength * (i + 1)));
        }
        return pieces;
    }
    private static CharSequence[] divideText(final CharSequence text, final int pieceCount) {
        final CharSequence[] pieces = new String[pieceCount];
        final float pieceLength = codePointCount(text.toString()) / (1f * pieceCount);
        for (int i = 0; i < pieceCount; i++) {

            pieces[i] = getSubSequence(text, Math.round(pieceLength * i),
                    Math.round(pieceLength * (i + 1)));
        }
        return pieces;
    }
    private static String dividedStringPiece(final String text,
                                             final int pieceIndex, final int pieceCount) {
        final float pieceLength = codePointCount(text) / (1f * pieceCount);
        return getSubstring(text, Math.round(pieceLength * pieceIndex),
                    Math.round(pieceLength * (pieceIndex + 1)));
    }
    private static String[] splitString(final String text, final int[] codePointSplitIndices) {
        final String[] pieces = new String[codePointSplitIndices.length + 1];
        for (int i = 0; i <= codePointSplitIndices.length; i++) {
            if (i == 0) {
                pieces[i] = getSubstring(text, 0, codePointSplitIndices[i]);
            } else if (i < codePointSplitIndices.length) {
                pieces[i] = getSubstring(text, codePointSplitIndices[i - 1], codePointSplitIndices[i]);
            } else {
                pieces[i] = getSubstring(text, codePointSplitIndices[i - 1]);
            }
        }
        return pieces;
    }

    private static CharSequence shiftText(final CharSequence text) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append((char)(text.charAt(i) + 'A' - 'a'));
        }
        return sb;
    }

    private static int matchingLength(final String a, final String b, final boolean right) {
        if (a == null || b == null) {
            return 0;
        }
        int length = 0;
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            char aChar = a.charAt(right ? a.length() - i - 1 : i);
            char bChar = b.charAt(right ? b.length() - i - 1 : i);
            if (aChar == bChar) {
                length++;
            } else {
                return length;
            }
        }
        return length;
    }
    //#endregion

    //#region test data types
    //TODO: either get rid of this and have other relevant classes manage their own names or use
    // this in all classes with names (maybe make this an abstract class they inherit from
    private static class Named<T> {
        final String name;
        final T data;
        public Named(final String name, final T data) {
            this.name = name;
            this.data = data;

        }

        @Override
        public String toString() {
            return name;
        }
    }
    private interface Named2 {
        String name();
    }

    //#region settings
    private static class TestSettings {
        public final VariableBehaviorSettings inputConnectionSettings;
        public final boolean initialCursorPositionKnown;
        public TestSettings(final VariableBehaviorSettings inputConnectionSettings,
                            final boolean initialCursorPositionKnown) {
            this.inputConnectionSettings = inputConnectionSettings;
            this.initialCursorPositionKnown = initialCursorPositionKnown;
        }
    }

    //TODO: rename - this isn't actually a builder. it just contains one
    private static class TestSettingsBuilder {
        public final VariableBehaviorSettings inputConnectionSettingsBuilder;
        public final boolean initialCursorPositionKnown;
        public TestSettingsBuilder(final VariableBehaviorSettings inputConnectionSettingsBuilder,
                            final boolean initialCursorPositionKnown) {
            this.inputConnectionSettingsBuilder = inputConnectionSettingsBuilder;
            this.initialCursorPositionKnown = initialCursorPositionKnown;
        }
    }
    //#endregion

    //#region test case builder components
    private static class AddTextOption {
        public final Named<String> initialText;
        public final Named<String> addingText;
        public AddTextOption(final Named<String> initialText, final Named<String> addingText) {
            this.initialText = initialText;
            this.addingText = addingText;
        }
    }

    private static class SurroundingTextParams {
        public final String name;
        public final String before;
        public final String after;
        public SurroundingTextParams(final String name, final String before, final String after) {
            this.name = name;
            this.before = before;
            this.after = after;
        }
    }

    private static class ChangingTextParams {
        public final String name;
        public final String initial;
        public final String update;
        public ChangingTextParams(final String name, final String initial, final String update) {
            this.name = name;
            this.initial = initial;
            this.update = update;
        }
        public ChangingTextParams(final String name, final String update) {
            this(name, "", update);
        }
    }

    private static class InitialCursorPositionParams {
        public final String name;
        public final int start;
        public final int end;
        public InitialCursorPositionParams(final String name, final int position) {
            this(name, position, position);
        }
        public InitialCursorPositionParams(final String name, final int start, final int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
    }

    private static class NewCursorPositionParams {
        public final String name;
        public final int newCursorPosition;
        public final int newCursorIndexSanitized;
        public final int newCursorIndexRaw;
        public NewCursorPositionParams(final String name,
                                       final int newCursorPosition,
                                       final int newCursorIndexSanitized,
                                       final int newCursorIndexRaw) {
            this.name = name;
            this.newCursorPosition = newCursorPosition;
            this.newCursorIndexSanitized = newCursorIndexSanitized;
            this.newCursorIndexRaw = newCursorIndexRaw;
        }
    }

    private static class RelativePosition {
        final AddingTextPosition referencePoint;
        final int codePointOffset;
        public RelativePosition(final AddingTextPosition referencePoint,
                                final int codePointOffset) {
            this.referencePoint = referencePoint;
            this.codePointOffset = codePointOffset;
        }
    }

    //TODO: rename better
    private static class ThreePartText {
        public final String left;
        public final String center;
        public final String right;
        public final String full;
        public ThreePartText(final String left, final String center, final String right) {
            this.left = left;
            this.center = center;
            this.right = right;
            full = left + center + right;
        }
        public ThreePartText(final String text) {
            final String[] pieces = divideString(text, 3);
            this.left = pieces[0];
            this.center = pieces[1];
            this.right = pieces[2];
            full = text;
        }
    }
    //TODO: rename better
    private static class TwoPartText {
        public final String left;
        public final String right;
        public final String full;
        public TwoPartText(final String left, final String right) {
            this.left = left;
            this.right = right;
            full = left + right;
        }
        public TwoPartText(final String text) {
            final String[] pieces = divideString(text, 2);
            left = pieces[0];
            right = pieces[1];
            full = text;
        }
        public TwoPartText(final String text, final int splitCodePointIndex) {
            final String[] pieces = divideString(text, 2);
            left = getSubstring(text, 0, splitCodePointIndex);
            right = getSubstring(text, splitCodePointIndex);
            full = text;
        }
    }
    private static class SplitText {
        public final String full;
        public final String biLeft;
        public final String biRight;
        public final String triLeft;
        public final String triCenter;
        public final String triRight;
        public final int codePointCount;
        public SplitText(final String text) {
            codePointCount = codePointCount(text);
            full = text;
            if (codePointCount > 1) {
                final String[] twoPartText = divideString(text, 2);
                biLeft = twoPartText[0];
                biRight = twoPartText[1];
            } else {
                biLeft = null;
                biRight = null;
            }
            if (codePointCount > 2) {
                final String[] pieces = divideString(text, 3);
                triLeft = pieces[0];
                triCenter = pieces[1];
                triRight = pieces[2];
            } else {
                triLeft = null;
                triCenter = null;
                triRight = null;
            }
        }
    }
    private static class SplitTextRecursive implements CharSequence {
        private final CharSequence mFull;
        private SplitTextRecursive mBiLeft;
        private SplitTextRecursive mBiRight;
        private SplitTextRecursive mTriLeft;
        private SplitTextRecursive mTriCenter;
        private SplitTextRecursive mTriRight;
        private final int mCodePointCount;

        public SplitTextRecursive(final CharSequence text) {
            mCodePointCount = RichInputConnectionTestsV3.codePointCount(text.toString());
            mFull = text;
        }

        private void bisect() {
            if (mCodePointCount > 1 && mBiLeft == null) {
                final CharSequence[] pieces = divideText(mFull, 2);
                mBiLeft = new SplitTextRecursive(pieces[0]);
                mBiRight = new SplitTextRecursive(pieces[1]);
            }
        }

        private void trisect() {
            if (mCodePointCount > 2 && mTriLeft == null) {
                final CharSequence[] pieces = divideText(mFull, 3);
                mTriLeft = new SplitTextRecursive(pieces[0]);
                mTriCenter = new SplitTextRecursive(pieces[1]);
                mTriRight = new SplitTextRecursive(pieces[2]);
            }
        }

        public SplitTextRecursive biLeft() {
            bisect();
            return mBiLeft;
        }

        public SplitTextRecursive biRight() {
            bisect();
            return mBiRight;
        }

        public SplitTextRecursive triLeft() {
            trisect();
            return mTriLeft;
        }

        public SplitTextRecursive triCenter() {
            trisect();
            return mTriCenter;
        }

        public SplitTextRecursive triRight() {
            trisect();
            return mTriRight;
        }

        public CharSequence triLeftCenter() {
            trisect();
            return new SpannableStringBuilder().append(mTriLeft).append(mTriCenter);
        }

        public CharSequence triCenterRight() {
            trisect();
            return new SpannableStringBuilder().append(mTriCenter).append(mTriRight);
        }

        public int codePointCount() {
            return mCodePointCount;
        }

        @Override
        public int length() {
            return mFull.length();
        }

        @Override
        public char charAt(int index) {
            return mFull.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return mFull.subSequence(start, end);
        }

        @Override
        public String toString() {
            return mFull.toString();
        }
    }

    private static class KeyEventInfo {
        public final int keyCode;
        public final char unicodeChar;
        public final String keyName;
        public KeyEventInfo(int keyCode, char unicodeChar, String keyName) {
            this.keyCode = keyCode;
            this.unicodeChar = unicodeChar;
            this.keyName = keyName;
        }
    }
    //#endregion

    //#region state
    //TODO: (EW) support CharSequence
    private interface State {
        String getText();
        int getCursorStart();
        int getCursorEnd();
        int getCompositionStart();
        int getCompositionEnd();
        String getComposedText();
        default String getTextBeforeCursor() {
            return getText().substring(0, getCursorStart());
        }
        default String getSelectedText() {
            return getText().substring(getCursorStart(), getCursorEnd());
        }
        default String getTextAfterCursor() {
            return getText().substring(getCursorEnd());
        }
    }

    private static class CommittedState implements State {
        public final String text;
        private final int cursorStart;
        private final int cursorEnd;
        public CommittedState(final String text,
                              final int cursorStart, final int cursorEnd) {
            this.cursorStart = cursorStart;
            this.cursorEnd = cursorEnd;
            this.text = text;
            if (cursorStart > cursorEnd || cursorStart < 0 || cursorEnd > text.length()) {
                throw new IllegalArgumentException("Invalid cursor position: "
                        + cursorStart + "," + cursorEnd
                        + " text: \"" + text + "\"");
            }
        }
        public CommittedState(final String text, final int cursorPosition) {
            this(text, cursorPosition, cursorPosition);
        }
        public CommittedState(final String text) {
            this(text, text.length());
        }
        public CommittedState(final String text, final InitialCursorPositionParams cursorPosition) {
            this(text, cursorPosition.start, cursorPosition.end);
        }
        public CommittedState(final SurroundingTextParams surroundingText,
                              final String selectedText) {
            this(surroundingText.before + selectedText + surroundingText.after,
                    surroundingText.before.length(),
                    surroundingText.before.length() + selectedText.length());
        }
        public CommittedState(final String beforeText, final String selectedText,
                              final String afterText) {
            this(beforeText + selectedText + afterText,
                    beforeText.length(),
                    beforeText.length() + selectedText.length());
        }
        public CommittedState(final String beforeText, final String afterText) {
            this(beforeText, "", afterText);
        }
        public CommittedState(final ComposedState state) {
            this(state.getText(), state.getCursorStart(), state.getCursorEnd());
        }
        public CommittedState(final TwoPartText text) {
            this(text.left, "", text.right);
        }
        public CommittedState(final ThreePartText text) {
            this(text.left, text.center, text.right);
        }

        public String getText() {
            return text;
        }
        public int getCursorStart() {
            return cursorStart;
        }
        public int getCursorEnd() {
            return cursorEnd;
        }
        public int getCompositionStart() {
            return -1;
        }
        public int getCompositionEnd() {
            return -1;
        }
        public String getComposedText() {
            return null;
        }
    }

    private static class ComposedState implements State {
        private final String textBefore;
        private final String composedText;
        private final String textAfter;
        private final int cursorStart;
        private final int cursorEnd;
        public ComposedState(final String textBefore, final String composedText,
                             final String textAfter, final int cursorStart, final int cursorEnd) {
            this.cursorStart = cursorStart;
            this.cursorEnd = cursorEnd;
            this.textBefore = textBefore;
            this.composedText = composedText;
            this.textAfter = textAfter;
            if (cursorStart > cursorEnd || cursorStart < 0
                    || cursorEnd > textBefore.length() + composedText.length() + textAfter.length()) {
                throw new IllegalArgumentException("Invalid cursor position: "
                        + cursorStart + "," + cursorEnd
                        + " text: \"" + textBefore + composedText + textAfter + "\"");
            }
        }
        public ComposedState(final String textBefore, final String composedText,
                             final String textAfter, final int cursorPosition) {
            this(textBefore, composedText, textAfter, cursorPosition, cursorPosition);
        }
        public ComposedState(final String textBefore, final String composedText,
                             final String textAfter) {
            this(textBefore, composedText, textAfter, textBefore.length() + composedText.length());
        }
        public ComposedState(final String textBefore, final String composedText,
                             final String textAfter,
                             final InitialCursorPositionParams cursorPosition) {
            this(textBefore, composedText, textAfter, cursorPosition.start, cursorPosition.end);
        }
        public ComposedState(final String textBefore, final String composedText,
                             final String textAfter,
                             final ComposedTextPosition cursorPosition) {
            this(textBefore, composedText, textAfter, cursorPosition, cursorPosition);
        }
        public ComposedState(final String textBefore, final String composedText,
                             final String textAfter,
                             final ComposedTextPosition cursorStart,
                             final ComposedTextPosition cursorEnd) {
            this(textBefore, composedText, textAfter,
                    getAbsolutePosition(textBefore, composedText, textAfter, cursorStart),
                    getAbsolutePosition(textBefore, composedText, textAfter, cursorEnd));
        }
        public ComposedState(final SurroundingTextParams surroundingText,
                             final ChangingTextParams changingText,
                             final InitialCursorPositionParams initialCursorPosition) {
            this(surroundingText.before, changingText.initial, surroundingText.after,
                    initialCursorPosition);
        }
        public ComposedState(final SurroundingTextParams surroundingText,
                             final String composedText,
                             final InitialCursorPositionParams initialCursorPosition) {
            this(surroundingText.before, composedText, surroundingText.after,
                    initialCursorPosition);
        }
        public ComposedState(final String text, final int cursorStart, final int cursorEnd, final int compositionStart, final int compositionEnd) {
            this(text.substring(0, compositionStart),
                    text.substring(compositionStart, compositionEnd),
                    text.substring(compositionEnd),
                    cursorStart, cursorEnd);
        }

        public String getText() {
            return textBefore + composedText + textAfter;
        }
        public int getCursorStart() {
            return cursorStart;
        }
        public int getCursorEnd() {
            return cursorEnd;
        }
        public int getCompositionStart() {
            return textBefore.length();
        }
        public int getCompositionEnd() {
            return textBefore.length() + composedText.length();
        }
        public String getComposedText() {
            return composedText;
        }
    }
    //#endregion

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

    private enum ComposedTextPosition {
        BEGINNING_OF_TEXT,
        AFTER_BEGINNING_OF_TEXT,
        BEFORE_BEGINNING_OF_COMPOSITION,
        BEGINNING_OF_COMPOSITION,
        AFTER_BEGINNING_OF_COMPOSITION,
        BEFORE_END_OF_COMPOSITION,
        END_OF_COMPOSITION,
        AFTER_END_OF_COMPOSITION,
        BEFORE_END_OF_TEXT,
        END_OF_TEXT
    }

    private enum AddingTextPosition {
        BEGINNING_OF_FULL_TEXT,
        BEGINNING_OF_NEW_TEXT,
        END_OF_NEW_TEXT,
        END_OF_FULL_TEXT
    }

    //#region test case classes
    private static class ActionTestCase<T extends Action> extends ActionTestCaseBase {
        final boolean useBatch;
        final T actionParams;
        final boolean isExpectedChange;
        final boolean isExpectedTextChange;

        public ActionTestCase(final String testBaseName, final String testNameExtras,
                              final VariableBehaviorSettings settings,
                              final boolean useBatch,
                              final State initialState,
                              final boolean cursorPositionKnown,
                              final T actionParams,
                              final State expectedState,
                              final boolean isExpectedChange,
                              final boolean isExpectedTextChange) {
            super(testBaseName, testNameExtras, settings, initialState, cursorPositionKnown, expectedState);
            this.useBatch = useBatch;
            this.actionParams = actionParams;
            this.isExpectedChange = isExpectedChange;
            this.isExpectedTextChange = isExpectedTextChange;
            if (!cursorPositionKnown && (settings.allowExtractingText)) {
                throw new IllegalArgumentException("settings not compatible with unknown cursor position: " + settings);
            }
        }

        public ActionTestCase(final String testBaseName, final String testNameExtras,
                              final VariableBehaviorSettings settings,
                              final boolean useBatch,
                              final State initialState,
                              final boolean cursorPositionKnown,
                              final T actionParams,
                              final State expectedState,
                              final boolean isExpectedChange) {
            this(testBaseName, testNameExtras, settings, useBatch, initialState, cursorPositionKnown, actionParams,
                    expectedState, isExpectedChange, isExpectedChange);
        }

        public ActionTestCase(final String testName,
                              final Named<TestSettings> settings,
                              final boolean useBatch,
                              final State initialState,
                              final T actionParams,
                              final State expectedState) {
            this(testName, settings, useBatch, initialState, actionParams, expectedState,
                    settings.data.initialCursorPositionKnown);
        }
        public ActionTestCase(final String testName,
                              final Named<TestSettings> settings,
                              final boolean useBatch,
                              final State initialState,
                              final T actionParams,
                              final State expectedState,
                              final boolean isExpectedChange) {
            this(testName, getNameExtrasForSettings(settings, useBatch),
                    settings.data.inputConnectionSettings, useBatch,
                    initialState, settings.data.initialCursorPositionKnown, actionParams,
                    expectedState, isExpectedChange);
        }
        public ActionTestCase(final String testName,
                              final Named<TestSettings> settings,
                              final boolean useBatch,
                              final State initialState,
                              final T actionParams,
                              final State expectedState,
                              final boolean isExpectedChange,
                              final boolean isExpectedTextChange) {
            this(testName, getNameExtrasForSettings(settings, useBatch),
                    settings.data.inputConnectionSettings, useBatch,
                    initialState, settings.data.initialCursorPositionKnown, actionParams,
                    expectedState, isExpectedChange, isExpectedTextChange);
        }
        public ActionTestCase(final String testName,
                              final Named<TestSettings> settings,
                              final State initialState,
                              final T actionParams,
                              final State expectedState) {
            this(testName, settings, false, initialState, actionParams, expectedState);
        }
        public ActionTestCase(final String testName,
                              final Named<TestSettings> settings,
                              final State initialState,
                              final T actionParams,
                              final State expectedState,
                              final boolean isExpectedChange) {
            this(testName, settings, false, initialState, actionParams, expectedState,
                    isExpectedChange);
        }
        private static String getNameExtrasForSettings(final Named<TestSettings> settings,
                                                       final boolean useBatch) {
            //TODO: update the name better
            return " (fake settings: " + settings.name + "; " + (useBatch ? "" : "don't ")
                    + "use batch)";
        }
    }

    private static abstract class ActionTestCaseBase implements Groupable {
        final String testBaseName;
        final String testNameExtras;
        final VariableBehaviorSettings settings;
        final State initialState;
        final boolean cursorPositionKnown;
        final boolean delayUpdates;
        final State expectedState;

        public ActionTestCaseBase(final String testBaseName, final String testNameExtras,
                                  final VariableBehaviorSettings settings,
                                  final State initialState,
                                  final boolean cursorPositionKnown,
                                  final State expectedState) {
            this(testBaseName, testNameExtras, settings, initialState, cursorPositionKnown,
                    initialState instanceof ComposedState && !cursorPositionKnown, expectedState);
        }
        public ActionTestCaseBase(final String testBaseName, final String testNameExtras,
                                  final VariableBehaviorSettings settings,
                                  final State initialState,
                                  final boolean cursorPositionKnown,
                                  final boolean delayUpdates,
                                  final State expectedState) {
            this.testBaseName = testBaseName;
            this.testNameExtras = testNameExtras;
            this.settings = settings;
            this.initialState = initialState;
            this.cursorPositionKnown = cursorPositionKnown;
            this.delayUpdates = delayUpdates;
            this.expectedState = expectedState;
        }

        @Override
        public String getGroup() {
            return testBaseName;
        }

        @Override
        public String toString() {
            return testBaseName + testNameExtras;
        }
    }

    private static class ExternalActionTestCase extends ActionTestCaseBase {
        final ExternalAction actionParams;

        public ExternalActionTestCase(final String testName,
                                      final Named<TestSettings> settings,
                                      final State initialState,
                                      final ExternalAction actionParams,
                                      final State expectedState) {
            super(testName, " (" + settings.name + ")",
                    settings.data.inputConnectionSettings, initialState,
                    settings.data.initialCursorPositionKnown, expectedState);
            this.actionParams = actionParams;
        }
    }

    private static int getChangeStart(final ExternalAction action) {
        if (action instanceof ExternalActionSetText) {
            return ((ExternalActionSetText)action).start;
        } else {
            final ExternalActionBatch<ExternalActionSetText> actionBatch =
                    (ExternalActionBatch<ExternalActionSetText>)action;
            int start = Integer.MAX_VALUE;
            for (final ExternalActionSetText innerAction : actionBatch.actions) {
                if (innerAction.start < start) {
                    start = innerAction.start;
                }
            }
            return start;
        }
    }

    private static int getChangeEnd(final ExternalAction action) {
        if (action instanceof ExternalActionSetText) {
            return ((ExternalActionSetText)action).end;
        } else {
            final ExternalActionBatch<ExternalActionSetText> actionBatch =
                    (ExternalActionBatch<ExternalActionSetText>)action;
            int initialEnd = actionBatch.actions[0].end;
            int workingEnd = initialEnd;
            for (final ExternalActionSetText innerAction : actionBatch.actions) {
                if (innerAction.end > workingEnd) {
                    initialEnd += innerAction.end - workingEnd;
                }
                workingEnd += innerAction.text.length() - (innerAction.end - innerAction.start);
            }
            return initialEnd;
        }
    }

    private static int getLengthChange(final ExternalAction action) {
        if (action instanceof ExternalActionSetText) {
            final ExternalActionSetText setTextAction = (ExternalActionSetText)action;
            return setTextAction.text.length() - (setTextAction.end - setTextAction.start);
        } else {
            final ExternalActionBatch<ExternalActionSetText> actionBatch =
                    (ExternalActionBatch<ExternalActionSetText>)action;
//            int start = Integer.MAX_VALUE;
            int initialEnd = actionBatch.actions[0].end;
            int workingEnd = initialEnd;
            for (final ExternalActionSetText innerAction : actionBatch.actions) {
//                if (innerAction.start < start) {
//                    start = innerAction.start;
//                }
                if (innerAction.end > workingEnd) {
                    initialEnd += innerAction.end - workingEnd;
                }
                workingEnd += innerAction.text.length() - (innerAction.end - innerAction.start);
            }
            return workingEnd - initialEnd;
        }
    }

    private static abstract class ExternalAction {
        private final String mName;
        private final String mDetails;

        protected ExternalAction(final String name, final String details) {
            mName = name;
            mDetails = details;
        }

        public String getName() {
            return mName;
        }

        public String getDetails() {
            return mDetails;
        }

        public abstract void doAction(FakeInputConnection inputConnection);
    }

    private static class ExternalActionSetText extends ExternalAction {
        private final CharSequence text;
        private final int start;
        private final int end;
        public ExternalActionSetText(final CharSequence text, final int start, final int end) {
            super("ShiftCharacters", "text=\"" + text + "\", start=" + start + ", end=" + end);
            this.text = text;
            this.start = start;
            this.end = end;
        }

        @Override
        public void doAction(FakeInputConnection inputConnection) {
            inputConnection.setText(text, start, end);
        }
    }

    private static class ExternalActionBatch<T extends ExternalAction> extends ExternalAction {
        private final T[] actions;
        public ExternalActionBatch(final T[] actions) {
            super("ShiftCharacters", getDetails(actions));
            this.actions = actions;
        }

        private static <T extends ExternalAction> String getDetails(final T[] actions) {
            final StringBuilder sb = new StringBuilder();
            for (final T action : actions) {
                sb.append(action.getDetails()).append("\n");
            }
            return sb.toString();
        }

        @Override
        public void doAction(FakeInputConnection inputConnection) {
            for (final T action : actions) {
                action.doAction(inputConnection);
            }
        }
    }

    private static class ExternalActionDeleteText extends ExternalAction {
        private final int start;
        private final int end;
        public ExternalActionDeleteText(final int start, final int end) {
            super("DeleteText", "start=" + start + ", end=" + end);
            this.start = start;
            this.end = end;
        }

        @Override
        public void doAction(FakeInputConnection inputConnection) {
            inputConnection.deleteText(start, end);
        }
    }

    private static abstract class Action {
        private final String mName;
        private final String mDetails;

        protected Action(final String name, final String details) {
            mName = name;
            mDetails = details;
        }

        public String getName() {
            return mName;
        }

        public String getDetails() {
            return mDetails;
        }

        public abstract void doAction(RichInputConnection richInputConnection);
    }

    //#region specific actions
    private static class AddText extends Action {
        final String newText;
        final boolean composeNewText;
        final int newCursorPosition;
        public AddText(final String newText, final boolean composeNewText, final int newCursorPosition) {
            super("AddText",
                    "new" + (composeNewText ? "Composing" : "Committing") + "Text=\"" + newText
                            + "\", newCursorPosition=" + newCursorPosition);
            this.newText = newText;
            this.composeNewText = composeNewText;
            this.newCursorPosition = newCursorPosition;
        }

        @Override
        public void doAction(final RichInputConnection richInputConnection) {
            if (composeNewText) {
                richInputConnection.setComposingText(newText, newCursorPosition);
            } else {
                richInputConnection.commitText(newText, newCursorPosition);
            }
        }
    }

    private static class FinishComposing extends Action {
        //TODO: consider using a single instance to avoid creating unnecessary objects
        public FinishComposing() {
            super("FinishComposingText", null);
        }

        @Override
        public void doAction(final RichInputConnection richInputConnection) {
            richInputConnection.finishComposingText();
        }
    }

    private static class DeleteSelected extends Action {
        //TODO: consider using a single instance to avoid creating unnecessary objects
        public DeleteSelected() {
            super("DeleteSelectedText", null);
        }

        @Override
        public void doAction(final RichInputConnection richInputConnection) {
            //TODO: (EW) this is broken now that we deleted it to simplify matching the deletion of
            // deleteTextBeforeCursor - probably delete all of these tests
            richInputConnection.deleteSelectedText();
        }
    }

    private static class DeleteTextBefore extends Action {
        final int beforeLength;
        public DeleteTextBefore(final int beforeLength) {
            super("DeleteTextBefore", "deleteBeforeLength=" + beforeLength);
            this.beforeLength = beforeLength;
        }

        @Override
        public void doAction(final RichInputConnection richInputConnection) {
            //TODO: (EW) this is broken now that upstream deleted deleteTextBeforeCursor - probably delete all of these tests
            richInputConnection.deleteTextBeforeCursor(beforeLength);
        }
    }

    private static abstract class SendKey extends Action {
        final int keyCode;
        protected SendKey(final String name, final int keyCode) {
            super(name, null);
            this.keyCode = keyCode;
        }

        @Override
        public void doAction(final RichInputConnection richInputConnection) {
            richInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            richInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }
    }

    private static class SendUnicodeCharKey extends SendKey {
        protected SendUnicodeCharKey(final KeyEventInfo info) {
            super("SendUnicodeCharKey(" + info.keyName + ")", info.keyCode);
        }
    }

    private static class SendDeleteKey extends SendKey {
        //TODO: consider using a single instance to avoid creating unnecessary objects
        public SendDeleteKey() {
            super("SendDeleteKey", KeyEvent.KEYCODE_DEL);
        }
    }

    private static class SetSelection extends Action {
        final int start;
        final int end;
        public SetSelection(final int start, final int end) {
            super("SetSelection", "start=" + start + ", end=" + end);
            this.start = start;
            this.end = end;
        }

        @Override
        public void doAction(final RichInputConnection richInputConnection) {
            richInputConnection.setSelection(start, end);
        }
    }
    //#endregion

    private static class UnicodeStepParams {
        final String name;
        final ThreePartText text;
        final int chars;
        final boolean rightSidePointer;
        final int expectedUnicodeSteps;
        public UnicodeStepParams(final String name, final ThreePartText text, final int chars, final boolean rightSidePointer, final int expectedUnicodeSteps) {
            this.name = name;
            this.text = text;
            this.chars = chars;
            this.rightSidePointer = rightSidePointer;
            this.expectedUnicodeSteps = expectedUnicodeSteps;
        }
        public UnicodeStepParams(final String name, final String before, final String selection, final String after, final int chars, final boolean rightSidePointer, final int expectedUnicodeSteps) {
            this(name, new ThreePartText(before, selection, after), chars, rightSidePointer, expectedUnicodeSteps);
        }
        public UnicodeStepParams(final String name, final State state, final int chars, final boolean rightSidePointer, final int expectedUnicodeSteps) {
            this(name, new ThreePartText(state.getText().substring(0, state.getCursorStart()),
                            state.getText().substring(state.getCursorStart(), state.getCursorEnd()),
                            state.getText().substring(state.getCursorEnd())),
                    chars, rightSidePointer, expectedUnicodeSteps);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    //TODO: (EW) probably rename
    private static class GenericTestState {
        final String titleInfo;
        final State initialState;
        final State expectedState;
        public GenericTestState(final String titleInfo, final State initialState,
                                final State expectedState) {
            this.titleInfo = titleInfo;
            this.initialState = initialState;
            this.expectedState = expectedState;
        }
    }
    private static class GenericTestStateWithAction<T> extends GenericTestState {
        final T action;
        public GenericTestStateWithAction(final String titleInfo, final State initialState,
                                          final T action, final State expectedState) {
            super(titleInfo, initialState, expectedState);
            this.action = action;
        }
    }
    private static class GenericTestStateWithSettings extends GenericTestState {
        final Named<TestSettings> settingsOption;
        public GenericTestStateWithSettings(final String titleInfo,
                                            final Named<TestSettings> settingsOption,
                                            final State initialState, final State expectedState) {
            super(titleInfo, initialState, expectedState);
            this.settingsOption = settingsOption;
        }
    }
    private static class GenericTestStateWithSettingsAndDeletingText extends GenericTestStateWithSettings {
        final String textToDelete;
        public GenericTestStateWithSettingsAndDeletingText(final String titleInfo,
                                                           final Named<TestSettings> settingsOption,
                                                           final String textToDelete,
                                                           final State initialState,
                                                           final State expectedState) {
            super(titleInfo, settingsOption, initialState, expectedState);
            this.textToDelete = textToDelete;
        }
    }
    private static class GenericTestStateWithTextModifier extends GenericTestState {
        final FakeInputConnection.TextModifier textModifier;
        final boolean isExpectedChange;
        final boolean isExpectedTextChange;
        public GenericTestStateWithTextModifier(final String titleInfo,
                                                final FakeInputConnection.TextModifier textModifier,
                                                final State initialState, final State expectedState,
                                                final boolean isExpectedChange,
                                                final boolean isExpectedTextChange) {
            super(titleInfo, initialState, expectedState);
            this.textModifier = textModifier;
            this.isExpectedChange = isExpectedChange;
            this.isExpectedTextChange = isExpectedTextChange;
        }
    }

    private interface Groupable {
        String getGroup();
    }
    private static class Grouped<T> implements Groupable {
        final String group;
        final T data;
        public Grouped(final String group, final T data) {
            this.group = group;
            this.data = data;
        }

        @Override
        public String getGroup() {
            return group;
        }
    }
    private static class GroupedList<T extends Groupable> implements Iterable<T> {
        private GroupEntry<String, T> mFirstGroup;
        private int mCount = 0;

        public void add(T item) {
            String groupId = item.getGroup();
            GroupEntry<String, T> group;
            if (mFirstGroup == null) {
                group = new GroupEntry<>(groupId);
                mFirstGroup = group;
            } else {
                GroupEntry<String, T> curGroup = mFirstGroup;
                while (!groupsMatch(curGroup.value, groupId) && curGroup.next != null) {
                    curGroup = curGroup.next;
                }
                if (groupsMatch(curGroup.value, groupId)) {
                    group = curGroup;
                } else {
                    group = new GroupEntry<>(groupId);
                    curGroup.next = group;
                }
            }
            ItemEntry<T> itemEntry = new ItemEntry<>(item);
            if (group.lastChild == null) {
                group.firstChild = itemEntry;
            } else {
                group.lastChild.next = itemEntry;
            }
            group.lastChild = itemEntry;
            mCount++;
        }

        private boolean groupsMatch(String a, String b) {
            if (a == b) {
                return true;
            }
            if (a != null && a.equals(b)) {
                return true;
            }
            return false;
        }

        public int size() {
            return mCount;
        }

        private static class GroupEntry<TGroup, TItem> {
            final TGroup value;
            GroupEntry<TGroup, TItem> next;
            ItemEntry<TItem> firstChild;
            ItemEntry<TItem> lastChild;
            public GroupEntry(TGroup value) {
                this.value = value;
            }
        }
        private static class ItemEntry<T> {
            T value;
            ItemEntry<T> next;
            public ItemEntry(T value) {
                this.value = value;
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new GroupedListIterator();
        }

        private class GroupedListIterator implements Iterator<T> {
            private GroupEntry<String, T> mCurrentGroup;
            private ItemEntry<T> mCurrentItem;
            @Override
            public boolean hasNext() {
                if (mCurrentGroup == null) {
                    return mFirstGroup != null;
                }
                return mCurrentItem.next != null || mCurrentGroup.next != null;
            }

            @Override
            public T next() {
                if (mCurrentGroup == null) {
                    mCurrentGroup = mFirstGroup;
                    mCurrentItem = mFirstGroup.firstChild;
                } else if (mCurrentItem.next != null) {
                    mCurrentItem = mCurrentItem.next;
                } else {
                    mCurrentGroup = mCurrentGroup.next;
                    mCurrentItem = mCurrentGroup.firstChild;
                }
                return mCurrentItem.value;
            }
        }
    }
    //#endregion

    //#endregion

    //#region generic test runner
    private static class RichInputConnectionManager {
        //TODO: create getters/assert functions for necessary things instead of having this public
        public final ArrayList<UpdateSelectionCall> updateSelectionCalls = new ArrayList<>();
        public final ArrayList<Integer> expectedUpdateSelectionCalls = new ArrayList<>();
        public final ArrayList<Boolean> expectedUpdateExtractedTextCalls = new ArrayList<>();
        private FakeInputConnection fakeInputConnection;
        private RichInputConnection richInputConnection;
        private final Queue<UpdateMessage> pendingMessages = new LinkedList<>();
        private boolean delayUpdates;
        private int allUpdatesReceivedCallCount = 0;

        public void setup(final VariableBehaviorSettings settings, final String initialText,
                          final int initialCursorStart, final int initialCursorEnd,
                          final boolean delayUpdates, final boolean cursorPositionKnown) {
            richInputConnection = null;
            updateSelectionCalls.clear();
            expectedUpdateSelectionCalls.clear();
            expectedUpdateExtractedTextCalls.clear();
            this.delayUpdates = delayUpdates;
            final FakeInputConnection.FakeInputMethodManager fakeInputMethodManager = new FakeInputConnection.FakeInputMethodManager() {
                @Override
                public void updateSelection(final int oldSelStart, final int oldSelEnd,
                                            final int newSelStart, final int newSelEnd,
                                            final int candidatesStart, final int candidatesEnd) {
                    UpdateSelectionMessage message = new UpdateSelectionMessage(oldSelStart,
                            oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
                    System.out.println((RichInputConnectionManager.this.delayUpdates
                            ? "" : "briefly ")
                            + "pending updateSelection: oss=" + oldSelStart + ", ose=" + oldSelEnd
                            + ", nss=" + newSelStart + ", nse=" + newSelEnd
                            + ", cs=" + candidatesStart + ", ce=" + candidatesEnd);
                    pendingMessages.add(message);
                }

                @Override
                public void updateExtractedText(View view, int token, ExtractedText text) {
                    UpdateExtractedTextMessage message = new UpdateExtractedTextMessage(token, text);
                    System.out.println((RichInputConnectionManager.this.delayUpdates
                            ? "" : "briefly ")
                            + "pending updateExtractedText");
                    pendingMessages.add(message);
                }

                @Override
                public void processUpdates() {
                    if (!RichInputConnectionManager.this.delayUpdates) {
                        System.out.println("processing updates");
                        processPendingMessages();
                    }
                }
            };
            if (settings != null) {
                fakeInputConnection = new FakeInputConnection(fakeInputMethodManager, initialText,
                        initialCursorStart, initialCursorEnd, settings);
            } else {
                fakeInputConnection = new FakeInputConnection(fakeInputMethodManager, initialText,
                        initialCursorStart, initialCursorEnd);
            }

            richInputConnection = new RichInputConnection(new InputMethodService() {
                @Override
                public InputConnection getCurrentInputConnection() {
                    return fakeInputConnection;
                }
            });
            richInputConnection.setUpdatesReceivedHandler(new UpdatesReceivedHandler() {
                @Override
                public void onAllUpdatesReceived() {
                    allUpdatesReceivedCallCount++;
                }
            });
            if (cursorPositionKnown) {
                richInputConnection.resetState(initialCursorStart, initialCursorEnd);
            } else {
                richInputConnection.resetState(-1, -1);
            }
            richInputConnection.reloadCachesForStartingInputView();
        }

        private void setUpState(final VariableBehaviorSettings settings, final State state,
                                final boolean cursorPositionKnown, final boolean delayUpdates) {
            setUpState(settings, state, cursorPositionKnown, delayUpdates, false);
        }

        private void setUpState(final VariableBehaviorSettings settings, final State state,
                                final boolean cursorPositionKnown, final boolean delayUpdates,
                                final boolean useSetComposingRegion) {
            System.out.println("Setting up state");
            //TODO: reevaluate how the unknown cursor position case is set up - I think
            // resetCachesUponCursorMoveAndReturnSuccess always gets called in a real scenario, but
            // we're skipping it for this case
            VariableBehaviorSettings setupSettings = settings == null
                    ? null
                    : new VariableBehaviorSettings(settings).setTextModifier(null);
            if (state.getCompositionStart() >= 0) {
                final int setupInitialPosition = state.getCompositionStart();
                final String textBefore = state.getText().substring(0, state.getCompositionStart());
                final String composedText = state.getText().substring(state.getCompositionStart(), state.getCompositionEnd());
                final String textAfter = state.getText().substring(state.getCompositionEnd());
                if (useSetComposingRegion) {
                    setup(setupSettings, state.getText(), state.getCursorStart(), state.getCursorEnd(), delayUpdates, cursorPositionKnown);
                    if (cursorPositionKnown) {
                        // I'm not sure if this is a very realistic test case. the composing region
                        // should be cleared when switching IMEs (unless the previous IME
                        // misbehaves). it seems unlikely for something else to call
                        // InputConnection#setComposingRegion. at least when not delaying updates,
                        // this somewhat simulates implementing our own setComposingRegion without
                        // actually bothering to do so (since we don't actually support it, why test
                        // this?), but it does function as a simple way to prevent the composed text
                        // from being in the cache (there are some specific scenarios that could
                        // wipe various things from the cache, and this allows an easier means to
                        // force that case, even if the specific scenario may not be directly
                        // realistic).
                        //TODO: (EW) RichInputConnection#setComposingRegion is only my testing method,
                        // so it doesn't actually update state, and it probably will be thrown away
                        /*richInputConnection*/fakeInputConnection.setComposingRegion(state.getCompositionStart(),
                                state.getCompositionEnd());
                    } else {
                        throw new RuntimeException("Can't have a ComposedState by forcing the composing region while still having an unknown cursor");
                    }
                } else {
                    setup(setupSettings, textBefore + textAfter, setupInitialPosition, setupInitialPosition, delayUpdates, cursorPositionKnown);
                    if (!cursorPositionKnown) {
                        fakeInputConnection.getSettings().allowExtractingText(false);
                    }
                    //TODO: handle cursorPositionKnown? it shouldn't make a difference because the input
                    // connection should send an update with the selection or should we block that to
                    // simulate a broken input connection? that doesn't really seem valid to test.
                    // we could just start a batch and expect the general test handling to release the batch
                    // at some point (this might get confusing since the committed initial state doesn't
                    // need to do this - and probably shouldn't).
                    richInputConnection.beginBatchEdit();
                    richInputConnection.setComposingText(composedText, 1);
                    if (cursorPositionKnown) {
                        //TODO: swapping to use do the setup in the rich input connection to avoid also
                        // testing an external action - verify this is appropriate
//                    moveCursor(state.getCursorStart(), state.getCursorEnd());
                        richInputConnection.setSelection(state.getCursorStart(), state.getCursorEnd());
                        richInputConnection.endBatchEdit();
                    } else {
                        richInputConnection.endBatchEdit();
                        //TODO: (EW) this will make the internal state be wrong when doing all actions.
                        // is this a valid test?
                        // I don't think so. it made some SendKeyEventUnicodeChar tests fail, so I had
                        // this error out to catch them more specifically, but apparently over 1000
                        // tests are passing despite this seemingly invalid test. those probably should
                        // be reevaluated. I just skipped cases in SendKeyEventUnicodeChar that would
                        // hit this.
                        System.out.println("Setting the selection without going through the IME");
                        fakeInputConnection.setSelection(state.getCursorStart(), state.getCursorEnd());
//                    throw new RuntimeException("starting a test with an invalid state");
                    }
                }
            } else {
                setup(setupSettings, state.getText(), state.getCursorStart(), state.getCursorEnd(),
                        delayUpdates, cursorPositionKnown);
                if (!cursorPositionKnown) {
                    fakeInputConnection.getSettings().allowExtractingText(false);
                }
            }

            // now that the initial state is set up, unexpected text changes can be allowed to occur
            if (settings != null && settings.textModifiers != null) {
                fakeInputConnection.getSettings().setTextModifiers(settings.textModifiers);
            }

            //TODO: (EW) it might be worth validating that FakeInputConnection (and maybe also
            // RichInputConnection) actually matches the test initial state so we don't get
            // distracted trying to find a bug in what we're trying to test when the test is broken
            // to start
            System.out.println(richInputConnection.getDebugState());
            System.out.println("Finished setting up state\n");
        }

        private void replaceEditorState(State state) {
            System.out.println("Replacing state");
            fakeInputConnection.beginBatchEdit();
            fakeInputConnection.finishComposingText();
            fakeInputConnection.setSelection(0, 0);
            fakeInputConnection.deleteText(0, fakeInputConnection.getText().length());
            if (TextUtils.isEmpty(state.getComposedText())) {
                fakeInputConnection.commitText(state.getText(), 1);
            } else {
                fakeInputConnection.commitText(
                        state.getText().substring(0, state.getCompositionStart())
                                + state.getText().substring(state.getCompositionEnd()),
                        1);
                fakeInputConnection.setSelection(state.getCompositionStart(),
                        state.getCompositionStart());
                fakeInputConnection.setComposingText(state.getComposedText(), 1);
            }
            fakeInputConnection.setSelection(state.getCursorStart(), state.getCursorEnd());
            fakeInputConnection.endBatchEdit();
            System.out.println(richInputConnection.getDebugState());
            System.out.println("actual state: " + fakeInputConnection.getDebugState());
            System.out.println("Finished replacing state\n");
        }

        public void resetUpdateSelectionCalls() {
            updateSelectionCalls.clear();
            expectedUpdateSelectionCalls.clear();
            expectedUpdateExtractedTextCalls.clear();
        }

        public void runExternalAction(final ExternalActionTestCase testCase) {
            final VariableBehaviorSettings settings = testCase.settings;
            final State initialState = testCase.initialState;
            final boolean cursorPositionKnown = testCase.cursorPositionKnown;
            final boolean delayUpdates = testCase.delayUpdates;
            final ExternalAction action = testCase.actionParams;
            final State expectedState = testCase.expectedState;

            printDebugInfo(action.getName(), initialState, action.getDetails(), expectedState);

            setUpState(settings, initialState, cursorPositionKnown, delayUpdates);
            System.out.println("\n" + fakeInputConnection.getSettingsDebug()
                    + "\nBatch not relevant"
                    + "\nDelay updates: " + testCase.delayUpdates + "\n");
            updateSelectionCalls.clear();
            expectedUpdateSelectionCalls.clear();
            expectedUpdateExtractedTextCalls.clear();

            fakeInputConnection.beginBatchEdit();
            action.doAction(fakeInputConnection);
            fakeInputConnection.endBatchEdit();
            //TODO: should this be here or somewhere else?
            processPendingMessages();

            //TODO: remove this - testing - RichInputConnection needs to figure out when to update
            // the composition cache
//            richInputConnection.updateComposingTextCacheFromExtractedTextUpdates();
//            richInputConnection.updateComposingTextCacheFromMainCache();
        }

        //TODO: (EW) either delete this with the tests directly testing getTextAroundCursor or merge
        // with the other overload
        public void runGetTextAroundCursorSetup(final GetTextAroundCursorTestCase testCase) {
            final State initialState = testCase.initialState;
            final State updatedState = testCase.updatedState;
            final boolean cursorPositionKnown = testCase.cursorPositionKnown;

            printDebugInfo(testCase.testName, initialState, "", updatedState);

            // modify the main settings to block loading any text as part of the setup
            final VariableBehaviorSettings settings = new VariableBehaviorSettings(testCase.settings)
                    .blockBaseExtractText()
                    .limitReturnedText(0, false, 0);
            setUpState(settings, initialState, cursorPositionKnown, false);
            // reset the settings to match the test case
            fakeInputConnection.getSettings().allowExtractingText(testCase.settings.allowExtractingText);
            fakeInputConnection.getSettings().setGetTextLimit(testCase.settings.getTextLimit);
            fakeInputConnection.getSettings().setExtractedTextMonitorInfo(
                    testCase.settings.partialTextMonitorUpdates,
                    testCase.settings.extractMonitorTextLimit);
            System.out.println("\n" + fakeInputConnection.getSettingsDebug());
            System.out.println("\nForcing updates to cache");
            updateSelectionCalls.clear();
            expectedUpdateSelectionCalls.clear();
            expectedUpdateExtractedTextCalls.clear();
            // force certain text to be cached
            if (cursorPositionKnown) {
                for (Range range : testCase.initialCachedRanges) {
                    ExtractedText text = new ExtractedText();
                    text.text = fakeInputConnection.getText().substring(range.start(),
                            range.end() + 1);
                    text.partialStartOffset = range.start();
                    text.partialEndOffset = range.end() + 1;
                    text.selectionStart = fakeInputConnection.getSelectionStart();
                    text.selectionEnd = fakeInputConnection.getSelectionEnd();
                    richInputConnection.onUpdateExtractedText(/*token*/0, text);
                }
            } else {
                // temporarily allow getting any text to allow forcing text to be cached
                fakeInputConnection.getSettings().setGetTextLimit(Integer.MAX_VALUE);
                // don't allow extracting text since that will set the cursor positions
                fakeInputConnection.getSettings().allowExtractingText(false);
                for (Range range : testCase.initialCachedRanges) {
                    //TODO: (EW) probably should only allow text before the cursor with this method
                    // because the selection length may be determined, which could mess up a test
                    if (range.end() + 1 != testCase.initialState.getCursorStart()) {
                        //TODO: (EW) see if there is a good way to handle other forced caching
                        throw new RuntimeException("forcing initial cache for an unknown cursor "
                                + "is currently not supported unless it's only immediately before "
                                + "the selection: "
                                + range.start() + " - " + range.end() + ", cursorStart=" + testCase.initialState.getCursorStart());
                    }
                    richInputConnection.getTextAroundCursor(range.start() - range.end() - 1, 0,
                            0, false, false, false, false);
                }
                // reset the settings
                fakeInputConnection.getSettings().setGetTextLimit(testCase.settings.getTextLimit);
                fakeInputConnection.getSettings().allowExtractingText(testCase.settings.allowExtractingText);
            }
            System.out.println(richInputConnection.getDebugState());
            System.out.println("Finished forcing updates to cache\n");

            // update the state without informing the IME
            delayUpdates = true;
            replaceEditorState(updatedState);
            while (pendingMessages.size() > 0) {
                pendingMessages.remove();
            }
            delayUpdates = false;
        }

        public void runGetTextAroundCursorSetup(final GetTextTestCaseBase<?> testCase) {
            final State initialState = testCase.initialState;
            final State updatedState = testCase.updatedState;
            final boolean cursorPositionKnown = testCase.cursorPositionKnown;

            printDebugInfo(testCase.testName, initialState, "", updatedState != null ? updatedState : initialState);

            // modify the main settings to block loading any text as part of the setup
            final VariableBehaviorSettings settings = new VariableBehaviorSettings(testCase.settings)
                    .blockBaseExtractText()
                    .limitReturnedText(0, false, 0);
            // use setComposingRegion to avoid the input connection from caching the text from the
            // composition
            setUpState(settings, initialState, cursorPositionKnown, false, true);
            // reset the settings to match the test case
            fakeInputConnection.getSettings().allowExtractingText(testCase.settings.allowExtractingText);
            fakeInputConnection.getSettings().setGetTextLimit(testCase.settings.getTextLimit);
            fakeInputConnection.getSettings().setExtractedTextMonitorInfo(
                    testCase.settings.partialTextMonitorUpdates,
                    testCase.settings.extractMonitorTextLimit);
            System.out.println("\n" + fakeInputConnection.getSettingsDebug());
            updateSelectionCalls.clear();
            expectedUpdateSelectionCalls.clear();
            expectedUpdateExtractedTextCalls.clear();
            // force certain text to be cached
            if (testCase.initialCachedRanges != null) {
                System.out.println("\nForcing updates to cache");
                if (cursorPositionKnown) {
                    for (Range range : testCase.initialCachedRanges) {
                        ExtractedText text = new ExtractedText();
                        text.text = fakeInputConnection.getText().substring(range.start(),
                                range.end() + 1);
                        text.partialStartOffset = range.start();
                        text.partialEndOffset = range.end() + 1;
                        text.selectionStart = fakeInputConnection.getSelectionStart();
                        text.selectionEnd = fakeInputConnection.getSelectionEnd();
                        richInputConnection.onUpdateExtractedText(/*token*/0, text);
                    }
                } else {
                    // temporarily allow getting any text to allow forcing text to be cached
                    fakeInputConnection.getSettings().setGetTextLimit(Integer.MAX_VALUE);
                    // don't allow extracting text since that will set the cursor positions
                    fakeInputConnection.getSettings().allowExtractingText(false);
                    for (Range range : testCase.initialCachedRanges) {
                        //TODO: (EW) probably should only allow text before the cursor with this method
                        // because the selection length may be determined, which could mess up a test
                        if (range.end() + 1 != testCase.initialState.getCursorStart()) {
                            //TODO: (EW) see if there is a good way to handle other forced caching
                            throw new RuntimeException("forcing initial cache for an unknown cursor "
                                    + "is currently not supported unless it's only immediately before "
                                    + "the selection: "
                                    + range.start() + " - " + range.end() + ", cursorStart=" + testCase.initialState.getCursorStart());
                        }
                        richInputConnection.getTextAroundCursor(range.start() - range.end() - 1, 0,
                                0, false, false, false, false);
                    }
                    // reset the settings
                    fakeInputConnection.getSettings().setGetTextLimit(testCase.settings.getTextLimit);
                    fakeInputConnection.getSettings().allowExtractingText(testCase.settings.allowExtractingText);
                }
                System.out.println(richInputConnection.getDebugState());
                System.out.println("Finished forcing updates to cache\n");
            }

            // update the state without informing the IME
            delayUpdates = true;
            if (updatedState != null) {
                replaceEditorState(updatedState);
            }
            while (pendingMessages.size() > 0) {
                pendingMessages.remove();
            }
            delayUpdates = false;
        }

        //TODO: if the update isn't going to be expected, we might need to also test with a batch
        public <T extends Action> void startRunAction(final ActionTestCase<T> testCase) {
            final VariableBehaviorSettings settings = testCase.settings;
            final State initialState = testCase.initialState;
            final boolean cursorPositionKnown = testCase.cursorPositionKnown;
            final boolean delayUpdates = testCase.delayUpdates;
            final Action action = testCase.actionParams;
            final State expectedState = testCase.expectedState;

            System.out.println(testCase.testBaseName + testCase.testNameExtras + "\n");

            printDebugInfo(action.getName(), initialState, action.getDetails(), expectedState);

            setUpState(settings, initialState, cursorPositionKnown, delayUpdates);
            System.out.println("\n" + fakeInputConnection.getSettingsDebug()
                    + "\nBatch: " + testCase.useBatch
                    + "\nIs expected change: " + testCase.isExpectedChange
                    + "\nIs expected text change: " + testCase.isExpectedTextChange
                    + "\nDelay updates: " + testCase.delayUpdates + "\n");
            updateSelectionCalls.clear();
            expectedUpdateSelectionCalls.clear();
            expectedUpdateExtractedTextCalls.clear();

            if (testCase.useBatch) {
                richInputConnection.beginBatchEdit();
            }

            action.doAction(richInputConnection);

            System.out.println(richInputConnection.getDebugState());
            System.out.println("Finished running action\n");

            //TODO: should this be here?
            processPendingMessages();
        }

        public void processPendingMessages() {
            System.out.println("processPendingMessages");
            while (pendingMessages.size() > 0) {
                pendingMessages.remove().process();
            }
        }

        public UpdateMessage processNextPendingMessage() {
            System.out.println("processNextPendingMessage");
            if (pendingMessages.size() > 0) {
                UpdateMessage message = pendingMessages.remove();
                message.process();
                return message;
            }
            return null;
        }

        public <T extends Action> void verifyTextCache(final ActionTestCaseBase testCase) {
            final State expectedState = testCase.expectedState;

            verifyTextCache(expectedState.getText().substring(0, expectedState.getCursorStart()),
                    expectedState.getText().substring(expectedState.getCursorStart(), expectedState.getCursorEnd()),
                    expectedState.getText().substring(expectedState.getCursorEnd()), false);
        }

        public <T extends Action> void verifyCompositionCache(final ActionTestCaseBase testCase) {
            final State initialState = testCase.initialState;
            final State expectedState = testCase.expectedState;
            //TODO: don't do this here - figure out how the composition cache should be updated
//            richInputConnection.updateComposingTextCacheFromMainCache();

//            assertEquals("composed text", expectedState.getComposedText(), richInputConnection.getCashedComposition());
            final CompositionState compositionState = richInputConnection.getCompositionState();
            if (expectedState.getComposedText() == null) {
//                assertEquals("composed text length", 0, compositionState.compositionLength);
                assertNull("composed text", compositionState.compositionText);
            } else {
//                assertEquals("composed text length", expectedState.getComposedText().length(), compositionState.compositionLength);
            }

            if (/*(compositionState.compositionText == null
                    || compositionState.compositionText.indexOf(NONCHARACTER_CODEPOINT_PLACEHOLDER) >= 0)
                    && */testCase.settings.getTextLimit < Integer.MAX_VALUE
                    && (testCase.settings.partialTextMonitorUpdates
                    || testCase.settings.extractMonitorTextLimit < Integer.MAX_VALUE)
                    && (expectedState.getCompositionEnd() > expectedState.getCursorEnd() + testCase.settings.getTextLimit
                    || expectedState.getCompositionStart() < expectedState.getCursorStart() - testCase.settings.getTextLimit)) {
                if (compositionState.compositionText != null) {
                    final String expectedComposition = expectedState.getComposedText();
                    if (expectedComposition != null) {
                        if (expectedComposition.length() != compositionState.compositionText.length()) {
//                            throw new RuntimeException(compositionState.compositionText + " != " + expectedComposition);
                            assertEquals("composed text", expectedState.getComposedText(), compositionState.compositionText);
                        }
                        for (int i = 0; i < expectedComposition.length(); i++) {
                            if (compositionState.compositionText.charAt(i) != NONCHARACTER_CODEPOINT_PLACEHOLDER
                                    && compositionState.compositionText.charAt(i) != expectedComposition.charAt(i)) {
//                                throw new RuntimeException(compositionState.compositionText + " != " + expectedComposition);
                                assertEquals("composed text", expectedState.getComposedText(), compositionState.compositionText);

                            }
                        }
                    } else {
//                        assertEquals("composed text length", 0, compositionState.compositionLength);
                        assertNull("composed text", compositionState.compositionText);
                    }
                }

                if (testCase instanceof ExternalActionTestCase) {
                    final ExternalActionTestCase externalActionTestCase = (ExternalActionTestCase)testCase;
                    //TODO: see if it's possible to compare the initial and expected
                    // cursor/composition instead of the actionParams for determining what might be
                    // unknowable
                    // alternatively have a flag in the test case for whether the composition should
                    // be known
                    final int changeStart = getChangeStart(externalActionTestCase.actionParams);
                    final int changeEnd = getChangeEnd(externalActionTestCase.actionParams);
                    final int lengthChange = expectedState.getText().length() - initialState.getText().length();
                    System.out.println("changeStart=" + changeStart + ", changeEnd=" + changeEnd + ", lengthChange=" + lengthChange);
                    System.out.println("compositionStart=" + initialState.getCompositionStart()
                            + ", compositionEnd=" + initialState.getCompositionEnd()
                            + ", cursorStart=" + initialState.getCursorStart()
                            + ", cursorEnd=" + initialState.getCursorEnd());
                    if (initialState.getCompositionStart() > initialState.getCursorEnd()) {
                        if (changeStart >= initialState.getCompositionStart()
                                && changeEnd <= initialState.getCompositionEnd()
                                && (changeStart != initialState.getCompositionStart()
                                    || changeEnd != initialState.getCompositionEnd())
                                && (changeStart == initialState.getCompositionStart()
                                    || changeStart > initialState.getCursorEnd() + testCase.settings.getTextLimit)
                                && lengthChange != 0
                                && ((!externalActionTestCase.settings.updateSelectionAfterExtractedText
                                        && (changeEnd < initialState.getCompositionEnd()
                                            || externalActionTestCase.settings.partialTextMonitorUpdates))
                                    || (externalActionTestCase.settings.updateSelectionAfterExtractedText
                                        && changeEnd < initialState.getCompositionEnd()))
                        ) {
                            // text changed in the composition with a changed length but not the
                            // whole composition was changed
                            System.out.println("skipping text check - can't be certain of unchanged text in changed composition after the cursor");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                        if (changeEnd <= initialState.getCompositionStart()
                                && lengthChange != 0
                                && (!externalActionTestCase.settings.updateSelectionAfterExtractedText
                                    || !externalActionTestCase.settings.partialTextMonitorUpdates
                                    || changeEnd > initialState.getCursorEnd()
                                    || (changeStart < initialState.getCursorStart()
                                        && changeEnd > initialState.getCursorStart()
                                        && changeEnd < initialState.getCompositionEnd()))) {
                            // text changed before the composition
                            System.out.println("skipping text check - can't be certain of unchanged text in composition from change before the composition after the cursor");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                        if (changeStart < initialState.getCompositionStart()
                                && changeEnd > initialState.getCompositionStart()
                                && changeEnd < initialState.getCompositionEnd()
                                && lengthChange != 0) {
                            // text changed through the beginning of the composition
                            System.out.println("skipping text check - can't be certain of unchanged text in composition from change through the composition start after the cursor");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                        if (expectedState.getComposedText() != null
                                && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                                && changeEnd < initialState.getCompositionEnd()
                                && lengthChange != 0) {
                            // text changed before the composition end
                            System.out.println("skipping text check - ");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                    } else {
                        if (changeStart >= initialState.getCompositionStart()
                                && changeEnd <= initialState.getCompositionEnd()
                                && (changeStart != initialState.getCompositionStart()
                                    || changeEnd != initialState.getCompositionEnd())
                                && (changeEnd == initialState.getCompositionEnd()
                                    || changeEnd < initialState.getCursorStart() - testCase.settings.getTextLimit)
                                && lengthChange != 0
                                && !externalActionTestCase.settings.updateSelectionAfterExtractedText
                                && (changeStart > initialState.getCompositionStart()
                                    || externalActionTestCase.settings.partialTextMonitorUpdates)) {
                            // text changed in the composition with a changed length but not the
                            // whole composition was changed and cursor updated first and
                            //   the change doesn't include the beginning of the composition
                            //   or
                            //   the update is partial
                            System.out.println("skipping text check - can't be certain of unchanged text in changed composition before the cursor");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                        if (changeEnd <= initialState.getCompositionStart()
                                && !externalActionTestCase.settings.updateSelectionAfterExtractedText
                                && externalActionTestCase.settings.partialTextMonitorUpdates
                                && lengthChange != 0) {
                            // text changed before the composition
                            System.out.println("skipping text check - can't be certain of unchanged text in composition from change before the composition");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                        if (changeStart < initialState.getCompositionStart()
                                && changeEnd > initialState.getCompositionStart()
                                && changeEnd < initialState.getCompositionEnd()
                                && lengthChange != 0
                                && externalActionTestCase.settings.partialTextMonitorUpdates
                                && !externalActionTestCase.settings.updateSelectionAfterExtractedText) {
                            // text changed through the beginning of the composition
                            System.out.println("skipping text check - can't be certain of unchanged text in composition from change through the composition start");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                        if (changeStart > initialState.getCompositionStart()
                                && changeStart < initialState.getCompositionEnd()
                                && changeEnd > initialState.getCompositionEnd()
                                && lengthChange != 0
                                && !externalActionTestCase.settings.updateSelectionAfterExtractedText) {
                            // text changed through the end of the composition
                            System.out.println("skipping text check - can't be certain of unchanged text in composition from change through the composition end");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                    }
                    if (expectedState.getComposedText() != null
                            && expectedState.getCursorEnd() < expectedState.getCompositionStart()
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                            && changeStart >= initialState.getCursorEnd()
                            && changeEnd < initialState.getCompositionEnd()
                            && (lengthChange != 0 || !testCase.settings.partialTextMonitorUpdates)) {
                        System.out.println("skipping text check - can't be certain of the composition after a change after the cursor and before the end of the composition");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    //TODO: these might be knowable
                    if (expectedState.getComposedText() != null
                            && expectedState.getCompositionStart() < expectedState.getCursorStart() - testCase.settings.getTextLimit
                            && testCase.settings.updateSelectionAfterExtractedText
                            && testCase.settings.partialTextMonitorUpdates
                            && changeStart < initialState.getCursorStart()
                            && changeEnd > initialState.getCursorStart()
                            && changeStart > initialState.getCompositionStart()) {
                        System.out.println("skipping text check - can't be certain of the composition when editing through the cursor start");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    //TODO: these might be knowable
                    if (expectedState.getComposedText() != null
                            && expectedState.getCompositionStart() < expectedState.getCursorStart() - testCase.settings.getTextLimit
                            && testCase.settings.updateSelectionAfterExtractedText
                            && testCase.settings.partialTextMonitorUpdates
                            && changeStart > initialState.getCursorStart()
                            && changeStart < initialState.getCursorEnd()
                            && changeEnd > initialState.getCursorEnd()) {
                        System.out.println("skipping text check - can't be certain of the composition when editing through the cursor end");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    //TODO: these might be knowable
                    if (expectedState.getComposedText() != null
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                            && !testCase.settings.partialTextMonitorUpdates
                            && changeEnd <= initialState.getCursorStart()
                            && initialState.getCursorStart() == expectedState.getCursorStart()) {
                        System.out.println("skipping text check - can't be certain of the composition when text before the cursor changes");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (expectedState.getComposedText() != null
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                            && !testCase.settings.partialTextMonitorUpdates
                            && changeStart >= initialState.getCursorStart()
                            && changeEnd <= initialState.getCursorEnd()
                            && initialState.getCursorStart() == expectedState.getCursorStart()) {
                        System.out.println("skipping text check - can't be certain of the composition when text in cursor before the composition changed");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (initialState.getComposedText() != null
                            && expectedState.getComposedText() != null
                            && ((
                                    expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                                    && (changeEnd < expectedState.getCompositionEnd()
                                            || (expectedState.getCursorEnd() + testCase.settings.getTextLimit < changeStart
                                                    && testCase.settings.partialTextMonitorUpdates))
                            ) || (
                                    expectedState.getCompositionStart() < expectedState.getCursorStart() - testCase.settings.getTextLimit
                                    && (changeStart > expectedState.getCompositionStart()
                                            || (changeEnd < expectedState.getCursorStart() - testCase.settings.getTextLimit
                                                    && testCase.settings.partialTextMonitorUpdates))
                            )) && initialState.getCursorStart() != expectedState.getCursorStart()) {
                        System.out.println("skipping text check - can't be certain of the composition when there is an unexpected cursor change and the missing composition isn't returned in the extracted text");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (!testCase.cursorPositionKnown && initialState.getComposedText() != null
                            && expectedState.getComposedText() != null
                            && ((
                                    expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                                    && (changeEnd < expectedState.getCompositionEnd()
                                            || (expectedState.getCursorEnd() + testCase.settings.getTextLimit < changeStart
                                                    && testCase.settings.partialTextMonitorUpdates))
                            ) || (
                                    expectedState.getCompositionStart() < expectedState.getCursorStart() - testCase.settings.getTextLimit
                                    && (changeStart > expectedState.getCompositionStart()
                                            || (changeEnd < expectedState.getCursorStart() - testCase.settings.getTextLimit
                                                    && testCase.settings.partialTextMonitorUpdates))
                            ))) {
                        System.out.println("skipping text check - can't be certain of the composition when the cursor position isn't known and the missing composition isn't returned in the extracted text");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                } else if (testCase instanceof ActionTestCase) {
                    final ActionTestCase actionTestCase = (ActionTestCase)testCase;
                    //TODO: it might be possible to keep the composition in this case. this might
                    // not be completely certain, but it might be worth keeping the composition if
                    // everything else looks fine to avoid the first character entered in a text box
                    // from immediately committing
                    if (initialState.getComposedText() == null
                            && expectedState.getComposedText() != null
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                            && testCase.settings.updateSelectionAfterExtractedText
                            && !testCase.settings.partialTextMonitorUpdates
                            && !testCase.cursorPositionKnown) {
                        System.out.println("skipping text check - can't be certain of composition after the cursor when the cursor is unknown");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (actionTestCase.actionParams instanceof AddText
                            && actionTestCase.settings.textModifiers.length > 0
                            && expectedState.getComposedText() != null
                            && ((AddText)actionTestCase.actionParams).newText.length() != expectedState.getComposedText().length()) {
                        if (initialState.getComposedText() != null) {//TODO: this might also need limiting based on how text is extracted
                            System.out.println("skipping text check - can't be certain of unexpected composition when only part of the composition change is returned");
                            checkPotentiallyUnknowableComposition(expectedState, compositionState);
                            return;
                        }
                    }
                    if (initialState.getComposedText() == null
                            && expectedState.getComposedText() != null
                            && actionTestCase.actionParams instanceof AddText
                            && expectedState.getComposedText().equals(((AddText)((ActionTestCase<?>) testCase).actionParams).newText)) {

                    } else if (expectedState.getComposedText() != null
                            && !actionTestCase.cursorPositionKnown
                            && !actionTestCase.settings.updateSelectionAfterExtractedText) {
                        //TODO: I'm not certain that this (or at least some of these) isn't unknowable - adding for now to validate the rest works
//                        System.out.println("skipping text check");
//                        if (compositionState.compositionText != null
//                                && compositionState.compositionText.indexOf(NONCHARACTER_CODEPOINT_PLACEHOLDER) < 0) {
//                            throw new RuntimeException("composition known: " + compositionState.compositionText);
//                        }
//                        return;
                    }
                    if (((actionTestCase.actionParams instanceof DeleteTextBefore
                            && ((DeleteTextBefore)actionTestCase.actionParams).beforeLength > 0)
                            || (actionTestCase.actionParams instanceof SendDeleteKey
                            && initialState.getCursorStart() == initialState.getCursorEnd()))
                            && expectedState.getComposedText() != null
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()) {
                        System.out.println("skipping text check - can't be certain of composition after where text was deleted before the cursor");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if ((actionTestCase.actionParams instanceof DeleteSelected
                            || actionTestCase.actionParams instanceof SendDeleteKey)
                            && initialState.getCursorStart() < initialState.getCursorEnd()
                            && expectedState.getComposedText() != null
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()) {
                        System.out.println("skipping text check - can't be certain of composition after where text was deleted in the selection");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (actionTestCase.actionParams instanceof SendUnicodeCharKey
                            && initialState.getCursorEnd() - initialState.getCursorStart() > 1
                            && expectedState.getComposedText() != null
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()) {
                        System.out.println("skipping text check - can't be certain of composition after where a character replaced the selection");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (actionTestCase.actionParams instanceof SendUnicodeCharKey
                            && initialState.getComposedText() != null
                            && expectedState.getComposedText() != null
                            && testCase.settings.textModifiers.length > 0
                            && !initialState.getComposedText().equals(expectedState.getComposedText())
                            && initialState.getComposedText().length() - initialState.getSelectedText().length() + 1 != expectedState.getComposedText().length()) {
                        System.out.println("skipping text check - can't be certain of unexpected composition length change");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (actionTestCase.actionParams instanceof SendUnicodeCharKey
                            && initialState.getComposedText() != null
                            && expectedState.getComposedText() != null
                            && (expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                                    || expectedState.getCursorStart() - testCase.settings.getTextLimit > expectedState.getCompositionStart())
                            && !testCase.cursorPositionKnown) {
                        System.out.println("skipping text check - can't be certain of composition away from an unknown cursor");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                    if (actionTestCase.actionParams instanceof AddText
                            && initialState.getComposedText() != null
                            && expectedState.getComposedText() != null
                            && testCase.settings.textModifiers.length > 0
                            && expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                            && (initialState.getComposedText().length() != expectedState.getComposedText().length()
                                    || !testCase.settings.partialTextMonitorUpdates)) {
                        System.out.println("skipping text check - can't be certain of slightly modified composition when the cursor is moved before it");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }

                    if (initialState.getComposedText() != null && expectedState.getComposedText() != null
                            && actionTestCase.actionParams instanceof AddText
                            && !((AddText)actionTestCase.actionParams).newText.equals(expectedState.getComposedText())
                            && !testCase.cursorPositionKnown
                            && (initialState.getCursorStart() != expectedState.getCursorStart() || testCase.settings.updateSelectionAfterExtractedText)
                            && ((
                                    expectedState.getCompositionStart() < expectedState.getCursorStart() - testCase.settings.getTextLimit
                                            && matchingLength(initialState.getComposedText(), ((AddText)actionTestCase.actionParams).newText, false) > 0
                            ) || (
                                    expectedState.getCursorEnd() + testCase.settings.getTextLimit < expectedState.getCompositionEnd()
                                            && matchingLength(initialState.getComposedText(), ((AddText)actionTestCase.actionParams).newText, true) > 0))
                    ) {
                        // only for the unknown cursor because the original composition would have
                        // been dropped from the cache when getting the first cursor position, but
                        // the delayed calls wouldn't have added the composition back to the cache
                        // appropriately due to the cursor position shifting and the delayed updates
                        // updating the wrong place and needing to get cleared because it doesn't
                        // match the current text or recognizing that the updates might be out of
                        // date
                        //TODO: might need more limits on this: initial composition new cursor
                        // position is 0 (or at least partially in range of the get text limit),
                        // composition update new cursor position (unsure whether it needs to be the
                        // absolute position changing or just a change in the relative value)
                        //TODO: verify this isn't a bug and test with a shorter expected composition
                        // length and different new cursor positions
                        System.out.println("skipping text check - can't be certain of updated composition when only part of it is returned");
                        checkPotentiallyUnknowableComposition(expectedState, compositionState);
                        return;
                    }
                }
            }
//            if (/*compositionState.compositionText == null
//                    && */testCase.settings.getTextLimit < Integer.MAX_VALUE
//                    && (expectedState.getCompositionEnd() > expectedState.getCursorEnd() + testCase.settings.getTextLimit)) {
//                //TODO: nothing to assert?
//            } else if (testCase.settings.getTextLimit < Integer.MAX_VALUE
//                    && expectedState.getCompositionStart() < expectedState.getCursorStart() - testCase.settings.getTextLimit
//                    && testCase.settings.partialTextMonitorUpdates//TODO: unsure if this is required
//                    && testCase.settings.updateSelectionAfterExtractedText//TODO: unsure if this is required
//                    && testCase instanceof ExternalActionTestCase
//                    && ((ExternalActionTestCase)testCase).actionParams instanceof ExternalActionSetText
//                    && ((ExternalActionSetText) ((ExternalActionTestCase)testCase).actionParams).end < expectedState.getCursorStart()
//                    && ((ExternalActionSetText) ((ExternalActionTestCase)testCase).actionParams).end < expectedState.getCompositionEnd()
//                    && ((ExternalActionSetText) ((ExternalActionTestCase)testCase).actionParams).end - ((ExternalActionSetText) ((ExternalActionTestCase)testCase).actionParams).start != ((ExternalActionSetText) ((ExternalActionTestCase)testCase).actionParams).text.length()) {
//                //TODO: verify this isn't skipping tests that would pass
//                if (expectedState.getComposedText().equals(compositionState.compositionText)) {
//                    throw new RuntimeException("valid test case: " + expectedState.getComposedText());
//                }
//                //TODO: nothing to assert?
//            } else {
//                if (compositionState.compositionText == null) {
////                    throw new RuntimeException("null != " + expectedState.getComposedText());
//                } else {
//                    final String expectedComposition = expectedState.getComposedText();
//                    if (expectedComposition != null) {
//                        if (expectedComposition.length() != compositionState.compositionText.length()) {
//                            throw new RuntimeException(compositionState.compositionText + " != " + expectedComposition);
//                        }
//                        for (int i = 0; i < expectedComposition.length(); i++) {
//                            if (compositionState.compositionText.charAt(i) != NONCHARACTER_CODEPOINT_PLACEHOLDER
//                                    && compositionState.compositionText.charAt(i) != expectedComposition.charAt(i)) {
//                                throw new RuntimeException(compositionState.compositionText + " != " + expectedComposition);
//                            }
//                        }
//                    }
//                }
            if ((compositionState.compositionText == null && expectedState.getComposedText() != null)
                    || (compositionState.compositionText != null
                    && compositionState.compositionText.indexOf(NONCHARACTER_CODEPOINT_PLACEHOLDER) >= 0)) {
                throw new RuntimeException(compositionState.compositionText + " != " + expectedState.getComposedText()
                        + ", cursorStart=" + compositionState.cursorStart
                        + ", cursorEnd=" + compositionState.cursorEnd);
            }
                assertEquals("composed text", expectedState.getComposedText(), compositionState.compositionText);
//            }
        }

        //TODO: (EW) this should be reevaluated. with the refactor of tracking the history of
        // updates, setting this to true causes some tests to fail. I haven't investigated whether
        // this is due to buggy code or that the cases that were previously unknowable are now
        // knowable with this method. in either case, it's probably best to not error on this. there
        // really should be other tests that point out the flaw in retaining cached text in certain
        // cases rather than this hard-coded assumption based the logic used at one point in time.
        private static final boolean ERROR_ON_KNOWN_POTENTIALLY_UNKNOWABLE_COMPOSITION = false;
        private void checkPotentiallyUnknowableComposition(State expectedState, CompositionState compositionState) {
            if (compositionState.compositionText != null
                    && compositionState.compositionText.indexOf(NONCHARACTER_CODEPOINT_PLACEHOLDER) < 0) {
                if (ERROR_ON_KNOWN_POTENTIALLY_UNKNOWABLE_COMPOSITION) {
                    throw new RuntimeException("composition known: " + compositionState.compositionText);
                } else {
                    assertEquals("composed text", expectedState.getComposedText(), compositionState.compositionText);
                }
            }
        }

        public <T extends Action> void verifyActualText(final ActionTestCaseBase testCase) {
            final State expectedState = testCase.expectedState;

            assertEquals("full text", expectedState.getText(), fakeInputConnection.getText());
            assertEquals("composing text", expectedState.getComposedText(), fakeInputConnection.getComposingText());
            assertEquals("selection start", expectedState.getCursorStart(), fakeInputConnection.getSelectionStart());
            assertEquals("selection end", expectedState.getCursorEnd(), fakeInputConnection.getSelectionEnd());
            assertEquals("composition start", expectedState.getCompositionStart(), fakeInputConnection.getCompositionStart());
            assertEquals("composition end", expectedState.getCompositionEnd(), fakeInputConnection.getCompositionEnd());
        }

        public <T extends Action> void finishAction(final ActionTestCase<T> testCase) {
            if (testCase.useBatch) {
                richInputConnection.endBatchEdit();
            } else {
                // force the update selection call in case the input connection happened to skip the
                // call (likely because the selection didn't change) to ensure we can verify whether
                // the current state is expected
                fakeInputConnection.forceUpdateSelectionCall();
            }
            processPendingMessages();

            System.out.println("\nWaiting for update timer");
            //TODO: (EW) actually test the timer rather than always directly calling this
            richInputConnection.checkLostUpdates();

            System.out.println("end state: " + richInputConnection.getDebugState());
        }

        public <T extends Action> void verifyUpdateSelectionCall(final ActionTestCase<T> testCase) {
            if (testCase.useBatch) {
                final int expectedCount;
                if (testCase.delayUpdates && testCase.initialState instanceof ComposedState) {
                    if (testCase.cursorPositionKnown) {
                        // the update for the initial composition will also be included because it
                        // will be delayed, so it can't be cleared after the initial setup
                        expectedCount = 2;
                    } else {
                        // the updates for the initial composition and the external cursor movement
                        // will also be included because it will be delayed, so it can't be cleared
                        // after the initial setup
                        expectedCount = 3;
                    }
                } else {
                    expectedCount = 1;
                }
                //TODO: if the input connection sees no change from the sum of the actions in the
                // batch, maybe it would skip the update selection call, so it might be fine to
                // allow no calls. we might also need to validate that the selection isn't expected
                // to be changing.
                assertEquals("update selection call count", expectedCount,
                        updateSelectionCalls.size());
            } else {
                assertNotEquals("update selection call count", 0, updateSelectionCalls.size());
            }
            verifyUpdateSelectionCallInternal(testCase);
        }
        public <T extends Action> void verifyUpdateSelectionCall(final ActionTestCaseBase testCase) {
            assertNotEquals("update selection call count", 0, updateSelectionCalls.size());
            verifyUpdateSelectionCallInternal(testCase);
        }
        public <T extends Action> void verifyUpdateSelectionCallInternal(final ActionTestCaseBase testCase) {
            final State expectedState = testCase.expectedState;

            final UpdateSelectionCall lastCall = updateSelectionCalls.get(updateSelectionCalls.size() - 1);
            assertEquals("selection start", expectedState.getCursorStart(), lastCall.newSelStart);
            assertEquals("selection end", expectedState.getCursorEnd(), lastCall.newSelEnd);
            assertEquals("composition start", expectedState.getCompositionStart(), lastCall.candidatesStart);
            assertEquals("composition end", expectedState.getCompositionEnd(), lastCall.candidatesEnd);
        }

        public <T extends Action> void verifyExpectedUpdateSelectionCalls(final ActionTestCase<T> testCase) {
            final boolean isExpectedUpdate = testCase.isExpectedChange;

            if (testCase.useBatch) {
                //TODO: reduce duplicate code with verifyUpdateSelectionCall
                final int expectedCount;
                if (testCase.delayUpdates && testCase.initialState instanceof ComposedState) {
                    if (testCase.cursorPositionKnown) {
                        // the update for the initial composition will also be included because it
                        // will be delayed, so it can't be cleared after the initial setup
                        expectedCount = 2;
                    } else {
                        // the updates for the initial composition and the external cursor movement
                        // will also be included because it will be delayed, so it can't be cleared
                        // after the initial setup
                        expectedCount = 3;
                    }
                } else {
                    expectedCount = 1;
                }
                //TODO: if the input connection sees no change from the sum of the actions in the
                // batch, maybe it would skip the update selection or extracted text call, so it
                // might be good to force the call if none were already made. before doing this, we
                // might need to validate that the selection isn't expected to be changing.
                assertEquals("expected update selection call count", expectedCount, expectedUpdateSelectionCalls.size());
//                assertEquals("expected update extracted text call count", 1, expectedUpdateExtractedTextCalls.size());
                int updateResult = expectedUpdateSelectionCalls.get(expectedUpdateSelectionCalls.size() - 1);
                boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
                boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
                final boolean bothExpected = !updateImpactedSelection
                        && (expectedUpdateExtractedTextCalls.size() == 0
                        || expectedUpdateExtractedTextCalls.get(expectedUpdateExtractedTextCalls.size() - 1));
                assertEquals("expected update", isExpectedUpdate, bothExpected);
            } else {
                assertNotEquals("expected update selection call count", 0, expectedUpdateSelectionCalls.size());
//                assertNotEquals("expected update extracted text call count", 0, expectedUpdateExtractedTextCalls.size());
                if (isExpectedUpdate) {
                    for (int i = 0; i < expectedUpdateSelectionCalls.size(); i++) {
                        int updateResult = expectedUpdateSelectionCalls.get(i);
                        boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
                        final boolean updateExpected = !updateImpactedSelection;
                        if (i == 0 && testCase.initialState instanceof ComposedState
                                && !testCase.cursorPositionKnown) {
                            // to keep the position unknown with a composed state, the update from
                            // the creation of the composition will be included and will always be
                            // unexpected due to having an unknown state. even if extracted text is
                            // updated first, that won't update the composition, so the selection
                            // update will still be unexpected.
                            assertFalse("update selection unexpected (" + i + " of "
                                    + expectedUpdateSelectionCalls.size() + ")", updateExpected);
                        } else {
                            assertTrue("update selection expected (" + i + " of "
                                    + expectedUpdateSelectionCalls.size() + ")", updateExpected);
                        }
                    }
                    for (int i = 0; i < expectedUpdateExtractedTextCalls.size(); i++) {
                        final boolean updateExpected = expectedUpdateExtractedTextCalls.get(i);
                        if (i == 0 && testCase.initialState instanceof ComposedState
                                && !testCase.cursorPositionKnown
                                && testCase.settings.updateSelectionAfterExtractedText) {
                            // to keep the position unknown with a composed state, the update from
                            // the creation of the composition will be included and as long as the
                            // selection wasn't updated first, this will be unexpected due to having
                            // an unknown state
                            assertFalse("update selection unexpected (" + i + " of "
                                    + expectedUpdateSelectionCalls.size() + ")", updateExpected);
                        } else {
                            assertTrue("update extracted text expected (" + i + " of "
                                    + expectedUpdateExtractedTextCalls.size() + ")",
                                    updateExpected);
                        }
                    }
                } else {
                    boolean allExpected = true;
                    for (final int updateResult : expectedUpdateSelectionCalls) {
                        boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
                        boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
                        allExpected = allExpected && !updateImpactedSelection;
                    }
                    for (final boolean updateExpected : expectedUpdateExtractedTextCalls) {
                        allExpected = allExpected && updateExpected;
                    }
                    assertFalse("all updates expected", allExpected);
                }
            }
        }


        private void verifySelection(final int selectionStart, final int selectionEnd) {
            assertEquals("selection start", selectionStart, richInputConnection.getExpectedSelectionStart());
            assertEquals("selection end", selectionEnd, richInputConnection.getExpectedSelectionEnd());
        }
        private void verifyText(final String beforeCursor, final String selected, final String afterCursor) {
            assertEquals("text before cursor", beforeCursor, richInputConnection.getTextBeforeCursor(Integer.MAX_VALUE, 0).toString());
            assertEquals("selected text", selected, richInputConnection.getSelectedText(0).toString());
            assertEquals("text after cursor", afterCursor, richInputConnection.getTextAfterCursor(Integer.MAX_VALUE, 0).toString());
        }
        private void verifyTextCache(final String beforeCursor, final String selected, final String afterCursor, final boolean exact) {
            // block the rich input connection from getting the real text so it's forced to only return
            // whatever it might have cached
            fakeInputConnection.allowGettingText(false);

            String cachedBefore = "";
            int i = 1;
            while (true) {
                String tempBefore = richInputConnection.getTextBeforeCursor(i, 0).toString();
                if (tempBefore.length() < i) {
                    // cache must have run out - the previously returned value must be the full cache
                    break;
                }
                cachedBefore = tempBefore;
                i++;
            }
            assertEquals("cached text before cursor",
                    exact
                            ? beforeCursor
                            : beforeCursor.substring(beforeCursor.length() - Math.min(cachedBefore.length(), beforeCursor.length())),
                    cachedBefore);

            CharSequence cachedSelected = richInputConnection.getSelectedText(0);
            if (isEmpty(selected)) {
                assertTrue("selected text is not empty (\"" + cachedSelected + "\")", isEmpty(cachedSelected));
            } else if (!isEmpty(cachedSelected)) {
                assertEquals("cached text in selection",
                        selected, cachedSelected.toString());
            }

            String cachedAfter = "";
            i = 1;
            while (true) {
                String tempAfter = richInputConnection.getTextAfterCursor(i, 0).toString();
                if (tempAfter.length() < i) {
                    // cache must have run out - the previously returned value must be the full cache
                    break;
                }
                cachedAfter = tempAfter;
                i++;
            }
            assertEquals("cached text after cursor",
                    exact
                            ? afterCursor
                            : afterCursor.substring(0, Math.min(cachedAfter.length(), afterCursor.length())),
                    cachedAfter);

            fakeInputConnection.allowGettingText(false);
        }

        private void verifyState(final UpdateSelectionCall lastUpdateSelectionCall, final boolean lastUpdateExpected) {
            assertNotEquals("update selection call count", 0, updateSelectionCalls.size());
            assertEquals("last update selection call", lastUpdateSelectionCall, updateSelectionCalls.get(updateSelectionCalls.size() - 1));
            assertNotEquals("expected update selection call count", 0, expectedUpdateSelectionCalls.size());
            assertNotEquals("expected update extracted text call count", 0, expectedUpdateExtractedTextCalls.size());
            int updateResult = expectedUpdateSelectionCalls.get(expectedUpdateSelectionCalls.size() - 1);
            boolean updateImpactedSelection = (updateResult & UPDATE_IMPACTED_SELECTION) > 0;
            boolean updateExpected = (updateResult & UPDATE_WAS_EXPECTED) > 0;
            assertEquals("last update call was expected", lastUpdateExpected,
                    !updateImpactedSelection
                            && expectedUpdateExtractedTextCalls.get(expectedUpdateExtractedTextCalls.size() - 1));
        }

        private interface UpdateMessage {
            void process();
        }
        private class UpdateSelectionMessage implements UpdateMessage {
            final int oldSelStart;
            final int oldSelEnd;
            final int newSelStart;
            final int newSelEnd;
            final int candidatesStart;
            final int candidatesEnd;
            public UpdateSelectionMessage(final int oldSelStart, final int oldSelEnd,
                                          final int newSelStart, final int newSelEnd,
                                          final int candidatesStart, final int candidatesEnd) {
                this.oldSelStart = oldSelStart;
                this.oldSelEnd = oldSelEnd;
                this.newSelStart = newSelStart;
                this.newSelEnd = newSelEnd;
                this.candidatesStart = candidatesStart;
                this.candidatesEnd = candidatesEnd;
            }
            public void process() {
                if (richInputConnection == null) {
                    return;
                }
                updateSelectionCalls.add(new UpdateSelectionCall(oldSelStart, oldSelEnd,
                        newSelStart, newSelEnd, candidatesStart, candidatesEnd));
                expectedUpdateSelectionCalls.add(richInputConnection.onUpdateSelection(oldSelStart,
                        oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd));
            }
        }
        private class UpdateExtractedTextMessage implements UpdateMessage {
            final int token;
            final ExtractedText text;
            public UpdateExtractedTextMessage(final int token, final ExtractedText text) {
                this.token = token;
                this.text = text;
            }
            public void process() {
                if (richInputConnection == null) {
                    return;
                }
                expectedUpdateExtractedTextCalls.add(
                        richInputConnection.onUpdateExtractedText(token, text));
            }
        }
    }
    //#endregion
}
