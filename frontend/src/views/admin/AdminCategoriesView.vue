<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, categoryApi } from '../../api'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', sort: 99 })

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 20, message: '分类名称不能超过 20 字', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    const res = await categoryApi.list()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.sort = 99
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name
  form.sort = row.sort
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  if (editingId.value) {
    await adminApi.updateCategory(editingId.value, { name: form.name, sort: form.sort })
    ElMessage.success('分类修改成功')
  } else {
    await adminApi.createCategory({ name: form.name, sort: form.sort })
    ElMessage.success('分类新增成功')
  }
  dialogVisible.value = false
  load()
}

function remove(row) {
  ElMessageBox.confirm(`确定删除分类「${row.name}」吗？分类下还有商品时无法删除。`, '删除分类', { type: 'warning' })
    .then(async () => {
      await adminApi.deleteCategory(row.id)
      ElMessage.success('分类已删除')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>分类管理（管理员）</h2>
      <div class="admin-nav">
        <el-button @click="router.push('/admin/products')">商品管理</el-button>
        <el-button @click="router.push('/admin/users')">用户管理</el-button>
        <el-button type="primary" @click="openCreate">新增分类</el-button>
      </div>
    </div>

    <div class="card-panel">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column prop="sort" label="排序值" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
          <span class="muted" style="margin-left: 10px">越小越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-nav {
  display: flex;
  gap: 10px;
}
</style>
