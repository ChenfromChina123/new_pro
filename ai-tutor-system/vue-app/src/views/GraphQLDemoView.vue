<!-- 
  GraphQL 数据分片加载演示页面
  展示如何使用 Fragments 实现按需加载
-->
<template>
  <div class="graphql-demo-view">
    <div class="demo-header">
      <h1>GraphQL 数据分片加载演示</h1>
      <p class="subtitle">
        基于 Spring for GraphQL 的声明式按需加载
      </p>
    </div>

    <div class="demo-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        :class="['tab-btn', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 场景 1：列表页（最小字段） -->
    <div
      v-if="activeTab === 'list-basic'"
      class="demo-section"
    >
      <div class="section-header">
        <h2>场景 1：列表页 - 只加载基础字段</h2>
        <button
          :disabled="loading"
          class="btn-primary"
          @click="loadBasicList"
        >
          {{ loading ? '加载中...' : '加载数据' }}
        </button>
      </div>
      
      <div class="code-block">
        <h3>GraphQL 查询语句</h3>
        <pre><code>{{ basicQueryCode }}</code></pre>
      </div>

      <div
        v-if="basicList.length > 0"
        class="result-grid"
      >
        <div
          v-for="doc in basicList"
          :key="doc.id"
          class="doc-card basic"
        >
          <h3>{{ doc.title }}</h3>
          <p>版本: {{ doc.version }}</p>
          <p>创建时间: {{ formatDate(doc.createdAt) }}</p>
        </div>
      </div>

      <div class="performance-info">
        <p>✅ 优势：每条记录约 200 bytes，列表加载极快</p>
        <p>📊 性能：100 条记录仅需传输 ~20KB</p>
      </div>
    </div>

    <!-- 场景 2：列表页（带用户信息） -->
    <div
      v-if="activeTab === 'list-with-user'"
      class="demo-section"
    >
      <div class="section-header">
        <h2>场景 2：列表页 - 带作者信息（DataLoader 批量加载）</h2>
        <button
          :disabled="loading"
          class="btn-primary"
          @click="loadListWithUser"
        >
          {{ loading ? '加载中...' : '加载数据' }}
        </button>
      </div>

      <div class="code-block">
        <h3>GraphQL 查询语句</h3>
        <pre><code>{{ withUserQueryCode }}</code></pre>
      </div>

      <div
        v-if="listWithUser.length > 0"
        class="result-grid"
      >
        <div
          v-for="doc in listWithUser"
          :key="doc.id"
          class="doc-card with-user"
        >
          <div class="doc-header">
            <h3>{{ doc.title }}</h3>
            <div
              v-if="doc.user"
              class="user-info"
            >
              <img
                :src="doc.user.avatar || '/default-avatar.png'"
                alt="头像"
              >
              <span>{{ doc.user.username }}</span>
            </div>
          </div>
          <p>版本: {{ doc.version }}</p>
          <p>创建时间: {{ formatDate(doc.createdAt) }}</p>
        </div>
      </div>

      <div class="performance-info">
        <p>✅ 优势：使用 DataLoader 批量加载用户，避免 N+1 问题</p>
        <p>📊 性能：10 个文档 = 1 次文档查询 + 1 次批量用户查询（共 2 次 SQL）</p>
      </div>
    </div>

    <!-- 场景 3：详情页（完整内容） -->
    <div
      v-if="activeTab === 'detail-full'"
      class="demo-section"
    >
      <div class="section-header">
        <h2>场景 3：详情页 - 加载完整内容</h2>
        <input 
          v-model.number="selectedDocId" 
          type="number" 
          placeholder="输入文档 ID" 
          class="input-field"
        >
        <button
          :disabled="loading"
          class="btn-primary"
          @click="loadDocDetail"
        >
          {{ loading ? '加载中...' : '加载详情' }}
        </button>
      </div>

      <div class="code-block">
        <h3>GraphQL 查询语句</h3>
        <pre><code>{{ detailQueryCode }}</code></pre>
      </div>

      <div
        v-if="docDetail"
        class="doc-detail"
      >
        <h2>{{ docDetail.title }}</h2>
        <div class="meta-info">
          <span>版本: {{ docDetail.version }}</span>
          <span>创建: {{ formatDate(docDetail.createdAt) }}</span>
          <span>更新: {{ formatDate(docDetail.updatedAt) }}</span>
        </div>
        <div
          v-if="docDetail.user"
          class="author-info"
        >
          <img
            :src="docDetail.user.avatar || '/default-avatar.png'"
            alt="作者"
          >
          <div>
            <p><strong>作者：</strong>{{ docDetail.user.username }}</p>
            <p><strong>邮箱：</strong>{{ docDetail.user.email }}</p>
          </div>
        </div>
        <div class="content-area">
          <h3>文档内容</h3>
          <div v-html="formatMarkdown(docDetail.content)" />
        </div>
      </div>

      <div class="performance-info">
        <p>✅ 优势：只在详情页加载完整内容，列表页不浪费流量</p>
        <p>📊 性能：单个文档约 5KB，按需加载</p>
      </div>
    </div>

    <!-- 场景 4：详情页（带统计和历史） -->
    <div
      v-if="activeTab === 'detail-advanced'"
      class="demo-section"
    >
      <div class="section-header">
        <h2>场景 4：详情页 - 带统计和历史版本</h2>
        <input 
          v-model.number="selectedDocId" 
          type="number" 
          placeholder="输入文档 ID" 
          class="input-field"
        >
        <button
          :disabled="loading"
          class="btn-primary"
          @click="loadAdvancedDetail"
        >
          {{ loading ? '加载中...' : '加载完整信息' }}
        </button>
      </div>

      <div class="code-block">
        <h3>GraphQL 查询语句（组合多个 Fragment）</h3>
        <pre><code>{{ advancedQueryCode }}</code></pre>
      </div>

      <div
        v-if="advancedDetail"
        class="advanced-detail"
      >
        <h2>{{ advancedDetail.title }}</h2>

        <!-- 统计信息 -->
        <div
          v-if="advancedDetail.statistics"
          class="statistics-panel"
        >
          <h3>📊 文档统计</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-label">字数统计</span>
              <span class="stat-value">{{ advancedDetail.statistics.wordCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">编辑次数</span>
              <span class="stat-value">{{ advancedDetail.statistics.editCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最后编辑</span>
              <span class="stat-value">{{ formatDate(advancedDetail.statistics.lastEditedAt) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">发布状态</span>
              <span class="stat-value">{{ advancedDetail.statistics.published ? '已发布' : '草稿' }}</span>
            </div>
          </div>
        </div>

        <!-- 历史版本 -->
        <div
          v-if="advancedDetail.historyVersions && advancedDetail.historyVersions.length > 0"
          class="history-panel"
        >
          <h3>📜 历史版本</h3>
          <div class="history-list">
            <div
              v-for="history in advancedDetail.historyVersions"
              :key="history.id"
              class="history-item"
            >
              <span class="version-badge">v{{ history.version }}</span>
              <span>{{ formatDate(history.createdAt) }}</span>
              <span v-if="history.createdBy">by {{ history.createdBy.username }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="performance-info">
        <p>✅ 优势：统计和历史版本独立加载，不影响基础页面性能</p>
        <p>📊 性能：按需计算，只在需要时执行额外的聚合查询</p>
      </div>
    </div>

    <!-- 场景 5：管理后台（Token 审计） -->
    <div
      v-if="activeTab === 'admin-token'"
      class="demo-section"
    >
      <div class="section-header">
        <h2>场景 5：管理后台 - Token 使用审计</h2>
        <button
          :disabled="loading"
          class="btn-primary"
          @click="loadAdminData"
        >
          {{ loading ? '加载中...' : '加载审计数据' }}
        </button>
      </div>

      <div class="code-block">
        <h3>GraphQL 查询语句</h3>
        <pre><code>{{ adminQueryCode }}</code></pre>
      </div>

      <div
        v-if="adminData.length > 0"
        class="admin-table"
      >
        <table>
          <thead>
            <tr>
              <th>文档标题</th>
              <th>作者</th>
              <th>总 Token</th>
              <th>输入 Token</th>
              <th>输出 Token</th>
              <th>平均响应时间</th>
              <th>提供商</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="doc in adminData"
              :key="doc.id"
            >
              <td>{{ doc.title }}</td>
              <td>{{ doc.user?.username }}</td>
              <td>{{ doc.tokenUsage?.totalTokens || 0 }}</td>
              <td>{{ doc.tokenUsage?.inputTokens || 0 }}</td>
              <td>{{ doc.tokenUsage?.outputTokens || 0 }}</td>
              <td>{{ doc.tokenUsage?.avgResponseTime || 0 }} ms</td>
              <td>{{ doc.tokenUsage?.provider || 'N/A' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="performance-info">
        <p>✅ 优势：管理员专用字段，普通用户查询不会泄露敏感信息</p>
        <p>📊 性能：独立片段加载，不影响其他页面性能</p>
      </div>
    </div>

    <!-- 错误提示 -->
    <div
      v-if="error"
      class="error-message"
    >
      ❌ {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import graphqlClient from '@/utils/graphqlClient';

// 状态管理
const activeTab = ref('list-basic');
const loading = ref(false);
const error = ref(null);
const selectedDocId = ref(1);

// 数据存储
const basicList = ref([]);
const listWithUser = ref([]);
const docDetail = ref(null);
const advancedDetail = ref(null);
const adminData = ref([]);

// 标签页配置
const tabs = [
  { id: 'list-basic', label: '列表页（基础）' },
  { id: 'list-with-user', label: '列表页（带用户）' },
  { id: 'detail-full', label: '详情页（完整）' },
  { id: 'detail-advanced', label: '详情页（高级）' },
  { id: 'admin-token', label: '管理后台' }
];

// GraphQL 查询代码（用于展示）
const basicQueryCode = `fragment RequirementDocBasicFields on RequirementDoc {
  id
  title
  version
  createdAt
  updatedAt
}

query GetRequirementDocsList($page: Int, $size: Int) {
  requirementDocs(page: $page, size: $size) {
    content {
      ...RequirementDocBasicFields
    }
    pageInfo {
      totalElements
      hasNext
    }
  }
}`;

const withUserQueryCode = `fragment RequirementDocWithUser on RequirementDoc {
  id
  title
  version
  createdAt
  user {
    id
    username
    avatar
  }
}

query GetRequirementDocsWithUser($page: Int, $size: Int) {
  requirementDocs(page: $page, size: $size) {
    content {
      ...RequirementDocWithUser
    }
  }
}`;

const detailQueryCode = `fragment RequirementDocFullContent on RequirementDoc {
  id
  title
  content
  version
  createdAt
  updatedAt
  user {
    id
    username
    email
    avatar
  }
}

query GetRequirementDocDetail($id: Long!) {
  requirementDoc(id: $id) {
    ...RequirementDocFullContent
  }
}`;

const advancedQueryCode = `query GetRequirementDocFull($id: Long!) {
  requirementDoc(id: $id) {
    id
    title
    content
    version
    statistics {
      wordCount
      editCount
      lastEditedAt
      published
    }
    historyVersions {
      id
      version
      createdAt
      createdBy {
        username
      }
    }
  }
}`;

const adminQueryCode = `query GetRequirementDocsAdmin($page: Int, $size: Int) {
  requirementDocs(page: $page, size: $size) {
    content {
      id
      title
      user {
        username
        email
      }
      tokenUsage {
        totalTokens
        inputTokens
        outputTokens
        avgResponseTime
        provider
      }
    }
  }
}`;

// 加载基础列表
const loadBasicList = async () => {
  loading.value = true;
  error.value = null;
  try {
    const response = await graphqlClient.execute({
      query: basicQueryCode,
      variables: { page: 0, size: 10 }
    });
    basicList.value = response.data.requirementDocs.content;
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

// 加载带用户信息的列表
const loadListWithUser = async () => {
  loading.value = true;
  error.value = null;
  try {
    const response = await graphqlClient.execute({
      query: withUserQueryCode,
      variables: { page: 0, size: 10 }
    });
    listWithUser.value = response.data.requirementDocs.content;
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

// 加载文档详情
const loadDocDetail = async () => {
  if (!selectedDocId.value) {
    error.value = '请输入文档 ID';
    return;
  }
  
  loading.value = true;
  error.value = null;
  try {
    const response = await graphqlClient.execute({
      query: detailQueryCode,
      variables: { id: selectedDocId.value }
    });
    docDetail.value = response.data.requirementDoc;
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

// 加载高级详情
const loadAdvancedDetail = async () => {
  if (!selectedDocId.value) {
    error.value = '请输入文档 ID';
    return;
  }
  
  loading.value = true;
  error.value = null;
  try {
    const response = await graphqlClient.execute({
      query: advancedQueryCode,
      variables: { id: selectedDocId.value }
    });
    advancedDetail.value = response.data.requirementDoc;
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

// 加载管理后台数据
const loadAdminData = async () => {
  loading.value = true;
  error.value = null;
  try {
    const response = await graphqlClient.execute({
      query: adminQueryCode,
      variables: { page: 0, size: 20 }
    });
    adminData.value = response.data.requirementDocs.content;
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

// 工具函数
const formatDate = (date) => {
  if (!date) return 'N/A';
  return new Date(date).toLocaleString('zh-CN');
};

const formatMarkdown = (content) => {
  if (!content) return '<p>无内容</p>';
  // 简单的 Markdown 渲染（实际项目中使用 marked 或 markdown-it）
  return content.replace(/\n/g, '<br>');
};
</script>

<style scoped>
.graphql-demo-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 20px;
}

.demo-header {
  text-align: center;
  margin-bottom: 40px;
}

.demo-header h1 {
  font-size: 2.5rem;
  color: #2c3e50;
  margin-bottom: 10px;
}

.subtitle {
  color: #7f8c8d;
  font-size: 1.1rem;
}

.demo-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
  overflow-x: auto;
}

.tab-btn {
  padding: 12px 24px;
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.tab-btn:hover {
  border-color: #3498db;
}

.tab-btn.active {
  background: #3498db;
  color: white;
  border-color: #3498db;
}

.demo-section {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.section-header h2 {
  flex: 1;
  color: #2c3e50;
}

.btn-primary {
  padding: 10px 20px;
  background: #27ae60;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover:not(:disabled) {
  background: #229954;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-field {
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 1rem;
}

.code-block {
  background: #2c3e50;
  color: #ecf0f1;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  overflow-x: auto;
}

.code-block h3 {
  color: #3498db;
  margin-bottom: 10px;
}

.code-block pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.doc-card {
  padding: 20px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  transition: transform 0.3s;
}

.doc-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.doc-card h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.performance-info {
  background: #e8f5e9;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #27ae60;
}

.performance-info p {
  margin: 5px 0;
  color: #2c3e50;
}

.doc-detail, .advanced-detail {
  background: #f8f9fa;
  padding: 30px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.meta-info {
  display: flex;
  gap: 20px;
  margin: 15px 0;
  color: #7f8c8d;
}

.author-info {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  margin: 20px 0;
}

.author-info img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
}

.content-area {
  margin-top: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.statistics-panel, .history-panel {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin: 20px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
  margin-top: 15px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-label {
  color: #7f8c8d;
  font-size: 0.9rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: bold;
  color: #3498db;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 15px;
}

.history-item {
  display: flex;
  gap: 15px;
  align-items: center;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
}

.version-badge {
  background: #3498db;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.9rem;
}

.admin-table {
  overflow-x: auto;
  margin-bottom: 20px;
}

.admin-table table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.admin-table th,
.admin-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.admin-table th {
  background: #3498db;
  color: white;
  font-weight: 600;
}

.admin-table tr:hover {
  background: #f8f9fa;
}

.error-message {
  background: #ffe5e5;
  color: #c0392b;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #c0392b;
  margin-top: 20px;
}
</style>
