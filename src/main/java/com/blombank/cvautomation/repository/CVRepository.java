package com.blombank.cvautomation.repository;

import com.blombank.cvautomation.model.CandidateCV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CVRepository extends JpaRepository<CandidateCV,Long> {

}
