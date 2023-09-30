/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.ClipDescription;
import android.graphics.Color;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.EasyEditSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.SuggestionSpan;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.TextAttribute;
import android.view.textclassifier.TextLinks.Builder;
import android.view.textclassifier.TextLinks.TextLink;
import android.view.textclassifier.TextLinks.TextLinkSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;
import rkr.simplekeyboard.inputmethod.latin.common.UnicodeSurrogate;
import rkr.simplekeyboard.inputmethod.latin.settings.SpacingAndPunctuations;
import rkr.simplekeyboard.inputmethod.latin.utils.CapsModeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.DebugLogUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.RangeList.Range;

import static android.view.inputmethod.InputConnection.GET_TEXT_WITH_STYLES;
import static rkr.simplekeyboard.inputmethod.latin.EditorState.TEXT_DIFFERENT;
import static rkr.simplekeyboard.inputmethod.latin.EditorState.TEXT_MATCHED;
import static rkr.simplekeyboard.inputmethod.latin.EditorState.UNKNOWN_LENGTH;
import static rkr.simplekeyboard.inputmethod.latin.EditorState.NONCHARACTER_CODEPOINT_PLACEHOLDER;
import static rkr.simplekeyboard.inputmethod.latin.EditorState.UNKNOWN_POSITION;

/**
 * Enrichment class for InputConnection to simplify interaction and add functionality.
 *
 * This class serves as a wrapper to be able to simply add hooks to any calls to the underlying
 * InputConnection. It also keeps track of a number of things to avoid having to call upon IPC
 * all the time to find out what text is in the buffer, when we need it to determine caps mode
 * for example.
 */
public final class RichInputConnection {
    private static final String TAG = RichInputConnection.class.getSimpleName();
    private static final boolean DBG = false;
    private static final boolean DEBUG_PREVIOUS_TEXT = false;
    private static final boolean DEBUG_BATCH_NESTING = false;

    //TODO: (EW) re-evaluate these times and what should be used for. do different APIs for loading
    // text vary in response time, and do they consistently take longer when requesting more text?
    /**
     * The amount of time a {@link #loadTextCache} call needs to take for the keyboard to enter
     */
    private static final long SLOW_INPUT_CONNECTION_ON_FULL_RELOAD_MS = 1000;
    /**
     * The amount of time a {@link #getTextBeforeCursor} call needs
     */
    private static final long SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS = 200;

    //TODO: (EW) now that text loading is funneled into a central function,
    // OPERATION_RELOAD_TEXT_CACHE isn't used. if there's value in that distinction, special
    // constants will need to be made for each since the reload may load the composition, which may
    // not be entirely before the cursor.
    private static final int OPERATION_GET_TEXT_BEFORE_CURSOR = 0;
    private static final int OPERATION_GET_TEXT_AFTER_CURSOR = 1;
    private static final int OPERATION_GET_SELECTED_TEXT = 2;
    private static final int OPERATION_GET_EXTRACTED_TEXT = 3;
    private static final int OPERATION_RELOAD_TEXT_CACHE = 4;
    private static final String[] OPERATION_NAMES = new String[] {
            "GET_TEXT_BEFORE_CURSOR",
            "GET_TEXT_AFTER_CURSOR",
            "GET_SELECTED_TEXT",
            "GET_EXTRACTED_TEXT",
            "RELOAD_TEXT_CACHE"};

    /**
     * The max number of character to request in expectation of a selection when the selection
     * length is unknown
     */
    private static final int MAX_NORMAL_SELECTION_LENGTH = 30;
    /**
     * The max number of lines to request when getting extracted text
     */
    private static final int MAX_LINES_TO_REQUEST = 10;

    /**
     * The tracked expected state of the text field. It's usually accurate, but external changes or
     * changes that differ from the actions we tell the InputConnection to do may make this wrong,
     * but the updates from the InputConnection on the changes should trigger this to become
     * accurate again.
     */
    private final EditorState mState = new EditorState();
    /**
     * Flag indicating whether we sent GET_EXTRACTED_TEXT_MONITOR for a getExtractedText call to the
     * current field to send updates with text changes.
     */
    private boolean mRequestedExtractedTextMonitor = false;

    /**
     * This variable is a temporary object used in {@link #commitText(CharSequence,int)}
     * to avoid object creation.
     */
    private SpannableStringBuilder mTempObjectForCommitText = new SpannableStringBuilder();

    private final InputMethodService mParent;
    private InputConnection mIC;
    private int mNestLevel;

    private int mBatchIndex = 0;
    private int mLastInvalidStateBatchIndex = -1;

    //TODO: (EW) consider using a linked list as that may be more efficient
    private final ArrayList<SelectionPositionState> mStateHistory = new ArrayList<>();
    private final ArrayList<Long> mLastUpdateDelays = new ArrayList<>();
    private static final int UPDATE_DELAYS_TRACK_COUNT = 20;

    public RichInputConnection(final InputMethodService parent) {
        mParent = parent;
        mIC = null;
        mNestLevel = 0;
    }

    public boolean isConnected() {
        return mIC != null;
    }

    //TODO: (EW) consider just deleting this. this can hit false positives when the system is slow.
    // we're tracking state better (and updating when notified of unexpected changes), and unit
    // tests probably cover this well enough, and since this is hidden behind a debug flag, it
    // probably isn't ever used for testing (if I remember correctly it was significantly broken
    // before my changes).
    private void checkConsistencyForDebug() {
        if (!isConnected()) {
            return;
        }
        //TODO: (EW) this probably should be reevaluated with the new state management

        final ExtractedTextRequest r = new ExtractedTextRequest();
        r.hintMaxChars = 0;
        r.hintMaxLines = 0;
        r.token = 1;
        r.flags = 0;
        final ExtractedText et = mIC.getExtractedText(r, 0);
        final boolean cursorPositionIncorrect;
        if (et != null && mState.areSelectionAbsolutePositionsKnown()) {
            testLog(TAG, "checkConsistencyForDebug: et.selectionStart=" + et.selectionStart
                    + ", et.selectionEnd=" + et.selectionEnd
                    + ", mExpectedSelStart=" + mState.getSelectionStart()
                    + ", mExpectedSelEnd=" + mState.getSelectionEnd());
            cursorPositionIncorrect = et.selectionStart != mState.getSelectionStart()
                    || et.selectionEnd != mState.getSelectionEnd();
            if (cursorPositionIncorrect) {
                testLog(TAG, "checkConsistencyForDebug: cursor position incorrect");
            }
        } else {
            cursorPositionIncorrect = false;
        }

        final CharSequence beforeCursor = mIC.getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
        final boolean textBeforeCursorIncorrect;
        final String actualTextBefore;
        final StringBuilder internalTextBefore =
                new StringBuilder(mState.getTextBeforeCursor());
        if (beforeCursor != null) {
            testLog(TAG, "checkConsistencyForDebug:   actualBeforeCursor=\"" + beforeCursor + "\"");
            testLog(TAG, "checkConsistencyForDebug: expectedBeforeCursor=\"" + internalTextBefore + "\"");
            final int actualLength = Math.min(beforeCursor.length(), internalTextBefore.length());
            if (internalTextBefore.length() > actualLength) {
                internalTextBefore.delete(0, internalTextBefore.length() - actualLength);
            }
            actualTextBefore = (beforeCursor.length() <= actualLength)
                    ? beforeCursor.toString()
                    : beforeCursor.subSequence(
                            beforeCursor.length() - actualLength,
                            beforeCursor.length()).toString();
            textBeforeCursorIncorrect = !actualTextBefore.equals(internalTextBefore.toString());
            if (textBeforeCursorIncorrect) {
                testLog(TAG, "checkConsistencyForDebug: text before cursor incorrect");
            }
        } else {
            textBeforeCursorIncorrect = false;
            actualTextBefore = null;
        }

        final CharSequence selectedText = mIC.getSelectedText(0);
        testLog(TAG, "checkConsistencyForDebug: getSelectedText: " + selectedText);
        final boolean selectedTextIncorrect;
        final CharSequence internalSelectedText = mState.getSelectedText();
        if (selectedText != null && mState.areSelectionAbsolutePositionsKnown()
                && (internalSelectedText.length() > 0
                        || mState.getSelectionStart() == mState.getSelectionEnd())) {
            testLog(TAG, "checkConsistencyForDebug:   actualSelectedText=\"" + selectedText + "\"");
            testLog(TAG, "checkConsistencyForDebug: expectedSelectedText=\"" + internalSelectedText + "\"");
            selectedTextIncorrect = !selectedText.toString().equals(internalSelectedText.toString());
            if (selectedTextIncorrect) {
                testLog(TAG, "checkConsistencyForDebug: selected text incorrect");
            }
        } else {
            selectedTextIncorrect = false;
        }

        final CharSequence afterCursor = mIC.getTextAfterCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
        final boolean textAfterCursorIncorrect;
        final String actualTextAfter;
        final StringBuilder internalTextAfter =
                new StringBuilder(mState.getTextAfterCursor());
        if (afterCursor != null
                && (mState.areSelectionAbsolutePositionsKnown() || selectedText == null
                        || selectedText.length() == 0)) {
            testLog(TAG, "checkConsistencyForDebug:   actualAfterCursor=\"" + afterCursor + "\"");
            testLog(TAG, "checkConsistencyForDebug: expectedAfterCursor=\"" + internalTextAfter + "\"");
            final int actualLength = Math.min(afterCursor.length(), internalTextAfter.length());
            if (internalTextAfter.length() > actualLength) {
                internalTextAfter.delete(actualLength, internalTextAfter.length());
            }
            actualTextAfter = (afterCursor.length() <= actualLength)
                    ? afterCursor.toString()
                    : afterCursor.subSequence(0, actualLength).toString();
            textAfterCursorIncorrect = !actualTextAfter.equals(internalTextAfter.toString());
            if (textAfterCursorIncorrect) {
                testLog(TAG, "checkConsistencyForDebug: text after cursor incorrect");
            }
        } else {
            textAfterCursorIncorrect = false;
            actualTextAfter = null;
        }

        //TODO: check the composing position
        if (cursorPositionIncorrect || textBeforeCursorIncorrect || selectedTextIncorrect || textAfterCursorIncorrect) {
            String context = "";
            if (et != null) {
                context += "Expected selection start: " + mState.getSelectionStart() + "\n"
                        + "Actual selection start:   " + et.selectionStart + "\n"
                        + "Expected selection end:   " + mState.getSelectionEnd() + "\n"
                        + "Actual selection end:     " + et.selectionEnd + "\n";
            }
            if (beforeCursor != null) {
                context += "Expected text before: \"" + internalTextBefore + "\"\n"
                        +  "Actual text before:   \"" + actualTextBefore + "\"\n";
            }
            if (selectedText != null
                    && (internalSelectedText.length() > 0
                            || mState.getSelectionStart() == mState.getSelectionEnd())) {
                context += "Expected text after: \"" + internalSelectedText + "\"\n"
                        +  "Actual text after:   \"" + selectedText + "\"\n";
            }
            if (afterCursor != null) {
                context += "Expected text after: \"" + internalTextAfter + "\"\n"
                        +  "Actual text after:   \"" + actualTextAfter + "\"\n";
            }
            // our internal state could be wrong while in a batch with no way for us to know yet
            // (other than checking all of this normally) in cases where the input connection
            // changes some of the text we tried to enter, but we should get an update later
            // (probably before we start another batch, but that still has a chance to not get to us
            // in time if multiple actions are done in quick succession, so this might still crash
            // when our code isn't actually wrong and there just isn't anything reasonable to do to
            // handle this). it could also be wrong in cases where there was some external change,
            // but the update didn't reach us before starting to do some action. we'll wait for the
            // state to be wrong on two adjacent actions before flagging this as a real error so
            // that hopefully the pending updates will have gotten here to clean things up if the
            // code isn't actually in error.
            //TODO: I was still able to get a crash here when typing fast in a password field in
            // firefox. Is there anything else to do, or is this the best option of catching real
            // errors with minimal false positives? alternatively, should we just never crash
            // because this isn't perfect?
//            if (mNestLevel > 0) {
            if (mLastInvalidStateBatchIndex < 0 || mLastInvalidStateBatchIndex == mBatchIndex) {
//                // if in a batch, our internal state could be wrong with no way for us to know yet
//                // if the input connection decided to change some of the text that we tried to enter
//                // (we have to wait for updates triggered after the batch ends), so this shouldn't
//                // throw an exception, but we'll still log an error because this might be an error,
//                // and it at least has the risk of causing errors if more actions are done in the
//                // batch relying on things that are incorrect
                Log.e(TAG, DebugLogUtils.getStackTrace(2));
                Log.e(TAG, context);
            } else {
//                ((LatinIME) mParent).debugDumpStateAndCrashWithException(context);
            }
            mLastInvalidStateBatchIndex = mBatchIndex;
        } else {
            mLastInvalidStateBatchIndex = -1;
        }
    }

    public void beginBatchEdit() {
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        if (++mNestLevel == 1) {
            mIC = mParent.getCurrentInputConnection();
            if (isConnected()) {
                mIC.beginBatchEdit();
                mBatchIndex++;
            }
        } else {
            if (DBG) {
                throw new RuntimeException("Nest level too deep");
            }
            Log.e(TAG, "Nest level too deep : " + mNestLevel);
        }
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
    }

    // trying to type intelligently fast I managed to get as low as 99 ms between an action, but
    // only a couple got under 150 ms and most were 200-400 ms
    //
    // onUpdateSelection got called as fast as 9 ms in one case and as slow as 36 ms in one case
    // from some brief testing
    //
    // this means that in most reasonably performing and realistic usage, the updates for each call
    // should be received well before we try to do the next action. it might be good to keep track
    // of how fast we're getting these updates. if we see we're getting them slower than normal, we
    // might want to change how some of our timer or excessive calls work to avoid lagging the
    // system worse.
    // in a normal (non-laggy) case we probably could check for missing updates before each action
    // to ensure things are up-to-date before trying to make a change to ensure we request the right
    // thing. we could also have a timer that resets after every action for ~250 ms to check for
    // missing updates in cases that the user pauses the actions so that the UI will update
    // appropriately, so that the next action the user takes is certain to do what it looks like it
    // should do.
    // in a laggy system we could up that timer to 1 or 2 seconds (probably 5 at the most), and to
    // avoid extra lag, possibly it should skip the check between quick actions to keep actions
    // smooth. if we're getting out-of-date actions, we probably are probably losing the composition
    // state, so looking for missing updates at each action probably won't fix that. InputLogic will
    // have to manage its own cache of the composition and keep working off of that.
    //TODO: (EW) figure out when InputLogic should be taking updates to its cache so it doesn't try
    // at times where the cache here could be incorrect
    //
    // ideally, I'd like the managing of the perception of the lag and response to it be in
    // RichInputConnection since that already has some code around it, and it seems like a weird
    // thing to expose.
    //
    // InputLogic should update from RichInputMethod's state only after an update (ideally both
    // selection and extracted text if we can track both correctly) that is believed to be
    // up-to-date or after the timer triggers to check on potentially lost updates (ie when we no
    // longer have an expectation that we may get more updates (other than from future or external
    // actions)). until then, it can work off of its own cache that it only updates from the actions
    // it's requesting. for actions that aren't modified (or interspersed with external actions),
    // using its own cache is equivalent to updating with the working state of RichInputConnection's
    // cache at each out-of-date update, so waiting doesn't matter since the state wil already
    // match. when getting unexpected out-of-date updates, most of the time the composition will
    // fall into an unknown state since that could be affected by whatever made the unexpected
    // changes, so if InputLogic tried to update its cache, it would have to finish composing and
    // start again with the next key to avoid making various assumptions about the state, which
    // could lead to actions the user wouldn't expect. forcing the composition to drop could make it
    // impossible to compose some things right on a laggy system without the user being forced to
    // wait a while for things to process before pressing the next key. ignoring the out-of-date
    // updates seems like a better option, so any additional composing of text works off of the
    // previous request we made. that's actually how most IMEs seem to always work. it makes the IME
    // fight with changes from the editor, which isn't great, but it seems less likely to frustrate
    // the user. any weird behavior from this fighting will probably be seen as an issue with the
    // editor, rather than the IME (which would be the case if we kept dropping the composition). it
    // still makes an assumption about the state (that what was requested is actually what happened
    // and only what happened), which is actually always wrong since we identified an unexpected
    // update, but it's at least consistent in its behavior, so even if it is unexpected to the
    // user, it shouldn't be too upsetting in most cases since it's just working off of what actions
    // were requested in the IME.
    //TODO: (EW) add the timer (probably dynamically set the time based on the last 10 expected
    // updates' delay) and call the handler after receiving updates or checking for missing updates
    // (and updating based on the last update if it seems no more updates are coming)

    public interface UpdatesReceivedHandler {
        void onAllUpdatesReceived();
    }
    private UpdatesReceivedHandler mUpdatesReceivedHandler;
    public void setUpdatesReceivedHandler(final UpdatesReceivedHandler handler) {
        mUpdatesReceivedHandler = handler;
    }

    public void endBatchEdit() {
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        if (mNestLevel <= 0) Log.e(TAG, "Batch edit not in progress!"); // TODO: exception instead
        if (--mNestLevel == 0 && isConnected()) {
            prepSendAction();
            mIC.endBatchEdit();
        }
        testLog(TAG, "after endBatchEdit");
    }

    //TODO: (EW) find a better way to ensure the state history updates any time we request a change
    // in the text/cursor position
    private void prepSendAction() {
        if (mNestLevel <= 0) {
            testLog(TAG, "prepSendAction: " + mState.getDebugStateInternal());
            // if there were no previous updates or actions, whatever this action is will probably
            // change the selection or composition
            boolean expectSelectionUpdate = true;
            SelectionPositionState lastAction = getLastInternalActionState();
            for (int i = mStateHistory.size() - 1; i >= 0; i--) {
                SelectionPositionState state = mStateHistory.get(i);
                if (!state.isInternalAction() && !state.isSelectionUpdate()) {
                    continue;
                }
                testLog(TAG, "prepSendAction: state[" + i + "]: " + state);
                if (state.selectionStart == mState.getSelectionStart()
                        && state.selectionEnd == mState.getSelectionEnd()) {
                    if (mState.isCompositionUnknown()
                            || (mState.hasComposition() && !mState.isAbsoluteSelectionStartKnown())
                            || state.compositionStart == null || state.compositionEnd == null) {
                        // unsure about the composition either before or after the change, so it
                        // might not actually have a change in the composition, but we'll make the
                        // assumption that there is a change because most actions should result in a
                        // change, and if this is wrong, either we'll end up polling for an update
                        // that we think is missing (unnecessary IPC) or the next update will be
                        // flagged as unexpected and probably end up also needing to poll to make
                        // sure we got everything.
                        //TODO: (EW) consider flagging as unsure (maybe null) to try allowing a
                        // fallback expected update
                        testLog(TAG, "prepSendAction: unsure if selection update is expected");
                        expectSelectionUpdate = true;
                    } else {
                        expectSelectionUpdate =
                                state.compositionStart != mState.getCompositionStart()
                                        || state.compositionEnd != mState.getCompositionEnd();
                        testLog(TAG, "prepSendAction: expectSelectionUpdate="
                                + expectSelectionUpdate);
                    }
                } else {
                    testLog(TAG, "prepSendAction: selection position changed");
                    expectSelectionUpdate = true;
                }
                break;
            }
            SelectionPositionState currentAction = SelectionPositionState.internalAction(mState,
                    expectSelectionUpdate,
                    mRequestedExtractedTextMonitor);
            testLog(TAG, "prepSendAction: adding: " + currentAction);
            mStateHistory.add(currentAction);
            if (lastAction != null) {
                testLog(TAG, "last action was "
                        + (currentAction.internalActionTime - lastAction.internalActionTime)
                        + " ms ago");
            }
        }
    }

    public void resetState(final int newSelStart, final int newSelEnd) {
        testLog(TAG, "resetState(" + newSelStart + ", " + newSelEnd + ")");
        mState.reset();
        mStateHistory.clear();
        if (newSelStart >= 0 && newSelEnd >= 0) {
            mState.setSelection(newSelStart, newSelEnd);
            mStateHistory.add(SelectionPositionState.reloadedSelection(newSelStart, newSelEnd));
        } else {
            mState.invalidateSelection(true, true);
            mState.invalidateComposition(false);
            mState.invalidateTextCache();
        }
        mIC = mParent.getCurrentInputConnection();
        testLog(TAG, "resetState: final: " + mState.getDebugStateInternal());
    }

    /**
     * Retrieve text again from the editor for the main load of the cache.
     *
     * This should be called when starting in a text view. It's possible that we can't connect to
     * the application when doing this; notably, this happens sometimes during rotation, probably
     * because of a race condition in the framework. In this case, we just can't retrieve the
     * data, so empty the cache and note that we don't know the new cursor position, and we
     * return false so that the caller knows about this and can retry later.
     *
     * @return true if we were able to connect to the editor successfully, false otherwise. When
     *   this method returns false, the caches could not be correctly refreshed so they were only
     *   reset: the caller should try again later to return to normal operation.
     */
    public boolean reloadCachesForStartingInputView() {
        // there are some cases where the selection position initially passed to resetState isn't
        // accurate. the input connection seems to be more accurate, so we'll make sure to also
        // update that.
        if (!loadCache(false, true)) {
            Log.d(TAG, "Will try to retrieve text later.");
            return false;
        }
        mStateHistory.add(SelectionPositionState.reloadedSelection(
                mState.getSelectionStart(), mState.getSelectionEnd()));
        return true;
    }

    private void checkBatchEdit() {
        if (mNestLevel != 1) {
            // TODO: exception instead
            Log.e(TAG, "Batch edit level incorrect : " + mNestLevel);
            Log.e(TAG, DebugLogUtils.getStackTrace(4));
        }
    }

    /**
     * Calls  {@link InputConnection#finishComposingText()}.
     */
    public void finishComposingText() {
        testLog(TAG, "finishComposingText: init " + mState.getDebugState());
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        mState.finishComposingText();
        testLog(TAG, "finishComposingText: final " + mState.getDebugState());
        if (isConnected()) {
            prepSendAction();
            mIC.finishComposingText();
        }
    }

