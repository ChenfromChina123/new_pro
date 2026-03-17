<template>
  <div class="admin-page">
    <div class="container">
      <div class="page-header">
        <h1>⚙️ 管理后台</h1>
        <p>系统管理和数据统计</p>
      </div>
      
      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card card">
          <div class="stat-icon">
            👥
          </div>
          <div class="stat-info">
            <h3>{{ statistics.totalUsers || 0 }}</h3>
            <p>总用户数</p>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">
            💬
          </div>
          <div class="stat-info">
            <h3>{{ statistics.totalChats || 0 }}</h3>
            <p>对话次数</p>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">
            📁
          </div>
          <div class="stat-info">
            <h3>{{ statistics.totalFiles || 0 }}</h3>
            <p>文件总数</p>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">
            💾
          </div>
          <div class="stat-info">
            <h3>{{ formatSize(statistics.totalStorage || 0) }}</h3>
            <p>存储空间</p>
          </div>
        </div>
      </div>
      
      <!-- 功能选项卡 -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>
      
      <!-- 用户管理 -->
      <div
        v-if="currentTab === 'users'"
        class="tab-content card"
      >
        <div class="content-header">
          <h2>用户管理</h2>
          <div class="search-box">
            <input 
              v-model="userSearchQuery" 
              type="text" 
              placeholder="搜索用户邮箱..."
              class="search-input"
            >
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>邮箱</th>
                <th>注册时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="user in filteredUsers"
                :key="user.id"
              >
                <td>{{ user.id }}</td>
                <td>{{ user.email }}</td>
                <td>{{ formatDate(user.created_at || user.createdAt) }}</td>
                <td>
                  <span :class="['badge', user.active ? 'success' : 'danger']">
                    {{ user.active ? '正常' : '禁用' }}
                  </span>
                </td>
                <td>
                  <button 
                    class="btn-small btn-secondary"
                    style="margin-right: 8px;"
                    @click="viewUserFiles(user.email)"
                  >
                    查看文件
                  </button>
                  <button class="btn-small btn-secondary">
                    详情
                  </button>
                </td>
              </tr>
              <tr v-if="filteredUsers.length === 0">
                <td
                  colspan="5"
                  class="empty-row"
                >
                  未找到匹配的用户
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 文件管理 -->
      <div
        v-if="currentTab === 'files'"
        class="tab-content card"
      >
        <div class="content-header">
          <h2>文件管理</h2>
          <div class="filter-group">
            <div class="user-select-wrapper">
              <select
                v-model="selectedUserEmail"
                class="user-select"
              >
                <option value="">
                  所有用户
                </option>
                <option
                  v-for="user in users"
                  :key="user.id"
                  :value="user.email"
                >
                  {{ user.email }}
                </option>
              </select>
            </div>
            <div class="search-box">
              <input 
                v-model="fileSearchQuery" 
                type="text" 
                placeholder="搜索文件名..."
                class="search-input"
              >
            </div>
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>文件名</th>
                <th>用户</th>
                <th>大小</th>
                <th>上传时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="file in filteredFiles"
                :key="file.id"
              >
                <td>{{ file.filename }}</td>
                <td>{{ file.user_email || file.userEmail }}</td>
                <td>{{ formatSize(file.file_size || file.fileSize) }}</td>
                <td>{{ formatDate(file.upload_time || file.uploadTime) }}</td>
                <td>
                  <button 
                    class="btn-small btn-secondary"
                    style="margin-right: 8px;"
                    @click="handleEditFile(file)"
                  >
                    {{ getActionText(file.filename) }}
                  </button>
                  <button 
                    class="btn-small btn-danger"
                    @click="handleDeleteFile(file.id)"
                  >
                    删除
                  </button>
                </td>
              </tr>
              <tr v-if="filteredFiles.length === 0">
                <td
                  colspan="5"
                  class="empty-row"
                >
                  未找到匹配的文件
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 文件编辑弹窗 -->
      <div 
        v-if="showEditModal" 
        class="modal-overlay"
      >
        <div class="modal-content edit-modal animate-slideIn">
          <div class="modal-header">
            <h3>{{ isBinaryFile ? '预览文件' : '编辑文件' }}: {{ editingFile?.filename }}</h3>
            <button 
              class="close-btn" 
              @click="closeModal"
            >
              &times;
            </button>
          </div>
          <div class="modal-body">
            <!-- 错误状态 -->
            <div 
              v-if="editContent === '获取内容失败'" 
              class="error-state"
            >
              <div class="error-icon">
                ⚠️
              </div>
              <p>获取文件内容失败</p>
              <button 
                class="btn btn-secondary btn-small" 
                @click="handleEditFile(editingFile)"
              >
                重试
              </button>
            </div>

            <!-- 二进制文件预览 (PDF/图片) -->
            <div
              v-else-if="isBinaryFile"
              class="preview-container"
            >
              <!-- 加载状态 -->
              <div
                v-if="isLoadingPreview"
                class="preview-loading"
              >
                <div class="loading-spinner" />
                <span>正在加载预览内容...</span>
              </div>

              <!-- 错误提示 -->
              <div
                v-else-if="editContent && editContent !== '加载中...'"
                class="preview-error"
              >
                <i class="fas fa-exclamation-circle" />
                <span>{{ editContent }}</span>
              </div>

              <template v-else-if="previewUrl && !isLoadingPreview">
                <div
                  v-if="previewType === 'image'"
                  class="image-preview"
                >
                  <img
                    :src="previewUrl"
                    :alt="editingFile?.filename"
                  >
                </div>
                <div
                  v-else-if="previewType === 'pdf'"
                  class="pdf-preview"
                >
                  <iframe 
                    :src="previewUrl" 
                    width="100%" 
                    height="100%"
                    style="border: none;"
                  />
                </div>
                <div
                  v-else-if="previewType === 'word' || previewType === 'other'"
                  class="download-preview"
                >
                  <div class="file-icon-large">
                    {{ previewType === 'word' ? '📄' : '📁' }}
                  </div>
                  <div class="file-info-text">
                    <p class="filename">
                      {{ editingFile?.filename }}
                    </p>
                    <p class="tip">
                      该文件类型不支持直接在线预览，请点击下方按钮下载后查看。
                    </p>
                  </div>
                  <button
                    class="btn btn-primary"
                    @click="downloadFile"
                  >
                    下载查看
                  </button>
                </div>
              </template>
            </div>

            <!-- 文本文件编辑 -->
            <div 
              v-else
              class="editor-container"
              :class="{ loading: editContent === '加载中...' }"
            >
              <textarea 
                v-model="editContent" 
                class="file-editor"
                spellcheck="false"
                placeholder="文件内容加载中..."
                :disabled="editContent === '加载中...'"
              />
              <div
                v-if="editContent === '加载中...'"
                class="editor-loading-overlay"
              >
                <div class="loading" />
                <span>内容加载中...</span>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button 
              class="btn btn-secondary" 
              @click="closeModal"
            >
              {{ isBinaryFile ? '关闭' : '取消' }}
            </button>
            <button 
              v-if="!isBinaryFile"
              class="btn btn-primary" 
              :disabled="saving || editContent === '加载中...' || editContent === '获取内容失败'"
              @click="saveFileContent"
            >
              <span
                v-if="saving"
                class="loading-spinner"
              />
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
      
      <!-- 软件管理 -->
      <div
        v-if="currentTab === 'software'"
        class="tab-content card"
      >
        <div class="content-header">
          <h2>软件版本管理</h2>
          <button
            class="btn btn-primary"
            @click="openSoftwareModal()"
          >
            添加软件
          </button>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>软件名称</th>
                <th>平台</th>
                <th>版本号</th>
                <th>类型</th>
                <th>下载链接/路径</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in softwareList"
                :key="item.id"
              >
                <td>{{ item.title }}</td>
                <td>
                  <span
                    class="badge"
                    :class="item.platform?.toLowerCase() || 'general'"
                  >{{ item.platform || '通用' }}</span>
                </td>
                <td>{{ item.version || '未设置' }}</td>
                <td>
                  <span class="badge secondary">{{ item.type }}</span>
                </td>
                <td>
                  <span
                    class="file-path-cell"
                    :title="item.filePath || item.url"
                  >
                    {{ item.filePath || item.url || '-' }}
                  </span>
                </td>
                <td>{{ formatDate(item.updatedAt || item.created_at) }}</td>
                <td>
                  <button 
                    class="btn-small btn-secondary"
                    style="margin-right: 8px;"
                    @click="openSoftwareModal(item)"
                  >
                    编辑
                  </button>
                  <button 
                    class="btn-small btn-primary"
                    style="margin-right: 8px;"
                    @click="triggerFileUpload(item)"
                  >
                    上传源文件
                  </button>
                  <button 
                    class="btn-small btn-danger"
                    @click="handleDeleteSoftware(item.id)"
                  >
                    删除
                  </button>
                </td>
              </tr>
              <tr v-if="softwareList.length === 0">
                <td
                  colspan="6"
                  class="empty-row"
                >
                  暂无软件信息
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 反馈管理 -->
      <div
        v-if="currentTab === 'feedback'"
        class="tab-content card"
      >
        <h2>用户反馈</h2>
        <div class="feedback-list">
          <div
            v-for="feedback in feedbacks"
            :key="feedback.id"
            class="feedback-item"
          >
            <div class="feedback-header">
              <span class="feedback-user">{{ feedback.user_email || feedback.userEmail }}</span>
              <span class="feedback-date">{{ formatDate(feedback.created_at || feedback.createdAt) }}</span>
            </div>
            <p class="feedback-content">
              {{ feedback.content }}
            </p>
            <button class="btn-small btn-secondary">
              标记已处理
            </button>
          </div>
        </div>
      </div>

      <!-- 外部链接管理 -->
      <div
        v-if="currentTab === 'externalLinks'"
        class="tab-content card"
      >
        <div class="content-header">
          <h2>🔗 外部链接管理</h2>
          <button 
            class="btn-primary" 
            @click="openLinkModal()"
          >
            <i class="fas fa-plus" /> 新增链接
          </button>
        </div>
        
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>链接</th>
                <th>点击次数</th>
                <th>状态</th>
                <th>排序</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="link in externalLinks"
                :key="link.id"
              >
                <td>{{ link.id }}</td>
                <td>{{ link.title }}</td>
                <td>
                  <a 
                    :href="link.url" 
                    target="_blank" 
                    class="link-url"
                  >
                    {{ truncateUrl(link.url) }}
                  </a>
                </td>
                <td>{{ link.clickCount || 0 }}</td>
                <td>
                  <span 
                    class="status-badge" 
                    :class="link.isActive ? 'active' : 'inactive'"
                  >
                    {{ link.isActive ? '激活' : '禁用' }}
                  </span>
                </td>
                <td>{{ link.sortOrder || '-' }}</td>
                <td>
                  <button 
                    class="btn-small btn-primary" 
                    @click="openLinkModal(link)"
                  >
                    编辑
                  </button>
                  <button 
                    class="btn-small btn-danger" 
                    @click="deleteLink(link.id)"
                  >
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Token 审计 -->
      <div
        v-if="currentTab === 'tokenAudit'"
        class="tab-content card"
      >
        <div class="content-header">
          <h2>🔍 Token 消耗审计</h2>
          <p style="color: var(--text-secondary); margin-top: 8px;">
            实时监控 AI Token 消耗与成本分析
          </p>
        </div>

        <!-- 统计卡片组 -->
        <div class="token-stats-grid">
          <div class="token-stat-card">
            <div class="token-stat-icon">
              📊
            </div>
            <div class="token-stat-info">
              <h4>总请求数</h4>
              <p class="token-stat-value">
                {{ tokenStats.totalRequests || 0 }}
              </p>
            </div>
          </div>
          <div class="token-stat-card">
            <div class="token-stat-icon">
              ⬆️
            </div>
            <div class="token-stat-info">
              <h4>输入 Token</h4>
              <p class="token-stat-value">
                {{ formatNumber(tokenStats.totalInputTokens) }}
              </p>
            </div>
          </div>
          <div class="token-stat-card">
            <div class="token-stat-icon">
              ⬇️
            </div>
            <div class="token-stat-info">
              <h4>输出 Token</h4>
              <p class="token-stat-value">
                {{ formatNumber(tokenStats.totalOutputTokens) }}
              </p>
            </div>
          </div>
          <div class="token-stat-card">
            <div class="token-stat-icon">
              💰
            </div>
            <div class="token-stat-info">
              <h4>总 Token 数</h4>
              <p class="token-stat-value">
                {{ formatNumber(tokenStats.totalTokens) }}
              </p>
            </div>
          </div>
          <div class="token-stat-card">
            <div class="token-stat-icon">
              ⚡
            </div>
            <div class="token-stat-info">
              <h4>平均耗时</h4>
              <p class="token-stat-value">
                {{ tokenStats.avgResponseTimeMs || 0 }}ms
              </p>
            </div>
          </div>
        </div>

        <!-- 提供方分布 -->
        <div class="token-provider-section">
          <h3>API 提供方分布</h3>
          <div class="provider-list">
            <div 
              v-for="(count, provider) in tokenStats.byProvider" 
              :key="provider"
              class="provider-item"
            >
              <span class="provider-name">{{ provider }}</span>
              <div class="provider-bar-container">
                <div 
                  class="provider-bar" 
                  :style="{ width: (count / tokenStats.totalRequests * 100) + '%' }"
                />
              </div>
              <span class="provider-count">{{ count }} 次 ({{ Math.round(count / tokenStats.totalRequests * 100) }}%)</span>
            </div>
          </div>
        </div>

        <!-- 审计记录 -->
        <div class="token-records-section">
          <h3>最近审计记录 (7天)</h3>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>提供方</th>
                  <th>模型</th>
                  <th>输入 Token</th>
                  <th>输出 Token</th>
                  <th>总计</th>
                  <th>耗时(ms)</th>
                  <th>流式</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="record in tokenRecords"
                  :key="record.id"
                >
                  <td>{{ formatDateTime(record.createdAt) }}</td>
                  <td><span class="badge">{{ record.provider }}</span></td>
                  <td>{{ record.modelName || '-' }}</td>
                  <td>{{ formatNumber(record.inputTokens) }}</td>
                  <td>{{ formatNumber(record.outputTokens) }}</td>
                  <td><strong>{{ formatNumber(record.totalTokens) }}</strong></td>
                  <td>{{ record.responseTimeMs }}</td>
                  <td>{{ record.streaming ? '✓' : '✗' }}</td>
                </tr>
                <tr v-if="tokenRecords.length === 0">
                  <td
                    colspan="8"
                    class="empty-row"
                  >
                    暂无审计记录
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 软件编辑弹窗 -->
      <div 
        v-if="showSoftwareModal" 
        class="modal-overlay"
      >
        <div class="modal-content animate-slideIn">
          <div class="modal-header">
            <h3>{{ isEdit ? '编辑软件' : '添加软件' }}</h3>
            <button
              class="close-btn"
              @click="showSoftwareModal = false"
            >
              &times;
            </button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>软件名称</label>
              <input
                v-model="editingSoftware.title"
                type="text"
                class="form-input"
                placeholder="例如: CodeNova"
              >
            </div>
            <div class="form-group">
              <label>运行平台</label>
              <select
                v-model="editingSoftware.platform"
                class="form-input"
              >
                <option value="">
                  通用
                </option>
                <option value="Windows">
                  Windows
                </option>
                <option value="Linux">
                  Linux
                </option>
                <option value="Android">
                  Android
                </option>
                <option value="iOS">
                  iOS
                </option>
                <option value="macOS">
                  macOS
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>版本号</label>
              <input
                v-model="editingSoftware.version"
                type="text"
                class="form-input"
                placeholder="例如: v1.0.0"
              >
            </div>
            <div class="form-group">
              <label>类型</label>
              <select
                v-model="editingSoftware.type"
                class="form-input"
              >
                <option value="software">
                  软件/安装包
                </option>
                <option value="source">
                  源代码
                </option>
                <option value="article">
                  文档/文章
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>外部链接 (可选)</label>
              <input
                v-model="editingSoftware.url"
                type="text"
                class="form-input"
                placeholder="https://..."
              >
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea
                v-model="editingSoftware.description"
                class="form-input"
                rows="3"
              />
            </div>
          </div>
          <div class="modal-footer">
            <button
              class="btn btn-secondary"
              @click="showSoftwareModal = false"
            >
              取消
            </button>
            <button
              class="btn btn-primary"
              @click="saveSoftware"
            >
              保存
            </button>
          </div>
        </div>
      </div>

      <!-- 隐藏的文件上传输入框 -->
      <input 
        ref="fileInput" 
        type="file" 
        style="display: none" 
        @change="handleFileUpload"
      >

      <!-- 上传进度弹窗 -->
      <div
        v-if="uploading"
        class="modal-overlay upload-progress-overlay"
      >
        <div class="modal-content progress-modal">
          <div class="modal-header">
            <h3>正在上传文件...</h3>
          </div>
          <div class="modal-body">
            <div class="progress-info">
              <span class="filename">{{ uploadingFileName }}</span>
              <span class="percentage">{{ uploadProgress }}%</span>
            </div>
            <div class="progress-bar-container">
              <div
                class="progress-bar"
                :style="{ width: uploadProgress + '%' }"
              />
            </div>
            <p class="upload-tip">
              请勿关闭页面，等待上传完成...
            </p>
          </div>
        </div>
      </div>

      <!-- 外部链接编辑弹窗 -->
      <div 
        v-if="showLinkModal" 
        class="modal-overlay"
      >
        <div class="modal-content animate-slideIn">
          <div class="modal-header">
            <h3>{{ isEditLink ? '编辑链接' : '新增链接' }}</h3>
            <button 
              class="close-btn" 
              @click="showLinkModal = false"
            >
              &times;
            </button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>标题 <span style="color: red;">*</span></label>
              <input 
                v-model="editingLink.title" 
                type="text" 
                class="form-input" 
                placeholder="请输入链接标题"
              >
            </div>
            <div class="form-group">
              <label>链接地址 <span style="color: red;">*</span></label>
              <input 
                v-model="editingLink.url" 
                type="url" 
                class="form-input" 
                placeholder="https://..."
              >
            </div>
            <div class="form-group">
              <label>描述（可选）</label>
              <textarea 
                v-model="editingLink.description" 
                class="form-input" 
                rows="3" 
                placeholder="请输入链接描述"
              />
            </div>
            <div class="form-group">
              <label>图片URL（可选）</label>
              <input 
                v-model="editingLink.imageUrl" 
                type="url" 
                class="form-input" 
                placeholder="https://..."
              >
            </div>
            <div class="form-group">
              <label>排序顺序（可选）</label>
              <input 
                v-model.number="editingLink.sortOrder" 
                type="number" 
                class="form-input" 
                placeholder="数字越小越靠前"
              >
            </div>
            <div class="form-group">
              <label>
                <input 
                  v-model="editingLink.isActive" 
                  type="checkbox"
                >
                激活状态
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button 
              class="btn-secondary" 
              @click="showLinkModal = false"
            >
              取消
            </button>
            <button 
              class="btn-primary" 
              @click="saveLink"
            >
              保存
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'

