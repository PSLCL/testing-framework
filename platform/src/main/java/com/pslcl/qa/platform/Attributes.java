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
package com.pslcl.qa.platform;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class represents a set of attributes that can be encoded as a string. Each attribute
 * can appear only once in an instance. In order to preserve hashing, the string representation
 * is always sorted by increasing attribute.
 * The format of the string is always attribute=value&attribute=value where each value is URL encoded.
 */
public class Attributes {
    private SortedMap<String,String> map = new TreeMap<String, String>();
    private String value = null;

    public Attributes() {
    }

    private void mapToValue() {
        boolean empty = true;
        StringBuilder sb = new StringBuilder();

        for ( SortedMap.Entry<java.lang.String,String> E : map.entrySet() ) {
            if ( ! empty )
                sb.append( "&" );
            empty = false;

            sb.append( E.getKey() );
            sb.append( "=" );
            try {
                sb.append( URLEncoder.encode( E.getValue(), "UTF-8" ) );
            }
            catch ( Exception e ) {
                // This should never happen, as UTF-8 is a required charset.
                sb.append( "<exception>" );
            }
        }

        value = sb.toString();
    }

    private void valueToMap() {    	
        try {
            if ( value != null ) {
                String[] elements = value.split("&");
                for ( String e : elements ) {
                    String[] kv = e.split("=");
                    if ( kv.length != 2 )
                        throw new IllegalArgumentException( "Attributes: value is not properly encoded." );
                    map.put( kv[0], URLDecoder.decode( kv[1], "UTF-8" ) );
                }
            }
        }
        catch ( Exception e ) {
            // Ignore, UTF-8 is a required encoding.
        }
        value = null;
    }

    public Attributes( Attributes attributes ) {
        map.putAll( attributes.map );
    }
    
    public Attributes( Map<String,String> attributes ) {
        map.putAll( attributes );
    }

    public Attributes( String attributes ) {
        value = attributes;
        valueToMap();
        mapToValue();
        if ( attributes.compareTo( value ) != 0 )
            throw new IllegalArgumentException( "Attributes: value is not properly encoded." );
    }

    public Attributes putAll( Attributes attributes ) {
        putAll( attributes.map );
        return this;
    }
    
    public Attributes putAll( Map<String,String> attributes ) {
        map.putAll( attributes );
        return this;
    }
    
    public Attributes put( String attribute, String value ) {
        map.put( attribute, value );
        mapToValue();
        return this;
    }

    public String get( String attribute ) {
        return map.get( attribute );
    }
    
    public Map<String, String> getAttributes(){
    	return new TreeMap<String, String>(map);
    }

    public String toString() {
        if ( value == null && map == null )
            return "";

        if ( value == null )
            mapToValue();

        return value;
    }

    public int hashCode() {
        if ( value == null )
            mapToValue();

        return value.hashCode();
    }
}
