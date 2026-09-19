import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

// 请求拦截：自动带上 JWT
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('campus_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 响应拦截：后端统一返回 {code, message, data}
 * - code = 200 时直接把 data 交给业务代码
 * - 其他情况抛出错误并提示 message（页面可自行 catch 做额外处理）
 */
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return body
  },
  (error) => {
    const response = error.response
    const body = response ? response.data : null
    const message = (body && body.message) || error.message || '网络异常，请稍后重试'

    if (response && response.status === 401) {
      localStorage.removeItem('campus_token')
      localStorage.removeItem('campus_user')
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.warning('登录已过期，请重新登录')
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(body || error)
  }
)

export default request
