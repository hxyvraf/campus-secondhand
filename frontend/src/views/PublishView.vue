<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi, fileApi, productApi } from '../api'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const categories = ref([])
const loading = ref(false)
const submitting = ref(false)
const fileList = ref([])

const productId = computed(() => route.query.id)
const isEdit = computed(() => Boolean(productId.value))

const form = reactive({
  title: '',
  description: '',
  price: null,
  originalPrice: null,
  categoryId: null,
  conditionLevel: '九成新',
  tradePlace: '',
  imageUrls: []
})

const rules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { max: 50, message: '标题不能超过 50 字', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        const num = Number(value)
        if (Number.isNaN(num) || num < 0.01 || num > 999999.99) callback(new Error('价格范围为 0.01 - 999999.99'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  conditionLevel: [{ required: true, message: '请选择成色', trigger: 'change' }]
}

const conditionOptions = ['全新', '九成新', '八成新', '七成新及以下']

async function loadCategories() {
  const res = await categoryApi.list()
  categories.value = res.data
}

async function loadProduct() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await productApi.detail(productId.value)
    const data = res.data
    Object.assign(form, {
      title: data.title,
      description: data.description,
      price: Number(data.price),
      originalPrice: data.originalPrice ? Number(data.originalPrice) : null,
      categoryId: data.categoryId,
      conditionLevel: data.conditionLevel,
      tradePlace: data.tradePlace,
      imageUrls: [...(data.images || [])]
    })
    fileList.value = (data.images || []).map((url, index) => ({ name: `图片${index + 1}`, url }))
  } finally {
    loading.value = false
  }
}

/** 自定义上传：直接调用后端上传接口，成功后把返回的 url 存进表单 */
async function customUpload(options) {
  try {
    const res = await fileApi.uploadImage(options.file)
    form.imageUrls.push(res.data.url)
    options.onSuccess(res.data)
  } catch (error) {
    options.onError(error)
  }
}

function handleRemove(file) {
  const url = file.url || file.response?.url
  form.imageUrls = form.imageUrls.filter((item) => item !== url)
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      description: form.description,
      price: form.price,
      originalPrice: form.originalPrice,
      categoryId: form.categoryId,
      conditionLevel: form.conditionLevel,
      tradePlace: form.tradePlace,
      imageUrls: form.imageUrls
    }
    if (isEdit.value) {
      await productApi.update(productId.value, payload)
      ElMessage.success('商品修改成功')
      router.push('/my-products')
    } else {
      const res = await productApi.create(payload)
      ElMessage.success('发布成功')
      router.push(`/products/${res.data.id}`)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  await loadProduct()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page-title">
      <h2>{{ isEdit ? '编辑商品' : '发布闲置' }}</h2>
      <span class="muted">填写越详细，越容易被同学看中</span>
    </div>

    <div class="card-panel">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="例如：九成新《软件测试技术》教材" />
        </el-form-item>

        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 220px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="成色" prop="conditionLevel">
          <el-radio-group v-model="form.conditionLevel">
            <el-radio v-for="item in conditionOptions" :key="item" :value="item">{{ item }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="售价" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :max="999999.99" :precision="2" :step="1" />
          <span class="muted" style="margin-left: 12px">单位：元，范围 0.01 - 999999.99</span>
        </el-form-item>

        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0.01" :max="999999.99" :precision="2" :step="1" />
          <span class="muted" style="margin-left: 12px">选填，用于展示折扣</span>
        </el-form-item>

        <el-form-item label="交易地点">
          <el-input v-model="form.tradePlace" maxlength="50" placeholder="例如：三号宿舍楼下 / 图书馆一楼" />
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="描述使用情况、瑕疵、配件等，最多 1000 字"
          />
        </el-form-item>

        <el-form-item label="商品图片">
          <el-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :http-request="customUpload"
            :on-remove="handleRemove"
            :limit="5"
            accept="image/png,image/jpeg"
          >
            <span class="upload-plus">+</span>
          </el-upload>
          <div class="muted">最多 5 张，支持 jpg/jpeg/png，单张不超过 5MB</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">
            {{ isEdit ? '保存修改' : '发布商品' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.upload-plus {
  font-size: 26px;
  color: #8c9399;
  line-height: 1;
}
</style>
