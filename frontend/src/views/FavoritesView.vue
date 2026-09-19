<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '../api'
import { money, productStatus } from '../utils/format'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const res = await favoriteApi.list({ page: query.page, size: query.size })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function remove(productId) {
  await favoriteApi.remove(productId)
  ElMessage.success('已取消收藏')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>我的收藏</h2>
      <span class="muted">共 {{ total }} 件</span>
    </div>

    <div class="card-panel">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <img v-if="row.coverImage" :src="row.coverImage" class="thumb" />
              <div v-else class="thumb thumb-empty">无图</div>
              <div>
                <router-link :to="`/products/${row.productId}`" class="link">{{ row.title }}</router-link>
                <div class="muted">卖家：{{ row.sellerNickname }} · 收藏于 {{ row.favoritedAt }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            <span class="price"><span class="price-symbol">¥</span>{{ money(row.price) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="productStatus(row.status).type">{{ productStatus(row.status).label }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="router.push(`/products/${row.productId}`)">去看看</el-button>
            <el-button text type="danger" @click="remove(row.productId)">取消收藏</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !list.length" class="empty-block">还没有收藏任何商品，去首页逛逛吧～</div>

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
