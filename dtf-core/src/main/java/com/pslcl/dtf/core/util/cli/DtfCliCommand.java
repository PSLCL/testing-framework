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
package com.pslcl.dtf.core.util.cli;

@SuppressWarnings("javadoc")
public class DtfCliCommand extends CliCommand
{
    // CliBase declares the following shorts. Best not to override them here.
    // 'h',"help"
    // 'c', "config-path" 
    // 'v', "version" 
    
    public DtfCliCommand(CliBase cliBase, String commandName)
    {
        super(cliBase, commandName);
    }

    public DtfCliCommand(CliBase cliBase, CliCommand previousCommand, String commandName)
    {
        super(cliBase, previousCommand, commandName);
    }

    @Override
    protected void cliSetup()
    {
        cliBase.cliReservedSetup(options, cliBase.isLogToFile(), cliBase.isConfigSwitch());
    }
    
    @Override
    protected void customInit()
    {
    }
}