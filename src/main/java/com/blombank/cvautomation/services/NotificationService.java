package com.blombank.cvautomation.services;

import org.springframework.stereotype.Service;
import com.blombank.cvautomation.model.CandidateCV;
import com.blombank.cvautomation.repository.CVRepository;

@Service
public class NotificationService {

    private final CVRepository cvRepository;

    public NotificationService(CVRepository cvRepository) {
        this.cvRepository = cvRepository;
    }

    public void notifyHR(){
        return;
    }
}
