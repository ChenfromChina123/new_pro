/**
 * 简历网站交互效果
 * 增强用户体验和视觉效果
 */

document.addEventListener('DOMContentLoaded', function() {
  // ===== 平滑滚动效果 =====
  const smoothScrollLinks = document.querySelectorAll('a[href^="#"]');
  smoothScrollLinks.forEach(link => {
    link.addEventListener('click', function(e) {
      e.preventDefault();
      const targetId = this.getAttribute('href');
      if (targetId === '#') return;
      
      const targetElement = document.querySelector(targetId);
      if (targetElement) {
        window.scrollTo({
          top: targetElement.offsetTop - 80,
          behavior: 'smooth'
        });
      }
    });
  });

  // ===== 技能标签悬停效果增强 =====
  const techTags = document.querySelectorAll('.tech-tag');
  techTags.forEach(tag => {
    tag.addEventListener('mouseenter', function() {
      this.style.transform = 'translateY(-3px) scale(1.05)';
    });
    
    tag.addEventListener('mouseleave', function() {
      this.style.transform = 'translateY(0) scale(1)';
    });
  });

  // ===== 卡片悬停效果 =====
  const cards = document.querySelectorAll('.project-item, .work-item, .education-item, .skill-detail-item');
  cards.forEach(card => {
    card.addEventListener('mouseenter', function() {
      this.style.zIndex = '10';
    });
    
    card.addEventListener('mouseleave', function() {
      this.style.zIndex = '1';
    });
  });

  // ===== 头像旋转效果 =====
  const avatar = document.querySelector('.avatar');
  if (avatar) {
    avatar.addEventListener('mouseenter', function() {
      this.style.transform = 'scale(1.05) rotate(5deg)';
    });
    
    avatar.addEventListener('mouseleave', function() {
      this.style.transform = 'scale(1) rotate(0deg)';
    });
  }

  // ===== 页面加载动画 =====
  function createLoadingSpinner() {
    const spinner = document.createElement('div');
    spinner.className = 'loading-spinner';
    spinner.style.display = 'none';
    document.body.appendChild(spinner);
    return spinner;
  }

  const loadingSpinner = createLoadingSpinner();

  // ===== 滚动时显示/隐藏头部阴影 =====
  const header = document.querySelector('.header');
  let lastScrollTop = 0;
  
  window.addEventListener('scroll', function() {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    
    if (header) {
      if (scrollTop > 100) {
        header.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.2)';
        header.style.background = 'rgba(30, 41, 59, 0.95)';
        header.style.backdropFilter = 'blur(10px)';
      } else {
        header.style.boxShadow = 'none';
        header.style.background = 'transparent';
        header.style.backdropFilter = 'none';
      }
    }
    
    lastScrollTop = scrollTop;
  });

  // ===== 打字机效果（可选） =====
  function typeWriterEffect(element, text, speed = 50) {
    let i = 0;
    element.textContent = '';
    
    function type() {
      if (i < text.length) {
        element.textContent += text.charAt(i);
        i++;
        setTimeout(type, speed);
      }
    }
    
    type();
  }

  // 如果有需要打字机效果的元素，可以启用
  const typeWriterElements = document.querySelectorAll('[data-typewriter]');
  typeWriterElements.forEach(element => {
    const text = element.getAttribute('data-text') || element.textContent;
    const speed = parseInt(element.getAttribute('data-speed')) || 50;
    typeWriterEffect(element, text, speed);
  });

  // ===== 视差滚动效果 =====
  function initParallax() {
    const parallaxElements = document.querySelectorAll('[data-parallax]');
    
    window.addEventListener('scroll', function() {
      const scrolled = window.pageYOffset;
      
      parallaxElements.forEach(element => {
        const speed = parseFloat(element.getAttribute('data-speed')) || 0.5;
        const yPos = -(scrolled * speed);
        element.style.transform = `translateY(${yPos}px)`;
      });
    });
  }

  // 初始化视差效果
  initParallax();

  // ===== 主题切换功能（可选） =====
  function createThemeToggle() {
    const themeToggle = document.createElement('button');
    themeToggle.className = 'theme-toggle';
  // ===== 主题切换功能 =====
  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    // 检查本地存储中的主题偏好
    const savedTheme = localStorage.getItem('resume-theme') || 'dark';
    document.body.setAttribute('data-theme', savedTheme);
    updateThemeButton(savedTheme);
    
    themeToggle.addEventListener('click', function(e) {
      e.preventDefault();
      const currentTheme = document.body.getAttribute('data-theme');
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      
      // 切换主题
      document.body.setAttribute('data-theme', newTheme);
      updateThemeButton(newTheme);
      
      // 保存到本地存储
      localStorage.setItem('resume-theme', newTheme);
      
      // 添加切换动画
      document.body.classList.add('theme-changing');
      setTimeout(() => {
        document.body.classList.remove('theme-changing');
      }, 300);
    });
  }
  
  function updateThemeButton(theme) {
    if (themeToggle) {
  // ===== 技能进度条动画 =====
  function animateSkillBars() {
    const skillBars = document.querySelectorAll('.skill-progress-bar');
    
    // 创建Intersection Observer来检测元素是否进入视口
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
      threshold: 0.3, // 当30%的元素可见时触发
      rootMargin: '0px 0px -50px 0px' // 底部偏移
    });
    
    // 开始观察所有技能条
    skillBars.forEach(bar => {
      bar.style.width = '0%'; // 初始宽度为0
      observer.observe(bar);
    });
  }
  
  // 页面加载完成后初始化技能条动画
  window.addEventListener('load', function() {
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
      if (currentTheme === 'light') {
        document.body.setAttribute('data-theme', 'dark');
        this.innerHTML = '🌙';
      } else {
        document.body.setAttribute('data-theme', 'light');
        this.innerHTML = '☀️';
      }
    });
    
    document.body.appendChild(themeToggle);
  }

  // 如果需要主题切换功能，可以取消注释下面这行
  // createThemeToggle();

  // ===== 页面加载完成动画 =====
  window.addEventListener('load', function() {
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
          const originalBorder = this.style.borderColor;
          
          this.style.background = 'var(--primary-color)';
          this.style.borderColor = 'var(--primary-color)';
          
          setTimeout(() => {
            this.style.background = originalBg;
            this.style.borderColor = originalBorder;
          }, 500);
        }
      } catch (err) {
        console.log('复制失败:', err);
      }
      
      document.body.removeChild(tempInput);
    });
  });

  // ===== 控制台欢迎信息 =====
  console.log('%c👋 欢迎访问我的简历网站！', 'color: #2563eb; font-size: 16px; font-weight: bold;');
  console.log('%c💻 Java后端开发工程师 | AI应用探索者', 'color: #06b6d4; font-size: 14px;');
  console.log('%c📧 联系我: 3301767269@qq.com', 'color: #94a3b8; font-size: 12px;');
});

// ===== 工具函数 =====
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

function throttle(func, limit) {
  let inThrottle;
  return function() {
    const args = arguments;
    const context = this;
    if (!inThrottle) {
      func.apply(context, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}