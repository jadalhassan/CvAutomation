package com.blombank.cvautomation.repository;

import com.blombank.cvautomation.model.JobTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobTrackRepository extends JpaRepository<JobTrack, Long> {

    Optional<JobTrack> findByNameIgnoreCase(String name);
    Optional<JobTrack> findByFolderCodeIgnoreCase(String folderCode);
}