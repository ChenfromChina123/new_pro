/**
 * ===========================
 * GraphQL Fragments 使用示例
 * ===========================
 * 
 * Fragments 是 GraphQL 的核心特性，用于定义可重用的字段集合
 * 实现按需加载，减少网络传输和服务器计算
 */

// ===========================
// 1. 基础 Fragments 定义
// ===========================

/**
 * 基础字段片段 - 列表页使用
 * 只包含展示列表所需的最小字段集
 */
export const REQUIREMENT_DOC_BASIC_FIELDS = `
  fragment RequirementDocBasicFields on RequirementDoc {
    id
    title
    version
    createdAt
    updatedAt
  }
`;

/**
 * 带用户信息的片段 - 需要展示作者的场景
 */
export const REQUIREMENT_DOC_WITH_USER = `
  fragment RequirementDocWithUser on RequirementDoc {
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
`;

/**
 * 完整内容片段 - 详情页使用
 * 包含文档全部内容
 */
export const REQUIREMENT_DOC_FULL_CONTENT = `
  fragment RequirementDocFullContent on RequirementDoc {
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
`;

/**
 * 统计信息片段 - 仪表盘/详情页使用
 */
export const REQUIREMENT_DOC_WITH_STATISTICS = `
  fragment RequirementDocWithStatistics on RequirementDoc {
    id
    title
    statistics {
      wordCount
      editCount
      lastEditedAt
      published
    }
  }
`;

/**
 * 历史版本片段 - 版本管理页使用
 */
export const REQUIREMENT_DOC_WITH_HISTORY = `
  fragment RequirementDocWithHistory on RequirementDoc {
    id
    title
    version
    historyVersions {
      id
      version
      createdAt
      createdBy {
        username
      }
    }
  }
`;

/**
 * Token 使用片段 - 管理后台使用
 */
export const REQUIREMENT_DOC_WITH_TOKEN_USAGE = `
  fragment RequirementDocWithTokenUsage on RequirementDoc {
    id
    title
    tokenUsage {
      totalTokens
      inputTokens
      outputTokens
      avgResponseTime
      provider
    }
  }
`;

// ===========================
// 2. GraphQL 查询示例
// ===========================

/**
 * 场景 1：列表页 - 只加载基础字段
 * 优势：最小化数据传输，快速渲染列表
 */
export const QUERY_REQUIREMENT_DOCS_LIST = `
  ${REQUIREMENT_DOC_BASIC_FIELDS}
  
  query GetRequirementDocsList($page: Int, $size: Int) {
    requirementDocs(page: $page, size: $size) {
      content {
        ...RequirementDocBasicFields
      }
      pageInfo {
        page
        size
        totalElements
        totalPages
        hasNext
      }
    }
  }
`;

/**
 * 场景 2：列表页（带作者信息）
 * 优势：利用 DataLoader 批量加载用户，避免 N+1 问题
 */
export const QUERY_REQUIREMENT_DOCS_WITH_USER = `
  ${REQUIREMENT_DOC_WITH_USER}
  
  query GetRequirementDocsWithUser($page: Int, $size: Int) {
    requirementDocs(page: $page, size: $size) {
      content {
        ...RequirementDocWithUser
      }
      pageInfo {
        page
        totalElements
        hasNext
      }
    }
  }
`;

/**
 * 场景 3：详情页 - 加载完整内容
 */
export const QUERY_REQUIREMENT_DOC_DETAIL = `
  ${REQUIREMENT_DOC_FULL_CONTENT}
  
  query GetRequirementDocDetail($id: Long!) {
    requirementDoc(id: $id) {
      ...RequirementDocFullContent
    }
  }
`;

/**
 * 场景 4：详情页（带统计和历史）
 * 组合多个 Fragment
 */
export const QUERY_REQUIREMENT_DOC_FULL = `
  ${REQUIREMENT_DOC_FULL_CONTENT}
  ${REQUIREMENT_DOC_WITH_STATISTICS}
  ${REQUIREMENT_DOC_WITH_HISTORY}
  
  query GetRequirementDocFull($id: Long!) {
    requirementDoc(id: $id) {
      ...RequirementDocFullContent
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
          avatar
        }
      }
    }
  }
`;

/**
 * 场景 5：管理后台 - 带 Token 统计
 */