const statistics = ref({})
const tokenStats = ref({
  totalRequests: 0,
  totalInputTokens: 0,
  totalOutputTokens: 0,
  totalTokens: 0,
  avgResponseTimeMs: 0,
  byProvider: {},
  byModel: {}
})
const tokenRecords = ref([])
const users = ref([])
const files = ref([])
const feedbacks = ref([])
const currentTab = ref('users')
const showEditModal = ref(false)
const editingFile = ref(null)
const editContent = ref('')
const saving = ref(false)
const fileSearchQuery = ref('')
const userSearchQuery = ref('')
const selectedUserEmail = ref('')
const previewUrl = ref('')
const previewType = ref('')
const isLoadingPreview = ref(false)

const isBinaryFile = computed(() => {
  if (!editingFile.value) return false
  const filename = editingFile.value.filename.toLowerCase()
  return filename.endsWith('.pdf') || 
         filename.endsWith('.doc') || 
         filename.endsWith('.docx') || 
         filename.endsWith('.png') || 
         filename.endsWith('.jpg') || 
         filename.endsWith('.jpeg') || 
         filename.endsWith('.gif')
})

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'files', label: '文件管理' },
  { key: 'software', label: '软件管理' },
  { key: 'externalLinks', label: '外部链接' },
  { key: 'feedback', label: '反馈管理' },
  { key: 'tokenAudit', label: 'Token 审计' }
]

