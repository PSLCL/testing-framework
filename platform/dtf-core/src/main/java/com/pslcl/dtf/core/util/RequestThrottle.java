/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dtf.core.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Requests Per Second Throttling class
 */
public class RequestThrottle
{
    private final AtomicInteger sendCount;
    private final AtomicLong lastSend;
    private final AtomicLong lastSecond;
    private final int maxPerSecond;

    /**
     * Request Throttle constructor
     * @param maxPerSecond the maximum allowed requests per second
     */
    public RequestThrottle(int maxPerSecond)
    {
        sendCount = new AtomicInteger(0);
        // go back 2 seconds to be well out of current second range for first request
        long t1 = System.currentTimeMillis();
        lastSecond = new AtomicLong(t1 - 3000);
        lastSend = new AtomicLong(lastSecond.get());
        this.maxPerSecond = maxPerSecond;
    }

    /**
     * wait for next second if we are already at max.
     */
    public void waitAsNeeded()
    {
        long t1 = System.currentTimeMillis();
        long deltaFromLastSend = t1 - (lastSend.get());
//        System.out.println("*** deltaFromLastSend: " + deltaFromLastSend + " ***");
        long deltaFromLastSecond = lastSend.get() - lastSecond.get();
//        System.out.println("*** deltaFromLastSecond: " + deltaFromLastSecond + " ***");
        if (deltaFromLastSend < 1000 && deltaFromLastSecond < 1000)
        {
//            System.out.println("*** deltaFromLastSend and second < 1000 ***");
            int count = sendCount.incrementAndGet();
            if (count >= maxPerSecond)
            {
//                System.out.println("*** max count in 1 second hit ***");
                try
                {
                    long neededDelay = 1000 - (deltaFromLastSend + deltaFromLastSecond);
//                    System.out.println("*** neededDelay: " + neededDelay + " ***");
                    if(neededDelay > 0)
                        Thread.sleep(neededDelay+2); // < 1 should never happen
//                    else
//                        System.out.println("*** neededDelay negative ***");
                } catch (InterruptedException e)
                {
                }
                sendCount.set(1);
                lastSecond.set(t1);
                lastSend.set(t1);
                return;
            }
//            System.out.println("*** less than second but max count not hit ***");
            lastSend.set(t1);
            return;
        }
//        System.out.println("*** outside of 1 second ***");
        sendCount.set(1);
        lastSecond.set(t1);
        lastSend.set(t1);
    }
}
