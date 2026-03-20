# 动画库使用指南

本动画库提供了丰富的动画效果，用于增强微信小程序的用户体验。

## 📚 动画分类

### 1. 淡入淡出动画

#### 淡入动画
```wxml
<view class="animate-fade-in">内容</view>
<view class="animate-fade-in-slow">慢速淡入</view>
```

#### 淡出动画
```wxml
<view class="animate-fade-out">内容</view>
```

### 2. 滑入滑出动画

#### 从不同方向滑入
```wxml
<view class="animate-slide-in-up">从下向上</view>
<view class="animate-slide-in-down">从上向下</view>
<view class="animate-slide-in-left">从左向右</view>
<view class="animate-slide-in-right">从右向左</view>
```

#### 滑出动画
```wxml
<view class="animate-slide-out-up">向上滑出</view>
```

### 3. 缩放动画

#### 缩放进入
```wxml
<view class="animate-scale-in">缩放进入</view>
```

#### 缩放退出
```wxml
<view class="animate-scale-out">缩放退出</view>
```

### 4. 弹跳动画

#### 持续弹跳
```wxml
<view class="animate-bounce">弹跳效果</view>
```

#### 弹跳进入
```wxml
<view class="animate-bounce-in">弹跳进入</view>
```

#### 弹跳退出
```wxml
<view class="animate-bounce-out">弹跳退出</view>
```

### 5. 脉冲和闪烁

#### 脉冲效果
```wxml
<view class="animate-pulse">脉冲效果</view>
```

#### 闪烁效果
```wxml
<view class="animate-blink">闪烁效果</view>
```

### 6. 旋转动画

#### 持续旋转
```wxml
<view class="animate-spin">旋转</view>
<view class="animate-spin-slow">慢速旋转</view>
```

### 7. 特殊效果

#### 摇摆动画
```wxml
<view class="animate-swing">摇摆</view>
```

#### 抖动动画
```wxml
<view class="animate-shake">抖动</view>
```

#### 翻转动画
```wxml
<view class="animate-flip-in">翻转进入</view>
<view class="animate-flip-out">翻转退出</view>
```

#### 浮动效果
```wxml
<view class="animate-float">浮动</view>
```

#### 心跳效果
```wxml
<view class="animate-heartbeat">心跳</view>
```

### 8. 骨架屏加载

```wxml
<view class="skeleton" style="width: 200rpx; height: 40rpx;"></view>
```

### 9. 打字机光标

```wxml
<text class="typing-cursor">正在输入...</text>
```

## 🎨 动画控制

### 1. 动画延迟

使用 `animation-delay` 属性控制动画延迟：

```wxml
<view class="animate-fade-in" style="animation-delay: 0.2s">延迟 0.2 秒</view>
<view class="animate-slide-in-up" style="animation-delay: 0.4s">延迟 0.4 秒</view>
```

### 2. 动画持续时间

使用预定义的类：

```wxml
<view class="duration-75">75ms</view>
<view class="duration-100">100ms</view>
<view class="duration-200">200ms</view>
<view class="duration-300">300ms</view>
<view class="duration-500">500ms</view>
<view class="duration-700">700ms</view>
<view class="duration-1000">1000ms</view>
```

### 3. 动画缓动函数

```wxml
<view class="ease-linear">线性</view>
<view class="ease-in">加速</view>
<view class="ease-out">减速</view>
<view class="ease-in-out">先加速后减速</view>
```

### 4. 动画填充模式

```wxml
<view class="fill-mode-forwards">保持结束状态</view>
<view class="fill-mode-backwards">应用初始状态</view>
<view class="fill-mode-both">同时应用</view>
```

### 5. 循环动画

```wxml
<view class="animate-spin animate-infinite">无限旋转</view>
<view class="animate-bounce animate-infinite">无限弹跳</view>
<view class="animate-pulse animate-infinite">无限脉冲</view>
```

## 🎯 实际应用场景

### 1. 页面加载动画

```wxml
<!-- 页面容器 -->
<view class="page-transition">
  <view class="animate-fade-in">
    <text>欢迎页面</text>
  </view>
</view>
```

### 2. 列表项动画

```wxml
<view wx:for="{{list}}" wx:key="id">
  <view 
    class="animate-slide-in-up" 
    style="animation-delay: {{index * 0.1}}s"
  >
    {{item.name}}
  </view>
</view>
```

### 3. 按钮点击反馈

```wxml
<view class="btn btn-primary hover-lift">
  点击按钮
</view>
```

### 4. 加载状态

```wxml
<view class="flex items-center justify-center">
  <view class="loading"></view>
  <text class="loading-text ml-2">加载中...</text>
</view>
```

