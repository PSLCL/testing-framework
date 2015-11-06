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

public class DBTemplate {
    
    long pk_template;       // INT(11) in template
    public byte [] hash;    // BINARY(32) in template
    public String steps;    // MEDIUMTEXT in template
    boolean enabled;        // BOOLEAN in template
    Long reNum;             // INT(11) in run.pk_run
    byte[] artifacts;       // LONGBLOB in run
    Date start_time;        // DATETIME in run
    Date ready_time;        // DATETIME in run
    Date end_time;          // DATETIME in run
    Boolean result = false; // nullable BOOLEAN in run
    String owner;           // VARCHAR(128) in run
    
    /**
     *  Constructor
     */
    public DBTemplate(Long reNum) {
        this.reNum = reNum;
    }
    
}
