package com.blombank.cvautomation.repository;

import com.blombank.cvautomation.model.KeywordSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeywordSetRepository extends JpaRepository<KeywordSet, Long> {

    List<KeywordSet> findByTrackIgnoreCase(String track);
    List<KeywordSet> findByTrackIgnoreCaseOrTrackIgnoreCase(String track, String common);
    boolean existsByKeywordIgnoreCaseAndTrackIgnoreCase(String keyword, String track);
}