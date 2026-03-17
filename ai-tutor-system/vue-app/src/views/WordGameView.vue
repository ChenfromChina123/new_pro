<template>
  <div class="word-game-view">
    <div v-if="viewLoading" class="center-state">加载中...</div>
    <div v-else-if="errorMessage" class="center-state">
      <p>{{ errorMessage }}</p>
      <button class="btn btn-primary" @click="reloadCurrentView">重试</button>
    </div>

    <template v-else-if="stage === 'packages'">
      <div class="top-bar">
        <div>
          <h2>单词记忆</h2>
          <p class="sub-text">选择课程包，开始学习之旅</p>
        </div>
        <div class="actions">
          <input v-model="search" class="search" placeholder="搜索课程包" @input="handleSearch" />
          <button class="btn btn-outline" @click="showUpload = true">上传课程包</button>
        </div>
      </div>
      <div class="package-list">
        <button v-for="(pkg, index) in packages" :key="pkg.id" class="card package-item" @click="openPackage(pkg.id)">
          <div class="pkg-rank" :class="{ 'rank-top3': index < 3 }">{{ index + 1 }}</div>
          <div class="pkg-icon-wrap">
            <img v-if="isIconImage(pkg.icon)" :src="pkg.icon" alt="" class="pkg-icon-img" />
            <span v-else class="pkg-icon">{{ pkg.icon || '📦' }}</span>
          </div>
          <div class="pkg-body">
            <div class="title-row">
              <strong>{{ pkg.name }}</strong>
              <span class="pkg-level-badge">{{ pkg.level || '自定义' }}</span>
            </div>
            <p>{{ pkg.description }}</p>
            <small>{{ pkg.courseCount }} 课 · {{ pkg.totalQuestions }} 题 · {{ pkg.clickCount || 0 }} 点击</small>
          </div>
          <div class="pkg-arrow">→</div>
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'courses'">
      <div class="top-bar">
        <button class="btn btn-outline" @click="goPackages">← 课程包</button>
        <div class="courses-head">
          <h2>{{ activePackage?.name || '课程列表' }}</h2>
          <p class="sub-text">
            共 {{ courses.length }} 课
            <template v-if="completedCoursesCount > 0"> · 已完成 {{ completedCoursesCount }} 课</template>
          </p>
        </div>
      </div>
      <div class="course-grid">
        <button
          v-for="course in courses"
          :key="course.index"
          class="card course-item"
          :class="{ 'is-completed': isCourseCompleted(course.index), 'in-progress': getCourseProgressPercent(course.index, course.count) > 0 && !isCourseCompleted(course.index) }"
          @click="openGame(course.index)"
        >
          <div class="course-badge">{{ String(course.index).padStart(2, '0') }}</div>
          <div class="course-content-wrapper">
            <div class="course-main">
              <strong>{{ course.title }}</strong>
              <span>{{ course.count }} 题</span>
              <small v-if="getCourseProgress(course.index)">
                已做到第 {{ (getCourseProgress(course.index)?.currentQuestion || 0) + 1 }} 题
              </small>
            </div>
            <div
              v-if="getCourseProgressPercent(course.index, course.count) > 0 && !isCourseCompleted(course.index)"
              class="course-progress-bar"
            >
              <div class="course-progress-fill" :style="{ width: `${getCourseProgressPercent(course.index, course.count)}%` }" />
            </div>
          </div>
          <div class="course-arrow">{{ isCourseCompleted(course.index) ? '✓' : '→' }}</div>
        </button>
      </div>
      <div v-if="courses.length > 16" class="scroll-tip">
        向上/向下滑动可查看更多课程
      </div>
    </template>

    <template v-else>
      <div class="game-page">
        <header class="game-header">
          <div class="header-inner">
            <button class="btn btn-outline back-btn" @click="goCourses">课程列表</button>
            <div class="course-info">
              <span class="course-title">{{ activeCourseTitle }}</span>
              <span class="progress-text">{{ visibleIndex }} / {{ totalCount }}</span>
            </div>
            <button
              v-if="isQuestion()"
              class="btn btn-outline hint-btn"
              @click="typingInputRef?.toggleAnswerTip()"
            >
              提示
            </button>
            <div v-else class="hint-placeholder" />
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent }" />
          </div>
        </header>

        <main class="game-main">
          <div class="game-content">
            <Transition name="fade">
              <div
                v-if="!gameStarted"
                class="ready-screen"
                @click="startGame"
                @touchstart.passive="startGame"
              >
                <p class="ready-hint">准备好了吗？</p>
                <p class="ready-sub">点击或按任意键开始</p>
              </div>
            </Transition>

            <Transition name="fade">
              <div v-if="gameStarted" class="playing-area">
                <div v-if="!currentQuestion" class="center-state">暂无题目数据，请返回重试</div>
                <template v-else-if="isQuestion()">
                  <div class="chinese-hint">{{ currentQuestion.chinese }}</div>
                  <div class="soundmark">{{ currentQuestion.soundmark }}</div>
                  <TypingInput
                    ref="typingInputRef"
                    :english="currentQuestion.english"
                    @correct="handleCorrect"
                    @wrong="handleWrong"
                  />
                </template>
                <AnswerView
                  v-else
                  :statement="currentQuestion"
                  @next="handleNext"
                  @retry="handleRetry"
                />
              </div>
            </Transition>
          </div>
        </main>
      </div>

      <CompletionModal
        :show="showCompletion"
        :course-title="activeCourseTitle"
        :total-count="totalCount"
        @close="showCompletion = false"
        @again="handleAgain"
        @back-to-list="goCourses"
      />
    </template>

    <div v-if="showUpload" class="modal-mask" @click.self="closeUpload">
      <div class="modal-box">
        <h3>上传课程包</h3>
        <input v-model="uploadForm.name" class="search" placeholder="课程包名称" />
        <input v-model="uploadForm.description" class="search" placeholder="简介" />
        <input v-model="uploadForm.level" class="search" placeholder="难度" />
        <label class="check">
          <input v-model="uploadForm.isPublic" type="checkbox" />
          公开课程包
        </label>
        <textarea v-model="uploadRaw" class="raw" placeholder="粘贴JSON数组：[{english,chinese,soundmark}]" />
        <div class="answer-actions">
          <button class="btn btn-outline" @click="closeUpload">取消</button>
          <button class="btn btn-primary" @click="submitUpload">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addPackageSection,
  createPackage,
  fetchCourseQuestions,
  fetchPackageCourses,
  fetchPackages,
  fetchProgress,
  recordPackageClick,
  saveProgress
} from '@/modules/word-game/api'
import TypingInput from '@/modules/word-game/components/TypingInput.vue'
import AnswerView from '@/modules/word-game/components/AnswerView.vue'
import CompletionModal from '@/modules/word-game/components/CompletionModal.vue'
import { useGameMode } from '@/modules/word-game/composables/useGameMode'
import { playEnglishSound } from '@/modules/word-game/composables/useEnglishSound'

