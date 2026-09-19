import request from './request'

/** 认证模块 */
export const authApi = {
  register: (data) => request.post('/api/auth/register', data),
  login: (data) => request.post('/api/auth/login', data),
  logout: () => request.post('/api/auth/logout'),
  me: () => request.get('/api/auth/me'),
  changePassword: (data) => request.put('/api/auth/password', data)
}

/** 用户模块 */
export const userApi = {
  profile: (id) => request.get(`/api/users/${id}`),
  updateProfile: (data) => request.put('/api/users/me', data),
  products: (id, params) => request.get(`/api/users/${id}/products`, { params })
}

/** 分类模块 */
export const categoryApi = {
  list: () => request.get('/api/categories')
}

/** 商品模块 */
export const productApi = {
  search: (params) => request.get('/api/products', { params }),
  mine: (params) => request.get('/api/products/mine', { params }),
  detail: (id) => request.get(`/api/products/${id}`),
  create: (data) => request.post('/api/products', data),
  update: (id, data) => request.put(`/api/products/${id}`, data),
  updateStatus: (id, status) => request.patch(`/api/products/${id}/status`, { status }),
  remove: (id) => request.delete(`/api/products/${id}`)
}

/** 文件上传 */
export const fileApi = {
  uploadImage: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/api/files/images', form)
  }
}

/** 收藏模块 */
export const favoriteApi = {
  add: (productId) => request.post(`/api/favorites/${productId}`),
  remove: (productId) => request.delete(`/api/favorites/${productId}`),
  list: (params) => request.get('/api/favorites', { params }),
  status: (productId) => request.get(`/api/favorites/${productId}/status`)
}

/** 订单模块 */
export const orderApi = {
  create: (data) => request.post('/api/orders', data),
  list: (params) => request.get('/api/orders', { params }),
  detail: (id) => request.get(`/api/orders/${id}`),
  confirm: (id) => request.post(`/api/orders/${id}/confirm`),
  finish: (id) => request.post(`/api/orders/${id}/finish`),
  cancel: (id, data) => request.post(`/api/orders/${id}/cancel`, data)
}

/** 消息模块 */
export const messageApi = {
  list: (params) => request.get('/api/messages', { params }),
  unreadCount: () => request.get('/api/messages/unread-count'),
  markRead: (id) => request.put(`/api/messages/${id}/read`),
  markAllRead: () => request.put('/api/messages/read-all')
}

/** 管理端模块 */
export const adminApi = {
  products: (params) => request.get('/api/admin/products', { params }),
  offShelf: (id) => request.put(`/api/admin/products/${id}/off-shelf`),
  users: (params) => request.get('/api/admin/users', { params }),
  updateUserStatus: (id, status) => request.put(`/api/admin/users/${id}/status`, { status }),
  cancelOrder: (id) => request.post(`/api/admin/orders/${id}/cancel`),
  createCategory: (data) => request.post('/api/admin/categories', data),
  updateCategory: (id, data) => request.put(`/api/admin/categories/${id}`, data),
  deleteCategory: (id) => request.delete(`/api/admin/categories/${id}`),
  timeoutScan: () => request.post('/api/admin/orders/timeout-scan')
}
