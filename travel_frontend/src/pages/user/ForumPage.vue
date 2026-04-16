<script setup lang="ts">
const inspirationTags = ['周末短途', '亲子友好', 'City Walk', '轻预算', '自然风光', '美食探索']

const featuredNotes = [
  {
    title: '杭州 2 天慢游模板',
    author: '旅刻编辑部',
    summary: '围绕西湖、灵隐和运河设计的轻松路线，适合第一次来杭州的用户快速获得灵感。',
    tags: ['西湖', '慢节奏', '拍照友好'],
  },
  {
    title: '苏州园林 + 平江路一日打卡',
    author: '城市漫游官',
    summary: '以公共交通为主，覆盖经典园林与老城街区，适合周末短途和轻度步行需求。',
    tags: ['周末游', '历史街区', '公共交通'],
  },
  {
    title: '成都三天美食行程清单',
    author: '美食路线组',
    summary: '从早茶、火锅到夜宵，一页看完成都高频美食节点，适合做 AI 行程输入参考。',
    tags: ['成都', '美食', '三日游'],
  },
]
</script>

<template>
  <div class="forum-page">
    <section class="page-header">
      <div class="header-copy">
        <p class="eyebrow">灵感广场</p>
        <h1>发现值得参考的旅行思路</h1>
        <p>
          这里先提供公开行程灵感与主题模版，帮助你快速组织需求，再进入 AI 行程规划。
        </p>
      </div>
      <div class="header-actions">
        <a-button type="primary" size="large" href="/workspace">去生成我的行程</a-button>
      </div>
    </section>

    <section class="toolbar-card">
      <a-card :bordered="false">
        <div class="toolbar-content">
          <div class="toolbar-block">
            <span class="toolbar-label">热门主题</span>
            <div class="tag-list">
              <a-tag v-for="tag in inspirationTags" :key="tag" color="blue">{{ tag }}</a-tag>
            </div>
          </div>
          <div class="toolbar-block toolbar-tip">
            <span class="toolbar-label">当前状态</span>
            <p>论坛详情接口暂未接入，当前页面用于承接导航与后续社区能力扩展。</p>
          </div>
        </div>
      </a-card>
    </section>

    <section class="content-grid">
      <a-card
        v-for="note in featuredNotes"
        :key="note.title"
        class="note-card"
        :bordered="false"
        hoverable
      >
        <template #title>
          <div class="card-title">
            <span>{{ note.title }}</span>
            <a-tag color="processing">精选</a-tag>
          </div>
        </template>

        <p class="card-author">作者：{{ note.author }}</p>
        <p class="card-summary">{{ note.summary }}</p>

        <div class="tag-list">
          <a-tag v-for="tag in note.tags" :key="tag">{{ tag }}</a-tag>
        </div>
      </a-card>
    </section>

    <section class="empty-section">
      <a-empty description="社区流内容建设中，后续可接入真实发布与浏览能力。">
        <a-button type="primary" href="/trips">查看我的行程</a-button>
      </a-empty>
    </section>
  </div>
</template>

<style scoped lang="scss">
.forum-page {
  max-width: 1320px;
  margin: 0 auto;
  padding: 32px 20px 72px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  padding: 28px 32px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
  box-shadow: var(--shadow-md);
}

.header-copy {
  max-width: 760px;

  h1 {
    margin: 0 0 12px;
    font-size: 34px;
    line-height: 1.2;
    color: var(--color-text);
  }

  p:last-child {
    margin: 0;
    font-size: 15px;
    color: var(--color-text-secondary);
  }
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-500);
}

.toolbar-card :deep(.ant-card-body) {
  padding: 24px 28px;
}

.toolbar-content {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 24px;
}

.toolbar-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.toolbar-tip p {
  margin: 0;
  color: var(--color-text-secondary);
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.note-card {
  border-radius: 18px;
  box-shadow: var(--shadow-sm);
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-author {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--color-subtle);
}

.card-summary {
  margin: 0 0 16px;
  color: var(--color-text-secondary);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-section {
  padding: 32px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(15, 28, 46, 0.06);
}

@media (max-width: 1080px) {
  .page-header {
    flex-direction: column;
  }

  .toolbar-content,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .forum-page {
    padding: 20px 16px 56px;
  }

  .page-header,
  .empty-section {
    padding: 20px;
  }

  .header-copy h1 {
    font-size: 28px;
  }
}
</style>
