<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi } from '../api'
import { money, productStatus } from '../utils/format'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ status: '', page: 1, size: 10 })

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
    const res = await productApi.mine({
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

function changeStatus(product, status) {
  const label = status === 'OFF_SHELF' ? '下架' : '重新上架'
  ElMessageBox.confirm(`确定要${label}商品「${product.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await productApi.updateStatus(product.id, status)
      ElMessage.success(`${label}成功`)
      load()
    })
    .catch(() => {})
}

function removeProduct(product) {
  ElMessageBox.confirm(`删除后商品将不再展示，确定删除「${product.title}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await productApi.remove(product.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>我发布的商品</h2>
      <el-button type="primary" @click="router.push('/publish')">发布新闲置</el-button>
    </div>

    <div class="card-panel">
      <div class="toolbar">
        <el-select v-model="query.status" style="width: 150px" @change="() => { query.page = 1; load() }">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <span class="muted">共 {{ total }} 件</span>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <img v-if="row.coverImage" :src="row.coverImage" class="thumb" />
              <div v-else class="thumb thumb-empty">无图</div>
              <div>
                <router-link :to="`/products/${row.id}`" class="link">{{ row.title }}</router-link>
                <div class="muted">发布于 {{ row.createdAt }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="110">
          <template #default="{ row }">
            <span class="price"><span class="price-symbol">¥</span>{{ money(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110" prop="categoryName" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="productStatus(row.status).type">{{ productStatus(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览/收藏" width="110">
          <template #default="{ row }">{{ row.viewCount }} / {{ row.favoriteCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ON_SALE' || row.status === 'OFF_SHELF'"
              text
              type="primary"
              @click="router.push({ path: '/publish', query: { id: row.id } })"
            >编辑</el-button>
            <el-button
              v-if="row.status === 'ON_SALE'"
              text
              type="warning"
              @click="changeStatus(row, 'OFF_SHELF')"
            >下架</el-button>
            <el-button
              v-if="row.status === 'OFF_SHELF'"
              text
              type="success"
              @click="changeStatus(row, 'ON_SALE')"
            >上架</el-button>
            <el-button text type="danger" @click="removeProduct(row)">删除</el-button>
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
.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 56px;
  height: 42px;
  object-fit: cover;
  border-radius: 6px;
}

.thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f4f6;
  color: #a9b1b8;
  font-size: 12px;
}

.link {
  color: #2c3235;
  font-weight: 500;
}

.link:hover {
  color: #1f9d76;
}
</style>