const route = useRoute()
const router = useRouter()
const { isQuestion, showAnswer, showQuestion, resetMode } = useGameMode()

const viewLoading = ref(false)
const errorMessage = ref('')
const packages = ref([])
const courses = ref([])
const questions = ref([])
const progressMap = ref({})
const search = ref('')
const searchTimer = ref(null)
const questionIndex = ref(0)
const showUpload = ref(false)
const uploadRaw = ref('')
const gameStarted = ref(false)
const showCompletion = ref(false)
const typingInputRef = ref(null)
const uploadForm = ref({
  name: '',
  description: '',
  level: '自定义',
  isPublic: false
})

const stage = computed(() => {
  if (route.params.packageId && route.params.courseIndex) return 'game'
  if (route.params.packageId) return 'courses'
  return 'packages'
})

const activePackageId = computed(() => String(route.params.packageId || ''))
const activeCourseIndex = computed(() => Number(route.params.courseIndex || 1))
const activePackage = computed(() => packages.value.find(p => p.id === activePackageId.value) || null)
const activeCourseTitle = computed(() => {
  const current = courses.value.find(c => c.index === activeCourseIndex.value)
  return current?.title || `第${activeCourseIndex.value}课`
})
const currentQuestion = computed(() => questions.value[questionIndex.value] || null)
const totalCount = computed(() => questions.value.length)
const visibleIndex = computed(() => questionIndex.value + 1)
const progressPercent = computed(() => {
  if (!totalCount.value) return '0%'
  return `${Math.round((visibleIndex.value / totalCount.value) * 100)}%`
})

