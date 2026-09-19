<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi, userApi } from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()
const saving = ref(false)
const passwordSaving = ref(false)
const profileRef = ref()
const passwordRef = ref()

const profile = reactive({
  nickname: '',
  phone: '',
  email: '',
  school: '',
  avatar: ''
})

const password = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== password.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function load() {
  const res = await authApi.me()
  Object.assign(profile, {
    nickname: res.data.nickname || '',
    phone: res.data.phone || '',
    email: res.data.email || '',
    school: res.data.school || '',
    avatar: res.data.avatar || ''
  })
}

async function saveProfile() {
  saving.value = true
  try {
    const res = await userApi.updateProfile({ ...profile })
    ElMessage.success('资料已更新')
    userStore.user = res.data
    localStorage.setItem('campus_user', JSON.stringify(res.data))
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  await passwordRef.value.validate()
  passwordSaving.value = true
  try {
    await authApi.changePassword({
      oldPassword: password.oldPassword,
      newPassword: password.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } finally {
    passwordSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-title"><h2>个人中心</h2></div>

    <div class="profile-grid">
      <div class="card-panel">
        <h3>基本资料</h3>
        <el-form ref="profileRef" :model="profile" label-width="90px">
          <el-form-item label="昵称">
            <el-input v-model="profile.nickname" maxlength="20" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profile.phone" placeholder="11 位手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profile.email" />
          </el-form-item>
          <el-form-item label="学校">
            <el-input v-model="profile.school" />
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="profile.avatar" placeholder="可先调上传接口拿到图片地址" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card-panel">
        <h3>修改密码</h3>
        <el-form ref="passwordRef" :model="password" :rules="passwordRules" label-width="90px">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="password.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="password.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="password.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordSaving" @click="savePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
        <el-alert
          title="提示：修改密码后需要重新登录，其他页面上的旧登录状态会失效。"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.card-panel h3 {
  margin: 0 0 18px;
  font-size: 16px;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
