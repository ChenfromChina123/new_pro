<template>
  <span class="file-icon" :class="[iconClass, typeClass]" :style="iconStyle">
    <component :is="iconComponent" v-if="iconComponent" />
    <span v-else class="icon-text">{{ iconText }}</span>
  </span>
</template>

<script setup>
/**
 * 文件图标组件
 * 根据文件类型显示不同的图标和颜色
 */
import { computed } from 'vue'

const props = defineProps({
  fileName: {
    type: String,
    required: true
  },
  isDirectory: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  }
})

const fileTypeConfig = computed(() => {
  if (props.isDirectory) {
    return {
      type: 'folder',
      icon: 'folder',
      color: '#f0c674',
      bgColor: 'rgba(240, 198, 116, 0.15)'
    }
  }

  const ext = props.fileName.split('.').pop()?.toLowerCase()
  const baseName = props.fileName.toLowerCase()

  const typeConfigs = {
    folder: { type: 'folder', icon: 'folder', color: '#f0c674', bgColor: 'rgba(240, 198, 116, 0.15)' },

    javascript: { type: 'code', icon: 'js', color: '#f7df1e', bgColor: 'rgba(247, 223, 30, 0.15)' },
    typescript: { type: 'code', icon: 'ts', color: '#3178c6', bgColor: 'rgba(49, 120, 198, 0.15)' },
    vue: { type: 'code', icon: 'vue', color: '#42b883', bgColor: 'rgba(66, 184, 131, 0.15)' },
    react: { type: 'code', icon: 'react', color: '#61dafb', bgColor: 'rgba(97, 218, 251, 0.15)' },
    html: { type: 'code', icon: 'html', color: '#e34c26', bgColor: 'rgba(227, 76, 38, 0.15)' },
    css: { type: 'code', icon: 'css', color: '#264de4', bgColor: 'rgba(38, 77, 228, 0.15)' },
    json: { type: 'data', icon: 'json', color: '#cbcb41', bgColor: 'rgba(203, 203, 65, 0.15)' },

    python: { type: 'code', icon: 'py', color: '#3776ab', bgColor: 'rgba(55, 118, 171, 0.15)' },
    java: { type: 'code', icon: 'java', color: '#b07219', bgColor: 'rgba(176, 114, 25, 0.15)' },
    go: { type: 'code', icon: 'go', color: '#00add8', bgColor: 'rgba(0, 173, 216, 0.15)' },
    rust: { type: 'code', icon: 'rs', color: '#dea584', bgColor: 'rgba(222, 165, 132, 0.15)' },
    php: { type: 'code', icon: 'php', color: '#777bb4', bgColor: 'rgba(119, 123, 180, 0.15)' },
    ruby: { type: 'code', icon: 'rb', color: '#cc342d', bgColor: 'rgba(204, 52, 45, 0.15)' },
    c: { type: 'code', icon: 'c', color: '#555555', bgColor: 'rgba(85, 85, 85, 0.15)' },
    cpp: { type: 'code', icon: 'cpp', color: '#f34b7d', bgColor: 'rgba(243, 75, 125, 0.15)' },
    csharp: { type: 'code', icon: 'cs', color: '#178600', bgColor: 'rgba(23, 134, 0, 0.15)' },
    swift: { type: 'code', icon: 'swift', color: '#f05138', bgColor: 'rgba(240, 81, 56, 0.15)' },
    kotlin: { type: 'code', icon: 'kt', color: '#7f52ff', bgColor: 'rgba(127, 82, 255, 0.15)' },

    shell: { type: 'script', icon: 'shell', color: '#89e051', bgColor: 'rgba(137, 224, 81, 0.15)' },

    image: { type: 'image', icon: 'image', color: '#a855f7', bgColor: 'rgba(168, 85, 247, 0.15)' },
    video: { type: 'video', icon: 'video', color: '#ef4444', bgColor: 'rgba(239, 68, 68, 0.15)' },
    audio: { type: 'audio', icon: 'audio', color: '#22c55e', bgColor: 'rgba(34, 197, 94, 0.15)' },

    archive: { type: 'archive', icon: 'archive', color: '#f59e0b', bgColor: 'rgba(245, 158, 11, 0.15)' },

    document: { type: 'document', icon: 'doc', color: '#3b82f6', bgColor: 'rgba(59, 130, 246, 0.15)' },
    pdf: { type: 'document', icon: 'pdf', color: '#dc2626', bgColor: 'rgba(220, 38, 38, 0.15)' },
    excel: { type: 'document', icon: 'excel', color: '#16a34a', bgColor: 'rgba(22, 163, 74, 0.15)' },
    ppt: { type: 'document', icon: 'ppt', color: '#ea580c', bgColor: 'rgba(234, 88, 12, 0.15)' },

    config: { type: 'config', icon: 'config', color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.15)' },
    database: { type: 'database', icon: 'db', color: '#8b5cf6', bgColor: 'rgba(139, 92, 246, 0.15)' },

    executable: { type: 'executable', icon: 'exe', color: '#10b981', bgColor: 'rgba(16, 185, 129, 0.15)' },

    markdown: { type: 'document', icon: 'md', color: '#083fa6', bgColor: 'rgba(8, 63, 166, 0.15)' },
    text: { type: 'document', icon: 'txt', color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.15)' },

    default: { type: 'file', icon: 'file', color: '#6b7280', bgColor: 'rgba(107, 114, 128, 0.1)' }
  }

  const specialFiles = {
    'package.json': typeConfigs.javascript,
    'package-lock.json': { ...typeConfigs.json, icon: 'lock' },
    'yarn.lock': { ...typeConfigs.json, icon: 'lock' },
    'pnpm-lock.yaml': { ...typeConfigs.json, icon: 'lock' },
    'tsconfig.json': typeConfigs.typescript,
    'jsconfig.json': typeConfigs.javascript,
    'vite.config.js': typeConfigs.javascript,
    'vite.config.ts': typeConfigs.typescript,
    'webpack.config.js': typeConfigs.javascript,
    'rollup.config.js': typeConfigs.javascript,
    '.gitignore': { ...typeConfigs.config, icon: 'git' },
    '.env': typeConfigs.config,
    '.env.local': typeConfigs.config,
    '.env.development': typeConfigs.config,
    '.env.production': typeConfigs.config,
    'dockerfile': { ...typeConfigs.config, icon: 'docker', color: '#2496ed', bgColor: 'rgba(36, 150, 237, 0.15)' },
    'docker-compose.yml': { ...typeConfigs.config, icon: 'docker', color: '#2496ed', bgColor: 'rgba(36, 150, 237, 0.15)' },
    'docker-compose.yaml': { ...typeConfigs.config, icon: 'docker', color: '#2496ed', bgColor: 'rgba(36, 150, 237, 0.15)' },
    'makefile': typeConfigs.shell,
    'readme.md': { ...typeConfigs.markdown, icon: 'readme' },
    'license': typeConfigs.document,
    'changelog.md': typeConfigs.markdown,
    '.eslintrc': typeConfigs.config,
    '.eslintrc.js': typeConfigs.config,
    '.eslintrc.json': typeConfigs.config,
    '.prettierrc': typeConfigs.config,
    '.prettierrc.js': typeConfigs.config,
    '.prettierrc.json': typeConfigs.config
  }

  if (specialFiles[baseName]) {
    return specialFiles[baseName]
  }

  const extensionMap = {
    js: typeConfigs.javascript,
    mjs: typeConfigs.javascript,
    cjs: typeConfigs.javascript,
    ts: typeConfigs.typescript,
    jsx: typeConfigs.react,
    tsx: typeConfigs.react,
    vue: typeConfigs.vue,
    html: typeConfigs.html,
    htm: typeConfigs.html,
    css: typeConfigs.css,
    scss: typeConfigs.css,
    less: typeConfigs.css,
    sass: typeConfigs.css,
    json: typeConfigs.json,

    py: typeConfigs.python,
    pyw: typeConfigs.python,
    java: typeConfigs.java,
    jar: typeConfigs.java,
    go: typeConfigs.go,
    rs: typeConfigs.rust,
    php: typeConfigs.php,
    rb: typeConfigs.ruby,
    c: typeConfigs.c,
    h: typeConfigs.c,
    cpp: typeConfigs.cpp,
    hpp: typeConfigs.cpp,
    cc: typeConfigs.cpp,
    cs: typeConfigs.csharp,
    swift: typeConfigs.swift,
    kt: typeConfigs.kotlin,
    kts: typeConfigs.kotlin,

    sh: typeConfigs.shell,
    bash: typeConfigs.shell,
    zsh: typeConfigs.shell,
    bat: typeConfigs.shell,
    cmd: typeConfigs.shell,
    ps1: typeConfigs.shell,

    png: typeConfigs.image,
    jpg: typeConfigs.image,
    jpeg: typeConfigs.image,
    gif: typeConfigs.image,
    svg: typeConfigs.image,
    webp: typeConfigs.image,
    ico: typeConfigs.image,
    bmp: typeConfigs.image,

    mp4: typeConfigs.video,
    avi: typeConfigs.video,
    mkv: typeConfigs.video,
    mov: typeConfigs.video,
    wmv: typeConfigs.video,
    flv: typeConfigs.video,
    webm: typeConfigs.video,

    mp3: typeConfigs.audio,
    wav: typeConfigs.audio,
    flac: typeConfigs.audio,
    aac: typeConfigs.audio,
    ogg: typeConfigs.audio,
    wma: typeConfigs.audio,

    zip: typeConfigs.archive,
    rar: typeConfigs.archive,
    tar: typeConfigs.archive,
    gz: typeConfigs.archive,
    '7z': typeConfigs.archive,
    bz2: typeConfigs.archive,
    xz: typeConfigs.archive,

    pdf: typeConfigs.pdf,
    doc: typeConfigs.document,
    docx: typeConfigs.document,
    xls: typeConfigs.excel,
    xlsx: typeConfigs.excel,
    ppt: typeConfigs.ppt,
    pptx: typeConfigs.ppt,

    md: typeConfigs.markdown,
    markdown: typeConfigs.markdown,
    txt: typeConfigs.text,
    log: typeConfigs.text,

    sql: typeConfigs.database,
    db: typeConfigs.database,
    sqlite: typeConfigs.database,

    xml: typeConfigs.data,
    yaml: typeConfigs.config,
    yml: typeConfigs.config,
    toml: typeConfigs.config,
    ini: typeConfigs.config,
    env: typeConfigs.config,

    exe: typeConfigs.executable,
    msi: typeConfigs.executable,
    app: typeConfigs.executable,
    dmg: typeConfigs.executable,
    deb: typeConfigs.executable,
    rpm: typeConfigs.executable,

    lock: { ...typeConfigs.default, icon: 'lock' },
    map: typeConfigs.default
  }

  return extensionMap[ext] || typeConfigs.default
})

