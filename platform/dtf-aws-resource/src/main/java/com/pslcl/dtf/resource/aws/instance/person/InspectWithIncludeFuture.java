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

// These are from the JavaMail API, which you can download at https://java.net/projects/javamail/pages/Home. 
// Be sure to include the mail.jar library in your project. In the build order, mail.jar should precede the AWS SDK for Java library.
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
// These are from the AWS SDK for Java, which you can download at https://aws.amazon.com/sdk-for-java.
// Be sure to include the AWS SDK for Java library in your project.
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;

@SuppressWarnings("javadoc")
public class InspectWithIncludeFuture implements Callable<Void>
{
    public static final String SesMidStr = "ses";
    private static final String TempFilePrefix = "dtf-runner-";
    // IMPORTANT: To successfully send an email, you must replace the values of the strings below with your own values.   
    //    private static String EMAIL_FROM = "SENDER@EXAMPLE.COM"; // Replace with the sender's address. This address must be verified with Amazon SES.
    //    private static String EMAIL_REPLY_TO = "REPLY-TO@EXAMPLE.COM"; // Replace with the address replies should go to. This address must be verified with Amazon SES. 
    //    private static String EMAIL_RECIPIENT = "RECIPIENT@EXAMPLE.COM"; // Replace with a recipient address. If your account is still in the sandbox,
    //                                                                     // this address must be verified with Amazon SES.  
    //    private static String EMAIL_ATTACHMENTS = "ATTACHMENT-FILE-NAME-WITH-PATH"; // Replace with the path of an attachment. Must be a valid path or this project will not build.
    //                                                                                // Remember to use two slashes in place of each slash.
    //
    //    // IMPORTANT: Ensure that the region selected below is the one in which your identities are verified.  
    //    private static Regions AWS_REGION = Regions.US_WEST_2; // Choose the AWS region of the Amazon SES endpoint you want to connect to. Note that your sandbox 
    //                                                           // status, sending limits, and Amazon SES identity-related settings are specific to a given AWS 
    //                                                           // region, so be sure to select an AWS region in which you set up Amazon SES. Here, we are using 
    //                                                           // the US West (Oregon) region. Examples of other regions that Amazon SES supports are US_EAST_1 
    //                                                           // and EU_WEST_1. For a complete list, see http://docs.aws.amazon.com/ses/latest/DeveloperGuide/regions.html 
    //
    //    private static String EMAIL_SUBJECT = "Amazon SES email test";
    //    private static String EMAIL_BODY_TEXT = "This MIME email was sent through Amazon SES using SendRawEmail.";

    //    private final String sender;
    //    private final String reply;
    //    private final String recipient;
    //    private final Region region;
    //    private final String subject;
    private final AmazonSimpleEmailServiceClient client;
    private final PersonConfigData config;
    private final String includeFileName;
    private final InputStream includeFileStream;
    private final String recipient;
    private final String bodyText;
    private final ProgressiveDelayData pdelayData;
//    private final String msg;
    private final File tmpFile;
    private final TabToLevel format;

    //@formatter:off
    public InspectWithIncludeFuture(
                    AmazonSimpleEmailServiceClient client, 
                    PersonConfigData config, 
                    String recipient, 
                    String fileName,
                    InputStream fileStream,
                    String bodyText,
                    ProgressiveDelayData pdelayData) throws IOException
    //@formatter:on
    {
        this.client = client;
        this.config = config;
        this.includeFileName = fileName;
        this.includeFileStream = fileStream;
        this.recipient = recipient;
        this.bodyText = bodyText;
        this.pdelayData = pdelayData;
        tmpFile = File.createTempFile(TempFilePrefix+fileName, ".gz");
        format = new TabToLevel();
        format.ttl(getClass().getSimpleName());
        format.level.incrementAndGet();
        format.ttl("sender = ", config.sender);
        format.ttl("reply = ", config.reply);
        format.ttl("recipient = ", recipient);
        format.ttl("tmpFile = ", tmpFile.getAbsolutePath());
        format.level.decrementAndGet();
    }

    private File getFilePath(String fileName, InputStream inputStream) throws IOException
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(tmpFile);
            byte[] raw = new byte[4096];
            int size = -1;
            do
            {
                size = inputStream.read(raw);
                if(size != -1)
                    fos.write(raw, 0, size);
            }while(size != -1);
            fos.flush();
            fos.close();
        } finally
        {
            try
            {
                if (fos != null){fos.close();}
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
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setSubject(config.subject, "UTF-8");

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

        File attachmentFile = getFilePath(includeFileName, includeFileStream);
        String[] attachmentsFiles = new String[] { attachmentFile.getAbsolutePath() };

        // This is just for testing HTML embedding of different type of attachments.
        StringBuilder sb = new StringBuilder();

        for (String attachmentFileName : attachmentsFiles)
        {
            String id = UUID.randomUUID().toString();
            sb.append("<img src=\"cid:");
            sb.append(id);
            sb.append("\" alt=\"ATTACHMENT\"/>\n");

            MimeBodyPart attachment = new MimeBodyPart();

            DataSource fds = new FileDataSource(attachmentFileName);
            attachment.setDataHandler(new DataHandler(fds));
            attachment.setHeader("Content-ID", "<" + id + ">");
            attachment.setFileName(fds.getName());

            content.addBodyPart(attachment);
        }

        html.setContent("<html><body><h1>HTML</h1>\n" + bodyText + "</body></html>", "text/html");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

        SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage);
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(SesMidStr, "sendRawEmail");
        format.ttl(msg);
        LoggerFactory.getLogger(getClass()).debug(format.toString());
        do
        {
            try
            {
                client.sendRawEmail(rawEmailRequest);
                break;
            }catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if(fre instanceof FatalException)
                {
                    tmpFile.delete();
                    throw fre;
                }
            }
        }while(true);
        tmpFile.delete();
        return null;
    }
}