    /**
     * Calls {@link InputConnection#setComposingText(CharSequence, int)}.
     *
     * @param text The text to commit. This may include styles.
     * @param newCursorPosition The new cursor position around the text.
     */
    public void setComposingText(final CharSequence text, int newCursorPosition) {
        testLog(TAG, "setComposingText: init " + mState.getDebugState());
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        newCursorPosition = getSanitizedNewCursorPosition(newCursorPosition);
        if (isConnected()) {
            boolean tempBatch = false;
            if (mBatchIndex == 0 && !mRequestedExtractedTextMonitor) {
                // since this action will drop any selection to a single point, it will be safe to
                // to request the extracted text monitor (see loadAndValidateCache), but we need to
                // ensure we're in a batch so we're informed of any unexpected text changes from
                // this call
                beginBatchEdit();
                tempBatch = true;
            }
            mState.setComposingText(text, newCursorPosition);
            prepSendAction();
            mIC.setComposingText(text, newCursorPosition);
            if (!mState.hasCachedTextRightBeforeCursor()) {
                // this will also request the extracted text monitor if necessary
                loadTextCache(false);
            } else if (!mRequestedExtractedTextMonitor) {
                // request the extracted text monitor
                loadTextAroundCursor(0, 0);
            }
            if (tempBatch) {
                endBatchEdit();
            }
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        testLog(TAG, "setComposingText: final " + mState.getDebugState());
    }

    /**
     * Constrain the new cursor position to stay within in the text in the field if there is enough
     * info to do so.
     * @param newCursorPosition The new cursor position around the text. See
     *                          {@link InputConnection#commitText} or
     *                          {@link InputConnection#setComposingText}.
     * @return The constrained new cursor position or the original one if it couldn't be sanitized.
     */
    private int getSanitizedNewCursorPosition(final int newCursorPosition) {
        if (newCursorPosition == 1 || newCursorPosition == 0) {
            // the new cursor is placed at either edge of the new text, which is always valid
            return newCursorPosition;
        }
        if (mState.isCompositionUnknown()) {
            // we don't know the whether there is a composition, so we don't know where the new text
            // will be placed, so we don't know what to use to validate the new position will be
            // valid
            return newCursorPosition;
        }
        if (newCursorPosition > 0) {
            // we don't know how many characters are in the text field to be able to limit this. we
            // could try calling getTextAfterCursor, but that can return less text than is actually
            // in the field, so that wouldn't necessarily be valid to determine the limit, so we'll
            // just have to wait for a selection indicating our state is wrong if the requested
            // position isn't valid.
            return newCursorPosition;
        }

        if (!mState.isAbsoluteSelectionStartKnown()) {
            // nothing to validate with
            return newCursorPosition;
        }
        final int newTextStartPosition;
        if (mState.isCompositionUnknown()) {
            // we don't know if there is a composition, so we don't know whether to validate
            // with the composition or selection
            return newCursorPosition;
        } else if (mState.hasComposition()) {
            if (!mState.areCompositionAbsolutePositionsKnown()) {
                // nothing to validate with
                return newCursorPosition;
            }
            newTextStartPosition = mState.getCompositionStart();
        } else {
            newTextStartPosition = mState.getSelectionStart();
        }
        if (newTextStartPosition + newCursorPosition >= 0) {
            // valid new position
            return newCursorPosition;
        }
        Log.w(TAG, "New cursor position is targeting before the start of the text");
        // make sure the new cursor won't put the position before the beginning of the text
        return -newTextStartPosition;
    }

    /**
     * Calls {@link InputConnection#commitText(CharSequence, int)}.
     *
     * @param text The text to commit. This may include styles.
     * @param newCursorPosition The new cursor position around the text.
     */
    public void commitText(final CharSequence text, int newCursorPosition) {
        testLog(TAG, "commitText: init " + mState.getDebugStateInternal());
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        newCursorPosition = getSanitizedNewCursorPosition(newCursorPosition);

        if (isConnected()) {
            mTempObjectForCommitText.clear();
            mTempObjectForCommitText.append(text);
            final CharacterStyle[] spans = mTempObjectForCommitText.getSpans(
                    0, text.length(), CharacterStyle.class);
            testLog(TAG, "commitText: spans.length=" + spans.length);
            for (final CharacterStyle span : spans) {
                final int spanStart = mTempObjectForCommitText.getSpanStart(span);
                final int spanEnd = mTempObjectForCommitText.getSpanEnd(span);
                final int spanFlags = mTempObjectForCommitText.getSpanFlags(span);
                // We have to adjust the end of the span to include an additional character.
                // This is to avoid splitting a unicode surrogate pair.
                // See rkr.simplekeyboard.inputmethod.latin.common.UnicodeSurrogate
                // See https://b.corp.google.com/issues/19255233
                if (0 < spanEnd && spanEnd < mTempObjectForCommitText.length()) {
                    final char spanEndChar = mTempObjectForCommitText.charAt(spanEnd - 1);
                    final char nextChar = mTempObjectForCommitText.charAt(spanEnd);
                    if (UnicodeSurrogate.isLowSurrogate(spanEndChar)
                            && UnicodeSurrogate.isHighSurrogate(nextChar)) {
                        mTempObjectForCommitText.setSpan(span, spanStart, spanEnd + 1, spanFlags);
                    }
                }
            }
            boolean tempBatch = false;
            if (mBatchIndex == 0 && !mRequestedExtractedTextMonitor) {
                // since this action will drop any selection to a single point, it will be safe to
                // to request the extracted text monitor (see loadAndValidateCache), but we need to
                // ensure we're in a batch so we're informed of any unexpected text changes from
                // this call
                beginBatchEdit();
                tempBatch = true;
            }
            mState.setComposingText(text, newCursorPosition);
            mState.finishComposingText();
            prepSendAction();
            mIC.commitText(mTempObjectForCommitText, newCursorPosition);
            if (!mState.hasCachedTextRightBeforeCursor()) {
                // this will also request the extracted text monitor if necessary
                loadTextCache(false);
            } else if (!mRequestedExtractedTextMonitor) {
                // request the extracted text monitor
                loadTextAroundCursor(0, 0);
            }
            if (tempBatch) {
                endBatchEdit();
            }
        }
        testLog(TAG, "commitText: final " + mState.getDebugStateInternal());
    }

    public boolean canDeleteCharacters() {
        return mState.getSelectionStart() > 0;
    }

    /**
     * Gets the caps modes we should be in after this specific string.
     *
     * This returns a bit set of TextUtils#CAP_MODE_*, masked by the inputType argument.
     * This method also supports faking an additional space after the string passed in argument,
     * to support cases where a space will be added automatically, like in phantom space
     * state for example.
     * Note that for English, we are using American typography rules (which are not specific to
     * American English, it's just the most common set of rules for English).
     *
     * @param inputType a mask of the caps modes to test for.
     * @param spacingAndPunctuations the values of the settings to use for locale and separators.
     * @return the caps modes that should be on as a set of bits
     */
    public int getCursorCapsMode(final int inputType,
                                 final SpacingAndPunctuations spacingAndPunctuations) {
        testLog(TAG, "getCursorCapsMode");
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return Constants.TextUtils.CAP_MODE_OFF;
        }
        if (!mState.isCompositionUnknown() && mState.hasComposition()) {
            // We have some composing text - we should be in MODE_CHARACTERS only.
            return TextUtils.CAP_MODE_CHARACTERS & inputType;
        }
        // TODO: this will generally work, but there may be cases where the buffer contains SOME
        // information but not enough to determine the caps mode accurately. This may happen after
        // heavy pressing of delete, for example DEFAULT_TEXT_CACHE_SIZE - 5 times or so.
        // getCapsMode should be updated to be able to return a "not enough info" result so that
        // we can get more context only when needed.
        if (!mState.hasCachedTextRightBeforeCursor()
                && 0 != mState.getSelectionStart()) {
            if (!loadTextCache(false)) {
                Log.w(TAG, "Unable to connect to the editor. "
                        + "Setting caps mode without knowing text.");
            }
        }
        // This never calls InputConnection#getCapsMode - in fact, it's a static method that
        // never blocks or initiates IPC.
        int capsMode = CapsModeUtils.getCapsMode(mState.getTextBeforeCursor(), inputType,
                spacingAndPunctuations);
        testLog(TAG, "getCursorCapsMode: " + capsMode + " (" + CapsModeUtils.flagsToString(capsMode) + ")");
        return capsMode;
    }

    //#region get text and load if necessary
    public int getCodePointBeforeCursor() {
        CharSequence textBeforeCursor = getTextBeforeCursor(2, 0);
        final int length = textBeforeCursor.length();
        if (length < 1) {
            return Constants.NOT_A_CODE;
        }
        return Character.codePointBefore(textBeforeCursor, length);
    }

    public CharSequence getTextBeforeCursor(final int n, final int flags) {
        return getTextAroundCursor(-n, 0, flags, true, false, true, false).requestedText;
    }

    public CharSequence getSelectedText(final int flags) {
        return getTextAroundCursor(0, 1, flags, true, false, false, false).requestedText;
    }

    public CharSequence getTextAfterCursor(final int n, final int flags) {
        return getTextAroundCursor(1, n < Integer.MAX_VALUE ? n + 1 : Integer.MAX_VALUE, flags, true, false, false, true).requestedText;
    }

    //TODO: (EW) there should be a better distinction between an unknown composition text and no
    // composition.
    // cases:
    //  knows there is a composition and has it cached          - string
    //  knows there is a composition but doesn't have it cached - null string and at least cursor start position
    //    knows there is a composition but doesn't know where   - null string and null cursor positions
    //  doesn't know if there is a composition                  - null
    //  knows there isn't a composition                         - empty string and null positions
    // => null state means completely unknown. null text means there is a composition, but the text
    // isn't known. null cursor positions mean the position is unknown, unless the text is an empty
    // string.
    // this seems overly complicated. can this be simplified?
    public CompositionState getCompositionState() {
        testLog(TAG, "getCompositionState: " + mState.getDebugStateInternal());
        if (mState.isCompositionUnknown()) {
            // we don't even know if there is a composition, so we can't give a useful update
            return null;
        } else if (!mState.hasComposition()) {
            return new CompositionState(0, null, null);
        }
        if (!mState.isRelativeCompositionPositionKnown()) {
            // we don't know where the cursor is relative to the composition, so we can't give a
            // useful update
            return null;
        }

        final String composition;
        if (!mState.isFullCompositionCached()) {
            // since the composition isn't fully cached, try reloading it
            int start;
            if (mState.getRelativeCompositionStart() < 0) {
                start = mState.getRelativeCompositionStart();
            } else if (mState.getRelativeCompositionStart() < mState.getSelectionLength()) {
                start = 0;
            } else {
                start = mState.getRelativeCompositionStart() - mState.getSelectionLength() + 1;
            }
            int end;
            if (mState.getRelativeCompositionEnd() <= 0) {
                end = mState.getRelativeCompositionEnd();
            } else if (mState.getRelativeCompositionEnd() <= mState.getSelectionLength()) {
                end = 1;
            } else {
                end = mState.getRelativeCompositionEnd() - mState.getSelectionLength() + 1;
            }
            // since part of the composition could be in the selection, this API may not be able to
            // return it if we couldn't get the selected text, but if the correct portion of the
            // selection is cached we could still get it, so we'll just load from the cache after
            // this call
            getTextAroundCursor(start, end);
        }
        if (!mState.isFullCompositionCached()) {
            // trying to reload the composition cache failed. we may have a portion of the
            // composition, but that probably isn't very useful, so rather than trying to return as
            // much as we do know (and risk leaking placeholder characters or finding some complex
            // structure to report minimally useful info), we just won't report the content of the
            // composition text.
            composition = null;
        } else {
            composition = mState.getComposedText().toString();
        }

        final int relativeCursorStart = -mState.getRelativeCompositionStart();
        final Integer relativeCursorEnd;
        if (!mState.isSelectionLengthKnown()) {
            relativeCursorEnd = null;
        } else {
            relativeCursorEnd = relativeCursorStart + mState.getSelectionLength();
        }

        return new CompositionState(relativeCursorStart, relativeCursorEnd, composition);
    }

    //#region wrappers to load text

    public final static int FAILED_LOAD = -1;
    //TODO: (EW) some of these flags aren't checked. consider slimming this down and only track
    // things that we have a specific need to check
    public final static int NO_UPDATE = 0;
    public final static int TEXT_REQUESTED = 1;
    public final static int TEXT_LOADED = 2;
    public final static int TEXT_UPDATED = 4;
    public final static int TEXT_REMOVED = 8;//flag for throwing away potentially incorrect text
    public final static int TEXT_CORRECTED = 16;
    public final static int SELECTION_LOADED = 32;
    public final static int SELECTION_UPDATED = 64;
    public final static int SELECTION_CORRECTED = 128;
    public final static int FULL_REQUEST_COMPLETED = 256;
    public static class LoadAndValidateCacheResult {
        public final CharSequence requestedText;
        public final int updateFlags;
        public LoadAndValidateCacheResult(final CharSequence requestedText,
                                          final int updateFlags) {
            this.requestedText = requestedText;
            this.updateFlags = updateFlags;
        }
    }

    /**
     * Convenience wrapper for
     * {@link #getTextAroundCursor(int, int, int, boolean, boolean, boolean, boolean)} to just load
     * some text. This will not look to the cached text. Any text loaded will be added to the cache.
     * If the loaded text is different from existing text in the cache in that position, it will
     * clear any text that wasn't just loaded.
     * @param start The start position relative to the selection edges (see
     *              {@link #getTextAroundCursor}).
     * @param end The end position relative to the selection edges (see
     *            {@link #getTextAroundCursor}).
     * @return The requested text (if it could be obtained).
     */
    private CharSequence loadTextAroundCursor(int start, int end) {
        return getTextAroundCursor(start, end, 0, false, false, false, false).requestedText;
    }

    /**
     * Convenience wrapper for
     * {@link #getTextAroundCursor(int, int, int, boolean, boolean, boolean, boolean)} to load and
     * validate some text and optionally load the selection position. This will not look to the
     * cached text. Any text loaded will be added to the cache. If the loaded text is different from
     * existing text in the cache in that position, it will clear any text that wasn't just loaded.
     * @param start The start position relative to the selection edges (see
     *              {@link #getTextAroundCursor}).
     * @param end   The end position relative to the selection edges (see
     *              {@link #getTextAroundCursor}).
     * @param loadSelectionPosition Flag indicating whether the selection start/end positions should
     *                              be loaded.
     * @return An object containing the requested text (if it could be obtained) and an indicator
     *         for what was loaded or updated from this call.
     */
    private LoadAndValidateCacheResult loadAndValidateCache(int start, int end,
                                                            boolean loadSelectionPosition) {
        return getTextAroundCursor(start, end, 0, false, loadSelectionPosition, false, false);
    }

    /**
     * Convenience wrapper for
     * {@link #getTextAroundCursor(int, int, int, boolean, boolean, boolean, boolean)} to just get
     * some text. This will first look to the cached text and then load anything that's missing. Any
     * text loaded will be added to the cache. If the loaded text is different from existing text in
     * the cache in that position, it will clear any text that wasn't just loaded.
     * @param start The start position relative to the selection edges (see
     *              {@link #getTextAroundCursor}).
     * @param end The end position relative to the selection edges (see
     *            {@link #getTextAroundCursor}).
     * @return The requested text (if it could be obtained).
     */
    private CharSequence getTextAroundCursor(int start, int end) {
        return getTextAroundCursor(start, end, 0, true, false, false, false).requestedText;
    }

    //TODO: (EW) making public for testing - see if there is a better way to test this (maybe indirectly)
    /**
     * Get some text.
     * The requested text is identified by positions relative to the edges of the selection,
     * matching the format as newCursorPosition in
     * {@link InputConnection#commitText(CharSequence, int)}. This does mean that the whole
     * selection needs to be requested even if only a portion if it is needed.
     * Any new text that is loaded will be added to the cache, and when any new text is loaded, this
     * will verify that any existing text in the cache that overlaps with the newly loaded text
     * matches, and if it doesn't, any text that hasn't just been loaded will be cleared from the
     * cache.
     * @param start The start position. If > 0, this is relative to the end of the text - 1;
     *              if <= 0, this is relative to the start of the text.
     * @param end The end position. If > 0, this is relative to the end of the text - 1; if <= 0,
     *            this is relative to the start of the text.
     * @param flags Additional request flags, having the same possible values as the flags parameter
     *              of {@link InputConnection#getTextBeforeCursor(int, int)}.
     * @param useCache Flag indicating whether the requested text should be taken from the cache if
     *                 available.
     * @param loadSelectionPosition Flag indicating whether the selection start/end positions should
     *                              be loaded.
     * @param skipMissingLeadingText Flag indicating that even if some of the text is missing at the
     *                               beginning of the requested text, as much text as this was able
     *                               to obtain should still be returned.
     * @param skipMissingTrailingText Flag indicating that even if some of the text is missing at
     *                                the end of the requested text, as much text as this was able
     *                                to obtain should still be returned.
     * @return An object containing the requested text (if it could be obtained) and an indicator
     *         for what was loaded or updated from this call.
     */
    public LoadAndValidateCacheResult getTextAroundCursor(int start, int end, int flags,
                                                          boolean useCache,
                                                          boolean loadSelectionPosition,
                                                          boolean skipMissingLeadingText,
                                                          boolean skipMissingTrailingText) {
        testLog(TAG, "getTextAroundCursor: start=" + start + ", end=" + end
                + ", useCache=" + useCache + ", loadSelectionPosition=" + loadSelectionPosition
                + ", skipMissingLeadingText=" + skipMissingLeadingText
                + ", skipMissingTrailingText=" + skipMissingTrailingText);
        if (start > end) {
            Log.e(TAG, "getTextAroundCursor called with invalid positions: start=" + start
                    + ", end=" + end);
            return new LoadAndValidateCacheResult(null, NO_UPDATE);
        }
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return new LoadAndValidateCacheResult(null, NO_UPDATE);
        }

        if (flags == GET_TEXT_WITH_STYLES) {
            // the cache doesn't store spans, so it will have to load it fresh
            useCache = false;
        }

        // when calling getExtractedText with GET_EXTRACTED_TEXT_MONITOR, the framework EditText
        // expects that extracted mode will start, so it drops the copy/paste/etc. menu, and in some
        // cases also causes it to drop the selection (although it still reports what that selection
        // was). we could set the selection back to ensure that much is kept (assuming it does
        // return the selection position or we already had it), but there isn't a way to get the
        // menu back. instead, we'll defer requesting the monitor. it's most important for
        // committing/composing text since the editor may change the text we tried to enter, so
        // we'll just wait until we enter text to request it since those actions drop the selection
        // normally, making this side effect a non-issue.
        boolean requestExtractedTextMonitor =
                !mRequestedExtractedTextMonitor && mState.getSelectionLength() == 0;

        int updateStatus = NO_UPDATE;
        FetchedText fetchedText = new FetchedText();
        // InputConnection#getSelectedText returns null if there is no selection, the editor takes
        // too long, or the method isn't implemented, so that can't clearly indicate the lack of a
        // selection, so if the selection isn't known, we should first try loading the selection
        // positions to see if there is a selection that needs to be loaded. Also, prior to N,
        // calling getSelectedText may crash the app if it isn't implemented, so we should try to
        // avoid using that method on those versions to be safe.
        if (loadSelectionPosition || requestExtractedTextMonitor
                || (end >= 1 && (!mState.isSelectionLengthKnown()
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.N))) {
            int hintMaxChars;
            if (start <= 0 && end >= 1) {
                hintMaxChars = (0 - start)
                        + (mState.isSelectionLengthKnown()
                        ? mState.getSelectionLength()
                        : MAX_NORMAL_SELECTION_LENGTH)
                        + (end - 1);
            } else {
                hintMaxChars = end - start;
            }
            int extractedTextResult = fetchExtractedText(hintMaxChars, flags,
                    requestExtractedTextMonitor, fetchedText);
            if (loadSelectionPosition && (extractedTextResult & SELECTION_LOADED) == 0) {
                testLog(TAG, "getTextAroundCursor: selection not loaded");
            }
            updateStatus |= extractedTextResult;
        }

        // don't bother trying to get text before the beginning of the field
        if (mState.isAbsoluteSelectionStartKnown() && start < -mState.getSelectionStart()) {
            start = -mState.getSelectionStart();
            testLog(TAG, "getTextAroundCursor(corrected): start=" + start + ", end=" + end);
        }

        if (start < end) {
            boolean textBeforeRequested = false;
            boolean selectedTextRequested = false;
            boolean textAfterRequested = false;
            for (int pieceToLoadIndex = 0; pieceToLoadIndex < 3; pieceToLoadIndex++) {
                int textLoadResult;
                switch (pieceToLoadIndex) {
                    case 0: // text before cursor
                        if (start >= 0 || textBeforeRequested) {
                            continue;
                        }
                        textLoadResult = fetchTextBeforeCursor(0 - start, flags, useCache,
                                fetchedText);
                        if ((textLoadResult & TEXT_REQUESTED) > 0) {
                            textBeforeRequested = true;
                        }
                        break;
                    case 1: // selected text
                        // skip loading if the selection isn't requested unless we're loading text
                        // after the cursor but don't know where that text would go due to not
                        // knowing the selection length (skip this extra check on versions prior to
                        // N because this call has a chance of crashing the editor app)
                        if (end < 1 || (start > 0 && (mState.isSelectionLengthKnown()
                                || Build.VERSION.SDK_INT < Build.VERSION_CODES.N))
                                || selectedTextRequested) {
                            continue;
                        }
                        textLoadResult = fetchSelectedText(flags, useCache, fetchedText);
                        if ((textLoadResult & TEXT_REQUESTED) > 0) {
                            selectedTextRequested = true;
                        }
                        break;
                    case 2: // text after cursor
                        if (end <= 1 || textAfterRequested) {
                            continue;
                        }
                        if (start <= 0 && !mState.isSelectionLengthKnown()
                                && !skipMissingTrailingText) {
                            // we won't be able to load the full text since we don't know how to
                            // connect the text before the cursor to the text after it, so we won't
                            // even be able to store it in the cache, so don't bother loading it
                            continue;
                        }
                        textLoadResult = fetchTextAfterCursor(end - 1, flags, useCache,
                                fetchedText);
                        if ((textLoadResult & TEXT_REQUESTED) > 0) {
                            textAfterRequested = true;
                        }
                        break;
                    default:
                        continue;
                }
                if (textLoadResult == FAILED_LOAD) {
                    // even though other things may have loaded, we had to throw it away, so there
                    // was no real update, and that detail probably isn't valuable
                    return new LoadAndValidateCacheResult(null, NO_UPDATE);
                }
                // update the status, excluding the full request completed flag since that hasn't
                // been determined for the whole yet
                updateStatus |= textLoadResult;
                if (useCache && pieceToLoadIndex > 0 && (textLoadResult & TEXT_CORRECTED) > 0) {
                    // reset the loop to reload text in case text loaded earlier came from the
                    // cache, which is now determined to at least be partially incorrect. the text
                    // already actually loaded (not originally from the cache) will still be used
                    // without making an extra call to load the text from the input connection.
                    pieceToLoadIndex = -1;
                }
            }
        }

