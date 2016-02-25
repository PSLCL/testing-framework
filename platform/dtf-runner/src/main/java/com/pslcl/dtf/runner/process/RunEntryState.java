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

public class RunEntryState {
    
    private final long reNum;
    private Object message;
    private Action action;
    private Action lastAction;
    
    /**
     * Constructor
     * @param reNum
     * @param message Opaque object used eventually to ack the templateNumber held in the QueueStore
     */
    public RunEntryState(long reNum, Object message) {
        this.reNum = reNum;
        this.message = message;
        this.action = Action.INITIALIZE;
        this.lastAction = this.action;
    }

    long getRunEntryNumber() {
        return reNum;
    }
    
    Object getMessage() {
        return message;
    }

    Action getAction() {
        return action;
    }
    
    void setAction(Action action) {
        this.action = action;
    }
    
    /**
     * @note blocks until Action has changed; calling this.setAction() is required 
     * @return
     * @throws InterruptedException
     */
    Action getNewAction() throws InterruptedException {
    	do {
	    	if (this.lastAction != this.action) {
	    		this.lastAction =  this.action;
	    		return this.action;
	    	}
	    	
	        try {
	            Thread.sleep(1000);
	        } catch (InterruptedException ie) {
	        	throw ie;
	        }
    	} while (true);
    }
    
}
