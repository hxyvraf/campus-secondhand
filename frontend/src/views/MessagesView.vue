<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { messageApi } from '../api'
import { useUserStore } from '../stores/user'
import { messageType } from '../utils/format'

const userStore = useUserStore()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ isRead: null, page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const res = await messageApi.list({
      isRead: query.isRead === null ? undefined : query.isRead,
      page: query.page,
      size: query.size
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function markRead(row) {
  if (row.isRead === 1) return
  await messageApi.markRead(row.id)
  row.isRead = 1
  userStore.refreshUnread()
}

async function markAllRead() {
  const res = await messageApi.markAllRead()
  ElMessage.success(`已把 ${res.data.updated} 条消息标记为已读`)
  userStore.refreshUnread()
  load()
}

function filterUnread() {
  query.isRead = query.isRead === 0 ? null : 0
  query.page = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>消息中心</h2>
      <div class="actions">
        <el-button :type="query.isRead === 0 ? 'primary' : 'default'" @click="filterUnread">只看未读</el-button>
        <el-button type="primary" plain @click="markAllRead">全部标记已读</el-button>
      </div>
    </div>

    <div class="card-panel" v-loading="loading">
      <ul class="message-list">
        <li v-for="row in list" :key="row.id" class="message-item" :class="{ unread: row.isRead === 0 }" @click="markRead(row)">
          <div class="message-head">
            <el-tag size="small" effect="plain">{{ messageType(row.type) }}</el-tag>
            <span class="title">{{ row.title }}</span>
            <span v-if="row.isRead === 0" class="dot">未读</span>
            <span class="time muted">{{ row.createdAt }}</span>
          </div>
          <p class="content">{{ row.content }}</p>
        </li>
      </ul>

      <div v-if="!loading && !list.length" class="empty-block">暂时没有消息</div>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.actions {
  display: flex;
  gap: 10px;
}

.message-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.message-item {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f2f4;
  cursor: pointer;
  transition: background 0.15s ease;
}

.message-item:hover {
  background: #fafbfc;
}

.message-item.unread {
  background: #f2fbf7;
}

.message-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  font-weight: 600;
}

.dot {
  font-size: 12px;
  color: #fff;
  background: #ef5b3c;
  border-radius: 10px;
  padding: 1px 8px;
}

.time {
  margin-left: auto;
}

.content {
  margin: 8px 0 0;
  color: #5a646c;
  line-height: 1.7;
}
</style>
