# GraphQL 快速测试指南

## 🚀 启动项目

### 1. 后端启动

```bash
cd aispring
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:5000` 启动

### 2. 访问 GraphiQL 界面

浏览器打开：`http://localhost:5000/graphiql`

这是一个可视化的 GraphQL 查询工具，支持：
- ✅ 语法高亮
- ✅ 自动补全
- ✅ Schema 文档浏览
- ✅ 查询历史记录

### 3. 前端启动

```bash
cd vue-app
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

访问演示页面：`http://localhost:3000/graphql-demo`

---

## 📝 测试查询示例

### 场景 1：查询文档列表（基础字段）

**最小字段查询，适用于列表页**

```graphql
# 查询语句
query GetRequirementDocsList {
  requirementDocs(page: 0, size: 10) {
    content {
      id
      title
      version
      createdAt
      updatedAt
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
```

**预期响应**：

```json
{
  "data": {
    "requirementDocs": {
      "content": [
        {
          "id": 1,
          "title": "电商系统需求文档",
          "version": 1,
          "createdAt": "2024-01-15T10:30:00",
          "updatedAt": "2024-01-15T10:30:00"
        },
        {
          "id": 2,
          "title": "AI 知识库平台",
          "version": 2,
          "createdAt": "2024-01-16T14:20:00",
          "updatedAt": "2024-01-16T15:45:00"
        }
      ],
      "pageInfo": {
        "page": 0,
        "size": 10,
        "totalElements": 25,
        "totalPages": 3,
        "hasNext": true
      }
    }
  }
}
```

---

### 场景 2：查询文档列表（带作者信息）

**使用 DataLoader 批量加载用户，避免 N+1 问题**

```graphql
# 定义 Fragment
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

# 查询语句
query GetRequirementDocsWithUser {
  requirementDocs(page: 0, size: 10) {
    content {
      ...RequirementDocWithUser
    }
    pageInfo {
      totalElements
      hasNext
    }
  }
}
```

**SQL 执行情况**（查看后端日志）：

```
✅ 优化后：
Hibernate: SELECT * FROM requirement_docs LIMIT 10           -- 1 次
Hibernate: SELECT * FROM users WHERE id IN (1,2,3,4,5)       -- 1 次（批量）
总计：2 次查询

❌ 传统 REST（N+1 问题）：
SELECT * FROM requirement_docs LIMIT 10                      -- 1 次
SELECT * FROM users WHERE id = 1                             -- 10 次
SELECT * FROM users WHERE id = 2
...
总计：11 次查询
```

---

### 场景 3：查询单个文档详情

**加载完整内容和作者信息**

```graphql
query GetRequirementDocDetail($id: Long!) {
  requirementDoc(id: $id) {
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
}

# 变量
{
  "id": 1
}
```

---

### 场景 4：查询文档详情（带统计信息）

**组合多个独立片段**

```graphql
query GetRequirementDocFullDetail($id: Long!) {
  requirementDoc(id: $id) {
    id
    title
    content
    version
    createdAt
    updatedAt
    
    # 作者信息
    user {
      id
      username
      email
      avatar
    }
    
    # 统计信息（独立片段，按需加载）
    statistics {
      wordCount
      editCount
      lastEditedAt
      published
    }
    
    # 历史版本（独立片段，按需加载）
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

# 变量
{
  "id": 1
}
```

**预期响应**：

```json
{
  "data": {
    "requirementDoc": {
      "id": 1,
      "title": "电商系统需求文档",
      "content": "# 1. 项目概述\n\n本系统旨在...",
      "version": 3,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-20T16:45:00",
      "user": {
        "id": 5,
        "username": "张三",
        "email": "zhangsan@example.com",
        "avatar": "/uploads/avatar/5.jpg"
      },
      "statistics": {
        "wordCount": 15000,
        "editCount": 12,
        "lastEditedAt": "2024-01-20T16:45:00",
        "published": true
      },
      "historyVersions": [
        {
          "id": 3,
          "version": 3,
          "createdAt": "2024-01-20T16:45:00",
          "createdBy": {
            "username": "张三",
            "avatar": "/uploads/avatar/5.jpg"
          }
        },
        {
          "id": 2,
          "version": 2,
          "createdAt": "2024-01-18T14:20:00",
          "createdBy": {
            "username": "张三",
            "avatar": "/uploads/avatar/5.jpg"
          }
        },
        {
          "id": 1,
          "version": 1,
          "createdAt": "2024-01-15T10:30:00",
          "createdBy": {
            "username": "李四",
            "avatar": "/uploads/avatar/8.jpg"
          }
        }
      ]
    }
  }
}
```

---

### 场景 5：管理后台 - Token 审计

**查询 Token 使用统计（仅管理员可见）**

```graphql
query GetRequirementDocsWithTokenUsage {
  requirementDocs(page: 0, size: 20) {
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
    pageInfo {
      totalElements
    }
  }
}
```

**预期响应**：

```json
{
  "data": {
    "requirementDocs": {
      "content": [
        {
          "id": 1,
          "title": "电商系统需求文档",
          "user": {
            "username": "张三",
            "email": "zhangsan@example.com"
          },
          "tokenUsage": {
            "totalTokens": 8500,
            "inputTokens": 3200,
            "outputTokens": 5300,
            "avgResponseTime": 2300,
            "provider": "deepseek"
          }
        },
        {
          "id": 2,
          "title": "AI 知识库平台",
          "user": {
            "username": "李四",
            "email": "lisi@example.com"
          },
          "tokenUsage": {
            "totalTokens": 12000,
            "inputTokens": 4500,
            "outputTokens": 7500,
            "avgResponseTime": 3100,
            "provider": "doubao"
          }
        }
      ],
      "pageInfo": {
        "totalElements": 25
      }
    }
  }
}
```

