<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const res = await adminApi.users({
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  const action = target === 0 ? '禁用' : '启用'
  ElMessageBox.confirm(`确定要${action}用户「${row.nickname}（${row.username}）」吗？`, `${action}用户`, { type: 'warning' })
    .then(async () => {
      await adminApi.updateUserStatus(row.id, target)
      ElMessage.success(`已${action}`)
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>用户管理（管理员）</h2>
      <div class="admin-nav">
        <el-button @click="router.push('/admin/products')">商品管理</el-button>
        <el-button @click="router.push('/admin/categories')">分类管理</el-button>
      </div>
    </div>

    <div class="card-panel">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="按账号/昵称搜索"
          style="width: 230px"
          clearable
          @keyup.enter="() => { query.page = 1; load() }"
        />
        <el-button type="primary" @click="() => { query.page = 1; load() }">查询</el-button>
        <span class="muted">共 {{ total }} 个用户</span>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/users/${row.id}`" class="link">{{ row.nickname }}</router-link>
            <div class="muted">@{{ row.username }}<template v-if="row.school"> · {{ row.school }}</template></div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="200">
          <template #default="{ row }">
            <div>{{ row.phone || '—' }}</div>
            <div class="muted">{{ row.email || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" effect="plain">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '已禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在售商品" prop="onSaleCount" width="100" />
        <el-table-column label="注册时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 'ADMIN'"
              text
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

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
.link {
  color: #2c3235;
  font-weight: 500;
}

.link:hover {
  color: #1f9d76;
}

.admin-nav {
  display: flex;
  gap: 10px;
}
</style>
