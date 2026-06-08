-- ─── candidate_cv ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS candidate_cv (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_name   VARCHAR(255),
    phone_number     VARCHAR(50),
    email_address    VARCHAR(255),
    matched_keywords TEXT,
    track            VARCHAR(50),
    file_name        VARCHAR(255),
    cv_file          BLOB,
    processed_at     TIMESTAMP,
    duplicate_status BOOLEAN
);

-- ─── job_track ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS job_track (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    folder_code VARCHAR(50)  NOT NULL UNIQUE
    );

-- ─── keyword_set ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS keyword_set (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    track   VARCHAR(50)  NOT NULL
);