---

## 🔧 变更操作测试

### 创建文档

```graphql
mutation CreateRequirementDoc($input: CreateRequirementDocInput!) {
  createRequirementDoc(input: $input) {
    id
    title
    content
    version
    createdAt
    user {
      username
    }
  }
}

# 变量
{
  "input": {
    "title": "新项目需求文档",
    "content": "# 项目背景\n\n这是一个新的项目..."
  }
}
```

### 更新文档

```graphql
mutation UpdateRequirementDoc($id: Long!, $input: UpdateRequirementDocInput!) {
  updateRequirementDoc(id: $id, input: $input) {
    id
    title
    content
    version
    updatedAt
  }
}

# 变量
{
  "id": 1,
  "input": {
    "title": "电商系统需求文档（已更新）",
    "content": "# 1. 项目概述\n\n更新后的内容..."
  }
}
```

### 删除文档

```graphql
mutation DeleteRequirementDoc($id: Long!) {
  deleteRequirementDoc(id: $id)
}

# 变量
{
  "id": 10
}
```

---

## 🎯 性能对比测试

### 测试 1：列表页加载速度

**场景**：加载 100 条文档记录

| 方案 | 响应时间 | 数据大小 | SQL 查询次数 |
|------|---------|---------|-------------|
| REST（完整对象） | ~500ms | ~500KB | 1 次 |
| GraphQL（基础字段） | ~50ms | ~20KB | 1 次 |
| GraphQL（带用户） | ~80ms | ~35KB | 2 次（批量） |

**测试方法**：

```bash
# REST API
curl -X GET "http://localhost:5000/api/requirement/list?page=0&size=100"

# GraphQL（基础字段）
curl -X POST "http://localhost:5000/graphql" \
  -H "Content-Type: application/json" \
  -d '{"query":"query{requirementDocs(page:0,size:100){content{id title version}}}"}'

# GraphQL（带用户）
curl -X POST "http://localhost:5000/graphql" \
  -H "Content-Type: application/json" \
  -d '{"query":"query{requirementDocs(page:0,size:100){content{id title user{username avatar}}}}"}'
```

### 测试 2：DataLoader 批量加载效果

**场景**：查询 10 个文档及其作者信息

**开启 Hibernate SQL 日志**（application.yml）：

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**执行查询**：

```graphql
query {
  requirementDocs(page: 0, size: 10) {
    content {
      id
      title
      user {
        username
      }
    }
  }
}
```

**观察后端日志**：

```
✅ 应该看到：
Hibernate: select ... from requirement_docs limit ?
Hibernate: select ... from users where id in (?, ?, ?, ...)  <- 批量查询

❌ 不应该看到：
Hibernate: select ... from users where id = ?  <- 重复 10 次
```

---

## 🐛 常见问题排查

### 问题 1：GraphQL 端点 404

**检查配置**：

```yaml
# application.yml
spring:
  graphql:
    path: /graphql  # 确认路径配置
    graphiql:
      enabled: true
```

**测试命令**：

```bash
curl -X POST http://localhost:5000/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"query{__typename}"}'
```

### 问题 2：DataLoader 未生效（仍有 N+1 问题）

**检查 DataLoaderConfig**：

```java
// 确认已注册 BatchLoader
registry.forTypePair(Long.class, User.class)
      .registerMappedBatchLoader(...);
```

**检查控制器方法**：

```java
// 应该返回 CompletableFuture
@SchemaMapping(typeName = "RequirementDoc", field = "user")
public CompletableFuture<User> user(RequirementDoc doc) {
    return CompletableFuture.supplyAsync(...);
}
```

### 问题 3：认证失败

**添加认证头**：

```bash
curl -X POST http://localhost:5000/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"query":"..."}'
```

### 问题 4：Schema 解析错误

**检查 schema.graphqls 语法**：

```bash
# 在 GraphiQL 中查看 Schema 文档
http://localhost:5000/graphiql

# 点击右侧 "Docs" 按钮，查看完整 Schema
```

---

## 📊 性能监控

### 1. 使用 Spring Boot Actuator

**添加依赖**：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**访问指标**：

```
http://localhost:5000/actuator/metrics/graphql.request
http://localhost:5000/actuator/metrics/graphql.dataloader
```

### 2. 查看 GraphQL 执行计划

**在 GraphiQL 中执行**：

```graphql
query {
  __schema {
    queryType {
      name
      fields {
        name
        type {
          name
        }
      }
    }
  }
}
```

---

## 🎓 下一步学习

1. **探索 GraphiQL 界面**：熟悉 Schema 文档和自动补全
2. **测试所有场景**：尝试本文档中的所有查询示例
3. **查看后端日志**：观察 SQL 查询优化效果
4. **访问演示页面**：`http://localhost:3000/graphql-demo`
5. **阅读完整指南**：`GRAPHQL_IMPLEMENTATION_GUIDE.md`

---

**🔗 相关资源**：
- [Spring for GraphQL 文档](https://docs.spring.io/spring-graphql/docs/current/reference/html/)
- [GraphQL 规范](https://graphql.org/learn/)
- [DataLoader 原理](https://github.com/graphql/dataloader)
