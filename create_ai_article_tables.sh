#!/bin/bash
# ==========================================
# 创建AI文章相关表结构
# 功能：创建AI生成文章相关的数据库表
# ==========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "📚 创建AI文章相关表结构"
echo "=========================================="

# 数据库配置
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-aispring}"
DB_USERNAME="${DB_USERNAME:-aispring}"
DB_PASSWORD="${DB_PASSWORD:-xGDswMCdHhsajfxF}"

echo ""
echo " 数据库配置:"
echo "   主机：$DB_HOST:$DB_PORT"
echo "   数据库：$DB_NAME"
echo "   用户：$DB_USERNAME"
echo ""

# 验证数据库连接
echo "🔌 正在连接 MySQL 数据库..."
if ! mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" &>/dev/null; then
    echo -e "${RED}❌ 数据库连接失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 数据库连接成功${NC}"
echo ""

# 创建AI生成文章表
echo "📋 正在创建AI生成文章表..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" <<EOF
-- 创建AI生成文章表
CREATE TABLE IF NOT EXISTS generated_articles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vocabulary_list_id INT,
    topic VARCHAR(200) NOT NULL,
    difficulty_level VARCHAR(50),
    article_length VARCHAR(20),
    target_language VARCHAR(10) NOT NULL DEFAULT 'en',
    original_text TEXT NOT NULL,
    translated_text TEXT,
    used_word_ids TEXT,
    word_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_deleted TINYINT(1) DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_topic (topic),
    INDEX idx_language (target_language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建文章使用单词表
CREATE TABLE IF NOT EXISTS article_used_words (
    id INT AUTO_INCREMENT PRIMARY KEY,
    article_id INT NOT NULL,
    word_id INT,
    word_text VARCHAR(100) NOT NULL,
    occurrence_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_article_id (article_id),
    INDEX idx_word_id (word_id),
    INDEX idx_word_text (word_text),
    FOREIGN KEY (article_id) REFERENCES generated_articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
EOF

echo -e "${GREEN}✅ AI文章相关表创建完成${NC}"
echo ""

# 检查表结构
echo "🔍 检查表结构..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
SHOW TABLES LIKE '%article%';
" 2>/dev/null || echo "   暂无数据"

echo ""
echo "📊 表结构详情:"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
SELECT 
    TABLE_NAME as '表名',
    TABLE_ROWS as '记录数',
    CREATE_TIME as '创建时间'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = '$DB_NAME' 
AND TABLE_NAME LIKE '%article%'
ORDER BY TABLE_NAME;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo -e "${GREEN}✅ AI文章表结构创建完成${NC}"
echo ""
echo "💡 提示：现在AI文章历史列表接口应该可以正常工作了"
