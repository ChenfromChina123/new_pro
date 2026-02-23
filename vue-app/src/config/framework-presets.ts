/**
 * 框架预设配置
 * 支持 Vue、React、Next.js、Nuxt 等主流框架
 */

/**
 * 框架预设接口
 */
export interface FrameworkPreset {
  /** 框架名称 */
  name: string
  /** 框架标识 */
  id: string
  /** 图标 */
  icon: string
  /** 主要语言 */
  languages: string[]
  /** 关键文件模式 */
  keyFilePatterns: string[]
  /** 项目结构 */
  projectStructure: string[]
  /** 常用命令 */
  commonCommands: Record<string, string>
  /** 环境变量 */
  envVariables: string[]
  /** 依赖包管理器 */
  packageManager: 'npm' | 'yarn' | 'pnpm' | 'bun'
  /** 快捷提示词模板 */
  templates: PromptTemplate[]
}

/**
 * 提示词模板
 */
export interface PromptTemplate {
  title: string
  description: string
  template: string
}

/**
 * Vue框架预设
 */
export const vuePreset: FrameworkPreset = {
  name: 'Vue.js',
  id: 'vue',
  icon: '💚',
  languages: ['javascript', 'typescript', 'vue'],
  keyFilePatterns: [
    'vite.config.ts',
    'vite.config.js',
    'vue.config.js',
    'src/main.ts',
    'src/main.js',
    'src/App.vue'
  ],
  projectStructure: [
    'src/',
    'src/components/',
    'src/views/',
    'src/router/',
    'src/stores/',
    'src/assets/',
    'public/'
  ],
  commonCommands: {
    dev: 'npm run dev',
    build: 'npm run build',
    preview: 'npm run preview',
    lint: 'npm run lint',
    test: 'npm run test'
  },
  envVariables: ['VITE_API_URL', 'VITE_APP_TITLE'],
  packageManager: 'npm',
  templates: [
    {
      title: '创建Vue组件',
      description: '创建新的Vue组件文件',
      template: '请创建一个Vue3组件 ${componentName}，包含 ${props} 属性和 ${events} 事件'
    },
    {
      title: '添加路由',
      description: '在Vue Router中添加新路由',
      template: '请在router中添加 ${path} 路由，对应组件 ${component}'
    },
    {
      title: '创建Pinia Store',
      description: '创建新的Pinia状态管理',
      template: '请创建一个Pinia store ${storeName}，管理 ${state} 状态'
    },
    {
      title: '优化性能',
      description: '优化Vue组件性能',
      template: '请优化 ${component} 组件的性能，重点关注 ${aspect}'
    }
  ]
}

/**
 * React框架预设
 */
export const reactPreset: FrameworkPreset = {
  name: 'React',
  id: 'react',
  icon: '⚛️',
  languages: ['javascript', 'typescript', 'jsx', 'tsx'],
  keyFilePatterns: [
    'package.json',
    'src/App.tsx',
    'src/App.jsx',
    'src/index.tsx',
    'src/index.jsx',
    'vite.config.ts',
    'webpack.config.js'
  ],
  projectStructure: [
    'src/',
    'src/components/',
    'src/pages/',
    'src/hooks/',
    'src/utils/',
    'src/styles/',
    'public/'
  ],
  commonCommands: {
    dev: 'npm start',
    build: 'npm run build',
    test: 'npm test',
    eject: 'npm run eject'
  },
  envVariables: ['REACT_APP_API_URL', 'REACT_APP_ENV'],
  packageManager: 'npm',
  templates: [
    {
      title: '创建React组件',
      description: '创建新的React函数组件',
      template: '请创建一个React函数组件 ${componentName}，使用TypeScript，包含 ${props} 属性'
    },
    {
      title: '创建自定义Hook',
      description: '创建自定义React Hook',
      template: '请创建一个自定义Hook ${hookName}，用于 ${purpose}'
    },
    {
      title: '添加Context',
      description: '创建Context Provider',
      template: '请创建Context ${contextName}，管理 ${state} 状态'
    },
    {
      title: '性能优化',
      description: '优化React组件渲染',
      template: '请优化 ${component} 的渲染性能，使用useMemo和useCallback'
    }
  ]
}

/**
 * Next.js框架预设
 */
