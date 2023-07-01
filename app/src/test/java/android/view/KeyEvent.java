package android.view;

public class KeyEvent {

    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int KEYCODE_0 = 7;
    public static final int KEYCODE_1 = 8;
    public static final int KEYCODE_2 = 9;
    public static final int KEYCODE_3 = 10;
    public static final int KEYCODE_4 = 11;
    public static final int KEYCODE_5 = 12;
    public static final int KEYCODE_6 = 13;
    public static final int KEYCODE_7 = 14;
    public static final int KEYCODE_8 = 15;
    public static final int KEYCODE_9 = 16;
    public static final int KEYCODE_DPAD_LEFT = 21;
    public static final int KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_A = 29;
    public static final int KEYCODE_B = 30;
    public static final int KEYCODE_C = 31;
    public static final int KEYCODE_D = 32;
    public static final int KEYCODE_E = 33;
    public static final int KEYCODE_F = 34;
    public static final int KEYCODE_G = 35;
    public static final int KEYCODE_H = 36;
    public static final int KEYCODE_I = 37;
    public static final int KEYCODE_J = 38;
    public static final int KEYCODE_K = 39;
    public static final int KEYCODE_L = 40;
    public static final int KEYCODE_M = 41;
    public static final int KEYCODE_N = 42;
    public static final int KEYCODE_O = 43;
    public static final int KEYCODE_P = 44;
    public static final int KEYCODE_Q = 45;
    public static final int KEYCODE_R = 46;
    public static final int KEYCODE_S = 47;
    public static final int KEYCODE_T = 48;
    public static final int KEYCODE_U = 49;
    public static final int KEYCODE_V = 50;
    public static final int KEYCODE_W = 51;
    public static final int KEYCODE_X = 52;
    public static final int KEYCODE_Y = 53;
    public static final int KEYCODE_Z = 54;
    public static final int KEYCODE_DEL = 67;

    private final int mAction;
    private final int mCode;

    public KeyEvent(int action, int code) {
        mAction = action;
        mCode = code;
    }

    public final int getAction() {
        return mAction;
    }

    public final int getKeyCode() {
        return mCode;
    }

    public int getUnicodeChar() {
        if (mCode >= KEYCODE_0 && mCode <= KEYCODE_9) {
            return mCode - KEYCODE_0 + '0';
        }
        if (mCode >= KEYCODE_A && mCode <= KEYCODE_Z) {
            return mCode - KEYCODE_A + 'a';
        }
        return 0;
    }
}