const iconClass = computed(() => `icon-${props.size}`)
const typeClass = computed(() => `type-${fileTypeConfig.value.type}`)

const iconStyle = computed(() => ({
  '--icon-color': fileTypeConfig.value.color,
  '--icon-bg': fileTypeConfig.value.bgColor
}))

const iconText = computed(() => {
  const icon = fileTypeConfig.value.icon
  const iconTexts = {
    folder: '📁',
    file: '📄',
    js: 'JS',
    ts: 'TS',
    vue: 'V',
    react: '⚛',
    html: '🌐',
    css: '🎨',
    json: '{ }',
    py: '🐍',
    java: '☕',
    go: '🔵',
    rs: '🦀',
    php: '🐘',
    rb: '💎',
    c: 'C',
    cpp: 'C++',
    cs: 'C#',
    swift: '🍎',
    kt: '🟣',
    shell: '🖥',
    image: '🖼',
    video: '🎬',
    audio: '🎵',
    archive: '📦',
    doc: '📄',
    pdf: '📕',
    excel: '📊',
    ppt: '📊',
    md: '📝',
    txt: '📄',
    config: '⚙',
    db: '🗃',
    exe: '⚡',
    lock: '🔒',
    git: '🙈',
    docker: '🐳',
    readme: '📖'
  }
  return iconTexts[icon] || '📄'
})

const iconComponent = computed(() => null)
</script>

<style scoped>
.file-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: var(--icon-bg);
  color: var(--icon-color);
  font-weight: 600;
  flex-shrink: 0;
}

.icon-sm {
  width: 20px;
  height: 20px;
  font-size: 10px;
}

.icon-md {
  width: 24px;
  height: 24px;
  font-size: 12px;
}

.icon-lg {
  width: 32px;
  height: 32px;
  font-size: 14px;
}

.icon-text {
  line-height: 1;
}

.type-folder {
  background: rgba(240, 198, 116, 0.2);
  color: #f0c674;
}

.type-code {
  background: rgba(97, 218, 251, 0.15);
}

.type-image {
  background: rgba(168, 85, 247, 0.15);
}

.type-video {
  background: rgba(239, 68, 68, 0.15);
}

.type-audio {
  background: rgba(34, 197, 94, 0.15);
}

.type-archive {
  background: rgba(245, 158, 11, 0.15);
}

.type-document {
  background: rgba(59, 130, 246, 0.15);
}

.type-config {
  background: rgba(107, 114, 128, 0.15);
}

.type-database {
  background: rgba(139, 92, 246, 0.15);
}

.type-executable {
  background: rgba(16, 185, 129, 0.15);
}
</style>
