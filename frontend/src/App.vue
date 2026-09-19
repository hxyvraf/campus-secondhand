<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from './stores/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const keyword = ref('')
let timer = null

function doSearch() {
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(async () => {
  await userStore.fetchMe()
  // 消息未读数每 30 秒轮询一次（对应接口文档中的消息通知机制）
  timer = setInterval(() => userStore.refreshUnread(), 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

watch(() => route.query.keyword, (value) => {
  keyword.value = value || ''
})
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-inner">
        <router-link to="/" class="brand">
          <span class="brand-logo">🎒</span>
          <span class="brand-name">校园二手交易平台</span>
        </router-link>

        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索教材、数码、生活用品…"
            clearable
            @keyup.enter="doSearch"
          >
            <template #append>
              <el-button @click="doSearch">搜索</el-button>
            </template>
          </el-input>
        </div>

        <nav class="nav-links">
          <router-link to="/">首页</router-link>
          <router-link to="/publish">发布闲置</router-link>

          <template v-if="userStore.isLogin">
            <router-link to="/my-products">我的商品</router-link>
            <router-link to="/favorites">我的收藏</router-link>
            <router-link to="/orders">我的订单</router-link>
            <router-link to="/messages" class="message-link">
              消息
              <el-badge v-if="userStore.unreadCount > 0" :value="userStore.unreadCount" class="unread-badge" />
            </router-link>

            <el-dropdown v-if="userStore.isAdmin" class="admin-entry">
              <span class="admin-dropdown">管理后台 ▾</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/admin/products')">商品管理</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/admin/users')">用户管理</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/admin/categories')">分类管理</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <el-dropdown class="user-entry">
              <span class="user-dropdown">{{ userStore.nickname }} ▾</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/my-products')">我发布的商品</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <router-link to="/login">登录</router-link>
            <router-link to="/register" class="register-link">注册</router-link>
          </template>
        </nav>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="app-footer">
      <p>校园二手交易平台 · 个人练习项目（Vue 3 + Spring Boot + MySQL）· 后端接口文档见 /swagger-ui.html</p>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #fff;
  box-shadow: 0 1px 6px rgba(31, 45, 61, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1f9d76;
  white-space: nowrap;
}

.brand-logo {
  font-size: 22px;
}

.search-box {
  flex: 1;
  max-width: 420px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
  font-size: 14px;
}

.nav-links a {
  color: #46505a;
  padding: 4px 2px;
}

.nav-links a.router-link-active {
  color: #1f9d76;
  font-weight: 600;
}

.register-link {
  color: #1f9d76;
  font-weight: 600;
}

.message-link {
  position: relative;
}

.unread-badge {
  margin-left: 4px;
  vertical-align: middle;
}

.user-dropdown,
.admin-dropdown {
  cursor: pointer;
  color: #46505a;
  outline: none;
}

.app-footer {
  margin-top: auto;
  padding: 18px 16px 26px;
  text-align: center;
  color: #98a1a8;
  font-size: 13px;
}
</style>
