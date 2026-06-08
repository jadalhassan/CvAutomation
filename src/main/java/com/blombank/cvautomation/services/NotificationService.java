package com.blombank.cvautomation.services;

import com.blombank.cvautomation.model.CandidateCV;
import com.blombank.cvautomation.repository.CVRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final CVRepository cvRepository;
    private final JavaMailSender mailSender;

    @Value("{app.mail.hr-recipient}")
    private String hrRecipient;

    public NotificationService(CVRepository cvRepository, JavaMailSender mailSender) {
        this.cvRepository = cvRepository;
        this.mailSender = mailSender;
    }

    public void handleCandidate(CandidateCV candidate, File cvFile) {
        if (isDuplicate(candidate)) {
            logger.warn("Duplicate candidate detected — skipping save. File: {}, Email: {}, Phone: {}",
                    candidate.getFileName(),
                    candidate.getEmailAddress(),
                    candidate.getPhoneNumber());
            candidate.setDuplicateStatus(true);
            return;
        }

        try{
            candidate.setProcessedAt(LocalDateTime.now());
            candidate.setDuplicateStatus(false);
            cvRepository.save(candidate);
            logger.info("Saved candidate: {} | Track: {} | File: {}",
                    candidate.getCandidateName(),
                    candidate.getTrack(),
                    candidate.getFileName());

        } catch (Exception e){
            logger.error("Failed to save candidate {} to database: {}", candidate.getFileName(), e.getMessage(), e);
            return;
        }

        openFileForReview(cvFile);
        sendHrEmail(candidate);
    }

    private boolean isDuplicate(CandidateCV candidate) {
        if (candidate.getEmailAddress() != null && !candidate.getEmailAddress().isBlank()) {
            if (cvRepository.existsByEmailAddressIgnoreCase(candidate.getEmailAddress())) {
                logger.warn("Duplicate by email: {}", candidate.getEmailAddress());
                return true;
            }
        }
        if (candidate.getPhoneNumber() != null && !candidate.getPhoneNumber().isBlank()) {
            if (cvRepository.existsByPhoneNumber(candidate.getPhoneNumber())) {
                logger.warn("Duplicate by phone: {}", candidate.getPhoneNumber());
                return true;
            }
        }

        if (candidate.getFileName() != null && !candidate.getFileName().isBlank()) {
            if (cvRepository.existsByFileNameIgnoreCase(candidate.getFileName())) {
                logger.warn("Duplicate by filename: {}", candidate.getFileName());
                return true;
            }
        }

        return false;
    }

    private void openFileForReview(File cvFile) {

        if (cvFile == null || !cvFile.exists()) {
            logger.warn("CV file not found for desktop open: {}", cvFile);
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            logger.info("Desktop not supported on this environment — skipping file open for: {}", cvFile.getName());
            return;
        }

        try{

            Desktop.getDesktop().open(cvFile);
            logger.info("Opened CV for HR review: {}", cvFile.getName());
        } catch (IOException e){
            logger.error("Failed to open CV file {}: {}", cvFile.getName(), e.getMessage(), e);
        }

    }

    private void sendHrEmail(CandidateCV candidate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(hrRecipient);
            message.setSubject("New Matched CV — " + candidate.getCandidateName() + " [" + candidate.getTrack() + "]");
            message.setText(buildEmailBody(candidate));
            mailSender.send(message);
            logger.info("HR notification email sent for candidate: {}", candidate.getCandidateName());

        } catch (Exception e) {
            logger.error("Failed to send HR email for candidate {}: {}", candidate.getCandidateName(), e.getMessage(), e);
        }

    }

    private String buildEmailBody(CandidateCV candidate) {
        return "A new CV has been processed and matched.\n\n"
                + "Candidate Name : " + candidate.getCandidateName() + "\n"
                + "Email Address  : " + candidate.getEmailAddress() + "\n"
                + "Phone Number   : " + candidate.getPhoneNumber() + "\n"
                + "Track          : " + candidate.getTrack() + "\n"
                + "File Name      : " + candidate.getFileName() + "\n"
                + "Matched On     : " + candidate.getProcessedAt() + "\n\n"
                + "Matched Keywords:\n" + candidate.getMatchedKeywords() + "\n\n"
                + "Please review the CV that has been opened on your workstation.";
    }

    public File writeTempFile(CandidateCV candidate){
        if(candidate.getCvFile() == null || candidate.getCvFile().length==0){
            return null;
        }

        try{
            File tempFile = File.createTempFile("cv_review_", "_" + candidate.getFileName());
            tempFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(candidate.getCvFile());
            }
            return tempFile;
        } catch (IOException e){
            logger.error("Failed to write temp file for {}: {}", candidate.getFileName(), e.getMessage(), e);
            return null;
        }
    }

}

