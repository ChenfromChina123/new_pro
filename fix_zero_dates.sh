#!/bin/bash
# ==========================================
# 修复零值日期问题
# 功能：将数据库中的零值日期替换为合法日期
# ==========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "🔧 修复零值日期问题"
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

# 修复 public_vocabulary_words 表中的零值日期
echo "🔧 正在修复 public_vocabulary_words 表中的零值日期..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" <<EOF
-- 更新 created_at 为零值的记录
UPDATE public_vocabulary_words 
SET created_at = '2024-01-01 00:00:00' 
WHERE created_at = '0000-00-00 00:00:00' OR created_at IS NULL;

-- 更新 updated_at 为零值的记录
UPDATE public_vocabulary_words 
SET updated_at = '2024-01-01 00:00:00' 
WHERE updated_at = '0000-00-00 00:00:00' OR updated_at IS NULL;
EOF

echo -e "${GREEN}✅ public_vocabulary_words 表修复完成${NC}"
echo ""

# 修复 vocabulary_lists 表中的零值日期
echo "🔧 正在修复 vocabulary_lists 表中的零值日期..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" <<EOF
-- 更新 created_at 为零值的记录
UPDATE vocabulary_lists 
SET created_at = '2024-01-01 00:00:00' 
WHERE created_at = '0000-00-00 00:00:00' OR created_at IS NULL;

-- 更新 updated_at 为零值的记录
UPDATE vocabulary_lists 
SET updated_at = '2024-01-01 00:00:00' 
WHERE updated_at = '0000-00-00 00:00:00' OR updated_at IS NULL;
EOF

echo -e "${GREEN}✅ vocabulary_lists 表修复完成${NC}"
echo ""

# 修复 vocabulary_words 表中的零值日期
echo "🔧 正在修复 vocabulary_words 表中的零值日期..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" <<EOF
-- 更新 created_at 为零值的记录
UPDATE vocabulary_words 
SET created_at = '2024-01-01 00:00:00' 
WHERE created_at = '0000-00-00 00:00:00' OR created_at IS NULL;

-- 更新 updated_at 为零值的记录
UPDATE vocabulary_words 
SET updated_at = '2024-01-01 00:00:00' 
WHERE updated_at = '0000-00-00 00:00:00' OR updated_at IS NULL;
EOF

echo -e "${GREEN}✅ vocabulary_words 表修复完成${NC}"
echo ""

# 显示修复结果
echo "📊 修复后的日期检查:"
echo ""
echo "【public_vocabulary_words 表日期范围】"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT 
        MIN(created_at) as '最早创建时间',
        MAX(created_at) as '最晚创建时间',
        COUNT(*) as '总记录数'
    FROM public_vocabulary_words;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo "【vocabulary_lists 表日期范围】"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT 
        MIN(created_at) as '最早创建时间',
        MAX(created_at) as '最晚创建时间',
        COUNT(*) as '总记录数'
    FROM vocabulary_lists;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo "【vocabulary_words 表日期范围】"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT 
        MIN(created_at) as '最早创建时间',
        MAX(created_at) as '最晚创建时间',
        COUNT(*) as '总记录数'
    FROM vocabulary_words;
" 2>/dev/null || echo "   暂无数据"

echo ""
echo -e "${GREEN}✅ 所有表的零值日期已修复完成${NC}"
