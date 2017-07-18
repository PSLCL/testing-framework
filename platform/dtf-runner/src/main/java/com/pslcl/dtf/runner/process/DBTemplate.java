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
package com.pslcl.dtf.runner.process;

import java.util.Date;

/**
 *
 */
public class DBTemplate {

//    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
//    private static String bytesToHex(byte[] bytes)
//    {
//        char[] hexChars = new char[bytes.length * 2];
//        for (int j = 0; j < bytes.length; j++)
//        {
//            int v = bytes[j] & 0xFF;
//            hexChars[j * 2] = hexArray[v >>> 4];
//            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
//        }
//        return new String(hexChars);
//    }

    /**
     * Return the value of the hash byte array as a hex String.
     *
     * @param templateHash The hash array
     * @return Hex String representation of the hash array
     */
    static public String getId(byte [] templateHash)
    {
        return new String(templateHash);
    }

    // instance members

    long pk_template = -1;      // INT(11) in template
    public byte [] hash;        // BINARY(32) in template
    public String steps = null; // MEDIUMTEXT in template
    boolean enabled = false;    // BOOLEAN in template
    Long reNum;                 // INT(11) in run.pk_run
    byte[] artifacts;           // LONGBLOB in run
    Date start_time = null;     // DATETIME in run
    Date ready_time = null;     // DATETIME in run
    Date end_time = null;       // DATETIME in run
    Boolean result = new Boolean(false); // nullable BOOLEAN in run
    String owner = null;        // VARCHAR(128) in run

    /**
     *  Constructor
     *
     *  @param reNum The run entry number
     */
    public DBTemplate(Long reNum) {
        this.reNum = reNum;
    }

    public Long getReNum() {
        return this.reNum;
    }

    public boolean checkValidTemplateInfo() {
        return (this.hash!=null && this.steps!=null && this.enabled);
    }

    public boolean isTopLevelTemplate() {
        return this.reNum >= 0;
    }

}