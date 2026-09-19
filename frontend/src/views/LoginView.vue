<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '买家 buyer01', username: 'buyer01' },
  { label: '卖家 seller01', username: 'seller01' },
  { label: '卖家 seller02', username: 'seller02' },
  { label: '管理员 admin', username: 'admin' }
]

function fillDemo(account) {
  form.username = account.username
  form.password = '123456'
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>欢迎回来</h2>
      <p class="muted">登录后即可发布闲置、收藏商品、下单交易</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>

      <div class="demo-box">
        <div class="demo-title">测试账号（密码均为 123456，点击可直接填入）</div>
        <div class="demo-list">
          <el-tag
            v-for="account in demoAccounts"
            :key="account.username"
            class="demo-tag"
            effect="plain"
            @click="fillDemo(account)"
          >{{ account.label }}</el-tag>
        </div>
      </div>

      <div class="auth-footer">
        还没有账号？
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 30px 30px 24px;
  box-shadow: 0 10px 30px rgba(31, 45, 61, 0.1);
}

.auth-card h2 {
  margin: 0 0 6px;
}

.auth-card p {
  margin: 0 0 20px;
}

.demo-box {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
}

.demo-title {
  font-size: 12px;
  color: #8c9399;
  margin-bottom: 8px;
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-tag {
  cursor: pointer;
}

.auth-footer {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: #8c9399;
}

.link {
  color: #1f9d76;
  font-weight: 600;
}
</style>
