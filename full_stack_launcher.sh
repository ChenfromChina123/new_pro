#!/bin/bash

# ===========================================
#  AI Study Project - 完整版一键启动脚本 (Linux/macOS版)
#  包含环境检测、代码更新、服务启动功能
# ===========================================

# 设置信号处理 - 确保Ctrl+C能正常退出
trap 'echo; echo "收到中断信号，正在退出..."; exit 130' INT TERM
trap 'echo "脚本已退出"; exit 0' EXIT

# 获取脚本路径
SCRIPT_PATH="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"

# 检查并修复执行权限
if [ ! -x "$SCRIPT_PATH" ]; then
    echo "检测到脚本缺少执行权限，正在自动修复..."
    chmod +x "$SCRIPT_PATH"
    if [ $? -eq 0 ]; then
        echo "权限修复成功，重新启动脚本..."
        exec "$SCRIPT_PATH" "$@"
    else
        echo "权限修复失败，请手动执行: chmod +x $SCRIPT_PATH"
        echo "或者使用命令: bash $SCRIPT_PATH"
        exit 1
    fi
fi

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印彩色信息
print_info() {
    echo -e "${BLUE}[信息]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[成功]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[警告]${NC} $1"
}

print_error() {
    echo -e "${RED}[错误]${NC} $1"
}

print_title() {
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}=========================================${NC}"
}

echo
print_title "AI Study Project - 完整版一键启动脚本 (Linux/macOS版)"

# 切换到脚本所在目录
cd "$(dirname "$0")"

echo
print_info "当前时间: $(date)"
echo

# ==================== 环境检测 ====================
print_info "[阶段1/4] 环境检测与验证..."
echo

# 检查Git
if ! command -v git &> /dev/null; then
    print_error "未找到Git，请安装Git并确保其在PATH中"
    exit 1
else
    print_success "Git已找到"
    print_info "版本: $(git --version)"
fi

# 检查Java
if ! command -v java &> /dev/null; then
    print_error "未找到Java，请安装Java并确保其在PATH中"
    exit 1
else
    print_success "Java已找到"
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    print_info "版本: $java_version"
fi

# 检查Maven
if ! command -v mvn &> /dev/null; then
    print_warning "未找到Maven，将尝试使用项目内的mvnw"
    MAVEN_CMD="./mvnw"
    if [ ! -f "aispring/mvnw" ]; then
        print_error "项目内也未找到mvnw，请安装Maven"
        exit 1
    fi
else
    print_success "Maven已找到"
    print_info "版本: $(mvn -version | grep "Apache Maven")"
    MAVEN_CMD="mvn"
fi

# 检查Node.js
if ! command -v node &> /dev/null; then
    print_error "未找到Node.js，请安装Node.js并确保其在PATH中"
    exit 1
else
    print_success "Node.js已找到"
    print_info "版本: $(node --version)"
fi

# 检查npm
if ! command -v npm &> /dev/null; then
    print_error "未找到npm，请安装npm并确保其在PATH中"
    exit 1
else
    print_success "npm已找到"
    print_info "版本: $(npm --version)"
fi

# 检查磁盘空间
if command -v df &> /dev/null; then
    freespace=$(df -h . | awk 'NR==2 {print $4}' | sed 's/G//')
    if [[ $freespace =~ ^[0-9]+(\.[0-9]+)?$ ]] && (( $(echo "$freespace < 2" | bc -l) )); then
        print_warning "磁盘空间不足，建议至少保留2GB空闲空间 (当前: ${freespace}G)"
    else
        print_success "磁盘空间充足 (${freespace}G可用)"
    fi
else
    print_info "无法检测磁盘空间"
fi

# ==================== 代码更新 ====================
echo
print_info "[阶段2/4] 检查并更新代码..."
echo

# 检查是否为Git仓库
if [ ! -d ".git" ]; then
    print_error "当前目录不是Git仓库"
    exit 1
fi

# 获取当前分支
current_branch=$(git branch | grep '^*' | colrm 1 2)
print_info "当前分支: $current_branch"

# 暂存当前更改（如果有）
if [ -n "$(git status --porcelain)" ]; then
    print_info "检测到本地更改，正在暂存..."
    timestamp=$(date +"%Y%m%d_%H%M%S")
    git stash push -m "Auto-stash before update $timestamp"
fi

# 获取远程更新
print_info "获取远程更新..."
git fetch origin
if [ $? -ne 0 ]; then
    print_warning "无法获取远程更新，可能网络问题"
else
    # 检查是否有更新
    update_count=$(git rev-list HEAD...origin/$current_branch --count 2>/dev/null)
    if [ "$update_count" -gt "0" ]; then
        print_info "检测到 $update_count 个更新，正在合并..."
        git merge origin/$current_branch
        if [ $? -eq 0 ]; then
            print_success "代码已更新到最新版本"
        else
            print_error "代码更新失败，请手动处理冲突"
            exit 1
        fi
    else
        print_info "代码已是最新版本"
    fi
