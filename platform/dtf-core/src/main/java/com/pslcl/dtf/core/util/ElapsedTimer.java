package com.pslcl.dtf.core.util;


@SuppressWarnings("javadoc")
public class ElapsedTimer
{
    protected static final long startTime;
    private static final long[] nanos;
    private static final long[] elapsed;
    private static final Object[] contexts;
    private static final String[] labels;
    private static final int[] hits;

    static
    {
        startTime = System.currentTimeMillis();
        nanos = new long[100];
        elapsed = new long[100];
        contexts = new Object[100];
        labels = new String[100];
        hits = new int[100];
    }
    
    public static String getNormalizedString(int value)
    {
        return getNormalizedString((long) value);
    }

    public static String getNormalizedString(long value)
    {
        StringBuilder rvalue = new StringBuilder();
        long nextScale = 1024;
        long currentScale = 1;
        for(int i=0; i < 5; i++)
        {
            long check = value / nextScale;
            if(check == 0 || i == 4)
            {
                boolean fractional = true;
                rvalue.append(""+value / currentScale);
                switch(i)
                {
                    case 0:
                        fractional = false;
                        break;
                    case 1:
                        rvalue.append("K");
                        break;
                    case 2:
                        rvalue.append("M");
                        break;
                    case 3:
                        rvalue.append("TeraBytes");
                        break;
                    case 4:
                        rvalue.append("PetaBytes");
                        break;
                    default:
                        break;
                }
                if(fractional)
                {
                    rvalue.append(".");
                    rvalue.append(""+value % currentScale);
                }
                return rvalue.toString();
            }
            nextScale *= 1024L;
            currentScale *= 1024L;
        }
        return rvalue.toString();
    }

    /**
     * Returns the time in milliseconds that the class Environment was loaded by
     * a class loader.
     * @return start time
     */
    public static long getStartTime()
    {
        return startTime;
    }

    /**
     * starts the generic millisecond timer for the given index.
     *
     * @param index Index of timer to control, 0-99 valid.
     *
     * @see #stopTimer
     * @see #resetTimer
     * @see #elapsedTime
     */
    public static void startTimer( int index )
    {
        synchronized(nanos)
        {
            nanos[index] = System.nanoTime();
        }
    }

    /**
     * stops the generic millisecond timer for the given index.
     *
     * @param index Index of timer to control, 0-99 valid.
     *
     * @see #startTimer
     * @see #resetTimer
     * @see #elapsedTime
     */
    public static void stopTimer( int index )
    {
        synchronized(nanos)
        {
            if(nanos[index] == 0)
                return;    // not running
            // simply add the current delta into the elapsed counter
            long t2 = System.nanoTime();
            elapsed[index] += t2 - nanos[index];
            nanos[index] = 0;
        }
    }

    /**
     * Zeros the elapsed timer, and captures the current system milliseconds for
     * the given index.
     *
     * @param index Index of timer to control, 0-9 valid.
     *
     * @see #startTimer
     * @see #stopTimer
     * @see #elapsedTime
     */
    public static void resetTimer( int index )
    {
        synchronized(nanos)
        {
            nanos[index] = System.nanoTime();
            elapsed[index] = 0;
        }
    }

    /**
     * Stops all timers and zeros all the elapsed timers.
     * <p>
     * User may want to call this before starting into a 
     * sequence that uses several timers which will be only
     * require a startTimer at the various points in the sequence.
     * 
     * @see #startTimers
     * @see #resetTimer
     * @see #startTimer
     * @see #stopTimer
     * @see #elapsedTime
     */
    public static void resetAllElapsedTimers()
    {
        synchronized(nanos)
        {
            for(int i=0; i < elapsed.length; i++)
                elapsed[i] = 0;
        }
    }

    public static void resetElapsedTimers(int index, int number)
    {
        synchronized(nanos)
        {
            for(int i=0; i < number; i++)
                elapsed[index + i] = 0;
        }
    }
    
    /**
     * Start n number of timers.
     * <p>
     * User may want to call this before starting into a 
     * sequence that uses several timers which will then only
     * require a startTimer at the various points in the sequence.
     * <p>
     * Calling <code>resetAllElapsedTimers</code> and then calling this method
     * can be useful for sequences where you want to capture elapsed times of 
     * multiple legs by only doing a <code>stopTimer</code> on the various indices
     * at the end of legs.
     * 
     * @param number Number of timers, starting at index 0, to start.
     * @see #resetTimer
     * @see #startTimer
     * @see #stopTimer
     * @see #elapsedTime
     */
    public static void startTimers(int number)
    {
        synchronized(nanos)
        {
            for(int i=0; i < number; i++)
                startTimer(i);
        }
    }

    /**
     * Returns the current elapsed time for the given index.
     *
     * @param index Index of timer to control, 0-99 valid.
     * @return the long
     * @see #startTimer
     * @see #stopTimer
     * @see #resetTimer
     */
    public static long elapsedTime( int index )
    {
        synchronized(nanos)
        {
            // simply add the current delta into the elapsed counter
            long t2 = System.nanoTime();
            elapsed[index] += (t2 - nanos[index]);
            nanos[index] = t2;
            return elapsed[index];
        }
    }

    public static long getTime(int index)
    {
        synchronized(nanos)
        {
            return elapsed[index];
        }
    }

