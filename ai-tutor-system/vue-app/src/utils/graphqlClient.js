import axios from 'axios';

/**
 * GraphQL 客户端工具类
 * 用于发送 GraphQL 查询和变更请求
 */
class GraphQLClient {
  constructor(endpoint, options = {}) {
    this.endpoint = endpoint;
    this.defaultHeaders = {
      'Content-Type': 'application/json',
      ...options.headers
    };
  }

  /**
   * 执行 GraphQL 查询或变更
   * 
   * @param {Object} params
   * @param {string} params.query - GraphQL 查询语句
   * @param {Object} params.variables - 查询变量
   * @param {Object} params.headers - 自定义请求头
   * @returns {Promise<Object>} GraphQL 响应
   */
  async execute({ query, variables = {}, headers = {} }) {
    try {
      const response = await axios.post(
        this.endpoint,
        {
          query,
          variables
        },
        {
          headers: {
            ...this.defaultHeaders,
            ...headers
          },
          withCredentials: true
        }
      );

      // 检查 GraphQL 错误
      if (response.data.errors) {
        console.error('GraphQL Errors:', response.data.errors);
        throw new Error(response.data.errors[0]?.message || 'GraphQL 请求失败');
      }

      return response.data;
    } catch (error) {
      console.error('GraphQL Request Failed:', error);
      throw error;
    }
  }

  /**
   * 批量查询（执行多个查询）
   * 
   * @param {Array<Object>} queries - 查询数组
   * @returns {Promise<Array<Object>>} 响应数组
   */
  async executeBatch(queries) {
    const promises = queries.map(q => this.execute(q));
    return Promise.all(promises);
  }
}

// 创建默认客户端实例
const defaultClient = new GraphQLClient('/graphql');

export default defaultClient;
export { GraphQLClient };