fi

# ==================== 端口清理 ====================
echo
print_info "[阶段3/4] 端口检测与清理..."
echo

# 检查并杀死5000端口占用进程
port_5000_pid=$(lsof -ti:5000)
if [ ! -z "$port_5000_pid" ]; then
    print_info "发现5000端口占用，正在终止PID $port_5000_pid..."
    kill -9 $port_5000_pid
    if [ $? -eq 0 ]; then
        print_success "PID $port_5000_pid 已终止"
    else
        print_info "PID $port_5000_pid 不存在或无法终止"
    fi
else
    print_info "5000端口未被占用"
fi

# 检查并杀死3000端口占用进程
port_3000_pid=$(lsof -ti:3000)
if [ ! -z "$port_3000_pid" ]; then
    print_info "发现3000端口占用，正在终止PID $port_3000_pid..."
    kill -9 $port_3000_pid
    if [ $? -eq 0 ]; then
        print_success "PID $port_3000_pid 已终止"
    else
        print_info "PID $port_3000_pid 不存在或无法终止"
    fi
else
    print_info "3000端口未被占用"
fi

sleep 2

# ==================== 服务启动 ====================
echo
print_info "[阶段4/4] 启动服务..."
echo

# 检查项目目录结构
if [ ! -d "aispring" ]; then
    print_error "未找到后端目录 aispring"
    exit 1
fi

if [ ! -d "vue-app" ]; then
    print_error "未找到前端目录 vue-app"
    exit 1
fi

# 设置JVM参数 (优化内存使用)
JVM_OPTS="-Xms128m -Xmx256m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 检测生产环境并配置数据库
# 生产环境数据库配置
DB_NAME="aispring"
DB_USER="aispring"
DB_PASSWORD="xGDswMCdHhsajfxF"
DB_HOST="localhost"
DB_PORT="3306"
SPRING_PROFILES=""

# 检测操作系统类型
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    print_success "检测到Linux生产环境"
    print_info "配置生产数据库: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
    
    # 设置数据库环境变量
    export SPRING_DATASOURCE_URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
    export SPRING_DATASOURCE_USERNAME="$DB_USER"
    export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
    
    # 添加prod profile参数
    SPRING_PROFILES="--spring.profiles.active=prod"
else
    print_info "检测到非Linux环境，使用默认配置"
    SPRING_PROFILES=""
fi

# 启动后端服务
print_info "启动后端服务 (Spring Boot)..."

# 进入后端目录
cd aispring

# 创建生产配置文件
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    mkdir -p src/main/resources
    cat > src/main/resources/application-prod.yml << EOF
spring:
  datasource:
    url: jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: $DB_USER
    password: $DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  flyway:
    enabled: true
    baseline-on-migrate: true
EOF
    print_success "创建生产配置文件 application-prod.yml"
fi

# 创建日志目录
mkdir -p logs

# 查找最新的JAR文件
JAR_FILE=$(find target -maxdepth 1 -name "*.jar" -type f ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | head -n 1)

# 如果没有JAR文件，先编译
if [ -z "$JAR_FILE" ]; then
    print_info "未找到JAR文件，正在编译项目..."
    $MAVEN_CMD clean package -DskipTests -q
    if [ $? -ne 0 ]; then
        print_error "项目编译失败"
        cd ..
        exit 1
    fi
    JAR_FILE=$(find target -maxdepth 1 -name "*.jar" -type f ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | head -n 1)
    print_success "项目编译完成"
fi

if [ -z "$JAR_FILE" ]; then
    print_error "编译后仍未找到JAR文件"
    cd ..
    exit 1
fi

print_info "使用JAR文件: $JAR_FILE"
print_info "内存配置: 128MB-256MB"

# 启动Spring Boot应用 (使用JAR方式，更省内存)
nohup java $JVM_OPTS -jar "$JAR_FILE" --server.port=5000 $SPRING_PROFILES > ../backend.log 2>&1 &
BACKEND_PID=$!

if [ $BACKEND_PID ]; then
    print_success "后端服务已启动，PID: $BACKEND_PID"
    print_info "日志文件: backend.log"
    print_info "等待后端初始化..."
    sleep 5
    # 检查进程是否还在运行
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        print_error "后端服务启动失败，请查看 backend.log"
        print_info "最后20行日志:"
        tail -n 20 ../backend.log
        cd ..
        exit 1
    fi
else
    print_error "后端服务启动失败"
    cd ..
    exit 1
fi

cd ..

