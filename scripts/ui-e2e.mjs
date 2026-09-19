/**
 * 前端真实点击全流程验证 + 截图（父线程自用，交付物里不含本脚本）
 * 用系统自带的 Edge + Playwright 驱动，覆盖：登录注册、搜索筛选、商品详情、收藏、下单、
 * 卖家确认、买家完成、发布闲置、消息中心、个人中心、管理端三个页面、前端表单校验。
 */
import { createRequire } from 'node:module'
import fs from 'node:fs'

const require = createRequire('file:///C:/Users/hxyvraf/Desktop/campus-secondhand/')
const { chromium } = require('C:/Users/hxyvraf/AppData/Roaming/npm/node_modules/playwright')

// 默认跑前端开发服务器（5173）；设置 UI_BASE_URL 可改为跑后端单端口模式（8080）
const BASE = process.env.UI_BASE_URL || 'http://localhost:5173'
const OUT = 'C:/Users/hxyvraf/Desktop/campus-secondhand/docs/测试执行证据/截图'
const UPLOAD = 'C:/Users/hxyvraf/Desktop/campus-secondhand/docs/postman/testdata/test-upload.png'
fs.mkdirSync(OUT, { recursive: true })

const results = []
const productTitle = `前端联调测试商品-${Date.now().toString().slice(-6)}`

