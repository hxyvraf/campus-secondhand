/**
 * 用 sharp 批量处理商品照片：EXIF 方向纠正 -> 居中裁成 4:3 -> 缩放到 800x600（不放大）-> JPEG q85。
 *
 * 用法：
 *   node scripts/process-photos.mjs --jobs <jobs.json>
 *
 * jobs.json 结构：
 *   { "outputDir": "...", "items": [ { "productId": 1, "source": "D:/照片/鼠标.jpg", "output": "product-01.jpg" } ] }
 *
 * 结果写到 jobs.json 同目录下的 <jobs>.result.json：
 *   { "results": [ { productId, source, output, ok, before:{...}, after:{...}, error? } ] }
 */
import { createRequire } from 'node:module'
import fs from 'node:fs'
import path from 'node:path'

const requireFromProject = createRequire('file:///C:/Users/hxyvraf/Desktop/campus-secondhand/')
const SHARP_PATH = process.env.SHARP_PATH || 'C:/Users/hxyvraf/AppData/Roaming/npm/node_modules/sharp'
const sharp = requireFromProject(SHARP_PATH)

const WIDTH = 800
const HEIGHT = 600
const QUALITY = 85
const TARGET_ASPECT = WIDTH / HEIGHT // 4:3

function readArg(name, fallback = null) {
  const index = process.argv.indexOf(name)
  return index >= 0 ? process.argv[index + 1] : fallback
}

const jobsFile = readArg('--jobs')
if (!jobsFile) {
  console.error('缺少参数：--jobs <jobs.json>')
  process.exit(2)
}

const jobs = JSON.parse(fs.readFileSync(jobsFile, 'utf8'))
const resultFile = jobs.resultFile || jobsFile.replace(/\.json$/i, '') + '.result.json'
const results = []

for (const item of jobs.items) {
  const target = path.join(jobs.outputDir, item.output)
  try {
    if (!fs.existsSync(item.source)) {
      throw new Error('照片文件不存在')
    }
    fs.mkdirSync(path.dirname(target), { recursive: true })
    const beforeMeta = await sharp(item.source).metadata()
    // 带 EXIF 方向标记的照片（手机竖拍）要先交换宽高，再计算裁剪区域
    const swapped = [5, 6, 7, 8].includes(beforeMeta.orientation)
    const srcWidth = swapped ? beforeMeta.height : beforeMeta.width
    const srcHeight = swapped ? beforeMeta.width : beforeMeta.height

    // 第一步：只裁剪不缩放，裁出居中的 4:3 区域（保证每张图都是 4:3）
    let cropWidth = srcWidth
    let cropHeight = srcHeight
    if (srcWidth / srcHeight > TARGET_ASPECT) {
      cropWidth = Math.round(srcHeight * TARGET_ASPECT)
    } else {
      cropHeight = Math.round(srcWidth / TARGET_ASPECT)
    }
    const left = Math.max(0, Math.round((srcWidth - cropWidth) / 2))
    const top = Math.max(0, Math.round((srcHeight - cropHeight) / 2))

    // 第二步：等比缩放到不超过 800x600（小图不放大，避免糊）
    await sharp(item.source)
      .rotate()
      .extract({ left, top, width: cropWidth, height: cropHeight })
      .resize({ width: WIDTH, height: HEIGHT, fit: 'inside', withoutEnlargement: true })
      .jpeg({ quality: QUALITY, mozjpeg: true })
      .toFile(target)
    const afterMeta = await sharp(target).metadata()
    const beforeSize = fs.statSync(item.source).size
    const afterSize = fs.statSync(target).size
    results.push({
      productId: item.productId,
      source: item.source,
      output: item.output,
      ok: true,
      before: {
        width: beforeMeta.width,
        height: beforeMeta.height,
        format: beforeMeta.format,
        size: beforeSize
      },
      after: {
        width: afterMeta.width,
        height: afterMeta.height,
        format: afterMeta.format,
        size: afterSize,
        aspect: Number((afterMeta.width / afterMeta.height).toFixed(3))
      }
    })
  } catch (error) {
    results.push({
      productId: item.productId,
      source: item.source,
      output: item.output,
      ok: false,
      error: String((error && error.message) || error)
    })
  }
}

fs.writeFileSync(resultFile, JSON.stringify({ outputDir: jobs.outputDir, results }, null, 2), 'utf8')

const okCount = results.filter((item) => item.ok).length
console.log(`图片处理完成：成功 ${okCount} 张，失败 ${results.length - okCount} 张`)
for (const item of results) {
  if (item.ok) {
    const before = item.before
    const after = item.after
    console.log(
      `  [OK]   商品 ${item.productId}: ${path.basename(item.source)} ` +
      `${before.width}x${before.height} ${before.format} ${Math.round(before.size / 1024)}KB -> ` +
      `${after.width}x${after.height} jpeg ${Math.round(after.size / 1024)}KB`
    )
  } else {
    console.log(`  [SKIP] 商品 ${item.productId}: ${path.basename(item.source)} —— ${item.error}`)
  }
}
console.log(`结果文件：${resultFile}`)
process.exit(0)
