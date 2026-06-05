package com.blombank.cvautomation.repository;

import com.blombank.cvautomation.model.JobTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobTrackRepository extends JpaRepository<JobTrack, String>{

}
