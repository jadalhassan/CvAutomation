package com.blombank.cvautomation.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Service
public class EmailListenerService {

    private static final Logger logger = LoggerFactory.getLogger(EmailListenerService.class);

    private final CVParserService cvParserService;

    @Value("${app.mail.imap.host}")
    private String imapHost;

    @Value("${app.mail.imap.port}")
    private String imapPort;

    @Value("${app.mail.username}")
    private String mailUsername;

    @Value("${app.mail.password}")
    private String mailPassword;

    @Value("${app.mail.expected-sender}")
    private String expectedSender;

    @Value("${app.mail.expected-recipient}")
    private String expectedRecipient;

    @Value("${app.mail.expected-subject}")
    private String expectedSubject;

    @Value("${app.paths.security-folder}")
    private String securityFolder;

    public EmailListenerService(CVParserService cvParserService) {
        this.cvParserService = cvParserService;
    }

    public void processInbox() {
        logger.info("Starting email inbox scan...");

        if (!hasMailboxCredentials()) {
            logger.warn("Mailbox credentials are not configured. Set APP_MAIL_USERNAME and APP_MAIL_PASSWORD to enable inbox scanning.");
            logger.info("Email inbox scan completed.");
            return;
        }

        Properties props =  buildImapProperties();
        Session session = Session.getInstance(props);

        try(Store store = session.getStore("imaps")){
            store.connect(imapHost,mailUsername, mailPassword);
            logger.info("Connected to mailbox: {}", mailUsername);

            try(Folder inbox = store.getFolder("INBOX")){
                inbox.open(Folder.READ_WRITE);

                Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN),false));
                logger.info("Found {} unread message(s) in inbox.", messages.length);

                for(Message message : messages){
                    try{
                        processMessage(message);
                    } catch(Exception e){
                        logger.error("Error processing message — skipping. Subject: {} | Error: {}",
                                getSubjectSafe(message), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e){
            logger.error("Failed to connect to mailbox {}: {}", mailUsername, e.getMessage(), e);
        }
        logger.info("Email inbox scan completed.");
    }

    private boolean hasMailboxCredentials() {
        return hasText(mailUsername)
                && hasText(mailPassword)
                && !mailUsername.startsWith("your-")
                && !mailPassword.startsWith("your-");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void processMessage(Message message) throws MessagingException, IOException {
        String subject = getSubjectSafe(message);
        String sender = getSenderAddress(message);
        String recipient = getRecipientAddress(message);

        logger.info("Evaluating message — From: {} | To: {} | Subject: {}", sender, recipient, subject);

        if(!matchesSender(sender)){
            logger.info("Skipping — sender does not match. Expected: {} | Got: {}", expectedSender, sender);
            markSeen(message);
            return;
        }

        if(!matchesRecipient(recipient)){
            logger.info("Skipping — recipient does not match. Expected: {} | Got: {}", expectedRecipient, recipient);
            markSeen(message);
            return;
        }

        if(!matchesSubject(subject)){
            logger.info("Skipping — subject does not match. Expected: {} | Got: {}", expectedSubject, subject);
            markSeen(message);
            return;
        }

        logger.info("Email matched filters. Processing attachments...");

        boolean hasZip = false;

        if(message.getContent() instanceof Multipart multipart){
            for(int i=0; i<multipart.getCount(); i++){
                BodyPart bodyPart = multipart.getBodyPart(i);
                String disposition = bodyPart.getDisposition();
                String partFileName = bodyPart.getFileName();

                if (partFileName != null && partFileName.toLowerCase().endsWith(".zip") && (Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition))) {
                    hasZip = true;
                    File zipFile = downloadAttachment(bodyPart,partFileName);

                    if (zipFile != null){
                        logger.info("Downloaded ZIP attachment: {}", zipFile.getAbsolutePath());
                        cvParserService.processZip(zipFile);
                    }
                }
            }
        }

        if (!hasZip) {
            logger.warn("Matched email has no ZIP attachment. Subject: {}", subject);
        }
        markSeen(message);
    }

    private File downloadAttachment(BodyPart part, String fileName) {
        File securityDir = new File(securityFolder);
        if (!securityDir.exists() && !securityDir.mkdirs()) {
            logger.warn("Could not create security folder: {}", securityDir.getAbsolutePath());
        }

        String dateSuffix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String safeFileName = dateSuffix +"_"+ sanitizeFileName(fileName);
        File destination =  new File(securityDir, safeFileName);

        try(InputStream is = part.getInputStream();
        FileOutputStream fos = new FileOutputStream(destination)){

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }

            logger.info("Saved ZIP to security folder: {}", destination.getAbsolutePath());
            return destination;

        } catch(Exception e){
            logger.error("Failed to download attachment {}: {}", fileName, e.getMessage(), e);
            return null;
        }
    }

    private boolean matchesSender(String sender) {
        if(sender==null || sender.isEmpty()){
            return false;
        }
        return sender.toLowerCase().contains(expectedSender.toLowerCase());
    }

    private boolean matchesRecipient(String recipient) {
        if(recipient==null || recipient.isEmpty()){
            return false;
        }
        return recipient.toLowerCase().contains(expectedRecipient.toLowerCase());
    }

    private boolean matchesSubject(String subject) {
        if(subject==null || subject.isEmpty()){
            return false;
        }
        return subject.toLowerCase().contains(expectedSubject.toLowerCase());
    }

    private String getSenderAddress(Message message) {
        try{
            Address[] from = message.getFrom();
            if(from!=null && from.length>0){
                return ((InternetAddress)from[0]).getAddress();
            }
        } catch(MessagingException e){
            logger.warn("Could not read sender address: {}", e.getMessage());
        }
        return null;
    }

    private String getRecipientAddress(Message message) {
        try{
            Address[] to = message.getRecipients(Message.RecipientType.TO);
            if(to!=null && to.length>0){
                return ((InternetAddress)to[0]).getAddress();
            }
        } catch(MessagingException e){
            logger.warn("Could not read recipient address: {}", e.getMessage());
        }
        return null;
    }

    private String getSubjectSafe(Message message) {
        try{
            return message.getSubject();
        } catch(MessagingException e){
            return"[unreadable subject]";
        }
    }

    private void markSeen(Message message) {
        try{
            message.setFlag(Flags.Flag.SEEN, true);
        } catch(MessagingException e){
            logger.warn("Could not mark message as seen: {}", e.getMessage());
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private Properties buildImapProperties() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "10000");
        props.put("mail.imaps.connectiontimeout", "10000");
        return props;
    }
}
