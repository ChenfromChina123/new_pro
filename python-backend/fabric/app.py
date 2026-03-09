from flask import Flask, request, jsonify, session
from flask_cors import CORS
from flask_socketio import SocketIO, send, emit
from fabric import Connection
import paramiko
import json
import threading
import time
import socket
import sqlite3
import os

app = Flask(__name__)
app.secret_key = os.urandom(24)
CORS(app, supports_credentials=True)

# 初始化 SocketIO
socketio = SocketIO(app, cors_allowed_origins="*", async_mode='threading')

# 数据库初始化
def init_db():
    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()
    # 创建用户表
    c.execute('''
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL
    )
    ''')
    # 创建服务器表（添加密码字段）
    c.execute('''
    CREATE TABLE IF NOT EXISTS servers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        server_name TEXT NOT NULL,
        host TEXT NOT NULL,
        user TEXT NOT NULL,
        password TEXT NOT NULL,
        port INTEGER DEFAULT 22,
        FOREIGN KEY (user_id) REFERENCES users (id)
    )
    ''')
    # 创建命令执行记录表
    c.execute('''
    CREATE TABLE IF NOT EXISTS executions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        server_id INTEGER,
        command TEXT NOT NULL,
        stdout TEXT,
        stderr TEXT,
        return_code INTEGER,
        executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id),
        FOREIGN KEY (server_id) REFERENCES servers (id)
    )
    ''')
    conn.commit()
    conn.close()

init_db()

# 存储活动连接
active_connections = {}

# 辅助函数：获取当前用户ID
def get_current_user_id():
    return session.get('user_id')

# 用户注册
@app.route('/api/auth/register', methods=['POST'])
def register():
    data = request.json
    username = data.get('username')
    password = data.get('password')

    if not username or not password:
        return jsonify({'error': 'Username and password are required'}), 400

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()

    try:
        c.execute('INSERT INTO users (username, password) VALUES (?, ?)', (username, password))
        conn.commit()
        user_id = c.lastrowid
        session['user_id'] = user_id
        session['username'] = username
        return jsonify({'message': 'Registration successful', 'user_id': user_id}), 201
    except sqlite3.IntegrityError:
        return jsonify({'error': 'Username already exists'}), 400
    finally:
        conn.close()

# 用户登录
@app.route('/api/auth/login', methods=['POST'])
def login():
    data = request.json
    username = data.get('username')
    password = data.get('password')

    if not username or not password:
        return jsonify({'error': 'Username and password are required'}), 400

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()

    c.execute('SELECT id FROM users WHERE username = ? AND password = ?', (username, password))
    user = c.fetchone()
    conn.close()

    if user:
        session['user_id'] = user[0]
        session['username'] = username
        return jsonify({'message': 'Login successful', 'user_id': user[0]})
    else:
        return jsonify({'error': 'Invalid username or password'}), 401

# 用户登出
@app.route('/api/auth/logout', methods=['POST'])
def logout():
    session.clear()
    return jsonify({'message': 'Logout successful'})

# 获取当前用户信息
@app.route('/api/auth/me', methods=['GET'])
def get_current_user():
    user_id = get_current_user_id()
    if user_id:
        return jsonify({'user_id': user_id, 'username': session.get('username')})
    else:
        return jsonify({'error': 'Not authenticated'}), 401

# 获取服务器列表
@app.route('/api/servers', methods=['GET'])
def get_servers():
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()
    c.execute('SELECT id, server_name, host, user, port FROM servers WHERE user_id = ?', (user_id,))
    servers = []
    for row in c.fetchall():
        servers.append({
            'id': str(row[0]),
            'name': row[1],
            'host': row[2],
            'user': row[3],
            'port': row[4]
        })
    conn.close()
    return jsonify(servers)

# 添加服务器
@app.route('/api/servers', methods=['POST'])
def add_server():
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    data = request.json
    server_name = data.get('name')
    host = data.get('host')
    user = data.get('user')
    password = data.get('password', '')
    port = data.get('port', 22)

    if not server_name or not host or not user:
        return jsonify({'error': 'Server name, host and user are required'}), 400

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()

    try:
        c.execute('INSERT INTO servers (user_id, server_name, host, user, password, port) VALUES (?, ?, ?, ?, ?, ?)',
                 (user_id, server_name, host, user, password, port))
        conn.commit()
        server_id = c.lastrowid
        new_server = {
            'id': str(server_id),
            'name': server_name,
            'host': host,
            'user': user,
            'port': port
        }
        return jsonify(new_server), 201
    finally:
        conn.close()

# 删除服务器
@app.route('/api/servers/<server_id>', methods=['DELETE'])
def delete_server(server_id):
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()

    c.execute('DELETE FROM servers WHERE id = ? AND user_id = ?', (server_id, user_id))
    conn.commit()
    affected = c.rowcount
    conn.close()

    if affected > 0:
        return jsonify({'message': 'Server deleted successfully'})
    else:
        return jsonify({'error': 'Server not found'}), 404