// 过滤用户列表
const filteredUsers = computed(() => {
  if (!userSearchQuery.value) return users.value
  const query = userSearchQuery.value.toLowerCase()
  return users.value.filter(user => 
    user.email?.toLowerCase().includes(query)
  )
})

// 过滤文件列表
const filteredFiles = computed(() => {
  let result = files.value
  
  // 按选择的用户邮箱筛选
  if (selectedUserEmail.value) {
    result = result.filter(file => (file.user_email || file.userEmail) === selectedUserEmail.value)
  }
  
  // 按搜索关键词筛选
  if (fileSearchQuery.value) {
    const query = fileSearchQuery.value.toLowerCase()
    result = result.filter(file => 
      file.filename?.toLowerCase().includes(query) || 
      (file.user_email || file.userEmail)?.toLowerCase().includes(query)
    )
  }
  
  return result
})

// 监听标签页切换
watch(currentTab, (newTab) => {
  if (newTab === 'users') fetchUsers()
  if (newTab === 'files') fetchFiles()
  if (newTab === 'feedback') fetchFeedbacks()
  if (newTab === 'externalLinks') fetchExternalLinks()
  if (newTab === 'tokenAudit') loadTokenAuditData()
})

// 软件管理相关
const softwareList = ref([])
const showSoftwareModal = ref(false)
const editingSoftware = ref({
  title: '',
  version: '',
  type: 'software',
  platform: '',
  url: '',
  description: '',
  categoryName: '软件项目'
})
const isEdit = ref(false)
const fileInput = ref(null)
const currentSoftwareId = ref(null)

