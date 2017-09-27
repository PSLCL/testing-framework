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
package com.pslcl.dtf.core;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

/**
 * This class represents a hash, which is a SHA-256 hash of some content.
 */
public class Hash implements Comparable<Hash>
{
    private static final int buffsize = 16384; // cannot be java long
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    @SuppressWarnings("MagicNumber")
    public static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    private String value = null;

    public static Hash fromContent(File f)
    {
        try {
            MessageDigest hashSum;
            try (RandomAccessFile file = new RandomAccessFile(f, "r")) {
                hashSum = MessageDigest.getInstance("SHA-256");
                long read = 0;

                long filelength = file.length();
                byte[] buffer = new byte[buffsize];
                while (read < filelength) {
                    long filelengthMinusRead = filelength - read;
                    long constrainedUnitsize = filelengthMinusRead>buffsize ? buffsize:filelengthMinusRead; // max buffsize is Integer.MAX_VALUE
                    @SuppressWarnings("NumericCastThatLosesPrecision") // we constrained it to Integer.MAX_VALUE (or buffsize if that is smaller)
                    int unitsize = (int)constrainedUnitsize;
                    file.read(buffer, 0, unitsize);
                    hashSum.update(buffer, 0, unitsize);
                    read += unitsize;
                }
//              file.close(); // Auto close happens (with our use of try-with-resources)
            }
            byte[] partialHash = hashSum.digest();
            return new Hash(partialHash);
        } catch (Exception ignore) {
            return null;
        }
    }

    public Hash(byte[] hash)
    {
        value = bytesToHex(hash);
    }

    public static Hash fromContent(String content)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes("UTF-8"));
            return new Hash(hash);
        } catch (Exception ignore)
        {
            // This should never happen.
            return null;
        }
    }

    public static Hash fromContent(InputStream stream)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            final int BUFFER_MAX_SIZE = 8192;
            byte[] buffer = new byte[BUFFER_MAX_SIZE];

            while (true) {
                int count = stream.read(buffer, 0, BUFFER_MAX_SIZE);
                if (count == -1)
                    break;
                digest.update(buffer, 0, count);
            }

            stream.close();
            return new Hash(digest.digest());
        } catch (Exception ignore)
        {
            // This should never happen
            return null;
        }
    }

    @Override
    public String toString()
    {
        return value;
    }

    public byte[] toBytes()
    {
        return DatatypeConverter.parseHexBinary(value);
    }

    @Override
    // Warning appears wrong "Not annotated param overrides @NotNull param." .compareTo() has not @NotNull requirement.
    // noinspection
    public int compareTo(Hash o)
    {
        return value.compareTo(o.value);
    }

    @Override
    public int hashCode()
    {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Hash))
            return false;

        Hash that = (Hash) o;
        return this.value.equals(that.value);
    }
}
