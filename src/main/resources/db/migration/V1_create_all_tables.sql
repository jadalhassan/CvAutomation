-- 1. Create job_track table
CREATE TABLE job_track(
     name VARCHAR(100) PRIMARY KEY
);

-- 2. Create keyword_set table
CREATE TABLE keyword_set(
    name VARCHAR(100) PRIMARY KEY,
    keywords VARCHAR(1000)
);

-- 3. Create candidate_cv table
CREATE TABLE candidate_cv(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    candidate_name VARCHAR (255),
    email VARCHAR (255),
    phone VARCHAR (50),
    job_track VARCHAR(100),
    keyword_set VARCHAR(100),
    filename VARCHAR(255),
    cv_data BLOB,
    FOREIGN KEY (job_track) REFERENCES job_track(name),
    FOREIGN KEY (keyword_set) REFERENCES keyword_set(name)
);