# 等待后端服务启动
print_info "等待后端服务就绪..."
max_wait=60
wait_count=0
while [ $wait_count -lt $max_wait ]; do
    if lsof -i:5000 >/dev/null 2>&1; then
        print_success "后端服务已就绪 (端口5000已监听)"
        break
    fi
    sleep 1
    wait_count=$((wait_count + 1))
    if [ $((wait_count % 10)) -eq 0 ]; then
        print_info "已等待 ${wait_count} 秒..."
    fi
done

if [ $wait_count -ge $max_wait ]; then
    print_warning "后端服务启动超时，请检查日志"
    print_info "最后30行日志:"
    tail -n 30 backend.log
fi

sleep 2

# 启动前端服务
print_info "启动前端服务 (Vue.js)..."

# 进入前端目录
cd vue-app

# 检查并安装依赖
if [ ! -d "node_modules" ]; then
    print_info "安装前端依赖..."
    npm install --registry https://registry.npmmirror.com
    if [ $? -ne 0 ]; then
        print_error "前端依赖安装失败"
        cd ..
        exit 1
    else
        print_success "前端依赖安装完成"
    fi
else
    print_info "前端依赖已存在，跳过安装"
fi

# 启动前端开发服务器 (在后台运行)
nohup npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!

if [ $FRONTEND_PID ]; then
    print_success "前端服务已启动，PID: $FRONTEND_PID"
    print_info "日志文件: frontend.log"
else
    print_error "前端服务启动失败"
    cd ..
    exit 1
fi

cd ..

# 等待前端服务启动
print_info "等待前端服务就绪..."
max_wait=30
wait_count=0
while [ $wait_count -lt $max_wait ]; do
    if lsof -i:3000 >/dev/null 2>&1; then
        print_success "前端服务已就绪 (端口3000已监听)"
        break
    fi
    sleep 1
    wait_count=$((wait_count + 1))
done

if [ $wait_count -ge $max_wait ]; then
    print_warning "前端服务启动超时，请检查日志"
    print_info "最后30行日志:"
    tail -n 30 frontend.log
fi

echo
print_title "服务启动完成！"
echo
print_info "当前时间: $(date)"
echo
print_info "后端服务: http://localhost:5000"
print_info "API文档: http://localhost:5000/swagger-ui.html"
print_info "管理后台: http://localhost:5000/admin"
echo
print_info "前端访问: http://localhost:3000"
echo
print_info "后台进程:"
print_info "  - 后端PID: $BACKEND_PID"
print_info "  - 前端PID: $FRONTEND_PID"
echo
print_info "日志文件:"
print_info "  - 后端: backend.log"
print_info "  - 前端: frontend.log"
echo
print_info "操作提示:"
print_info "  - 使用 'kill $BACKEND_PID' 停止后端服务"
print_info "  - 使用 'kill $FRONTEND_PID' 停止前端服务"
print_info "  - 使用 'tail -f backend.log' 查看后端实时日志"
print_info "  - 使用 'tail -f frontend.log' 查看前端实时日志"
print_info "  - 首次启动可能需要等待30-60秒"
print_title ""

echo
echo "=========================================="
echo "  快捷操作菜单"
echo "=========================================="
echo "  1) 查看后端日志 (实时)"
echo "  2) 查看前端日志 (实时)"
echo "  3) 查看后端日志 (最后50行)"
echo "  4) 查看前端日志 (最后50行)"
echo "  5) 停止所有服务"
echo "  6) 退出"
echo "=========================================="
echo
read -p "请选择操作 (1-6): " choice

case $choice in
    1)
        print_info "显示后端实时日志 (Ctrl+C 退出)..."
        print_info "按 Ctrl+C 返回菜单"
        tail -f backend.log || true
        ;;
    2)
        print_info "显示前端实时日志 (Ctrl+C 退出)..."
        print_info "按 Ctrl+C 返回菜单"
        tail -f frontend.log || true
        ;;
    3)
        print_info "显示后端最后50行日志..."
        tail -n 50 backend.log
        echo
        read -p "按Enter键继续..."
        ;;
    4)
        print_info "显示前端最后50行日志..."
        tail -n 50 frontend.log
        echo
        read -p "按Enter键继续..."
        ;;
    5)
        if kill $BACKEND_PID 2>/dev/null; then
            print_success "后端服务已停止 (PID: $BACKEND_PID)"
        fi
        if kill $FRONTEND_PID 2>/dev/null; then
            print_success "前端服务已停止 (PID: $FRONTEND_PID)"
        fi
        print_info "所有服务已停止"
        ;;
    6)
        print_info "服务仍在后台运行"
        print_info "使用以下命令查看日志:"
        print_info "  tail -f backend.log"
        print_info "  tail -f frontend.log"
        ;;
    *)
        print_info "无效选择，服务仍在后台运行"
        ;;
esac