export const QUERY_REQUIREMENT_DOCS_ADMIN = `
  ${REQUIREMENT_DOC_WITH_TOKEN_USAGE}
  
  query GetRequirementDocsAdmin($page: Int, $size: Int) {
    requirementDocs(page: $page, size: $size) {
      content {
        ...RequirementDocWithTokenUsage
        user {
          username
          email
        }
      }
      pageInfo {
        page
        totalElements
      }
    }
  }
`;

// ===========================
// 3. Mutation 示例
// ===========================

/**
 * 创建需求文档
 */
export const MUTATION_CREATE_REQUIREMENT_DOC = `
  ${REQUIREMENT_DOC_FULL_CONTENT}
  
  mutation CreateRequirementDoc($input: CreateRequirementDocInput!) {
    createRequirementDoc(input: $input) {
      ...RequirementDocFullContent
    }
  }
`;

/**
 * 更新需求文档
 */
export const MUTATION_UPDATE_REQUIREMENT_DOC = `
  ${REQUIREMENT_DOC_FULL_CONTENT}
  
  mutation UpdateRequirementDoc($id: Long!, $input: UpdateRequirementDocInput!) {
    updateRequirementDoc(id: $id, input: $input) {
      ...RequirementDocFullContent
    }
  }
`;

/**
 * 删除需求文档
 */
export const MUTATION_DELETE_REQUIREMENT_DOC = `
  mutation DeleteRequirementDoc($id: Long!) {
    deleteRequirementDoc(id: $id)
  }
`;

// ===========================
// 4. Vue 3 组件中的使用示例
// ===========================

/**
 * 示例：列表页组件
 */
/*
<script setup>
import { ref, onMounted } from 'vue';
import { useGraphQLClient } from '@/composables/useGraphQLClient';
import { QUERY_REQUIREMENT_DOCS_WITH_USER } from '@/graphql/requirementDoc.fragments';

const { executeQuery } = useGraphQLClient();
const documents = ref([]);
const loading = ref(false);

const loadDocuments = async (page = 0, size = 10) => {
  loading.value = true;
  
  try {
    const response = await executeQuery({
      query: QUERY_REQUIREMENT_DOCS_WITH_USER,
      variables: { page, size }
    });
    
    documents.value = response.data.requirementDocs.content;
  } catch (error) {
    console.error('加载失败', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadDocuments();
});
</script>

<template>
  <div class="doc-list">
    <div v-for="doc in documents" :key="doc.id" class="doc-item">
      <h3>{{ doc.title }}</h3>
      <p>版本: {{ doc.version }}</p>
      <p>作者: {{ doc.user?.username }}</p>
      <img :src="doc.user?.avatar" alt="头像" />
    </div>
  </div>
</template>
*/

// ===========================
// 5. 性能优化说明
// ===========================

/**
 * Fragment 的性能优势：
 * 
 * 1. 减少网络传输
 *    - 列表页只传输基础字段（~200 bytes/doc）
 *    - 详情页才加载完整内容（~5KB/doc）
 *    - 对于 100 个文档，节约约 480KB 流量
 * 
 * 2. 避免 N+1 查询问题
 *    - 传统 REST：查询 10 个文档 = 1 次文档查询 + 10 次用户查询
 *    - GraphQL + DataLoader：查询 10 个文档 = 1 次文档查询 + 1 次批量用户查询
 * 
 * 3. 按需计算
 *    - statistics 字段需要额外计算，列表页不请求则不计算
 *    - historyVersions 需要额外查询，不请求则不查询数据库
 * 
 * 4. 灵活组合
 *    - 不同页面组合不同 Fragment
 *    - 同一个 API 端点满足多种场景
 */

/**
 * DataLoader 工作原理示例：
 * 
 * 假设查询 3 个文档，每个文档都请求 user 字段
 * 
 * 传统方式（N+1 问题）：
 * SELECT * FROM requirement_docs WHERE id IN (1, 2, 3);  -- 1 次
 * SELECT * FROM users WHERE id = 10;                     -- 3 次
 * SELECT * FROM users WHERE id = 20;
 * SELECT * FROM users WHERE id = 30;
 * 
 * DataLoader 批量加载：
 * SELECT * FROM requirement_docs WHERE id IN (1, 2, 3);  -- 1 次
 * SELECT * FROM users WHERE id IN (10, 20, 30);          -- 1 次（批量）
 * 
 * 性能提升：从 4 次查询降低到 2 次查询
 */
