<template>
  <div class="language-learning-container">
    <div class="language-learning-page">
      <!-- Main Content Area -->
      <div class="main-content">
        <!-- Dashboard View -->
        <div
          v-if="currentView === 'dashboard'"
          class="view-section dashboard-view"
        >
          <div class="view-header">
            <h2>学习概览</h2>
            <p>查看你的学习进度和今日任务</p>
          </div>

          <div class="dashboard-grid">
            <div class="stat-card primary">
              <div class="stat-icon">
                📚
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ learningStats?.totalWords ?? 0 }}
                </div>
                <div class="stat-label">
                  已学习单词
                </div>
              </div>
            </div>
            <div class="stat-card success">
              <div class="stat-icon">
                ✅
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ learningStats?.masteredWords ?? 0 }}
                </div>
                <div class="stat-label">
                  已掌握
                </div>
              </div>
            </div>
            <div class="stat-card warning">
              <div class="stat-icon">
                ⏱️
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ formatDuration(learningStats?.todayDuration ?? 0) }}
                </div>
                <div class="stat-label">
                  今日时长
                </div>
              </div>
            </div>
            <div class="stat-card info">
              <div class="stat-icon">
                📈
              </div>
              <div class="stat-content">
                <div class="stat-value">
                  {{ formatDuration(learningStats?.totalDuration ?? 0) }}
                </div>
                <div class="stat-label">
                  总时长
                </div>
              </div>
            </div>
          </div>

          <div class="review-section card mt-4">
            <div class="card-header">
              <h3>今日复习</h3>
              <button
                class="btn btn-text"
                @click="refreshReview"
              >
                刷新
              </button>
            </div>
            <div
              v-if="reviewItems.length === 0"
              class="empty-state"
            >
              <div class="illustration">
                🎉
              </div>
              <p>太棒了！今日复习任务已完成</p>
            </div>
            <div
              v-else
              class="review-grid"
            >
              <div
                v-for="item in reviewItems"
                :key="item.id"
                class="review-card-item"
              >
                <div class="review-content">
                  <h4 class="review-word">
                    {{ item.word?.word || item.wordId }}
                  </h4>
                  <p class="review-def">
                    {{ item.word?.definition }}
                  </p>
                </div>
                <div class="review-actions">
                  <button
                    class="btn btn-sm btn-outline"
                    @click="quickReview(getReviewWordId(item), Math.min((item.masteryLevel ?? 0) + 1, 5))"
                  >
                    认识
                  </button>
                  <button
                    class="btn btn-sm btn-primary"
                    @click="quickReview(getReviewWordId(item), 5)"
                  >
                    掌握
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- My Words View -->
        <div
          v-if="currentView === 'my-words'"
          class="view-section my-words-view"
        >
          <div class="two-column-layout">
            <div class="list-column card">
              <div class="card-header">
                <h3>单词表</h3>
                <button
                  class="btn btn-sm btn-primary"
                  @click="showCreateList = true"
                >
                  + 新建
                </button>
              </div>
              <div class="list-container">
                <div
                  v-for="list in vocabularyLists"
                  :key="list.id"
                  class="list-item"
                  :class="{ active: currentListId === list.id }"
                  @click="selectList(list.id)"
                >
                  <div class="list-info">
                    <h4>{{ list.name }}</h4>
                    <span class="badge">{{ (list.language || 'en').toUpperCase() }}</span>
                    <span class="count">{{ list.wordCount || 0 }} 词</span>
                  </div>
                  <button
                    class="btn-icon delete-btn"
                    title="删除"
                    @click.stop="removeList(list.id)"
                  >
                    ×
                  </button>
                </div>
              </div>
            </div>

            <div class="words-column card">
              <div class="card-header">
                <h3>{{ currentList ? currentList.name : '单词列表' }}</h3>
                <div class="actions">
                  <button
                    v-if="currentListId"
                    class="btn btn-sm btn-outline"
                    @click="refreshCurrentList"
                  >
                    刷新
                  </button>
                  <button
                    v-if="currentListId"
                    class="btn btn-sm btn-primary"
                    @click="showAddWord = true"
                  >
                    + 添加单词
                  </button>
                </div>
              </div>

              <div
                v-if="!currentListId"
                class="empty-state"
              >
                <p>请选择一个单词表查看详情</p>
              </div>
              <div
                v-else-if="currentWords.length === 0"
                class="empty-state"
              >
                <p>暂无单词，点击上方按钮添加</p>
              </div>
              <div
                v-else
                class="words-grid"
              >
                <div
                  v-for="word in currentWords"
                  :key="word.id"
                  class="word-card-item"
                >
                  <div class="word-header">
                    <h4>{{ word.word }}</h4>
                    <span class="pos">{{ word.partOfSpeech }}</span>
                  </div>
                  <div class="word-body">
                    <p class="definition">
                      {{ word.definition }}
                    </p>
                    <p
                      v-if="word.example"
                      class="example"
                    >
                      {{ word.example }}
                    </p>
                  </div>
                  <div class="word-footer">
                    <span :class="['status-tag', getMasteryClass(getWordProgress(word.id).masteryLevel)]">
                      {{ getMasteryText(getWordProgress(word.id).masteryLevel) }}
                    </span>
                    <div class="controls">
                      <select
                        class="select-sm"
                        :value="getWordProgress(word.id).masteryLevel ?? 0"
                        @change="changeMastery(word.id, $event.target.value)"
                      >
                        <option
                          v-for="i in 6"
                          :key="i"
                          :value="i-1"
                        >
                          {{ i-1 }}
                        </option>
                      </select>
                      <button
                        class="btn-icon delete-btn"
                        @click="removeWord(word.id)"
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Public Library View -->
        <div
          v-if="currentView === 'public-library'"
          class="view-section public-library-view"
        >
          <div class="view-header">
            <h2>公共词库</h2>
            <p>搜索并添加单词到你的个人词库</p>
          </div>

          <div class="search-bar-container">
            <div class="search-input-wrapper">
              <input
                v-model="publicKeyword"
                type="text"
                class="search-input"
                placeholder="搜索单词、释义..."
                @keyup.enter="() => { currentPage = 1; searchPublic(); }"
              >
              <button
                class="btn btn-primary search-btn"
                @click="() => { currentPage = 1; searchPublic(); }"
              >
                搜索
              </button>
            </div>
            <div class="wordlist-selection">
              <label for="wordlist-select">添加到：</label>
              <select
                id="wordlist-select"
                v-model="currentListId"
                class="select-input"
                @change="onListChange"
              >
                <option
                  :value="null"
                  disabled
                >
                  请选择单词表
                </option>
                <option
                  value="public"
                  disabled
                  style="display:none;"
                />
                <option
                  v-for="list in vocabularyLists"
                  :key="list.id"
                  :value="list.id"
                >
                  {{ list.name }} ({{ list.wordCount || 0 }}词)
                </option>
              </select>
            </div>
          </div>

          <div style="margin-bottom: 16px; color: var(--text-secondary); font-size: 14px;">
            共找到 {{ vocabularyStore.publicSearchTotal }} 个单词
          </div>

          <div class="public-results-grid">
            <div
              v-if="isLoading"
              class="loading-state"
            >
              <div class="spinner" />
              <p>正在获取词汇...</p>
            </div>
            <div
              v-else-if="publicResults.length === 0"
              class="empty-state"
            >
              <p>未找到匹配的词汇，尝试更换关键词</p>
            </div>
            <div
              v-else
              class="public-results-container"
            >
              <div class="public-results-scroll-container">
                <div
                  class="review-grid"
                >
                  <div
                    v-for="w in publicResults"
                    :key="w.id"
                    class="review-card-item"
                  >
                    <div class="review-content">
                      <h4 class="review-word">
                        {{ w.word }}
                        <span
                          v-if="w.partOfSpeech"
                          class="public-pos"
                        >
                          {{ w.partOfSpeech }}
                        </span>
                      </h4>
                      <p class="review-def">
                        {{ w.definition }}
                      </p>
                    </div>
                    <div class="review-actions">
                      <button
                        class="btn btn-sm btn-primary"
                        :disabled="!currentListId"
                        @click="addPublicWord(w)"
                      >
                        添加
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div
                v-if="totalPages > 1"
                class="pagination-container"
              >
                <div class="pagination-info">
                  显示第 {{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, vocabularyStore.publicSearchTotal) }} 条，共 {{ vocabularyStore.publicSearchTotal }} 条
                </div>
                <div class="pagination-buttons">
                  <button
                    class="btn btn-sm btn-outline"
                    :disabled="currentPage <= 1"
                    @click="goToPage(currentPage - 1)"
                  >
                    上一页
                  </button>
                  <span class="pagination-current">第 {{ currentPage }} / {{ totalPages }} 页</span>
                  <button
                    class="btn btn-sm btn-outline"
                    :disabled="currentPage >= totalPages"
                    @click="goToPage(currentPage + 1)"
                  >
                    下一页
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Articles View -->
        <div
          v-if="currentView === 'ai-articles'"
          class="view-section ai-articles-view"
        >
          <div class="view-header ai-articles-header">
            <div class="header-info">
              <h2>AI 生成学习文章</h2>
              <p>基于你的词汇表生成个性化阅读材料</p>
            </div>
            <div class="ai-article-tabs">
              <button
                class="ai-tab-btn"
                :class="{ active: aiArticleTab === 'generate' }"
                @click="aiArticleTab = 'generate'"
              >
                ✨ 生成文章
              </button>
              <button
                class="ai-tab-btn"
                :class="{ active: aiArticleTab === 'mine' }"
                @click="aiArticleTab = 'mine'; loadMyArticles()"
              >
                我的文章
              </button>
            </div>
          </div>

          <div class="article-generator card">
            <div
              v-if="aiArticleTab === 'generate'"
              class="ai-generate-panel"
            >
              <div class="ai-generate-grid">
                <div class="ai-card ai-config-card">
                  <div class="ai-card-header">
                    <div class="ai-card-title">
                      生成配置
                    </div>
                    <div class="ai-card-subtitle">
                      选择词表、难度与篇幅，可用 AI 推荐主题
                    </div>
                  </div>
                  <div class="ai-card-body">
                    <div class="ai-form-grid">
                      <!-- 单词表来源 -->
                      <div class="form-group full-width">
                        <label>📚 单词表来源</label>
                        <select
                          v-model="currentListId"
                          class="select-input"
                          @change="onListChange"
                        >
                          <option
                            :value="null"
                            disabled
                          >
                            请选择单词表
                          </option>
                          <option value="public">
                            公共词库 ({{ vocabularyStore.publicSearchTotal || 0 }}词)
                          </option>
                          <option
                            v-for="list in vocabularyLists"
                            :key="list.id"
                            :value="list.id"
                          >
                            {{ list.name }} ({{ list.wordCount || 0 }}词)
                          </option>
                        </select>
                      </div>

                      <!-- 文章长度 -->
                      <div class="form-group">
                        <label>📏 文章长度</label>
                        <select
                          v-model="articleOptions.length"
                          class="select-input"
                        >
                          <option value="Short">
                            短篇（约200词）
                          </option>
                          <option value="Medium">
                            中篇（约400词）
                          </option>
                          <option value="Long">
                            长篇（约700词）
                          </option>
                        </select>
                      </div>

                      <!-- 难度级别 -->
                      <div class="form-group">
                        <label>🎓 难度级别</label>
                        <select
                          v-model="articleOptions.difficulty"
                          class="select-input"
                        >
                          <option value="基础英语">
                            基础英语
                          </option>
                          <option value="四级">
                            四级
                          </option>
                          <option value="六级">
                            六级
                          </option>
                          <option value="托福">
                            托福
                          </option>
                          <option value="雅思">
                            雅思
                          </option>
                          <option value="商务英语">
                            商务英语
                          </option>
                          <option value="学术英语">
                            学术英语
                          </option>
                        </select>
                      </div>

                      <!-- 文章主题 -->
                      <div class="form-group full-width">
                        <label>📝 文章主题</label>
                        <div class="ai-topic-input-group">
                          <input
                            v-model="articleOptions.topic"
                            type="text"
                            class="input"
                            placeholder="输入文章主题（可选，留空则自动生成）"
                          >
                          <button
                            class="btn btn-secondary btn-icon-text"
                            :disabled="!currentListId || selectedWordIds.size === 0 || isGeneratingTopics"
                            @click="generateTopics"
                          >
                            <span
                              v-if="isGeneratingTopics"
                              class="spinner-small"
                            />
                            <span v-else>✨</span>
                            AI 推荐主题
                          </button>
                        </div>
                        <div
                          v-if="generatedTopics.length"
                          class="ai-topic-suggestions"
                        >
                          <div class="ai-topic-suggestions-label">
                            AI 推荐主题：
                          </div>
                          <div class="ai-topic-suggestions-list">
                            <button
                              v-for="topic in generatedTopics"
                              :key="topic"
                              class="ai-topic-chip"
                              type="button"
                              @click="selectTopic(topic)"
                            >
                              {{ topic }}
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="ai-card ai-words-card">
                  <div class="ai-card-header">
                    <div class="ai-card-title">
                      选词与生成
                    </div>
                    <div class="ai-card-subtitle">
                      <span v-if="currentListId === 'public'">已加载公共词库，共 {{ vocabularyStore.publicSearchTotal || 0 }} 个单词，当前显示第 {{ currentPage }} 页</span>
                      <span v-else-if="currentListId">已加载 {{ currentWords.length }} 个单词</span>
                      <span v-else>选择词表后可勾选单词生成文章</span>
                    </div>
                  </div>
                  <div class="ai-card-body">
                    <div
                      v-if="currentListId"
                      class="ai-word-selection-section"
                    >
                      <div class="ai-toolbar">
                        <div class="ai-toolbar-top">
                          <div class="ai-search-wrapper">
                            <span class="search-icon">🔍</span>
                            <input
                              v-model="searchQuery"
                              type="text"
                              class="ai-search-input"
                              placeholder="在当前列表中搜索单词、释义或词性..."
                            >
                          </div>
                        </div>
                        <div class="ai-toolbar-bottom">
                          <div class="ai-toolbar-left">
                            <label class="checkbox-label">
                              <input
                                type="checkbox"
                                :checked="isAllSelected"
                                @change="toggleSelectAll"
                              >
                              全选
                              <span class="ai-toolbar-meta">已选 {{ selectedWordIds.size }} / 20</span>
                            </label>
                          </div>
                          <div class="ai-toolbar-actions">
                            <div
                              v-if="currentListId === 'public'"
                              class="pagination-mini"
                            >
                              <button
                                class="btn btn-outline btn-sm"
                                :disabled="currentPage <= 1"
                                @click="changePublicPage(currentPage - 1)"
                              >
                                上一页
                              </button>
                              <span class="page-info">第 {{ currentPage }} 页</span>
                              <button
                                class="btn btn-outline btn-sm"
                                :disabled="publicResults.length < 50"
                                @click="changePublicPage(currentPage + 1)"
                              >
                                下一页
                              </button>
                            </div>
                            <button
                              class="btn btn-outline btn-sm"
                              :disabled="selectedWordIds.size === 0"
                              @click="showSelectedWords = true"
                            >
                              📘 查看已选
                            </button>
                            <button
                              class="btn btn-primary btn-sm"
                              :disabled="!canGenerateArticle"
                              @click="generateArticleNow"
                            >
                              ✨ 生成文章
                            </button>
                          </div>
                        </div>
                      </div>

                      <div class="ai-words-table-wrapper">
                        <table class="ai-words-table">
                          <thead>
                            <tr>
                              <th style="width: 50px;">
                                选择
                              </th>
                              <th>单词</th>
                              <th>释义</th>
                              <th style="width: 100px;">
                                词性
                              </th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr
                              v-for="word in filteredWords"
                              :key="word.id"
                              :class="{ 'selected': selectedWordIds.has(word.id) }"
                              @click="toggleWordSelection(word.id)"
                            >
                              <td @click.stop>
                                <input
                                  type="checkbox"
                                  :checked="selectedWordIds.has(word.id)"
                                  @change="toggleWordSelection(word.id)"
                                >
                              </td>
                              <td class="ai-word-text">
                                {{ word.word }}
                              </td>
                              <td class="ai-word-definition">
                                {{ word.definition }}
                              </td>
                              <td class="ai-word-pos">
                                <span class="pos-tag">{{ word.partOfSpeech || '-' }}</span>
                              </td>
                            </tr>
                            <tr v-if="filteredWords.length === 0">
                              <td
                                colspan="4"
                                class="empty-cell"
                              >
                                未找到匹配的单词
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </div>

                    <div
                      v-else
                      class="ai-empty-hint ai-empty-in-card"
                    >
                      <div class="empty-icon">
                        📭
                      </div>
                      <p>请先选择一个单词表开始</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>


            <div
              v-else
              class="ai-myarticles-panel"
            >
              <div class="ai-myarticles-header">
                <div class="ai-myarticles-meta">
                  共 {{ myArticles.length }} 篇
                </div>
                <button
                  class="btn btn-outline btn-sm"
                  :disabled="isLoadingArticles"
                  @click="loadMyArticles(true)"
                >
                  刷新
                </button>
              </div>

              <div
                v-if="isLoadingArticles"
                class="loading-state"
              >
                <div class="spinner" />
                <p>正在加载文章列表...</p>
              </div>

              <div v-else>
                <div
                  v-if="myArticles.length === 0"
                  class="ai-empty-hint"
                >
                  暂无文章，先去“生成文章”创建一篇吧
                </div>

                <div
                  v-else
                  class="ai-articles-table-wrapper"
                >
                  <table class="ai-articles-table">
                    <thead>
                      <tr>
                        <th>标题</th>
                        <th style="width: 120px;">
                          难度
                        </th>
                        <th style="width: 120px;">
                          长度
                        </th>
                        <th style="width: 180px;">
                          创建时间
                        </th>
                        <th style="width: 120px;">
                          操作
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="a in myArticles"
                        :key="a.id"
                      >
                        <td class="ai-article-title">
                          {{ a.topic || '未命名文章' }}
                        </td>
                        <td>{{ a.difficulty_level || a.difficultyLevel }}</td>
                        <td>{{ a.article_length || a.articleLength }}</td>
                        <td>{{ a.created_at || a.createdAt }}</td>
                        <td>
                          <button
                            class="btn btn-outline btn-sm"
                            @click="openMyArticle(a.id)"
                          >
                            查看
                          </button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Translation View -->
        <div
          v-if="currentView === 'translation'"
          class="view-section"
        >
          <div class="view-header">
            <h2>智能翻译</h2>
            <p>多语言 AI 翻译工具</p>
          </div>
          <TranslationTool />
        </div>
      </div>
    </div>

    <!-- Dialogs -->
    <div
      v-if="showSelectedWords"
      class="modal-overlay"
      @click.self="showSelectedWords = false"
    >
      <div class="modal-card modal-wide">
        <div class="modal-wide-header">
          <h3>已选择 {{ selectedWordsDetails.length }} 个单词</h3>
          <button
            class="modal-close-btn"
            @click="showSelectedWords = false"
          >
            ×
          </button>
        </div>

        <div class="ai-selected-words-table-wrapper">
          <table class="ai-words-table">
            <thead>
              <tr>
                <th>单词</th>
                <th>释义</th>
                <th style="width: 120px;">
                  词性
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="w in selectedWordsDetails"
                :key="w.id"
              >
                <td class="ai-word-text">
                  {{ w.word }}
                </td>
                <td class="ai-word-definition">
                  {{ w.definition }}
                </td>
                <td class="ai-word-pos">
                  {{ w.partOfSpeech || '-' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 非阻塞的文章生成状态通知 -->
    <div
      v-if="articleGenerationInProgress || articleGenerationComplete"
      class="article-generation-notification"
    >
      <div
        v-if="articleGenerationInProgress"
        class="generation-progress"
      >
        <div class="spinner small" />
        <span>正在生成文章...</span>
      </div>
      <div
        v-if="articleGenerationComplete"
        class="generation-complete"
      >
        <span>✓ 文章生成完成！请在"我的文章"中查看</span>
      </div>
    </div>

    <div
      v-if="showArticleModal"
      class="modal-overlay"
      @click.self="closeArticleModal"
    >
      <div class="modal-card modal-xxl">
        <div class="modal-wide-header">
          <div class="ai-article-modal-title">
            {{ activeArticle?.topic || '未命名文章' }}
          </div>
          <div class="ai-article-modal-actions">
            <div class="ai-download">
              <button
                class="btn btn-primary btn-sm"
                @click="toggleDownloadMenu"
              >
                📥 下载文章 ▾
              </button>
              <div
                v-if="showDownloadMenu"
                class="ai-download-menu"
              >
                <button
                  class="ai-download-item"
                  type="button"
                  @click="downloadArticle('html')"
                >
                  下载 HTML
                </button>
                <button
                  class="ai-download-item"
                  type="button"
                  @click="downloadArticle('txt')"
                >
                  下载 TXT
                </button>
                <button
                  class="ai-download-item"
                  type="button"
                  @click="downloadArticle('study')"
                >
                  下载（学习版）
                </button>
                <button
                  class="ai-download-item"
                  type="button"
                  @click="downloadArticle('pdf')"
                >
                  下载 PDF（学习版）
                </button>
              </div>
            </div>
            <button
              class="modal-close-btn"
              @click="closeArticleModal"
            >
              ×
            </button>
          </div>
        </div>

        <div class="ai-article-modal-meta">
          难度：{{ activeArticle?.difficulty_level || activeArticle?.difficultyLevel }}｜
          长度：{{ activeArticle?.article_length || activeArticle?.articleLength }}｜
          使用单词：{{ selectedWordsCountForActive }} 个
        </div>

        <div
          id="printable-article"
          class="ai-article-modal-content"
        >
          <div
            v-for="(p, idx) in articleParagraphPairs"
            :key="idx"
            class="ai-article-paragraph-pair"
          >
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div
              class="ai-article-en"
              v-html="p.enHtml"
            />
            <div class="ai-article-zh">
              {{ p.zhText }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <teleport to="body">
      <div
        v-if="showArticleModal && activeArticle"
        id="article-print-area"
        class="article-print-area"
      >
        <div class="article-print-title">
          {{ activeArticle?.topic || '未命名文章' }}
        </div>

        <div class="article-print-content">
          <div
            v-for="(p, idx) in printParagraphPairs"
            :key="idx"
            class="article-print-paragraph"
          >
            <div class="article-print-en">
              {{ p.enText }}
            </div>
            <div
              v-if="p.zhText"
              class="article-print-zh"
            >
              {{ p.zhText }}
            </div>
          </div>
        </div>

        <div
          v-if="printUsedWords.length > 0"
          class="article-print-words"
        >
          <div class="article-print-words-title">
            单词列表
          </div>
          <table class="article-print-words-table">
            <thead>
              <tr>
                <th style="width: 25%;">
                  单词
                </th>
                <th style="width: 15%;">
                  词性
                </th>
                <th>释义</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(w, idx) in printUsedWords"
                :key="idx"
              >
                <td>{{ w.word }}</td>
                <td>{{ w.partOfSpeech || '-' }}</td>
                <td>{{ w.definition || '' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </teleport>

    <div
      v-if="showCreateList"
      class="modal-overlay"
      @click.self="showCreateList = false"
    >
      <div class="modal-card">
        <h3>创建单词表</h3>
        <input
          v-model="newList.name"
          type="text"
          class="input"
          placeholder="输入单词表名称"
          @keyup.enter="createList"
        >
        <textarea
          v-model="newList.description"
          class="input textarea"
          placeholder="描述（可选）"
        />
        <select
          v-model="newList.language"
          class="input"
        >
          <option value="en">
            英语
          </option>
          <option value="ja">
            日语
          </option>
          <option value="ko">
            韩语
          </option>
          <option value="fr">
            法语
          </option>
          <option value="de">
            德语
          </option>
          <option value="es">
            西班牙语
          </option>
          <option value="zh">
            中文
          </option>
        </select>
        <div class="modal-actions">
          <button
            class="btn btn-secondary"
            @click="showCreateList = false"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="createList"
          >
            创建
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="showAddWord"
      class="modal-overlay"
      @click.self="showAddWord = false"
    >
      <div class="modal-card">
        <h3>添加单词</h3>
        <div class="form-group">
          <label>单词</label>
          <input
            v-model="newWord.word"
            type="text"
            class="input"
            placeholder="单词"
          >
        </div>
        <div class="form-group">
          <label>释义</label>
          <input
            v-model="newWord.definition"
            type="text"
            class="input"
            placeholder="释义"
          >
        </div>
        <div class="form-group">
          <label>词性</label>
          <input
            v-model="newWord.partOfSpeech"
            type="text"
            class="input"
            placeholder="例如: n., v., adj."
          >
        </div>
        <div class="form-group">
          <label>例句</label>
          <textarea
            v-model="newWord.example"
            class="input textarea"
            placeholder="例句（可选）"
          />
        </div>
        <div class="modal-actions">
          <button
            class="btn btn-secondary"
            @click="showAddWord = false"
          >
            取消
          </button>
          <button
            class="btn btn-primary"
            @click="addWord"
          >
            添加
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, reactive, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'
import { useVocabularyStore } from '@/stores/vocabulary'
import { useUIStore } from '@/stores/ui'
import TranslationTool from '@/components/TranslationTool.vue'

const route = useRoute()
const currentView = computed(() => route.query.view || 'dashboard') // dashboard, my-words, public-library, ai-articles, translation

const currentListId = ref(null)
const showCreateList = ref(false)
const showAddWord = ref(false)
const newList = ref({
  name: '',
  description: '',
  language: 'en'
})
const newWord = ref({
  word: '',
  definition: '',
  partOfSpeech: '',
  example: ''
})
const publicKeyword = ref('')
const searchQuery = ref('')
const isGeneratingTopics = ref(false)
const isGeneratingArticle = ref(false)
const articleGenerationInProgress = ref(false)
const articleGenerationComplete = ref(false)
const currentPage = ref(1)
const pageSize = ref(50)

const totalPages = computed(() => {
  return Math.ceil(vocabularyStore.publicSearchTotal / pageSize.value)
})

const filteredWords = computed(() => {
  const sourceList = currentListId.value === 'public' ? publicResults.value : currentWords.value
  if (!searchQuery.value.trim()) return sourceList
  const query = searchQuery.value.toLowerCase().trim()
  return sourceList.filter(w =>
    (w.word || '').toLowerCase().includes(query) ||
    (w.definition || '').toLowerCase().includes(query) ||
    (w.partOfSpeech || '').toLowerCase().includes(query)
  )
})

const goToPage = async (page) => {
  if (page < 1 || page > totalPages.value) return
  markActive()
  currentPage.value = page
  const kw = publicKeyword.value.trim()
  const language = currentList.value?.language || 'en'
  await vocabularyStore.searchPublic(kw, language, page, pageSize.value)
}

// AI Article State
const aiArticleTab = ref('generate') // generate | mine
const selectedWordIds = reactive(new Set())
const articleOptions = reactive({
  difficulty: '商务英语',
  length: 'Medium',
  topic: ''
})
const generatedTopics = ref([])
const showSelectedWords = ref(false)
const showArticleModal = ref(false)
const showDownloadMenu = ref(false)
const activeArticle = ref(null)
const myArticles = ref([])
const isLoadingArticles = ref(false)

const vocabularyStore = useVocabularyStore()
const uiStore = useUIStore()
const vocabularyLists = computed(() => vocabularyStore.lists)
const currentList = computed(() => vocabularyStore.lists.find(l => l.id === currentListId.value) || null)
const currentWords = computed(() => vocabularyStore.wordsByListId[currentListId.value] || [])
const reviewItems = computed(() => vocabularyStore.reviewWords)
const learningStats = computed(() => vocabularyStore.stats)
const publicResults = computed(() => vocabularyStore.publicSearchResults)
const isLoading = computed(() => vocabularyStore.isLoading)

const isAllSelected = computed(() => {
  const list = filteredWords.value
  return list.length > 0 && list.every(w => selectedWordIds.has(w.id))
})

const selectedWordsDetails = computed(() => {
  const sourceList = currentListId.value === 'public' ? publicResults.value : currentWords.value
  return sourceList.filter(w => selectedWordIds.has(w.id))
})

const canGenerateArticle = computed(() => {
  return !!currentListId.value && selectedWordIds.size > 0 && !isGeneratingArticle.value
})

const selectedWordsCountForActive = computed(() => {
  if (!activeArticle.value) return selectedWordIds.size
  const usedWords = activeArticle.value.used_words || activeArticle.value.usedWords
  if (Array.isArray(usedWords) && usedWords.length > 0) return usedWords.length
  const used = activeArticle.value.used_word_ids || activeArticle.value.usedWordIds
  if (!used) return selectedWordIds.size
  try {
    const ids = Array.isArray(used) ? used : JSON.parse(used)
    return Array.isArray(ids) ? ids.length : selectedWordIds.size
  } catch {
    return selectedWordIds.size
  }
})

const articleParagraphPairs = computed(() => {
  const a = activeArticle.value
  if (!a) return []
  const original = a.original_text || a.originalText || ''
  const translated = a.translated_text || a.translatedText || ''
  const enParts = splitParagraphs(original)
  const zhParts = splitParagraphs(translated)
  const metaMap = buildSelectedWordMetaMap()
  const max = Math.max(enParts.length, zhParts.length)
  const pairs = []
  for (let i = 0; i < max; i += 1) {
    const en = enParts[i] || ''
    const zh = zhParts[i] || ''
    pairs.push({
      enHtml: renderHighlightedParagraph(en, metaMap),
      zhText: zh
    })
  }
  return pairs
})

/**
 * 打印专用：构建纯文本段落对（避免高亮 HTML 影响打印布局）
 */
const printParagraphPairs = computed(() => {
  const a = activeArticle.value
  if (!a) return []
  const original = a.original_text || a.originalText || ''
  const translated = a.translated_text || a.translatedText || ''
  const enParts = splitParagraphs(original)
  const zhParts = splitParagraphs(translated)
  const max = Math.max(enParts.length, zhParts.length)
  const pairs = []
  for (let i = 0; i < max; i += 1) {
    pairs.push({
      enText: enParts[i] || '',
      zhText: zhParts[i] || ''
    })
  }
  return pairs
})

/**
 * 打印专用：从文章详情中提取使用单词列表（不足时回退到当前选择）
 */
const printUsedWords = computed(() => {
  const a = activeArticle.value
  const usedWords = a?.used_words || a?.usedWords
  if (Array.isArray(usedWords) && usedWords.length > 0) {
    return usedWords
      .map((uw) => {
        const w = uw?.word || null
        const wordText = String(w?.word || uw?.word_text || uw?.wordText || '').trim()
        const definition = String(w?.definition || w?.meaning || w?.translation || '').trim()
        const partOfSpeech = String(w?.part_of_speech || w?.partOfSpeech || '').trim()
        const occurrenceCount = Number(uw?.occurrence_count ?? uw?.occurrenceCount ?? 0) || 0
        return {
          word: wordText || String(uw?.word_text || uw?.wordText || '').trim(),
          definition,
          partOfSpeech,
          occurrenceCount
        }
      })
      .filter(item => String(item.word || '').trim())
  }
  return selectedWordsDetails.value.map(w => ({
    word: w.word,
    definition: w.definition,
    partOfSpeech: w.partOfSpeech,
    occurrenceCount: 0
  }))
})

const lastActiveAt = ref(Date.now())
const lastTickAt = ref(Date.now())
const pendingDuration = ref(0)
const isFlushingDuration = ref(false)
let durationTimer = null

const markActive = () => {
  lastActiveAt.value = Date.now()
}

const flushDuration = async () => {
  if (isFlushingDuration.value) return
  const duration = pendingDuration.value
  if (duration <= 0) return

  pendingDuration.value = 0
  isFlushingDuration.value = true
  try {
    await vocabularyStore.recordActivity({
      activityType: 'vocabulary_learning',
      activityDetails: JSON.stringify({ view: currentView.value, listId: currentListId.value }),
      duration
    })
    await vocabularyStore.fetchStats()
  } catch {
    pendingDuration.value += duration
  } finally {
    isFlushingDuration.value = false
  }
}

const tickDuration = () => {
  const now = Date.now()
  const deltaSeconds = Math.floor((now - lastTickAt.value) / 1000)
  if (deltaSeconds <= 0) return
  lastTickAt.value = now

  if (document.hidden) return
  if (now - lastActiveAt.value > 60_000) return

  pendingDuration.value += deltaSeconds
  if (pendingDuration.value >= 60) {
    void flushDuration()
  }
}

const onVisibilityChange = () => {
  if (document.hidden) {
    void flushDuration()
    return
  }
  markActive()
  lastTickAt.value = Date.now()
}

const setupEventListeners = () => {
  window.addEventListener('mousemove', markActive, { passive: true })
  window.addEventListener('keydown', markActive)
  window.addEventListener('scroll', markActive, { passive: true })
  window.addEventListener('click', markActive, { passive: true })
  window.addEventListener('focus', markActive)
  window.addEventListener('blur', flushDuration)
  document.addEventListener('visibilitychange', onVisibilityChange)

  if (!durationTimer) {
    lastTickAt.value = Date.now()
    durationTimer = window.setInterval(tickDuration, 1000)
  }
}

const removeEventListeners = () => {
  if (durationTimer) {
    window.clearInterval(durationTimer)
    durationTimer = null
  }
  window.removeEventListener('mousemove', markActive)
  window.removeEventListener('keydown', markActive)
  window.removeEventListener('scroll', markActive)
  window.removeEventListener('click', markActive)
  window.removeEventListener('focus', markActive)
  window.removeEventListener('blur', flushDuration)
  document.removeEventListener('visibilitychange', onVisibilityChange)
}

onMounted(async () => {
  await vocabularyStore.fetchLists()
  await Promise.all([
    vocabularyStore.fetchStats(),
    vocabularyStore.fetchReviewWords()
  ])

  if (currentView.value === 'public-library') {
    await searchPublic()
  }

  markActive()
  setupEventListeners()
})

onActivated(() => {
  markActive()
  setupEventListeners()
})

onDeactivated(() => {
  removeEventListeners()
  void flushDuration()
})

onUnmounted(() => {
  removeEventListeners()
  void flushDuration()
})

watch(currentView, async (newView) => {
  markActive()
  if (newView === 'public-library' && publicResults.value.length === 0) {
    await searchPublic()
  }
  if (newView === 'ai-articles') {
    await initAiArticles()
  }
})

watch(currentListId, () => {
  markActive()
})

const selectList = async (listId) => {
  markActive()
  currentListId.value = listId
  if (!vocabularyStore.wordsByListId[listId]) {
    await vocabularyStore.fetchWords(listId)
  }
  await vocabularyStore.fetchListProgress(listId)
}

/**
 * 初始化AI文章页默认数据
 */
const initAiArticles = async () => {
  markActive()
  if (!currentListId.value && vocabularyLists.value.length > 0) {
    currentListId.value = vocabularyLists.value[0].id
  }
  if (currentListId.value && !vocabularyStore.wordsByListId[currentListId.value]) {
    await vocabularyStore.fetchWords(currentListId.value)
  }
}

/**
 * 将文本按空行拆分成段落
 */
const splitParagraphs = (text) => {
  if (!text) return []
  return String(text)
    .replace(/\r\n/g, '\n')
    .split(/\n\s*\n+/)
    .map(s => s.trim())
    .filter(Boolean)
}

/**
 * HTML转义（用于安全渲染）
 */
const escapeHtml = (value) => {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

/**
 * 构建已选单词的元信息映射，用于高亮时的title提示
 */
const buildSelectedWordMetaMap = () => {
  const map = new Map()
  const usedWords = activeArticle.value?.used_words || activeArticle.value?.usedWords
  if (Array.isArray(usedWords) && usedWords.length > 0) {
    for (const uw of usedWords) {
      const w = uw?.word || null
      const wordText = uw?.word_text || uw?.wordText || w?.word || ''
      if (!wordText) continue
      map.set(String(wordText).toLowerCase(), {
        word: wordText,
        definition: w?.definition || '',
        partOfSpeech: w?.part_of_speech || w?.partOfSpeech || ''
      })
    }
    return map
  }

  for (const w of selectedWordsDetails.value) {
    if (!w?.word) continue
    map.set(String(w.word).toLowerCase(), w)
  }
  return map
}

/**
 * 将带 **word** 标记的英文段落渲染为高亮HTML，同时支持代码块
 */
const renderHighlightedParagraph = (paragraph, metaMap) => {
  if (!paragraph) return ''

  // 处理代码块
  if (paragraph.startsWith('```') && paragraph.endsWith('```')) {
    const codeContent = paragraph.slice(3, -3).trim()
    return `
      <pre class="code-block" style="position: relative">
        <code>${escapeHtml(codeContent)}</code>
        <button class="copy-button" onclick="window.copyCodeBlock(this)">
          <i class="far fa-copy"></i><span>复制</span>
        </button>
      </pre>
    `
  }

  // 处理普通段落
  const escaped = escapeHtml(paragraph)
  const html = escaped.replace(/\*\*(.+?)\*\*/g, (_, rawWord) => {
    const raw = String(rawWord || '').trim()
    if (!raw) return ''
    const normalized = raw.replace(/^[^a-zA-Z]+|[^a-zA-Z]+$/g, '')
    const displayWord = normalized || raw
    const key = displayWord.toLowerCase()
    const meta = metaMap?.get(key)
    const tip = meta
      ? `${meta.word}${meta.partOfSpeech ? ` (${meta.partOfSpeech})` : ''}：${meta.definition || ''}`
      : displayWord
    return `<span class="vocab-chip" title="${escapeHtml(tip)}">${escapeHtml(displayWord)}</span>`
  })
  return `<p>${html}</p>`
}

/**
 * 复制代码块内容到剪贴板
 */
window.copyCodeBlock = (element) => {
  // 查找最近的pre标签中的代码元素
  const preElement = element.closest('.code-block');
  if (!preElement) {
    console.error('找不到代码块容器');
    return;
  }

  const codeElement = preElement.querySelector('code');
  if (!codeElement) {
    console.error('找不到代码元素');
    return;
  }

  const code = codeElement.textContent;
  const button = element;
  const icon = button.querySelector('i');
  const text = button.querySelector('span');

  navigator.clipboard.writeText(code)
    .then(() => {
      const originalIconClass = icon ? icon.className : '';
      const originalText = text ? text.textContent : button.textContent;

      if (icon) icon.className = 'fas fa-check';
      if (text) text.textContent = '已复制';
      else if (!icon) button.textContent = '已复制';

      button.classList.add('copied');

      setTimeout(() => {
        if (icon) icon.className = originalIconClass;
        if (text) text.textContent = originalText;
        else if (!icon) button.textContent = originalText;
        button.classList.remove('copied');
      }, 2000);
    })
    .catch(err => console.error('复制失败:', err));
}

/**
 * 触发浏览器下载
 */
const downloadBlob = (filename, content, mime) => {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * 构建文章导出HTML（中英对照 + 高亮词）
 */
const buildHtmlForArticle = (article) => {
  const title = article?.topic || '未命名文章'
  const original = article?.original_text || article?.originalText || ''
  const translated = article?.translated_text || article?.translatedText || ''
  const enParts = splitParagraphs(original)
  const zhParts = splitParagraphs(translated)
  const metaMap = buildSelectedWordMetaMap()
  const max = Math.max(enParts.length, zhParts.length)
  const blocks = []
  for (let i = 0; i < max; i += 1) {
    const enHtml = renderHighlightedParagraph(enParts[i] || '', metaMap)
    const zhText = escapeHtml(zhParts[i] || '')
    blocks.push(`
      <div class="pair">
        <div class="en">${enHtml}</div>
        <div class="zh">${zhText}</div>
      </div>
    `)
  }

  const difficulty = article?.difficulty_level || article?.difficultyLevel || ''
  const length = article?.article_length || article?.articleLength || ''

  return `<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>${escapeHtml(title)}</title>
    <style>
      body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif; max-width: 980px; margin: 0 auto; padding: 32px; line-height: 1.7; color: #0f172a; }
      h1 { margin: 0 0 8px 0; font-size: 28px; }
      .meta { color: #64748b; margin-bottom: 24px; }
      .pair { margin: 18px 0; padding: 14px 16px; border: 1px solid #e2e8f0; border-radius: 10px; background: #ffffff; }
      .en p { margin: 0; font-size: 16px; }
      .zh { margin-top: 10px; color: #334155; white-space: pre-wrap; }
      .vocab-chip { display: inline-block; padding: 0 6px; border-radius: 6px; background: #fef3c7; color: #1d4ed8; font-weight: 700; }
      .code-block { position: relative; background: #0b1220; border-radius: 10px; padding: 14px 16px; margin: 14px 0; overflow: auto; }
      .code-block code { color: #e2e8f0; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; font-size: 13px; line-height: 1.6; }
      .copy-button { position: absolute; top: 10px; right: 10px; background: rgba(255,255,255,0.9); border: 1px solid #e2e8f0; border-radius: 999px; padding: 6px 10px; font-size: 12px; cursor: pointer; }
      .copy-button.copied { border-color: #10b981; color: #10b981; }
    </style>
  </head>
  <body>
    <h1>${escapeHtml(title)}</h1>
    <div class="meta">难度：${escapeHtml(difficulty)} ｜ 长度：${escapeHtml(length)}</div>
    ${blocks.join('\n')}
  </body>
</html>`
}

const escapeRegExp = (value) => {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

const renderHighlightedTextBySelection = (text, metaMap) => {
  if (!text) return ''
  const keys = Array.from(metaMap?.keys?.() || [])
    .map(k => String(k || '').trim())
    .filter(Boolean)
  if (keys.length === 0) return escapeHtml(text)
  keys.sort((a, b) => b.length - a.length)
  const pattern = keys.map(escapeRegExp).join('|')
  const re = new RegExp(`\\b(${pattern})\\b`, 'gi')
  const escaped = escapeHtml(String(text).replaceAll('**', ''))
  return escaped.replace(re, (matched) => {
    const key = String(matched || '').toLowerCase()
    const meta = metaMap?.get?.(key)
    const tip = meta
      ? `${meta.word}${meta.partOfSpeech ? ` (${meta.partOfSpeech})` : ''}：${meta.definition || ''}`
      : matched
    return `<span class="vocab-chip" title="${escapeHtml(tip)}">${escapeHtml(matched)}</span>`
  })
}

const buildHtmlForStudyDownload = (article) => {
  const title = article?.topic || '未命名文章'
  const original = article?.original_text || article?.originalText || ''
  const translated = article?.translated_text || article?.translatedText || ''
  const enParts = splitParagraphs(String(original).replaceAll('**', ''))
  const zhParts = splitParagraphs(translated)
  const metaMap = buildSelectedWordMetaMap()
  const max = Math.max(enParts.length, zhParts.length)
  const blocks = []
  for (let i = 0; i < max; i += 1) {
    const enHtml = renderHighlightedTextBySelection(enParts[i] || '', metaMap)
    const zhText = escapeHtml(zhParts[i] || '')
    blocks.push(`
      <div class="para">
        <div class="en">${enHtml}</div>
        ${zhText ? `<div class="zh">${zhText}</div>` : ''}
      </div>
    `)
  }

  const words = (printUsedWords.value || []).slice().sort((a, b) => String(a.word).localeCompare(String(b.word)))
  const wordRows = words.map((w) => `
    <tr>
      <td class="w-word">${escapeHtml(w.word || '')}</td>
      <td class="w-pos">${escapeHtml(w.partOfSpeech || '-')}</td>
      <td class="w-def">${escapeHtml(w.definition || '')}</td>
      <td class="w-cnt">${w.occurrenceCount ? escapeHtml(String(w.occurrenceCount)) : '-'}</td>
    </tr>
  `).join('\n')

  return `<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>${escapeHtml(title)}</title>
    <style>
      body { font-family: "Microsoft YaHei", -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif; max-width: 980px; margin: 0 auto; padding: 32px; line-height: 1.7; color: #0f172a; }
      h1 { margin: 0 0 22px 0; font-size: 28px; text-align: center; }
      .para { margin: 14px 0; padding: 14px 16px; border: 1px solid #e2e8f0; border-radius: 10px; background: #ffffff; page-break-inside: avoid; }
      .en { margin: 0; font-size: 16px; white-space: pre-wrap; }
      .zh { margin-top: 10px; color: #334155; white-space: pre-wrap; }
      .vocab-chip { display: inline-block; padding: 0 6px; border-radius: 6px; background: #fef3c7; color: #1d4ed8; font-weight: 700; }
      .word-title { margin: 28px 0 0 0; font-size: 18px; font-weight: 700; }
      table { width: 100%; border-collapse: collapse; margin-top: 10px; }
      th, td { border: 1px solid #e5e7eb; padding: 10px 12px; text-align: left; vertical-align: top; font-size: 14px; }
      th { background: #f8fafc; }
      .w-cnt { width: 86px; text-align: center; }
    </style>
  </head>
  <body>
    <h1>${escapeHtml(title)}</h1>
    ${blocks.join('\n')}
    <div class="word-title">单词列表</div>
    <table>
      <thead>
        <tr>
          <th style="width: 25%;">单词</th>
          <th style="width: 15%;">词性</th>
          <th>释义</th>
          <th class="w-cnt">次数</th>
        </tr>
      </thead>
      <tbody>
        ${wordRows || ''}
      </tbody>
    </table>
  </body>
</html>`
}

// AI Article Methods
const onListChange = async () => {
  markActive()
  if (currentListId.value) {
    if (currentListId.value === 'public') {
      await searchPublic()
    } else {
      await vocabularyStore.fetchWords(currentListId.value)
    }
    selectedWordIds.clear()
    generatedTopics.value = []
    articleOptions.topic = ''
  }
}

const toggleSelectAll = (e) => {
  markActive()
  const list = filteredWords.value
  if (e.target.checked) {
    list.forEach(w => selectedWordIds.add(w.id))
  } else {
    list.forEach(w => selectedWordIds.delete(w.id))
  }
}

const toggleWordSelection = (wordId) => {
  markActive()
  if (selectedWordIds.has(wordId)) {
    selectedWordIds.delete(wordId)
  } else {
    selectedWordIds.add(wordId)
  }
}

/**
 * 选择AI推荐主题
 */
const selectTopic = (topic) => {
  markActive()
  articleOptions.topic = topic
}

/**
 * 生成主题建议
 */
const generateTopics = async () => {
  markActive()
  if (selectedWordIds.size === 0) return
  isGeneratingTopics.value = true

  const sourceList = currentListId.value === 'public' ? publicResults.value : currentWords.value
  const words = sourceList
    .filter(w => selectedWordIds.has(w.id))
    .map(w => w.word)

  const lang = currentListId.value === 'public' ? 'en' : (currentList.value?.language || 'en')
  const result = await vocabularyStore.generateTopics(words, lang)
  if (result.success) {
    generatedTopics.value = result.data
  } else {
    uiStore.showToast(result.message)
  }
  isGeneratingTopics.value = false
}

/**
 * 生成文章并打开结果弹窗（非阻塞版本）
 */
const generateArticleNow = async () => {
  markActive()
  if (!currentListId.value || selectedWordIds.size === 0) return

  // 显示生成状态指示器
  articleGenerationInProgress.value = true
  articleGenerationComplete.value = false

  // 异步执行文章生成，不阻塞主线程
  const generateArticleAsync = async () => {
    try {
      const sourceList = currentListId.value === 'public' ? publicResults.value : currentWords.value
      const selectedWordsList = sourceList
        .filter(w => selectedWordIds.has(w.id))
        .map(w => w.word)
      const listIdToPass = currentListId.value === 'public' ? null : currentListId.value

      const result = await vocabularyStore.generateArticle({
        listId: listIdToPass,
        wordIds: Array.from(selectedWordIds),
        wordList: selectedWordsList,
        topic: articleOptions.topic,
        difficulty: articleOptions.difficulty,
        length: articleOptions.length,
        targetLanguage: currentListId.value === 'public' ? 'en' : (currentList.value?.language || 'en')
      })

      if (result.success) {
        // 更新文章列表，用户可以在“我的文章”中查看新生成的文章
        await loadMyArticles(true)
        articleGenerationComplete.value = true
        uiStore.showToast('文章生成成功')

        // 3秒后自动隐藏完成提示
        setTimeout(() => {
          articleGenerationComplete.value = false
        }, 3000)
      } else {
        uiStore.showToast(result.message)
      }
    } catch (error) {
      console.error('生成文章失败:', error)
      uiStore.showToast('生成文章失败，请稍后重试')
    } finally {
      articleGenerationInProgress.value = false
    }
  }

  // 启动异步生成任务
  generateArticleAsync()
}

/**
 * 拉取“我的文章”列表
 */
const loadMyArticles = async (force = false) => {
  markActive()
  if (!force && myArticles.value.length > 0) return
  isLoadingArticles.value = true
  const result = await vocabularyStore.fetchArticles()
  if (result.success) {
    myArticles.value = result.data || []
  } else {
    uiStore.showToast(result.message)
  }
  isLoadingArticles.value = false
}

/**
 * 打开历史文章详情
 */
const openMyArticle = async (articleId) => {
  markActive()
  if (!articleId) return
  isGeneratingArticle.value = true
  const result = await vocabularyStore.fetchArticle(articleId)
  if (result.success) {
    activeArticle.value = result.data
    showArticleModal.value = true
    showDownloadMenu.value = false
  } else {
    uiStore.showToast(result.message)
  }
  isGeneratingArticle.value = false
}

/**
 * 关闭文章弹窗
 */
const closeArticleModal = () => {
  markActive()
  showDownloadMenu.value = false
  showArticleModal.value = false
  activeArticle.value = null
}

/**
 * 切换下载菜单显示状态
 */
const toggleDownloadMenu = () => {
  markActive()
  showDownloadMenu.value = !showDownloadMenu.value
}

const downloadArticle = async (type) => {
  markActive()
  if (!activeArticle.value) return
  showDownloadMenu.value = false
  const title = activeArticle.value.topic || '未命名文章'
  if (type === 'study') {
    await nextTick()
    const html = buildHtmlForStudyDownload(activeArticle.value)
    downloadBlob(`${title}-学习版.html`, html, 'text/html;charset=utf-8')
    return
  }
  if (type === 'pdf') {
    await nextTick()
    const html = buildHtmlForStudyDownload(activeArticle.value)
    const articleId = activeArticle.value.id
    if (!articleId) {
      uiStore.showToast('文章ID不存在，无法下载PDF')
      return
    }
    try {
      const pdfBlob = await request.post(
        API_ENDPOINTS.vocabulary.downloadArticlePdf(articleId),
        { html, filename: title },
        { responseType: 'blob', timeout: 60000 }
      )
      const blob = pdfBlob instanceof Blob ? pdfBlob : new Blob([pdfBlob], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `${title}-学习版.pdf`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (error) {
      let message = error?.response?.data?.message || error?.response?.data?.detail || error?.message || '下载PDF失败'
      const data = error?.response?.data
      if (data instanceof Blob) {
        try {
          const text = await data.text()
          const json = JSON.parse(text)
          message = json?.message || json?.detail || message
        } catch {
        }
      }
      uiStore.showToast(message)
    }
    return
  }
  if (type === 'txt') {
    const original = activeArticle.value.original_text || activeArticle.value.originalText || ''
    const translated = activeArticle.value.translated_text || activeArticle.value.translatedText || ''
    const content = `${title}\n\n${original}\n\n---\n\n${translated}\n`
    downloadBlob(`${title}.txt`, content, 'text/plain;charset=utf-8')
    return
  }
  const html = buildHtmlForArticle(activeArticle.value)
  downloadBlob(`${title}.html`, html, 'text/html;charset=utf-8')
}

// Other existing methods
const createList = async () => {
  markActive()
  if (!newList.value.name.trim()) {
    uiStore.showToast('请输入单词表名称')
    return
  }

  const result = await vocabularyStore.createList({
    name: newList.value.name.trim(),
    description: newList.value.description?.trim() || '',
    language: newList.value.language || 'en'
  })
  if (!result.success) {
    uiStore.showToast('创建失败: ' + (result.message || '未知错误'))
    return
  }
  showCreateList.value = false
  newList.value = { name: '', description: '', language: 'en' }
  if (result.data?.id) {
    await selectList(result.data.id)
    currentView.value = 'my-words' // Switch to list view
    uiStore.showToast('创建成功')
  }
}

const addWord = async () => {
  markActive()
  if (!currentListId.value) return
  if (!newWord.value.word.trim() || !newWord.value.definition.trim()) {
    uiStore.showToast('请填写单词和释义')
    return
  }

  const result = await vocabularyStore.addWord(currentListId.value, {
    word: newWord.value.word.trim(),
    definition: newWord.value.definition.trim(),
    partOfSpeech: newWord.value.partOfSpeech?.trim() || '',
    example: newWord.value.example?.trim() || '',
    language: currentList.value?.language || 'en'
  })
  if (!result.success) {
    uiStore.showToast('添加失败: ' + (result.message || '未知错误'))
    return
  }
  showAddWord.value = false
  newWord.value = { word: '', definition: '', partOfSpeech: '', example: '' }
  uiStore.showToast('添加成功')
  await vocabularyStore.recordActivity({
    activityType: 'vocabulary_add_word',
    activityDetails: JSON.stringify({ listId: currentListId.value, wordId: result.data?.id }),
    duration: 0
  })
}

const refreshCurrentList = async () => {
  markActive()
  if (!currentListId.value) return
  await vocabularyStore.fetchWords(currentListId.value)
  await vocabularyStore.fetchListProgress(currentListId.value)
}

const refreshReview = async () => {
  markActive()
  await vocabularyStore.fetchReviewWords()
}

const getWordProgress = (wordId) => {
  return vocabularyStore.progressByWordId[wordId] || { masteryLevel: 0, isDifficult: false }
}

const getReviewWordId = (item) => {
  return item?.wordId ?? item?.word?.id ?? item?.word_id ?? item?.word?.wordId ?? item?.word?.word_id ?? null
}

const getMasteryText = (level) => {
  const l = Number(level || 0)
  if (l >= 5) return '完全掌握'
  if (l >= 4) return '已掌握'
  if (l >= 2) return '学习中'
  return '新单词'
}

const getMasteryClass = (level) => {
  const l = Number(level || 0)
  if (l >= 4) return 'mastered'
  if (l >= 2) return 'learning'
  return 'new'
}

const changeMastery = async (wordId, value) => {
  markActive()
  const masteryLevel = Number(value)
  const current = getWordProgress(wordId)
  const result = await vocabularyStore.updateProgress({
    wordId,
    masteryLevel,
    isDifficult: current.isDifficult
  })
  if (!result.success) {
    uiStore.showToast('更新失败: ' + (result.message || '未知错误'))
    return
  }
  uiStore.showToast('更新成功')
  await Promise.all([vocabularyStore.fetchStats(), vocabularyStore.fetchReviewWords()])
}

const removeWord = async (wordId) => {
  markActive()
  if (!currentListId.value) return
  if (!confirm('确定要删除这个单词吗？')) return
  const result = await vocabularyStore.deleteWord(currentListId.value, wordId)
  if (!result.success) {
    uiStore.showToast('删除失败: ' + (result.message || '未知错误'))
  } else {
    uiStore.showToast('删除成功')
  }
}

const removeList = async (listId) => {
  markActive()
  if (!confirm('确定要删除这个单词表吗？')) return
  const result = await vocabularyStore.deleteList(listId)
  if (!result.success) {
    uiStore.showToast('删除失败: ' + (result.message || '未知错误'))
    return
  }
  uiStore.showToast('删除成功')
  if (currentListId.value === listId) {
    currentListId.value = null
    activeArticle.value = null
    showArticleModal.value = false
    publicKeyword.value = ''
  }
}

const quickReview = async (wordId, masteryLevel) => {
  markActive()
  if (wordId == null) {
    uiStore.showToast('单词ID不能为空')
    return
  }
  const current = getWordProgress(wordId)
  const result = await vocabularyStore.updateProgress({
    wordId,
    masteryLevel,
    isDifficult: current.isDifficult
  })
  if (!result.success) {
    uiStore.showToast('更新失败: ' + (result.message || '未知错误'))
    return
  }
  uiStore.showToast('更新成功')
  await vocabularyStore.recordActivity({
    activityType: 'vocabulary_review',
    activityDetails: JSON.stringify({ wordId, masteryLevel }),
    duration: 0
  })
  await Promise.all([vocabularyStore.fetchStats(), vocabularyStore.fetchReviewWords()])
}

const changePublicPage = async (page) => {
  if (page < 1) return
  currentPage.value = page
  await searchPublic()
}

const searchPublic = async () => {
  markActive()
  const kw = publicKeyword.value.trim()
  const language = currentList.value?.language || 'en'
  const result = await vocabularyStore.searchPublic(kw, language, currentPage.value, 50)
  if (!result.success) {
    uiStore.showToast('搜索失败: ' + (result.message || '未知错误'))
  }
}

const addPublicWord = async (w) => {
  markActive()
  if (!currentListId.value) {
    uiStore.showToast('请先选择一个单词表')
    return
  }
  const result = await vocabularyStore.addWord(currentListId.value, {
    word: w.word,
    definition: w.definition,
    partOfSpeech: w.partOfSpeech,
    example: w.example,
    language: w.language || currentList.value?.language || 'en'
  })
  if (!result.success) {
    uiStore.showToast('添加失败: ' + (result.message || '未知错误'))
    return
  }
  uiStore.showToast('添加成功')
  await vocabularyStore.recordActivity({
    activityType: 'vocabulary_add_public_word',
    activityDetails: JSON.stringify({ listId: currentListId.value, publicWordId: w.id, wordId: result.data?.id }),
    duration: 0
  })
}

const formatDuration = (seconds) => {
  const s = Math.max(0, Number(seconds || 0))
  const m = Math.floor(s / 60)
  const h = Math.floor(m / 60)
  const mm = m % 60
  if (h > 0) return `${h}小时${mm}分钟`
  if (mm > 0) return `${mm}分钟`
  return `${s}秒`
}
</script>

<style scoped>
/* Page Layout */
.language-learning-page {
  height: 100%;
  overflow: hidden;
  background-color: var(--bg-primary);
  position: relative;
}

/* Article Generation Notification */
.article-generation-notification {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 300px;
  justify-content: center;
}

.generation-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-primary);
  font-size: 14px;
}

.generation-complete {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--success-color);
  font-size: 14px;
}

.spinner.small {
  width: 20px;
  height: 20px;
  border-width: 2px;
}

/* Main Content */
.main-content {
  flex: 1;
  overflow-y: auto; /* Scroll internally */
  padding: 10px 15px;
  position: relative;
  background-color: var(--bg-primary);
}

.view-header {
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.view-header h2 {
  font-size: 18px;
  color: var(--text-primary);
  margin: 0 0 2px 0;
}

.view-header p {
  color: var(--text-secondary);
  margin: 0;
  font-size: 12px;
}

/* AI Articles View Header Specific */
.ai-articles-header {
  align-items: center;
}

.ai-articles-view .ai-article-tabs {
  display: inline-flex;
  gap: 6px;
  padding: 6px;
  margin: 0;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
}

/* Dashboard Grid - Improved */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-secondary);
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s;
  border: 1px solid var(--border-color);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background-color: var(--bg-tertiary);
}

.stat-card.primary .stat-icon { background-color: var(--chip-bg); color: var(--primary-color); }
.stat-card.success .stat-icon { background-color: rgba(16, 185, 129, 0.15); color: #10b981; }
.stat-card.warning .stat-icon { background-color: rgba(245, 158, 11, 0.15); color: #f59e0b; }
.stat-card.info .stat-icon { background-color: rgba(59, 130, 246, 0.15); color: #3b82f6; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* Review Section */
.review-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
}

.review-card-item {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-word {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 2px 0;
}

.review-def {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.review-actions {
  display: flex;
  gap: 6px;
}

.review-actions .btn {
  padding: 4px 10px;
  min-height: 32px;
  font-size: 12px;
}

/* Code Block Styles */
.code-block {
  position: relative;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  padding: 16px;
  margin: 16px 0;
  overflow-x: auto;
  box-shadow: var(--shadow-sm);
}

.code-block code {
  font-family: var(--font-mono);
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* Copy Button Styles */
.copy-button {
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: rgba(243, 244, 246, 0.8);
  color: #4b5563;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.code-block:hover .copy-button {
  opacity: 1;
}

.copy-button:hover {
  background-color: #f9fafb;
  color: var(--primary-color);
  border-color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.copy-button:active {
  transform: translateY(0);
}

.copy-button.copied {
  background-color: #ecfdf5;
  color: #10b981;
  border-color: #10b981;
  opacity: 1 !important;
}

/* Dark Mode Adjustments */
body.dark-mode .copy-button {
  background-color: rgba(31, 41, 55, 0.8);
  color: #e5e7eb;
  border-color: #4b5563;
}

body.dark-mode .copy-button:hover {
  background-color: #374151;
  color: #60a5fa;
  border-color: #60a5fa;
}

body.dark-mode .copy-button.copied {
  background-color: rgba(6, 78, 59, 0.4);
  color: #34d399;
  border-color: #34d399;
}

.public-pos {
  font-size: 12px;
  color: var(--text-tertiary);
  font-style: italic;
  font-weight: 400;
  margin-left: 8px;
}

/* Search Bar Styles */
.search-bar-container {
  margin-bottom: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
}

.search-input-wrapper {
  display: flex;
  gap: 8px;
  flex: 1;
  max-width: 600px;
  width: 100%;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  transition: all 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  background-color: var(--bg-primary);
}

.search-btn {
  min-width: 80px;
  white-space: nowrap;
  transition: all 0.2s ease;
}

/* Wordlist Selection Styles */
.wordlist-selection {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  max-width: 600px;
  padding: 20px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
}

.wordlist-selection:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.wordlist-selection label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 70px;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

/* Custom Select Styles */
.wordlist-selection select {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  font-size: 15px;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  font-weight: 500;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  background-size: 16px;
  padding-right: 45px !important;
}

.wordlist-selection select:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
  background-color: var(--bg-primary);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%233b82f6' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
}

.wordlist-selection select:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

/* Option Styles */
.wordlist-selection select option {
  padding: 12px 16px;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.wordlist-selection select option:checked {
  background-color: var(--chip-bg);
  color: var(--primary-color);
}

.wordlist-selection select option:hover {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
}

/* Focus styles for the entire selection area */
.wordlist-selection:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

/* Ensure consistent spacing */
.wordlist-selection label, .wordlist-selection select {
  margin: 0;
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .wordlist-selection {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .wordlist-selection label {
    min-width: auto;
  }

  .wordlist-selection select {
    width: 100%;
  }
}

/* Public Results Grid Styles */
.public-results-grid {
  margin-bottom: 32px;
}

.public-results-container {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
}

.public-results-scroll-container {
  max-height: 500px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  transition: all 0.3s ease;
  scrollbar-width: thin;
  scrollbar-color: var(--border-color) var(--bg-tertiary);
}

/* Custom scrollbar */
.public-results-scroll-container::-webkit-scrollbar {
  width: 8px;
}

.public-results-scroll-container::-webkit-scrollbar-track {
  background-color: var(--bg-tertiary);
  border-radius: 4px;
}

.public-results-scroll-container::-webkit-scrollbar-thumb {
  background-color: var(--border-color);
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.public-results-scroll-container::-webkit-scrollbar-thumb:hover {
  background-color: var(--text-tertiary);
}

.public-library-view .review-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  transition: all 0.3s ease;
}

.public-library-view .review-card-item {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 18px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 160px;
  box-shadow: var(--shadow-sm);
}

.public-library-view .review-card-item:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-color);
}

.public-library-view .review-content {
  flex: 1;
  margin-bottom: 14px;
}

.public-library-view .review-word {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary-color);
  margin: 0 0 6px 0;
  display: flex;
  align-items: baseline;
  gap: 8px;
  transition: all 0.2s ease;
}

.public-library-view .review-def {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
  margin: 0 0 10px 0;
}

.public-library-view .review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.public-library-view .review-actions .btn {
  min-width: 70px;
  font-size: 13px;
  padding: 6px 12px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Pagination Styles */
.pagination-container {
  padding: 16px 20px;
  background-color: var(--bg-tertiary);
  border-top: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

/* Optimize select dropdown */
select {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  background-size: 16px;
  padding-right: 40px !important;
}

/* Smooth transition for all interactive elements */
.btn, .select-input, .list-item, .review-card-item, .search-input, .wordlist-selection {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

/* Add subtle animations */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.public-library-view .review-card-item {
  animation: fadeInUp 0.4s ease-out;
  animation-fill-mode: both;
}

.public-library-view .review-card-item:nth-child(1) { animation-delay: 0.05s; }
.public-library-view .review-card-item:nth-child(2) { animation-delay: 0.1s; }
.public-library-view .review-card-item:nth-child(3) { animation-delay: 0.15s; }
.public-library-view .review-card-item:nth-child(4) { animation-delay: 0.2s; }
.public-library-view .review-card-item:nth-child(5) { animation-delay: 0.25s; }
.public-library-view .review-card-item:nth-child(6) { animation-delay: 0.3s; }
.public-library-view .review-card-item:nth-child(7) { animation-delay: 0.35s; }
.public-library-view .review-card-item:nth-child(8) { animation-delay: 0.4s; }

/* Loading and Empty States */
.loading-state, .empty-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--text-secondary);
  font-size: 14px;
}

.loading-state .spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  margin: 0 auto 16px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* My Words Layout */
.two-column-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  height: calc(100vh - 160px); /* Fit within main content */
}

.list-column, .words-column {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-container {
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-item {
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background-color: var(--bg-secondary);
}

.list-item:hover {
  border-color: var(--primary-color);
  background-color: var(--bg-tertiary);
}

.list-item.active {
  border-color: var(--primary-color);
  background-color: var(--chip-bg);
  box-shadow: var(--shadow-sm);
}

.list-info h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  color: var(--text-primary);
}

.badge {
  display: inline-block;
  padding: 2px 6px;
  background: var(--bg-tertiary);
  border-radius: 4px;
  font-size: 10px;
  color: var(--text-secondary);
  margin-right: 8px;
}

.count {
  font-size: 12px;
  color: var(--text-tertiary);
}

.words-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
  overflow-y: auto;
  padding: 4px;
}

.word-card-item {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.word-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.word-header h4 {
  margin: 0;
  font-size: 18px;
  color: var(--primary-color);
}

.pos {
  font-size: 12px;
  color: var(--text-secondary);
  font-style: italic;
}

.word-body {
  flex: 1;
  margin-bottom: 12px;
}

.definition {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.example {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.word-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
}
.status-tag.new { background: var(--chip-bg); color: var(--primary-color); }
.status-tag.learning { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
.status-tag.mastered { background: rgba(16, 185, 129, 0.15); color: #10b981; }

.controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.select-sm {
  padding: 2px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-size: 12px;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.ai-articles-view .article-generator {
  padding: 0;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background-color: var(--bg-primary);
  overflow: hidden;
}

.ai-articles-view .ai-tab-btn {
  padding: 6px 12px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--text-secondary);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  line-height: 1;
}

.ai-articles-view .ai-tab-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.ai-articles-view .ai-tab-btn.active {
  background-color: var(--chip-bg);
  border-color: rgba(59, 130, 246, 0.35);
  color: var(--primary-color);
}

.ai-articles-view .ai-generate-panel {
  padding: 12px;
}

.ai-articles-view .ai-generate-grid {
  display: grid;
  grid-template-columns: 330px 1fr;
  gap: 12px;
  align-items: stretch; /* Stretch cards to match height */
  height: 580px; /* Reduced to fit without scrolling */
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-primary);
  user-select: none;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.ai-articles-view .ai-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background-color: var(--bg-secondary);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%; /* Fill grid height */
}

.ai-articles-view .ai-card-header {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(180deg, var(--bg-secondary) 0%, var(--bg-primary) 100%);
}

.ai-articles-view .ai-card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.1px;
}

.ai-articles-view .ai-card-subtitle {
  margin-top: 1px;
  font-size: 10px;
  color: var(--text-secondary);
}

.ai-articles-view .ai-card-body {
  padding: 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-articles-view .ai-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 8px;
}

.ai-articles-view .ai-form-grid .full-width {
  grid-column: span 2;
}

.ai-articles-view .ai-form-grid .form-group {
  margin-bottom: 0;
}

.ai-articles-view .ai-form-grid .form-group label {
  font-size: 11px;
  font-weight: 600;
}

.ai-articles-view .ai-topic-input-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.ai-articles-view .ai-topic-input-group .input {
  flex: 1;
  margin-bottom: 0;
  padding: 8px 12px;
  font-size: 13px;
}

.ai-articles-view .btn-icon-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  padding: 0 12px;
  font-size: 12px;
}

.ai-articles-view .ai-topic-suggestions {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background-color: var(--bg-tertiary);
  border: 1px dashed var(--border-color);
}

.ai-articles-view .ai-topic-suggestions-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.ai-articles-view .ai-topic-suggestions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ai-articles-view .ai-topic-chip {
  border: 1px solid var(--border-color);
  background-color: var(--bg-primary);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s;
}

.ai-articles-view .ai-topic-chip:hover {
  border-color: rgba(59, 130, 246, 0.35);
  background-color: var(--chip-bg);
  color: var(--primary-color);
}

.ai-articles-view .ai-word-selection-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}

.ai-articles-view .ai-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background-color: var(--bg-secondary);
  margin-bottom: 12px;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.02);
  align-items: stretch; /* Ensure children fill width but don't stretch height unevenly */
}

.ai-toolbar-top {
  width: 100%;
  flex: 0 0 auto;
}

.ai-toolbar-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
  flex: 0 0 auto;
}

.ai-toolbar-left {
  display: flex;
  align-items: center;
}

.ai-search-wrapper {
  position: relative;
  width: 100%;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  color: var(--text-tertiary);
  pointer-events: none;
}

.ai-search-input {
  width: 100%;
  padding: 6px 12px 6px 36px;
  border: 1.5px solid var(--border-color);
  border-radius: 8px;
  font-size: 13px;
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-sm);
  min-height: 34px; /* Reduced from 40px */
}

.ai-search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
  background: var(--bg-primary);
  transform: translateY(-1px);
}

.ai-search-input::placeholder {
  color: var(--text-tertiary);
  font-style: italic;
  opacity: 0.7;
}

.ai-articles-view .ai-toolbar-meta {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.ai-articles-view .ai-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-mini {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-right: 12px;
  padding-right: 16px;
  border-right: 1.5px solid var(--border-color);
}

.page-info {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 60px;
  text-align: center;
}

.ai-toolbar-actions .btn {
  min-width: 90px;
  justify-content: center;
}

.ai-words-table-wrapper,
 .ai-articles-table-wrapper,
 .ai-selected-words-table-wrapper {
   border: 1px solid var(--border-color);
   border-radius: 10px;
   overflow: hidden;
   background-color: var(--bg-primary);
 }

 .ai-words-table-wrapper {
   flex: 1; /* Grow to fill card body */
   overflow-y: auto;
 }

.ai-words-table,
.ai-articles-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.ai-words-table th,
.ai-articles-table th {
  background-color: var(--bg-tertiary);
  padding: 10px 12px;
  text-align: left;
  font-weight: 700;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 10;
  font-size: 11px;
  letter-spacing: 0.4px;
}

.ai-words-table td,
.ai-articles-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 12px;
  vertical-align: top;
  background-color: var(--bg-primary);
  transition: background-color 0.2s;
}

.ai-words-table tbody tr:nth-child(even) td,
.ai-articles-table tbody tr:nth-child(even) td {
  background-color: rgba(0, 0, 0, 0.01);
}

.ai-words-table tbody tr:hover td,
.ai-articles-table tbody tr:hover td {
  background-color: var(--bg-tertiary);
  cursor: pointer;
}

.ai-words-table tr.selected td {
  background-color: var(--chip-bg);
}

.ai-word-text {
  font-weight: 700;
  color: var(--text-primary);
}

.ai-word-definition {
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pos-tag {
  display: inline-flex;
  padding: 1px 6px;
  background-color: var(--bg-tertiary);
  border-radius: 999px;
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 600;
}

.ai-articles-view .ai-empty-hint {
  text-align: center;
  padding: 40px 15px;
  color: var(--text-secondary);
  background-color: var(--bg-primary);
  border-radius: 10px;
  border: 1px dashed var(--border-color);
}

.ai-articles-view .ai-empty-in-card {
  margin: 0;
}

.ai-articles-view .empty-icon {
  font-size: 32px;
  margin-bottom: 10px;
  opacity: 0.55;
}

.ai-articles-view .ai-myarticles-panel {
  padding: 12px;
}

.ai-articles-view .ai-myarticles-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 10px 0;
  padding: 0 2px;
}

.ai-articles-view .ai-myarticles-meta {
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 600;
}

.ai-article-title {
  font-weight: 700;
  color: var(--text-primary);
}

.empty-cell {
  text-align: center;
  padding: 30px !important;
  color: var(--text-tertiary);
  font-style: italic;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .ai-articles-view .ai-generate-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .ai-articles-view .ai-article-tabs {
    width: calc(100% - 36px);
  }

  .ai-articles-view .ai-form-grid {
    grid-template-columns: 1fr;
  }

  .ai-articles-view .ai-topic-input-group {
    flex-direction: column;
    align-items: stretch;
  }

  .ai-articles-view .ai-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .ai-articles-view .ai-toolbar-actions {
    justify-content: flex-end;
  }
}

/* AI Wizard Styles */
.step-container {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px;
}

.step-container h3 {
  margin-bottom: 24px;
  color: var(--text-primary);
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-primary);
}

.select-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 14px;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  transition: border-color 0.2s;
}

.select-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.word-selection {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
  max-height: 500px;
  overflow-y: auto;
  background-color: var(--bg-secondary);
}

.selection-header {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 12px;
}

.word-checkboxes {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.word-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background 0.2s;
  color: var(--text-primary);
}

.word-checkbox:hover {
  background-color: var(--bg-tertiary);
}

/* Pagination Styles */
.pagination-container {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination-info {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
}

.pagination-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.pagination-page {
  font-size: 14px;
  color: var(--text-secondary);
  padding: 0 12px;
}

.btn-outline {
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--primary-color);
  transition: all 0.2s;
}

.btn-outline:hover:not(:disabled) {
  background-color: var(--chip-bg);
  border-color: var(--primary-color);
}

.btn-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.step-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 24px;
  padding: 24px;
  border-top: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  border-radius: 0 0 8px 8px;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.topic-card {
  border: 2px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.topic-card:hover {
  border-color: var(--text-tertiary);
}

.topic-card.selected {
  border-color: var(--primary-color);
  background-color: var(--chip-bg);
}

.topic-text {
  font-weight: 500;
}

.loading-state {
  text-align: center;
  padding: 60px;
  color: var(--text-secondary);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.article-display-area {
  background: var(--bg-secondary);
  padding: 40px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.article-content :deep(strong) {
  color: var(--primary-color);
  font-weight: 700;
}

.vocabulary-list-append {
  margin-top: 40px;
  padding-top: 40px;
  border-top: 1px solid var(--border-color);
}

.vocabulary-list-append table {
  width: 100%;
  border-collapse: collapse;
}

.vocabulary-list-append th,
.vocabulary-list-append td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
}

.print-only {
  display: none;
}

@media print {
  :global(html),
  :global(body) {
    background: white !important;
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    height: auto !important;
    overflow: visible !important;
  }

  :global(body *) {
    visibility: hidden !important;
  }

  :global(#article-print-area),
  :global(#article-print-area *) {
    visibility: visible !important;
  }

  :global(#article-print-area) {
    display: block !important;
    position: fixed !important;
    left: 0 !important;
    top: 0 !important;
    width: 100% !important;
    padding: 18pt 18pt 24pt !important;
    box-sizing: border-box !important;
    background: white !important;
    color: black !important;
  }
}

.article-print-area {
  display: none;
}

.article-print-title {
  font-size: 22pt;
  font-weight: 700;
  text-align: center;
  margin: 0 0 16pt 0;
}

.article-print-paragraph {
  page-break-inside: avoid;
  margin: 0 0 14pt 0;
}

.article-print-en {
  font-size: 12pt;
  line-height: 1.7;
  margin: 0 0 8pt 0;
  white-space: pre-wrap;
}

.article-print-zh {
  font-size: 11pt;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

.article-print-words {
  margin-top: 18pt;
  page-break-inside: avoid;
}

.article-print-words-title {
  font-size: 14pt;
  font-weight: 700;
  margin: 0 0 10pt 0;
}

.article-print-words-table {
  width: 100%;
  border-collapse: collapse;
}

.article-print-words-table th,
.article-print-words-table td {
  border: 1px solid #e5e7eb;
  padding: 8pt 10pt;
  font-size: 10.5pt;
  vertical-align: top;
}

/* Modals */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.modal-card.modal-xxl {
  max-width: min(1200px, 95vw);
  width: 1200px;
}

.ai-article-modal-content {
  padding: 24px;
  max-height: calc(85vh - 140px);
  overflow-y: auto;
  line-height: 1.8;
  background-color: var(--bg-primary);
}

.ai-article-paragraph-pair {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px dashed var(--border-color);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}

.ai-article-en {
  font-size: 18px;
  color: var(--text-primary);
}

.ai-article-zh {
  font-size: 16px;
  color: var(--text-secondary);
  background-color: var(--bg-tertiary);
  padding: 16px;
  border-radius: 8px;
}

.ai-article-en :deep(.vocab-chip) {
  display: inline-block;
  padding: 0 6px;
  border-radius: 6px;
  background: #fef3c7;
  color: #1d4ed8;
  font-weight: 700;
}

.ai-article-modal-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 24px;
}

.modal-wide-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}

.ai-article-modal-meta {
  padding: 12px 24px;
  background-color: var(--bg-tertiary);
  font-size: 13px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
}

.ai-article-modal-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.modal-close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  font-size: 24px;
  line-height: 1;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s ease;
  padding: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.modal-close-btn:hover {
  background-color: #fee2e2;
  color: #dc2626;
  border-color: #fecaca;
  transform: scale(1.05);
}

:global(body.dark-mode) .modal-close-btn:hover {
  background-color: rgba(220, 38, 38, 0.2);
  color: #f87171;
  border-color: rgba(220, 38, 38, 0.3);
}

.ai-download {
  position: relative;
}

.ai-download-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: var(--shadow-lg);
  z-index: 100;
  min-width: 140px;
  overflow: hidden;
}

.ai-download-item {
  width: 100%;
  padding: 10px 16px;
  text-align: left;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.ai-download-item:hover {
  background-color: var(--bg-tertiary);
}

.modal-card {
  background: var(--bg-secondary);
  padding: 24px;
  border-radius: 12px;
  width: 100%;
  max-width: 400px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.modal-card h3 {
  color: var(--text-primary);
  margin-top: 0;
  margin-bottom: 20px;
}

.input, .textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  margin-bottom: 12px;
  font-size: 16px; /* Prevent zoom on iOS */
  min-height: 44px; /* Touch target size */
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.textarea {
  min-height: 120px;
  resize: vertical;
  font-size: 16px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

/* Buttons */
.btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  min-height: 36px; /* Reduced from 44px to be more compact */
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-sm {
  padding: 4px 12px;
  min-height: 30px;
  font-size: 13px;
}

.btn-primary {
  background: var(--primary-color);
  color: white;
}
.btn-primary:hover { background: var(--primary-dark); }
.btn-primary:disabled { background: var(--gray-400); cursor: not-allowed; }

.btn-secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}
.btn-secondary:hover { background: var(--border-color); }

.btn-outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
}
.btn-outline:hover { border-color: var(--primary-color); color: var(--primary-color); }

.btn-text {
  background: transparent;
  color: var(--text-secondary);
  padding: 4px 8px;
}
.btn-text:hover { color: var(--primary-color); }

.btn-icon {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  color: var(--text-tertiary);
  border-radius: 4px;
}
.btn-icon:hover { background: var(--bg-tertiary); color: var(--danger-color); }

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-tertiary);
}
.empty-state.large { padding: 80px; }
.empty-state .illustration { font-size: 48px; margin-bottom: 16px; }

/* Responsive Design */
@media (max-width: 768px) {
  .mobile-header {
    display: flex;
  }

  .language-learning-page {
    flex-direction: column;
    height: auto;
    overflow: visible;
  }

  .sidebar {
    position: fixed;
    top: 64px;
    left: 0;
    bottom: 0;
    width: 280px;
    z-index: 1000;
    transform: translateX(-100%);
    box-shadow: var(--shadow-lg);
  }

  .sidebar-mobile-open {
    transform: translateX(0);
  }

  .close-mobile-menu {
    display: block;
  }

  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .main-content {
    padding: 16px;
    margin-top: 0; /* 移除冗余的边距，由 AppLayout 处理 */
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 24px;
  }

  .two-column-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .words-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .word-card-item {
    padding: 12px;
  }

  .word-header h4 {
    font-size: 16px;
  }

  .review-grid {
    grid-template-columns: 1fr;
  }

  .word-checkboxes {
    grid-template-columns: 1fr;
  }

  .topics-grid {
    grid-template-columns: 1fr;
  }

  .article-display-area {
    padding: 16px;
  }

  .modal-card {
    margin: 16px;
    max-width: calc(100% - 32px);
  }

  .step-container {
    padding: 0 16px;
  }

  .form-group {
    margin-bottom: 16px;
  }

  .select-input {
    padding: 14px 16px;
    font-size: 16px;
  }

  .word-selection {
    max-height: 400px;
    padding: 12px;
  }

  .word-checkboxes {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .word-checkbox {
    padding: 12px;
  }

  .step-actions {
    flex-direction: column;
    padding: 16px;
  }

  .btn {
    min-height: 48px;
    font-size: 16px;
    padding: 12px 24px;
  }

  .pagination-container {
    margin-top: 16px;
    padding-top: 16px;
  }

  .pagination-info {
    font-size: 13px;
  }

  .pagination-buttons {
    gap: 8px;
  }

  .pagination-page {
    font-size: 13px;
    padding: 0 8px;
  }

  .btn-outline {
    min-height: 40px;
    font-size: 14px;
    padding: 8px 16px;
  }

  .btn-icon {
    min-height: 40px;
    min-width: 40px;
    font-size: 18px;
  }

  .input, .textarea {
    padding: 14px 16px;
    font-size: 16px;
  }

  .nav-item {
    padding: 16px;
    font-size: 16px;
  }

  .nav-item .icon {
    font-size: 20px;
  }

  .stat-card {
    padding: 20px;
  }

  .ai-article-paragraph-pair {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .ai-article-en {
    font-size: 16px;
  }

  .ai-article-zh {
    font-size: 14px;
    padding: 12px;
  }

  .ai-article-modal-title {
    font-size: 18px;
  }

  .modal-wide-header {
    padding: 12px 16px;
  }

  .ai-article-modal-meta {
    padding: 8px 16px;
  }

  .ai-article-modal-content {
    padding: 16px;
  }
}




@media (max-width: 1024px) and (min-width: 769px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .words-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}

/* Dark Mode Overrides */
:global(body.dark-mode) .stat-card.success .stat-icon { background-color: rgba(16, 185, 129, 0.2); color: #34d399; }
:global(body.dark-mode) .stat-card.warning .stat-icon { background-color: rgba(245, 158, 11, 0.2); color: #fbbf24; }
:global(body.dark-mode) .stat-card.info .stat-icon { background-color: rgba(59, 130, 246, 0.2); color: #60a5fa; }
:global(body.dark-mode) .status-tag.learning { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
:global(body.dark-mode) .status-tag.mastered { background: rgba(16, 185, 129, 0.2); color: #34d399; }
:global(body.dark-mode) .btn-secondary { background: var(--bg-secondary); border-color: var(--border-color); }
:global(body.dark-mode) .btn-secondary:hover { background: var(--bg-tertiary); }
:global(body.dark-mode) .input, :global(body.dark-mode) .textarea, :global(body.dark-mode) .select-input, :global(body.dark-mode) .select-sm {
  background-color: var(--bg-secondary);
  border-color: var(--border-color);
}
:global(body.dark-mode) .nav-item:hover { background-color: var(--bg-tertiary); }
:global(body.dark-mode) .list-item:hover { background-color: var(--bg-tertiary); }
:global(body.dark-mode) .word-checkbox:hover { background-color: var(--bg-tertiary); }
:global(body.dark-mode) .step-actions { background-color: var(--bg-secondary); }
:global(body.dark-mode) .article-display-area { background-color: var(--bg-secondary); }
:global(body.dark-mode) .vocabulary-list-append th { background-color: var(--bg-tertiary); }
</style>
