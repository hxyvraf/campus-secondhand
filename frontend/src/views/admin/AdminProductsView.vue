<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'
import { money, productStatus } from '../../utils/format'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '在售', value: 'ON_SALE' },
  { label: '交易中', value: 'LOCKED' },
  { label: '已售出', value: 'SOLD' },
  { label: '已下架', value: 'OFF_SHELF' }
]

async function load() {
  loading.value = true
  try {
    const res = await adminApi.products({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      page: query.page,
      size: query.size
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function offShelf(row) {
  ElMessageBox.confirm(`确定强制下架商品「${row.title}」吗？下架后买家将无法购买。`, '强制下架', { type: 'warning' })
    .then(async () => {
      await adminApi.offShelf(row.id)
      ElMessage.success('已强制下架')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>商品管理（管理员）</h2>
      <div class="admin-nav">
        <el-button @click="router.push('/admin/users')">用户管理</el-button>
        <el-button @click="router.push('/admin/categories')">分类管理</el-button>
      </div>
    </div>

    <div class="card-panel">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="按标题/描述搜索"
          style="width: 230px"
          clearable
          @keyup.enter="() => { query.page = 1; load() }"
        />
        <el-select v-model="query.status" style="width: 150px" @change="() => { query.page = 1; load() }">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="() => { query.page = 1; load() }">查询</el-button>
        <span class="muted">共 {{ total }} 件商品</span>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <router-link :to="`/products/${row.id}`" class="link">{{ row.title }}</router-link>
            <div class="muted">卖家：{{ row.sellerNickname }} · 分类：{{ row.categoryName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="price"><span class="price-symbol">¥</span>{{ money(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="productStatus(row.status).type">{{ productStatus(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" prop="viewCount" width="90" />
        <el-table-column label="发布时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ON_SALE'"
              text
              type="danger"
              @click="offShelf(row)"
            >强制下架</el-button>
            <span v-else class="muted">不可下架</span>
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
