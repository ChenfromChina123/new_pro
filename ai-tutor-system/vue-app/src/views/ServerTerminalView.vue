<template>
  <div class="server-terminal-view">
    <h1 class="page-title">多服务器终端控制</h1>
    
    <div class="content-container">
      <!-- 服务器列表 -->
      <div class="servers-section">
        <h2>服务器列表</h2>
        <div class="servers-list">
          <div 
            v-for="server in servers" 
            :key="server.id"
            class="server-item"
            :class="{ 'connected': connectedServers.includes(server.id) }"
          >
            <div class="server-info">
              <h3>{{ server.name }}</h3>
              <p>{{ server.host }}:{{ server.port }}</p>
              <p>用户: {{ server.user }}</p>
            </div>
            <div class="server-actions">
              <button 
                v-if="!connectedServers.includes(server.id)"
                @click="connectServer(server.id)"
                class="btn btn-primary"
              >
                连接
              </button>
              <button 
                v-else
                @click="disconnectServer(server.id)"
                class="btn btn-danger"
              >
                断开
              </button>
              <button 
                @click="deleteServer(server.id)"
                class="btn btn-secondary"
              >
                删除
              </button>
            </div>
          </div>
        </div>
        
        <!-- 添加服务器 -->
        <div class="add-server-form">
          <h3>添加服务器</h3>
          <form @submit.prevent="addServer">
            <div class="form-group">
              <label>服务器名称</label>
              <input type="text" v-model="newServer.name" required>
            </div>
            <div class="form-group">
              <label>主机地址</label>
              <input type="text" v-model="newServer.host" required>
            </div>
            <div class="form-group">
              <label>用户名</label>
              <input type="text" v-model="newServer.user" required>
            </div>
            <div class="form-group">
              <label>密码</label>
              <input type="password" v-model="newServer.password" required>
            </div>
            <div class="form-group">
              <label>端口</label>
              <input type="number" v-model="newServer.port" required>
            </div>
            <button type="submit" class="btn btn-success">添加</button>
          </form>
        </div>
      </div>
      
      <!-- 终端控制 -->
      <div class="terminal-section">
        <h2>终端控制</h2>
        <div v-if="selectedServer">
          <h3>当前服务器: {{ getServerName(selectedServer) }}</h3>
          <div class="terminal-container">
            <div class="terminal-output">
              <div v-for="(output, index) in terminalOutput" :key="index" class="output-line">
                <span class="prompt">{{ getServerName(selectedServer) }} $</span>
                <span class="command">{{ output.command }}</span>
                <div class="output-content">
                  <pre>{{ output.result.stdout }}</pre>
                  <pre v-if="output.result.stderr" class="error">{{ output.result.stderr }}</pre>
                  <div v-if="output.result.return_code !== 0" class="return-code error">
                    返回码: {{ output.result.return_code }}
                  </div>
                </div>
              </div>
            </div>
            <div class="terminal-input">
              <input 
                type="text" 
                v-model="currentCommand"
                @keyup.enter="executeCommand"
                placeholder="输入命令..."
                class="command-input"
              >
              <button @click="executeCommand" class="btn btn-primary">执行</button>
            </div>
          </div>
        </div>
        <div v-else class="no-server-selected">
          请选择一个已连接的服务器
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'

// 服务器列表
const servers = ref([])
// 已连接的服务器
const connectedServers = ref([])
// 新服务器表单
const newServer = ref({ name: '', host: '', user: '', password: '', port: 22 })
// 选中的服务器
const selectedServer = ref(null)

// 终端输出
const terminalOutput = ref([])
// 当前命令
const currentCommand = ref('')

// API基础URL
const API_BASE = '/api/server-terminal'

// 获取服务器列表
const fetchServers = async () => {
  try {
    const response = await request.get(`${API_BASE}/servers`)
    servers.value = response.data || []
  } catch (error) {
    console.error('获取服务器列表失败:', error)
  }
}

// 添加服务器
const addServer = async () => {
  try {
    const response = await request.post(`${API_BASE}/servers`, {
      serverName: newServer.value.name,
      host: newServer.value.host,
      username: newServer.value.user,
      password: newServer.value.password,
      port: newServer.value.port
    })
    if (response.data) {
      servers.value.push({
        id: response.data.id,
        name: response.data.serverName,
        host: response.data.host,
        user: response.data.username,
        port: response.data.port
      })
      // 重置表单
      newServer.value = { name: '', host: '', user: '', password: '', port: 22 }
    }
  } catch (error) {
    console.error('添加服务器失败:', error)
  }
}

