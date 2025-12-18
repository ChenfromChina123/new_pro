<template>
  <div class="not-found-page">
    <div class="error-content">
      <h1 class="error-code">
        404
      </h1>
      <h2>页面未找到</h2>
      <p>抱歉，您访问的页面不存在</p>
      <router-link
        to="/"
        class="btn btn-primary"
      >
        返回首页
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
}

.error-content {
  text-align: center;
  color: white;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  margin: 0 0 20px 0;
  text-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.error-content h2 {
  font-size: 32px;
  margin: 0 0 16px 0;
}

.error-content p {
  font-size: 18px;
  margin: 0 0 32px 0;
  opacity: 0.9;
}

.btn {
  background-color: white;
  color: var(--primary-color);
  padding: 14px 32px;
  font-size: 16px;
  font-weight: 600;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}
</style>