// 上传进度相关
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadingFileName = ref('')

// 外部链接管理相关
const externalLinks = ref([])
const showLinkModal = ref(false)
const isEditLink = ref(false)
const editingLink = ref({
  id: null,
  title: '',
  url: '',
  description: '',
  imageUrl: '',
  sortOrder: null,
  isActive: true
})

// 加载外部链接列表
const fetchExternalLinks = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.externalLinks)
    externalLinks.value = response.data || []
  } catch (error) {
    console.error('加载外部链接列表失败:', error)
    alert('加载外部链接列表失败')
  }
}

// 打开链接编辑弹窗
const openLinkModal = (link = null) => {
  if (link) {
    editingLink.value = { ...link }
    isEditLink.value = true
  } else {
    editingLink.value = {
      id: null,
      title: '',
      url: '',
      description: '',
      imageUrl: '',
      sortOrder: null,
      isActive: true
    }
    isEditLink.value = false
  }
  showLinkModal.value = true
}

// 保存链接
const saveLink = async () => {
  if (!editingLink.value.title || !editingLink.value.url) {
    alert('请填写标题和链接地址')
    return
  }

  try {
    if (isEditLink.value) {
      await request.put(`${API_ENDPOINTS.admin.externalLinks}/${editingLink.value.id}`, editingLink.value)
      alert('更新成功')
    } else {
      await request.post(API_ENDPOINTS.admin.externalLinks, editingLink.value)
      alert('添加成功')
    }
    showLinkModal.value = false
    fetchExternalLinks()
  } catch (error) {
    console.error('保存链接失败:', error)
    alert('保存失败: ' + (error.response?.data?.message || error.message))
  }
}

