# 校园二手交易平台（Web 端）

一个可本地运行、可演示、可用来练接口测试的完整前后端项目：**Vue 3 前端 + Spring Boot 后端 + MySQL 8**。

功能对齐简历项目描述中的 5 个模块：**注册登录、商品发布与管理、搜索收藏、订单管理、消息通知**，另附精简管理端（商品管理、用户管理、分类维护）。

---

## 一、技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 3.5 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 后端 | Spring Boot 3.3.5 + Java 21 + MyBatis-Plus 3.5.7 + springdoc-openapi（Swagger） + JWT（jjwt） + BCrypt |
| 数据库 | MySQL 8（`campus_trade`，7 张表，首次启动自动建库建表 + 写入初始数据） |
| 测试 | Postman 集合 + newman 批量执行、Playwright 浏览器真实点击、MySQL 数据校验 |

## 二、快速开始（Windows）

> 前置条件：已安装 JDK 21、Maven 3.9+、Node.js 20+、MySQL 8（本地 3306，账号 `root` / `123456`）。

1. 双击 **`一键启动.bat`**：会同时启动后端（8080）与前端（5173），并自动打开浏览器；
   也可以分开双击 **`启动后端.bat`** 和 **`启动前端.bat`**。
2. 首次启动后端会自动打包（联网下载 Maven 依赖），并自动创建数据库 `campus_trade`、建表、写入初始数据。
3. 首次启动前端会自动执行 `npm install`。

| 用途 | 地址 |
| --- | --- |
| 前端页面 | http://localhost:5173 |
| 后端接口根地址 | http://localhost:8080 |
| 在线接口文档（Swagger UI） | http://localhost:8080/swagger-ui.html |
| OpenAPI 原始定义 | http://localhost:8080/v3/api-docs |

### 测试账号（密码均为 `123456`）

| 账号 | 角色 | 说明 |
| --- | --- | --- |
| `admin` | 管理员 | 可访问管理后台（商品/用户/分类管理） |
| `seller01` | 卖家 | 昵称「林晓」，有 10 件商品（含已下架、交易中） |
| `seller02` | 卖家 | 昵称「陈默」，有 9 件商品（含已售出） |
| `buyer01` | 买家 | 昵称「苏晴」，有 3 条收藏、若干订单与消息 |

## 三、功能一览

| 模块 | 页面 | 接口 |
| --- | --- | --- |
| 注册登录 | 登录、注册 | 注册、登录、退出、当前用户、修改密码 |
| 商品发布与管理 | 发布闲置、我的商品、商品详情 | 发布、编辑、上下架、删除、我发布的、图片上传 |
| 搜索收藏 | 首页（搜索/筛选/排序/分页）、我的收藏 | 商品列表、商品详情、收藏/取消收藏、我的收藏 |
| 订单管理 | 我的订单（我买到的/我卖出的） | 下单、列表、详情、卖家确认、买家完成、取消 |
| 消息通知 | 消息中心、导航栏未读红点 | 消息列表、未读数、标记已读、全部已读 |
| 管理端 | 商品管理、用户管理、分类管理 | 商品列表、强制下架、用户列表、启用/禁用、分类增改删、超时扫描 |

共 **40 个 REST 接口**，详细说明见 [docs/接口文档.md](docs/接口文档.md)。

## 四、目录结构

```
campus-secondhand/
├─ backend/                    Spring Boot 后端
│  ├─ src/main/java/com/campus/trade/
│  │  ├─ controller/           9 个控制器（认证/用户/分类/商品/文件/收藏/订单/消息/管理端）
│  │  ├─ service/              业务逻辑（订单状态机、重复下单防护、超时扫描等）
│  │  ├─ mapper/               MyBatis-Plus Mapper（含联表查询）
│  │  ├─ entity/ dto/ vo/      实体、请求体、响应对象
│  │  ├─ security/             JWT 工具、登录拦截器、用户上下文
│  │  ├─ config/               MyBatis-Plus/Web/OpenAPI/密码加密配置
│  │  └─ task/                 订单超时关闭定时任务
│  ├─ src/main/resources/
│  │  ├─ application.yml       端口、数据库、JWT、上传、超时等配置
│  │  ├─ db/schema.sql         建表脚本
│  │  ├─ db/data.sql           初始数据（可重复执行）
│  │  └─ seed-images/          初始数据用的示例商品图
│  └─ uploads/                 用户上传的图片（运行时生成）
├─ frontend/                   Vue 3 前端（页面、路由、Pinia、Axios 封装）
├─ docs/                       接口文档与测试文档（见下）
├─ scripts/                    启动/重置/打包/测试脚本与文档生成脚本
├─ 一键启动.bat 等             双击即用的入口脚本
```