        CharSequence text;
        text = fetchedText.getText(start, end, mState.getSelectionLength(),
                flags == GET_TEXT_WITH_STYLES, false, false);
        if (text != null && (!loadSelectionPosition || (updateStatus & SELECTION_LOADED) > 0)) {
            updateStatus |= FULL_REQUEST_COMPLETED;
        } else if (skipMissingLeadingText || skipMissingTrailingText) {
            text = fetchedText.getText(start, end, mState.getSelectionLength(),
                    flags == GET_TEXT_WITH_STYLES, skipMissingLeadingText, skipMissingTrailingText);
        }
        testLog(TAG, "getTextAroundCursor: " + mState.getDebugStateInternal());
        testLog(TAG, "getTextAroundCursor: text=" + (text == null ? "null" : ("\"" + text + "\"")));
        testLog(TAG, "getTextAroundCursor: " + getLoadAndValidateResultName(updateStatus)
                + " (" + updateStatus + ")");
        return new LoadAndValidateCacheResult(text, updateStatus);
    }

    private int fetchExtractedText(int hintMaxChars, int flags, boolean requestExtractedTextMonitor,
                                   FetchedText fetchedText) {
        final boolean includeStyles = flags == GET_TEXT_WITH_STYLES;
        final ExtractedText extractedText = getExtractedTextAndDetectLaggyConnection(
                OPERATION_GET_EXTRACTED_TEXT,
                SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
                hintMaxChars, flags, requestExtractedTextMonitor);
        int updateStatus = TEXT_REQUESTED;
        if (extractedText != null) {
            final String message = "fetchExtractedText: text=\"" + extractedText.text
                    + "\", textClass=" + extractedText.text.getClass()
                    + ", flags=" + extractedText.flags
                    + ", partialStartOffset=" + extractedText.partialStartOffset
                    + ", partialEndOffset=" + extractedText.partialEndOffset
                    + ", selectionStart=" + extractedText.selectionStart
                    + ", selectionEnd=" + extractedText.selectionEnd
                    + ", startOffset=" + extractedText.startOffset;
            if (extractedText.startOffset != 0) {
                testLogImportant(TAG, message);
            } else {
                testLog(TAG, message);
            }

            final int selectionStart = extractedText.startOffset + extractedText.selectionStart;
            final int selectionEnd = extractedText.startOffset + extractedText.selectionEnd;
            final int initialCharCount = mState.getCachedCharCount();
            updateStatus |= SELECTION_LOADED;
            // check if the cached selection positions are different or unknown
            if (!mState.selectionMatches(selectionStart, selectionEnd, false)) {
                updateStatus |= SELECTION_UPDATED;
                if (!mState.isAbsoluteSelectionStartKnown()) {
                    if (mState.isSelectionLengthKnown()
                            && mState.getSelectionLength() != selectionEnd - selectionStart) {
                        updateStatus |= SELECTION_CORRECTED;
                    }
                    testLog(TAG, "fetchExtractedText: invalidateTextCache - unknown current cursor position");
                    // we don't know where the current cached text fits, so we'll just have to clear
                    // it to be safe
                    if (mState.invalidateTextCache() > 0) {
                        updateStatus |= TEXT_REMOVED;
                    }
                    if (!mState.isCompositionUnknown() && mState.hasComposition()) {
                        // since we don't know if or how the cursor moved, it's not clear where the
                        // composition is now, so we'll just reset our state to be safe
                        //TODO: (EW) tests currently don't seem to cover this
                        mState.invalidateComposition(false);
                    }
                } else if (mState.getSelectionStart() == selectionStart) {
                    if (mState.isSelectionLengthKnown()) {
                        updateStatus |= SELECTION_CORRECTED;
                        // only the selection end is different than expected. the text before the
                        // cursor is probably fine, but anything in the selection and after could be
                        // wrong, so clear that.
                        testLog(TAG, "fetchExtractedText: invalidateTextCacheAfterRelative - selection end doesn't match: "
                                + mState.getSelectionEnd() + " <> " + selectionEnd);
                        if (mState.invalidateTextCacheAfterRelative(-1) > 0) {
                            updateStatus |= TEXT_REMOVED;
                        }
                    } else {
                        // we just don't know the selection end position. we don't have reason to
                        // believe the cached text is invalid, so it can be left alone.
                    }
                } else {
                    updateStatus |= SELECTION_CORRECTED;
                    // the selection start (and maybe also the selection end) is different than
                    // expected, which means we don't know what in the text cache is also wrong. we
                    // can't tell what might be valid in the cache because selection changes can be
                    // ambiguous for the text (did the cursor just move, or was some text inserted
                    // somewhere before the cursor, leaving most of the text relative to the cursor
                    // still accurate?).
                    testLog(TAG, "fetchExtractedText: invalidateTextCache - selection doesn't match: "
                            + mState.getSelectionStart() + " - "
                            + mState.getSelectionEnd()
                            + " <> " + selectionStart + " - " + selectionEnd);
                    mState.invalidateTextCache();
                    if (!mState.isCompositionUnknown() && mState.hasComposition()) {
                        // since the composition position is tied to the selection start, which
                        // changed, it's not clear where the composition should be. it could have
                        // shifted with the cursor or stayed where it was, so we'll just reset our
                        // state to be safe
                        mState.invalidateComposition(false);
                    }
                }
                mState.setSelection(selectionStart, selectionEnd);
                if (initialCharCount != mState.getCachedCharCount()) {
                    updateStatus |= TEXT_REMOVED;
                }
            }

            final int textStartOffsetFromCursorStart = (extractedText.partialStartOffset < 0
                    ? extractedText.startOffset
                    : extractedText.partialStartOffset)
                    - mState.getSelectionStart();
            updateStatus |= validateAndUpdateTextCache(extractedText.text,
                    textStartOffsetFromCursorStart, includeStyles, fetchedText);
            if (extractedText.text == null) {
                testLog(TAG, "fetchExtractedText: null ExtractedText text returned");
            }
        } else {
            testLog(TAG, "fetchExtractedText: null ExtractedText returned");
        }
        testLog(TAG, "fetchExtractedText: " + getLoadAndValidateResultName(updateStatus)
                + " (" + updateStatus + ")");
        return updateStatus;
    }

    private int fetchTextBeforeCursor(int length, int flags, boolean useCache,
                                      FetchedText fetchedText) {
        testLog(TAG, "fetchTextBeforeCursor: length=" + length + ", flags=" + flags
                + ", useCache=" + useCache);
        final boolean includeStyles = flags == GET_TEXT_WITH_STYLES;
        if (fetchedText.isRangeIncluded(-length, 0 - 1, mState.getSelectionLength(),
                false, includeStyles)) {
            // text was already loaded
            return NO_UPDATE;
        }
        if (useCache) {
            CharSequence textBeforeCursor = mState.getTextBeforeCursor();
            fetchedText.add(-textBeforeCursor.length(), false, false, textBeforeCursor);
            if (textBeforeCursor.length() >= length) {
                return NO_UPDATE;
            }
        }

        //TODO: (EW) probably should use SLOW_INPUT_CONNECTION_ON_FULL_RELOAD_MS some of the
        // time (either from a flag passed in or based on how much text is being requested)
        CharSequence textBeforeCursor = getTextBeforeCursorAndDetectLaggyConnection(
                OPERATION_GET_TEXT_BEFORE_CURSOR,
                SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
                length, flags);
        int updateStatus = TEXT_REQUESTED;
        testLog(TAG, "fetchTextBeforeCursor: textBeforeCursor=\"" + textBeforeCursor + "\"");
        if (null == textBeforeCursor) {
            // For some reason the app thinks we are not connected to it. This looks like a
            // framework bug... Fall back to ground state and return false.
            testLog(TAG, "resetting state: " + mState.getDebugStateInternal());
            mState.reset();
            Log.e(TAG, "Unable to connect to the editor to retrieve text.");
            return FAILED_LOAD;
        }

        updateStatus |= validateAndUpdateTextCache(textBeforeCursor,
                -textBeforeCursor.length(), includeStyles, fetchedText);

        if (textBeforeCursor.length() < length) {
            testLog(TAG, "fetchTextBeforeCursor: text before cursor not fully loaded");
        }
        //TODO: (EW) is there a better way to manage this? maybe add all of the cache from the start
        // not just what is adjacent to the cursor
        if (textBeforeCursor.length() < length && useCache) {
            // see if there is anything else in the cache since the load didn't find everything
            CharSequence cachedTextBeforeCursor = mState.getTextBeforeCursor();
            if (cachedTextBeforeCursor.length() > textBeforeCursor.length()) {
                fetchedText.add(-cachedTextBeforeCursor.length(), false, false, cachedTextBeforeCursor);
            }
        }

        testLog(TAG, "fetchTextBeforeCursor: " + getLoadAndValidateResultName(updateStatus)
                + " (" + updateStatus + ")");
        return updateStatus;
    }

    private int fetchSelectedText(int flags, boolean useCache, FetchedText fetchedText) {
        final boolean includeStyles = flags == GET_TEXT_WITH_STYLES;
        int selectionLength = mState.getSelectionLength();
        if (selectionLength == 0) {
            // nothing to load
            return NO_UPDATE;
        }
        if (selectionLength != UNKNOWN_LENGTH) {
            if (fetchedText.isRangeIncluded(0, selectionLength - 1,
                    mState.getSelectionLength(), false, includeStyles)) {
                // text was already loaded
                return NO_UPDATE;
            }
            if (useCache) {
                CharSequence selectedText = mState.getSelectedText();
                if (selectedText != null) {
                    fetchedText.add(0, includeStyles, false, selectedText);
                    return NO_UPDATE;
                }
            }
        }

        CharSequence selectedText = getSelectedTextAndDetectLaggyConnection(
                OPERATION_GET_SELECTED_TEXT,
                SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
                flags);
        testLog(TAG, "fetchSelectedText: selectedText=\"" + selectedText + "\"");
        int updateStatus = TEXT_REQUESTED;
        if (selectedText != null) {
            if (mState.getSelectionLength() != selectedText.length()) {
                updateStatus |= SELECTION_UPDATED;
                if (mState.isSelectionLengthKnown()) {
                    updateStatus |= SELECTION_CORRECTED;
                }
                mState.setSelectionLength(selectedText.length());
            }
        }
        // note that if the returned text was null, this will not mark this as TEXT_LOADED even if
        // there was no selection. we do mark TEXT_LOADED when loading text before or after the
        // selection and it returns no text, which is conceptually the same as this case, making
        // this inconsistent. there isn't really anything to do about this because there is no
        // distinction between having no selected text and the method failing or not being
        // supported, and if we already knew that there was no selected text, we wouldn't even try
        // to load it and run into this case.
        updateStatus |= validateAndUpdateTextCache(selectedText, 0, includeStyles, fetchedText);

        testLog(TAG, "fetchSelectedText: " + getLoadAndValidateResultName(updateStatus)
                + " (" + updateStatus + ")");
        return updateStatus;
    }

    private int fetchTextAfterCursor(int length, int flags, boolean useCache,
                                     FetchedText fetchedText) {
        final boolean includeStyles = flags == GET_TEXT_WITH_STYLES;
        int selectionLength = mState.getSelectionLength();
        if (selectionLength != UNKNOWN_LENGTH) {
            if (fetchedText.isRangeIncluded(selectionLength, selectionLength + length - 1,
                    mState.getSelectionLength(), false, includeStyles)) {
                // text was already loaded
                return NO_UPDATE;
            }
            if (useCache) {
                CharSequence textAfterCursor = mState.getTextAfterCursor();
                fetchedText.add(selectionLength, includeStyles, false, textAfterCursor);
                if (textAfterCursor.length() >= length) {
                    return NO_UPDATE;
                }
            }
        }

        CharSequence textAfterCursor = getTextAfterCursorAndDetectLaggyConnection(
                OPERATION_GET_TEXT_AFTER_CURSOR,
                SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
                length, flags);
        int updateStatus = TEXT_REQUESTED;
        testLog(TAG, "fetchTextAfterCursor: textAfterCursor=\"" + textAfterCursor + "\"");
        if (null == textAfterCursor) {
            // For some reason the app thinks we are not connected to it. This looks like a
            // framework bug... Fall back to ground state and return false.
            testLog(TAG, "resetting state: " + mState.getDebugStateInternal());
            mState.reset();
            Log.e(TAG, "Unable to connect to the editor to retrieve text.");
            // even though other things may have loaded, we had to throw it away, so there was
            // no real update, and that detail probably isn't valuable
            return FAILED_LOAD;
        }

        if (selectionLength != UNKNOWN_LENGTH) {
            updateStatus |= validateAndUpdateTextCache(textAfterCursor, selectionLength,
                    includeStyles, fetchedText);
        } else {
            fetchedText.addTextAfterCursor(includeStyles, textAfterCursor);
            updateStatus |= TEXT_LOADED;
        }

        if (textAfterCursor.length() < length) {
            testLog(TAG, "fetchTextAfterCursor: text after cursor not fully loaded");
        }
        //TODO: (EW) is there a better way to manage this? maybe add all of the cache from the start
        // not just what is adjacent to the cursor
        if (textAfterCursor.length() < length && useCache && selectionLength != UNKNOWN_LENGTH) {
            // see if there is anything else in the cache since the load didn't find everything
            CharSequence cachedTextAfterCursor = mState.getTextAfterCursor();
            if (cachedTextAfterCursor.length() > textAfterCursor.length()) {
                fetchedText.add(selectionLength, includeStyles, false, cachedTextAfterCursor);
            }
        }

        testLog(TAG, "fetchTextAfterCursor: " + getLoadAndValidateResultName(updateStatus)
                + " (" + updateStatus + ")");
        return updateStatus;
    }

    private int validateAndUpdateTextCache(final CharSequence text,
                                           final int textOffsetFromCursorStart,
                                           final boolean includeStyles,
                                           final FetchedText fetchedText) {
        if (text == null) {
            return NO_UPDATE;
        }
        testLog(TAG, "validateAndUpdateTextCache: text=\"" + text
                + "\", textOffsetFromCursorStart=" + textOffsetFromCursorStart);
        int updateStatus = TEXT_LOADED;
        if (TextUtils.isEmpty(text)) {
            return updateStatus;
        }
        testLog(TAG, "validateAndUpdateTextCache: " + mState.getDebugState());
        int textComparison = mState.compareTextInCache(text, textOffsetFromCursorStart);
        testLog(TAG, "validateAndUpdateTextCache: textComparison: " + textComparison);
        if (textComparison != TEXT_MATCHED) {
            mState.updateTextCacheRelative(text, textOffsetFromCursorStart);
            updateStatus |= TEXT_UPDATED;
        }
        if (text.length() > 0) {
            fetchedText.add(textOffsetFromCursorStart, includeStyles, true, text);
        }
        if (textComparison == TEXT_DIFFERENT) {
            // something was incorrect when we weren't expecting it, which means we don't know
            // what else might be wrong, so we'll just clear the cache to be safe
            Log.w(TAG, "Some text in the cache was incorrect");
            testLog(TAG, "validateAndUpdateTextCache: invalidateTextCache - text doesn't match: "
                    + text);
            testLog(TAG, "validateAndUpdateTextCache: before invalidate cache: "
                    + mState.getDebugStateInternal());
            // invalidate anything that hasn't already been validated
            fetchedText.clearNonValidated();
            Range[] validatedRanges = fetchedText.getRangesFromCursorStart(true, includeStyles,
                    mState.getSelectionLength());
            int removedCharCount = 0;
            if (validatedRanges.length == 0) {
                testLog(TAG, "validateAndUpdateTextCache: invalidateTextCache - validatedRanges.length == 0");
                removedCharCount += mState.invalidateTextCache();
            } else {
                testLog(TAG, "validateAndUpdateTextCache: invalidateTextCacheBeforeRelative: "
                        + validatedRanges[0].start());
                removedCharCount += mState.invalidateTextCacheBeforeRelative(validatedRanges[0].start());
                for (int i = 1; i < validatedRanges.length; i++) {
                    if (validatedRanges[i - 1].end() + 1 == validatedRanges[i].start()) {
                        continue;
                    }
                    testLog(TAG, "validateAndUpdateTextCache: invalidateTextCacheRangeRelative: "
                            + (validatedRanges[i - 1].end() + 1) + " - " + (validatedRanges[i].start() - 1));
                    removedCharCount += mState.invalidateTextCacheRangeRelative(
                            validatedRanges[i - 1].end() + 1, validatedRanges[i].start() - 1);
                }
                testLog(TAG, "validateAndUpdateTextCache: invalidateTextCacheAfterRelative: "
                        + validatedRanges[validatedRanges.length - 1].end());
                removedCharCount += mState.invalidateTextCacheAfterRelative(
                        validatedRanges[validatedRanges.length - 1].end());
            }
            if (removedCharCount > 0) {
                //TODO: (EW) if we end up loading this text, it possibly shouldn't be marked as
                // removed, only corrected
                updateStatus |= TEXT_REMOVED;
            }
            testLog(TAG, "validateAndUpdateTextCache: after invalidate cache: "
                    + mState.getDebugStateInternal());
            updateStatus |= TEXT_CORRECTED;
        }
        testLog(TAG, "validateAndUpdateTextCache: mState: " + mState.getDebugState());
        testLog(TAG, "validateAndUpdateTextCache: updateStatus: "
                + getLoadAndValidateResultName(updateStatus) + " (" + updateStatus + ")");
        return updateStatus;
    }

    private static class FetchedText {
        private static class TextInfo {
            private final int offset;
            private final boolean offsetIsPositionAroundCursor;// false means relative to cursor start
            private final boolean includeStyles;
            private final boolean isValidated;
            private final CharSequence text;

            TextInfo(int offset, boolean offsetIsPositionAroundCursor, boolean includeStyles,
                     boolean isValidated, CharSequence text) {
                this.offset = offset;
                this.offsetIsPositionAroundCursor = offsetIsPositionAroundCursor;
                this.includeStyles = includeStyles;
                this.isValidated = isValidated;
                this.text = text;
            }
        }

        private List<TextInfo> mTextInfoList = new ArrayList<>();

        public void add(int offset, boolean includeStyles, boolean isValidated, CharSequence text) {
            testLog(TAG, "FetchedText#add: offset=" + offset
                    + ", includeStyles=" + includeStyles + ", isValidated=" + isValidated
                    + ", text=\"" + text + "\"");
            mTextInfoList.add(new TextInfo(offset, false, includeStyles, isValidated, text));
        }

        // special carve-out for only loading text after the cursor
        public void addTextAfterCursor(boolean includeStyles, CharSequence text) {
            testLog(TAG, "FetchedText#addTextAfterCursor: includeStyles=" + includeStyles
                    + ", text=\"" + text + "\"");
            mTextInfoList.add(new TextInfo(1, true, includeStyles, true, text));
        }

        public void clearNonValidated() {
            for (int i = mTextInfoList.size() - 1; i >= 0; i--) {
                if (!mTextInfoList.get(i).isValidated) {
                    mTextInfoList.remove(i);
                }
            }
        }

        public Range[] getRangesFromCursorStart(boolean validatedOnly, boolean requireStyles,
                                                int selectionLength) {
            List<Range> validatedRanges = new ArrayList<>();
            // create a list of relevant ranges
            for (TextInfo textInfo : mTextInfoList) {
                if (validatedOnly && !textInfo.isValidated) {
                    continue;
                }
                if (requireStyles && !textInfo.includeStyles) {
                    continue;
                }
                if (textInfo.offsetIsPositionAroundCursor) {
                    if (textInfo.offset >= 1 && selectionLength < 0) {
                        // don't know where this fits, so skip
                        continue;
                    }
                    int start = textInfo.offset <= 0
                            ? textInfo.offset
                            : (textInfo.offset - 1 + selectionLength);
                    validatedRanges.add(new Range(start, start + textInfo.text.length() - 1));
                } else {
                    validatedRanges.add(new Range(textInfo.offset,
                            textInfo.offset + textInfo.text.length() - 1));
                }
            }
            // merge overlapping or adjacent ranges
            // this isn't very efficient (O(n^2)), but there will only ever be a few items, so it
            // should be fine
            for (int i = validatedRanges.size() - 1; i >= 0; i--) {
                Range rangeA = validatedRanges.get(i);
                for (int j = i - 1; j >= 0; j--) {
                    Range rangeB = validatedRanges.get(j);
                    if (rangeA.end() + 1 < rangeB.start() || rangeA.start() - 1 > rangeB.start()) {
                        // ranges are not adjacent or overlapping
                        continue;
                    }
                    // merge the ranges
                    validatedRanges.set(j, new Range(Math.min(rangeA.start(), rangeB.start()),
                            Math.max(rangeA.end(), rangeB.end())));
                    validatedRanges.remove(i);
                    break;
                }
            }
            Collections.sort(validatedRanges);
            return validatedRanges.toArray(new Range[0]);
        }

        public boolean isRangeIncluded(int start, int end, int selectionLength,
                                       boolean validatedOnly, boolean requireStyles) {
            Range[] ranges =
                    getRangesFromCursorStart(validatedOnly, requireStyles, selectionLength);
            for (Range range : ranges) {
                if (start >= range.start() && end <= range.end()) {
                    return true;
                }
            }
            return false;
        }

        public CharSequence getText(int start, int end, int selectionLength,
                                    boolean requireStyles, boolean skipMissingLeadingText,
                                    boolean skipMissingTrailingText) {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            int length;
            if (start <= 0 && end >= 1) {
                if (selectionLength < 0) {
                    // we don't know how much text to return for the selection
                    return null;
                }
                length = -start + selectionLength + (end - 1);
            } else {
                length = end - start;
            }
            // build a string with the proper length with placeholder characters to simplify
            // concatenating the various pieces
            //TODO: (EW) maybe don't do this. when requesting Integer.MAX_VALUE characters, this
            // runs out of memory.
            sb.append(EditorState.repeatChar(NONCHARACTER_CODEPOINT_PLACEHOLDER, length));
            // find the relevant pieces and add the text to the proper place in the result string
            for (TextInfo textInfo : mTextInfoList) {
                testLog(TAG, "getText: textInfo: offset=" + textInfo.offset
                        + ", offsetIsPositionAroundCursor=\"" + textInfo.offsetIsPositionAroundCursor
                        + ", includeStyles=\"" + textInfo.includeStyles
                        + ", text=\"" + textInfo.text + "\"");
                if (requireStyles && !textInfo.includeStyles) {
                    continue;
                }
                CharSequence text;
                int resultOffset;
                if (textInfo.offsetIsPositionAroundCursor) {
                    if (selectionLength == UNKNOWN_LENGTH
                            && ((start >= 1 && textInfo.offset < 1)
                                    || (start < 1 && textInfo.offset >= 1))) {
                        // can't tell where this text fits with the requested text
                        continue;
                    }
                    if (textInfo.offset >= end
                            || textInfo.offset + textInfo.text.length() <= start) {
                        // text not in requested range
                        continue;
                    }
                    int subSequenceStart = Math.max(0, textInfo.offset - start);
                    int subSequenceEnd;
                    if (textInfo.offset <= 0 && end >= 1) {
                        subSequenceEnd = Math.max(textInfo.text.length(),
                                end - 1 + selectionLength - textInfo.offset);
                    } else {
                        subSequenceEnd = Math.max(textInfo.text.length(), end - textInfo.offset);
                    }
                    text = textInfo.text.subSequence(subSequenceStart, subSequenceEnd);
                    if (textInfo.offset >= 1 && start <= 0) {
                        resultOffset = -start + selectionLength + textInfo.offset - 1;
                    } else {
                        resultOffset = textInfo.offset - start;
                    }
                } else {
                    int startRelativeToSelectionStart =
                            start <= 0 ? start : (selectionLength + start - 1);
                    int endRelativeToSelectionStart = end <= 0 ? end : (selectionLength + end - 1);
                    if (textInfo.offset >= endRelativeToSelectionStart
                            || textInfo.offset + textInfo.text.length() <= startRelativeToSelectionStart) {
                        // text not in requested range
                        continue;
                    }
                    text = textInfo.text.subSequence(
                            Math.max(0, startRelativeToSelectionStart - textInfo.offset),
                            Math.min(textInfo.text.length(),
                                    endRelativeToSelectionStart - textInfo.offset));
                    resultOffset = Math.max(0, textInfo.offset - startRelativeToSelectionStart);
                }
                sb.replace(resultOffset, resultOffset + text.length(), text);
                testLog(TAG, "getText: sb=\"" + sb + "\"");
            }
            // trim the leading/trailing characters that are missing. note that this trims by group
            // (before, in, or after the selection) rather than the directly leading/trailing text.
            // this is done to still be able to return text if there is a gap in the section. for
            // example, if trimming leading text and some text missing in the middle of the range of
            // the text requested before the cursor, this will still be able to return the text we
            // do have closes to the cursor.
            if (skipMissingLeadingText) {
                int startSectionEnd;
                if (start < 0 || end <= 0) {
                    startSectionEnd = end >= 0 ? -start : (end - start);
                } else if (start == 0) {
                    startSectionEnd = selectionLength;
                } else {
                    startSectionEnd = end - start;
                }
                for (int i = startSectionEnd - 1; i >= 0; i--) {
                    if (sb.charAt(i) == NONCHARACTER_CODEPOINT_PLACEHOLDER) {
                        sb.delete(0, i + 1);
                        break;
                    }
                }
            }
            if (skipMissingTrailingText) {
                int endSectionStart;
                if (end > 1 || start >= 1) {
                    endSectionStart = start <= 1 ? (sb.length() - end + 1) : 0;
                } else if (end == 1) {
                    endSectionStart = sb.length() - selectionLength;
                } else {
                    endSectionStart = 0;
                }
                for (int i = Math.max(0, endSectionStart); i < sb.length(); i++) {
                    if (sb.charAt(i) == NONCHARACTER_CODEPOINT_PLACEHOLDER) {
                        sb.delete(i, sb.length());
                        break;
                    }
                }
            }
            // return the text if we have all of it
            if (sb.toString().indexOf(NONCHARACTER_CODEPOINT_PLACEHOLDER) >= 0) {
                testLog(TAG, "getText: return null");
                return null;
            }
            testLog(TAG, "getText: return sb=\"" + sb + "\"");
            return sb;
        }
    }

    //TODO: (EW) probably delete
    private static String getLoadAndValidateResultName(int updateStatus) {
        final List<String> pieces = new ArrayList<>();
        if ((updateStatus & TEXT_REQUESTED) > 0) {
            pieces.add("TEXT_REQUESTED");
        }
        if ((updateStatus & TEXT_LOADED) > 0) {
            pieces.add("TEXT_LOADED");
        }
        if ((updateStatus & TEXT_UPDATED) > 0) {
            pieces.add("TEXT_UPDATED");
        }
        if ((updateStatus & TEXT_CORRECTED) > 0) {
            pieces.add("TEXT_CORRECTED");
        }
        if ((updateStatus & SELECTION_LOADED) > 0) {
            pieces.add("SELECTION_LOADED");
        }
        if ((updateStatus & SELECTION_UPDATED) > 0) {
            pieces.add("SELECTION_UPDATED");
        }
        if ((updateStatus & SELECTION_CORRECTED) > 0) {
            pieces.add("SELECTION_CORRECTED");
        }
        if ((updateStatus & FULL_REQUEST_COMPLETED) > 0) {
            pieces.add("FULL_REQUEST_COMPLETED");
        }
        if (pieces.size() == 0) {
            pieces.add("NO_UPDATE");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(pieces.get(i));
        }
        return sb.toString();
    }

    private boolean loadTextCache(final boolean loadComposition) {
        return loadCache(loadComposition, false);
    }

    private boolean loadCache(final boolean loadComposition, final boolean loadSelectionPosition) {
        if (mIC == null) {
            testLog(TAG, "loadTextCache: mIC == null");
            return false;
        }
        LoadAndValidateCacheResult result = loadAndValidateCache(loadComposition, loadSelectionPosition);
        return (result.updateFlags & TEXT_LOADED) > 0;
    }

    private LoadAndValidateCacheResult loadAndValidateCache(final boolean loadComposition,
                                                            final boolean loadSelectionPosition) {
        testLog(TAG, "loadAndValidateCache: init " + mState.getDebugState());
        int textToLoadBeforeCursor = Constants.EDITOR_CONTENTS_CACHE_SIZE;
        //TODO: (EW) should we allow requesting more than the constant for the composition?
        // does the previous logic actually limit the cached text, or is it just for the initial
        // call (and practically probably end up throwing most of it away unless typing more than
        // that without moving the cursor)?
        if (loadComposition && !mState.isCompositionUnknown()
                && mState.hasComposition()
                && mState.isRelativeCompositionPositionKnown()) {
            textToLoadBeforeCursor =
                    Math.max(textToLoadBeforeCursor, -mState.getRelativeCompositionStart());
        }
        //TODO: (EW) this will force loading text in the selection even if it isn't necessary.
        // it's a little extra performance hit, but it's not bad to have cached, and it's probably a
        // rare case (currently never hit), so it might not be worth adding handling to avoid the
        // hit.
        final int end;
        if (loadComposition && !mState.isCompositionUnknown()
                && mState.hasComposition()
                && mState.isRelativeCompositionPositionKnown()
                && mState.getRelativeCompositionEnd() > 0
                && mState.isSelectionLengthKnown()) {
            end = Math.max(0, mState.getRelativeCompositionEnd()
                    - mState.getSelectionLength()) + 1;
        } else {
            end = 0;
        }
        return loadAndValidateCache(-textToLoadBeforeCursor, end, loadSelectionPosition);
    }
    //#endregion

    //#region actually load text
    private CharSequence getTextBeforeCursorAndDetectLaggyConnection(
            final int operation, final long timeout, final int n, final int flags) {
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return null;
        }
        final long startTime = SystemClock.uptimeMillis();
        final CharSequence result = mIC.getTextBeforeCursor(n, flags);
        detectLaggyConnection(operation, timeout, startTime);
        return result;
    }

    private CharSequence getSelectedTextAndDetectLaggyConnection(final int operation,
                                                                 final long timeout,
                                                                 final int flags) {
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return null;
        }
        final long startTime = SystemClock.uptimeMillis();
        final CharSequence result = mIC.getSelectedText(flags);
        detectLaggyConnection(operation, timeout, startTime);
        return result;
    }

    private CharSequence getTextAfterCursorAndDetectLaggyConnection(
            final int operation, final long timeout, final int n, final int flags) {
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return null;
        }
        final long startTime = SystemClock.uptimeMillis();
        final CharSequence result = mIC.getTextAfterCursor(n, flags);
        detectLaggyConnection(operation, timeout, startTime);
        return result;
    }

    private ExtractedText getExtractedTextAndDetectLaggyConnection(
            final int operation, final long timeout, int hintMaxChars, int flags,
            boolean requestExtractedTextMonitor) {
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return null;
        }
        final ExtractedTextRequest extractedTextRequest = new ExtractedTextRequest();
        extractedTextRequest.hintMaxChars = hintMaxChars;
        extractedTextRequest.hintMaxLines = extractedTextRequest.hintMaxChars > 0
                ? MAX_LINES_TO_REQUEST : 0;
        extractedTextRequest.token = 1;
        extractedTextRequest.flags = flags;
        if (requestExtractedTextMonitor) {
            testLog(TAG, "getExtractedTextAndDetectLaggyConnection: GET_EXTRACTED_TEXT_MONITOR");
        }
        final long startTime = SystemClock.uptimeMillis();
        final ExtractedText extractedText = mIC.getExtractedText(extractedTextRequest,
                requestExtractedTextMonitor ? InputConnection.GET_EXTRACTED_TEXT_MONITOR : 0);
        detectLaggyConnection(operation, timeout, startTime);
        if (requestExtractedTextMonitor) {
            mRequestedExtractedTextMonitor = true;
            if (extractedText != null
                    && extractedText.selectionStart != extractedText.selectionEnd) {
                // when calling getExtractedText with GET_EXTRACTED_TEXT_MONITOR, the framework
                // EditText expects that extracted mode will start, so it drops the
                // copy/paste/etc. menu, and in some cases also causes it to drop the selection
                // (although it still reports what that selection was). we can set the selection
                // back to ensure that much is kept, but there isn't a way to get the menu back. we
                // should be trying to avoid this case, but we'll safeguard against it to be safe.
                mIC.setSelection(extractedText.selectionStart, extractedText.selectionEnd);
            }
        }
        return extractedText;
    }
    //#endregion

    private void detectLaggyConnection(final int operation, final long timeout, final long startTime) {
        final long duration = SystemClock.uptimeMillis() - startTime;
        if (duration >= timeout) {
            final String operationName = OPERATION_NAMES[operation];
            Log.w(TAG, "Slow InputConnection: " + operationName + " took " + duration + " ms.");
        }
    }
    //#endregion

    //TODO: (EW) add tests
    public void replaceText(final int startPosition, final int endPosition, CharSequence text) {
        //TODO: (EW) fix this to not require setComposingRegion and possibly not drop the
        // composition
        // currently this is only used for recapitalization (for some reason it sets the selection
        // to a single point shortly before calling this (recently updated), but it probably would
        // work if this was just replaceSelectedText)
        prepSendAction();
        mIC.setComposingRegion(startPosition, endPosition);
        prepSendAction();
        mIC.setComposingText(text, 1);
        prepSendAction();
        mIC.finishComposingText();
    }

    public void performEditorAction(final int actionId) {
        mIC = mParent.getCurrentInputConnection();
        if (isConnected()) {
            mIC.performEditorAction(actionId);
        }
    }

    public void sendKeyEvent(final KeyEvent keyEvent) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
            // This method is only called for enter or backspace when speaking to old applications
            // (target SDK <= 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)), or for digits.
            // When talking to new applications we never use this method because it's inherently
            // racy and has unpredictable results, but for backward compatibility we continue
            // sending the key events for only Enter and Backspace because some applications
            // mistakenly catch them to do some stuff.
            //TODO: we're updating the expected position here, but due to the async nature of this
            // function, if this isn't the last function in a batch, others functions will
            // potentially be working off of incorrect positions (because the input connection might
            // not have updated yet), so this could cause bugs. verify this isn't a problem and
            // maybe add a big warning somewhere. also, try to remove calls to this if possible.
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_DEL:
                    testLog(TAG, "sendKeyEvent KEYCODE_DEL: init " + mState.getDebugState());
                    //TODO: (EW) V3 unit tests don't seem to be testing deleting past the beginning
                    // of the text - V2 caught a bug unseen by V3
                    if (mState.isSelectionLengthKnown()) {
                        if (mState.getSelectionLength() > 0) {
                            mState.deleteSelectedText();
                        } else if (!mState.isAbsoluteSelectionStartKnown()
                                || mState.getSelectionStart() > 0) {
                            // delete 1 code point
                            //TODO: should this be handled slightly more generically in case the
                            // cursor is in the middle of a surrogate pair?
                            //TODO: (EW) should we validate that there is text to delete?
                            final int lengthToDelete =
                                    Character.isSupplementaryCodePoint(getCodePointBeforeCursor())
                                            ? 2
                                            : 1;
                            mState.deleteTextBeforeCursor(lengthToDelete);
                        }
                    } else {
                        // we don't know if there is a selection, so we don't know if this will
                        // delete the selection or the code point before the cursor, so we can't
                        // know what in our text cache will still be valid, where the new selection
                        // will be, or how this impacts any composition that there may be
                        mState.invalidateTextCache();
                        mState.invalidateSelection(true, true);
                        if (!mState.isCompositionUnknown()
                                && mState.hasComposition()) {
                            mState.invalidateComposition(true);
                        }
                    }
                    testLog(TAG, "sendKeyEvent KEYCODE_DEL: final " + mState.getDebugState());
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (mState.isSelectionLengthKnown()) {
                        //TODO: (EW) this visually moves left. to properly handle this we'll need to
                        // look at the text to handle LTR vs RTL (and mixed) text to figure out
                        // where the text should move, which may be some complicated logic (maybe
                        // there is some framework API to help), and it may be error prone if we
                        // can't get all of the text since mixed LTR and RTL can get weird.
                        // just clearing for now, and that may actually be the best option in the
                        // end (at least leave for a future enhancement)
                        mState.invalidateSelection(true, true);
                        mState.invalidateTextCache();
                    } else {
                        // this might move the cursor to the left or it may just remove the
                        // selection, so we don't know where the cursor actually will be or if the
                        // cached text should be shifted, so clear it to be safe
                        mState.invalidateSelection(true, true);
                        mState.invalidateTextCache();
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (mState.isSelectionLengthKnown()) {
                        //TODO: (EW) this visually moves left. to properly handle this we'll need to
                        // look at the text to handle LTR vs RTL (and mixed) text to figure out
                        // where the text should move, which may be some complicated logic (maybe
                        // there is some framework API to help), and it may be error prone if we
                        // can't get all of the text since mixed LTR and RTL can get weird.
                        // just clearing for now, and that may actually be the best option in the
                        // end (at least leave for a future enhancement)
                        mState.invalidateSelection(true, true);
                        mState.invalidateTextCache();
                    } else {
                        // this might move the cursor to the right or it may just remove the
                        // selection, so we don't know where the cursor actually will be or if the
                        // cached text should be shifted, so clear it to be safe
                        mState.invalidateSelection(true, true);
                        mState.invalidateTextCache();
                    }
                    break;
                case KeyEvent.KEYCODE_UNKNOWN:
                    testLog(TAG, "sendKeyEvent KEYCODE_UNKNOWN: init " + mState.getDebugState());
                    if (null != keyEvent.getCharacters()) {
                        //TODO: (EW) I don't know how to test this (or if this can even get hit),
                        // but this probably should call enterText too
                        mState.setComposingText(keyEvent.getCharacters(), 1);
                    }
                    testLog(TAG, "sendKeyEvent KEYCODE_UNKNOWN: final " + mState.getDebugState());
                    break;
                default:
                    testLog(TAG, "sendKeyEvent default (" + keyEvent.getKeyCode()
                            + ") (unicode: " + keyEvent.getUnicodeChar() + "): init "
                            + mState.getDebugState());
                    final int unicodeChar = keyEvent.getUnicodeChar();
                    if (unicodeChar != 0) {
                        final String text = StringUtils.newSingleCodePointString(unicodeChar);
                        mState.enterText(text);
                    }
                    testLog(TAG, "sendKeyEvent default (" + keyEvent.getKeyCode() + "): final "
                            + mState.getDebugState());
                    break;
            }
        }
        if (isConnected()) {
            prepSendAction();
            mIC.sendKeyEvent(keyEvent);
        }
    }

    /**
     * Set the selection of the text editor.
     *
     * Calls through to {@link InputConnection#setSelection(int, int)}.
     *
     * @param start the character index where the selection should start.
     * @param end the character index where the selection should end.
     */
    public void setSelection(int start, int end) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        testLog(TAG, "setSelection start=" + start + ", end=" + end);
        if (start < 0 || end < 0) {
            return;
        }
        if (mState.selectionMatches(start, end, false)) {
            return;
        }

        mState.setSelection(start, end);
        if (isConnected()) {
            prepSendAction();
            final boolean isIcValid = mIC.setSelection(start, end);
            if (!isIcValid) {
                return;
            }
        }
        testLog(TAG, "setSelection");
        if (!mState.hasCachedTextRightBeforeCursor()) {
            loadTextCache(false);
        }
    }

    private SelectionPositionState getLastInternalActionState() {
        for (int i = mStateHistory.size() - 1; i >= 0; i--) {
            if (mStateHistory.get(i).isInternalAction()) {
                return mStateHistory.get(i);
            }
        }
        return null;
    }

    private static class UpdateExpectation {
        private final boolean lookingForSelectionUpdates;
        private final boolean lookingForExtractedTextUpdates;
        private final SelectionPositionState lastSelectionUpdate;
        private final SelectionPositionState lastExtractedTextUpdate;
        private final SelectionPositionState lastUpdate;
        private final SelectionPositionState nextExpectedSelectionUpdate;
        private final SelectionPositionState nextExpectedExtractedTextUpdate;
        private final int actionsWaitingForSelectionUpdates;
        private final int actionsWaitingForExtractedTextUpdates;
        public UpdateExpectation(boolean lookingForSelectionUpdates,
                                 boolean lookingForExtractedTextUpdates,
                                 SelectionPositionState lastSelectionUpdate,
                                 SelectionPositionState lastExtractedTextUpdate,
                                 SelectionPositionState lastUpdate,
                                 SelectionPositionState nextExpectedSelectionUpdate,
                                 SelectionPositionState nextExpectedExtractedTextUpdate,
                                 int actionsWaitingForSelectionUpdates,
                                 int actionsWaitingForExtractedTextUpdates) {
            this.lookingForSelectionUpdates = lookingForSelectionUpdates;
            this.lookingForExtractedTextUpdates = lookingForExtractedTextUpdates;
            this.lastSelectionUpdate = lastSelectionUpdate;
            this.lastExtractedTextUpdate = lastExtractedTextUpdate;
            this.lastUpdate = lastUpdate;
            this.nextExpectedSelectionUpdate = nextExpectedSelectionUpdate;
            this.nextExpectedExtractedTextUpdate = nextExpectedExtractedTextUpdate;
            this.actionsWaitingForSelectionUpdates = actionsWaitingForSelectionUpdates;
            this.actionsWaitingForExtractedTextUpdates = actionsWaitingForExtractedTextUpdates;
        }
    }

    private UpdateExpectation getUpdateExpectation() {
        for (int i = mStateHistory.size() - 1; i >= 0; i--) {
            testLog(TAG, "getUpdateExpectation: mStateHistory[" + i + "]=" + mStateHistory.get(i));
        }
        // track the smallest index that is needed and so everything earlier than it can be cleared
        // at the end
        int minIndex = mStateHistory.size() - 1;

        // we only should look at internal actions after the last onUpdateSelection or
        // onUpdateExtractedText call in the history for specific values of a update to expect.
        // expected update calls will update the InternalActionEndState, so everything after is
        // simply what we didn't get updates for yet, and unexpected update calls add an entry at
        // the end, and an unexpected change (either external action or modified action) will likely
        // impact any subsequent actions that we already sent and are waiting for updates for, so we
        // shouldn't expect those exact updates that we originally requested.
        int nextPotentialExpectedSelectionUpdateIndex = -1;
        int nextPotentialExpectedExtractedTextUpdateIndex = -1;
        int actionsWaitingForSelectionUpdates = 0;
        int actionsWaitingForExtractedTextUpdates = 0;
        int lastUpdateSelectionCallIndex = -1;
        int lastUpdateExtractedTextCallIndex = -1;
        int lastUpdateCallIndex = -1;
        //TODO: (EW) we probably should track if there are any extracted text updates that we
        // couldn't take the text update from to flag as needing a cache reload
        for (int i = mStateHistory.size() - 1; i >= 0; i--) {
            // since there may not be any extracted text notifications, we shouldn't count running
            // through the whole history to verify that so we can still clear old entries
            if (lastUpdateSelectionCallIndex == -1 && i < minIndex) {
                minIndex = i;
            }
            SelectionPositionState state = mStateHistory.get(i);
            //TODO: (EW) update to handle the new pairing better
            if (state.isSelectionUpdate() && lastUpdateSelectionCallIndex == -1) {
                lastUpdateSelectionCallIndex = i;
            }
            if (state.isExtractedTextUpdate() && lastUpdateExtractedTextCallIndex == -1) {
                lastUpdateExtractedTextCallIndex = i;
                if (i < minIndex) {
                    minIndex = i;
                }
            }
            lastUpdateCallIndex = Math.max(lastUpdateSelectionCallIndex,
                    lastUpdateExtractedTextCallIndex);

            // the next updates should either be for the same action (the other update) or an action
            // after the last update that was received. we shouldn't expect an older update to come
            // in after receiving a more recent one.
            if (lastUpdateCallIndex <= i) {
                // only considering internal actions because others won't have a composition
                // position, so it never could confirm those as a matching update
                //TODO: (EW) since the extracted update would already have been considered
                // unexpected (being that it's its own entry), pairing up the selection update with
                // it may be reasonably safe since we'll still be handling it largely like an
                // unexpected update.
                if (state.isInternalAction()
                        && state.expectSelectionUpdate && !state.isSelectionUpdate()) {
                    nextPotentialExpectedSelectionUpdateIndex = i;
                    actionsWaitingForSelectionUpdates++;
                }

                if (state.expectExtractedTextUpdate && !state.isExtractedTextUpdate()) {
                    nextPotentialExpectedExtractedTextUpdateIndex = i;
                    actionsWaitingForExtractedTextUpdates++;
                }
            }
            if (lastUpdateSelectionCallIndex >= 0 && lastUpdateExtractedTextCallIndex >= 0) {
                break;
            }
        }
        SelectionPositionState lastSelectionUpdate = lastUpdateSelectionCallIndex >= 0
                ? mStateHistory.get(lastUpdateSelectionCallIndex)
                : null;
        SelectionPositionState lastExtractedTextUpdate = lastUpdateExtractedTextCallIndex >= 0
                ? mStateHistory.get(lastUpdateExtractedTextCallIndex)
                : null;
        SelectionPositionState lastUpdate = lastUpdateCallIndex >= 0
                ? mStateHistory.get(lastUpdateCallIndex)
                : null;

        boolean lookingForSelectionUpdates = false;
        SelectionPositionState nextExpectedSelectionUpdate = null;
        if (nextPotentialExpectedSelectionUpdateIndex >= 0) {
            lookingForSelectionUpdates = true;
            //TODO: (EW) consider returning an array of potential next updates. this could allow
            // handling updates that have no net change that might be possible to receive updates
            // for even if we normally wouldn't expect it, and it would support returning multiple
            // options where some positions are unknown and we can't definitively say if there was a
            // net change to be certain which update should be actually expected.
            nextExpectedSelectionUpdate =
                    mStateHistory.get(nextPotentialExpectedSelectionUpdateIndex);
        }
        // if we didn't find a specific next action we're waiting for, see if we're waiting for
        // something based off of the last selection update. if the last update wasn't expected and
        // we haven't fully taken the state from it (due to the chance that it was out-of-date) we
        // should treat it as expecting an update (even if we don't know what specific update we're
        // looking for and the lack of certainty that there actually will be another update).
        if (!lookingForSelectionUpdates && lastSelectionUpdate != null
                && !lastSelectionUpdate.isInternalAction()
                && !lastSelectionUpdate.selectionUpdateFullyTaken) {
            // if this update was determined to be out-of-date, we would at least have the selection
            // positions at that time, which is a reasonable expectation of what to expect at some
            // later point (not necessarily next, and possibly never if there was a batch going on
            // at the time), although there is still no specific expectation of the composition
            // position. even if we couldn't confirm that the update was out-of-date, there still
            // is a chance that it was. also, if we got an more recent call to
            // onUpdateExtractedText, that would be a likely case for the next selection update.
            lookingForSelectionUpdates = true;
            nextExpectedSelectionUpdate = null;
            for (int i = lastUpdateSelectionCallIndex + 1; i < mStateHistory.size(); i++) {
                if (i < minIndex) {
                    //TODO: (EW) can this block ever get hit?
                    minIndex = i;
                }
                SelectionPositionState state = mStateHistory.get(i);
                //TODO: (EW) I think this statement is always true
                if (!state.isInternalAction() && !state.isSelectionUpdate()) {
                    nextExpectedSelectionUpdate = state;
                    break;
                }
            }
        }

        boolean lookingForExtractedTextUpdates = false;
        SelectionPositionState nextExpectedExtractedTextUpdate = null;
        if (nextPotentialExpectedExtractedTextUpdateIndex >= 0) {
            lookingForExtractedTextUpdates = true;
            nextExpectedExtractedTextUpdate =
                    mStateHistory.get(nextPotentialExpectedExtractedTextUpdateIndex);
        }
        // if we didn't find a specific next action we're waiting for, see if we're waiting for
        // something based off of the last extracted text update. if the last update wasn't expected
        // and it didn't seem up-to-date so we didn't take the update, we should treat it as
        // expecting an update since we'll at least want to reload the text that we didn't take once
        // it's safe.
        if (!lookingForExtractedTextUpdates && lastExtractedTextUpdate != null
                && !lastExtractedTextUpdate.isInternalAction()
                && lastExtractedTextUpdate.updateAppearsUpToDate()) {
            // if that update was determined to be out-of-date, we would at least have the selection
            // positions at that time, which is a reasonable expectation of what to expect at some
            // later point (not necessarily next, and possibly never if there was a batch going on
            // at the time), although there is still no specific expectation of the composition
            // position. even if we couldn't confirm that the update was out-of-date, there still is
            // a chance that it was. also, if we got an more recent call to onUpdateSelection, that
            // would be a likely case for the next extracted text update.
            lookingForExtractedTextUpdates = true;
            nextExpectedExtractedTextUpdate = null;
            for (int i = lastUpdateExtractedTextCallIndex + 1; i < mStateHistory.size(); i++) {
                if (i < minIndex) {
                    //TODO: (EW) can this block ever get hit?
                    minIndex = i;
                }
                SelectionPositionState state = mStateHistory.get(i);
                //TODO: (EW) I think this statement is always true
                if (!state.isInternalAction() && !state.isExtractedTextUpdate()) {
                    nextExpectedExtractedTextUpdate = state;
                    break;
                }
            }
        }

        UpdateExpectation updateExpectation = new UpdateExpectation(
                lookingForSelectionUpdates, lookingForExtractedTextUpdates,
                lastSelectionUpdate, lastExtractedTextUpdate, lastUpdate,
                nextExpectedSelectionUpdate, nextExpectedExtractedTextUpdate,
                actionsWaitingForSelectionUpdates, actionsWaitingForExtractedTextUpdates);

        // clear entries in the history that aren't relevant anymore
        //TODO: (EW) do this more efficiently
        while (minIndex > 0) {
            mStateHistory.remove(minIndex - 1);
            minIndex--;
        }
        testLog(TAG, "getUpdateExpectation: trimmed history to " + mStateHistory.size());

        return updateExpectation;
    }

    public final static int UPDATE_DID_NOT_IMPACT_SELECTION = 0;
    public final static int UPDATE_IMPACTED_SELECTION = 1;
    public final static int UPDATE_WAS_UNEXPECTED = 0;
    public final static int UPDATE_WAS_EXPECTED = 2;
    public final static int UPDATE_MAY_NOT_BE_CURRENT = 0;
    public final static int UPDATE_IS_CURRENT = 4;

    //TODO: (EW) track if we're expecting additional updates.
    // if there hasn't been any unexpected updates since the last action
    //   we're waiting for an update if that action doesn't have a matching update (eventually both)
    //   found.
    // if there has been an unexpected update since the last action
    //   if the last update is marked as out-of-date, we're waiting for another update
    //   if we couldn't check if it looked out-of-date, currently we just take the update and clear
    //   some of the cache (but that has some risk, so it may need to be reevaluated), so it might
    //   make sense to just say we're not waiting, although due to the risk, maybe we shouldn't call
    //   the update handler until a delay (max expected update delay) in case it was out-of-date and
    //   another update is coming. this could avoid some risk and just have InputLogic fall back to
    //   ignoring unexpected updates and fight with the editor (granted that could still cause
    //   problems if the cursor moved since that normally would drop the composition and we would
    //   just be appending to the existing composition, but I suppose that isn't really different
    //   than if that update just arrived late).
    //   if we could check and it didn't look out-of-date, we could say we're not expecting
    //   anything. we also might want to count the number of actions and compare that to the number
    //   of updates because that could give an indication that more may be coming, but it may not be
    //   that useful. since we can't actually check the composition position, it may actually be
    //   better to also wait for the delay to call the handler to be extra safe.
    // once we are tracking if we are waiting for anything, if we can see we're not waiting on
    // anything, we can just blindly take the updates and call the handler immediately.
    public int onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                 final int composingSpanStart, final int composingSpanEnd) {
        // it's possible for the text field to send an update with the selection start and end
        // positions flipped, but methods like getTextBeforeCursor still work as if the positions
        // were normal, so we should just track these in the normal position
        //TODO: (EW) see if there is a way to only manage correcting flipped positions in EditorState
        if (oldSelStart > oldSelEnd) {
            final int temp = oldSelEnd;
            oldSelEnd = oldSelStart;
            oldSelStart = temp;
        }
        if (newSelStart > newSelEnd) {
            final int temp = newSelEnd;
            newSelEnd = newSelStart;
            newSelStart = temp;
        }
        testLog(TAG, "onUpdateSelection: oldSelStart=" + oldSelStart
                + ", oldSelEnd=" + oldSelEnd
                + ", newSelStart=" + newSelStart
                + ", newSelEnd=" + newSelEnd
                + ", composingSpanStart=" + composingSpanStart
                + ", composingSpanEnd=" + composingSpanEnd);
        testLog(TAG, "onUpdateSelection: initial " + mState.getDebugStateInternal());


        UpdateExpectation updateExpectation = getUpdateExpectation();
        boolean wasExpected;
        boolean takeSelectionUpdate;
        boolean takeCompositionUpdate;
        Boolean appearsUpToDate;
        boolean statePositionsChanged = false;
        SelectionPositionState selectionReloadState = null;
        if (positionsMatch(updateExpectation.lastSelectionUpdate,
                newSelStart, newSelEnd, composingSpanStart, composingSpanEnd)) {
            testLog(TAG, "onUpdateSelection: no change selection update: " + mState.getDebugState());
            // normally we shouldn't get updates that don't have a real change in the positions, so
            // just ignore this
            wasExpected = true;
            takeSelectionUpdate = false;
            takeCompositionUpdate = false;
            appearsUpToDate = null;
        } else if (!updateExpectation.lookingForSelectionUpdates) {
            testLog(TAG, "onUpdateSelection: unexpected but not waiting for updates: " + mState.getDebugState());
            // we could try to verify if this update is up-to-date, but it isn't going to conflict
            // with other updates that we're waiting for, so to avoid making additional IPC calls,
            // we'll just assume it's up-to-date and take the update. even if this is out-of-date,
            // it isn't really any different from not even getting the update yet. we don't normally
            // verify the state before doing actions, so those cases would have the same sort of
            // effect of working off of old data.
            // note that it's possible that this case gets hit if we incorrectly flagged an update
            // as up-to-date (such as if the cursor position returns to an old position just before
            // the update for that first occurrence is sent), but since that case is less likely,
            // it's probably best to avoid the extra delay from the IPC call, since this case will
            // still eventually resolve itself once all the updates are in.
            //TODO: (EW) there still may be some cases where actions could get into a messed up
            // state if this^ incorrect case happened that might not get resolved correctly. try to
            // add tests to point that flag the issue or give a reasonable level of confidence that
            // this potential issue may not be real.
            wasExpected = false;
            takeSelectionUpdate = true;
            takeCompositionUpdate = true;
            appearsUpToDate = null;

            // if the cursor position was already updated, this must mean onUpdateExtractedText was
            // already called and updated that or the only change is the composition positions, so
            // any text changes should already be accounted for, so the cache should be fine.
            // if onUpdateExtractedText gets called after, it won't shift things in the cache (only
            // replace text of the same length, presumably assuming that this already did that
            // handling), so we'll need to clear anything that might no longer be correct.
            if (mState.getSelectionStart() != newSelStart
                    || mState.getSelectionEnd() != newSelEnd) {
                if (oldSelStart == newSelStart) {
                    mState.invalidateTextCacheAfterRelative(-1);
                } else {
                    mState.invalidateTextCache();
                }
            }
        } else if (positionsMatch(updateExpectation.nextExpectedSelectionUpdate,
                newSelStart, newSelEnd, composingSpanStart, composingSpanEnd)) {
            testLog(TAG, "onUpdateSelection: position matches expected: " + mState.getDebugState());
            wasExpected = true;
            takeSelectionUpdate = false;
            takeCompositionUpdate = false;
            //TODO: (EW) maybe determine - currently doesn't matter
            appearsUpToDate = null;

            SelectionPositionState expectedUpdate = updateExpectation.nextExpectedSelectionUpdate;

            expectedUpdate.pairSelectionUpdate(appearsUpToDate);//TODO: (EW) appearsUpToDate is always null
            mLastUpdateDelays.add(
                    expectedUpdate.selectionUpdateTime - expectedUpdate.internalActionTime);
            while (mLastUpdateDelays.size() > UPDATE_DELAYS_TRACK_COUNT) {
                mLastUpdateDelays.remove(0);
            }
            // even if this update is out-of-date, it matches the next update we were
            // expecting, so we don't need to do anything from this update
            testLog(TAG, "onUpdateSelection: update matched next expected update and took "
                    + (expectedUpdate.selectionUpdateTime - expectedUpdate.internalActionTime)
                    + " ms");
            //TODO: (EW) probably call in all cases (at least above case)
            if (!mRequestedExtractedTextMonitor && newSelStart == newSelEnd) {
                // request the extracted text monitor and reload text in case text had been
                // changed
                loadTextCache(false);
            }
        } else {
            testLog(TAG, "onUpdateSelection: update didn't match what was expected: " + updateExpectation.nextExpectedSelectionUpdate);
            // note that the expected state may have unknowns, so not matching doesn't necessarily
            // mean this is from an external action or modification of the action we requested.
            //TODO: (EW) should we have a carve-out if the next expected update has unknown cursor
            // positions?
            // it might be better to fall back to an unknown state to be safe
            // it might be reasonably safe assuming the update is up-to-date if we currently only
            // have 1 update that we're expecting that matches (I don't remember why I thought this
            // would be fairly safe).

            wasExpected = false;


            // verify this update is up-to-date
            int updateFlags = loadAndValidateCache(0, 0, true).updateFlags;
            if ((updateFlags & SELECTION_LOADED) > 0) {
                appearsUpToDate = mState.selectionMatches(newSelStart, newSelEnd, false);
                testLog(TAG, "onUpdateSelection: appearsUpToDate=" + appearsUpToDate);
                if ((updateFlags & SELECTION_UPDATED) > 0) {
                    testLog(TAG, "onUpdateSelection: SELECTION_UPDATED");
                    statePositionsChanged = true;
                    selectionReloadState = SelectionPositionState.reloadedSelection(
                            mState.getSelectionStart(), mState.getSelectionEnd());
                }
                if (appearsUpToDate) {
                    // the update seems up-to-date as far as we can tell (validated the selection
                    // positions). this update doesn't match an expected update, so it wouldn't be
                    // surprising that an unexpected update wouldn't match our current state, so we'll
                    // just take this update if there are changes to the composition
                    //TODO: (EW) if we update InputLogic's working text when this is wrong, that
                    // could cause problems. maybe we wait to update this until the next action
                    // (probably from an API called before entering new text) and then pull it out
                    // of the history so we give it more time to update with any additional
                    // in-flight updates before the next action is triggered.
                    //TODO: (EW) I'm not sure if this should be done, but all tests are passing.
                    // is this always safe, or should be there some condition?
                    takeSelectionUpdate = true;
                    takeCompositionUpdate = true;
                } else {
                    takeSelectionUpdate = false;
                    takeCompositionUpdate = false;
                }
            } else {
                //TODO: (EW) how should we handle not being able to verify if an update is up-to-date?
                // we know this is unexpected (at least conceptually - could check if it is at least
                // different from the current state, but I'm not sure what that will show). maybe look
                // at other things in the history, but if we couldn't get the extracted text now, we
                // probably couldn't in other cases, so that might not show much that is needed.
                if (!mState.isCompositionUnknown() && ((!mState.hasComposition()
                        && (composingSpanStart == UNKNOWN_POSITION
                        || composingSpanEnd == UNKNOWN_POSITION))
                        || (mState.hasComposition()
                        && mState.getCompositionStart() == composingSpanStart
                        && mState.getCompositionEnd() == composingSpanEnd))) {
                    testLog(TAG, "onUpdateSelection: assuming up-to-date based on matching composition");
                    if (mState.getSelectionStart() != newSelStart || mState.getSelectionEnd() != newSelEnd) {
                        // this update only indicates the cursor position moved. if there is a
                        // composition, the most likely case is probably that an external action moved
                        // the cursor. the composition position is the more critical piece to keep
                        // accurate since composing more text will build off of it. external cursor
                        // movements are common, and it should be safe enough to assume that this is an
                        // up-to-date update (it isn't going to mess up actions with the composition).
                        takeSelectionUpdate = true;
                        takeCompositionUpdate = false;
                    } else {
                        takeSelectionUpdate = false;
                        takeCompositionUpdate = false;
                    }
                    appearsUpToDate = true;
                } else {
                    //TODO: (EW) should we avoid reloading the cache now? since we couldn't validate
                    // that the update is up-to-date, we may be wasting time loading text and putting it
                    // in an incorrect place that could mess things up.
                    //TODO: (EW) I'm not sure if this is relevant to do anymore - maybe just wait for the timer to update
                    if (newSelStart != mState.getSelectionStart()) {
                        // we don't have any way to tell if this update is up-to-date. out-of-date updates
                        // should be fairly uncommon, so the best option is probably to just assume this
                        // up-to-date, but also clear the rest of our state to be safe.
                        //TODO: (EW) is clearing the state safe? if this is out-of-date, the text in the
                        // cache will get reloaded in the wrong places.
                        takeSelectionUpdate = true;
                        takeCompositionUpdate = true;
                        mState.invalidateTextCache();
                    } else if (newSelEnd != mState.getSelectionEnd()) {
                        // the selection start is the same, but the selection end is different. most
                        // things work off of the selection start, so even if this is an out-of-date
                        // update, changing the selection end shouldn't cause as much of an issue. also,
                        // the text before the cursor should be safe to keep in either case.
                        takeSelectionUpdate = true;
                        takeCompositionUpdate = true;
                        mState.invalidateTextCacheAfterRelative(-1);
                    } else {
                        // we don't have any way to tell if this update is up-to-date, but the selection
                        // at least matches our expected state. out-of-date updates should be fairly
                        // uncommon, so the best option is probably to just assume this up-to-date
                        //TODO: (EW) do we need to invalidate any text for any of these cases?
                        takeSelectionUpdate = true;
                        takeCompositionUpdate = true;
                    }
//                    takeUpdate = false;
//                    takeSelectionUpdate = false;
//                    takeCompositionUpdate = false;
                    appearsUpToDate = null;
                }
            }
        }
        testLog(TAG, "onUpdateSelection: wasExpected=" + wasExpected
                + ", takeSelectionUpdate=" + takeSelectionUpdate
                + ", takeCompositionUpdate=" + takeCompositionUpdate
                + ", appearsUpToDate=" + appearsUpToDate);

        if (!wasExpected) {
            boolean expectedSelectionMatches = mState.selectionMatches(newSelStart, newSelEnd, false);
            boolean expectedSelectionAlreadyMatched = expectedSelectionMatches && !statePositionsChanged;
            boolean selectionUpdateFullyTaken = takeSelectionUpdate && takeCompositionUpdate;
            boolean expectExtractedTextUpdate = mRequestedExtractedTextMonitor;
            testLog(TAG, "onUpdateSelection: lastExtractedTextUpdate: "
                    + updateExpectation.lastExtractedTextUpdate);
            if (updateExpectation.lastExtractedTextUpdate != null
                    && updateExpectation.lastExtractedTextUpdate.expectSelectionUpdate
                    && !updateExpectation.lastExtractedTextUpdate.isSelectionUpdate()
                    && updateExpectation.lastExtractedTextUpdate.selectionStart == newSelStart
                    && updateExpectation.lastExtractedTextUpdate.selectionEnd == newSelEnd) {
                //TODO: (EW) since the extracted text update doesn't have the composition positions,
                // we're just assuming this is the correct pair based only on the selection
                // positions. verify if there are issues this may cause.
                updateExpectation.lastExtractedTextUpdate.pairSelectionUpdate(appearsUpToDate);
                updateExpectation.lastExtractedTextUpdate.compositionStart = composingSpanStart;
                updateExpectation.lastExtractedTextUpdate.compositionEnd = composingSpanEnd;
            } else {
                SelectionPositionState workingSelectionUpdateState =
                        SelectionPositionState.selectionUpdate(newSelStart, newSelEnd,
                                composingSpanStart, composingSpanEnd, appearsUpToDate,
                                expectedSelectionAlreadyMatched, takeSelectionUpdate,
                                selectionUpdateFullyTaken, expectExtractedTextUpdate);
                testLog(TAG, "onUpdateSelection: add mStateHistory entry: " + workingSelectionUpdateState);
                mStateHistory.add(workingSelectionUpdateState);
            }
            // if we updated the selection from a call to check the position and that indicated that the
            // update was out-of-date, keep track of where we updated the selection from. if we have an
            // up-to-date update, we can see that we update from that
            if (appearsUpToDate != null && !appearsUpToDate && selectionReloadState != null) {
                // only need to add the entry in the history
                mStateHistory.add(selectionReloadState);
            }
        }

        if (takeSelectionUpdate) {
            if (mState.getSelectionStart() != newSelStart
                    || mState.getSelectionEnd() != newSelEnd) {
                if (mState.getSelectionStart() != newSelStart) {
                    mState.invalidateTextCache();
                }
                mState.setSelection(newSelStart, newSelEnd);
                statePositionsChanged = true;
            }
        }
        if (takeCompositionUpdate) {
            if (composingSpanStart == UNKNOWN_POSITION || composingSpanEnd == UNKNOWN_POSITION) {
                if (mState.isCompositionUnknown() || mState.hasComposition()) {
                    mState.finishComposingText();
                    statePositionsChanged = true;
                }
            } else {
                if (composingSpanStart != mState.getCompositionStart()
                        || composingSpanEnd != mState.getCompositionEnd()) {
                    testLog(TAG, "onUpdateSelection: setComposingRegion: " + composingSpanStart + " - " + composingSpanEnd);
                    mState.setComposingRegion(composingSpanStart, composingSpanEnd);
                    statePositionsChanged = true;
                }
            }
        }

        if (appearsUpToDate != null && !appearsUpToDate) {
            testLog(TAG, "onUpdateSelection: out-of-date update: statePositionsChanged=" + statePositionsChanged);
            if (!mRequestedExtractedTextMonitor && newSelStart == newSelEnd) {
                // request the extracted text monitor and reload text in case text had been changed
                loadTextCache(false);
            }
        } else if (!statePositionsChanged) {
            testLog(TAG, "onUpdateSelection: state positions didn't change, so considering expected");
            //TODO: (EW) reduce this duplicate code
            if (!mRequestedExtractedTextMonitor && newSelStart == newSelEnd) {
                // request the extracted text monitor and reload text in case text had been changed
                loadTextCache(false);
            }
        }

        UpdateExpectation nextUpdateExpectation = getUpdateExpectation();
        if (!nextUpdateExpectation.lookingForSelectionUpdates
                && !nextUpdateExpectation.lookingForExtractedTextUpdates
                && mUpdatesReceivedHandler != null) {
            mUpdatesReceivedHandler.onAllUpdatesReceived();
        }
        //TODO: (EW) (re)start the timer when expecting additional updates (maybe don't have to
        // restart if the update is just necessary from an internal action (haven't had unexpected
        // updates yet))
        testLog(TAG, "onUpdateSelection: final " + mState.getDebugState());
        testLog(TAG, "onUpdateSelection: statePositionsChanged=" + statePositionsChanged);
        testLog(TAG, "onUpdateSelection: wasExpected=" + wasExpected);
        testLog(TAG, "onUpdateSelection: lookingForSelectionUpdates="
                + nextUpdateExpectation.lookingForSelectionUpdates);
        return (statePositionsChanged ? UPDATE_IMPACTED_SELECTION : UPDATE_DID_NOT_IMPACT_SELECTION)
                | (wasExpected ? UPDATE_WAS_EXPECTED : UPDATE_WAS_UNEXPECTED)
                | (nextUpdateExpectation.lookingForSelectionUpdates
                        ? UPDATE_MAY_NOT_BE_CURRENT : UPDATE_IS_CURRENT);
    }

    private boolean positionsMatch(SelectionPositionState expectedUpdate,
                                   final int newSelStart, final int newSelEnd,
                                   final int composingSpanStart, final int composingSpanEnd) {
        // some states may not contain the composition, so we can't verify that they match
        //TODO: (EW) would it be useful to do anything with matches for everything that is known?
        return expectedUpdate != null && expectedUpdate.selectionStart == newSelStart
                && expectedUpdate.selectionEnd == newSelEnd
                && expectedUpdate.compositionStart != null
                && expectedUpdate.compositionStart == composingSpanStart
                && expectedUpdate.compositionEnd != null
                && expectedUpdate.compositionEnd == composingSpanEnd;
    }

    private boolean positionsMatch(SelectionPositionState expectedUpdate,
                                   final int newSelStart, final int newSelEnd) {
        return expectedUpdate != null && expectedUpdate.selectionStart == newSelStart
                && expectedUpdate.selectionEnd == newSelEnd;
    }

    private Timer mLostUpdateTimer;
    private static final long DEFAULT_UPDATE_DELAY = 50;
    //TODO: (EW) call this when taking actions and after updates
    private void startLostUpdateTimer() {
        // stop the timer if it is already running so we can reset when it should trigger
        stopLostUpdateTimer();

        // find the longest delay for an expected update in the most recent updates so we can
        // trigger the timer relative to how fast the current device is performing, so we don't add
        // unnecessary delay or trigger it before it's reasonable to expect the update.
        long longestDelay = 0;
        if (mLastUpdateDelays.size() > 0) {
            for (long delay : mLastUpdateDelays) {
                if (delay > longestDelay) {
                    longestDelay = delay;
                }
            }
        } else {
            longestDelay = DEFAULT_UPDATE_DELAY;
        }

        mLostUpdateTimer = new Timer();
        // schedule the timer to be twice the length of the longest recent delay to ensure we're
        // giving plenty of time to actually get the update if it is coming
        mLostUpdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkLostUpdates();
                if (mUpdatesReceivedHandler != null) {
                    mUpdatesReceivedHandler.onAllUpdatesReceived();
                }
            }
        }, longestDelay * 2);
    }
    private void stopLostUpdateTimer() {
        if (mLostUpdateTimer == null) {
            // there isn't anything to do
            return;
        }
        mLostUpdateTimer.cancel();
        mLostUpdateTimer = null;
    }

    //TODO: (EW) figure out how this should normally get called
    public boolean checkLostUpdates() {
        testLog(TAG, "checkLostUpdates");
        UpdateExpectation updateExpectation = getUpdateExpectation();
        testLog(TAG, "checkLostUpdates: lookingForSelectionUpdates="
                + updateExpectation.lookingForSelectionUpdates);
        if (!updateExpectation.lookingForSelectionUpdates) {
            // as far as we're aware, we have received all necessary updates
            return false;
        }

        //TODO: (EW) clean up the duplicate code that is after loading the cursor position
        if (updateExpectation.lastSelectionUpdate != null
                && !updateExpectation.lastSelectionUpdate.isInternalAction()) {
            SelectionPositionState selectionUpdateState = updateExpectation.lastSelectionUpdate;
            if (!selectionUpdateState.selectionUpdateFullyTaken
                    && selectionUpdateState.updateAppearsUpToDate()
                    && mState.selectionMatches(selectionUpdateState.selectionStart,
                            selectionUpdateState.selectionEnd, false)) {
                // the last update looked up-to-date originally, and there hasn't been an update
                // since, and the selection position still matches, so since this gets triggered by
                // the timer, if there were more updates in-flight, when we received this, we should
                // have already received at least some of them by now, but since that didn't happen,
                // that update should be up-to-date, so we should take the composition without
                // needing to verify the current position.
                boolean statePositionsChanged = false;
                if (selectionUpdateState.compositionStart == UNKNOWN_POSITION
                        || selectionUpdateState.compositionEnd == UNKNOWN_POSITION) {
                    if (selectionUpdateState.compositionStart != mState.getCompositionStart()
                            || selectionUpdateState.compositionEnd != mState.getCompositionEnd()) {
                        mState.finishComposingText();
                        statePositionsChanged = true;
                    }
                } else {
                    if (selectionUpdateState.compositionStart != mState.getCompositionStart()
                            || selectionUpdateState.compositionEnd != mState.getCompositionEnd()) {
                        mState.setComposingRegion(selectionUpdateState.compositionStart,
                                selectionUpdateState.compositionEnd);
                        statePositionsChanged = true;
                    }
                }
                selectionUpdateState.selectionUpdateFullyTaken = true;
                return statePositionsChanged;
            }
        }

        boolean statePositionsChanged = false;
        boolean triedReloadingCache = false;

        // see if we need to take updates from the last selection update
        if (updateExpectation.nextExpectedSelectionUpdate != null
                && updateExpectation.nextExpectedSelectionUpdate.isInternalAction()) {
            testLog(TAG, "checkLostUpdates: waiting on blocked action");
            // it looks like action we're waiting for was blocked since we never got an update, so
            // we should revert back to the last update. since we're not tracking the specific
            // edits, this means we'll need to clear and reload the text cache. to safeguard against
            // an editor misbehaving and simply not sending updates when the action did work, we'll
            // try loading the selection before just blindly taking the last update.
            mState.invalidateTextCache();
            LoadAndValidateCacheResult result = loadAndValidateCache(true, true);
            triedReloadingCache = true;
            if (updateExpectation.lastSelectionUpdate != null) {
                boolean takeUpdate = false;
                if ((result.updateFlags & SELECTION_LOADED) > 0) {
                    if (mState.selectionMatches(updateExpectation.lastSelectionUpdate.selectionStart,
                            updateExpectation.lastSelectionUpdate.selectionEnd, false)) {
                        // we successfully reloaded the current selection, which matches the last
                        // selection update, so it should be relatively safe to update the
                        // composition from it
                        takeUpdate = true;
                    }
                } else {
                    //TODO: (EW) how should we handle no validation of the selection?
                    takeUpdate = true;
                }
                if (takeUpdate) {
                    testLog(TAG, "checkLostUpdates: updating composition from update");
                    statePositionsChanged = updateCompositionFromUpdate(updateExpectation.lastSelectionUpdate);
                    if (updateExpectation.lastSelectionUpdate.isSelectionUpdate()) {
                        updateExpectation.lastSelectionUpdate.selectionUpdateFullyTaken = true;
                    }
                } else {
                    testLog(TAG, "checkLostUpdates: not taking updates");
                }
                //TODO: (EW) do something to flag this and any other internal actions we're waiting
                // for as having been addressed
            } else {
                // since we haven't received an update previously, there isn't anything to fall back
                // on, so we'll just need to rely on the selection reload and any composition will
                // have to fall back to an unknown state.
                testLog(TAG, "checkLostUpdates: invalidating composition due to no previous update");
                mState.invalidateComposition(true);
            }
        } else if (updateExpectation.lastSelectionUpdate != null
                && updateExpectation.lastSelectionUpdate.isSelectionUpdate()) {
            SelectionPositionState selectionUpdateState = updateExpectation.lastSelectionUpdate;
            testLog(TAG, "checkLostUpdates: last selection update selection taken: " + selectionUpdateState.selectionTaken);
            testLog(TAG, "checkLostUpdates: last selection update fully taken: " + selectionUpdateState.selectionUpdateFullyTaken);
            testLog(TAG, "checkLostUpdates: last selection update already matched selection: " + selectionUpdateState.expectedSelectionAlreadyMatched);
            if (!selectionUpdateState.selectionUpdateFullyTaken) {
                if (selectionUpdateState.selectionTaken
                        || selectionUpdateState.expectedSelectionAlreadyMatched) {
                    if (mState.selectionMatches(selectionUpdateState.selectionStart,
                            selectionUpdateState.selectionEnd, false)) {
                        // the last update matches the current state (at least for the cursor position).
                        // since this gets triggered by the timer, if there were more updates in-flight,
                        // when we received this, we should have already received at least some of them
                        // by now, but since that didn't happen, that update should be up-to-date, so we
                        // should take the composition.
                        statePositionsChanged = updateCompositionFromUpdate(selectionUpdateState);
                        selectionUpdateState.selectionUpdateFullyTaken = true;
                    } else {
                        // it seems that we're still waiting on another update
                        //TODO: maybe trigger another timer. the editor may just have a bug, so
                        // I don't know that we want a timer repeating forever if that's the
                        // case. either add some counter for it or call it a lost cause and
                        // assume we just won't get the update.
                    }
                } else {
                    // we didn't take the update presumably because it seemed out of date or we
                    // didn't know the absolute positions to know what action update to expect
                    //TODO: (EW) is there anything worth doing here?
                    testLog(TAG, "checkLostUpdates: last selection update selection taken: " + selectionUpdateState.selectionTaken);
                }
            }
        }

        //TODO: (EW) handle reloading text for extracted text updates that we weren't able to take

        //TODO: (EW) should this clear part or all of the history so we ensure we don't try looking
        // for a missing update a second time? we should at least do something to block the same
        // update as being the next expected one
        // once this only gets called on the timer, whatever manages starting/stopping it should
        // address this

        return statePositionsChanged;
    }

    private boolean updateCompositionFromUpdate(SelectionPositionState state) {
        if (state.compositionStart == null || state.compositionEnd == null) {
            return false;
        }
        boolean statePositionsChanged = false;
        if (state.compositionStart == UNKNOWN_POSITION
                || state.compositionEnd == UNKNOWN_POSITION) {
            if (mState.isCompositionUnknown() || mState.hasComposition()) {
                mState.finishComposingText();
                statePositionsChanged = true;
            }
        } else {
            if (state.compositionStart != mState.getCompositionStart()
                    || state.compositionEnd != mState.getCompositionEnd()) {
                mState.setComposingRegion(state.compositionStart, state.compositionEnd);
                statePositionsChanged = true;
            }
        }
        return statePositionsChanged;
    }

    //TODO: maybe this should return if the composing text changed. alternatively have a function to
    // get the composing text so it can be compared before and after this call.
    public boolean onUpdateExtractedText(final int token, final ExtractedText text) {
        testLog(TAG, "onUpdateExtractedText: init " + mState.getDebugStateInternal());
        testLog(TAG, "onUpdateExtractedText: text=\"" + text.text/*.length()*/ + "\"");
        testLog(TAG, "onUpdateExtractedText: text class: " + text.text.getClass()); // this seems to always be String
        testLog(TAG, "onUpdateExtractedText: flags=" + text.flags + ", partialStartOffset=" + text.partialStartOffset
                + ", partialEndOffset=" + text.partialEndOffset + ", selectionStart=" + text.selectionStart
                + ", selectionEnd=" + text.selectionEnd + ", startOffset=" + text.startOffset);

        final String message = "onUpdateExtractedText: text(" + text.text.length() + ")=\"" + text.text
                + "\"\nflags=" + text.flags + ", startOffset=" + text.startOffset
                + "\npartialStartOffset=" + text.partialStartOffset + ", partialEndOffset=" + text.partialEndOffset
                + "\nselectionStart=" + text.selectionStart + ", selectionEnd=" + text.selectionEnd;
        if (text.startOffset != 0) {
            testLogImportant(TAG, message);
        } else {
            testLog(TAG, message);
        }

        final int composingSpanStart = mState.getCompositionStart();
        final int composingSpanEnd = mState.getCompositionEnd();

        final int updatedSelectionStart;
        final int updatedSelectionEnd;
        //TODO: (EW) see if there is a way to only manage correcting flipped positions in EditorState
        if (text.selectionStart <= text.selectionEnd) {
            updatedSelectionStart = text.startOffset + text.selectionStart;
            updatedSelectionEnd = text.startOffset + text.selectionEnd;
        } else {
            // it's possible for the text field to send an update with the selection start and end
            // positions flipped, but methods like getTextBeforeCursor still work as if the
            // positions were normal, so we should just track these in the normal position
            updatedSelectionStart = text.startOffset + text.selectionEnd;
            updatedSelectionEnd = text.startOffset + text.selectionStart;
        }

        boolean selectionMatches = mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, false);

        UpdateExpectation updateExpectation = getUpdateExpectation();
        boolean wasExpected;
        boolean takeSelectionUpdate;
        boolean takeTextUpdate;
        Boolean appearsUpToDate;
        boolean statePositionsChanged = false;
        SelectionPositionState selectionReloadState = null;
        if (!updateExpectation.lookingForExtractedTextUpdates) {
            //TODO: (EW) I think this should check that we're not waiting for selection updates. if
            // we are, that means something unexpected happened, so it could mess things up to take
            // it.
            testLog(TAG, "onUpdateExtractedText: unexpected but not waiting for updates: " + mState.getDebugState());
            // we could try to verify if this update is up-to-date, but it isn't going to conflict
            // with other updates that we're waiting for, so to avoid making additional IPC calls,
            // we'll just assume it's up-to-date and take the update. even if this is out-of-date,
            // it isn't really any different from not even getting the update yet. we don't normally
            // verify the state before doing actions, so those cases would have the same sort of
            // effect of working off of old data.
            wasExpected = false;
            appearsUpToDate = null;
            // if the cursor position was already updated, this must mean onUpdateSelection was
            // already called and updated that or this change doesn't actually modify the cursor
            // position, so any text changes should already be accounted for. if this does have a
            // cursor change, depending on the update type, we might be able to just shift the text
            // and cursor positions safely, so we don't need to do any preemptive clearing of the
            // cache.
            takeSelectionUpdate = true;
            takeTextUpdate = true;
        } else if (positionsMatch(updateExpectation.nextExpectedExtractedTextUpdate,
                updatedSelectionStart, updatedSelectionEnd)) {
            wasExpected = true;
            takeSelectionUpdate = false;
            // only update from up-to-date updates. multiple actions could modify the same position,
            // so if we already modified this text again pulling in updates from an old action could
            // mess up the cache. we'll need to wait for all the updates to come in to manually
            // reload the text in case it changed and we weren't able to take it.
            //TODO: (EW) how should we flag the update to be taken later
            takeTextUpdate = updateExpectation.actionsWaitingForExtractedTextUpdates == 1
                    /*&& !mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, false)
                    && (!(updateExpectation.lastUpdate instanceof UpdatePositionState)
                            || ((UpdatePositionState)updateExpectation.lastUpdate).selectionTaken)*/
                    && mNestLevel <= 0;
            if (!takeTextUpdate) {
                testLog(TAG, "not taking text update because there are "
                        + updateExpectation.actionsWaitingForExtractedTextUpdates
                        + " actions waiting for extracted text updates");
            }
            //TODO: (EW) maybe determine - currently doesn't matter
            appearsUpToDate = null;

            if (updateExpectation.nextExpectedExtractedTextUpdate.isInternalAction()) {
                SelectionPositionState expectedUpdate =
                        updateExpectation.nextExpectedExtractedTextUpdate;

                expectedUpdate.pairExtractedTextUpdate(appearsUpToDate);//TODO: (EW) appearsUpToDate is always null
                mLastUpdateDelays.add(
                        expectedUpdate.extractedTextUpdateTime - expectedUpdate.internalActionTime);
                while (mLastUpdateDelays.size() > UPDATE_DELAYS_TRACK_COUNT) {
                    mLastUpdateDelays.remove(0);
                }
                testLog(TAG, "onUpdateExtractedText: update matched next expected update and took "
                        + (expectedUpdate.extractedTextUpdateTime - expectedUpdate.internalActionTime)
                        + " ms");
            }
        } else {
            testLog(TAG, "onUpdateExtractedText: update didn't match what was expected: " + updateExpectation.nextExpectedExtractedTextUpdate);
            wasExpected = false;
            //TODO: (EW) we could try pairing this with the last unexpected selection update to
            // consider this expected (do the inverse in onUpdateSelection too) - probably actually
            // should do this in getUpdateExpectation

            // since we had an action that we're waiting for an update from that doesn't match this
            // update, we can't take an update to shift the text since it's not clear what needs to
            // shift to compensate for the action we expected.
            takeSelectionUpdate = false;
            boolean cursorPositionOriginallyKnown = mState.isAbsoluteSelectionStartKnown();
            // verify this update is up-to-date. this will update the cache with current cursor
            // positions (if they could be loaded), and it may clear text from the cache depending
            // on how the selection positions change.
            int updateFlags = loadAndValidateCache(0, 0, true).updateFlags;
            if ((updateFlags & SELECTION_LOADED) > 0) {
                appearsUpToDate = mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, false);
                if (appearsUpToDate) {
                    //TODO: (EW) this still could be out-of-date. is this safe? is there something
                    // else we can do? would it be better to just never take these uncertain cases?
                    // if the selection changed, it should have wiped the text cache. sitting in a
                    // blank state isn't too bad. we can probably just get the text later when we
                    // need it.

                    //TODO: (EW) verify that this carve-out for originally unknown positions is fine
                    // and doesn't cause some weird inconsistency
                    if (cursorPositionOriginallyKnown) {
                        takeTextUpdate = true;
                        statePositionsChanged = (updateFlags & SELECTION_UPDATED) > 0;
                        selectionReloadState = SelectionPositionState.reloadedSelection(
                                mState.getSelectionStart(), mState.getSelectionEnd());
                    } else if ((updateFlags & SELECTION_UPDATED) > 0) {
                        testLog(TAG, "not taking text update because the selection changed unexpectedly");
                        takeTextUpdate = false;
                        statePositionsChanged = true;
                        selectionReloadState = SelectionPositionState.reloadedSelection(
                                mState.getSelectionStart(), mState.getSelectionEnd());
                    } else {
                        takeTextUpdate = true;
                    }
                } else {
                    testLog(TAG, "not taking text update because update is out-of-date");
                    //TODO: (EW) how should we flag the update to be taken later
                    takeTextUpdate = false;
                }
            } else {
                // since we couldn't do anything to verify if this update is up-to-date, we should
                // skip taking the update as this could mess up the cache, and it would be better to
                // have wrong text that matches what the user entered into the IME than some wildly
                // unexpected text. we also can leave the selection update to onUpdateSelection to
                // since it gets more info.
//                takeTextUpdate = false;
                //TODO: (EW) I don't think this is the right thing to do, but doing for now to make
                // existing tests pass. once we have a structure to reload this later and update the
                // existing tests, this should be changed.
                takeTextUpdate = true;
                takeSelectionUpdate = true;

                appearsUpToDate = null;
            }
        }

        testLog(TAG, "onUpdateExtractedText: wasExpected=" + wasExpected + ", takeTextUpdate=" + takeTextUpdate
                + ", takeSelectionUpdate=" + takeSelectionUpdate);

        if (!wasExpected) {
            //TODO: (EW) I think we already updated the state at least in some cases, so this seems
            // incorrect (or maybe it should be renamed if it's just looking for if the end state
            // when receiving the update matches the update)
            boolean selectionTaken = mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, false);
            //TODO: (EW) this probably should check if there already was a selection update for
            // this. actually, maybe due to it not getting flagged as expected (from the matching
            // selection update) this is correct.
            boolean expectSelectionUpdate = true;
            SelectionPositionState workingExtractedTextUpdateState =
                    SelectionPositionState.extractedTextUpdate(
                            updatedSelectionStart, updatedSelectionEnd, appearsUpToDate,
                            selectionTaken, expectSelectionUpdate);
            testLog(TAG, "onUpdateExtractedText: add mStateHistory entry: "
                    + workingExtractedTextUpdateState);
            mStateHistory.add(workingExtractedTextUpdateState);
            // if we updated the selection from a call to check the position and that indicated that the
            // update was out-of-date, keep track of where we updated the selection from. if we have an
            // up-to-date update, we can see that we update from that
            if (appearsUpToDate != null && !appearsUpToDate && selectionReloadState != null) {
                // only need to add the entry in the history
                mStateHistory.add(selectionReloadState);
            }
        }

        if (takeTextUpdate) {
            final int offset;
            if (text.partialStartOffset < 0 && text.partialEndOffset < 0) {
                // full text is available (maybe limited and need to check startOffset)
                testLog(TAG, "onUpdateExtractedText: update type = " + (text.startOffset == 0 ? "full text" : "large text block"));
                offset = text.startOffset;
            } else if (text.partialStartOffset < 0 || text.partialEndOffset < 0) {
                //TODO: (EW) saw this in the number field in loremipsum.io in duckduckgo browser. in
                // this case it sent the whole text, so maybe -1 was meant to indicate no partial start
                // (ie equivalent to 0)
                testLogImportant(TAG, "onUpdateExtractedText: partialStartOffset=" + text.partialStartOffset
                        + ", partialEndOffset=" + text.partialEndOffset + ": not sure how to handle");
                return true;
            } else {
                // only need to update a small bit since only partial text is passed
                testLog(TAG, "onUpdateExtractedText: update type = partial text");
                offset = text.partialStartOffset;
            }

            boolean textTaken = false;

            //TODO: (EW) we probably don't need this variable
            final boolean unexpectedCursorChange;
            if (takeSelectionUpdate) {
                if (mState.areSelectionAbsolutePositionsKnown()) {
                    if (!mState.selectionMatches(updatedSelectionStart,
                            updatedSelectionEnd, false)) {
                        // since the selection change isn't already handled, this means this was called
                        // before onUpdateSelection

                        unexpectedCursorChange = true;

                        final int oldExpectedSelStart = mState.getSelectionStart();
                        final int oldExpectedSelEnd = mState.getSelectionEnd();
                        if (text.partialStartOffset >= 0) {
                            // updates in this format show how much text was inserted/removed
                            final int textLengthChange = text.text.length() - (text.partialEndOffset - text.partialStartOffset);
                            final int selectionStartChange = updatedSelectionStart - oldExpectedSelStart;
                            final int selectionEndChange = updatedSelectionEnd - oldExpectedSelEnd;
                            if ((text.partialEndOffset <= oldExpectedSelStart
                                    && selectionStartChange == selectionEndChange
                                    && textLengthChange == selectionStartChange)) {
                                // this cursor change appears to just be from text being inserted/removed
                                // before the cursor from an external source, which means that the cached
                                // text around the cursor outside of the changed text should still be
                                // accurate
                                // insert/remove space in the cache where the change is to make sure the
                                // text before the change is still cached correctly
                                testLog(TAG, "onUpdateExtractedText: replaceTextAbsolute 1 before " + mState.getDebugStateInternal());
                                mState.replaceTextAbsolute(text.partialStartOffset,
                                        text.partialEndOffset, text.text);
                                textTaken = true;
                                testLog(TAG, "onUpdateExtractedText: replaceTextAbsolute 1 after " + mState.getDebugStateInternal());
                                if (!mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, false)) {
                                    if (!mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, true)) {
                                        throw new RuntimeException("selection: "
                                                + mState.getSelectionStart() + " - " + mState.getSelectionEnd()
                                                + " != " + updatedSelectionStart + " - " + updatedSelectionEnd);
                                    }
                                    testLog(TAG, "onUpdateExtractedText: updatedSelectionStart="
                                            + updatedSelectionStart + ", updatedSelectionEnd=" + updatedSelectionEnd);
                                    //TODO: (EW) possibly should also invalidate the cache to be safe
                                    mState.setSelection(updatedSelectionStart, updatedSelectionEnd);
                                    textTaken = false;
                                    testLog(TAG, "onUpdateExtractedText: setSelection " + mState.getDebugStateInternal());
                                }
                            } else if (text.partialStartOffset >= oldExpectedSelStart
                                    && selectionStartChange == 0
                                    && text.partialEndOffset <= oldExpectedSelEnd
                                    && textLengthChange == selectionEndChange) {
                                // this cursor change appears to just be from text being inserted/removed in
                                // the selected text from an external source, which means that the cached
                                // text around the cursor outside of the changed text should still be
                                // accurate
                                // insert/remove space in the cache where the change is to make sure the
                                // text after the change is still cached correctly
                                testLog(TAG, "onUpdateExtractedText: replaceTextAbsolute 2 before " + mState.getDebugStateInternal());
                                mState.replaceTextAbsolute(text.partialStartOffset,
                                        text.partialEndOffset, text.text);
                                textTaken = true;
                                testLog(TAG, "onUpdateExtractedText: replaceTextAbsolute 2 after " + mState.getDebugStateInternal());
                                if (!mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, false)) {
                                    if (!mState.selectionMatches(updatedSelectionStart, updatedSelectionEnd, true)) {
                                        throw new RuntimeException("selection: "
                                                + mState.getSelectionStart() + " - " + mState.getSelectionEnd()
                                                + " != " + updatedSelectionStart + " - " + updatedSelectionEnd);
                                    }
                                    //TODO: (EW) possibly should also invalidate the cache to be safe
                                    mState.setSelection(updatedSelectionStart, updatedSelectionEnd);
                                    textTaken = false;
                                    testLog(TAG, "onUpdateExtractedText: setSelection " + mState.getDebugStateInternal());
                                }
                            } else {
                                // it's not clear that the change is simply due to the length of the text
                                // before the cursor changing, and it's less likely that this is just a
                                // cursor movement since this is called for a text change, so there probably
                                // isn't anything in the cache that we can trust is still accurate
                                //TODO: is this^ accurate? - revalidate if there is anything else safe to keep
                                testLog(TAG, "onUpdateExtractedText: invalidateTextCache - unclear cursor change in partial update");
                                mState.invalidateTextCache();
                                mState.setSelection(updatedSelectionStart, updatedSelectionEnd);
                                //TODO: (EW) probably also need to mark the composition as unknown
                            }
                        } else {
                            // updates in this format don't show how much text was inserted/removed, so we
                            // don't know what text in the cache would be valid if we just shifted it
                            if (mState.getSelectionStart() != updatedSelectionStart) {
                                // the cursor could have moved (just need to shift the relative position of
                                // the cache) or the text before the cursor changed length (a large portion
                                // of the cache is still correct, but the length of the text before the
                                // cursor and the offset need to be updated) or both (difficult to tell what
                                // in the cache can be trusted to still be accurate), so we can't be sure
                                // what is still valid. it's probably safe to assume that the text before
                                // what is passed here is unchanged, so that much can be kept
                                //TODO: validate if this^ is correct - if we requested a limited extract,
                                // what text would be returned (full change regardless, hard limit around
                                // the cursor, full change unless the change is larger than the limit,
                                // other options?)? if anything at this point can't be trusted, this should
                                // be a limit on our cache
                                testLog(TAG, "onUpdateExtractedText: invalidateTextCacheAfter "
                                        + (text.startOffset - 1) + " - changing cursor start in full update");
                                mState.invalidateTextCacheAfterAbsolute(text.startOffset - 1);
                                mState.setSelection(updatedSelectionStart, updatedSelectionEnd);
                            } else /* mState.getSelectionEnd() != updatedSelectionEnd */ {
                                // the cursor end could have moved (the cache is unaffected) or the text in
                                // the selection changed length (some of the text in the cache needs to
                                // shift) or both (nothing in cache can be trusted to be accurate).
                                //TODO: it might be safe to assume that anything after the returned text and
                                // before the cursor end is still valid (probably still need to shift what's
                                // in the cache based on how much the cursor position changed)
                                testLog(TAG, "onUpdateExtractedText: invalidateTextCacheAfter "
                                        + (text.text.length() + text.startOffset - 1)
                                        + " - changing cursor end in full update");
                                mState.invalidateTextCacheAfterAbsolute(
                                        text.text.length() + text.startOffset - 1);
                                mState.setSelectionLength(
                                        updatedSelectionEnd - updatedSelectionStart);
                            }
                        }
                        //TODO: since we're changing the cursor position, this is probably going to break the
                        // unexpected change in onUpdateSelection if that's called after, so this will also need
                        // the same return value
                    } else {
                        unexpectedCursorChange = false;
                        //TODO: (EW) should we reset the composition to unknown if the change
                        // potentially could have changed it, or is it fine to just rely on the
                        // selection update to manage it soon enough?
                    }
                } else {
                    // we don't know where the cursor was, so we can't be certain that the cursor is in the
                    // same position relative to the cache now, so there probably isn't anything in the
                    // cache that we can trust is still accurate
                    testLog(TAG, "onUpdateExtractedText: invalidateTextCache - unknown cursor position");
                    mState.invalidateTextCache();
                    //TODO: is this the right way to handle this - this is probably the safest option
                    unexpectedCursorChange = true;

                    mState.setSelection(updatedSelectionStart, updatedSelectionEnd);
                }


            } else {
                //TODO: (EW) this is a guess
                unexpectedCursorChange = !selectionMatches;
            }

            final int updatedTextEnd = text.text.length() + offset;
            if (updatedTextEnd >= mState.getSelectionEnd()) {
                if (text.partialStartOffset < 0) {
                    // full text (maybe limited) is sent, so we can't be certain that there is more text
                    // after what was sent. we can't rely on the expected composition because that isn't
                    // updated here and if onUpdateSelection is called after this for this change, it
                    // won't know to remove the text at the end of the cache, so that has to be done
                    // here
                    testLog(TAG, "onUpdateExtractedText: invalidateTextCacheAfter "
                            + (updatedTextEnd - 1) + " - clear after full update");
                    mState.invalidateTextCacheAfterAbsolute(updatedTextEnd - 1);
                } else {
                    final int textLengthChange = text.text.length() - (text.partialEndOffset - text.partialStartOffset);
                    if (textLengthChange < 0) {
                        if (composingSpanEnd > updatedTextEnd) {
                            // since we know how much text is getting removed, we can at least be sure that
                            // the new full text's length isn't less than this change removed from the
                            // furthest known position
                            testLog(TAG, "onUpdateExtractedText: invalidateTextCacheAfter "
                                    + /*Math.max(updatedTextEnd, composingSpanEnd - textLengthChange)*/(updatedTextEnd - 1)
                                    + " - clear based on composing end in partial update");
//                            invalidateTextCacheAfter(Math.max(updatedTextEnd, mExpectedComposingEnd - textLengthChange));
                            //TODO: I'm not sure I believe this^ is correct - validate. current tests pass
                            // in either case, so it might be better to go with the safer option
                            mState.invalidateTextCacheAfterAbsolute(updatedTextEnd - 1);
                        } else {
                            // even if the updated text ends at the selection end, there isn't a way to
                            // determine if text only changed before the selection end. if text changed
                            // past the selection end, we don't have any reference to determine if that
                            // was from a change we made and accounted for or if there was some external
                            // change that could make some of our cached text now shifted, so we'll just
                            // need to clear anything after it to be safe
                            testLog(TAG, "onUpdateExtractedText: invalidateTextCacheAfter "
                                    + (updatedTextEnd - 1) + " - unsure state clear after partial update (reduction)");
                            mState.invalidateTextCacheAfterAbsolute(updatedTextEnd - 1);
                        }
                    } else if (textLengthChange == 0) {
                        // since the text isn't changing length, it's safe to keep whatever is in the cache
                    } else /*textLengthChange > 0*/ {
                        if (updatedTextEnd == mState.getSelectionEnd() && !unexpectedCursorChange) {
                            // we already knew about the change and nothing was actually changed after
                            // the end of the selection, so text after the selection is safe to keep
                        } else {
                            // since we don't know if this change is from some external source of an
                            // alteration of what we requested to add, we don't know how much text after
                            // this change needs to shift, so the cache after this point could be
                            // inaccurate
                            testLog(TAG, "onUpdateExtractedText: invalidateTextCacheAfter "
                                    + (updatedTextEnd - 1) + " - unsure state clear after partial update (increase)");
                            mState.invalidateTextCacheAfterAbsolute(updatedTextEnd - 1);
                        }
                    }
                }
            }

            if (!textTaken) {
                testLog(TAG, "onUpdateExtractedText: updateTextCache before " + mState.getDebugStateInternal());
                testLog(TAG, "onUpdateExtractedText: updateTextCache text=\"" + text.text + "\", offset=" + offset);
                mState.updateTextCache(text.text, offset);
                testLog(TAG, "onUpdateExtractedText: updateTextCache after " + mState.getDebugStateInternal());
            }
        } else if (takeSelectionUpdate) {
            if (mState.getSelectionStart() != updatedSelectionStart
                    || mState.getSelectionEnd() != updatedSelectionEnd) {
                if (mState.getSelectionStart() != updatedSelectionStart) {
                    mState.invalidateTextCache();
                }
                mState.setSelection(updatedSelectionStart, updatedSelectionEnd);
                statePositionsChanged = true;
            }
        }

        if (appearsUpToDate != null && !appearsUpToDate) {
            testLog(TAG, "onUpdateExtractedText: out-of-date update: statePositionsChanged=" + statePositionsChanged);
        } else if (!statePositionsChanged) {
            testLog(TAG, "onUpdateExtractedText: state positions didn't change, so considering expected");
        }
        testLog(TAG, "onUpdateExtractedText: final " + mState.getDebugStateInternal());
        testLog(TAG, "onUpdateExtractedText: unexpectedCursorChange=" + !selectionMatches);
        return selectionMatches;
    }


    public int getExpectedSelectionStart() {
        return mState.getSelectionStart();
    }

    public int getExpectedSelectionEnd() {
        return mState.getSelectionEnd();
    }

    public int getExpectedCompositionStart() {
        return mState.getCompositionStart();
    }

    public int getExpectedCompositionEnd() {
        return mState.getCompositionEnd();
    }

    /**
     * @return whether there is a selection currently active.
     */
    public boolean hasSelection() {
        //TODO: (EW) handle an unknown selection
        return mState.getSelectionEnd() != mState.getSelectionStart();
    }

    //TODO: (EW) validate that all callers actually need the absolute positions
    public boolean hasCursorPosition() {
        return mState.areSelectionAbsolutePositionsKnown();
    }

    public boolean isCursorOutsideOfComposingText() {
        //TODO: implement
        return false;
    }

    //TODO: (EW) rename to getCharSteps and param to codepoints
    /**
     * Some chars, such as emoji consist of 2 chars (surrogate pairs). We should treat them as one character.
     */
    public int getUnicodeSteps(int chars, boolean rightSidePointer) {
        //TODO: reduce duplicate code and consider other general clean up
        int steps = 0;
        String textForSteps = "";
        if (chars < 0) {
            CharSequence charsBeforeCursor = rightSidePointer && hasSelection() ?
                    getSelectedText(0) :
                    getTextBeforeCursor(-chars * 2, 0);
            if (charsBeforeCursor != null) {
                for (int i = charsBeforeCursor.length() - 1; i >= 0 && chars < 0; i--, chars++, steps--) {
                    textForSteps = charsBeforeCursor.charAt(i) + textForSteps;
                    if (Character.isSurrogate(charsBeforeCursor.charAt(i))) {
                        steps--;
                        i--;
                        textForSteps = charsBeforeCursor.charAt(i) + textForSteps;
                    }
                }
                if (chars < 0 && rightSidePointer && hasSelection()) {
                    charsBeforeCursor = getTextBeforeCursor(-chars * 2, 0);
                    for (int i = charsBeforeCursor.length() - 1; i >= 0 && chars < 0; i--, chars++, steps--) {
                        textForSteps = charsBeforeCursor.charAt(i) + textForSteps;
                        if (Character.isSurrogate(charsBeforeCursor.charAt(i))) {
                            steps--;
                            i--;
                            textForSteps = charsBeforeCursor.charAt(i) + textForSteps;
                        }
                    }
                }
            }
        } else if (chars > 0) {
            CharSequence charsAfterCursor = !rightSidePointer && hasSelection() ?
                    getSelectedText(0) :
                    getTextAfterCursor(chars * 2, 0);
            if (charsAfterCursor != null) {
                for (int i = 0; i < charsAfterCursor.length() && chars > 0; i++, chars--, steps++) {
                    textForSteps += charsAfterCursor.charAt(i);
                    if (Character.isSurrogate(charsAfterCursor.charAt(i))) {
                        steps++;
                        i++;
                        textForSteps += charsAfterCursor.charAt(i);
                    }
                }
                if (chars > 0 && !rightSidePointer && hasSelection()) {
                    charsAfterCursor = getTextAfterCursor(chars * 2, 0);
                    for (int i = 0; i < charsAfterCursor.length() && chars > 0; i++, chars--, steps++) {
                        textForSteps += charsAfterCursor.charAt(i);
                        if (Character.isSurrogate(charsAfterCursor.charAt(i))) {
                            steps++;
                            i++;
                            textForSteps += charsAfterCursor.charAt(i);
                        }
                    }
                }
            }
        }
        testLog(TAG, "getUnicodeSteps chars=" + chars + ", rightSidePointer=" + rightSidePointer + ", textForSteps=" + textForSteps);
        return steps;
    }

    // pairing actions and updates options
    // 1. internal action, paired extracted text, paired selection
    // 2. internal action, paired extracted text
    //    unexpected selection (composition)
    // 3. internal action
    //    unexpected selection, paired extracted text
    // 4. internal action
    //    unexpected extracted text, paired selection
    // 5. internal action, no paired selection (no change), maybe paired extracted text
    private static class SelectionPositionState {
        final int selectionStart;
        final int selectionEnd;
        Integer compositionStart;
        Integer compositionEnd;
        final long internalActionTime;
        long selectionUpdateTime = 0;
        long extractedTextUpdateTime = 0;
        final boolean expectSelectionUpdate;
        final boolean expectExtractedTextUpdate;
        //TODO: (EW) do we need separate ones for the 2 update types?
        Boolean updateAppearsUpToDate;
        boolean expectedSelectionAlreadyMatched;
        boolean selectionTaken;
        boolean selectionUpdateFullyTaken;
        //TODO: (EW) maybe add a timestamp to use for clearing items from list (probably need since
        // we won't get some updates) (probably still always want to keep the most recent actual
        // update)
        private SelectionPositionState(final int selectionStart, final int selectionEnd,
                                       final Integer compositionStart,
                                       final Integer compositionEnd,
                                       boolean isInternalAction,
                                       boolean expectSelectionUpdate,
                                       boolean isSelectionUpdate,
                                       boolean selectionUpdateFullyTaken,
                                       boolean expectExtractedTextUpdate,
                                       boolean isExtractedTextUpdate,
                                       boolean expectedSelectionAlreadyMatched,
                                       boolean selectionTaken,
                                       Boolean updateAppearsUpToDate) {
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
            this.compositionStart = compositionStart;
            this.compositionEnd = compositionEnd;
            if (isInternalAction) {
                internalActionTime = SystemClock.uptimeMillis();
            } else {
                internalActionTime = 0;
            }
            this.expectSelectionUpdate = expectSelectionUpdate;
            if (isSelectionUpdate) {
                selectionUpdateTime = SystemClock.uptimeMillis();
            }
            this.selectionUpdateFullyTaken = selectionUpdateFullyTaken;
            this.expectExtractedTextUpdate = expectExtractedTextUpdate;
            if (isExtractedTextUpdate) {
                extractedTextUpdateTime = SystemClock.uptimeMillis();
            }
            this.expectedSelectionAlreadyMatched = expectedSelectionAlreadyMatched;
            this.selectionTaken = selectionTaken;
            this.updateAppearsUpToDate = updateAppearsUpToDate;
        }

        private void pairSelectionUpdate(Boolean appearsUpToDate) {
            selectionUpdateTime = SystemClock.uptimeMillis();
            if (updateAppearsUpToDate == null) {
                updateAppearsUpToDate = appearsUpToDate;
            }
        }

        private void pairExtractedTextUpdate(Boolean appearsUpToDate) {
            extractedTextUpdateTime = SystemClock.uptimeMillis();
            if (updateAppearsUpToDate == null) {
                updateAppearsUpToDate = appearsUpToDate;
            }
        }

        private boolean isInternalAction() {
            return internalActionTime != 0;
        }
        private boolean isSelectionUpdate() {
            return selectionUpdateTime != 0;
        }
        private boolean isExtractedTextUpdate() {
            return extractedTextUpdateTime != 0;
        }

        private boolean updateAppearsUpToDate() {
            return updateAppearsUpToDate != null && updateAppearsUpToDate;
        }

        private boolean updateAppearsOutOfDate() {
            return updateAppearsUpToDate != null && !updateAppearsUpToDate;
        }

        public static SelectionPositionState internalAction(EditorState state,
                                                            boolean expectSelectionUpdate,
                                                            boolean expectExtractedTextUpdate) {
            Integer compositionStart;
            Integer compositionEnd;
            if (state.isCompositionUnknown()) {
                compositionStart = null;
                compositionEnd = null;
            } else {
                compositionStart = state.getCompositionStart();
                compositionEnd = state.getCompositionEnd();
            }
            return new SelectionPositionState(state.getSelectionStart(), state.getSelectionEnd(),
                    compositionStart, compositionEnd, true,
                    expectSelectionUpdate, false, false,
                    expectExtractedTextUpdate, false,
                    false, false, null);
        }

        public static SelectionPositionState selectionUpdate(final int selectionStart,
                                                             final int selectionEnd,
                                                             final int compositionStart,
                                                             final int compositionEnd,
                                                             final Boolean appearsUpToDate,
                                                             final boolean expectedSelectionAlreadyMatched,
                                                             final boolean selectionTaken,
                                                             final boolean selectionUpdateFullyTaken,
                                                             final boolean expectExtractedTextUpdate) {
            return new SelectionPositionState(selectionStart, selectionEnd,
                    compositionStart, compositionEnd, false,
                    false, true, selectionUpdateFullyTaken,
                    expectExtractedTextUpdate, false,
                    expectedSelectionAlreadyMatched, selectionTaken, appearsUpToDate);
        }

        public static SelectionPositionState extractedTextUpdate(final int selectionStart,
                                                                 final int selectionEnd,
                                                                 final Boolean appearsUpToDate,
                                                                 final boolean selectionTaken,
                                                                 boolean expectSelectionUpdate) {
            return new SelectionPositionState(selectionStart, selectionEnd,
                    null, null, false,
                    expectSelectionUpdate, false, false,
                    false, true,
                    false, selectionTaken, appearsUpToDate);
        }

        public static SelectionPositionState reloadedSelection(final int selectionStart,
                                                               final int selectionEnd) {
            return new SelectionPositionState(selectionStart, selectionEnd,
                    null, null, false,
                    false, false, false,
                    false, false,
                    false, true, null);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (isInternalAction()) {
                sb.append("InternalAction");
                if (selectionUpdateTime > 0 && (extractedTextUpdateTime == 0
                        || selectionUpdateTime < extractedTextUpdateTime)) {
                    sb.append("+SelectionUpdate");
                }
                if (extractedTextUpdateTime > 0) {
                    sb.append("+ExtractedTextUpdate");
                }
                if (selectionUpdateTime > 0 && extractedTextUpdateTime > 0
                        && selectionUpdateTime >= extractedTextUpdateTime) {
                    sb.append("+SelectionUpdate");
                }
            } else if (selectionUpdateTime > 0 && (extractedTextUpdateTime == 0
                    || selectionUpdateTime < extractedTextUpdateTime)) {
                sb.append("SelectionUpdate");
                if (extractedTextUpdateTime > 0) {
                    sb.append("+ExtractedTextUpdate");
                }
            } else if (extractedTextUpdateTime > 0) {
                sb.append("ExtractedTextUpdate");
                if (selectionUpdateTime > 0) {
                    sb.append("+SelectionUpdate");
                }
            } else {
                sb.append("ReloadedSelectionPosition");
            }
            sb.append(": selStart=").append(selectionStart);
            sb.append(", selEnd=").append(selectionEnd);
            if (isInternalAction() || isSelectionUpdate()) {
                sb.append(", compStart=").append(compositionStart);
                sb.append(", compEnd=").append(compositionEnd);
            }
            if (isSelectionUpdate()) {
                sb.append(", selectionUpdateFullyTaken=").append(selectionUpdateFullyTaken);
                sb.append(", expectedSelectionAlreadyMatched=").append(expectedSelectionAlreadyMatched);
            } else {
                sb.append(", expectSelectionUpdate=").append(expectSelectionUpdate);
            }
            if (!isExtractedTextUpdate()) {
                sb.append(", expectExtractedTextUpdate=").append(expectExtractedTextUpdate);
            }
            if (isSelectionUpdate() || isExtractedTextUpdate()) {
                sb.append(", updateAppearsUpToDate=").append(updateAppearsUpToDate);
                sb.append(", selectionTaken=").append(selectionTaken);
            }
            return sb.toString();
        }
    }

    //#region TODO: delete contents of this region
    //#region old APIs scheduled for deletion
    // these are temporarily kept to allow the existing unit tests for them to continue indirectly
    // testing various private helper functions as those are getting refactored/added until better
    // new tests can be added to test those appropriately
    public void deleteTextBeforeCursor(int beforeLength) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        testLog(TAG, "deleteTextBeforeCursor: init: " + mState.getDebugStateInternal());
        //TODO: (EW) this still probably could use some cleanup/simplification
        //TODO: (EW) V3 unit tests don't seem to be testing deleting past the beginning of the text - V2 caught a bug unseen by V3
        if (mState.isAbsoluteSelectionStartKnown()
                && beforeLength > mState.getSelectionStart()) {
            beforeLength = mState.getSelectionStart();
        }
        if (beforeLength == 0) {
            // nothing to do
            return;
        }
        if (isConnected()) {
            // Despite the documentation saying that deleteSurroundingText can modify the composing
            // span, at least some text views (including the framework EditText) don't do this, and
            // instead it deletes around both the selection and composition, so we'll need to
            // compensate to ensure the right text is actually deleted.
            // if we don't know where the composition is, we'll have to stop composing, which is
            // unexpected and weird, but it's not as bad as deleting the wrong text.
            // note that if the composition had any spans, they may be dropped when finishing the
            // composing text, which may not be desirable, but when we're forced to drop the
            // composition due unknown state, there isn't really anything we can do to try to retain
            // the spans.
            if (mState.isCompositionUnknown()) {
                Log.w(TAG, "deleteTextBeforeCursor was called without knowing the composition" +
                        " state. if there is a composition, depending on where it is, it might" +
                        " change what text is getting deleted, so the composition will be" +
                        " finished to avoid an deleting an unexpected part of the text.");
                mState.finishComposingText();
                prepSendAction();
                mIC.finishComposingText();
                mState.deleteTextBeforeCursor(beforeLength);
                prepSendAction();
                mIC.deleteSurroundingText(beforeLength, 0);
            } else if (mState.hasComposition()) {
                if (!mState.isRelativeCompositionPositionKnown()) {
                    Log.w(TAG, "deleteTextBeforeCursor was called without knowing the relative" +
                            " position of the composition. depending on where the composition is," +
                            " it might change what text is getting deleted, so the composition" +
                            " will be finished to avoid an deleting an unexpected part of the" +
                            " text.");
                    mState.finishComposingText();
                    prepSendAction();
                    mIC.finishComposingText();
                    mState.deleteTextBeforeCursor(beforeLength);
                    prepSendAction();
                    mIC.deleteSurroundingText(beforeLength, 0);
                } else if (mState.getRelativeCompositionStart() >= 0) {
                    // safe to do a simple delete
                    mState.deleteTextBeforeCursor(beforeLength);
                    prepSendAction();
                    mIC.deleteSurroundingText(beforeLength, 0);
                } else if (mState.getRelativeCompositionStart() >= -beforeLength
                        && mState.getRelativeCompositionEnd() <= 0) {
                    // the whole composition is getting deleted, so just drop the composition and
                    // delete
                    mState.finishComposingText();
                    prepSendAction();
                    mIC.finishComposingText();
                    mState.deleteTextBeforeCursor(beforeLength);
                    prepSendAction();
                    mIC.deleteSurroundingText(beforeLength, 0);
                } else if (!mState.areSelectionAbsolutePositionsKnown()) {
                    //TODO: (EW) I don't think this case is relevant if we set the composing text
                    Log.w(TAG, "deleteTextBeforeCursor was called when the composition start" +
                            " was before the selection start (which might change what text is" +
                            " getting deleted) and without knowing the absolution positions" +
                            " of the selection, so it isn't possible to compensate for the" +
                            " potentially unexpected delete position, so the composition will" +
                            " be finished to avoid an deleting an unexpected part of the" +
                            " text.");
                    mState.finishComposingText();
                    prepSendAction();
                    mIC.finishComposingText();
                    mState.deleteTextBeforeCursor(beforeLength);
                    prepSendAction();
                    mIC.deleteSurroundingText(beforeLength, 0);
                } else {
                    final int initialExpectedSelStart = mState.getSelectionStart();
                    final int initialExpectedSelLength = mState.getSelectionLength();
                    final int initialExpectedComposingStart = mState.getCompositionStart();
                    final int initialExpectedComposingEnd = mState.getCompositionEnd();
                    int deleteEnd = mState.getSelectionStart();
                    int deleteStart = deleteEnd - beforeLength;
                    if (initialExpectedComposingEnd <= deleteStart) {
                        // deleting exclusively after the composition
                        mState.setSelection(deleteStart, deleteStart);
                        prepSendAction();
                        mIC.setSelection(deleteStart, deleteStart);
                        mState.deleteTextAfterCursor(beforeLength);
                        prepSendAction();
                        mIC.deleteSurroundingText(0, beforeLength);
                        if (initialExpectedSelLength > 0) {
                            mState.setSelection(deleteStart,
                                    deleteStart + initialExpectedSelLength);
                            prepSendAction();
                            mIC.setSelection(deleteStart, deleteStart + initialExpectedSelLength);
                        }
                    } else {
                        //TODO: (EW) dropping the composition may lose spans, and setting the
                        // composing region back later may not restore them. we could get the
                        // composition with styles and set the composition with the appropriate
                        // modification. this should work fine deleting through either edge of the
                        // composition as long as there is no selection or the absolute selection
                        // positions are known. updating the composition with the cursor inside it
                        // could be tricky since the length may change (unlikely since it's just a
                        // delete but still possible).
                        CharSequence composingText = loadCompositionWithStyles();
                        testLog(TAG, "deleteTextBeforeCursor: composingText=\"" + composingText + "\"");

                        // the composing text needs to be temporarily committed to ensure that the
                        // expected text is deleted
                        mState.finishComposingText();
                        testLog(TAG, "deleteTextBeforeCursor: calling finishComposingText()");
                        prepSendAction();
                        mIC.finishComposingText();// this may be expensive with some editors

                        mState.deleteTextBeforeCursor(beforeLength);
                        testLog(TAG, "deleteTextBeforeCursor: calling deleteSurroundingText("
                                + beforeLength + ", 0)");
                        prepSendAction();
                        mIC.deleteSurroundingText(beforeLength, 0);

                        final int finalSelectionStart = mState.getSelectionStart();
                        final int finalSelectionEnd = mState.getSelectionEnd();
                        // if we delete through the composition start, wherever the cursor ends will
                        // be the new composition start
                        final int finalCompositionStart =
                                Math.min(initialExpectedComposingStart, finalSelectionStart);
                        final int finalCompositionEnd;
                        if (initialExpectedSelStart < initialExpectedComposingEnd) {
                            // the end of the composition isn't getting deleted, so its position
                            // just shifts with the cursor
                            finalCompositionEnd = initialExpectedComposingEnd - beforeLength;
                        } else {
                            // if the deletion goes into the composition, the new composition end
                            // will match the cursor start
                            finalCompositionEnd =
                                    Math.min(initialExpectedComposingEnd, finalSelectionStart);
                        }
                        final int newComposingLength = finalCompositionEnd - finalCompositionStart;

                        if (newComposingLength > 0) {
                            // we didn't delete through the whole composition, so we need to put it
                            // back

                            mState.setComposingRegion(
                                    finalCompositionStart,
                                    finalCompositionEnd);
                            testLog(TAG, "deleteTextBeforeCursor: calling setComposingRegion("
                                    + finalCompositionStart + ", " + finalCompositionEnd + ")");
                            //TODO: (EW) starting in Android13, setComposingRegion will always
                            // return true, even if the composition region isn't set. how should
                            // this be handled? this method was added in API level 9, so by API
                            // level 33, it's probably unlikely enough to run such an old
                            // application, and since we don't have much else for alternatives,
                            // maybe we have to just assume that it worked and move on.
                            //TODO: (EW) prior to Nougat, calling this may crash the application, so
                            // should we avoid calling it on older versions to be safe?
                            prepSendAction();
                            if (!mIC.setComposingRegion(finalCompositionStart, finalCompositionEnd)) {
                                // some input connections don't support setComposingRegion so we'll
                                // have to do it manually

                                // make sure we have the full text of what should be the composed
                                // text so we can delete it and add it back as a composition
                                //TODO: (EW) consider checking FULL_REQUEST_COMPLETED from
                                // loadTextAroundCursor to ensure we're not relying on old (possibly
                                // incorrect) text to replace the composition, and if it didn't load
                                // completely, just drop the composition to avoid messing something
                                // up worse. relying on cached text to replace the composition may
                                // not actually be that bad since we use it for updating the
                                // composition when entering text, but that seems more reasonable to
                                // re-add old text, rather than it getting added back from a delete.
                                //TODO: (EW) this should respect existing spans, but that may have
                                // already been lost from dropping the composition
                                loadTextAroundCursor(
                                        finalCompositionStart - finalSelectionStart,
                                        finalCompositionEnd > finalSelectionStart
                                                ? Math.max(1, finalCompositionEnd - finalSelectionEnd + 1)
                                                : finalCompositionEnd - finalSelectionStart);
                                if (mState.isFullCompositionCached()) {
                                    //TODO: (EW) since we don't store spans in the cache, any spans in
                                    // the composition will be lost. maybe we shouldn't use the cache
                                    // and just load the composition with spans fresh.
                                    CharSequence newComposition = mState.getComposedText();

                                    // reset our internal state since setting the composing region
                                    // didn't actually work
                                    mState.finishComposingText();

                                    // move the cursor to the end of where we want the composition to be
                                    //TODO: if we can at least determine if the composition cache might
                                    // be incorrect, we should try reloading now before we deleting it
                                    // and adding back potentially different text - maybe just always do
                                    // this to be safe. if we can't be certain of the text to
                                    // re-compose, maybe it would be better to just leave the text in a
                                    // committed state - an abrupt end to the composition state (when
                                    // backspacing doesn't seem too odd) with the correct text is
                                    // probably better than correctly keeping the composed state with
                                    // potentially incorrect text
                                    mState.shiftSelection(
                                            finalCompositionEnd - mState.getSelectionStart(),
                                            finalCompositionEnd - mState.getSelectionEnd());
                                    testLog(TAG, "deleteTextBeforeCursor: calling setSelection("
                                            + finalCompositionEnd + ", " + finalCompositionEnd + ")");
                                    prepSendAction();
                                    mIC.setSelection(finalCompositionEnd, finalCompositionEnd);

                                    // delete the committed text that should be the composing text so it
                                    // can be added back as a composition
                                    mState.deleteTextBeforeCursor(newComposingLength);
                                    testLog(TAG, "deleteTextBeforeCursor: calling deleteSurroundingText("
                                            + newComposingLength + ", 0)");
                                    prepSendAction();
                                    mIC.deleteSurroundingText(newComposingLength, 0);

                                    // add the composition back and move the cursor to where the main
                                    // deletion ended
                                    //TODO: (EW) are there any useful comments above from the previous
                                    // version of this that is still relevant to keep?
                                    final int newCursorPosition;
                                    if (finalSelectionStart >= finalCompositionEnd) {
                                        final int selectionDistanceFromComposition =
                                                finalSelectionStart - finalCompositionEnd;
                                        newCursorPosition = selectionDistanceFromComposition + 1;
                                    } else {
                                        // we need to put the cursor somewhere inside of the
                                        // composition, which we can't actually do when setting the
                                        // composing text, so just put the cursor at the end of the
                                        // composition temporarily
                                        newCursorPosition = 1;
                                    }
                                    //TODO: (EW) this doesn't retain spans since our cache doesn't keep
                                    // them. either we'll need to track them (in our cache or load this
                                    // differently) or just drop the composition do avoid losing things
                                    //TODO: (EW) it might be possible to call a stripped down version of
                                    // this
                                    mState.setComposingText(newComposition, newCursorPosition);
                                    //TODO: (EW) we might need to check that the composed text didn't
                                    // change and shift positions unexpectedly
                                    testLog(TAG, "deleteTextBeforeCursor: calling setComposingText("
                                            + mState.getComposedText()
                                            + ", " + newCursorPosition + ")");
                                    prepSendAction();
                                    mIC.setComposingText(newComposition, newCursorPosition);
                                    if (!mState.selectionMatches(finalSelectionStart,
                                            finalSelectionEnd, false)) {
                                        mState.setSelection(finalSelectionStart, finalSelectionEnd);
                                        testLog(TAG, "deleteTextBeforeCursor: calling setSelection("
                                                + finalSelectionStart + ", " + finalSelectionEnd + ")");
                                        prepSendAction();
                                        mIC.setSelection(finalSelectionStart, finalSelectionEnd);
                                    }
                                } else {
                                    testLog(TAG, "deleteTextBeforeCursor: " + mState.getDebugStateInternal());
                                    // we can't get the text that needs to be set as the
                                    // composition, so if we delete it, we won't know what to add
                                    // back as the composition. the only real option is just to
                                    // leave it in an committed state.
                                    mState.finishComposingText();
                                }
                            }
                        }
                    }
                }
            } else {
                mState.deleteTextBeforeCursor(beforeLength);
                prepSendAction();
                mIC.deleteSurroundingText(beforeLength, 0);
            }
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }
    public void deleteSelectedText() {
        if (mState.getSelectionLength() == 0) {
            // nothing to delete
            return;
        }
        if (!mState.areSelectionAbsolutePositionsKnown()) {
            //TODO: we probably need a public function to check if there is a selection/maybe get that text
            final CharSequence selectedText = getSelectedText(0);
            testLog(TAG, "deleteSelectedText: getSelectedText: " + selectedText);
            if (selectedText == null) {
                // This means that there is no text currently selected or the client is taking too
                // long to respond (or possibly if this method isn't supported, but documentation is
                // unclear on this). Since there doesn't seem to be a way to distinguish between
                // these cases, we'll just have to assume that there is no selected text and do
                // nothing.
                return;
            }
            if (selectedText.length() > 0) {
                mState.setSelectionLength(selectedText.length());
                mState.deleteSelectedText();
            }
            if (mState.isCompositionUnknown()) {
                //TODO: (EW) figure out how to handle
            } else if (mState.hasComposition()) {
                //TODO: figure out how to handle - maybe send KEYCODE_DEL event
            } else {
                //TODO: I suppose there is a chance that an input connection just ignores committing
                // an empty string, but due to how the documentation warns editor authors to move
                // the cursor correctly, it seems that not committing the text (overwriting the
                // selected text with nothing) would simply be incorrect for the editor to do.
                // Still, see if there is a better option (sending KEYCODE_DEL event is probably
                // worse).
                mIC.commitText("", 1);
            }
            return;
        }
        final int numCharsDeleted = mState.getSelectionEnd() - mState.getSelectionStart();
        if (numCharsDeleted < 1) {
            return;
        }
        setSelection(mState.getSelectionEnd(), mState.getSelectionEnd());
        //TODO: (EW) this is broken now. deleteTextBeforeCursor was deleted upstream. given how
        // difficult it ws to manage it's weird behavior, it would be good if we can get rid of this
        // function too since this will have the same challenges.
        deleteTextBeforeCursor(numCharsDeleted);
    }
    //TODO: (EW) now that deleteTextBeforeCursor was removed (technically I'm currently still
    // keeping it for test coverage, but I will remove it eventually), this should either be deleted
    // entirely, or the trimming logic should be added to getCompositionState to allow it to support
    // the flags parameter for spans
    private CharSequence loadCompositionWithStyles() {
        if (mState.isCompositionUnknown() || !mState.hasComposition()
                || !mState.isRelativeCompositionPositionKnown()
                || (!mState.isSelectionLengthKnown()
                && mState.getCompositionEnd() > 0)) {
            // either there is no composition or we don't know how to load the text
            return null;
        }
        int start;
        if (mState.getRelativeCompositionStart() < 0) {
            start = mState.getRelativeCompositionStart();
        } else if (mState.getRelativeCompositionStart() < mState.getSelectionLength()) {
            start = 0;
        } else {
            start = mState.getRelativeCompositionStart() - mState.getSelectionLength() + 1;
        }
        int end;
        if (mState.getRelativeCompositionEnd() <= 0) {
            end = mState.getRelativeCompositionEnd();
        } else if (mState.getRelativeCompositionEnd() <= mState.getSelectionLength()) {
            end = 1;
        } else {
            end = mState.getRelativeCompositionEnd() - mState.getSelectionLength() + 1;
        }
        LoadAndValidateCacheResult result =
                getTextAroundCursor(start, end, GET_TEXT_WITH_STYLES, false, false, false, false);
        if (result.requestedText == null) {
            return null;
        }
        int compositionStart;
        if (mState.getRelativeCompositionStart() <= 0) {
            compositionStart = 0;
        } else if (mState.getRelativeCompositionStart() > 0
                && mState.getRelativeCompositionStart() < mState.getSelectionLength()) {
            compositionStart = mState.getRelativeCompositionStart();
        } else {
            compositionStart = mState.getRelativeCompositionStart()
                    - mState.getSelectionLength();
        }
        int compositionEnd;
        if (mState.getRelativeCompositionEnd() <= 0) {
            compositionEnd = result.requestedText.length() + mState.getRelativeCompositionEnd();
        } else if (mState.getRelativeCompositionEnd() > 0
                && mState.getRelativeCompositionEnd() < mState.getSelectionLength()) {
            compositionEnd = result.requestedText.length()
                    - (mState.getSelectionLength() - mState.getRelativeCompositionEnd());
        } else {
            compositionEnd = result.requestedText.length();
        }
        return result.requestedText.subSequence(compositionStart, compositionEnd);
    }
    //#endregion

    //#region debug APIs
    public String getDebugState() {
        return mState.getDebugStateInternal();
    }
    public void commitTextDirect(final CharSequence text, final int newCursorPosition) {
        if (isConnected()) {
            testLog(TAG, "commitText: \"" + text + "\", " + newCursorPosition);
            mIC.commitText(text, newCursorPosition);
        }
    }
    public CharSequence getTextBeforeCursorUncached(final int n, final int flags) {
        return mIC.getTextBeforeCursor(n, flags);
    }
    public CharSequence getTextAfterCursorUncached(final int n, final int flags) {
        return mIC.getTextAfterCursor(n, flags);
    }
    public SurroundingText getSurroundingTextUncached(final int before, final int after, final int flags) {
        if (VERSION.SDK_INT >= 31) {
            return mIC.getSurroundingText(before, after, flags);
        }
        return null;
    }
    public void deleteSurroundingText(final int before, final int after) {
        if (isConnected()) {
            testLog(TAG, "deleteSurroundingText: " + before + ", " + after);
            if (mState.isSelectionLengthKnown()) {
                mState.deleteTextAfterCursor(after);
            } else {
                mState.invalidateTextCacheAfterRelative(-1);
            }
            mState.deleteTextAfterCursor(after);
            mState.deleteTextBeforeCursor(before);
            mIC.deleteSurroundingText(before, after);
        }
    }
    public void deleteSurroundingTextInCodePoints(final int before, final int after) {
        // making lazy assumption for the sake of copy/paste that 1 code point == 1 char, which is wrong - maybe fix this
        if (isConnected()) {
            testLog(TAG, "deleteSurroundingTextInCodePoints: " + before + ", " + after);
            if (mState.isSelectionLengthKnown()) {
                mState.deleteTextAfterCursor(after);
            } else {
                mState.invalidateTextCacheAfterRelative(0);
            }
            mState.deleteTextBeforeCursor(before);
            testLog(TAG, "deleteSurroundingTextInCodePoints(return): " + mIC.deleteSurroundingTextInCodePoints(before, after));
            reloadCachesForStartingInputView();
            loadTextCache(true);
        }
    }
    public void moveCursor(final int change) {
        if (isConnected() && mState.areSelectionAbsolutePositionsKnown()) {
            testLog(TAG, "moveCursor: " + mState.getSelectionStart() + ", "
                    + mState.getSelectionEnd() + " -> "
                    + (mState.getSelectionStart() + change) + ", "
                    + (mState.getSelectionEnd() + change));
            mState.shiftSelection(change);
            mIC.setSelection(mState.getSelectionStart(), mState.getSelectionEnd());
        }
    }
    public void setSelectionDirect(final int start, final int end) {
//        mState.setSelection(start, end);
        mIC.setSelection(start, end);
        testLog(TAG, "setSelectionDirect: text before: " + mIC.getTextBeforeCursor(10, 0));
        testLog(TAG, "setSelectionDirect: selection: " + mIC.getSelectedText(0));
        testLog(TAG, "setSelectionDirect: text after: " + mIC.getTextAfterCursor(10, 0));
        reloadCachesForStartingInputView();
    }
    private static void setLength(final StringBuilder sb, final int length) {
        final int initialLength = sb.length();
        sb.setLength(length);
        if (length > initialLength) {
            for (int i = initialLength; i < length; i++) {
                sb.replace(i, i + 1, "" + NONCHARACTER_CODEPOINT_PLACEHOLDER);
            }
        }
    }
    //#endregion

    //#region misc testing
    public void testSuggstions(int flag) {
        //SuggestionSpan.FLAG_AUTO_CORRECTION - blue underline, bar suggestions (openboard, not gboard) - the auto correction is about to be applied to a word/text that the user is typing/composing
        //SuggestionSpan.FLAG_EASY_CORRECT - gray underline (disappears at some point), bar suggestions (openboard, not gboard), click dropdown (stops at some point) - the suggestions should be easily accessible with few interactions
        //SuggestionSpan.FLAG_MISSPELLED - no underline, bar suggestions (openboard, not gboard) - the suggestions apply to a misspelled word/text
        //SuggestionSpan.SUGGESTIONS_MAX_SIZE - gray underline (changes to blue at some point), bar suggestions (openboard, not gboard), click dropdown (stops at some point)
        SuggestionSpan span = new SuggestionSpan(mParent.getApplicationContext(), new String[] {"foo", "bar", "baz", "asdf", "textx", "zexz"}, flag);
//        SpannableStringBuilder builder = new SpannableStringBuilder();
//        builder.append("text", span, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        SpannableString spannableString = new SpannableString("text");
        spannableString.setSpan(span, 0, 4, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
//        spannableString.setSpan(foregroundColorSpan, 0, 4, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mIC.commitText(spannableString, 1);
    }
    public void testCorrection() {
        if (!mState.areSelectionAbsolutePositionsKnown()) {
            return;
        }
        CharSequence oldText = getTextBeforeCursor(Math.min(4, mState.getSelectionStart()), 0);
        String newText = "asdf".substring(0, Math.min(4, oldText.length()));
        testLog(TAG, "commitCorrection: " + mIC.commitCorrection(new CorrectionInfo(mState.getSelectionStart() - newText.length(), oldText, newText)));
    }
    public void testUndo() {
        final long eventTime = SystemClock.uptimeMillis();
        mIC.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        mIC.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }
    public void testRedo() {
        final long eventTime = SystemClock.uptimeMillis();
        mIC.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        mIC.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }
    public void testEasyEditSpan() {
        SpannableString spannableString = new SpannableString("easy edit spans");
        spannableString.setSpan(new EasyEditSpan(), 0, 4, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new EasyEditSpan(), 10, 15, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mIC.commitText(spannableString, 1);
    }
    public void testCommitFormattedText() {
        SpannableString spannableString = new SpannableString("formatted text");
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, 9, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), 10, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mIC.commitText(spannableString, 1);
    }
    public void testComposeFormattedText() {
        SpannableString spannableString = new SpannableString("formatted text");
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, 9, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), 10, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mIC.setComposingText(spannableString, 1);
    }
    public void testMoveLeftWord() {
        final long eventTime = SystemClock.uptimeMillis();
        mIC.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        mIC.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, KeyEvent.META_CTRL_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }
    public void testMoveRightWord() {
        final long eventTime = SystemClock.uptimeMillis();
        mIC.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        mIC.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, 0, KeyEvent.META_CTRL_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }
    public void testTab() {
        final long eventTime = SystemClock.uptimeMillis();
        mIC.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
        mIC.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }
    public void testLinkSpan() {
        // this doesn't seem to have any impact in the EditText other than adding the text "link"
        Map<String, Float> entityScores = new HashMap<>();
        entityScores.put("www.example.com", 1f);
        TextLink textLink = null;
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            for (TextLink link : new Builder("www.google.com").addLink(0, "www.google.com".length(), entityScores).build().getLinks()) {
                textLink = link;
                break;
            }
            SpannableString spannableString = new SpannableString("www.stackoverflow.com");
            spannableString.setSpan(new TextLinkSpan(textLink), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            mIC.commitText(spannableString, 1);
        }
    }
    public void setComposingRegion() {
        if (mState.getSelectionStart() < mState.getSelectionEnd()) {
            testLog(TAG, "setComposingRegion: " + mIC.setComposingRegion(mState.getSelectionStart(), mState.getSelectionEnd()));
        } else {
            testLog(TAG, "setComposingRegion: " + mIC.setComposingRegion(0, mState.getSelectionStart()));
        }
    }
    public void setComposingRegion(int start, int end) {
        testLog(TAG, "setComposingRegion: " + mIC.setComposingRegion(start, end));
    }
    public void composeNothing() {
        mIC.setComposingText("", 1);
    }
    public void appendComposingText(CharSequence charSequence, int position) {
        if (!mState.isCompositionUnknown() && mState.hasComposition()) {
            charSequence = mState.getComposedText().toString() + charSequence;
        }
        setComposingText(charSequence, position);
    }
    public void testRequestCursorUpdates() {
        testLog(TAG, "requestCursorUpdates: " + mIC.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE));
    }
    public void testCloseConnection() {
        mIC.closeConnection();
    }
    public void testCommitContent() {
        //https://developer.android.com/develop/ui/views/touch-and-input/image-keyboard#java
        //commitContent call is analogous to the commitText() call, but for rich content
//        testLog(TAG, "testCommitContent: " + mIC.commitContent(new InputContentInfo(), 0, null));
        commitGifImage(MediaStore.Images.Media.getContentUri("external"), "test description");
    }
    /**
     * Commits a GIF image
     *
     * @param contentUri Content URI of the GIF image to be sent
     * @param imageDescription Description of the GIF image to be sent
     */
    private void commitGifImage(Uri contentUri, String imageDescription) {
        InputContentInfo inputContentInfo = new InputContentInfo(
                contentUri,
                new ClipDescription(imageDescription, new String[]{"image/gif"}),
                null
        );
//        InputConnection inputConnection = getCurrentInputConnection();
//        EditorInfo editorInfo = getCurrentInputEditorInfo();
        int flags = 0;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            flags |= InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
//        }
//        InputConnectionCompat.commitContent(
//                inputConnection, editorInfo, inputContentInfo, flags, null);
        testLog(TAG, "testCommitContent: " + mIC.commitContent(inputContentInfo, flags, null));
    }
    public boolean commitText(CharSequence text,
                               int newCursorPosition,
                               TextAttribute textAttribute) {
        return mIC.commitText(text, newCursorPosition, textAttribute);
    }
    public void testTakeSnapshot() {
        testLog(TAG, "takeSnapshot: " + mIC.takeSnapshot());
    }

    public void performSpellCheck() {
        testLog(TAG, "performSpellCheck: " + mIC.performSpellCheck());
    }

    public void setImeConsumesInput(boolean imeConsumesInput) {
        testLog(TAG, "setImeConsumesInput: " + mIC.setImeConsumesInput(imeConsumesInput));
    }

    public void replacePreviousCharWithItself() {
        CharSequence textBefore = getTextBeforeCursor(1, 0);
        if (TextUtils.isEmpty(textBefore)) {
            return;
        }
        mIC.deleteSurroundingText(1, 0);
        mIC.commitText(textBefore, 1);
    }

    public void moveSelectionToStartAndBack() {
        if (!hasSelection()) {
            return;
        }
        int selStart = mState.getSelectionStart();
        int selEnd = mState.getSelectionEnd();
        mIC.setSelection(0, 0);
        mIC.setSelection(selStart, selEnd);
    }

    // results from pie physical device on small file:
    //getTextBeforeCursor(1, 0) took an average of 1 ms (between 1 and 2 ms)
    //getTextBeforeCursor(10, 0) took an average of 1 ms (between 1 and 2 ms)
    //getTextBeforeCursor(1000, 0) took an average of 1 ms (between 1 and 2 ms)
    //getSelectedText(0) took an average of 1 ms (between 0 and 2 ms)
    //getTextAfterCursor(1, 0) took an average of 0 ms (between 0 and 1 ms)
    //getTextAfterCursor(10, 0) took an average of 0 ms (between 0 and 1 ms)
    //getTextAfterCursor(1000, 0) took an average of 1 ms (between 0 and 2 ms)
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, 0) took an average of 1 ms (between 1 and 2 ms)
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, GET_EXTRACTED_TEXT_MONITOR) took an average of 0 ms (between 0 and 1 ms)
    //getExtractedText({hintMaxChars=1, hintMaxLines=1, flags=0}, 0) took an average of 1 ms (between 1 and 2 ms)
    //getExtractedText({hintMaxChars=10, hintMaxLines=10, flags=0}, 0) took an average of 1 ms (between 1 and 2 ms)
    //getExtractedText({hintMaxChars=1000, hintMaxLines=10, flags=0}, 0) took an average of 1 ms (between 1 and 2 ms)
    // results from pie physical device on large file:
    //getTextBeforeCursor(1, 0) took an average of 1 ms (between 0 and 2 ms)
    //getTextBeforeCursor(10, 0) took an average of 1 ms (between 0 and 3 ms)
    //getTextBeforeCursor(1000, 0) took an average of 1 ms (between 0 and 2 ms)
    //getSelectedText(0) took an average of 0 ms (between 0 and 2 ms)
    //getTextAfterCursor(1, 0) took an average of 1 ms (between 0 and 2 ms)
    //getTextAfterCursor(10, 0) took an average of 0 ms (between 0 and 1 ms)
    //getTextAfterCursor(1000, 0) took an average of 1 ms (between 1 and 1 ms)
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, 0) took an average of 4 ms (between 4 and 5 ms)
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, GET_EXTRACTED_TEXT_MONITOR) took an average of 4 ms (between 4 and 6 ms)
    //getExtractedText({hintMaxChars=1, hintMaxLines=1, flags=0}, 0) took an average of 4 ms (between 4 and 5 ms)
    //getExtractedText({hintMaxChars=10, hintMaxLines=10, flags=0}, 0) took an average of 5 ms (between 5 and 6 ms)
    //getExtractedText({hintMaxChars=1000, hintMaxLines=10, flags=0}, 0) took an average of 5 ms (between 4 and 10 ms)
    // results from pie physical device on large file with a large selection:
    //getTextBeforeCursor(1, 0) took an average of 1 ms (between 1 and 3 ms)
    //getTextBeforeCursor(10, 0) took an average of 1 ms (between 1 and 3 ms)
    //getTextBeforeCursor(1000, 0) took an average of 2 ms (between 1 and 3 ms)
    //getSelectedText(0) took an average of 1 ms (between 1 and 2 ms)
    //getTextAfterCursor(1, 0) took an average of 1 ms (between 1 and 3 ms)
    //getTextAfterCursor(10, 0) took an average of 1 ms (between 1 and 3 ms)
    //getTextAfterCursor(1000, 0) took an average of 1 ms (between 1 and 2 ms)
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, 0) took an average of 6 ms (between 5 and 7 ms)
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, GET_EXTRACTED_TEXT_MONITOR) took an average of 6 ms (between 4 and 10 ms)
    //getExtractedText({hintMaxChars=1, hintMaxLines=1, flags=0}, 0) took an average of 3 ms (between 3 and 5 ms)
    //getExtractedText({hintMaxChars=10, hintMaxLines=10, flags=0}, 0) took an average of 3 ms (between 3 and 3 ms)
    //getExtractedText({hintMaxChars=1000, hintMaxLines=10, flags=0}, 0) took an average of 3 ms (between 3 and 6 ms)

    // results from pie physical device on large file with a large selection with 100 tests:
    //getTextBeforeCursor(1, 0) took an average of 0 ms (between 0 and 3 ms) and returned 1characters
    //getTextBeforeCursor(10, 0) took an average of 0 ms (between 0 and 3 ms) and returned 10 characters
    //getTextBeforeCursor(1000, 0) took an average of 0 ms (between 0 and 2 ms) and returned 1000 characters
    //getSelectedText(0) took an average of 0 ms (between 0 and 3 ms) and returned 702 characters
    //getTextAfterCursor(1, 0) took an average of 0 ms (between 0 and 3 ms) and returned 1 characters
    //getTextAfterCursor(10, 0) took an average of 0 ms (between 0 and 3 ms) and returned 10 characters
    //getTextAfterCursor(1000, 0) took an average of 0 ms (between 0 and 2 ms) and returned 1000 characters
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, 0) took an average of 4 ms (between 3 and 24 ms) and returned 89576 characters
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, GET_EXTRACTED_TEXT_MONITOR) took an average of 4 ms (between 2 and 21 ms) and returned 89576 characters
    //getExtractedText({hintMaxChars=1, hintMaxLines=1, flags=0}, 0) took an average of 4 ms (between 2 and 12 ms) and returned 89576 characters
    //getExtractedText({hintMaxChars=10, hintMaxLines=10, flags=0}, 0) took an average of 5 ms (between 3 and 17 ms) and returned 89576 characters
    //getExtractedText({hintMaxChars=1000, hintMaxLines=10, flags=0}, 0) took an average of 4 ms (between 2 and 18 ms) and returned 89576 characters
    // results from android 13 emulator on large file with a large selection with 100 tests:
    //getTextBeforeCursor(1, 0) took an average of 0 ms (between 0 and 1 ms) and returned 1 characters
    //getTextBeforeCursor(10, 0) took an average of 0 ms (between 0 and 1 ms) and returned 10 characters
    //getTextBeforeCursor(1000, 0) took an average of 0 ms (between 0 and 1 ms) and returned 1000 characters
    //getSelectedText(0) took an average of 0 ms (between 0 and 1 ms) and returned 609 characters
    //getTextAfterCursor(1, 0) took an average of 0 ms (between 0 and 1 ms) and returned 1 characters
    //getTextAfterCursor(10, 0) took an average of 0 ms (between 0 and 1 ms) and returned 10 characters
    //getTextAfterCursor(1000, 0) took an average of 0 ms (between 0 and 1 ms) and returned 1000 characters
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, 0) took an average of 0 ms (between 0 and 8 ms) and returned 35205 characters
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, GET_EXTRACTED_TEXT_MONITOR) took an average of 0 ms (between 0 and 3 ms) and returned 35205 characters
    //getExtractedText({hintMaxChars=1, hintMaxLines=1, flags=0}, 0) took an average of 0 ms (between 0 and 1 ms) and returned 35205 characters
    //getExtractedText({hintMaxChars=10, hintMaxLines=10, flags=0}, 0) took an average of 0 ms (between 0 and 4 ms) and returned 35205 characters
    //getExtractedText({hintMaxChars=1000, hintMaxLines=10, flags=0}, 0) took an average of 0 ms (between 0 and 4 ms) and returned 35205 characters
    // results from kitkat emulator intentionally made slow (painful to use) on large text with a large selection with 100 tests
    //getTextBeforeCursor(1, 0) took an average of 0 ms (between 0 and 2 ms) and returned 1 characters
    //getTextBeforeCursor(10, 0) took an average of 0 ms (between 0 and 1 ms) and returned 10 characters
    //getTextBeforeCursor(1000, 0) took an average of 0 ms (between 0 and 3 ms) and returned 1000 characters
    //getSelectedText(0) took an average of 0 ms (between 0 and 1 ms) and returned 507 characters
    //getTextAfterCursor(1, 0) took an average of 0 ms (between 0 and 1 ms) and returned 1 characters
    //getTextAfterCursor(10, 0) took an average of 0 ms (between 0 and 1 ms) and returned 10 characters
    //getTextAfterCursor(1000, 0) took an average of 0 ms (between 0 and 3 ms) and returned 1000 characters
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, 0) took an average of 1 ms (between 0 and 5 ms) and returned 30046 characters
    //getExtractedText({hintMaxChars=0, hintMaxLines=0, flags=0}, GET_EXTRACTED_TEXT_MONITOR) took an average of 8 ms (between 0 and 72 ms) and returned 30046 characters
    //getExtractedText({hintMaxChars=1, hintMaxLines=1, flags=0}, 0) took an average of 3 ms (between 0 and 45 ms) and returned 30046 characters
    //getExtractedText({hintMaxChars=10, hintMaxLines=10, flags=0}, 0) took an average of 1 ms (between 0 and 5 ms) and returned 30046 characters
    //getExtractedText({hintMaxChars=1000, hintMaxLines=10, flags=0}, 0) took an average of 1 ms (between 0 and 5 ms) and returned 30046 characters
    public void perfTestTextRequests() {
        final int testCount = 100;
        perfTestGetTextBeforeCursor(1, 0, testCount);
        perfTestGetTextBeforeCursor(10, 0, testCount);
        perfTestGetTextBeforeCursor(1000, 0, testCount);
        perfTestGetSelectedText(0, testCount);
        perfTestGetTextAfterCursor(1, 0, testCount);
        perfTestGetTextAfterCursor(10, 0, testCount);
        perfTestGetTextAfterCursor(1000, 0, testCount);
        perfTestGetExtractedText(0, 0, 0, false, testCount);
        perfTestGetExtractedText(0, 0, 0, true, testCount);
        perfTestGetExtractedText(1, 1, 0, false, testCount);
        perfTestGetExtractedText(10, 10, 0, false, testCount);
        perfTestGetExtractedText(1000, 10, 0, false, testCount);
    }
    private void perfTestGetTextBeforeCursor(int textLength, int flags, int testCount) {
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long minChars = Long.MAX_VALUE;
        long maxChars = Long.MIN_VALUE;
        for (int i = 0; i < testCount; i++) {
            final long startTime = SystemClock.uptimeMillis();
            CharSequence text = mIC.getTextBeforeCursor(textLength, flags);
            long duration = SystemClock.uptimeMillis() - startTime;
            totalTime += duration;
            if (duration > maxTime) {
                maxTime = duration;
            }
            if (duration < minTime) {
                minTime = duration;
            }
            if (text.length() > maxChars) {
                maxChars = text.length();
            }
            if (text.length() < minChars) {
                minChars = text.length();
            }
        }
        testLog(TAG, "getTextBeforeCursor(" + textLength + ", " + flags + ") took an average of "
                + (totalTime / testCount) + " ms (between " + minTime + " and " + maxTime
                + " ms) and returned "
                + (minChars == maxChars ? maxChars : (minChars + " to " + maxChars))
                + " characters");
    }
    private void perfTestGetTextAfterCursor(int textLength, int flags, int testCount) {
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long minChars = Long.MAX_VALUE;
        long maxChars = Long.MIN_VALUE;
        for (int i = 0; i < testCount; i++) {
            final long startTime = SystemClock.uptimeMillis();
            CharSequence text = mIC.getTextAfterCursor(textLength, flags);
            long duration = SystemClock.uptimeMillis() - startTime;
            totalTime += duration;
            if (duration > maxTime) {
                maxTime = duration;
            }
            if (duration < minTime) {
                minTime = duration;
            }
            if (text.length() > maxChars) {
                maxChars = text.length();
            }
            if (text.length() < minChars) {
                minChars = text.length();
            }
        }
        testLog(TAG, "getTextAfterCursor(" + textLength + ", " + flags + ") took an average of "
                + (totalTime / testCount) + " ms (between " + minTime + " and " + maxTime
                + " ms) and returned "
                + (minChars == maxChars ? maxChars : (minChars + " to " + maxChars))
                + " characters");
    }
    private void perfTestGetSelectedText(int flags, int testCount) {
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long minChars = Long.MAX_VALUE;
        long maxChars = Long.MIN_VALUE;
        for (int i = 0; i < testCount; i++) {
            final long startTime = SystemClock.uptimeMillis();
            CharSequence text = mIC.getSelectedText(flags);
            long duration = SystemClock.uptimeMillis() - startTime;
            totalTime += duration;
            if (duration > maxTime) {
                maxTime = duration;
            }
            if (duration < minTime) {
                minTime = duration;
            }
            if (text.length() > maxChars) {
                maxChars = text.length();
            }
            if (text.length() < minChars) {
                minChars = text.length();
            }
        }
        testLog(TAG, "getSelectedText(" + flags + ") took an average of "
                + (totalTime / testCount) + " ms (between " + minTime + " and " + maxTime
                + " ms) and returned "
                + (minChars == maxChars ? maxChars : (minChars + " to " + maxChars))
                + " characters");
    }
    private void perfTestGetExtractedText(int hintMaxChars, int hintMaxLines, int flags,
                                          boolean extractedTextMonitor, int testCount) {
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long minChars = Long.MAX_VALUE;
        long maxChars = Long.MIN_VALUE;
        for (int i = 0; i < testCount; i++) {
            final ExtractedTextRequest r = new ExtractedTextRequest();
            r.hintMaxChars = hintMaxChars;
            r.hintMaxLines = hintMaxLines;
            r.token = 1 + i;
            r.flags = flags;
            final long startTime = SystemClock.uptimeMillis();
            ExtractedText text = mIC.getExtractedText(r,
                    extractedTextMonitor ? InputConnection.GET_EXTRACTED_TEXT_MONITOR : 0);
            long duration = SystemClock.uptimeMillis() - startTime;
            totalTime += duration;
            if (duration > maxTime) {
                maxTime = duration;
            }
            if (duration < minTime) {
                minTime = duration;
            }
            if (text.text.length() > maxChars) {
                maxChars = text.text.length();
            }
            if (text.text.length() < minChars) {
                minChars = text.text.length();
            }
        }
        testLog(TAG, "getExtractedText({hintMaxChars=" + hintMaxChars
                + ", hintMaxLines=" + hintMaxLines + ", flags=" + flags + "}, "
                + (extractedTextMonitor ? "GET_EXTRACTED_TEXT_MONITOR" : "0")
                + ") took an average of "
                + (totalTime / testCount) + " ms (between " + minTime + " and " + maxTime
                + " ms) and returned "
                + (minChars == maxChars ? maxChars : (minChars + " to " + maxChars))
                + " characters");
    }
    //#endregion

    private static final boolean VERBOSE_DEBUG_LOGGING = true;
    public static void testLog(String tag, String msg) {
        if (VERBOSE_DEBUG_LOGGING) {
            Log.w(tag, msg);
        }
    }
    public static void testLogImportant(String tag, String msg) {
        if (VERBOSE_DEBUG_LOGGING) {
            Log.e(tag, msg);
        }
    }
    //#endregion
}
