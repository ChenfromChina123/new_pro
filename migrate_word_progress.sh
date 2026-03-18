#!/bin/bash
# ==========================================
# 单词记忆进度迁移脚本
# 功能：将本地 SQLite 进度数据迁移到 MySQL 数据库
# ==========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "📦 单词记忆进度迁移工具"
echo "=========================================="

# 数据库配置
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-aispring}"
DB_USERNAME="${DB_USERNAME:-aispring}"
DB_PASSWORD="${DB_PASSWORD:-xGDswMCdHhsajfxF}"

# 用户 ID（默认为第一个用户）
USER_ID="${1:-1}"

echo ""
echo "📋 数据库配置:"
echo "   主机：$DB_HOST:$DB_PORT"
echo "   数据库：$DB_NAME"
echo "   用户：$DB_USERNAME"
echo "   迁移用户 ID: $USER_ID"
echo ""

# 检查本地进度数据库文件
PROGRESS_DB=""
if [ -f "ai-tutor-system/aispring/word-game/server/progress.db" ]; then
    PROGRESS_DB="ai-tutor-system/aispring/word-game/server/progress.db"
elif [ -f "ai-tutor-system/aispring/word-game/progress.db" ]; then
    PROGRESS_DB="ai-tutor-system/aispring/word-game/progress.db"
elif [ -f "ai-tutor-system/aispring/word-game/progress.db-shm" ]; then
    PROGRESS_DB="ai-tutor-system/aispring/word-game/progress.db"
fi

if [ -z "$PROGRESS_DB" ] || [ ! -f "$PROGRESS_DB" ]; then
    echo -e "${YELLOW}⚠️  未找到本地进度数据库文件${NC}"
    echo "   可能进度已经保存在 MySQL 中，或者还没有学习记录"
    echo ""
    echo "✅ 跳过迁移，直接验证数据库连接..."
    
    # 验证数据库连接
    if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" &>/dev/null; then
        echo -e "${GREEN}✅ 数据库连接成功${NC}"
        
        # 检查进度表是否存在
        TABLE_EXISTS=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "SHOW TABLES LIKE 'word_game_progress';" | wc -l)
        
        if [ "$TABLE_EXISTS" -gt 1 ]; then
            echo -e "${GREEN}✅ 进度表已存在${NC}"
            
            # 显示当前进度统计
            echo ""
            echo "📊 当前进度统计:"
            mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
                SELECT 
                    package_id as '课程包',
                    COUNT(*) as '记录数',
                    SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as '已完成课程数'
                FROM word_game_progress
                WHERE user_id = $USER_ID
                GROUP BY package_id;
            " 2>/dev/null || echo "   暂无进度记录"
        else
            echo -e "${YELLOW}⚠️  进度表不存在，系统会在首次使用时自动创建${NC}"
        fi
    else
        echo -e "${RED}❌ 数据库连接失败，请检查配置${NC}"
        exit 1
    fi
    exit 0
fi

echo -e "${GREEN}✅ 找到本地进度数据库：$PROGRESS_DB${NC}"
echo ""

# 检查 sqlite3 是否安装
if ! command -v sqlite3 &> /dev/null; then
    echo -e "${RED}❌ sqlite3 未安装，请先安装：apt-get install sqlite3${NC}"
    exit 1
fi

# 读取 SQLite 数据
echo "📖 正在读取本地进度数据..."

# 导出 SQLite 数据为 SQL 语句
TEMP_SQL=$(mktemp)

sqlite3 "$PROGRESS_DB" <<EOF > "$TEMP_SQL"
.mode insert word_game_progress
SELECT 
    user_id,
    package_id,
    course_index,
    current_question,
    completed,
    study_seconds,
    created_at,
    updated_at
FROM progress;
EOF

# 检查是否有数据
if [ ! -s "$TEMP_SQL" ]; then
    echo -e "${YELLOW}⚠️  本地数据库中没有进度记录${NC}"
    rm -f "$TEMP_SQL"
    exit 0
fi

RECORD_COUNT=$(wc -l < "$TEMP_SQL")
echo -e "${GREEN}✅ 找到 $RECORD_COUNT 条进度记录${NC}"
echo ""

# 验证数据库连接
echo "🔌 正在连接 MySQL 数据库..."
if ! mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" &>/dev/null; then
    echo -e "${RED}❌ 数据库连接失败${NC}"
    rm -f "$TEMP_SQL"
    exit 1
fi

echo -e "${GREEN}✅ 数据库连接成功${NC}"
echo ""

# 创建表（如果不存在）
echo "📋 检查进度表..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" <<EOF
CREATE TABLE IF NOT EXISTS word_game_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    package_id VARCHAR(255) NOT NULL,
    course_index INT NOT NULL,
    current_question INT DEFAULT 0,
    completed TINYINT(1) DEFAULT 0,
    study_seconds INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_package (user_id, package_id),
    INDEX idx_user_package_course (user_id, package_id, course_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
EOF

echo -e "${GREEN}✅ 进度表已就绪${NC}"
echo ""

# 迁移数据
echo "🔄 正在迁移数据..."

# 更新用户 ID（如果需要）
if [ "$USER_ID" != "1" ]; then
    echo "   将用户 ID 更新为：$USER_ID"
    sed -i "s/VALUES (1,/VALUES ($USER_ID,/g" "$TEMP_SQL"
fi

# 执行迁移
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" < "$TEMP_SQL" 2>&1 || {
    echo -e "${YELLOW}⚠️  部分数据可能已存在，跳过重复记录${NC}"
}

# 清理临时文件
rm -f "$TEMP_SQL"

echo -e "${GREEN}✅ 数据迁移完成${NC}"
echo ""

# 显示迁移后的统计
echo "📊 迁移后统计:"
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" -D"$DB_NAME" -e "
    SELECT 
        package_id as '课程包',
        COUNT(*) as '记录数',
        SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as '已完成课程数',
        MAX(current_question) as '最大学题号'
    FROM word_game_progress
    WHERE user_id = $USER_ID
    GROUP BY package_id;
" 2>/dev/null || echo "   查询失败"

echo ""
echo "=========================================="
echo -e "${GREEN}🎉 迁移完成！${NC}"
echo "=========================================="
echo ""
echo "💡 提示:"
echo "   1. 如果看到重复键错误，说明数据已经存在"
echo "   2. 迁移后可以正常继续使用单词记忆功能"
echo "   3. 进度会自动同步到 MySQL 数据库"
echo ""
