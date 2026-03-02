/**
 * 个人简历网页交互脚本
 * 实现项目经历的可折叠功能
 */

document.addEventListener('DOMContentLoaded', function() {
  // 获取所有可折叠的项目
  const collapsibleItems = document.querySelectorAll('.collapsible');
  
  // 初始化所有项目为展开状态
  collapsibleItems.forEach(item => {
    item.classList.remove('collapsed');
  });
  
  // 为每个折叠头部添加点击事件
  const collapsibleHeaders = document.querySelectorAll('.collapsible-header');
  collapsibleHeaders.forEach(header => {
    header.addEventListener('click', function(e) {
      // 阻止事件冒泡
      e.stopPropagation();
      
      // 获取父级可折叠元素
      const collapsibleItem = this.closest('.collapsible');
      
      // 切换折叠状态
      if (collapsibleItem) {
        collapsibleItem.classList.toggle('collapsed');
      }
    });
  });
  
  // 为折叠按钮添加独立点击事件（可选，提供额外的点击区域）
  const collapseToggles = document.querySelectorAll('.collapse-toggle');
  collapseToggles.forEach(toggle => {
    toggle.addEventListener('click', function(e) {
      e.stopPropagation();
      
      // 获取父级可折叠元素
      const collapsibleItem = this.closest('.collapsible');
      
      // 切换折叠状态
      if (collapsibleItem) {
        collapsibleItem.classList.toggle('collapsed');
      }
    });
  });
});
