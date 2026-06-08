package com.blombank.cvautomation.repository;

import com.blombank.cvautomation.model.CandidateCV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CVRepository extends JpaRepository<CandidateCV, Long> {

    boolean existsByEmailAddressIgnoreCase(String emailAddress);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByFileNameIgnoreCase(String fileName);
    Optional<CandidateCV> findByEmailAddressIgnoreCase(String emailAddress);
}