// 删除链接
const deleteLink = async (id) => {
  if (!confirm('确定要删除这个链接吗？')) return

  try {
    await request.delete(`${API_ENDPOINTS.admin.externalLinks}/${id}`)
    alert('删除成功')
    fetchExternalLinks()
  } catch (error) {
    console.error('删除链接失败:', error)
    alert('删除失败: ' + (error.response?.data?.message || error.message))
  }
}

// 截断URL显示
const truncateUrl = (url) => {
  if (!url) return ''
  return url.length > 50 ? url.substring(0, 50) + '...' : url
}

// 加载软件列表
const fetchSoftwareList = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.resources, {
      params: { categoryName: '软件项目' }
    })
    softwareList.value = response.data || []
  } catch (error) {
    console.error('加载软件列表失败:', error)
  }
}

// 打开软件编辑弹窗
const openSoftwareModal = (item = null) => {
  if (item) {
    editingSoftware.value = { ...item, categoryName: item.category?.name || '软件项目' }
    isEdit.value = true
  } else {
    editingSoftware.value = {
      title: '',
      version: '',
      type: 'software',
      platform: '',
      url: '',
      description: '',
      categoryName: '软件项目'
    }
    isEdit.value = false
  }
  showSoftwareModal.value = true
}

// 保存软件信息
const saveSoftware = async () => {
  try {
    if (isEdit.value) {
      await request.put(API_ENDPOINTS.admin.updateResource(editingSoftware.value.id), editingSoftware.value)
    } else {
      await request.post(API_ENDPOINTS.admin.resources, editingSoftware.value)
    }
    showSoftwareModal.value = false
    fetchSoftwareList()
  } catch (error) {
    console.error('保存软件失败:', error)
    alert('保存失败')
  }
}

// 删除软件
const handleDeleteSoftware = async (id) => {
  if (!confirm('确定要删除该软件吗？')) return
  try {
    await request.delete(API_ENDPOINTS.admin.updateResource(id))
    fetchSoftwareList()
  } catch (error) {
    console.error('删除软件失败:', error)
  }
}

// 触发文件上传
const triggerFileUpload = (item) => {
  currentSoftwareId.value = item.id
  fileInput.value.click()
}

/**
 * 处理文件上传
 * @param {Event} event - 文件输入框变更事件
 */
const handleFileUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  uploading.value = true
  uploadProgress.value = 0
  uploadingFileName.value = file.name

  try {
    await request.post(API_ENDPOINTS.admin.uploadSoftware(currentSoftwareId.value), formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000, // 设置 10 分钟超时
      onUploadProgress: (progressEvent) => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        uploadProgress.value = percentCompleted
      }
    })
    alert('上传成功')
    fetchSoftwareList()
  } catch (error) {
    console.error('上传失败:', error)
    if (error.code === 'ECONNABORTED') {
      alert('上传超时，文件可能过大，请稍后重试或联系管理员')
    } else {
      alert('上传失败: ' + (error.response?.data?.message || error.message))
    }
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    uploadingFileName.value = ''
    event.target.value = ''
  }
}

onMounted(() => {
  fetchStatistics()
  fetchUsers()
  fetchFiles()
  fetchFeedbacks()
  fetchSoftwareList()
  loadTokenAuditData()
})

const fetchStatistics = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.statistics)
    statistics.value = response.data || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchUsers = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.users)
    users.value = response.data?.users || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchFiles = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.files)
    files.value = response.data || []
  } catch (error) {
    console.error('获取文件列表失败:', error)
  }
}

