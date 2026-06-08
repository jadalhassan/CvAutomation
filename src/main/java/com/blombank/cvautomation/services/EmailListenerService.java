package com.blombank.cvautomation.services;

import com.blombank.cvautomation.repository.CVRepository;
import com.blombank.cvautomation.services.CVParserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmailListenerService {

    private final CVParserService cvParserService;

    public EmailListenerService(CVParserService cvParserService) {
        this.cvParserService= cvParserService;
    }

    public void ProcessCV(MultipartFile file,String candidateName,String email, String phone, String jobTrackName, String keywordsSetName) {
        return;
    }
}