## 五、接口文档与测试文档

| 文档 | 说明 |
| --- | --- |
| [docs/接口文档.md](docs/接口文档.md) | 40 个接口的详细说明：参数表、请求示例、真实响应示例、异常场景与业务码 |
| [docs/openapi.json](docs/openapi.json) | OpenAPI 3.0 定义，可导入 Postman / Apifox / Swagger |
| [docs/postman/](docs/postman/) | Postman 集合（162 个请求 / 446 条断言）+ 环境变量 + 上传测试素材 |
| [docs/测试计划.md](docs/测试计划.md) | **功能测试专项计划**：7 个模块的需求基线（搜索模块按 需求 / 用户 / 系统 三列，其余模块为 需求 / 规则与约束 / 优先级，并含商品与订单状态流转表、消息触发表）、55 条带编号需求、测试项与验证要点、执行顺序、69 条功能用例的覆盖统计、准入准出与风险对策（不含接口测试） |
| [docs/测试方案.md](docs/测试方案.md) | 测试策略、用例设计方法、断言规范、造数脚本、缺陷流程、通过标准 |
| [docs/测试点梳理.md](docs/测试点梳理.md) | 144 个测试点层级大纲（可粘贴进 Xmind 生成脑图） |
| [docs/测试用例.xlsx](docs/测试用例.xlsx) | 231 条功能测试用例（接口 162 + 前端 69），含真实执行结果与统计 |
| [docs/缺陷记录表.xlsx](docs/缺陷记录表.xlsx) | 测试中真实发现的 3 个缺陷、复现步骤、修复与回归结果，附缺陷填写模板 |
| [docs/测试报告.md](docs/测试报告.md) | 执行统计、缺陷分布、关键场景结论、遗留问题与证据清单 |
| [docs/测试执行证据/](docs/测试执行证据/) | newman HTML/JSON 报告、24 张真实操作截图、冒烟结果 |
| [docs/数据库设计.md](docs/数据库设计.md) | 7 张表结构、索引、ER 关系、初始数据与校验 SQL |

## 六、接口测试怎么练

1. 双击 **`启动后端.bat`**（或一键启动），确认 http://localhost:8080/api/categories 能返回数据；
2. 打开 Postman → Import → 选择 `docs/postman/campus-trade.postman_collection.json` 和 `campus-trade.postman_environment.json`；
3. 右上角切换环境为「校园二手交易平台-本地环境」，用 Collection Runner 按顺序整体运行；
4. 想命令行跑并出 HTML 报告，双击 **`跑接口测试-newman.bat`**，报告在 `docs/测试执行证据/newman-report.html`；
5. 想练手工功能测试，按 `docs/测试用例.xlsx` 中的「测试步骤」在浏览器操作，把结果填回「实际结果」列。

## 七、常用脚本

| 脚本 | 作用 |
| --- | --- |
| `一键启动.bat` | 分别启动后端与前端，并打开浏览器 |
| `启动后端.bat` / `启动前端.bat` | 单独启动某一端（首次会自动安装依赖/打包） |
| `重新打包后端.bat` | 停掉运行中的后端 → `mvn clean package` |
| `打包前端到后端.bat` | 前端构建产物复制进后端 static 目录并重新打包，实现单端口（8080）访问 |
| `重置数据.bat` | 删除 `campus_trade` 数据库 → 重启后端 → 恢复到初始数据 |
| `初始化数据库.bat` | 不启动后端，直接用 mysql 客户端执行 schema.sql 与 data.sql 并校验条数 |
| `跑接口测试-newman.bat` | newman 执行 Postman 集合并生成 HTML 报告 |
| `scripts/smoke-test.ps1` | 接口冒烟脚本（111 条断言，覆盖正常/异常/边界，结果写入 docs/测试执行证据/smoke-result.txt） |
| `scripts/ui-e2e.mjs` | Playwright 驱动浏览器真实点击 23 个前端场景并截图（需先启动前后端） |
| `scripts/generate_postman_collection.py` | 重新生成 Postman 集合与环境文件 |
| `scripts/generate_api_doc.py` | 依据 openapi.json + 真实执行报告重新生成接口文档 |
| `scripts/generate_test_docs.py` | 依据真实执行报告重新生成测试用例.xlsx / 缺陷记录表.xlsx / 测试报告.md |
| `scripts/import-product-photos.py` | 用真实照片替换商品占位图：`--init` 生成桌面「商品照片」文件夹与对照表模板，`--dry-run` 预演，`--apply` 正式替换 |
| `scripts/process-photos.mjs` | 图片处理（sharp）：EXIF 方向纠正 → 4:3 居中裁剪 → 800×600 → JPEG q85 |

