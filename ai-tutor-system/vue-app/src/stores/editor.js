import { defineStore } from 'pinia'

export const useEditorStore = defineStore('editor', {
  state: () => ({
    openFiles: [],       // 当前打开的所有文件对象 { name, path, content, type }
    activeFilePath: null, // 当前正在编辑的文件路径
    isDirty: new Set(),  // 记录未保存的文件路径
  }),

  getters: {
    activeFile: (state) => state.openFiles.find(f => f.path === state.activeFilePath),

    // 根据后缀名返回编辑器类型
    editorType: (state) => {
      if (!state.activeFilePath) return null
      const ext = state.activeFilePath.split('.').pop().toLowerCase()
      const codeExts = ['js', 'ts', 'vue', 'py', 'json', 'css', 'html', 'sql']
      const mdExts = ['md', 'markdown']

      if (codeExts.includes(ext)) return 'monaco'
      if (mdExts.includes(ext)) return 'markdown'
      return 'richtext' // 默认文本
    }
  },

  actions: {
    openFile(file) {
      if (!this.openFiles.find(f => f.path === file.path)) {
        this.openFiles.push(file)
      }
      this.activeFilePath = file.path
    },
    closeFile(path) {
      this.openFiles = this.openFiles.filter(f => f.path !== path)
      if (this.activeFilePath === path) {
        this.activeFilePath = this.openFiles[this.openFiles.length - 1]?.path || null
      }
      this.isDirty.delete(path)
    },
    updateFileContent(path, content) {
      const file = this.openFiles.find(f => f.path === path)
      if (file) {
        file.content = content
        this.isDirty.add(path)
      }
    },
    saveFile(path) {
      this.isDirty.delete(path)
    }
  }
})
