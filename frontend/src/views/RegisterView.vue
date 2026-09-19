<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: '',
  school: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '用户名为 4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      phone: form.phone,
      email: form.email,
      school: form.school
    })
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { username: form.username } })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册新账号</h2>
      <p class="muted">注册后就能发布闲置、收藏商品、下单交易</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4-20 位字母、数字或下划线" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="展示给其他同学看的名字" clearable />
        </el-form-item>
        <el-form-item label="手机号（可选）" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" clearable />
        </el-form-item>
        <el-form-item label="邮箱（可选）" prop="email">
          <el-input v-model="form.email" placeholder="用于接收通知" clearable />
        </el-form-item>
        <el-form-item label="学校（可选）" prop="school">
          <el-input v-model="form.school" placeholder="例如：南昌职业大学" clearable />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          注册
        </el-button>
      </el-form>

      <div class="auth-footer">
        已经有账号了？
        <router-link to="/login" class="link">去登录</router-link>
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
  width: 460px;
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
