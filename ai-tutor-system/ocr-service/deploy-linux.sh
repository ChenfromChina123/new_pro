#!/bin/bash
#
# OCR服务一键部署脚本 - Linux服务器
# 支持: Ubuntu/Debian, CentOS/RHEL/Rocky Linux/Alibaba Cloud Linux
# 内存限制: 200MB, CPU: 单核
# 服务端口: 8089
#

set -e

# ==================== 配置 ====================
SERVICE_NAME="ocr-service"
SERVICE_PORT=8089
INSTALL_DIR="/opt/ocr-service"
SERVICE_USER="ocr"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ==================== 函数 ====================

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "此脚本需要root权限运行"
        print_info "请使用: sudo bash $0"
        exit 1
    fi
}

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

install_dependencies() {
    print_info "安装系统依赖..."
    
    case $OS in
        ubuntu|debian)
            apt-get update -qq
            apt-get install -y -qq \
                python3 python3-pip python3-venv \
                curl wget git \
                libgomp1 libgl1 libglib2.0-0 > /dev/null
            ;;
        centos|rhel|rocky|almalinux|alinux)
            # alinux 是阿里云Linux，基于CentOS/RHEL
            if command -v yum &> /dev/null; then
                yum install -y -q \
                    python3 python3-pip \
                    curl wget git \
                    libgomp libglvnd-glx glib2 > /dev/null
            elif command -v dnf &> /dev/null; then
                dnf install -y -q \
                    python3 python3-pip \
                    curl wget git \
                    libgomp libglvnd-glx glib2 > /dev/null
            else
                print_error "未找到 yum 或 dnf 包管理器"
                exit 1
            fi
            ;;
        *)
            print_error "不支持的操作系统: $OS"
            print_info "支持的系统: ubuntu, debian, centos, rhel, rocky, almalinux, alinux"
            exit 1
            ;;
    esac
    
    print_success "系统依赖安装完成"
}

create_user() {
    if ! id -u $SERVICE_USER > /dev/null 2>&1; then
        print_info "创建服务用户: $SERVICE_USER"
        useradd -r -s /bin/false $SERVICE_USER
    fi
}

install_python_deps() {
    print_info "安装Python依赖..."
    
    cd $INSTALL_DIR
    
    python3 -m venv venv
    source venv/bin/activate
    
    pip install --upgrade pip -q
    pip install -r requirements.txt -q
    
    deactivate
    
    print_success "Python依赖安装完成"
}

deploy_service() {
    print_info "部署服务文件..."
    
    mkdir -p $INSTALL_DIR
    
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    
    cp -r $SCRIPT_DIR/*.py $INSTALL_DIR/
    cp -r $SCRIPT_DIR/requirements.txt $INSTALL_DIR/
    
    chown -R $SERVICE_USER:$SERVICE_USER $INSTALL_DIR
    
    print_success "服务文件部署完成"
}

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
Environment="OMP_NUM_THREADS=1"
Environment="MKL_NUM_THREADS=1"
ExecStart=$INSTALL_DIR/venv/bin/python $INSTALL_DIR/app.py
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

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

start_service() {
    print_info "启动OCR服务..."
    systemctl start $SERVICE_NAME
    sleep 3
    print_success "服务启动完成"
}

test_service() {
    print_info "测试服务..."
    
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

print_deploy_info() {
    SERVER_IP=$(hostname -I | awk '{print $1}')
    
    echo ""
    echo "=========================================="
    echo -e "${GREEN}OCR服务部署完成!${NC}"
    echo "=========================================="
    echo ""
    echo "服务端口: $SERVICE_PORT"
    echo ""
    echo "服务地址:"
    echo "  - 本地: http://localhost:$SERVICE_PORT"
    echo "  - 外部: http://$SERVER_IP:$SERVICE_PORT"
    echo ""
    echo "API接口:"
    echo "  健康检查: GET  http://localhost:$SERVICE_PORT/health"
    echo "  文件识别: POST http://localhost:$SERVICE_PORT/ocr"
    echo "  Base64:   POST http://localhost:$SERVICE_PORT/ocr/base64"
    echo ""
    echo "管理命令:"
    echo "  启动:   systemctl start $SERVICE_NAME"
    echo "  停止:   systemctl stop $SERVICE_NAME"
    echo "  重启:   systemctl restart $SERVICE_NAME"
    echo "  状态:   systemctl status $SERVICE_NAME"
    echo "  日志:   journalctl -u $SERVICE_NAME -f"
    echo ""
    echo "资源限制:"
    echo "  内存: 200MB"
    echo "  CPU:  单核"
    echo ""
    echo "卸载命令: bash $0 --uninstall"
    echo "=========================================="
}

uninstall_service() {
    print_warning "正在卸载OCR服务..."
    
    systemctl stop $SERVICE_NAME 2>/dev/null || true
    systemctl disable $SERVICE_NAME 2>/dev/null || true
    rm -f /etc/systemd/system/$SERVICE_NAME.service
    systemctl daemon-reload
    
    userdel -r $SERVICE_USER 2>/dev/null || true
    rm -rf $INSTALL_DIR
    
    print_success "OCR服务已卸载"
}

main() {
    if [[ "$1" == "--uninstall" ]]; then
        check_root
        uninstall_service
        exit 0
    fi
    
    echo ""
    echo "=========================================="
    echo "  OCR服务 - Linux一键部署脚本"
    echo "  服务端口: $SERVICE_PORT"
    echo "  内存限制: 200MB | CPU: 单核"
    echo "=========================================="
    echo ""
    
    check_root
    detect_os
    install_dependencies
    create_user
    deploy_service
    install_python_deps
    setup_systemd
    start_service
    test_service
    print_deploy_info
}

main "$@"
