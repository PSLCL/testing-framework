package com.pslcl.qa.runner.config.util;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("javadoc")
public class TabToLevel
{
    public final StringBuilder sb;
    public final AtomicInteger level;
    
    public TabToLevel(StringBuilder sbIn)
    {
        if(sbIn == null)
            sbIn = new StringBuilder();
        sb = sbIn;
        level = new AtomicInteger(0);
    }
    
    public void ttl(Object ... values)
    {
        String[] array = new String[values.length];
        for(int i=0; i < array.length; i++)
            array[i] = (values[i] == null ? "null" : values[i].toString());
        tabToLevel(true, array);
    }

    public void tabToLevel(boolean eol, String ... values)
    {
        for(int i=0; i < level.get(); i++)
            sb.append("\t");
        for(int j=0; j < values.length; j++)
            sb.append(values[j]);
        if(eol)
            sb.append("\n");
    }
    
    public void indentedOk()
    {
        level.incrementAndGet();
        ttl("ok");
        level.decrementAndGet();
    }
    
}
