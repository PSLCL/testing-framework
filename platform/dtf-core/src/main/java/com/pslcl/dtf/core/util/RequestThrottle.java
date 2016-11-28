package com.pslcl.dtf.core.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Requests Per Second Throttling class
 */
public class RequestThrottle
{

    private final AtomicLong firstAccess;
    private final AtomicLong lastRealtime;
    private final AtomicLong lastRealInterval;
    private final AtomicInteger realtimeCount;
    private final AtomicInteger sendtimeCount;
    private final AtomicLong lastAccess;
    private final int maxPerInterval;
    private final int customScale;


    /**
     * PerSecond Request Throttle constructor
     * @param maxPerSecond the maximum allowed requests per the declared interval
     */
    public RequestThrottle(int maxPerSecond)
    {
        this(maxPerSecond, IntervalType.Second, 1);
    }

    /**
     * Partial Request Throttle constructor
     * @param maxPerInterval the maximum allowed requests per the declared interval
     * @param type the interval type.
     */
    public RequestThrottle(int maxPerInterval, IntervalType type)
    {
        this(maxPerInterval, type, 1);
    }


    /**
     * Full Request Throttle constructor
     * <p>Use the customScale to obtain the likes of 1 per 5 minutes (IntervalType.Minute, customScale = 5).
     * @param maxPerInterval the maximum allowed requests per interval
     * @param type the interval type.
     * @param customScale value to be multiplied against the IntervalTypes scale.
     */
    public RequestThrottle(int maxPerInterval, IntervalType type, int customScale)
    {
        firstAccess = new AtomicLong(System.currentTimeMillis());
        realtimeCount = new AtomicInteger(0);
        sendtimeCount = new AtomicInteger(0);
        lastRealInterval = new AtomicLong(0);
        this.maxPerInterval = maxPerInterval - 1;
        lastAccess = new AtomicLong(0);
        this.customScale = customScale * type.scale;
        lastRealtime = new AtomicLong(firstAccess.get() - customScale * 2);
    }

    /**
     * @return return the last timestamp when waitAsNeeded was called
     */
    public long getLastAccess()
    {
        return lastAccess.get();
    }


    private final AtomicInteger hits = new AtomicInteger();
    /**
     * wait for next interval if we are already at max.
     */
    public void waitAsNeeded()
    {
        TabToLevel format = new TabToLevel("waitAsNeeded:");
        format.ttl("hits: " + hits.incrementAndGet());
        long t1 = System.currentTimeMillis();
        lastAccess.set(t1);

        // realtime calculations, always based on System.currentTimeMillis
        long elapsedRealtime = t1 - lastRealtime.get();
        lastRealtime.set(t1);
        long realInterval = t1 / customScale;
        long lastInterval = lastRealInterval.get();
        lastRealInterval.set(realInterval);
        format.ttl("elapsedRealtime: ", ElapsedTimer.getNormalizedString(elapsedRealtime));
        format.ttl("realInterval: ", realInterval);
        format.ttl("lastRealInterval: ", lastInterval);
        realtimeCount.incrementAndGet();
        format.ttl("realtimeCount: ", realtimeCount.get());
        if (lastInterval == 0 || realInterval - lastInterval >= 2)
        {
            sendtimeCount.set(0);
            realtimeCount.set(0);
            format.ttl("real time up two intervals, cleared counters and return");
            //            LoggerFactory.getLogger(getClass()).info(format.toString());
            return;
        }
        format.ttl("another hit within the same realtime interval");
        format.inc();
        if (realInterval - lastInterval > 0)
        {
            format.ttl("spanned a realtime interval");
            format.inc();
            if (realtimeCount.get() < maxPerInterval)
            {
                format.ttl("adjusting sendtime count and returning");
                format.inc();
                format.ttl("realtimeCount was: ", realtimeCount.get());
                format.ttl("sendtimeCount was: ", sendtimeCount.get());
                realtimeCount.set(sendtimeCount.get());
                sendtimeCount.set(0);
                format.ttl("realtimeCount: ", realtimeCount.get());
                format.ttl("sendtimeCount: ", sendtimeCount.get());
                format.ttl("returning");
                //                LoggerFactory.getLogger(getClass()).info(format.toString());
                return;
            }
            format.ttl("realtime count < ", maxPerInterval, " continue to check sendtime processing");
            format.dec();
        }
        // capture counts in the sendtime interval window
        sendtimeCount.incrementAndGet();
        format.ttl("realtimeCount: ", realtimeCount.get());
        format.ttl("sendtimeCount: ", sendtimeCount.get());
        // if same send interval, we need to block on sendtimeCounts >= maxPerInterval
        if (realtimeCount.get() >= maxPerInterval)
        {
            // block until the next realtime interval
            format.ttl("need to wait");
            format.inc();
            long realRemainder = elapsedRealtime % customScale;
            if (realRemainder == 0)
                ++realRemainder;
            realRemainder = customScale - realRemainder;
            ++realRemainder; // rounding errors can leave us just a bit shy, would rather go over
            format.ttl("waiting for: ", ElapsedTimer.getNormalizedString(realRemainder));
            try
            {
                Thread.sleep(realRemainder);
            } catch (InterruptedException e)
            {
            }
            realtimeCount.set(0);
            sendtimeCount.set(0);
            t1 = System.currentTimeMillis();
            realInterval = t1 / customScale;
        }
        //        LoggerFactory.getLogger(getClass()).info(format.toString());
    }

    @SuppressWarnings("javadoc")
    public enum IntervalType
    {
        Second(1000), Minute(1000 * 60), Hour(1000 * 60 * 60), Day(1000 * 60 * 60 * 24);

        IntervalType(int scale)
        {
            this.scale = scale;
        }

        public static IntervalType getIntervalType(int seconds)
        {
            if (seconds < 61)
                return Second;
            if (seconds < 3601)
                return Minute;
            if (seconds < 86401)
                return Hour;
            return Day;
        }

        public static RequestThrottle getRequestThrottle(int maxCount, int seconds)
        {
            if (seconds < 61)
                return new RequestThrottle(maxCount, IntervalType.Second, 60/seconds);
            if (seconds < 3601)
                return new RequestThrottle(maxCount, IntervalType.Minute, seconds/60);
            if (seconds < 86401)
                return new RequestThrottle(maxCount, IntervalType.Hour, seconds/3600);
            return new RequestThrottle(maxCount, IntervalType.Day, seconds/86400);
        }
        public int getScale()
        {
            return scale;
        }

        private int scale;
    }
}
