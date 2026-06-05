package com.blombank.cvautomation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="candidate_cv")

public class CandidateCV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String candidateName;
    private String email;
    private String phone;
    private String filename;

    @Lob
    private byte[] cvData;

    @ManyToOne
    @JoinColumn(name="job_track")
    private JobTrack jobTrack;

    @ManyToOne
    @JoinColumn(name="keyword_set")
    private KeywordSet keywordSet;

    public CandidateCV(){}

    public CandidateCV(String candidateName,String email,String phone,String filename,byte[] cvData,JobTrack jobTrack,KeywordSet keywordSet){
        this.candidateName = candidateName;
        this.email = email;
        this.phone = phone;
        this.filename = filename;
        this.cvData = cvData;
        this.jobTrack = jobTrack;
        this.keywordSet = keywordSet;
    }
}
