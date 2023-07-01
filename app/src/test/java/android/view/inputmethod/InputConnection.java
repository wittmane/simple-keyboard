package android.view.inputmethod;

import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;

public interface InputConnection {
    static final int GET_TEXT_WITH_STYLES = 0x0001;
    public static final int GET_EXTRACTED_TEXT_MONITOR = 1;
    public CharSequence getTextBeforeCursor(int i, int i1);
    public CharSequence getTextAfterCursor(int i, int i1);
    public CharSequence getSelectedText(int i);
    public int getCursorCapsMode(int i);
    public ExtractedText getExtractedText(ExtractedTextRequest extractedTextRequest, int i);
    public boolean deleteSurroundingText(int i, int i1);
    public boolean deleteSurroundingTextInCodePoints(int i, int i1);
    public boolean setComposingText(CharSequence charSequence, int i);
    public boolean setComposingRegion(int i, int i1);
    public boolean finishComposingText();
    public boolean commitText(CharSequence charSequence, int i);
    public boolean commitCompletion(CompletionInfo completionInfo);
    public boolean commitCorrection(CorrectionInfo correctionInfo);
    public boolean setSelection(int i, int i1);
    public boolean performEditorAction(int i);
    public boolean performContextMenuAction(int i);
    public boolean beginBatchEdit();
    public boolean endBatchEdit();
    public boolean sendKeyEvent(KeyEvent keyEvent);
    public boolean clearMetaKeyStates(int i);
    public boolean reportFullscreenMode(boolean b);
    public boolean performPrivateCommand(String s, Bundle bundle);
    public boolean requestCursorUpdates(int i);
    public Handler getHandler();
    public void closeConnection();
    public boolean commitContent(InputContentInfo inputContentInfo, int i, Bundle bundle);
}
