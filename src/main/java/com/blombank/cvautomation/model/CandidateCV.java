package com.blombank.cvautomation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name ="candidate_cv")
@Getter
@Setter
@NoArgsConstructor
public class CandidateCV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "matched_keywords", columnDefinition = "TEXT")
    private String matchedKeywords;

    @Column(name = "track")
    private String track;

    @Column(name = "file_name")
    private String fileName;

    @Lob
    @Column(name = "cv_file", columnDefinition = "BLOB")
    private byte[] cvFile;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "duplicate_status")
    private Boolean duplicateStatus;
}