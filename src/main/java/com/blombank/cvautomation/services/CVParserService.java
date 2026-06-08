package com.blombank.cvautomation.services;

import com.blombank.cvautomation.model.CandidateCV;
import com.blombank.cvautomation.model.JobTrack;
import com.blombank.cvautomation.model.KeywordSet;
import com.blombank.cvautomation.repository.CVRepository;
import com.blombank.cvautomation.repository.JobTrackRepository;
import com.blombank.cvautomation.repository.KeywordSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CVParserService {

    final CVRepository cvRepository;
    final JobTrackRepository jobTrackRepository;
    final KeywordSetRepository keywordSetRepository;

    public CVParserService(CVRepository cvRepository,JobTrackRepository jobTrackRepository, KeywordSetRepository keywordSetRepository){
        this.cvRepository = cvRepository;
        this.jobTrackRepository = jobTrackRepository;
        this.keywordSetRepository = keywordSetRepository;
    }

    public void saveCVFromFile(MultipartFile file,String candidateName,String email, String phone, String jobTrackName, String keywordsSetName) throws Exception {

        return;
    }
}
