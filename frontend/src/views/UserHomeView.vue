<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { userApi } from '../api'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const profile = ref(null)
const products = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const userRes = await userApi.profile(route.params.id)
    profile.value = userRes.data
    const productRes = await userApi.products(route.params.id, { page: page.value, size: 12 })
    products.value = productRes.data.records
    total.value = productRes.data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, () => {
  page.value = 1
  load()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div v-if="profile" class="profile-card">
      <span class="avatar">{{ (profile.nickname || '?').slice(0, 1) }}</span>
      <div class="info">
        <h2>{{ profile.nickname }}</h2>
        <p class="muted">
          @{{ profile.username }}
          <template v-if="profile.school"> · {{ profile.school }}</template>
          <template v-if="profile.phone"> · {{ profile.phone }}</template>
        </p>
        <p class="muted">注册时间：{{ profile.createdAt }}</p>
      </div>
      <div class="count">
        <strong>{{ profile.onSaleCount }}</strong>
        <span>件在售闲置</span>
      </div>
    </div>

    <div class="page-title"><h2>TA 的在售商品</h2></div>

    <div v-if="products.length" class="product-grid">
      <ProductCard v-for="item in products" :key="item.id" :product="item" :show-favorite="false" />
    </div>
    <div v-else class="empty-block">TA 暂时没有在售的闲置</div>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        :page-size="12"
        :total="total"
        layout="prev, pager, next"
        @current-change="load"
      />
    </div>
  </div>
</template>

<style scoped>
.profile-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  margin-bottom: 22px;
}

.avatar {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: #1f9d76;
  color: #fff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info h2 {
  margin: 0 0 6px;
}

.info p {
  margin: 2px 0;
}

.count {
  margin-left: auto;
  text-align: center;
  padding-left: 24px;
  border-left: 1px solid #eef0f2;
}

.count strong {
  display: block;
  font-size: 24px;
  color: #1f9d76;
}

.count span {
  font-size: 12px;
  color: #8c9399;
}
</style>