# 连接服务器
@app.route('/api/servers/<server_id>/connect', methods=['POST'])
def connect_server(server_id):
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()

    c.execute('SELECT server_name, host, user, password, port FROM servers WHERE id = ? AND user_id = ?', (server_id, user_id))
    server = c.fetchone()
    conn.close()

    if not server:
        return jsonify({"error": "Server not found or not owned by user"}), 404

    try:
        user_ssh = paramiko.SSHClient()
        user_ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

        user_ssh.connect(
            hostname=server[1],
            port=server[4],
            username=server[2],
            password=server[3],
            timeout=10,
            allow_agent=False,
            look_for_keys=False
        )

        if user_id not in active_connections:
            active_connections[user_id] = {}
        active_connections[user_id][server_id] = user_ssh

        return jsonify({"message": "Connected successfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# 断开服务器连接
@app.route('/api/servers/<server_id>/disconnect', methods=['POST'])
def disconnect_server(server_id):
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    if user_id in active_connections and server_id in active_connections[user_id]:
        try:
            active_connections[user_id][server_id].close()
            del active_connections[user_id][server_id]
            return jsonify({"message": "Disconnected successfully"})
        except Exception as e:
            return jsonify({"error": str(e)}), 500
    return jsonify({"error": "Not connected"}), 400

# 执行命令
@app.route('/api/servers/<server_id>/execute', methods=['POST'])
def execute_command(server_id):
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    if user_id not in active_connections or server_id not in active_connections[user_id]:
        return jsonify({"error": "Not connected to server"}), 400

    data = request.json
    command = data.get("command")
    if not command:
        return jsonify({"error": "Command is required"}), 400

    ssh = active_connections[user_id][server_id]
    execution_id = f"exec_{int(time.time())}_{server_id}"

    def run_command():
        try:
            stdin, stdout, stderr = ssh.exec_command(command)
            result = stdout.read().decode('utf-8', errors='ignore')
            error = stderr.read().decode('utf-8', errors='ignore')
            return_code = stdout.channel.recv_exit_status()

            execution_results[execution_id] = {
                "stdout": result,
                "stderr": error,
                "return_code": return_code,
                "completed": True
            }
        except Exception as e:
            execution_results[execution_id] = {
                "stdout": "",
                "stderr": str(e),
                "return_code": 1,
                "completed": True
            }

    thread = threading.Thread(target=run_command)
    thread.start()

    return jsonify({"execution_id": execution_id})

# 存储执行结果
execution_results = {}

# 获取命令执行结果
@app.route('/api/executions/<execution_id>', methods=['GET'])
def get_execution_result(execution_id):
    if execution_id in execution_results:
        return jsonify(execution_results[execution_id])
    return jsonify({"error": "Execution not found"}), 404

# WebSocket 事件处理
@socketio.on('connect')
def handle_connect():
    print('Client connected')
    send('Connected to WebSocket server')

@socketio.on('disconnect')
def handle_disconnect():
    print('Client disconnected')

@socketio.on('ssh_connect')
def handle_ssh_connect(data):
    """通过 WebSocket 连接 SSH"""
    user_id = data.get('user_id')
    server_id = data.get('server_id')

    if not user_id or not server_id:
        emit('ssh_error', 'Missing user_id or server_id')
        return

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()
    c.execute('SELECT server_name, host, user, password, port FROM servers WHERE id = ? AND user_id = ?', (server_id, user_id))
    server = c.fetchone()
    conn.close()

    if not server:
        emit('ssh_error', 'Server not found')
        return

    try:
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

        ssh.connect(
            hostname=server[1],
            port=server[4],
            username=server[2],
            password=server[3],
            timeout=10,
            allow_agent=False,
            look_for_keys=False
        )

        # 保存连接
        if user_id not in active_connections:
            active_connections[user_id] = {}
        active_connections[user_id][server_id] = ssh

        # 启动shell
        channel = ssh.invoke_shell()
        channel.set_combine_stderr(True)
        channel.settimeout(0.1)

        # 保存channel用于后续交互
        if user_id not in active_connections:
            active_connections[user_id] = {}
        active_connections[user_id][f"{server_id}_channel"] = channel

        emit('ssh_connected', 'SSH连接成功！')

        # 启动读取输出的线程
        def read_output():
            while True:
                try:
                    if channel.recv_ready():
                        output = channel.recv(4096).decode('utf-8', errors='ignore')
                        if output:
                            emit('ssh_output', output)
                    time.sleep(0.1)
                except Exception as e:
                    emit('ssh_error', f'读取输出失败: {str(e)}')
                    break

        thread = threading.Thread(target=read_output)
        thread.daemon = True
        thread.start()

    except Exception as e:
        emit('ssh_error', f'连接失败: {str(e)}')

@socketio.on('ssh_input')
def handle_ssh_input(data):
    """通过 WebSocket 发送命令"""
    user_id = data.get('user_id')
    server_id = data.get('server_id')
    command = data.get('command')

    if not user_id or not server_id or not command:
        emit('ssh_error', 'Missing parameters')
        return

    channel_key = f"{server_id}_channel"
    if user_id in active_connections and channel_key in active_connections[user_id]:
        try:
            channel = active_connections[user_id][channel_key]
            channel.send(command)
        except Exception as e:
            emit('ssh_error', f'发送命令失败: {str(e)}')
    else:
        emit('ssh_error', 'Not connected to SSH')

@socketio.on('ssh_disconnect')
def handle_ssh_disconnect(data):
    """断开 SSH 连接"""
    user_id = data.get('user_id')
    server_id = data.get('server_id')

    if user_id in active_connections:
        channel_key = f"{server_id}_channel"
        if channel_key in active_connections[user_id]:
            try:
                active_connections[user_id][channel_key].close()
                del active_connections[user_id][channel_key]
            except:
                pass
        if server_id in active_connections[user_id]:
            try:
                active_connections[user_id][server_id].close()
                del active_connections[user_id][server_id]
            except:
                pass

    emit('ssh_disconnected', 'SSH连接已断开')

if __name__ == '__main__':
    socketio.run(app, host='0.0.0.0', port=5001, debug=True)
