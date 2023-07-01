package android.text;

public class Selection {
    /**
     * Return the offset of the selection anchor or cursor, or -1 if
     * there is no selection or cursor.
     */
    public static final int getSelectionStart(CharSequence text) {
        if (text instanceof Spanned) {
            return ((Spanned) text).getSpanStart(SELECTION_START);
        }
        return -1;
    }

    /**
     * Return the offset of the selection edge or cursor, or -1 if
     * there is no selection or cursor.
     */
    public static final int getSelectionEnd(CharSequence text) {
        if (text instanceof Spanned) {
            return ((Spanned) text).getSpanStart(SELECTION_END);
        }
        return -1;
    }

    private static final class START implements NoCopySpan { }
    private static final class END implements NoCopySpan { }

    public static final Object SELECTION_START = new START();
    public static final Object SELECTION_END = new END();
}
