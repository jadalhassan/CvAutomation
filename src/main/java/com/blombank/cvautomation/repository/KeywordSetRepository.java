package com.blombank.cvautomation.repository;

import com.blombank.cvautomation.model.KeywordSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordSetRepository extends JpaRepository<KeywordSet,String> {
    
}