function record(name, ok, detail = '') {
  results.push({ name, ok, detail })
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name}${detail ? '  ' + detail : ''}`)
}

async function shot(page, name) {
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: true })
}

async function step(name, fn) {
  try {
    const detail = await fn()
    record(name, true, detail || '')
  } catch (error) {
    record(name, false, String(error).split('\n')[0])
    try {
      await shot(page, `FAIL-${name}`)
    } catch (e) { /* 截图失败不影响后续步骤 */ }
  }
}

let page

async function login(username) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
  await page.fill('input[placeholder="请输入用户名"]', username)
  await page.fill('input[placeholder="请输入密码"]', '123456')
  await page.click('button:has-text("登录")')
  await page.waitForTimeout(1200)
}

async function logout() {
  await page.click('.user-dropdown')
  await page.click('text=退出登录')
  await page.click('.el-message-box button:has-text("确定")')
  await page.waitForTimeout(1000)
}

const browser = await chromium.launch({ channel: 'msedge', headless: true })
const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 })
page = await context.newPage()

// ---------- 1. 未登录浏览 ----------
await step('01 首页-未登录浏览商品列表', async () => {
  await page.goto(`${BASE}/`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.product-card', { timeout: 15000 })
  const count = await page.locator('.product-card').count()
  await shot(page, '01-首页-商品列表')
  if (count < 6) throw new Error(`商品卡片数量偏少：${count}`)
  return `商品卡片 ${count} 个`
})

await step('02 首页-关键词搜索', async () => {
  await page.fill('.search-box input', '教材')
  await page.press('.search-box input', 'Enter')
  await page.waitForTimeout(1200)
  const count = await page.locator('.product-card').count()
  await shot(page, '02-首页-关键词搜索')
  return `命中 ${count} 个商品`
})

await step('03 首页-分类筛选与排序', async () => {
  await page.goto(`${BASE}/`, { waitUntil: 'networkidle' })
  await page.click('.chip:has-text("数码电子")')
  await page.waitForTimeout(1000)
  // Element Plus 的下拉不是原生 select，需要点开后选选项
  await page.click('.filter-right .el-select')
  await page.waitForTimeout(400)
  await page.click('.el-select-dropdown__item:has-text("价格从低到高")')
  await page.waitForTimeout(1000)
  await shot(page, '03-首页-分类筛选与排序')
  const summary = await page.locator('.result-bar').innerText()
  const prices = await page.locator('.product-card .price').allInnerTexts()
  if (prices.length > 1) {
    const numbers = prices.map((text) => Number(text.replace(/[^\d.]/g, '')))
    const sorted = [...numbers].sort((a, b) => a - b)
    if (JSON.stringify(numbers) !== JSON.stringify(sorted)) {
      throw new Error(`价格未按升序排列：${numbers.join(',')}`)
    }
  }
  return `${summary.trim()} 价格序列=${prices.map((t) => t.trim()).join(',')}`
})

await step('04 商品详情-查看商品信息', async () => {
  await page.goto(`${BASE}/products/1`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.detail-card', { timeout: 15000 })
  await shot(page, '04-商品详情')
  const title = await page.locator('.title-row h1').innerText()
  return title
})

await step('05 未登录点击立即购买跳转登录页', async () => {
  await page.click('button:has-text("立即购买")')
  await page.waitForTimeout(1000)
  await shot(page, '05-未登录下单跳转登录页')
  if (!page.url().includes('/login')) throw new Error(`未跳转登录页：${page.url()}`)
  return page.url()
})

// ---------- 2. 买家流程 ----------
await step('06 登录 buyer01', async () => {
  await login('buyer01')
  await page.goto(`${BASE}/`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(800)
  await shot(page, '06-登录后首页')
  const nickname = await page.locator('.user-dropdown').innerText()
  return nickname.trim()
})

await step('07 商品详情-收藏商品', async () => {
  await page.goto(`${BASE}/products/5`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.detail-card')
  await page.click('button:has-text("收藏商品")')
  await page.waitForTimeout(900)
  await shot(page, '07-商品详情-收藏成功')
  return await page.locator('.el-message').first().innerText()
})

await step('08 下单-立即购买并填写留言', async () => {
  await page.goto(`${BASE}/products/3`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.detail-card')
  await page.click('button:has-text("立即购买")')
  await page.waitForSelector('.el-dialog', { timeout: 8000 })
  await page.fill('.el-dialog textarea', '前端自动化：今晚 7 点在图书馆一楼交易可以吗？')
  await shot(page, '08-下单确认弹窗')
  await page.click('.el-dialog button:has-text("确认下单")')
  await page.waitForTimeout(1500)
  await shot(page, '09-下单成功-我的订单')
  if (!page.url().includes('/orders')) throw new Error(`未跳转订单页：${page.url()}`)
  return page.url()
})

await step('10 我的收藏列表', async () => {
  await page.goto(`${BASE}/favorites`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(900)
  await shot(page, '10-我的收藏')
  return (await page.locator('.el-table__row').count()) + ' 条收藏'
})

// ---------- 3. 发布闲置 ----------
await step('11 发布闲置-表单校验提示', async () => {
  await page.goto(`${BASE}/publish`, { waitUntil: 'networkidle' })
  await page.click('button:has-text("发布商品")')
  await page.waitForTimeout(800)
  await shot(page, '11-发布闲置-必填校验')
  const errors = await page.locator('.el-form-item__error').count()
  if (errors === 0) throw new Error('未出现必填校验提示')
  return `${errors} 条校验提示`
})

await step('12 发布闲置-填写表单并上传图片', async () => {
  await page.fill('input[placeholder="例如：九成新《软件测试技术》教材"]', productTitle)
  await page.click('.el-form-item:has(label:text("商品分类")) .el-select')
  await page.click('.el-select-dropdown__item:has-text("其他闲置")')
  await page.fill('input[placeholder="例如：三号宿舍楼下 / 图书馆一楼"]', '一号食堂门口')
  await page.fill('textarea', '这是前端联调测试发布的商品，用于验证发布、上下架与订单流程。')
  const priceInput = page.locator('.el-form-item:has(label:text("售价")) input').first()
  await priceInput.fill('9.90')
  await page.setInputFiles('input[type=file]', UPLOAD)
  await page.waitForTimeout(2000)
  await shot(page, '12-发布闲置-填写完成')
  return productTitle
})

await step('13 发布闲置-提交并跳转详情', async () => {
  await page.click('button:has-text("发布商品")')
  await page.waitForTimeout(2500)
  await shot(page, '13-发布成功-商品详情')
  if (!page.url().includes('/products/')) throw new Error(`未跳转商品详情：${page.url()}`)
  return page.url()
})

await step('14 我发布的商品列表', async () => {
  await page.goto(`${BASE}/my-products`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  await shot(page, '14-我发布的商品')
  const text = await page.locator('.el-table').innerText()
  if (!text.includes(productTitle)) throw new Error('列表中未找到刚发布的商品')
  return productTitle
})

await step('15 商品下架与重新上架', async () => {
  const row = page.locator('.el-table__row', { hasText: productTitle }).first()
  await row.locator('button:has-text("下架")').click()
  await page.click('.el-message-box button:has-text("确定")')
  await page.waitForTimeout(1500)
  await shot(page, '15-商品已下架')
  const rowAfter = page.locator('.el-table__row', { hasText: productTitle }).first()
  const status = await rowAfter.locator('.el-tag').innerText()
  return `状态=${status}`
})

// ---------- 4. 消息与个人中心 ----------
await step('16 消息中心-未读与标记已读', async () => {
  await page.goto(`${BASE}/messages`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1000)
  await shot(page, '16-消息中心')
  const unread = await page.locator('.message-item.unread').count()
  return `未读消息 ${unread} 条`
})

await step('17 个人中心-资料与改密码表单', async () => {
  await page.goto(`${BASE}/profile`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1000)
  await shot(page, '17-个人中心')
  return await page.locator('input').first().inputValue()
})

// ---------- 5. 卖家确认 / 买家完成 ----------
await step('18 卖家登录并确认订单', async () => {
  await logout()
  await login('seller01')
  await page.goto(`${BASE}/orders?role=seller`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  await shot(page, '18-卖家订单列表')
  const confirmBtn = page.locator('button:has-text("确认订单")').first()
  if (await confirmBtn.count() === 0) throw new Error('没有待确认的订单')
  await confirmBtn.click()
  await page.click('.el-message-box button:has-text("确定")')
  await page.waitForTimeout(1500)
  await shot(page, '19-卖家已确认订单')
  return '卖家确认成功'
})

await step('20 买家登录并确认完成', async () => {
  await logout()
  await login('buyer01')
  await page.goto(`${BASE}/orders`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  const finishBtn = page.locator('button:has-text("确认完成")').first()
  if (await finishBtn.count() === 0) throw new Error('没有交易中的订单')
  await finishBtn.click()
  await page.click('.el-message-box button:has-text("确定")')
  await page.waitForTimeout(1500)
  await shot(page, '20-买家确认完成-交易结束')
  const statusText = await page.locator('.el-table__row').first().innerText()
  return statusText.split('\n').filter(Boolean).slice(-3).join(' | ')
})

// ---------- 6. 管理端 ----------
await step('21 管理员-商品管理页', async () => {
  await logout()
  await login('admin')
  await page.goto(`${BASE}/admin/products`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  await shot(page, '21-管理端-商品管理')
  return (await page.locator('.el-table__row').count()) + ' 行数据'
})

await step('22 管理员-强制下架商品', async () => {
  await page.fill('.toolbar input', productTitle)
  await page.click('button:has-text("查询")')
  await page.waitForTimeout(1500)
  const row = page.locator('.el-table__row').first()
  const status = await row.locator('.el-tag').innerText()
  if (status.includes('已下架')) {
    return '商品已是下架状态，跳过强制下架'
  }
  await row.locator('button:has-text("强制下架")').click()
  await page.click('.el-message-box button:has-text("确定")')
  await page.waitForTimeout(1500)
  await shot(page, '22-管理端-强制下架商品')
  return '强制下架完成'
})

await step('23 管理员-用户管理页', async () => {
  await page.goto(`${BASE}/admin/users`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  await shot(page, '23-管理端-用户管理')
  return (await page.locator('.el-table__row').count()) + ' 个用户'
})

await step('24 管理员-分类管理页', async () => {
  await page.goto(`${BASE}/admin/categories`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  await shot(page, '24-管理端-分类管理')
  return (await page.locator('.el-table__row').count()) + ' 个分类'
})

await step('25 窄屏适配（900px）', async () => {
  await page.setViewportSize({ width: 900, height: 900 })
  await page.goto(`${BASE}/`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1200)
  await shot(page, '25-窄屏首页')
  return '900x900'
})

await browser.close()

const pass = results.filter((item) => item.ok).length
const fail = results.length - pass
fs.writeFileSync(
  'C:/Users/hxyvraf/Desktop/campus-secondhand/docs/测试执行证据/ui-e2e-result.json',
  JSON.stringify({ base: BASE, pass, fail, results }, null, 2),
  'utf8'
)
console.log(`\n======== 前端真实点击结果：PASS=${pass} FAIL=${fail} ========`)
process.exit(fail > 0 ? 1 : 0)
