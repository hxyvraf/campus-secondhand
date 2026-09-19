<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { money, productStatus } from '../utils/format'

const props = defineProps({
  product: { type: Object, required: true },
  showFavorite: { type: Boolean, default: true }
})

const emit = defineEmits(['toggle-favorite'])
const router = useRouter()

const status = computed(() => productStatus(props.product.status))

function openDetail() {
  router.push(`/products/${props.product.id}`)
}

function onFavorite(event) {
  event.stopPropagation()
  emit('toggle-favorite', props.product)
}
</script>

<template>
  <div class="product-card" @click="openDetail">
    <div class="cover-wrap">
      <img v-if="product.coverImage" :src="product.coverImage" :alt="product.title" class="cover" />
      <div v-else class="cover cover-placeholder">暂无图片</div>

      <el-tag v-if="product.status !== 'ON_SALE'" :type="status.type" class="status-tag" size="small">
        {{ status.label }}
      </el-tag>
      <button v-if="showFavorite" class="fav-btn" :class="{ active: product.favorited }" @click="onFavorite">
        {{ product.favorited ? '已收藏' : '收藏' }}
      </button>
    </div>

    <div class="info">
      <h4 class="title" :title="product.title">{{ product.title }}</h4>
      <div class="price-row">
        <span class="price"><span class="price-symbol">¥</span>{{ money(product.price) }}</span>
        <span v-if="product.originalPrice" class="origin-price">¥{{ money(product.originalPrice) }}</span>
      </div>
      <div class="meta">
        <el-tag size="small" effect="plain">{{ product.conditionLevel }}</el-tag>
        <span class="muted">{{ product.categoryName }}</span>
      </div>
      <div class="meta second">
        <span class="muted">{{ product.sellerNickname }}</span>
        <span class="muted">{{ product.favoriteCount }} 人收藏</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.06);
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(31, 45, 61, 0.12);
}

.cover-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #f2f4f6;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a9b1b8;
  font-size: 13px;
}

.status-tag {
  position: absolute;
  left: 10px;
  top: 10px;
}

.fav-btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
  border: none;
  border-radius: 14px;
  padding: 4px 12px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #59636b;
  cursor: pointer;
}

.fav-btn.active {
  background: #1f9d76;
  color: #fff;
}

.info {
  padding: 12px 14px 16px;
}

.title {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.4;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-row {
  display: flex;
  align-items: baseline;
}

.price {
  font-size: 19px;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
}

.meta.second {
  margin-top: 4px;
}
</style>