const fetchFeedbacks = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.feedback.admin.list)
    feedbacks.value = response.data || []
  } catch (error) {
    console.error('获取反馈列表失败:', error)
  }
}

const viewUserFiles = (email) => {
  selectedUserEmail.value = email
  currentTab.value = 'files'
}

const handleEditFile = async (file) => {
  editingFile.value = file
  showEditModal.value = true
  editContent.value = '加载中...'
  previewUrl.value = ''
  
  const filename = file.filename.toLowerCase()
  if (filename.endsWith('.pdf')) {
    previewType.value = 'pdf'
    await loadBinaryPreview(file.id)
  } else if (filename.match(/\.(jpg|jpeg|png|gif)$/)) {
    previewType.value = 'image'
    await loadBinaryPreview(file.id)
  } else if (filename.endsWith('.doc') || filename.endsWith('.docx')) {
    previewType.value = 'word'
    editContent.value = '' // 不显示文本内容
    previewUrl.value = 'word-file' // 设置标记以显示下载预览
    isLoadingPreview.value = false
  } else if (isBinaryFile.value) {
    previewType.value = 'other'
    editContent.value = ''
    previewUrl.value = 'other-file' // 设置标记以显示下载预览
    isLoadingPreview.value = false
  } else {
    previewType.value = 'text'
    try {
      const response = await request.get(API_ENDPOINTS.admin.getFileContent(file.id))
      editContent.value = response.data || ''
    } catch (error) {
      console.error('获取文件内容失败:', error)
      editContent.value = '获取内容失败'
    }
  }
}

const loadBinaryPreview = async (fileId) => {
  try {
    editContent.value = ''
    isLoadingPreview.value = true
    previewUrl.value = ''
    
    // 使用 mode=inline 确保后端返回 inline disposition，方便在 iframe 中预览
    const response = await request.get(`${API_ENDPOINTS.admin.downloadFile(fileId)}?mode=inline`, {
      responseType: 'blob',
      transformResponse: [(data) => data]
    })
    
    const blobData = response.data || response;
    
    if (!(blobData instanceof Blob)) {
      console.error('响应不是 Blob 类型:', blobData)
      editContent.value = '获取内容失败：服务器返回格式错误'
      return
    }

    // 检查是否是 JSON 错误信息 (如果 Blob 的 type 是 application/json)
    if (blobData.type === 'application/json') {
      const text = await blobData.text()
      try {
        const errorData = JSON.parse(text)
        editContent.value = `获取内容失败: ${errorData.message || errorData.detail || '未知错误'}`
        return
      } catch {
        // 不是 JSON，继续按文件处理
      }
    }

    // 确保正确的 MIME 类型，否则浏览器可能无法正确渲染 PDF 或图片
    const filename = editingFile.value?.filename?.toLowerCase() || ''
    let mimeType = blobData.type
    if (filename.endsWith('.pdf')) {
      mimeType = 'application/pdf'
    } else if (filename.endsWith('.jpg') || filename.endsWith('.jpeg')) {
      mimeType = 'image/jpeg'
    } else if (filename.endsWith('.png')) {
      mimeType = 'image/png'
    } else if (filename.endsWith('.gif')) {
      mimeType = 'image/gif'
    }

    const blob = new Blob([blobData], { type: mimeType })
    previewUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('加载预览失败:', error)
    let errorMessage = '获取内容失败：' + (error.message || '网络连接失败')
    
    if (error.response && error.response.data instanceof Blob) {
      try {
        const text = await error.response.data.text()
        const errorData = JSON.parse(text)
        errorMessage = `获取内容失败: ${errorData.message || errorData.detail || '服务器内部错误'}`
      } catch {}
    } else if (error.response?.data?.message) {
      errorMessage = `获取内容失败: ${error.response.data.message}`
    }
    
    editContent.value = errorMessage
  } finally {
    isLoadingPreview.value = false
  }
}

const downloadFile = async () => {
  if (!editingFile.value) return
  
  try {
    const response = await request.get(API_ENDPOINTS.admin.downloadFile(editingFile.value.id), {
      responseType: 'blob',
      transformResponse: [(data) => data]
    })
    
    const blobData = response.data || response;
    
    if (blobData.type === 'application/json') {
      const text = await blobData.text()
      try {
        const errorData = JSON.parse(text)
        alert('下载失败: ' + (errorData.message || errorData.detail || '未知错误'))
        return
      } catch {}
    }

    const downloadUrl = URL.createObjectURL(new Blob([blobData]))
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = editingFile.value.filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(downloadUrl)
  } catch (error) {
    console.error('下载文件失败:', error)
    let errorMessage = '下载文件失败：' + (error.message || '网络错误')
    
    if (error.response && error.response.data instanceof Blob) {
      try {
        const text = await error.response.data.text()
        const errorData = JSON.parse(text)
        errorMessage = '下载失败: ' + (errorData.message || errorData.detail || '服务器错误')
      } catch {}
    } else if (error.response?.data?.message) {
      errorMessage = '下载失败: ' + error.response.data.message
    }
    
    alert(errorMessage)
  }
}

const closeModal = () => {
  showEditModal.value = false
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
  editingFile.value = null
  editContent.value = ''
}

