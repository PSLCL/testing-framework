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
package com.pslcl.dtf.resource.aws.instance.person;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.provider.person.AwsPersonProvider;

@SuppressWarnings("javadoc")
public class InspectWithAttachmentFuture implements Callable<Void>
{
    public static final String SesMidStr = "ses";
    private final AmazonSimpleEmailServiceClient client;
    private final PersonConfigData config;
    private final String attachmentFileName;
    private final InputStream attachmentFileStream;
    private final String recipient;
    private final String bodyText;
    private final ProgressiveDelayData pdelayData;
    private final TabToLevel format;
    private final String msg;

    //@formatter:off
    public InspectWithAttachmentFuture(
                    AmazonSimpleEmailServiceClient client, 
                    PersonConfigData config, 
                    String recipient, 
                    String attachmentFileName,
                    InputStream fileStream,
                    String bodyText,
                    ProgressiveDelayData pdelayData)
    //@formatter:on
    {
        this.client = client;
        this.config = config;
        this.attachmentFileName = attachmentFileName;
        this.attachmentFileStream = fileStream;
        this.recipient = recipient;
        this.bodyText = bodyText;
        this.pdelayData = pdelayData;
        format = new TabToLevel();
        format.ttl(getClass().getSimpleName());
        format.level.incrementAndGet();
        msg = pdelayData.getHumanName(SesMidStr, "sendRawEmail");
        format.ttl(msg);
        format.level.incrementAndGet();
        format.ttl("sender = ", config.sender);
        format.ttl("reply = ", config.reply);
        format.ttl("recipient = ", recipient);
    }

    private File getLocalFile(String fileName, InputStream inputStream) throws IOException
    {
        //        int idx = fileName.lastIndexOf('.');
        //        String postfix = null;
        //        if (idx != -1)
        //        {
        //            postfix = fileName.substring(idx);
        //            fileName = fileName.substring(0, idx);
        //        }
        //        File tmpFile = File.createTempFile(pdelayData.preFixMostName + "-" + fileName, postfix);
        String tmpPath = System.getProperty("java.io.tmpdir");
        tmpPath = tmpPath.replace('\\', '/');
        if (!tmpPath.endsWith("/"))
            tmpPath += "/";
        tmpPath += fileName;
        File tmpFile = new File(tmpPath);
        if (tmpFile.exists())
        {
            tmpFile.delete();
            // just a note that some previous inspect must have failed and not cleanup properly 
            LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".getTmpFile deleted existing tmpFile " + tmpFile.getAbsolutePath());
        }
        format.ttl("tmpFile = ", tmpFile.getAbsolutePath());
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(tmpFile, false);
            byte[] raw = new byte[4096];
            int size = -1;
            do
            {
                size = inputStream.read(raw);
                if (size != -1)
                    fos.write(raw, 0, size);
            } while (size != -1);
            fos.flush();
        } finally
        {
            try
            {
                if (fos != null)
                {
                    fos.close();
                }
            } catch (IOException e)
            {
                LoggerFactory.getLogger(getClass()).info(e.getMessage(), e);
            }
        }
        return tmpFile;
    }

    @Override
    public Void call() throws Exception
    {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("InspectWithAttachmentFuture");
        synchronized (pdelayData.provider)
        {
            // since we can only send 1 per second synch the whole call thus simplifying the 
            // temp file creation/rename and delete as well as the call to RequestThrottle
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);
            message.setSubject(config.subject + "runId: " + pdelayData.coord.getRunId(), "UTF-8");

            message.setFrom(new InternetAddress(config.sender));
            message.setReplyTo(new Address[] { new InternetAddress(config.reply) });
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));

            // Cover wrap
            MimeBodyPart wrap = new MimeBodyPart();

            // Alternative TEXT/HTML content
            MimeMultipart cover = new MimeMultipart("alternative");
            MimeBodyPart html = new MimeBodyPart();
            cover.addBodyPart(html);
            wrap.setContent(cover);

            MimeMultipart content = new MimeMultipart("related");
            message.setContent(content);
            content.addBodyPart(wrap);

            File attachmentFile = null;
            if (attachmentFileStream != null)
            {
                attachmentFile = getLocalFile(attachmentFileName, attachmentFileStream);
                MimeBodyPart attachment = new MimeBodyPart();
                DataSource fds = new FileDataSource(attachmentFile.getAbsolutePath());
                attachment.setDataHandler(new DataHandler(fds));
                String id = UUID.randomUUID().toString();
                attachment.setHeader("Content-ID", "<" + id + ">");
                attachment.setFileName(fds.getName());
                content.addBodyPart(attachment);
            }
            //            html.setContent("<html><body>" + bodyText + "</body></html>", "text/html");
            html.setContent(bodyText, "text/html");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
            //            rawMessageDump(message);

            SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage);
            ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
            ((AwsPersonProvider) pdelayData.provider).inspectThrottle.waitAsNeeded();
            do
            {
                try
                {
                    SendRawEmailResult result = client.sendRawEmail(rawEmailRequest);
                    format.ttl("messageId = ", result.getMessageId());
                    LoggerFactory.getLogger(getClass()).debug(format.toString());
                    break;
                } catch (Exception e)
                {
                    FatalResourceException fre = pdelay.handleException(msg, e);
                    if (fre instanceof FatalException)
                    {
                        format.ttl("messageId = failed to send");
                        LoggerFactory.getLogger(getClass()).debug(format.toString());
                        if(attachmentFile != null)
                            attachmentFile.delete();
                        Thread.currentThread().setName(tname);
                        throw fre;
                    }
                }
            } while (true);
            if(attachmentFile != null)
                attachmentFile.delete();
            Thread.currentThread().setName(tname);
            return null;
        }
    }

    @SuppressWarnings("unused")
    private void rawMessageDump(MimeMessage message) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        String msg = new String(baos.toByteArray());
        LoggerFactory.getLogger(getClass()).debug("\n" + msg);
    }
}