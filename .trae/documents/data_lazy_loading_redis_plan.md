# 数据懒加载与Redis集成实现计划

## 问题分析

1. **当前消息加载方式**：前端一次性加载整个会话的所有消息，对于长会话会导致性能问题
2. **缺乏缓存机制**：每次请求都直接查询数据库，没有利用缓存提高性能
3. **用户体验**：长会话加载缓慢，影响用户体验

## 实现计划

### [x] 任务1：前端实现消息懒加载
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 修改 `fetchSessionMessages` 函数，添加分页参数
  - 实现滚动到底部加载更多历史消息的功能
  - 优化消息容器的滚动事件处理
- **Success Criteria**: 
  - 首次加载时只加载最新的20条消息
  - 滚动到顶部时自动加载更多历史消息
  - 加载过程中显示加载指示器
- **Test Requirements**: 
  - `programmatic` TR-1.1: 首次加载会话时只加载最新的20条消息
  - `programmatic` TR-1.2: 滚动到顶部时触发加载更多消息的请求
  - `human-judgement` TR-1.3: 加载过程流畅，无明显卡顿

### [x] 任务2：后端实现消息分页查询
- **Priority**: P0
- **Depends On**: 任务1
- **Description**: 
  - 修改 `ChatRecordService.getSessionMessages` 方法，支持分页参数
  - 修改 `ChatRecordController.getSessionMessages` 方法，接收分页参数
  - 更新API文档，添加分页参数说明
- **Success Criteria**: 
  - API支持 `page` 和 `pageSize` 参数
  - 按消息时间倒序返回指定页的消息
  - 支持返回消息总数和分页信息
- **Test Requirements**: 
  - `programmatic` TR-2.1: API请求包含分页参数时返回正确的消息页
  - `programmatic` TR-2.2: 消息按时间倒序排列
  - `programmatic` TR-2.3: 响应包含分页元数据

### [x] 任务3：集成Redis缓存
- **Priority**: P1
- **Depends On**: 任务2
- **Description**: 
  - 添加Redis依赖配置
  - 实现Redis缓存管理器
  - 缓存热点会话的消息数据
  - 实现缓存失效策略
- **Success Criteria**: 
  - Redis缓存服务正常运行
  - 热点会话的消息查询优先从缓存获取
  - 缓存数据在消息更新时自动失效
- **Test Requirements**: 
  - `programmatic` TR-3.1: 首次查询后，相同请求从缓存获取数据
  - `programmatic` TR-3.2: 消息更新后，缓存自动失效
  - `programmatic` TR-3.3: 缓存命中时查询响应时间显著减少

### [/] 任务4：优化前端消息渲染
- **Priority**: P1
- **Depends On**: 任务1
- **Description**: 
  - 优化消息渲染性能，避免大量消息导致的渲染卡顿
  - 实现消息的虚拟滚动
  - 优化图片和媒体内容的加载
- **Success Criteria**: 
  - 渲染100+条消息时无明显卡顿
  - 虚拟滚动正常工作，只渲染可视区域内的消息
  - 图片和媒体内容懒加载
- **Test Requirements**: 
  - `programmatic` TR-4.1: 渲染100+条消息时页面响应时间<100ms
  - `human-judgement` TR-4.2: 滚动消息列表时流畅无卡顿
  - `programmatic` TR-4.3: 图片按需加载，不影响页面加载速度

### [ ] 任务5：测试与性能优化
- **Priority**: P2
- **Depends On**: 任务1-4
- **Description**: 
  - 进行性能测试，验证懒加载和缓存的效果
  - 优化Redis缓存策略
  - 调整分页参数和加载阈值
- **Success Criteria**: 
  - 长会话加载时间减少50%以上
  - Redis缓存命中率>80%
  - 页面滚动流畅，无卡顿
- **Test Requirements**: 
  - `programmatic` TR-5.1: 100条消息的会话加载时间<200ms
  - `programmatic` TR-5.2: Redis缓存命中率>80%
  - `human-judgement` TR-5.3: 整体用户体验流畅

## 技术要点

1. **前端实现**：
   - 使用 `IntersectionObserver` 监听滚动到底部事件
   - 实现消息的分页加载逻辑
   - 优化消息容器的滚动性能

2. **后端实现**：
   - 使用 Spring Data JPA 的分页查询
   - 集成 Redis 缓存
   - 实现缓存的自动失效机制

3. **Redis缓存策略**：
   - 缓存热点会话的最新消息
   - 设置合理的缓存过期时间
   - 实现缓存的读写一致性

4. **性能优化**：
   - 实现消息的虚拟滚动
   - 优化数据库查询
   - 减少网络传输数据量

## 预期结果

- 长会话加载速度显著提升
- 系统整体性能得到改善
- 用户体验更加流畅
- 服务器负载降低