package de.minekonst.mariokartwiiai.shared.utils;

public final class TimeUtils {

    /**
     * Lets the current thread sleep
     *
     * @param ms Time to sleep in milliseconds
     * @param nanos Nanosecounds to add to the time (0 - 999.999)
     */
    public static void sleep(long ms, int nanos) {
        if (ms <= 0 && nanos <= 0) {
            return;
        }
        
        try {
            Thread.sleep(ms, nanos);
        }
        catch (InterruptedException ex) {

        }
    }

    /**
     * Lets the current thread sleep
     *
     * @param ms Time to sleep in milliseconds
     */
    public static void sleep(long ms) {
        sleep(ms, 0);
    }
    
    /**
     * Lets the current thread sleep
     *
     * @param ms Time to sleep in milliseconds
     */
    public static void sleep(int ms) {
        sleep(ms, 0);
    }
    
    public static double timed(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return (System.nanoTime() - start) / 1_000_000.0;
    }
    
    public static void debugLogTime(Runnable r) {
        System.out.printf("Took %.2f ms\n", timed(r));
    }
    
    public static void debugLogTime(String name, Runnable r) {
        System.out.printf("Took %.2f ms to %s\n", timed(r), name);
    }
}
