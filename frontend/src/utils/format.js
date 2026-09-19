/** 价格格式化：18 -> 18.00 */
export function money(value) {
  if (value === null || value === undefined || value === '') return '0.00'
  return Number(value).toFixed(2)
}

/** 商品状态中文名与标签颜色 */
const PRODUCT_STATUS = {
  ON_SALE: { label: '在售', type: 'success' },
  LOCKED: { label: '交易中', type: 'warning' },
  SOLD: { label: '已售出', type: 'info' },
  OFF_SHELF: { label: '已下架', type: 'info' }
}

export function productStatus(status) {
  return PRODUCT_STATUS[status] || { label: status || '未知', type: 'info' }
}

/** 订单状态中文名与标签颜色 */
const ORDER_STATUS = {
  PENDING_CONFIRM: { label: '待卖家确认', type: 'warning' },
  CONFIRMED: { label: '交易中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELED: { label: '已取消', type: 'info' },
  TIMEOUT: { label: '超时关闭', type: 'danger' }
}

export function orderStatus(status) {
  return ORDER_STATUS[status] || { label: status || '未知', type: 'info' }
}

/** 消息类型中文名 */
export function messageType(type) {
  const map = { ORDER: '订单消息', FAVORITE: '收藏消息', SYSTEM: '系统消息' }
  return map[type] || type
}

/** 图片地址：没有图片时返回空串，由页面显示占位块 */
export function imageUrl(url) {
  return url || ''
}
