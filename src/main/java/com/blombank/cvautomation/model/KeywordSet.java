package com.blombank.cvautomation.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Entity
@Table(name="keyword_set")

public class KeywordSet {
    @Id
    private String name;

    @ElementCollection
    private List<String> keywords;

    public KeywordSet(){}

    public KeywordSet(String name,List<String> keywords){
        this.name=name;
        this.keywords=keywords;
    }

}