## 八、关键业务规则（面试可讲）

1. **统一响应 + 语义化状态码**：所有接口返回 `{code, message, data}`，同时使用 200/400/401/403/404/409 等 HTTP 状态码，方便做「状态码 + 业务码」双层断言；
2. **JWT 无状态鉴权**：登录返回 token，拦截器统一校验；被管理员禁用的账号即使持有旧 token 也会立即被拒绝（403/40302）；
3. **商品状态机**：在售 → 交易中（下单锁定）→ 已售出（订单完成）；取消或超时后回到在售；
4. **重复下单防护**：同一买家对同一商品只能有一个进行中的订单（409/40901），并通过 `UPDATE ... WHERE status='ON_SALE'` 的乐观更新防止并发抢单；
5. **订单超时关闭**：下单时写入 `expire_at`，定时任务每分钟扫描并关闭超时订单，同时通知买卖双方；管理端提供手动触发接口便于测试；
6. **消息通知**：下单、确认、完成、取消、超时、商品被收藏都会生成站内消息，前端每 30 秒轮询未读数刷新红点。

## 九、常见问题

| 问题 | 处理方式 |
| --- | --- |
| 后端启动报数据库连接失败 | 检查 MySQL 服务是否启动、账号密码是否为 `root/123456`（可在 `backend/src/main/resources/application.yml` 修改） |
| 8080 / 5173 端口被占用 | 关闭占用端口的程序，或修改 `application.yml` 的 `server.port`、`frontend/vite.config.js` 的 `server.port` |
| 前端页面能打开但数据加载失败 | 后端未启动或已停止，重新双击「启动后端.bat」 |
| 想恢复到刚初始化时的数据 | 双击「重置数据.bat」，输入 Y 确认 |
| 重新打包后端失败，提示 jar 被占用 | 先关闭运行后端的窗口（或运行「重新打包后端.bat」，脚本会先停服务） |
| 上传图片失败 | 只支持 jpg/jpeg/png，单张不超过 5MB；上传目录为 `backend/uploads` |

## 十、关于商品图片

- 初始数据里的商品图片是脚本生成的**占位图**（`backend/src/main/resources/seed-images/p01.png` ~ `p08.png`，8 张循环使用），由 `scripts/generate_placeholder_images.py` 生成，可随时重新生成。
- **替换成你自己的真实照片**：把照片和填好的「对照表模板.xlsx」放到桌面 `商品照片` 文件夹（执行 `python scripts/import-product-photos.py --init` 会自动创建该文件夹与模板），然后运行：
  - `python scripts/import-product-photos.py --dry-run`：预演，只处理图片到临时目录并打印替换清单，不改动任何项目文件；
  - `python scripts/import-product-photos.py --apply`：正式替换，把处理后的 `product-01.jpg` ~ `product-20.jpg` 写入 `seed-images`、改写 `data.sql` 并更新数据库（不会清空已有测试数据）；
  - 之后双击「重新打包后端.bat」再「启动后端.bat」，刷新页面即可看到新照片；未提供照片的商品继续使用占位图。
- 照片会被统一处理成 4:3、800×600、JPEG 质量 85（小于该尺寸不放大），单张约 50–150KB。
- 商品图片为个人练习与简历演示用的素材，请使用可自由使用的图片，不要用于商业用途。