    public static double elapsedMs(int index)
    {
        synchronized(nanos)
        {
            double ns = elapsedTime(index);
            return ns/1000000.0;
        }
    }
        

    /**
     * Associates a context object to a given timer.
     * 
     * @param index Index of timer to associate the context with, 0-99 valid.
     * @param context The context to associate with the indexed timer.
     * 
     * @see #getContext
     */
    public static void setContext(int index, Object context)
    {
        synchronized(nanos)
        {
            contexts[index] = context;
        }
    }

    /**
     * Associates a context object to a number of timers.
     * <p>
     * Sometimes you will need a context later in the sequence, but you are not sure
     * what timer that might be by the time you get there.  Just assign the context to 
     * several timers in this case so it will be available in the timer when you need it later.
     * 
     * @param index Start index of timers to associate the context with, 0-99 minus number valid.
     * @param number Number of timers, starting at index, to associate the context with.
     * @param context The context to associate with the timers.
     * 
     * @see #getContext
     */
    public static void setContext(int index, int number, Object context)
    {
        synchronized(nanos)
        {
            for(int i = 0; i < number; i++)
                contexts[index + i] = context;
        }
    }

    /**
     * Gets the associated context for the given timer.
     * 
     * @param index Index of timer to return the associated context for, 0-99 valid.
     * @return the context
     * 
     * @see #setContext
     */
    public static Object getContext(int index)
    {
        synchronized(nanos)
        {
            return contexts[index];
        }
    }

    /**
     * Associates a label to a given timer.
     * 
     * @param index Index of timer to associate the label with, 0-99 valid.
     * @param label The label to associate with the indexed timer.
     * 
     * @see #getLabel
     */
    public static void setLabel(int index, String label)
    {
        synchronized(nanos)
        {
            labels[index] = label;
        }
    }

    /**
     * Gets the associated label for the given timer.
     * 
     * @param index Index of timer to return the associated label for, 0-99 valid.
     * @return the label
     * 
     * @see #setLabel
     */
    public static String getLabel(int index)
    {
        synchronized(nanos)
        {
            return labels[index];
        }
    }
    
    public static void clearAllHitCounts()
    {
        synchronized(nanos)
        {
            for(int i=0; i < hits.length; i++)
                hits[i] = 0;
        }
    }
    
    public static void clearHitCounts(int index, int number)
    {
        synchronized(nanos)
        {
            for(int i=0; i < number; i++)
                hits[index + i] = 0;
        }
    }
    
    public static void clearHitCount(int index)
    {
        synchronized(nanos)
        {
            hits[index] = 0;
        }
    }
    
    public static int getHitCount(int index)
    {
        synchronized(nanos)
        {
            return hits[index];
        }
    }
    
    public static int incHitCount(int index)
    {
        synchronized(nanos)
        {
            return ++hits[index];
        }
    }
    
    public static String buildMessageForElapsed(String message, int index, int number)
    {
        synchronized(nanos)
        {
            StringBuilder msg = new StringBuilder(message == null ? "" : message);
            if(message != null)
                msg.append("\n");
            for(int i=0; i < number; i++)
            {
                if(labels[index+i] == null)
                    continue;
                msg.append("timer").append(index+i).append(" ").append(labels[index + i]).append(" ").append(scaleNanoSeconds(elapsed[index + i])).append("\n");
            }
            return msg.toString();
        }
    }
    public static long nanoToMs(long ns)
    {
        return ns / 1000000;
    }

    public static long msToNano(long ms)
    {
        return 1000000 * ms;
    }

    public static String scaleMilliSeconds(long value)
    {
        return scaleNanoSeconds(msToNano(value));
    }
    
    public static String scaleNanoSeconds(long value)
    {
/*
                about max                 about min
ns:                 999                            1
mico:         999222                         1222
ms:           999111222                      1111222
sec:     59000111222                   1000111222
min:   3633000111222                  60000111222
hr:      86400000000000                3600000111222
*/
        if(value < 0)
            return "0";

        if(value < 1000)
        {// nano seconds
            return "" + value + "ns";
        }
        if(value < 1000000)
        {// micro seconds
            return "" + (value / 1000.0) + "micos";
        }

        if(value < 1000000000)
        {// milli seconds
            return "" + (value / 1000000.0) + "ms";
        }

        if(value < 60000000000L)
        {// seconds
            return "" + (value / 1000000000.0) + "sec";
        }

        if(value < 3600000000000L)
        {// minutes
            return "" + (value / 60000000000.0) + "min";
        }

        if(value < 86400000000000L)
        {// hours
            return "" + (value / 3600000000000.0) + "hr";
        }
        return "" + (value / 3600000000000.0) + "hr";
    }

    public static String getElapsedTime(String message, int index)
    {
        double d = elapsedTime(index);
        StringBuilder etime = new StringBuilder(message).append(" : ").append((d/1000000)).append(" ms");
        return etime.toString();
    }

    public static String getCurrentInfo(String message, int index, int total)
    {
        long et = elapsedTime(index);
        long avt = et / total;
        double ps = (1.0 / et);
        ps *= 10000000000000L;
        StringBuilder msg = new StringBuilder(message).
        append(" total: ").append(total).
        append(" time: ").append(scaleNanoSeconds(et)).
        append(" avg: ").append(scaleNanoSeconds(avt)).
        append(" persec: ").append(ps);
        return msg.toString();
    }
}
