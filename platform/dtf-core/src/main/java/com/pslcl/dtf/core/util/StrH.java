/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.core.util;

import java.util.Map;
import java.util.Map.Entry;


@SuppressWarnings("javadoc")
public class StrH
{
    public final static char ForwardSlashSeperator = '/';
    public final static char BackSlashSeperator = '\\';

    
    public static String mapToString(Map<?, ?> map)
    {
        StringBuilder sb = new StringBuilder("{[");
        boolean firstDone = false;
        for(Entry<?,?> entry : map.entrySet())
        {
            if(!firstDone)
                firstDone = true;
            else
                sb.append(",");
            String value = (String)entry.getValue(); 
            if(value == null)
                value = "null";
            sb.append("{" + entry.getKey().toString() + "," + value + "}");
        }
        sb.append("]}");
      return sb.toString();  
    }
    
    public static StringBuilder ttl(StringBuilder sb, int level, Object ... values)
    {
        String[] array = new String[values.length];
        for(int i=0; i < array.length; i++)
            array[i] = (values[i] == null ? "null" : values[i].toString());
        return tabToLevel(sb, level, true, array);
    }

    /**
     * Tab to level.
     *
     * @param sb to add the values to
     * @param level number of tabs
     * @param eol add eol at end of values
     * @param values list of values to add
     * @return the string builder that has handed in.
     */
    public static StringBuilder tabToLevel(StringBuilder sb, int level, boolean eol, String ... values)
    {
        for(int i=0; i < level; i++)
            sb.append("\t");
        for(int j=0; j < values.length; j++)
            sb.append(values[j]);
        if(eol)
            sb.append("\n");
        return sb;
    }
    
    /**
     * Pad string to width and add to StringBuilder.
     *
     * @param sb to add newValue to
     * @param newValue the newValue to be padded. 
     * @param length desired length of newValue
     * @return the input sb if it was not null, otherwise one was provided.
     */
    public static StringBuilder pad(StringBuilder sb, String newValue, int length)
    {
        if(sb == null)
            sb = new StringBuilder();
        if(newValue == null)
            newValue = "";
        sb.append(newValue);
        int delta = length - newValue.length();
        if(delta <= 0)
            return sb;
        for(int i=0; i < delta; i++)
            sb.append(" ");
        return sb;
    }
    
    /**
     * Gets the atomic name from a character seperated full name.
     *
     * @param name the fully distinguished name
     * @param seperator the name space separator
     * @return the atomic name
     */
    public static String getAtomicName(String name, char seperator)
    {
        if (name == null)
            return null;

        int index = name.lastIndexOf(seperator);
        if (index == -1)
            return name;
        return name.substring(++index);
    }
    
    /**
     * Gets the penultimate name.
     *
     * @param name the name
     * @param seperator the seperator
     * @return the penultimate name
     */
    public static String getPenultimateName(String name, char seperator)
    {
        if (name == null)
            return null;

        int index = name.lastIndexOf(seperator);
        if (index == -1)
            return null;
        return (name.substring(0, index));
    }
    
    /**
     * Gets the atomic name from a file system path
     * @param name the name
     * @return the atomic name from the path
     */
    public static String getAtomicNameFromPath(String name)
    {
        if (name == null)
            return null;

        int index = name.lastIndexOf(ForwardSlashSeperator);
        if (index == -1)
            index = name.lastIndexOf(BackSlashSeperator);
        if (index == -1)
            index = name.lastIndexOf(".");
        if (index == -1)
            return name;

        ++index;
        return (name.substring(index));
    }
    
    /**
     * Gets the penultimate name from a file system path.
     * @param name the name
     * @return the penultimate name
     */
    public static String getPenultimateNameFromPath(String name)
    {
        if (name == null)
            return null;

        int index = name.lastIndexOf(ForwardSlashSeperator);
        if (index == -1)
            index = name.lastIndexOf(BackSlashSeperator);
        if (index == -1)
            return null;

        return (name.substring(0, index));
    }
    
    public static String stripTrailingSeparator(String value)
    {
        if(value == null || value.length() == 0)
            return value;
        value = value.trim();
        if(value.charAt(value.length() - 1) == '/')
            value = value.substring(0, value.length() - 1);
        if(value.charAt(value.length() - 1) == '\\')
            value = value.substring(0, value.length() - 1);
        return value;
    }
    
    public static String addTrailingSeparator(String value, char seperator)
    {
        return addTrailingSeparator(value, ""+seperator);
    }
    
    public static String addTrailingSeparator(String value, String seperator)
    {
        if(value == null || value.length() == 0)
            return value;
        value = value.trim();
        boolean exists = false;
        if(value.charAt(value.length() - 1) == '/')
            exists = true;
        if(value.charAt(value.length() - 1) == '\\')
            exists = true;
        if(!exists)
            value += seperator;
        return value;
    }
    
    public static class StringPair implements Entry<String, String>
    {
        private final String key;
        private String value;
        
        public StringPair(String key, String value)
        {
            this.key = key;
            this.value = value;
        }
        
        public StringPair(Entry<Object, Object> entry)
        {
            key = (String) entry.getKey();
            value = (String) entry.getValue();
        }
        
        @Override
        public String setValue(String value)
        {
            String old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public String getValue()
        {
            return value;
        }
        
        @Override
        public String toString()
        {
            return key+"="+value; 
        }
    }
    
    public static String getAttribute(Map<String,String> map, String key, String defaultValue)
    {
        String value = map.get(key);
        if(value != null)
            return value;
        return defaultValue;
    }
    
    public static int getIntAttribute(Map<String,String> map, String key, String defaultValue)
    {
        String value = map.get(key);
        if(value == null)
            value = defaultValue;
        return Integer.parseInt(value);
    }
    
    public static long msToNano(long ms)
    {
        return 1000000 * ms;
    }

    public static long nanoToMs(long ns)
    {
        return ns / 1000000;
    }
    
    public static String scaleMilliSeconds(long value)
    {
        return scaleNanoSeconds(msToNano(value));
    }
    
    public static String scaleNanoSeconds(long value)
    {
/*
                about max               about min
ns:              999                            1
mico:         999222                         1222
ms:        999111222                      1111222
sec:     59000111222                   1000111222
min:   3633000111222                  60000111222
hr:   86400000000000                3600000111222
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
}
