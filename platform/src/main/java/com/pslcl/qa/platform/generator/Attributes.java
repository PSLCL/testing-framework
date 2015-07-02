package com.pslcl.qa.platform.generator;

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
    SortedMap<String,String> map = null;
    String value = null;

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
        map = new TreeMap<String, String>();

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

    public void put( String attribute, String value ) {
        if ( map == null )
            valueToMap();

        map.put( attribute, value );
        this.value = null;
    }

    public String get( String attribute ) {
        if ( map == null )
            valueToMap();

        return map.get( attribute );
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