async function loadPackagesData() {
  viewLoading.value = true
  errorMessage.value = ''
  try {
    packages.value = await fetchPackages(search.value.trim())
  } catch (e) {
    errorMessage.value = e.message || '课程包加载失败'
  } finally {
    viewLoading.value = false
  }
}

async function loadCoursesData() {
  viewLoading.value = true
  errorMessage.value = ''
  try {
    if (packages.value.length === 0) {
      packages.value = await fetchPackages('')
    }
    courses.value = await fetchPackageCourses(activePackageId.value)
    progressMap.value = await fetchProgress(activePackageId.value)
  } catch (e) {
    errorMessage.value = e.message || '课程加载失败'
  } finally {
    viewLoading.value = false
  }
}

async function loadGameData() {
  viewLoading.value = true
  errorMessage.value = ''
  showCompletion.value = false
  gameStarted.value = false
  try {
    if (courses.value.length === 0) {
      courses.value = await fetchPackageCourses(activePackageId.value)
    }
    questions.value = await fetchCourseQuestions(activeCourseIndex.value, activePackageId.value)
    progressMap.value = await fetchProgress(activePackageId.value)
    const saved = getCourseProgress(activeCourseIndex.value)
    questionIndex.value = saved?.completed ? 0 : Math.max(0, saved?.currentQuestion || 0)
    resetMode()
  } catch (e) {
    errorMessage.value = e.message || '题目加载失败'
  } finally {
    viewLoading.value = false
  }
}

async function persistProgress(completed) {
  await saveProgress({
    packageId: activePackageId.value,
    courseIndex: activeCourseIndex.value,
    currentQuestion: questionIndex.value,
    completed,
    studySeconds: 0
  })
}

function isIconImage(icon) {
  if (!icon || typeof icon !== 'string') return false
  return icon.startsWith('data:') || icon.startsWith('http://') || icon.startsWith('https://')
}

function getCourseProgress(courseIndex) {
  const direct = progressMap.value[String(courseIndex)]
  if (direct) return direct
  const legacy = progressMap.value[String(courseIndex - 1)]
  if (legacy) return legacy
  return null
}

function isCourseCompleted(courseIndex) {
  return getCourseProgress(courseIndex)?.completed === true
}

function getCourseProgressPercent(courseIndex, totalCountForCourse) {
  const p = getCourseProgress(courseIndex)
  if (!p || !totalCountForCourse) return 0
  if (p.completed) return 100
  return Math.min(100, Math.max(0, Math.round(((p.currentQuestion || 0) / totalCountForCourse) * 100)))
}

const completedCoursesCount = computed(() => {
  return courses.value.filter(course => isCourseCompleted(course.index)).length
})

function handleSearch() {
  if (searchTimer.value) clearTimeout(searchTimer.value)
  searchTimer.value = setTimeout(() => {
    loadPackagesData()
    searchTimer.value = null
  }, 300)
}

async function openPackage(packageId) {
  await recordPackageClick(packageId).catch(() => {})
  router.push(`/word-game/package/${encodeURIComponent(packageId)}`)
}

function openGame(courseIndex) {
  router.push(`/word-game/game/${encodeURIComponent(activePackageId.value)}/${courseIndex}`)
}

function goPackages() {
  router.push('/word-game')
}

