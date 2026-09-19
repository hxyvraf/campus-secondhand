<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, orderApi } from '../api'
import { useUserStore } from '../stores/user'
import { money, orderStatus } from '../utils/format'

const userStore = useUserStore()
const route = useRoute()

const role = ref('buyer')
const status = ref('')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const cancelDialog = ref(false)
const cancelTarget = ref(null)
const cancelReason = ref('')

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待卖家确认', value: 'PENDING_CONFIRM' },
  { label: '交易中', value: 'CONFIRMED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELED' },
  { label: '超时关闭', value: 'TIMEOUT' }
]

async function load() {
  loading.value = true
  try {
    const res = await orderApi.list({
      role: role.value,
      status: status.value || undefined,
      page: query.page,
      size: query.size
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function switchRole(value) {
  role.value = value
  query.page = 1
  load()
}

function canCancel(row) {
  return row.status === 'PENDING_CONFIRM' || row.status === 'CONFIRMED'
}

function openCancelDialog(row) {
  cancelTarget.value = row
  cancelReason.value = ''
  cancelDialog.value = true
}

async function submitCancel() {
  await orderApi.cancel(cancelTarget.value.id, { reason: cancelReason.value })
  ElMessage.success('订单已取消，商品已重新上架')
  cancelDialog.value = false
  load()
}

function confirmOrder(row) {
  ElMessageBox.confirm(`确认商品「${row.productTitle}」已与买家达成交易？`, '卖家确认订单', { type: 'warning' })
    .then(async () => {
      await orderApi.confirm(row.id)
      ElMessage.success('已确认订单')
      load()
    })
    .catch(() => {})
}

function finishOrder(row) {
  ElMessageBox.confirm(`确认已收到商品「${row.productTitle}」？确认后订单完成，商品将标记为已售出。`, '买家确认完成', { type: 'warning' })
    .then(async () => {
      await orderApi.finish(row.id)
      ElMessage.success('交易已完成')
      load()
    })
    .catch(() => {})
}

async function triggerTimeoutScan() {
  const res = await adminApi.timeoutScan()
  ElMessage.success(`超时扫描完成，本次关闭 ${res.data.closedCount} 个订单`)
  load()
}

onMounted(() => {
  if (route.query.role === 'seller') role.value = 'seller'
  load()
})
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>我的订单</h2>
      <div>
        <el-button v-if="userStore.isAdmin" @click="triggerTimeoutScan">触发订单超时扫描（管理员）</el-button>
      </div>
    </div>

    <div class="card-panel">
      <el-tabs :model-value="role" @tab-change="switchRole">
        <el-tab-pane label="我买到的" name="buyer" />
        <el-tab-pane label="我卖出的" name="seller" />
      </el-tabs>

      <div class="toolbar">
        <el-select v-model="status" style="width: 160px" @change="() => { query.page = 1; load() }">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <span class="muted">共 {{ total }} 笔订单</span>
      </div>

      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column label="订单信息" min-width="280">
          <template #default="{ row }">
            <div class="order-cell">
              <img v-if="row.coverImage" :src="row.coverImage" class="thumb" />
              <div v-else class="thumb thumb-empty">无图</div>
              <div>
                <router-link :to="`/products/${row.productId}`" class="link">{{ row.productTitle }}</router-link>
                <div class="muted">订单号：{{ row.orderNo }}</div>
                <div class="muted">下单时间：{{ row.createdAt }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="110">
          <template #default="{ row }">
            <span class="price"><span class="price-symbol">¥</span>{{ money(row.amount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="对方" width="140">
          <template #default="{ row }">
            {{ role === 'buyer' ? row.sellerNickname : row.buyerNickname }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="orderStatus(row.status).type">{{ orderStatus(row.status).label }}</el-tag>
            <div v-if="row.status === 'PENDING_CONFIRM'" class="muted small">超时时间 {{ row.expireAt }}</div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="role === 'seller' && row.status === 'PENDING_CONFIRM'"
              text
              type="primary"
              @click="confirmOrder(row)"
            >确认订单</el-button>
            <el-button
              v-if="role === 'buyer' && row.status === 'CONFIRMED'"
              text
              type="success"
              @click="finishOrder(row)"
            >确认完成</el-button>
            <el-button v-if="canCancel(row)" text type="warning" @click="openCancelDialog(row)">取消订单</el-button>
            <span v-if="!canCancel(row) && !(role === 'seller' && row.status === 'PENDING_CONFIRM') && !(role === 'buyer' && row.status === 'CONFIRMED')" class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !list.length" class="empty-block">暂时没有订单</div>

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

    <el-dialog v-model="cancelDialog" title="取消订单" width="420px">
      <p class="muted">取消后商品会自动重新上架，其他同学可以继续购买。</p>
      <el-input
        v-model="cancelReason"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="请填写取消原因（可选）"
      />
      <template #footer>
        <el-button @click="cancelDialog = false">再想想</el-button>
        <el-button type="primary" @click="submitCancel">确认取消订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-cell {
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

.small {
  font-size: 12px;
}
</style>
