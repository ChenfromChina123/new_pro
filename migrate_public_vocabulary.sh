#!/bin/bash
# ==========================================
# 公共词库迁移脚本
# 功能：将本地的公共词库数据迁移到 MySQL 数据库
# ==========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "📚 公共词库迁移工具"
echo "=========================================="

# 数据库配置
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-aispring}"
DB_USERNAME="${DB_USERNAME:-aispring}"
DB_PASSWORD="${DB_PASSWORD:-xGDswMCdHhsajfxF}"

echo ""
echo "📋 数据库配置:"
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

# 检查 SQLite 数据库文件
SQLITE_DB=""
if [ -f "ai-tutor-system/aispring/database.db" ]; then
    SQLITE_DB="ai-tutor-system/aispring/database.db"
elif [ -f "database.db" ]; then
    SQLITE_DB="database.db"
fi

# 检查表结构并创建
echo "📋 检查并创建公共词库表结构..."

# 创建 public_vocabulary_words 表
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" <<EOF
CREATE TABLE IF NOT EXISTS public_vocabulary_words (
    id INT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    definition TEXT NOT NULL,
    part_of_speech VARCHAR(50) NOT NULL,
    example TEXT,
    tag VARCHAR(50),
    usage_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_word (word),
    INDEX idx_language (language),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vocabulary_lists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    category VARCHAR(50),
    is_public TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT,
    INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vocabulary_words (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vocabulary_list_id INT NOT NULL,
    word VARCHAR(100) NOT NULL,
    definition TEXT NOT NULL,
    part_of_speech VARCHAR(50) NOT NULL,
    example TEXT,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_list_id (vocabulary_list_id),
    INDEX idx_word (word),
    INDEX idx_language (language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
EOF

echo -e "${GREEN}✅ 表结构创建成功${NC}"
echo ""

# 如果有 SQLite 数据库，迁移数据
if [ -n "$SQLITE_DB" ] && command -v sqlite3 &> /dev/null; then
    echo -e "${GREEN}✅ 找到 SQLite 数据库：$SQLITE_DB${NC}"
    echo "🔄 正在检查 SQLite 中的表..."

    # 检查 SQLite 中的表
    SQLITE_TABLES=$(sqlite3 "$SQLITE_DB" ".tables" 2>/dev/null || echo "")

    if echo "$SQLITE_TABLES" | grep -q "public_vocabulary_words"; then
        echo "   找到 public_vocabulary_words 表"

        # 导出并迁移 public_vocabulary_words
        TEMP_SQL=$(mktemp)
        sqlite3 "$SQLITE_DB" <<EOF > "$TEMP_SQL"
.mode insert public_vocabulary_words
SELECT id, word, language, definition, part_of_speech, example, tag, usage_count, created_at, updated_at
FROM public_vocabulary_words;
EOF

        if [ -s "$TEMP_SQL" ]; then
            COUNT=$(wc -l < "$TEMP_SQL")
            echo "   📊 找到 $COUNT 条公共单词记录，正在迁移..."
            mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" < "$TEMP_SQL" 2>&1 | grep -v "Duplicate" || true
            echo -e "   ${GREEN}✅ 公共单词迁移完成${NC}"
        fi
        rm -f "$TEMP_SQL"
    else
        echo -e "   ${YELLOW}⚠️  SQLite 中没有 public_vocabulary_words 表${NC}"
    fi

    if echo "$SQLITE_TABLES" | grep -q "vocabulary_lists"; then
        echo "   找到 vocabulary_lists 表"

        # 导出并迁移 vocabulary_lists
        TEMP_SQL=$(mktemp)
        sqlite3 "$SQLITE_DB" <<EOF > "$TEMP_SQL"
.mode insert vocabulary_lists
SELECT id, name, description, language, category, is_public, created_at, updated_at, created_by
FROM vocabulary_lists;
EOF

        if [ -s "$TEMP_SQL" ]; then
            COUNT=$(wc -l < "$TEMP_SQL")
            echo "   📊 找到 $COUNT 条词库记录，正在迁移..."
            mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" < "$TEMP_SQL" 2>&1 | grep -v "Duplicate" || true
            echo -e "   ${GREEN}✅ 词库列表迁移完成${NC}"
        fi
        rm -f "$TEMP_SQL"
    fi

    if echo "$SQLITE_TABLES" | grep -q "vocabulary_words"; then
        echo "   找到 vocabulary_words 表"

        # 导出并迁移 vocabulary_words
        TEMP_SQL=$(mktemp)
        sqlite3 "$SQLITE_DB" <<EOF > "$TEMP_SQL"
.mode insert vocabulary_words
SELECT id, vocabulary_list_id, word, definition, part_of_speech, example, language, created_at, updated_at
FROM vocabulary_words;
EOF

        if [ -s "$TEMP_SQL" ]; then
            COUNT=$(wc -l < "$TEMP_SQL")
            echo "   📊 找到 $COUNT 条词库单词记录，正在迁移..."
            mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" < "$TEMP_SQL" 2>&1 | grep -v "Duplicate" || true
            echo -e "   ${GREEN}✅ 词库单词迁移完成${NC}"
        fi
        rm -f "$TEMP_SQL"
    fi

    echo ""
else
    if [ -z "$SQLITE_DB" ]; then
        echo -e "${YELLOW}⚠️  未找到 SQLite 数据库文件${NC}"
        echo "   可能数据已经在 MySQL 中，或者还没有数据"
    else
        echo -e "${YELLOW}⚠️  sqlite3 未安装，跳过数据迁移${NC}"
        echo "   可手动安装：apt-get install sqlite3"
    fi
fi

# 显示当前统计
echo "📊 当前公共词库统计:"
echo ""
echo "【公共词库单词】"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT
        COUNT(*) as '总单词数',
        COUNT(DISTINCT tag) as '标签数量',
        SUM(usage_count) as '总使用次数'
    FROM public_vocabulary_words;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo "【词库列表】"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT
        COUNT(*) as '词库数量',
        SUM(CASE WHEN is_public = 1 THEN 1 ELSE 0 END) as '公开词库数'
    FROM vocabulary_lists;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo "【词库单词详情】"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT
        vl.name as '词库名称',
        vl.category as '分类',
        COUNT(vw.id) as '单词数'
    FROM vocabulary_lists vl
    LEFT JOIN vocabulary_words vw ON vl.id = vw.vocabulary_list_id
    GROUP BY vl.id, vl.name, vl.category
    ORDER BY vl.id DESC
    LIMIT 10;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo "【热门标签分布】"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT
        tag as '标签',
        COUNT(*) as '单词数'
    FROM public_vocabulary_words
    WHERE tag IS NOT NULL AND tag != ''
    GROUP BY tag
    ORDER BY COUNT(*) DESC
    LIMIT 10;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo "=========================================="
echo -e "${GREEN}🎉 迁移完成！${NC}"
echo "=========================================="
echo ""
echo "💡 提示:"
echo "   1. 如果看到重复键错误，说明数据已经存在"
echo "   2. 迁移后可以正常使用公共词库功能"
echo "   3. 数据会自动同步到 MySQL 数据库"
echo ""