export const nextPreset: FrameworkPreset = {
  name: 'Next.js',
  id: 'nextjs',
  icon: '▲',
  languages: ['javascript', 'typescript', 'jsx', 'tsx'],
  keyFilePatterns: [
    'next.config.js',
    'next.config.mjs',
    'pages/_app.tsx',
    'pages/_app.jsx',
    'app/layout.tsx',
    'app/page.tsx'
  ],
  projectStructure: [
    'app/',
    'pages/',
    'components/',
    'lib/',
    'public/',
    'styles/',
    'api/'
  ],
  commonCommands: {
    dev: 'npm run dev',
    build: 'npm run build',
    start: 'npm start',
    lint: 'npm run lint'
  },
  envVariables: ['NEXT_PUBLIC_API_URL', 'DATABASE_URL'],
  packageManager: 'npm',
  templates: [
    {
      title: '创建页面',
      description: '创建Next.js页面',
      template: '请创建Next.js页面 ${pagePath}，包含 ${features} 功能'
    },
    {
      title: '创建API路由',
      description: '创建API路由处理器',
      template: '请创建API路由 /api/${endpoint}，处理 ${method} 请求'
    },
    {
      title: '实现SSR',
      description: '实现服务端渲染',
      template: '请为 ${page} 实现服务端渲染，获取 ${data} 数据'
    },
    {
      title: '配置中间件',
      description: '配置Next.js中间件',
      template: '请创建中间件，用于 ${purpose}'
    }
  ]
}

/**
 * Nuxt框架预设
 */
export const nuxtPreset: FrameworkPreset = {
  name: 'Nuxt.js',
  id: 'nuxt',
  icon: '💚',
  languages: ['javascript', 'typescript', 'vue'],
  keyFilePatterns: [
    'nuxt.config.ts',
    'nuxt.config.js',
    'app.vue',
    'pages/index.vue'
  ],
  projectStructure: [
    'pages/',
    'components/',
    'layouts/',
    'composables/',
    'plugins/',
    'server/',
    'public/'
  ],
  commonCommands: {
    dev: 'npm run dev',
    build: 'npm run build',
    generate: 'npm run generate',
    preview: 'npm run preview'
  },
  envVariables: ['NUXT_PUBLIC_API_BASE', 'DATABASE_URL'],
  packageManager: 'npm',
  templates: [
    {
      title: '创建页面',
      description: '创建Nuxt页面',
      template: '请创建Nuxt页面 pages/${pagePath}.vue，包含 ${features}'
    },
    {
      title: '创建Composable',
      description: '创建可组合函数',
      template: '请创建composable ${name}，用于 ${purpose}'
    },
    {
      title: '配置插件',
      description: '配置Nuxt插件',
      template: '请创建插件 ${pluginName}，用于 ${functionality}'
    }
  ]
}

/**
 * 通用预设（用于未识别的项目）
 */
export const genericPreset: FrameworkPreset = {
  name: '通用项目',
  id: 'generic',
  icon: '📦',
  languages: ['javascript', 'typescript'],
  keyFilePatterns: ['package.json'],
  projectStructure: ['src/', 'dist/', 'public/'],
  commonCommands: {
    install: 'npm install',
    build: 'npm run build',
    start: 'npm start',
    test: 'npm test'
  },
  envVariables: ['NODE_ENV'],
  packageManager: 'npm',
  templates: [
    {
      title: '创建文件',
      description: '创建新文件',
      template: '请创建文件 ${filePath}，包含 ${content}'
    },
    {
      title: '执行命令',
      description: '执行终端命令',
      template: '请执行命令: ${command}'
    },
    {
      title: '分析代码',
      description: '分析代码结构',
      template: '请分析 ${file} 的代码结构和问题'
    }
  ]
}

/**
 * 所有预设配置
 */
export const frameworkPresets: Record<string, FrameworkPreset> = {
  vue: vuePreset,
  react: reactPreset,
  nextjs: nextPreset,
  nuxt: nuxtPreset,
  generic: genericPreset
}

/**
 * 检测项目框架
 */
export function detectFramework(files: string[]): FrameworkPreset {
  // 检查Next.js
  if (files.some(f => /next\.config\.(js|mjs|ts)$/.test(f))) {
    return nextPreset
  }

  // 检查Nuxt
  if (files.some(f => /nuxt\.config\.(js|ts)$/.test(f))) {
    return nuxtPreset
  }

  // 检查Vue
  if (files.some(f => /vite\.config\.(js|ts)$/.test(f)) &&
      files.some(f => /\.vue$/.test(f))) {
    return vuePreset
  }

  // 检查React
  if (files.some(f => /\.(jsx|tsx)$/.test(f))) {
    return reactPreset
  }

  // 默认通用预设
  return genericPreset
}

/**
 * 获取框架配置
 */
export function getFrameworkPreset(frameworkId: string): FrameworkPreset {
  return frameworkPresets[frameworkId] || genericPreset
}

