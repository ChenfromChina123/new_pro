import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// 引入Font Awesome样式
import '@fortawesome/fontawesome-free/css/all.min.css'

// 引入KaTeX样式
import 'katex/dist/katex.min.css'

import './assets/styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')

