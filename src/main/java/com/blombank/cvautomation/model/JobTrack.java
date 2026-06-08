package com.blombank.cvautomation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_track")
@Getter
@Setter
@NoArgsConstructor
public class JobTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "folder_code", nullable = false, unique = true)
    private String folderCode;
}