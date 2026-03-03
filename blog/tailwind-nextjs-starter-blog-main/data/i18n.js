/**
 * 中文国际化配置
 * 根据 IP 地理位置自动切换语言
 */

const zhCN = {
  // 导航栏
  nav: {
    home: '首页',
    blog: '博客',
    about: '关于',
    projects: '项目',
    tags: '标签',
  },
  // 首页
  home: {
    title: 'AI Study Platform Blog',
    subtitle: '分享 AI 学习平台开发技术与经验',
    latestPosts: '最新文章',
    viewAll: '查看全部',
    noPosts: '没有找到文章。',
  },
  // 博客列表
  blog: {
    title: '博客文章',
    readMore: '阅读全文',
    publishedOn: '发表于',
    updatedOn: '更新于',
    readingTime: '分钟阅读',
    tags: '标签',
    noPosts: '没有找到文章。',
    allPosts: '所有文章',
  },
  // 文章页面
  post: {
    tableOfContents: '目录',
    backToBlog: '返回博客',
    previous: '上一篇',
    next: '下一篇',
    comments: '评论',
    share: '分享',
  },
  // 关于页面
  about: {
    title: '关于本站',
    description: '这是一个关于 AI 学习平台开发的技术博客',
  },
  // 项目页面
  projects: {
    title: '项目展示',
    description: '展示我们开发的 AI 学习相关项目',
  },
  // 搜索
  search: {
    placeholder: '搜索文章...',
    noResults: '未找到结果',
    search: '搜索',
  },
  // 标签页面
  tags: {
    title: '标签',
    description: '浏览所有标签',
    noTags: '暂无标签',
  },
  // 订阅
  newsletter: {
    title: '订阅我们的通讯',
    placeholder: '请输入您的邮箱',
    subscribed: '您已订阅！🎉',
    button: '订阅',
    thanks: '谢谢！',
    invalidEmail: '您的邮箱地址无效或者您已经订阅！',
  },
  // 页脚
  footer: {
    copyright: '版权所有',
    poweredBy: '技术支持',
    theme: 'Tailwind Nextjs 主题',
  },
}

const enUS = {
  // Navigation
  nav: {
    home: 'Home',
    blog: 'Blog',
    about: 'About',
    projects: 'Projects',
    tags: 'Tags',
  },
  // Home
  home: {
    title: 'AI Study Platform Blog',
    subtitle: 'Sharing AI learning platform development technologies and experiences',
    latestPosts: 'Latest Posts',
    viewAll: 'View All',
    noPosts: 'No posts found.',
  },
  // Blog
  blog: {
    title: 'Blog Posts',
    readMore: 'Read more',
    publishedOn: 'Published on',
    updatedOn: 'Updated on',
    readingTime: 'min read',
    tags: 'Tags',
    noPosts: 'No posts found.',
    allPosts: 'All Posts',
  },
  // Post
  post: {
    tableOfContents: 'Table of Contents',
    backToBlog: 'Back to Blog',
    previous: 'Previous',
    next: 'Next',
    comments: 'Comments',
    share: 'Share',
  },
  // About
  about: {
    title: 'About',
    description: 'A technical blog about AI learning platform development',
  },
  // Projects
  projects: {
    title: 'Projects',
    description: 'Showcasing our AI learning related projects',
  },
  // Search
  search: {
    placeholder: 'Search articles...',
    noResults: 'No results found',
    search: 'Search',
  },
  // Tags
  tags: {
    title: 'Tags',
    description: 'Browse all tags',
    noTags: 'No tags found',
  },
  // Newsletter
  newsletter: {
    title: 'Subscribe to the newsletter',
    placeholder: 'Enter your email',
    subscribed: "You're subscribed! 🎉",
    button: 'Sign up',
    thanks: 'Thank you!',
    invalidEmail: 'Your e-mail address is invalid or you are already subscribed!',
  },
  // Footer
  footer: {
    copyright: 'Copyright',
    poweredBy: 'Powered by',
    theme: 'Tailwind Nextjs Theme',
  },
}

export { zhCN, enUS }
