#!/bin/bash
#
# OCR服务一键部署脚本
# 适用于: Ubuntu 20.04+ / Debian 11+ / CentOS 8+
# 内存要求: 200MB+
# 

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
SERVICE_NAME="ocr-service"
SERVICE_PORT=8089
INSTALL_DIR="/opt/ocr-service"
SERVICE_USER="ocr"
PYTHON_VERSION="3.10"

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查是否为root用户
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "此脚本需要root权限运行"
        print_info "请使用: sudo bash $0"
        exit 1
    fi
}

# 检测操作系统
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        OS_VERSION=$VERSION_ID
    else
        print_error "无法检测操作系统"
        exit 1
    fi
    print_info "检测到操作系统: $OS $OS_VERSION"
}

# 安装系统依赖
install_dependencies() {
    print_info "安装系统依赖..."
    
    case $OS in
        ubuntu|debian)
            apt-get update -qq
            apt-get install -y -qq \
                python3 python3-pip python3-venv \
                curl wget git \
                libgomp1 libgl1 libglib2.0-0 \
                nginx > /dev/null
            ;;
        centos|rhel|rocky|almalinux)
            yum install -y -q \
                python3 python3-pip \
                curl wget git \
                libgomp libglvnd-glx glib2 \
                nginx > /dev/null || \
            dnf install -y -q \
                python3 python3-pip \
                curl wget git \
                libgomp libglvnd-glx glib2 \
                nginx > /dev/null
            ;;
        *)
            print_error "不支持的操作系统: $OS"
            exit 1
            ;;
    esac
    
    print_success "系统依赖安装完成"
}

# 创建服务用户
create_user() {
    if ! id -u $SERVICE_USER > /dev/null 2>&1; then
        print_info "创建服务用户: $SERVICE_USER"
        useradd -r -s /bin/false $SERVICE_USER
        print_success "用户创建完成"
    else
        print_info "用户 $SERVICE_USER 已存在"
    fi
}

# 安装Python依赖
install_python_deps() {
    print_info "安装Python依赖..."
    
    cd $INSTALL_DIR
    
    # 创建虚拟环境
    python3 -m venv venv
    source venv/bin/activate
    
    # 升级pip
    pip install --upgrade pip -q
    
    # 安装依赖
    pip install -r requirements.txt -q
    
    deactivate
    
    print_success "Python依赖安装完成"
}

# 部署服务文件
deploy_service() {
    print_info "部署服务文件..."
    
    # 创建安装目录
    mkdir -p $INSTALL_DIR
    
    # 获取脚本所在目录
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    
    # 复制文件
    cp -r $SCRIPT_DIR/*.py $INSTALL_DIR/
    cp -r $SCRIPT_DIR/requirements.txt $INSTALL_DIR/
    
    # 设置权限
    chown -R $SERVICE_USER:$SERVICE_USER $INSTALL_DIR
    
    print_success "服务文件部署完成"
}

# 配置systemd服务
setup_systemd() {
    print_info "配置systemd服务..."
    
    cat > /etc/systemd/system/$SERVICE_NAME.service << EOF
[Unit]
Description=OCR Service - Lightweight OCR with RapidOCR
After=network.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$INSTALL_DIR
Environment="PATH=$INSTALL_DIR/venv/bin"
Environment="PORT=$SERVICE_PORT"
Environment="HOST=0.0.0.0"
Environment="OMP_NUM_THREADS=1"
Environment="MKL_NUM_THREADS=1"
ExecStart=$INSTALL_DIR/venv/bin/python $INSTALL_DIR/app.py
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

# 资源限制
LimitNOFILE=65535
MemoryMax=200M
CPUQuota=100%

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable $SERVICE_NAME
    
    print_success "systemd服务配置完成"
}

# 配置Nginx反向代理
setup_nginx() {
    print_info "配置Nginx反向代理..."
    
    cat > /etc/nginx/sites-available/$SERVICE_NAME << 'EOF'
server {
    listen 80;
    server_name _;

    # OCR服务
    location / {
        proxy_pass http://127.0.0.1:8089;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 文件上传大小限制
        client_max_body_size 20M;
    }
    
    # 健康检查
    location /health {
        proxy_pass http://127.0.0.1:8089/health;
        access_log off;
    }
}
EOF

    # 启用站点
    ln -sf /etc/nginx/sites-available/$SERVICE_NAME /etc/nginx/sites-enabled/
    
    # 测试并重载Nginx
    nginx -t && systemctl reload nginx
    
    print_success "Nginx配置完成"
}

# 启动服务
start_service() {
    print_info "启动OCR服务..."
    
    systemctl start $SERVICE_NAME
    
    # 等待服务启动
    sleep 3
    
    # 检查服务状态
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_success "OCR服务启动成功"
    else
        print_error "OCR服务启动失败"
        journalctl -u $SERVICE_NAME --no-pager -n 20
        exit 1
    fi
}

# 健康检查
health_check() {
    print_info "执行健康检查..."
    
    sleep 2
    
    if curl -sf http://localhost:$SERVICE_PORT/health > /dev/null; then
        print_success "健康检查通过"
        echo ""
        curl -s http://localhost:$SERVICE_PORT/health | python3 -m json.tool 2>/dev/null || \
        curl -s http://localhost:$SERVICE_PORT/health
        echo ""
    else
        print_warning "健康检查失败，请检查日志: journalctl -u $SERVICE_NAME"
    fi
}

# 打印部署信息
print_deploy_info() {
    echo ""
    echo "=========================================="
    echo -e "${GREEN}OCR服务部署完成!${NC}"
    echo "=========================================="
    echo ""
    echo "服务地址:"
    echo "  - 本地: http://localhost:$SERVICE_PORT"
    echo "  - Nginx: http://$(hostname -I | awk '{print $1}')"
    echo ""
    echo "管理命令:"
    echo "  启动:   systemctl start $SERVICE_NAME"
    echo "  停止:   systemctl stop $SERVICE_NAME"
    echo "  重启:   systemctl restart $SERVICE_NAME"
    echo "  状态:   systemctl status $SERVICE_NAME"
    echo "  日志:   journalctl -u $SERVICE_NAME -f"
    echo ""
    echo "API接口:"
    echo "  健康检查: GET  /health"
    echo "  文件识别:  POST /ocr"
    echo "  Base64:    POST /ocr/base64"
    echo ""
    echo "资源限制:"
    echo "  内存: 200MB"
    echo "  CPU:  1核"
    echo ""
}

# 主函数
main() {
    echo ""
    echo "=========================================="
    echo "   OCR服务一键部署脚本"
    echo "   内存: ~110MB | CPU: 单核"
    echo "=========================================="
    echo ""
    
    check_root
    detect_os
    install_dependencies
    create_user
    deploy_service
    install_python_deps
    setup_systemd
    setup_nginx
    start_service
    health_check
    print_deploy_info
}

# 执行主函数
main "$@"
