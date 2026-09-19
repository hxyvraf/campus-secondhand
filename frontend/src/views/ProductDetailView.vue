<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteApi, orderApi, productApi } from '../api'
import { useUserStore } from '../stores/user'
import { money, productStatus } from '../utils/format'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null)
const sellerProducts = ref([])
const loading = ref(true)
const orderDialog = ref(false)
const submitting = ref(false)
const orderForm = reactive({ remark: '' })

const status = computed(() => productStatus(product.value?.status))
const isOwner = computed(() => product.value && userStore.user && product.value.sellerId === userStore.user.id)
const canBuy = computed(() => product.value && product.value.status === 'ON_SALE' && !isOwner.value)

async function load() {
  loading.value = true
  try {
    const res = await productApi.detail(route.params.id)
    product.value = res.data
    // 展示卖家其他在售商品（从在售列表中筛选，最多 4 件）
    const other = await productApi.search({ page: 1, size: 50 })
    sellerProducts.value = other.data.records
      .filter((item) => item.sellerId === product.value.sellerId && item.id !== product.value.id)
      .slice(0, 4)
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (product.value.favorited) {
    await favoriteApi.remove(product.value.id)
    product.value.favorited = false
    product.value.favoriteCount = Math.max(0, product.value.favoriteCount - 1)
    ElMessage.success('已取消收藏')
  } else {
    await favoriteApi.add(product.value.id)
    product.value.favorited = true
    product.value.favoriteCount += 1
    ElMessage.success('收藏成功')
  }
}

function openOrderDialog() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再下单')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  orderForm.remark = ''
  orderDialog.value = true
}

async function submitOrder() {
  submitting.value = true
  try {
    const res = await orderApi.create({ productId: product.value.id, remark: orderForm.remark })
    orderDialog.value = false
    ElMessage.success('下单成功，等待卖家确认')
    product.value.status = 'LOCKED'
    product.value.statusLabel = '交易中'
    router.push({ path: '/orders', query: { orderId: res.data.id } })
  } finally {
    submitting.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <div class="page" v-loading="loading">
    <div v-if="product" class="detail-card">
      <div class="gallery">
        <el-carousel v-if="product.images && product.images.length" height="340px" :autoplay="false" indicator-position="outside">
          <el-carousel-item v-for="(img, index) in product.images" :key="index">
            <img :src="img" class="gallery-img" :alt="product.title" />
          </el-carousel-item>
        </el-carousel>
        <div v-else class="gallery-empty">暂无图片</div>
      </div>

      <div class="summary">
        <div class="title-row">
          <h1>{{ product.title }}</h1>
          <el-tag :type="status.type" effect="dark">{{ status.label }}</el-tag>
        </div>

        <div class="price-block">
          <span class="price big"><span class="price-symbol">¥</span>{{ money(product.price) }}</span>
          <span v-if="product.originalPrice" class="origin-price">原价 ¥{{ money(product.originalPrice) }}</span>
        </div>

        <div class="attrs">
          <div class="attr"><span class="label">成色</span><span>{{ product.conditionLevel }}</span></div>
          <div class="attr"><span class="label">分类</span><span>{{ product.categoryName }}</span></div>
          <div class="attr"><span class="label">交易地点</span><span>{{ product.tradePlace || '面议' }}</span></div>
          <div class="attr"><span class="label">浏览量</span><span>{{ product.viewCount }}</span></div>
          <div class="attr"><span class="label">收藏数</span><span>{{ product.favoriteCount }}</span></div>
          <div class="attr"><span class="label">发布时间</span><span>{{ product.createdAt }}</span></div>
        </div>

        <div class="seller-box">
          <div class="seller-info">
            <span class="avatar">{{ (product.sellerNickname || '?').slice(0, 1) }}</span>
            <div>
              <div class="seller-name">{{ product.sellerNickname }}</div>
              <div class="muted">卖家主页可查看 TA 的其他闲置</div>
            </div>
          </div>
          <el-button text type="primary" @click="router.push(`/users/${product.sellerId}`)">进入卖家主页</el-button>
        </div>

        <div class="actions">
          <el-button
            size="large"
            :type="product.favorited ? 'warning' : 'default'"
            @click="toggleFavorite"
          >{{ product.favorited ? '取消收藏' : '收藏商品' }}</el-button>
          <el-button
            v-if="!isOwner"
            size="large"
            type="primary"
            :disabled="!canBuy"
            @click="openOrderDialog"
          >{{ canBuy ? '立即购买' : (product.status === 'LOCKED' ? '商品交易中' : '商品不可购买') }}</el-button>
          <el-button
            v-else
            size="large"
            @click="router.push({ path: '/publish', query: { id: product.id } })"
          >编辑我的商品</el-button>
        </div>
      </div>
    </div>

    <div v-if="product" class="card-panel desc-panel">
      <h3>商品描述</h3>
      <p class="desc">{{ product.description || '卖家没有填写详细描述。' }}</p>
    </div>

    <div v-if="sellerProducts.length" class="card-panel">
      <h3>TA 的其他闲置</h3>
      <div class="product-grid">
        <ProductCard v-for="item in sellerProducts" :key="item.id" :product="item" :show-favorite="false" />
      </div>
    </div>

    <el-dialog v-model="orderDialog" title="确认下单" width="440px">
      <div class="order-tip">
        <div class="order-product">{{ product?.title }}</div>
        <div class="price"><span class="price-symbol">¥</span>{{ money(product?.price) }}</div>
      </div>
      <el-form label-position="top">
        <el-form-item label="给卖家留言（可选）">
          <el-input
            v-model="orderForm.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：今晚 7 点在三号宿舍楼下交易可以吗？"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialog = false">再想想</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-card {
  display: grid;
  grid-template-columns: 460px 1fr;
  gap: 26px;
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  margin-bottom: 18px;
}

.gallery-img {
  width: 100%;
  height: 340px;
  object-fit: cover;
  border-radius: 10px;
}

.gallery-empty {
  height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f4f6;
  border-radius: 10px;
  color: #a9b1b8;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-row h1 {
  margin: 0;
  font-size: 22px;
  line-height: 1.4;
}

.price-block {
  margin: 14px 0 18px;
  padding: 14px 16px;
  background: #fff7f4;
  border-radius: 10px;
  display: flex;
  align-items: baseline;
}

.price.big {
  font-size: 28px;
}

.attrs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 20px;
  font-size: 14px;
  margin-bottom: 18px;
}

.attr .label {
  color: #8c9399;
  display: inline-block;
  width: 72px;
}

.seller-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #eef0f2;
  border-radius: 10px;
  margin-bottom: 18px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #1f9d76;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.seller-name {
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 12px;
}

.desc-panel h3,
.card-panel h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.desc {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
  color: #4a545c;
}

.order-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f7f9fa;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 14px;
}

.order-product {
  font-size: 14px;
}

:deep(.el-carousel__item) {
  border-radius: 10px;
  overflow: hidden;
}
</style>