const saveFileContent = async () => {
  if (!editingFile.value) return
  
  saving.value = true
  try {
    await request.put(API_ENDPOINTS.admin.updateFileContent(editingFile.value.id), {
      content: editContent.value
    })
    showEditModal.value = false
    alert('文件保存成功')
    await fetchFiles()
  } catch (error) {
    console.error('保存文件失败:', error)
    alert('保存失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

const handleDeleteFile = async (fileId) => {
  if (!confirm('确定要删除这个文件吗？')) return
  
  try {
    await request.delete(API_ENDPOINTS.cloudDisk.delete(fileId))
    await fetchFiles()
    await fetchStatistics()
  } catch (error) {
    console.error('删除文件失败:', error)
    alert('删除失败')
  }
}

const getActionText = (filename) => {
  if (!filename) return '查看'
  const name = filename.toLowerCase()
  if (name.endsWith('.pdf') || name.match(/\.(jpg|jpeg|png|gif)$/)) return '预览'
  if (name.endsWith('.doc') || name.endsWith('.docx')) return '查看'
  // 文本文件
  const textExtensions = ['.txt', '.md', '.js', '.css', '.html', '.json', '.vue', '.java', '.py', '.sql']
  if (textExtensions.some(ext => name.endsWith(ext))) return '编辑'
  return '查看'
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatNumber = (num) => {
  if (!num) return '0'
  return num.toLocaleString()
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 加载 Token 审计数据
const loadTokenAuditData = async () => {
  try {
    console.log('🔍 开始加载 Token 审计数据...')
    console.log('📡 API 端点 - stats:', API_ENDPOINTS.admin.tokenAuditStats)
    console.log('📡 API 端点 - records:', API_ENDPOINTS.admin.tokenAuditRecords)
    
    // 获取统计数据
    const statsRes = await request.get(API_ENDPOINTS.admin.tokenAuditStats)
    console.log('✅ 统计数据响应:', statsRes)
    console.log('✅ statsRes.data:', statsRes.data)
    
    // 由于响应拦截器返回了 response.data，所以 statsRes 就是 ApiResponse
    // statsRes = { code: 200, message: "...", data: {...} }
    if (statsRes && statsRes.data) {
      tokenStats.value = statsRes.data
      console.log('✅ tokenStats 更新为:', tokenStats.value)
    } else {
      console.warn('⚠️ statsRes.data 为空:', statsRes)
    }
    
    // 获取记录列表
    const recordsRes = await request.get(API_ENDPOINTS.admin.tokenAuditRecords, {
      params: { page: 0, size: 50 }
    })
    console.log('✅ 记录列表响应:', recordsRes)
    console.log('✅ recordsRes.data:', recordsRes.data)
    
    if (recordsRes && recordsRes.data) {
      tokenRecords.value = recordsRes.data
      console.log('✅ tokenRecords 更新为:', tokenRecords.value)
    } else {
      console.warn('⚠️ recordsRes.data 为空:', recordsRes)
    }
  } catch (error) {
    console.error('❌ 加载 Token 审计数据失败:', error)
    console.error('错误详情:', error.response || error.message)
  }
}

</script>

<style scoped>
.admin-page {
  height: 100%;
  overflow-y: auto;
  padding: 32px 0;
  background-color: var(--bg-primary);
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .admin-page {
    padding: 16px;
  }

  .page-header {
    margin-bottom: 24px;
  }

  .page-header h1 {
    font-size: 24px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .stat-card {
    padding: 16px;
  }
  
  .stat-icon {
    width: 60px;
    height: 60px;
    font-size: 32px;
  }
  
  .stat-info h3 {
    font-size: 24px;
  }

  .tabs {
    overflow-x: auto;
    white-space: nowrap;
    padding-bottom: 4px; /* Hide scrollbar potentially or give space */
    -webkit-overflow-scrolling: touch;
  }

  .tab-btn {
    padding: 10px 16px;
    font-size: 14px;
    flex-shrink: 0;
  }
  
  .data-table th, 
  .data-table td {
    padding: 8px;
    font-size: 12px;
  }
  
  /* Hide less important columns on mobile if needed, or just rely on scroll */
  
  .modal-content {
    width: 95%;
    max-height: 85vh;
  }
  
  .modal-header,
  .modal-body,
  /* 预览容器样式 */
.preview-container {
  height: 500px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: var(--input-bg);
  border-radius: 8px;
  overflow: hidden;
}

.image-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.pdf-preview {
  width: 100%;
  height: 100%;
}

.pdf-preview iframe {
  border: none;
}

.download-preview {
  text-align: center;
  padding: 40px;
}

.file-icon-large {
  font-size: 64px;
  margin-bottom: 20px;
}

.download-preview p {
  margin-bottom: 8px;
  font-weight: 500;
}

.modal-footer {
    padding: 16px;
  }
  
  .editor-container {
    height: 300px;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  font-size: 48px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
  border-radius: 12px;
}

.stat-info h3 {
  font-size: 32px;
  margin: 0 0 4px 0;
  color: var(--primary-color);
}

.stat-info p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--border-color);
}

.tab-btn {
  padding: 12px 24px;
  border: none;
  background: none;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  position: relative;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  color: var(--primary-color);
}

.tab-btn.active {
  color: var(--primary-color);
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
}

.tab-content h2 {
  font-size: 20px;
  margin-bottom: 0;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-select-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.user-select-wrapper::after {
  content: '▼';
  font-size: 10px;
  color: var(--text-secondary);
  position: absolute;
  right: 12px;
  pointer-events: none;
}

.user-select {
  padding: 8px 32px 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background-color: var(--input-bg);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  cursor: pointer;
  min-width: 200px;
  transition: all 0.3s ease;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
}

.user-select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

.user-select:hover {
  border-color: var(--gray-300);
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 上传进度条样式 */
.upload-progress-overlay {
  z-index: 2000;
}

.progress-modal {
  max-width: 400px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-info .filename {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 250px;
}

.progress-info .percentage {
  font-weight: 600;
  color: var(--primary-color);
  font-size: 16px;
}

.progress-bar-container {
  width: 100%;
  height: 8px;
  background-color: var(--border-color);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 16px;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--gradient-start), var(--gradient-end));
  transition: width 0.3s ease;
}

.upload-tip {
  text-align: center;
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}
.search-box {
  width: 250px;
}

.search-input {
  width: 100%;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background-color: var(--input-bg);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

.empty-row {
  text-align: center;
  padding: 32px !important;
  color: var(--text-secondary);
  font-style: italic;
}

.file-path-cell {
  display: inline-block;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.badge.windows {
  background-color: rgba(0, 120, 215, 0.1);
  color: #0078d7;
}

.badge.linux {
  background-color: rgba(240, 180, 0, 0.1);
  color: #f0b400;
}

.badge.android {
  background-color: rgba(164, 198, 57, 0.1);
  color: #a4c639;
}

.badge.ios {
  background-color: rgba(0, 0, 0, 0.1);
  color: var(--text-primary);
}

.badge.general {
  background-color: rgba(100, 116, 139, 0.1);
  color: #64748b;
}

.badge.secondary {
  background: rgba(100, 108, 255, 0.1);
  color: #646cff;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.6rem;
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.9rem;
  opacity: 0.9;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 0.95rem;
  transition: all 0.2s;
  box-sizing: border-box;
  height: 42px;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

select.form-input {
  appearance: none;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%236b7280'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 16px;
  padding-right: 40px;
}

/* 适配不同浏览器的选择框内容垂直居中 */
select.form-input {
  display: block;
  padding-top: 0;
  padding-bottom: 0;
  line-height: 42px;
}

select.form-input option {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

textarea.form-input {
  height: auto;
  min-height: 100px;
  resize: vertical;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.data-table th,
.data-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.data-table th {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background-color: var(--bg-secondary);
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table tr:hover td {
  background-color: rgba(var(--primary-rgb), 0.02);
}

.data-table td {
  font-size: 14px;
  color: var(--text-primary);
  transition: background-color 0.2s ease;
}

.badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge.success {
  background-color: rgba(39, 174, 96, 0.1);
  color: var(--success-color);
}

.badge.danger {
  background-color: rgba(231, 76, 60, 0.1);
  color: var(--danger-color);
}

.btn-small {
  padding: 6px 12px;
  font-size: 12px;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-item {
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.feedback-user {
  font-weight: 500;
}

.feedback-date {
  color: var(--text-secondary);
}

.feedback-content {
  margin-bottom: 12px;
  line-height: 1.6;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.modal-content {
  background-color: var(--bg-secondary);
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.edit-modal {
  max-width: 900px;
}

.modal-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--bg-secondary);
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80%;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: var(--danger-color);
}

.modal-body {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.edit-modal .modal-body {
  padding: 0;
  display: flex;
  flex-direction: column;
  min-height: 400px;
}

.preview-container {
  flex: 1;
  width: 100%;
  min-height: 500px;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  position: relative;
  overflow: hidden;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  gap: 12px;
  color: #64748b;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.preview-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  gap: 12px;
  color: #ef4444;
  padding: 20px;
  text-align: center;
}

.preview-error i {
  font-size: 32px;
  margin-bottom: 8px;
}

.image-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.pdf-preview {
  width: 100%;
  height: 600px;
  border: none;
}

.download-preview {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 40px;
  background-color: var(--bg-primary);
}

.file-icon-large {
  font-size: 64px;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.1));
}

.file-info-text {
  text-align: center;
}

.file-info-text .filename {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.file-info-text .tip {
  color: var(--text-secondary);
  font-size: 14px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 100%;
  color: var(--text-secondary);
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--input-bg);
}

.file-editor {
  flex: 1;
  width: 100%;
  padding: 20px;
  border: none;
  resize: none;
  background-color: transparent;
  color: var(--text-primary);
  font-family: 'Fira Code', 'Cascadia Code', 'Source Code Pro', monospace;
  font-size: 14px;
  line-height: 1.6;
  outline: none;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background-color: var(--bg-secondary);
}

/* Token 审计样式 */
.token-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.token-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, var(--bg-secondary) 0%, var(--bg-primary) 100%);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.token-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.token-stat-icon {
  font-size: 32px;
  opacity: 0.8;
}

.token-stat-info h4 {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 4px 0;
  font-weight: 500;
}

.token-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.token-provider-section,
.token-records-section {
  margin-top: 32px;
}

.token-provider-section h3,
.token-records-section h3 {
  font-size: 18px;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.provider-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.provider-item {
  display: grid;
  grid-template-columns: 120px 1fr 120px;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.provider-name {
  font-weight: 600;
  color: var(--text-primary);
  text-transform: capitalize;
}

.provider-bar-container {
  height: 8px;
  background: var(--bg-primary);
  border-radius: 4px;
  overflow: hidden;
}

.provider-bar {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.provider-count {
  text-align: right;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 外部链接管理样式 */
.link-url {
  color: var(--primary-color);
  text-decoration: none;
  transition: color 0.2s;
}

.link-url:hover {
  color: var(--primary-hover);
  text-decoration: underline;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.active {
  background-color: #d4edda;
  color: #155724;
}

.status-badge.inactive {
  background-color: #f8d7da;
  color: #721c24;
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .status-badge.active {
    background-color: #1e4620;
    color: #7dff8a;
  }

  .status-badge.inactive {
    background-color: #5a1a1a;
    color: #ff8a8a;
  }
}

</style>

