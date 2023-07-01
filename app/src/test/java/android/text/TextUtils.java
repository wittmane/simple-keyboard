package android.text;

public class TextUtils {
    public static void getChars(CharSequence s, int start, int end,
                                char[] dest, int destoff) {
        Class<? extends CharSequence> c = s.getClass();

        if (c == String.class) {
            ((String) s).getChars(start, end, dest, destoff);
        } else if (c == StringBuffer.class) {
            ((StringBuffer) s).getChars(start, end, dest, destoff);
        } else if (c == StringBuilder.class) {
            ((StringBuilder) s).getChars(start, end, dest, destoff);
        } else if (s instanceof GetChars) {
            ((GetChars) s).getChars(start, end, dest, destoff);
        } else {
            for (int i = start; i < end; i++)
                dest[destoff++] = s.charAt(i);
        }
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
