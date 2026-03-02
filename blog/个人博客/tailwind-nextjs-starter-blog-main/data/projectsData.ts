interface Project {
  title: string
  description: string
  href?: string
  imgSrc?: string
}

const projectsData: Project[] = [
  {
    title: 'AI智能学习助手',
    description: `基于Spring Boot和Vue.js构建的AI学习助手系统，集成DeepSeek、豆包等大模型，
    提供AI对话、云盘管理、代码助手等功能。支持多模型切换、流式响应、历史记录等特性。`,
    imgSrc: '/static/images/google.png',
    href: 'https://aistudy.icu',
  },
  {
    title: '单词消消乐游戏',
    description: `基于Vue 3开发的单词学习游戏，支持多种游戏模式、课程包管理、学习进度追踪。
    使用Express.js + SQLite构建轻量级后端服务。`,
    imgSrc: '/static/images/time-machine.jpg',
    href: 'https://aistudy.icu/word-game',
  },
  {
    title: 'CodeNova 移动应用',
    description: `AI代码助手移动应用，支持代码生成与优化。集成多个AI模型，提供智能编程辅助功能，
    实现代码高亮、历史记录等核心功能。`,
    imgSrc: '/static/images/time-machine.jpg',
    href: 'https://aistudy.icu',
  },
]

export default projectsData