### 5. 模态框动画

```wxml
<view class="modal-overlay animate-fade-in">
  <view class="modal-content animate-scale-in">
    <text>模态框内容</text>
  </view>
</view>
```

### 6. 提示消息动画

```wxml
<view class="toast animate-slide-in-down">
  <text>操作成功</text>
</view>
```

### 7. 卡片悬浮效果

```wxml
<view class="card hover-lift">
  <text>悬浮卡片</text>
</view>
```

### 8. 数据刷新动画

```wxml
<view class="refresh-indicator animate-spin">
  <view class="loading"></view>
</view>
```

## 💡 组合使用示例

### 1. 复杂入场动画

```wxml
<view class="animate-fade-in animate-scale-in" style="animation-delay: 0.2s">
  <view class="animate-slide-in-up">
    <text>组合动画</text>
  </view>
</view>
```

### 2. 加载骨架屏

```wxml
<view class="card">
  <view class="skeleton" style="width: 200rpx; height: 40rpx; margin-bottom: 20rpx;"></view>
  <view class="skeleton" style="width: 300rpx; height: 30rpx; margin-bottom: 20rpx;"></view>
  <view class="skeleton" style="width: 250rpx; height: 30rpx;"></view>
</view>
```

### 3. 打字机效果

```wxml
<view class="flex items-center">
  <text>AI 正在输入</text>
  <text class="typing-cursor ml-1"></text>
</view>
```

### 4. 成功提示

```wxml
<view class="toast animate-bounce-in">
  <view class="flex items-center">
    <text class="text-success">✓</text>
    <text class="ml-2">操作成功</text>
  </view>
</view>
```

## ⚠️ 使用注意事项

1. **性能考虑**
   - 避免同时使用过多动画
   - 在列表滚动时暂停复杂动画
   - 使用 `will-change` 属性优化性能

2. **用户体验**
   - 动画时间不宜过长（建议 200-500ms）
   - 保持动画风格一致
   - 提供关闭动画的选项

3. **兼容性**
   - 所有动画都经过小程序平台测试
   - 在低版本设备上可能自动降级

4. **可访问性**
   - 重要信息不要仅通过动画传达
   - 考虑晕动症用户，避免过度动画

## 🎨 深色模式支持

所有动画都支持深色模式，会自动适配当前主题：

```wxml
<!-- 自动适配深色模式 -->
<view class="card animate-fade-in">
  <text class="text-primary">内容</text>
</view>
```

## 📖 API 参考

### 动画类名速查表

| 类别 | 类名 | 说明 |
|------|------|------|
| 淡入 | `animate-fade-in` | 淡入效果 |
| 淡入 | `animate-fade-in-slow` | 慢速淡入 |
| 淡出 | `animate-fade-out` | 淡出效果 |
| 滑入 | `animate-slide-in-up` | 从下向上滑入 |
| 滑入 | `animate-slide-in-down` | 从上向下滑入 |
| 滑入 | `animate-slide-in-left` | 从左向右滑入 |
| 滑入 | `animate-slide-in-right` | 从右向左滑入 |
| 缩放 | `animate-scale-in` | 缩放进入 |
| 缩放 | `animate-scale-out` | 缩放退出 |
| 弹跳 | `animate-bounce` | 持续弹跳 |
| 弹跳 | `animate-bounce-in` | 弹跳进入 |
| 弹跳 | `animate-bounce-out` | 弹跳退出 |
| 脉冲 | `animate-pulse` | 脉冲效果 |
| 闪烁 | `animate-blink` | 闪烁效果 |
| 旋转 | `animate-spin` | 旋转 |
| 旋转 | `animate-spin-slow` | 慢速旋转 |
| 浮动 | `animate-float` | 浮动效果 |
| 心跳 | `animate-heartbeat` | 心跳效果 |
| 骨架屏 | `skeleton` | 骨架屏加载 |
| 光标 | `typing-cursor` | 打字机光标 |

## 🚀 快速开始

1. 动画库已在全局引入，无需额外配置
2. 直接在组件或页面中使用动画类名
3. 可组合多个动画类实现复杂效果
4. 使用 `style` 属性控制延迟和持续时间

```wxml
<!-- 最简单的使用方式 -->
<view class="animate-fade-in">Hello World</view>

<!-- 带延迟的动画 -->
<view class="animate-slide-in-up" style="animation-delay: 0.3s">延迟出现</view>

<!-- 组合动画 -->
<view class="hover-lift animate-scale-in">悬浮缩放</view>
```

## 📝 示例代码

完整示例请参考 `pages/index/index.wxml` 文件。
