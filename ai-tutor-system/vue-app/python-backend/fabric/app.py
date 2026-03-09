from flask import Flask, request, jsonify
from flask_cors import CORS
from fabric import Connection
import json
import threading
import time

app = Flask(__name__)
CORS(app)  # 允许跨域请求

# 存储服务器连接
server_connections = {}
# 存储命令执行结果
execution_results = {}
# 存储服务器列表
servers = [
    {"id": "server1", "name": "服务器1", "host": "localhost", "user": "admin", "port": 22},
    {"id": "server2", "name": "服务器2", "host": "127.0.0.1", "user": "admin", "port": 22}
]

@app.route('/api/servers', methods=['GET'])
def get_servers():
    """获取服务器列表"""
    return jsonify(servers)

@app.route('/api/servers', methods=['POST'])
def add_server():
    """添加新服务器"""
    data = request.json
    new_server = {
        "id": f"server{len(servers) + 1}",
        "name": data.get("name"),
        "host": data.get("host"),
        "user": data.get("user"),
        "port": data.get("port", 22)
    }
    servers.append(new_server)
    return jsonify(new_server), 201

@app.route('/api/servers/<server_id>', methods=['DELETE'])
def delete_server(server_id):
    """删除服务器"""
    global servers
    servers = [s for s in servers if s["id"] != server_id]
    # 同时删除对应的连接
    if server_id in server_connections:
        del server_connections[server_id]
    return jsonify({"message": "Server deleted"})

@app.route('/api/servers/<server_id>/connect', methods=['POST'])
def connect_server(server_id):
    """连接到服务器"""
    server = next((s for s in servers if s["id"] == server_id), None)
    if not server:
        return jsonify({"error": "Server not found"}), 404
    
    try:
        # 这里简化处理，实际应该处理密码或密钥认证
        conn = Connection(
            host=server["host"],
            user=server["user"],
            port=server["port"]
            # 实际使用时需要添加密码或密钥
            # password="your_password" 或 connect_kwargs={"key_filename": "/path/to/key"}
        )
        # 测试连接
        conn.run("echo 'Connected'")
        server_connections[server_id] = conn
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
    app.run(host='0.0.0.0', port=5000, debug=True)