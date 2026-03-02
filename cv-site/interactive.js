/**
 * 交互式功能脚本
 * 包含：视差滚动、技能条动画、主题切换、复制功能等
 */

(function() {
  'use strict';

  // ===== 视差滚动效果 =====
  function initParallax() {
    const parallaxElements = document.querySelectorAll('.section');
    
    window.addEventListener('scroll', function() {
      const scrolled = window.pageYOffset;
      
      parallaxElements.forEach(element => {
        const speed = element.dataset.parallaxSpeed || 0.3;
        const yPos = -(scrolled * speed);
        element.style.transform = `translateY(${yPos}px)`;
      });
    });
  }

  // 初始化视差效果
  initParallax();

  // ===== 技能进度条动画 =====
  function animateSkillBars() {
    const skillBars = document.querySelectorAll('.skill-progress-bar');
    
    // 创建 Intersection Observer 来检测元素是否进入视口
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const skillBar = entry.target;
          const level = skillBar.getAttribute('data-level');
          
          // 延迟一点开始动画，让滚动更自然
          setTimeout(() => {
            skillBar.style.width = `${level}%`;
          }, 200);
          
          // 动画完成后停止观察
          observer.unobserve(skillBar);
        }
      });
    }, {
      threshold: 0.3, // 当 30% 的元素可见时触发
      rootMargin: '0px 0px -50px 0px' // 底部偏移
    });
    
    // 开始观察所有技能条
    skillBars.forEach(bar => {
      bar.style.width = '0%'; // 初始宽度为 0
      observer.observe(bar);
    });
  }

  // ===== 页面加载完成动画 =====
  window.addEventListener('load', function() {
    // 初始化技能条动画
    animateSkillBars();
    
    // 移除加载状态
    document.body.classList.add('loaded');
    
    // 添加元素进入动画
    const animatedElements = document.querySelectorAll('.section, .header, .footer');
    animatedElements.forEach((element, index) => {
      element.style.animationDelay = `${index * 0.1}s`;
      element.classList.add('animate-in');
    });
  });

  // ===== 复制联系信息功能 =====
  const contactItems = document.querySelectorAll('.contact-item');
  contactItems.forEach(item => {
    item.style.cursor = 'pointer';
    item.title = '点击复制';
    
    item.addEventListener('click', function() {
      const text = this.textContent.trim();
      
      // 创建临时输入框
      const tempInput = document.createElement('input');
      tempInput.value = text;
      document.body.appendChild(tempInput);
      tempInput.select();
      tempInput.setSelectionRange(0, 99999); // 移动设备兼容
      
      try {
        const successful = document.execCommand('copy');
        if (successful) {
          // 显示复制成功提示
          const originalBg = this.style.background;
          this.style.background = 'rgba(76, 175, 80, 0.2)';
          
          // 创建提示元素
          const toast = document.createElement('div');
          toast.className = 'copy-toast';
          toast.textContent = '✓ 已复制';
          toast.style.cssText = `
            position: fixed;
            bottom: 20px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(76, 175, 80, 0.9);
            color: white;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 14px;
            z-index: 10000;
            animation: toastFadeIn 0.3s ease;
          `;
          
          document.body.appendChild(toast);
          
          setTimeout(() => {
            this.style.background = originalBg;
            toast.remove();
          }, 1500);
        }
      } catch (err) {
        console.error('复制失败:', err);
      }
      
      tempInput.remove();
    });
  });

  // ===== 平滑滚动到锚点 =====
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function(e) {
      const href = this.getAttribute('href');
      
      // 跳过空链接
      if (href === '#' || href === 'javascript:;') {
        return;
      }
      
      e.preventDefault();
      
      const targetId = href.substring(1);
      const targetElement = document.getElementById(targetId);
      
      if (targetElement) {
        const offsetTop = targetElement.offsetTop - 80; // 减去头部高度
        
        window.scrollTo({
          top: offsetTop,
          behavior: 'smooth'
        });
      }
    });
  });

  // ===== 头部阴影效果（滚动时） =====
  const header = document.querySelector('.header');
  if (header) {
    window.addEventListener('scroll', function() {
      if (window.scrollY > 50) {
        header.classList.add('scrolled');
      } else {
        header.classList.remove('scrolled');
      }
    });
  }

  // ===== 项目卡片悬停效果增强 =====
  const projectCards = document.querySelectorAll('.project-item');
  projectCards.forEach(card => {
    card.addEventListener('mouseenter', function() {
      // 添加轻微的上浮效果
      this.style.transform = 'translateY(-5px)';
      this.style.transition = 'transform 0.3s ease, box-shadow 0.3s ease';
    });
    
    card.addEventListener('mouseleave', function() {
      this.style.transform = 'translateY(0)';
    });
  });

  // ===== 添加复制提示动画样式 =====
  const style = document.createElement('style');
  style.textContent = `
    @keyframes toastFadeIn {
      from {
        opacity: 0;
        transform: translateX(-50%) translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateX(-50%) translateY(0);
      }
    }
  `;
  document.head.appendChild(style);

})();
