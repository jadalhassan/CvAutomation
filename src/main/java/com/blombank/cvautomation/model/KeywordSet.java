package com.blombank.cvautomation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "keyword_set")
@Getter
@Setter
@NoArgsConstructor
public class KeywordSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    @Column(name = "track", nullable = false)
    private String track;
}