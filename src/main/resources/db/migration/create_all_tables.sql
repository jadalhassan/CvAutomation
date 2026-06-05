-- 1. Create job_track table
CREATE job_track(
     name VARCHAR(100) PRIMARY KEY
);

-- 2. Create keyword_set table
CREATE keyword_set(
    name VARCHAR(100) PRIMARY KEY
    keywords VARCHAR(1000)
);

-- 3. Create candidate_cv table
CREATE candidate_cv(
    id BIGINT PRIMARY KEY AUTO_INTCREMENT
    candinate_name VARCHAR (255)
    email VARCHAR (225)
    phone VARCHAR (50)
    job_track VARCHAR(100)
    keyword_track VARCHAR(100)
    filename VARCHAR(255)
    cv_date BlOB
    FOREIGN KEY (job_track) REFERENCES job_track(name)
    FOREIGN KEY (keyword_track) REFERENCES keyword_set(name)
);