// 删除服务器
const deleteServer = async (serverId) => {
  try {
    const response = await request.delete(`${API_BASE}/servers/${serverId}`)
    if (response.code === 200) {
      servers.value = servers.value.filter(s => s.id !== serverId)
      connectedServers.value = connectedServers.value.filter(id => id !== serverId)
      if (selectedServer.value === serverId) {
        selectedServer.value = null
      }
    }
  } catch (error) {
    console.error('删除服务器失败:', error)
  }
}

// 连接服务器
const connectServer = async (serverId) => {
  try {
    const response = await request.post(`${API_BASE}/servers/${serverId}/connect`)
    if (response.code === 200) {
      connectedServers.value.push(serverId)
      // 自动选中连接的服务器
      if (!selectedServer.value) {
        selectedServer.value = serverId
      }
    } else {
      alert('连接失败: ' + response.message)
    }
  } catch (error) {
    console.error('连接服务器失败:', error)
    alert('连接失败: ' + error.message)
  }
}

// 断开服务器
const disconnectServer = async (serverId) => {
  try {
    const response = await request.post(`${API_BASE}/servers/${serverId}/disconnect`)
    if (response.code === 200) {
      connectedServers.value = connectedServers.value.filter(id => id !== serverId)
      if (selectedServer.value === serverId) {
        selectedServer.value = null
      }
    }
  } catch (error) {
    console.error('断开服务器失败:', error)
  }
}

// 执行命令
const executeCommand = async () => {
  if (!selectedServer.value || !currentCommand.value) return
  
  const command = currentCommand.value
  // 清空输入
  currentCommand.value = ''
  
  try {
    // 发送命令
    const response = await request.post(`${API_BASE}/servers/${selectedServer.value}/execute`, {
      command: command
    })
    
    if (response.code === 200 && response.data) {
      terminalOutput.value.push({
        command,
        result: {
          stdout: response.data.stdout,
          stderr: response.data.stderr,
          return_code: response.data.returnCode,
          completed: true
        }
      })
    }
  } catch (error) {
    console.error('执行命令失败:', error)
  }
}

// 根据ID获取服务器名称
const getServerName = (serverId) => {
  const server = servers.value.find(s => s.id === serverId)
  return server ? server.name : '未知服务器'
}

// 初始化
onMounted(() => {
  fetchServers()
})
</script>

<style scoped>
.server-terminal-view {
  padding: 20px;
}

.page-title {
  color: #333;
  margin-bottom: 30px;
}

.content-container {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 30px;
}

.servers-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.servers-list {
  margin-bottom: 30px;
}

.server-item {
  background: white;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s ease;
}

.server-item.connected {
  border-left: 4px solid #4CAF50;
}

.server-info h3 {
  margin: 0 0 5px 0;
  color: #333;
}

.server-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.server-actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-primary {
  background: #4CAF50;
  color: white;
}

.btn-primary:hover {
  background: #45a049;
}

.btn-danger {
  background: #f44336;
  color: white;
}

.btn-danger:hover {
  background: #da190b;
}

.btn-secondary {
  background: #9e9e9e;
  color: white;
}

.btn-secondary:hover {
  background: #757575;
}

.btn-success {
  background: #2196F3;
  color: white;
}

.btn-success:hover {
  background: #0b7dda;
}

.add-server-form {
  background: white;
  padding: 20px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.add-server-form h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #666;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.terminal-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.terminal-container {
  background: #2d2d2d;
  border-radius: 6px;
  overflow: hidden;
  height: 400px;
  display: flex;
  flex-direction: column;
}

.terminal-output {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  color: #f0f0f0;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.output-line {
  margin-bottom: 15px;
}

.prompt {
  color: #4CAF50;
  font-weight: bold;
  margin-right: 8px;
}

.command {
  color: #f0f0f0;
  margin-right: 8px;
}

.output-content {
  margin-left: 20px;
  margin-top: 5px;
}

.output-content pre {
  margin: 0;
  white-space: pre-wrap;
  color: #f0f0f0;
}

.output-content pre.error {
  color: #f44336;
}

.return-code {
  margin-top: 5px;
  font-size: 12px;
}

.terminal-input {
  display: flex;
  padding: 10px;
  background: #3d3d3d;
  border-top: 1px solid #555;
}

.command-input {
  flex: 1;
  background: #2d2d2d;
  border: 1px solid #555;
  border-radius: 4px;
  padding: 8px 12px;
  color: #f0f0f0;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  margin-right: 10px;
}

.command-input:focus {
  outline: none;
  border-color: #4CAF50;
}

.no-server-selected {
  background: white;
  padding: 40px;
  border-radius: 6px;
  text-align: center;
  color: #666;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

@media (max-width: 768px) {
  .content-container {
    grid-template-columns: 1fr;
  }
}
</style>