function goCourses() {
  router.push(`/word-game/package/${encodeURIComponent(activePackageId.value)}`)
}

function startGame() {
  if (gameStarted.value) return
  gameStarted.value = true
  playEnglishSound(currentQuestion.value?.english)
}

function handleGlobalStart() {
  if (stage.value === 'game') {
    startGame()
  }
}

function handleCorrect() {
  playEnglishSound(currentQuestion.value?.english)
  showAnswer()
  persistProgress(false)
}

function handleWrong() {}

async function handleNext() {
  if (questionIndex.value >= questions.value.length - 1) {
    await persistProgress(true)
    showCompletion.value = true
    return
  }
  questionIndex.value += 1
  await persistProgress(false)
  showQuestion()
  playEnglishSound(currentQuestion.value?.english)
}

function handleRetry() {
  showQuestion()
}

async function handleAgain() {
  questionIndex.value = 0
  showCompletion.value = false
  showQuestion()
  await persistProgress(false)
  playEnglishSound(currentQuestion.value?.english)
}

function closeUpload() {
  showUpload.value = false
  uploadRaw.value = ''
}

async function submitUpload() {
  let statements = []
  try {
    statements = JSON.parse(uploadRaw.value || '[]')
  } catch {
    errorMessage.value = 'JSON格式错误'
    return
  }
  const pkg = await createPackage({
    name: uploadForm.value.name,
    description: uploadForm.value.description,
    icon: '📦',
    level: uploadForm.value.level || '自定义',
    isPublic: uploadForm.value.isPublic
  })
  await addPackageSection(pkg.id, {
    title: '第一课',
    statements
  })
  closeUpload()
  await loadPackagesData()
}

function reloadCurrentView() {
  if (stage.value === 'packages') {
    loadPackagesData()
  } else if (stage.value === 'courses') {
    loadCoursesData()
  } else {
    loadGameData()
  }
}

watch(
  () => route.fullPath,
  () => {
    if (stage.value === 'packages') {
      loadPackagesData()
    } else if (stage.value === 'courses') {
      loadCoursesData()
    } else {
      loadGameData()
    }
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('keydown', handleGlobalStart, { once: false })
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalStart)
})
</script>

<style scoped>
.word-game-view {
  padding: 20px;
  color: var(--text-primary);
  min-height: 100%;
  overflow-y: auto;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  width: 100%;
  max-width: 920px;
  margin-left: auto;
  margin-right: auto;
}

.sub-text {
  color: var(--text-tertiary);
  font-size: 0.85rem;
  margin-top: 2px;
}

.actions {
  display: flex;
  gap: 8px;
}

.search {
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  min-width: 220px;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.package-list {
  display: flex; /* Changed to flex for single column */
  flex-direction: column; /* Ensure single column layout */
  gap: 14px;
  width: 100%;
  max-width: 920px;
  margin: 0 auto;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); /* Adjusted to fit more items */
  gap: 14px;
  width: 100%;
  max-width: 920px;
  margin: 0 auto;
}

.card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--bg-secondary);
  padding: 18px 24px; /* Adjusted padding */
}

.package-item {
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: row; /* Ensure horizontal layout */
  align-items: center;
  gap: 14px;
  width: 100%;
  white-space: normal; /* Ensure text within is horizontal */
  word-break: break-word; /* Ensure text within breaks words */
}

.title-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 6px;
  flex-wrap: wrap; /* Allow title and badge to wrap if space is limited */
}

