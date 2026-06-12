package com.blombank.cvautomation.services;

import com.blombank.cvautomation.model.CandidateCV;
import com.blombank.cvautomation.repository.CVRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final CVRepository cvRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.hr-recipient}")
    private String hrRecipient;

    @Value("${app.viewer.chrome-path:C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe}")
    private String chromePath;

    @Value("${app.viewer.word-path:C:\\Program Files\\Microsoft Office\\root\\Office16\\WINWORD.EXE}")
    private String wordPath;

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
            cvRepository.findByEmailAddressIgnoreCase(candidate.getEmailAddress()).ifPresent(original ->
                    logger.warn("Duplicate by email: {} — already processed as '{}' on {}",
                            candidate.getEmailAddress(),
                            original.getFileName(),
                            original.getProcessedAt())
            );
            if (cvRepository.existsByEmailAddressIgnoreCase(candidate.getEmailAddress())) {
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
            logger.warn("CV file not found for review open: {}", cvFile);
            return;
        }

        String name = cvFile.getName().toLowerCase();

        try {
            if (name.endsWith(".pdf")) {
                openWithChrome(cvFile);
            } else if (name.endsWith(".docx") || name.endsWith(".doc")) {
                openWithWord(cvFile);
            } else {
                logger.warn("No targeted viewer for file type — skipping open: {}", cvFile.getName());
            }
        } catch (Exception e) {
            logger.error("Failed to open CV file {}: {}", cvFile.getName(), e.getMessage(), e);
        }
    }

    private void openWithChrome(File file) throws IOException {
        File chrome = new File(chromePath);
        if (!chrome.exists()) {
            logger.warn("Chrome not found at '{}' — skipping PDF open for: {}", chromePath, file.getName());
            return;
        }
        new ProcessBuilder(chromePath, file.getAbsolutePath())
                .inheritIO()
                .start();
        logger.info("Opened PDF in Chrome: {}", file.getName());
    }

    private void openWithWord(File file) throws IOException {
        File word = new File(wordPath);
        if (!word.exists()) {
            logger.warn("Word not found at '{}' — skipping DOCX open for: {}", wordPath, file.getName());
            return;
        }
        new ProcessBuilder(wordPath, file.getAbsolutePath())
                .inheritIO()
                .start();
        logger.info("Opened DOCX in Word: {}", file.getName());
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

