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
package com.pslcl.dtf.core.runner.resource.jsch;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class JschConnect
{
    public static void main(String[] arg)
    {

//        String pubkeyfile = "/home/cr/users/anand/.ssh/id_dsa";
        String pubkeyfile = "/wsp/developer/tests/config/ssh/chad-windows.pem";
        String passphrase = "";
        String host = "52.91.84.25", user = "ec2-user";

        try
        {
            JSch jsch = new JSch();
            jsch.addIdentity(pubkeyfile);
            jsch.setConfig("StrictHostKeyChecking", "no");
            //   jsch.addIdentity(pubkeyfile, passphrase);
//            jsch.setKnownHosts("/home/cr/users/anand/.ssh/known_hosts");
            jsch.setKnownHosts("/wsp/developer/tests/config/ssh/known_hosts");

            Session session = jsch.getSession(user, host, 22);
            session.connect();

            Channel channel = session.openChannel("shell");

            channel.setInputStream(System.in);
            channel.setOutputStream(System.out);

            channel.connect();
        } catch (Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }
    } //end of main 
} //end of class}