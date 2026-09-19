<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi, favoriteApi, productApi } from '../api'
import { useUserStore } from '../stores/user'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const categories = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  keyword: '',
  categoryId: null,
  minPrice: null,
  maxPrice: null,
  sort: 'latest',
  page: 1,
  size: 12
})

const priceRange = ref([])

const currentCategoryName = computed(() => {
  const hit = categories.value.find((item) => item.id === query.categoryId)
  return hit ? hit.name : '全部商品'
})

async function loadCategories() {
  const res = await categoryApi.list()
  categories.value = res.data
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await productApi.search({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      minPrice: query.minPrice ?? undefined,
      maxPrice: query.maxPrice ?? undefined,
      sort: query.sort,
      page: query.page,
      size: query.size
    })
    products.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  query.page = 1
  loadProducts()
}

function pickCategory(id) {
  query.categoryId = query.categoryId === id ? null : id
  applyFilters()
}

function resetFilters() {
  query.categoryId = null
  query.minPrice = null
  query.maxPrice = null
  query.sort = 'latest'
  priceRange.value = []
  applyFilters()
}

/** 收藏 / 取消收藏：未登录先去登录页 */
async function toggleFavorite(product) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再收藏商品')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (product.favorited) {
    await favoriteApi.remove(product.id)
    product.favorited = false
    product.favoriteCount = Math.max(0, product.favoriteCount - 1)
    ElMessage.success('已取消收藏')
  } else {
    await favoriteApi.add(product.id)
    product.favorited = true
    product.favoriteCount += 1
    ElMessage.success('收藏成功')
  }
}

function handlePriceRangeChange(value) {
  if (value && value.length === 2) {
    query.minPrice = value[0]
    query.maxPrice = value[1]
  } else {
    query.minPrice = null
    query.maxPrice = null
  }
  applyFilters()
}

onMounted(async () => {
  await loadCategories()
  await loadProducts()
})

watch(() => route.query.keyword, (value) => {
  query.keyword = value || ''
  query.page = 1
  loadProducts()
})

watch(() => route.query, () => {
  if (route.path === '/' && route.query.keyword !== query.keyword) {
    query.keyword = route.query.keyword || ''
    loadProducts()
  }
}, { deep: true })
</script>

<template>
  <div class="page">
    <section class="hero">
      <div class="hero-text">
        <h1>把闲置，交给需要的同学</h1>
        <p>教材、数码、生活用品……同校交易，当面验货，省心又省钱</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/publish')">发布闲置</el-button>
          <el-button size="large" @click="router.push('/orders')">我的订单</el-button>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat">
          <strong>{{ total }}</strong>
          <span>当前在售商品</span>
        </div>
        <div class="stat">
          <strong>{{ categories.length }}</strong>
          <span>商品分类</span>
        </div>
      </div>
    </section>

    <div class="toolbar">
      <div class="category-chips">
        <span
          class="chip"
          :class="{ active: query.categoryId === null }"
          @click="pickCategory(null)"
        >全部</span>
        <span
          v-for="item in categories"
          :key="item.id"
          class="chip"
          :class="{ active: query.categoryId === item.id }"
          @click="pickCategory(item.id)"
        >{{ item.name }}</span>
      </div>

      <div class="filter-right">
        <el-select v-model="query.sort" style="width: 130px" @change="applyFilters">
          <el-option label="最新发布" value="latest" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="收藏最多" value="hot" />
        </el-select>
        <el-slider
          v-model="priceRange"
          range
          :min="0"
          :max="500"
          :step="10"
          style="width: 200px"
          @change="handlePriceRangeChange"
        />
        <el-button text @click="resetFilters">重置</el-button>
      </div>
    </div>

    <div class="result-bar">
      <span class="muted">
        {{ currentCategoryName }}
        <template v-if="query.keyword">· 关键词「{{ query.keyword }}」</template>
        · 共 {{ total }} 件
      </span>
    </div>

    <div v-loading="loading">
      <div v-if="products.length" class="product-grid">
        <ProductCard
          v-for="item in products"
          :key="item.id"
          :product="item"
          @toggle-favorite="toggleFavorite"
        />
      </div>
      <div v-else class="empty-block">没有找到符合条件的商品，换个筛选条件试试～</div>
    </div>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next, jumper, total"
        @current-change="loadProducts"
      />
    </div>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  background: linear-gradient(120deg, #1f9d76 0%, #38b48b 55%, #7fd0b3 100%);
  color: #fff;
  border-radius: 16px;
  padding: 30px 34px;
  margin-bottom: 20px;
}

.hero-text h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

.hero-text p {
  margin: 0 0 18px;
  opacity: 0.92;
  font-size: 14px;
}

.hero-actions :deep(.el-button) {
  border: none;
}

.hero-stats {
  display: flex;
  gap: 30px;
  text-align: center;
}

.stat strong {
  display: block;
  font-size: 30px;
}

.stat span {
  font-size: 13px;
  opacity: 0.9;
}

.category-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  padding: 6px 14px;
  border-radius: 16px;
  background: #fff;
  font-size: 13px;
  color: #59636b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip.active,
.chip:hover {
  background: #1f9d76;
  color: #fff;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
}

.result-bar {
  margin-bottom: 12px;
}
</style>
