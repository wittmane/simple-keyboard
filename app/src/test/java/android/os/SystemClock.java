package android.os;

public class SystemClock {
    private static final long sInitialTime = System.currentTimeMillis();
    public static long uptimeMillis() {
        return System.currentTimeMillis() - sInitialTime;
    }
}
