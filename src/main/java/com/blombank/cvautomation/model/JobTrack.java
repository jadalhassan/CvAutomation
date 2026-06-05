package com.blombank.cvautomation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="job_track")

public class JobTrack {

    @Id
    private String name;

    public JobTrack(){}

    public JobTrack(String name){
        this.name=name;
    }

}
