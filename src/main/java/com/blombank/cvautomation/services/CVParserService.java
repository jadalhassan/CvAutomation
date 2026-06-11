package com.blombank.cvautomation.services;

import com.blombank.cvautomation.model.CandidateCV;
import com.blombank.cvautomation.model.KeywordSet;
import com.blombank.cvautomation.repository.KeywordSetRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Service
public class CVParserService {

    private static final Logger logger = LoggerFactory.getLogger(CVParserService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+961[3-9]\\d{7}|[3-9]\\d{7})$");

    private static final Pattern SKIP_LINE_PATTERN = Pattern.compile("\"(?i)(email|phone|linkedin|github|address|cv|curriculum vitae|@|http|www|\\\\d{5,})");

    private static final List<String> DEFAULT_KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring", "Hibernate", "JPA", "REST API", "API",
            "Maven", "Gradle", "SQL", "MySQL", "PostgreSQL", "Oracle", "H2", "Git",
            "GitHub", "Docker", "Kubernetes", "Microservices", "HTML", "CSS",
            "JavaScript", "TypeScript", "React", "Angular", "Vue", "Node.js",
            "Express", "Python", "Django", "Flask", "C++", "C#", ".NET", "PHP",
            "Laravel", "Android", "Kotlin", "Swift", "iOS", "Flutter", "Dart",
            "React Native", "Mobile Development", "Firebase", "Linux",
            "Windows Server", "Networking", "Cybersecurity", "Security", "Testing",
            "JUnit", "Mockito", "Selenium", "Agile", "Scrum", "Jira",
            "Data Analysis", "Excel", "Power BI", "Machine Learning", "AI",
            "SQL Server", "PL/SQL");

    private final KeywordSetRepository keywordSetRepository;
    private final NotificationService notificationService;


    @Value("${app.paths.base-cv-folder}")
    private String baseCvFolder;

    @Value("${app.paths.invalid-zip-folder}")
    private String invalidZipFolder;

    public CVParserService(KeywordSetRepository keywordSetRepository, NotificationService notificationService) {
        this.keywordSetRepository = keywordSetRepository;
        this.notificationService = notificationService;
    }

    public void processZip(File zipFile) {
        logger.info("Processing ZIP: {}", zipFile.getName());

        if(!isValidZip(zipFile)) {
            logger.error("Invalid or corrupted ZIP — moving to invalid folder: {}", zipFile.getName());
            moveToInvalidFolder(zipFile);
            return;
        }

        String track = resolveTrack(zipFile.getName());
        logger.info("Resolved track: {} for ZIP: {}", track, zipFile.getName());

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File trackFolder = new File(baseCvFolder + File.separator + today+ File.separator+ track);
        File unmatchedFolder = new File(baseCvFolder + File.separator + today+ File.separator+ "unmatched");
        File unsupportedFolder = new File(baseCvFolder + File.separator + today+ File.separator+ "unsupported");

        ensureFolder(trackFolder);
        ensureFolder(unmatchedFolder);
        ensureFolder(unsupportedFolder);

        List<KeywordSet> keywords = loadKeywords(track);

        try(ZipFile zip = new ZipFile(zipFile)) {

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (!entry.isDirectory()) {
                    continue;
                }

                String entryName = new File(entry.getName()).getName();
                String lowerName = entryName.toLowerCase();

                if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx") && !lowerName.endsWith(".doc")) {
                    logger.warn("Unsupported file type — moving to unsupported: {}", entryName);
                    extractEntryToFolder(zip, entry, unsupportedFolder, entryName);
                    continue;
                }

                File extractedFile = extractEntryToFolder(zip, entry, trackFolder, entryName);

                if (extractedFile == null) {
                    logger.error("Failed to extract entry: {}", entryName);
                    continue;
                }
                try {
                    processCvFile(extractedFile, track, keywords, unmatchedFolder);
                } catch (Exception e) {
                    logger.error("Error processing CV file {}: {}", entryName, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to read ZIP file {}: {}", zipFile.getName(), e.getMessage(), e);
        }
    }

    private void processCvFile(File cvFile, String track, List<KeywordSet> keywords, File unmatchedFolder) {
        String text = extractText(cvFile);

        if (text == null ||  text.isBlank()) {
            logger.warn("No text extracted from: {}", cvFile.getName());
            moveFile(cvFile, unmatchedFolder);
            return;
        }

        List<String> matched = matchKeywords(text, keywords);

        if (matched.isEmpty()) {
            logger.info("No keywords matched in: {} — moving to unmatched.", cvFile.getName());
            moveFile(cvFile, unmatchedFolder);
            return;
        }

        logger.info("Matched {} keyword(s) in: {}", matched.size(), cvFile.getName());

        CandidateCV candidateCV = new CandidateCV();
        candidateCV.setTrack(track);
        candidateCV.setFileName(cvFile.getName());
        candidateCV.setMatchedKeywords(String.join(", ", matched));
        candidateCV.setEmailAddress(extractEmail(text));
        candidateCV.setPhoneNumber(extractPhone(text));
        candidateCV.setCandidateName(extractName(text, cvFile.getName()));

        try{
            candidateCV.setCvFile(Files.readAllBytes(cvFile.toPath()));
        } catch (IOException e){
            logger.error("Could not read CV bytes for {}: {}", cvFile.getName(), e.getMessage(), e);
        }

        File tempFile =notificationService.writeTempFile(candidateCV);
        notificationService.handleCandidate(candidateCV,tempFile !=null ? tempFile : cvFile);
    }

    private String extractText(File file) {
        String name = file.getName().toLowerCase();
        try{
            if(name.endsWith(".pdf")) {
                return extractPdf(file);
            } else if(name.endsWith(".docx")) {
                return extractDocx(file);
            } else if(name.endsWith(".doc")) {
                return extractDoc(file);
            }
        } catch (Exception e) {
            logger.error("Text extraction failed for {}: {}", file.getName(), e.getMessage(), e);
        }
        return null;
    }

    private String extractPdf(File file) throws IOException {
        try(PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractDocx(File file) throws IOException {
        try(FileInputStream fis = new FileInputStream(file);XWPFDocument doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                sb.append(paragraph.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    private String extractDoc(File file) throws IOException {
        try(FileInputStream fis = new FileInputStream(file);
            HWPFDocument doc = new HWPFDocument(fis);
            WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private List<String> matchKeywords(String text, List<KeywordSet> keywords) {
        List<String> matched = new ArrayList<>();
        String lowerText = text.toLowerCase();
        for (KeywordSet keyword : keywords) {
            if (lowerText.contains(keyword.getKeyword().toLowerCase())) {
                matched.add(keyword.getKeyword());
            }
        }
        return matched;
    }

    private List<KeywordSet> loadKeywords(String track) {
        List<KeywordSet> keywords = keywordSetRepository.findByTrackIgnoreCaseOrTrackIgnoreCase(track,"common");

        if(keywords.isEmpty()) {
            logger.warn("No keywords found in database — using default keyword list.");
            keywords = buildDefaultKeywords(track);
        }

        logger.info("Loaded {} keyword(s) for track: {}", keywords.size(), track);
        return keywords;
    }

    private List<KeywordSet> buildDefaultKeywords(String track) {
        List<KeywordSet> keywords = new ArrayList<>();
        for (String kw : DEFAULT_KEYWORDS) {
            KeywordSet ks = new KeywordSet();
            ks.setKeyword(kw);
            ks.setTrack(track);
            keywords.add(ks);
        }
        return keywords;
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractName(String text, String fileName) {
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {continue;}
            if (trimmed.length()<3) {continue;}
            if (SKIP_LINE_PATTERN.matcher(trimmed).find()) {continue;}
            if (trimmed.split("\\s+").length>6) {continue;}

            boolean hasLetter = trimmed.chars().anyMatch(Character::isLetter);
            if(!hasLetter) {continue;}

            return trimmed;
        }
        String fallback = fileName;
        int dot = fallback.lastIndexOf('.');
        if (dot > 0) {
            fallback = fallback.substring(0, dot);
        }
        logger.info("No name found in CV text — using filename as fallback: {}", fallback);
        return fallback;
    }

    private String resolveTrack(String zipFileName) {
        String lower = zipFileName.toLowerCase();
        if (lower.contains("springboot") || lower.contains("spring boot") || lower.contains("sb")) {
            return "sb";
        } else if (lower.contains("mobile") || lower.contains("mb")) {
            return "mb";
        }
        return "unknown";
    }

    private boolean isValidZip(File file) {
        try(ZipFile  zipFile = new ZipFile(file)) {
            return zipFile.size() > 0;
        } catch (ZipException e) {
            logger.error("ZIP validation failed — corrupted file: {}", file.getName());
            return false;
        }
        catch (IOException e){
            logger.error("ZIP validation IO error for {}: {}", file.getName(), e.getMessage());
            return false;
        }
    }

    private File extractEntryToFolder(ZipFile zip, ZipEntry entry, File targetFolder, String fileName) {
        ensureFolder(targetFolder);
        File destFolder = new File(targetFolder, fileName);

        try(InputStream is = zip.getInputStream(entry);
        FileOutputStream fos = new FileOutputStream(destFolder)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            return destFolder;

        } catch(IOException e){
            logger.error("Failed to extract {} to {}: {}", fileName, targetFolder.getPath(), e.getMessage(), e);
            return null;
        }
    }

    private void moveFile(File file, File targetFolder) {
        ensureFolder(targetFolder);
        File destination = new File (targetFolder,file.getName());
        try {
            Files.move(file.toPath(), destination.toPath(),StandardCopyOption.REPLACE_EXISTING);
            logger.info("Moved {} to {}", file.getName(), targetFolder.getName());
        } catch (IOException e) {
            logger.error("Failed to move file {} to {}: {}", file.getName(), targetFolder.getPath(), e.getMessage(), e);

        }
    }

    private void moveToInvalidFolder(File zipFile) {
        File invalidDir = new  File(invalidZipFolder);
        ensureFolder(invalidDir);
        File destDir = new File(invalidDir, zipFile.getName());
        try{
            Files.move(zipFile.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Moved invalid ZIP to: {}", destDir.getAbsolutePath());
        } catch(IOException e){
            logger.error("Failed to move invalid ZIP {}: {}", zipFile.getName(), e.getMessage(), e);
        }
    }

    private void ensureFolder(File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warn("Could not create folder: {}", folder.getAbsolutePath());
        }
    }

}