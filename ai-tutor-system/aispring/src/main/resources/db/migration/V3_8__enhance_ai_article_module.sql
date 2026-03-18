CREATE TABLE IF NOT EXISTS generated_articles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vocabulary_list_id INT NULL,
    topic VARCHAR(255) NULL,
    difficulty_level VARCHAR(50) NULL,
    article_length VARCHAR(50) NULL,
    original_text TEXT NOT NULL,
    translated_text TEXT NULL,
    used_word_ids TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE generated_articles
    ADD COLUMN target_language VARCHAR(20) NOT NULL DEFAULT 'en',
    ADD COLUMN word_count INT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

UPDATE generated_articles
SET is_deleted = 0
WHERE is_deleted IS NULL;

CREATE INDEX idx_generated_articles_user_created
    ON generated_articles(user_id, created_at);

CREATE INDEX idx_generated_articles_language
    ON generated_articles(target_language);

CREATE INDEX idx_generated_articles_deleted
    ON generated_articles(is_deleted);

CREATE TABLE IF NOT EXISTS article_used_words (
    id INT PRIMARY KEY AUTO_INCREMENT,
    article_id INT NOT NULL,
    word_id INT NULL,
    word_text VARCHAR(100) NOT NULL,
    occurrence_count INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE article_used_words
    MODIFY COLUMN word_id INT NULL;

CREATE INDEX idx_article_used_words_article_id
    ON article_used_words(article_id);

CREATE INDEX idx_article_used_words_word_id
    ON article_used_words(word_id);