.pkg-rank {
  width: 40px; /* Increased size */
  height: 40px; /* Increased size */
  border-radius: 50%;
  background: var(--primary-color); /* Changed to primary blue */
  color: #fff; /* White text */
  font-size: 0.9rem; /* Adjusted font size */
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pkg-rank.rank-top3 {
  background: var(--primary-color); /* Keep primary blue for top 3 */
  color: #fff;
}

.pkg-icon-wrap {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  background: #fffbe6; /* Light yellow/orange background */
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.pkg-icon {
  font-size: 1.7rem;
}

.pkg-icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pkg-body {
  flex: 1;
  min-width: 0; /* Important for flex items to shrink */
  display: flex; /* Make pkg-body a flex container */
  flex-direction: column; /* Stack its content vertically */
  justify-content: center;
  white-space: normal; /* Explicitly ensure horizontal text flow */
  word-break: break-word; /* Explicitly ensure words break */
}

.pkg-body p,
.pkg-body small {
  text-align: left;
  white-space: normal; /* Ensure text wraps */
  word-break: break-word; /* Break long words */
}

.pkg-level-badge {
  padding: 2px 10px;
  border-radius: 20px;
  background: var(--chip-bg);
  color: var(--chip-color);
  font-size: 0.72rem;
  font-weight: 600;
  flex-shrink: 0; /* Prevent badge from shrinking */
}

.pkg-arrow,
.course-arrow {
  font-size: 1.2rem;
  color: var(--text-tertiary);
  flex-shrink: 0;
  margin-left: auto;
}

.courses-head {
  flex: 1;
}

.course-item {
  position: relative;
}

.course-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
}

.course-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.course-main strong {
  font-size: 1rem;
  font-weight: 600;
}

.course-main span {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.course-main small {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.course-badge {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-color);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 0.9rem;
  font-weight: 700;
}

.course-item.is-completed {
  border-color: rgba(34, 197, 94, 0.5) !important;
}

.course-item.in-progress {
  border-color: rgba(59, 130, 246, 0.4) !important;
}

.course-progress-bar {
  height: 3px;
  border-radius: 2px;
  background: var(--border-color);
  overflow: hidden;
  margin-top: 8px; /* Adjusted margin-top */
  width: 100%;
}

.course-progress-fill {
  height: 100%;
  border-radius: 2px;
  background: var(--primary-color);
  transition: width 0.3s ease;
}

.scroll-tip {
  margin-top: 10px;
  font-size: 0.8rem;
  color: var(--text-tertiary);
  text-align: center;
  width: 100%;
  max-width: 920px;
  margin-left: auto;
  margin-right: auto;
}

.game-page {
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 14px;
}

.game-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.header-inner {
  max-width: 980px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  font-size: 0.875rem;
  flex-shrink: 0;
}

.course-info {
  flex: 1;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.course-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.progress-text {
  font-size: 0.85rem;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 2px 10px;
  border-radius: 20px;
}

.hint-btn {
  font-size: 0.8rem;
  padding: 6px 14px;
  flex-shrink: 0;
}

.hint-placeholder {
  width: 64px;
  flex-shrink: 0;
}

.progress-bar {
  height: 3px;
  background: var(--bg-tertiary);
}

.progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  transition: width 0.4s ease;
}

.game-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
}

.game-content {
  width: 100%;
  max-width: 980px;
  padding: 40px 24px;
  position: relative;
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ready-screen {
  position: absolute;
  text-align: center;
  cursor: pointer;
  padding: 24px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ready-hint {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.ready-sub {
  font-size: 1rem;
  color: var(--text-tertiary);
}

.playing-area {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.chinese-hint {
  font-size: 1.5rem;
  font-weight: 500;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.4;
}

.soundmark {
  font-size: 0.95rem;
  color: var(--text-tertiary);
  text-align: center;
  letter-spacing: 0.5px;
}

.center-state {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-box {
  width: min(640px, 92vw);
  background: var(--bg-secondary);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.answer-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.raw {
  min-height: 140px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.check {
  display: flex;
  gap: 8px;
  align-items: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .word-game-view {
    padding: 12px;
  }

  .actions {
    width: 100%;
  }

  .search {
    min-width: 0;
    flex: 1;
  }

  .header-inner {
    padding: 10px 14px;
    gap: 8px;
  }

  .game-content {
    padding: 22px 14px;
  }

  .chinese-hint {
    font-size: 1.2rem;
  }
}
</style>
