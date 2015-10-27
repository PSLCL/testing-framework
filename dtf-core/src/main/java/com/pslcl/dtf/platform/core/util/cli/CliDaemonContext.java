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
package com.pslcl.dtf.platform.core.util.cli;

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;


@SuppressWarnings("javadoc")
public class CliDaemonContext implements DaemonContext
{
    private final DaemonController controller;
    private String[] args;
    
    public CliDaemonContext(CliBase cliBase)
    {
        controller = new CliDaemonController(cliBase);
    }

    @Override
    public DaemonController getController()
    {
        return controller;
    }

    @Override
    public synchronized String[] getArguments()
    {
        return args;
    }

    public synchronized void setArguments(String[] args)
    {
        this.args = new String[args.length];
        System.arraycopy(args, 0, this.args, 0, args.length);
    }
    
    public static class CliDaemonController implements DaemonController
    {
        public final CliBase cliBase;
        public CliDaemonController(CliBase cliBase)
        {
            this.cliBase = cliBase;
        }
        
        @Override
        public void shutdown() throws IllegalStateException
        {
        }

        @Override
        public void reload() throws IllegalStateException
        {
        }

        @Override
        public void fail() throws IllegalStateException
        {
        }

        @Override
        public void fail(String message) throws IllegalStateException
        {
        }

        @Override
        public void fail(Exception exception) throws IllegalStateException
        {
        }

        @Override
        public void fail(String message, Exception exception) throws IllegalStateException
        {
        }
    }
}