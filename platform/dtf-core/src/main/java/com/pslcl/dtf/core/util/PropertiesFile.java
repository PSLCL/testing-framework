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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.pslcl.dtf.core.util.StrH.StringPair;

/**
 * Static properties file helper 
 */
public class PropertiesFile
{

    private PropertiesFile()
    {
        // prevent construction
    }

    /**
     * Load a properties file from the given path from the filesystem.
     * @param url The URL to load. This must not be null.
     * @return the properties object loaded with the name/value pairs from the given file.  Will not return null.
     * @throws FileNotFoundException if the file could not be found on the filesystem.
     * @throws IOException if there was an error reading the file.
     * @throws IllegalArgumentException if file is null.
     */
    public static Properties load(URL url) throws IOException
    {
        if (url == null)
            throw new IllegalArgumentException("url == null");
        Properties properties = new Properties();
        InputStream stream = url.openStream();
        try
        {
            properties.load(stream);
        } finally
        {
            try
            {
                if (stream != null)
                    stream.close();
            } catch (Exception e)
            {
                e.toString(); // lose findbugs warning best try
            }
        }
        return properties;
    }

    /**
     * Load a properties file from the given path, either from the filesystem or from the classpath.
     * @param properties The properties object to be populated from the contents of the file. This must not be null.
     * @param path The path to the file. This must not be null.
     * @throws FileNotFoundException if the file could not be found on the filesystem or in the classpath.
     * @throws IOException if there was an error reading the file.
     */
    public static void load(Properties properties, String path) throws FileNotFoundException, IOException
    {
        if (properties == null || path == null)
            throw new IllegalArgumentException("properties == null || path == null");
        try
        {
            loadFile(properties, new File(path));
        } catch (FileNotFoundException ex)
        {
            try
            {
                loadResource(properties, path);
            } catch (FileNotFoundException e)
            {
                throw new FileNotFoundException(path + " not found on filesystem or classpath");
            }
        }
    }

    /**
     * Load a properties file from the given path from the filesystem.
     * @param properties The properties object to be populated from the contents of the file. This must not be null.
     * @param file The file to load. This must not be null.
     * @throws FileNotFoundException if the file could not be found on the filesystem.
     * @throws IOException if there was an error reading the file.
     */
    public static void loadFile(Properties properties, File file) throws FileNotFoundException, IOException
    {
        if (properties == null || file == null)
            throw new IllegalArgumentException("properties == null || path == null");
        InputStream isPrimary = null;
        try
        {
            isPrimary = new FileInputStream(file);
            properties.load(isPrimary);
        } finally
        {
            try
            {
                if (isPrimary != null)
                    isPrimary.close();
            } catch (Exception e)
            {
                // Ignore
            }
        }
    }

    /**
     * Load a properties file from the given path from the classpath.
     * @param properties The properties object to be populated from the contents of the file. This must not be null.
     * @param path The path to the file. This must not be null.
     * @throws FileNotFoundException if the file could not be found on the classpath.
     * @throws IOException if there was an error reading the file.
     */
    public static void loadResource(Properties properties, String path) throws FileNotFoundException, IOException
    {
        if (properties == null || path == null)
            throw new IllegalArgumentException("properties == null || path == null");
        InputStream isPrimary = null;
        try
        {
            isPrimary = ClassLoader.getSystemResourceAsStream(path);
            if (isPrimary == null)
            {
                isPrimary = ClassLoader.getSystemResourceAsStream(path.substring(path.lastIndexOf('/')));
                if (isPrimary == null)
                {
                    throw new FileNotFoundException(path + " not found on classpath");
                }
            }
            properties.load(isPrimary);
        } finally
        {
            try
            {
                if (isPrimary != null)
                    isPrimary.close();
            } catch (Exception e)
            {
                // Ignore
            }
        }
    }

    @SuppressWarnings("javadoc")
    public static List<Entry<String, String>> getPropertiesForBaseKey(String baseKey, Map<String, String> map)
    {
        Properties properties = new Properties();
        for(Entry<String, String> entry : map.entrySet())
        {
            String value = entry.getValue();
            if(value != null)
                properties.setProperty(entry.getKey(), value);
        }
        return getPropertiesForBaseKey(baseKey, properties);
    }
    
    @SuppressWarnings("javadoc")
    public static List<Entry<String, String>> getPropertiesForBaseKey(String baseKey, Properties properties)
    {
        ArrayList<Entry<String, String>> entries = new ArrayList<Entry<String, String>>();
        Hashtable<Integer, StringPair> orderingMap = new Hashtable<Integer, StringPair>();
        
        int found = 0;
        for (Entry<Object, Object> entry : properties.entrySet())
        {
            String key = (String) entry.getKey();
            int index = 0;
            if (key.startsWith(baseKey))
            {
                ++found;
                char[] chars = key.toCharArray();
                if(Character.isDigit(chars[chars.length-1]))
                {
                    int strIndex = 0;
                    for(int i=chars.length-1; i >=0; i--)
                    {
                        if(!Character.isDigit(chars[i]))
                        {
                            strIndex = i + 1;
                            break;
                        }
                    }
                    index = Integer.parseInt(key.substring(strIndex));
                }
                orderingMap.put(index, new StringPair(entry));
            }
        }
        int i=0;
        int hit = 0;
        do
        {
            StringPair pair = orderingMap.get(i);
            if(pair != null)
            {
                entries.add(pair);
                ++hit;
            }
            ++i;
        }while(hit < found);
        return entries;
    }
}
