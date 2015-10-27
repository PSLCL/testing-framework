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
package com.pslcl.dtf.platform.core.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Utility class for accessing package classmap.
 */
public class ClassInfo {
	
	private static final HashMap<String, ClassInfo> classmap = new HashMap<String, ClassInfo>();
	
	private final Class<?> c;
	private final Package pkg;
	private Manifest manifest;
	private URL location;
    
    /**
     * Get the version of a class's package using the current class loader.
     * 
     * @param className The name of the class including package.
     * @return The version of the class's package.
     * @throws Exception the exception
     */
    public static ClassInfo getInfo(String className) throws ClassNotFoundException {
        if( className == null )
            throw new IllegalArgumentException("ClassInfo.getInfo: className == null");
        
        ClassInfo ci = classmap.get(className);
        if (ci == null) {
        	Class<?> c = Class.forName(className);
        	ci = new ClassInfo(c);
        }
        
        return ci;
    }
    
    /**
     * Get the version of a class's package using the specified class loader.
     * 
     * @param className The name of the class including package.
     * @param classLoader The class's associated class loader.
     * @return The version of the class's package.
     * @throws Exception the exception
     */
    public static ClassInfo getInfo(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if( className == null || classLoader == null )
            throw new IllegalArgumentException("ClassInfo.getInfo: className == null || classLoader == null");
        
        ClassInfo ci = classmap.get(className);
        if (ci == null) {
            Class<?> c = Class.forName(className, false, classLoader);
            ci = new ClassInfo(c);
        }
        
        return ci;
    }
    
    /**
     * Get the version of a class's package.
     *
     * @param c The class.
     * @return The version of the class's package.
     */
    public static ClassInfo getInfo(Class<?> c) {
        if( c == null )
            throw new IllegalArgumentException("ClassInfo.getInfo: c == null");
        
    	ClassInfo ci = classmap.get(c.getName());
    	if (ci == null) {
    		ci = new ClassInfo(c);
    	}
    	return ci;
    }

    /**
     * Get the version of a class's package.
     *
     * @param c The class.
     * @return The version of the class's package.
     */
    private ClassInfo(Class<?> c) {
        this.c = c;
        this.pkg = c.getPackage();
        this.manifest = null;
        this.location = null;
        classmap.put(c.getName(), this);
    }

    /**
     * Return the cached package.
     *
     * @return The cached package.
     */
    public Package getPackage() {
    	return pkg;
    }
    
    /**
     * Return the cached class location.
     *
     * @return The cached class location. May be null if the location was not available for the class.
     */
    public URL getLocation() {
    	if (location == null) {
	        try {
	            this.location = c.getProtectionDomain().getCodeSource().getLocation();
	        }
	        catch (Exception e) {
	        }
    	}
    	return location;
    }
    
    /**
     * Return the cached manifest.
     *
     * @return The cached manifest. May be null if a manifest was not available for the class.
     */
    public Manifest getManifest() {
    	if (manifest == null) {
            JarInputStream jis = null;
            try {
                URLConnection jar_connection = getLocation().openConnection();
                jis = new JarInputStream(jar_connection.getInputStream());
                this.manifest = jis.getManifest();
            }
            catch (Exception e) {
            }
            finally {
                if ( jis != null ) {
                    try {
                    	jis.close();
                    } catch (IOException ioe) {
                    	// Ignore
                    }
                }
            }
    	}
    	return manifest;
    }
    
    /**
     * Return the cached class.
     *
     * @return The cached class.
     */
    public Class<?> getClassRef() {
    	return c;
    }
    
    /**
     * Return the class information String representation.
     *
     * @return The class information String representation.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(c.getName())
        	.append(": ")
        	.append(pkg.getImplementationTitle())
        	.append(" ")
        	.append(pkg.getImplementationVersion())
        	.append(" <")
        	.append(pkg.getImplementationVendor())
        	.append("> (")
        	.append(getLocation())
        	.append(")");
        return sb.toString();
    }
}
