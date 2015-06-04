package com.pslcl.qa.platform;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

/**
 * This class represents a hash, which is a SHA-256 hash of some content.
 */
public class Hash implements Comparable<Hash> {
    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex( byte[] bytes ) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String( hexChars );
    }

    private String value = null;

    public static Hash fromContent( File f ) {
        int buff = 16384;
        try {
            RandomAccessFile file = new RandomAccessFile( f, "r" );
            MessageDigest hashSum = MessageDigest.getInstance( "SHA-256" );

            byte[] buffer = new byte[buff];
            byte[] partialHash = null;

            long read = 0;

            long offset = file.length();
            int unitsize;
            while ( read < offset ) {
                unitsize = (int) (((offset - read) >= buff) ? buff : (offset - read));
                file.read(buffer, 0, unitsize);
                hashSum.update(buffer, 0, unitsize);
                read += unitsize;
            }

            file.close();
            partialHash = hashSum.digest();
            return new Hash( partialHash );
        } catch ( Exception e ) {
            return null;
        }
    }

    public Hash( byte[] hash ) {
        value = bytesToHex( hash );
    }

    public static Hash fromContent( String content ) {
        try {
            MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
            byte[] hash = digest.digest( content.getBytes("UTF-8") );
            return new Hash( hash );
        }
        catch ( Exception e ) {
            // This should never happen.
            return null;
        }
    }

    public static Hash fromContent( InputStream stream ) {
        try {
            MessageDigest digest = MessageDigest.getInstance( "SHA-256" );

            final int BUFFER_MAX_SIZE = 8192;
            byte[] buffer = new byte[ BUFFER_MAX_SIZE ];

            int count = 0;
            while ( ( count = stream.read( buffer, 0, BUFFER_MAX_SIZE ) ) != -1 ) {
                digest.update( buffer, 0, count );
            }

            stream.close();
            return new Hash( digest.digest() );
        }
        catch ( Exception e ) {
            // This should never happen
            return null;
        }
    }

    public String toString() {
        return value;
    }

    public byte[] toBytes() {
        return DatatypeConverter.parseHexBinary( value );
    }

    public int compareTo( Hash o ) {
        return value.compareTo( o.value );
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals( Object o ) {
        if ( ! (o instanceof Hash) )
            return false;

        Hash that = (Hash) o;
        return this.value.equals( that.value );
    }
}
