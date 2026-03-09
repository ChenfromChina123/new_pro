from flask import Flask, request, jsonify, session
from flask_cors import CORS
from fabric import Connection
import json
import threading
import time
import sqlite3
import os

app = Flask(__name__)
app.secret_key = os.urandom(24)  # 用于session加密
CORS(app, supports_credentials=True)  # 允许跨域请求并支持凭证

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
    # 创建服务器表
    c.execute('''
    CREATE TABLE IF NOT EXISTS servers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        server_name TEXT NOT NULL,
        host TEXT NOT NULL,
        user TEXT NOT NULL,
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

# 初始化数据库
init_db()

# 存储服务器连接（按用户ID分类）
server_connections = {}
# 存储命令执行结果（按用户ID分类）
execution_results = {}

# 辅助函数：获取当前用户ID
def get_current_user_id():
    return session.get('user_id')

# 用户注册
@app.route('/api/auth/register', methods=['POST'])
def register():
    """用户注册"""
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
    """用户登录"""
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
    """用户登出"""
    session.clear()
    return jsonify({'message': 'Logout successful'})

# 获取当前用户信息
@app.route('/api/auth/me', methods=['GET'])
def get_current_user():
    """获取当前用户信息"""
    user_id = get_current_user_id()
    if user_id:
        return jsonify({'user_id': user_id, 'username': session.get('username')})
    else:
        return jsonify({'error': 'Not authenticated'}), 401

@app.route('/api/servers', methods=['GET'])
def get_servers():
    """获取服务器列表"""
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

@app.route('/api/servers', methods=['POST'])
def add_server():
    """添加新服务器"""
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    data = request.json
    server_name = data.get('name')
    host = data.get('host')
    user = data.get('user')
    port = data.get('port', 22)

    if not server_name or not host or not user:
        return jsonify({'error': 'Server name, host and user are required'}), 400

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()

    try:
        c.execute('INSERT INTO servers (user_id, server_name, host, user, port) VALUES (?, ?, ?, ?, ?)',
                 (user_id, server_name, host, user, port))
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

@app.route('/api/servers/<server_id>/connect', methods=['POST'])
def connect_server(server_id):
    """连接到服务器"""
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({'error': 'Not authenticated'}), 401

    conn = sqlite3.connect('server_terminal.db')
    c = conn.cursor()

    # 检查服务器是否属于当前用户
    c.execute('SELECT server_name, host, user, port FROM servers WHERE id = ? AND user_id = ?', (server_id, user_id))
    server = c.fetchone()
    conn.close()

    if not server:
        return jsonify({"error": "Server not found or not owned by user"}), 404

    try:
        # 这里简化处理，实际应该处理密码或密钥认证
        fabric_conn = Connection(
            host=server[1],
            user=server[2],
            port=server[3]
            # 实际使用时需要添加密码或密钥
            # password="your_password" 或 connect_kwargs={"key_filename": "/path/to/key"}
        )
        # 测试连接
        fabric_conn.run("echo 'Connected'")

        # 按用户ID存储连接
        if user_id not in server_connections:
            server_connections[user_id] = {}
        server_connections[user_id][server_id] = fabric_conn

        return jsonify({"message": "Connected successfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/servers/<server_id>/disconnect', methods=['POST'])
def disconnect_server(server_id):
    """断开服务器连接"""
    if server_id in server_connections:
        try:
            server_connections[server_id].close()
            del server_connections[server_id]
            return jsonify({"message": "Disconnected successfully"})
        except Exception as e:
            return jsonify({"error": str(e)}), 500
    return jsonify({"error": "Not connected"}), 400

@app.route('/api/servers/<server_id>/execute', methods=['POST'])
def execute_command(server_id):
    """在服务器上执行命令"""
    if server_id not in server_connections:
        return jsonify({"error": "Not connected to server"}), 400

    data = request.json
    command = data.get("command")
    if not command:
        return jsonify({"error": "Command is required"}), 400

    conn = server_connections[server_id]
    execution_id = f"exec_{int(time.time())}_{server_id}"

    def run_command():
        """在后台线程中执行命令"""
        try:
            result = conn.run(command, hide=True)
            execution_results[execution_id] = {
                "stdout": result.stdout,
                "stderr": result.stderr,
                "return_code": result.return_code,
                "completed": True
            }
        except Exception as e:
            execution_results[execution_id] = {
                "stdout": "",
                "stderr": str(e),
                "return_code": 1,
                "completed": True
            }

    # 启动后台线程执行命令
    thread = threading.Thread(target=run_command)
    thread.start()

    return jsonify({"execution_id": execution_id})

@app.route('/api/executions/<execution_id>', methods=['GET'])
def get_execution_result(execution_id):
    """获取命令执行结果"""
    if execution_id in execution_results:
        return jsonify(execution_results[execution_id])
    return jsonify({"error": "Execution not found"}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
