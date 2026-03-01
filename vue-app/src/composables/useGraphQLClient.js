import { ref } from 'vue';
import graphqlClient from '@/utils/graphqlClient';

/**
 * GraphQL 查询 Composable
 * Vue 3 Composition API 风格的 GraphQL 客户端
 */
export function useGraphQLClient() {
  const loading = ref(false);
  const error = ref(null);

  /**
   * 执行 GraphQL 查询
   */
  const executeQuery = async ({ query, variables, headers }) => {
    loading.value = true;
    error.value = null;

    try {
      const result = await graphqlClient.execute({ query, variables, headers });
      return result;
    } catch (err) {
      error.value = err.message || '查询失败';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 执行 GraphQL 变更
   */
  const executeMutation = async ({ mutation, variables, headers }) => {
    loading.value = true;
    error.value = null;

    try {
      const result = await graphqlClient.execute({ 
        query: mutation, 
        variables, 
        headers 
      });
      return result;
    } catch (err) {
      error.value = err.message || '操作失败';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    error,
    executeQuery,
    executeMutation
  };
}

/**
 * 需求文档专用 Composable
 * 封装常用的需求文档操作
 */
export function useRequirementDocGraphQL() {
  const { loading, error, executeQuery, executeMutation } = useGraphQLClient();

  /**
   * 获取需求文档列表
   */
  const getRequirementDocs = async (page = 0, size = 10, fragment) => {
    const query = `
      ${fragment}
      
      query GetRequirementDocs($page: Int, $size: Int) {
        requirementDocs(page: $page, size: $size) {
          content {
            ...RequirementDocFields
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

    return executeQuery({ query, variables: { page, size } });
  };

  /**
   * 获取需求文档详情
   */
  const getRequirementDoc = async (id, fragment) => {
    const query = `
      ${fragment}
      
      query GetRequirementDoc($id: Long!) {
        requirementDoc(id: $id) {
          ...RequirementDocFields
        }
      }
    `;

    return executeQuery({ query, variables: { id } });
  };

  /**
   * 创建需求文档
   */
  const createRequirementDoc = async (input) => {
    const mutation = `
      mutation CreateRequirementDoc($input: CreateRequirementDocInput!) {
        createRequirementDoc(input: $input) {
          id
          title
          content
          version
          createdAt
        }
      }
    `;

    return executeMutation({ mutation, variables: { input } });
  };

  /**
   * 更新需求文档
   */
  const updateRequirementDoc = async (id, input) => {
    const mutation = `
      mutation UpdateRequirementDoc($id: Long!, $input: UpdateRequirementDocInput!) {
        updateRequirementDoc(id: $id, input: $input) {
          id
          title
          content
          version
          updatedAt
        }
      }
    `;

    return executeMutation({ mutation, variables: { id, input } });
  };

  /**
   * 删除需求文档
   */
  const deleteRequirementDoc = async (id) => {
    const mutation = `
      mutation DeleteRequirementDoc($id: Long!) {
        deleteRequirementDoc(id: $id)
      }
    `;

    return executeMutation({ mutation, variables: { id } });
  };

  return {
    loading,
    error,
    getRequirementDocs,
    getRequirementDoc,
    createRequirementDoc,
    updateRequirementDoc,
    deleteRequirementDoc
  };
}
