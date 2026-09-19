"""根据运行中的服务导出的 openapi.json + Postman 集合 + newman 执行报告，生成《接口文档.md》。

这样生成的文档有两个好处：
1. 参数、路径、响应结构直接来自代码里的注解，不会出现"文档和实现不一致"；
2. 每个接口的请求示例、成功响应示例、异常场景全部来自真实执行过的请求和真实响应。
"""

import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")
OPENAPI = os.path.join(DOCS, "openapi.json")
COLLECTION = os.path.join(DOCS, "postman", "campus-trade.postman_collection.json")
REPORT = os.path.join(DOCS, "测试执行证据", "newman-report.json")
OUTPUT = os.path.join(DOCS, "接口文档.md")

METHODS = ["get", "post", "put", "patch", "delete"]
LITERAL_PATHS = set()


def load_json(path):
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)


def resolve_ref(spec, node):
    """展开 $ref（只处理本文件内部的引用）"""
    if not isinstance(node, dict):
        return {}
    if "$ref" in node:
        ref = node["$ref"].lstrip("#/").split("/")
        target = spec
        for part in ref:
            target = target.get(part, {})
        return target
    return node


def schema_type(schema):
    if not schema:
        return "-"
    if "$ref" in schema:
        return schema["$ref"].split("/")[-1]
    if "type" in schema:
        if schema["type"] == "array":
            return "array<{0}>".format(schema_type(schema.get("items", {})))
        if schema["type"] == "integer":
            return "integer"
        return schema["type"]
    return "-"


def path_to_regex(path_template):
    pattern = re.sub(r"\{[^}]+\}", "[^/]+", path_template)
    return re.compile("^" + pattern + "$")


def collect_executions():
    """从 newman 报告里取出每条请求的真实执行结果"""
    if not os.path.exists(REPORT):
        return []
    report = load_json(REPORT)
    executions = []
    for item in report.get("run", {}).get("executions", []):
        request = item.get("request", {})
        url = request.get("url", {}) or {}
        path = "/" + "/".join(url.get("path") or [])
        if path == "/":
            raw = url.get("raw", "")
            path = raw.split("?", 1)[0]
        response = item.get("response") or {}
        stream = response.get("stream")
        if isinstance(stream, dict) and "data" in stream:
            # newman 的 JSON 报告把响应体存成 Buffer 字节数组
            response_text = bytes(stream["data"]).decode("utf-8", errors="replace")
        else:
            response_text = stream if isinstance(stream, str) else ""
        executions.append({
            "name": item.get("item", {}).get("name", ""),
            "method": request.get("method", "").upper(),
            "path": path,
            "body": (request.get("body") or {}).get("raw", ""),
            "status": response.get("code"),
            "response_body": response_text,
            "assertions": item.get("assertions", []),
        })
    return executions


def collect_collection_map():
    """从 Postman 集合取出每条请求所属的模块，便于在文档里标注用例名称"""
    collection = load_json(COLLECTION)
    mapping = {}
    for folder in collection.get("item", []):
        for item in folder.get("item", []):
            mapping[item["name"]] = folder["name"]
    return mapping


def format_json(text, max_length=1200):
    if not text:
        return ""
    try:
        parsed = json.loads(text)
        pretty = json.dumps(parsed, ensure_ascii=False, indent=2)
    except Exception:
        pretty = text
    if len(pretty) > max_length:
        pretty = pretty[:max_length] + "\n... （内容过长，已截断）"
    return pretty


def main():
    global LITERAL_PATHS
    spec = load_json(OPENAPI)
    LITERAL_PATHS = {item for item in spec["paths"] if "{" not in item}
    executions = collect_executions()
    folder_map = collect_collection_map()
    tags = {tag["name"]: tag.get("description", "") for tag in spec.get("tags", [])}

    # 按 tag 归组接口
    grouped = {}
    for path, operations in spec["paths"].items():
        for method in METHODS:
            if method not in operations:
                continue
            operation = operations[method]
            tag = (operation.get("tags") or ["未分类"])[0]
            grouped.setdefault(tag, []).append((path, method, operation))
    for tag in grouped:
        grouped[tag].sort(key=lambda item: (item[0], item[1]))
    ordered_tags = sorted(grouped.keys())

    lines = []

    # ---------------- 文档说明 ----------------
    lines.append("# 校园二手交易平台 接口文档")
    lines.append("")
    lines.append("> 版本：v1.0.0　|　接口数量：{0} 个　|　后端：Spring Boot 3.3.5 + MyBatis-Plus + MySQL 8"
                 "　|　生成方式：`scripts/generate_api_doc.py`（读取运行中服务导出的 openapi.json + 真实执行报告）".format(
                     sum(len(v) for v in grouped.values())))
    lines.append("")
    lines.append("## 一、怎么用这份文档")
    lines.append("")
    lines.append("| 用途 | 地址 / 方式 |")
    lines.append("| --- | --- |")
    lines.append("| 在线接口文档（Swagger UI） | http://localhost:8080/swagger-ui.html |")
    lines.append("| OpenAPI 原始定义 | http://localhost:8080/v3/api-docs （或本目录 openapi.json） |")
    lines.append("| 导入 Postman / Apifox | `docs/postman/campus-trade.postman_collection.json` + "
                 "`campus-trade.postman_environment.json` |")
    lines.append("| 命令行批量执行 | 双击根目录 `跑接口测试-newman.bat` |")
    lines.append("")
    lines.append("**鉴权约定**：除下列白名单接口外，所有接口都需要在请求头携带 `Authorization: Bearer {token}`，"
                 "token 由登录接口返回。")
    lines.append("")
    lines.append("- 白名单（匿名可访问）：`POST /api/auth/register`、`POST /api/auth/login`、`GET /api/categories`、"
                 "`GET /api/products`、`GET /api/products/{id}`、`GET /api/users/{id}`、`GET /api/users/{id}/products`")
    lines.append("- 管理员接口（`/api/admin/**`）：需要 `admin` 账号登录后的 token，普通用户访问返回 403/40303。")
    lines.append("")
    lines.append("## 二、公共约定")
    lines.append("")
    lines.append("### 1. 统一响应体")
    lines.append("")
    lines.append("```json")
    lines.append('{ "code": 200, "message": "操作成功", "data": { } }')
    lines.append("```")
    lines.append("")
    lines.append("- `code`：业务返回码，200 表示成功，其他值见下方错误码表；")
    lines.append("- `message`：面向用户的提示信息（前端可直接展示）；")
    lines.append("- `data`：业务数据，失败时一般为 null；参数校验失败时为字段级错误列表 "
                 "`[{\"field\": \"price\", \"message\": \"价格不能低于 0.01 元\"}]`。")
    lines.append("")
    lines.append("### 2. HTTP 状态码与业务码")
    lines.append("")
    lines.append("接口同时返回语义化 HTTP 状态码与业务码，便于做双层断言。")
    lines.append("")
    lines.append("| HTTP 状态码 | 业务码 | 含义 | 典型场景 |")
    lines.append("| --- | --- | --- | --- |")
    error_rows = [
        ("200", "200", "成功", "查询、创建、修改成功"),
        ("400", "40001", "参数校验失败", "必填为空、长度超限、价格越界、分页参数非法、页码/ID 非数字"),
        ("400", "40002", "文件类型不支持", "上传非 jpg/jpeg/png 文件"),
        ("400", "40003", "文件大小超限", "上传超过 5MB 的图片"),
        ("401", "40101", "未登录或登录已过期", "未带 token、token 非法/过期访问受保护接口"),
        ("401", "40102", "用户名或密码错误", "登录时账号或密码不正确"),
        ("403", "40301", "无权限操作该资源", "修改他人商品、查看无关订单、标记他人消息、禁用自己"),
        ("403", "40302", "账号已被禁用", "被管理员禁用后继续调用接口或登录"),
        ("403", "40303", "需要管理员权限", "普通用户访问 /api/admin/**"),
        ("404", "40401", "资源不存在", "商品/订单/消息/用户不存在"),
        ("405", "40500", "请求方法不支持", "用 GET 调 POST 接口等"),
        ("409", "40901", "重复下单", "同一买家对同一商品重复提交订单"),
        ("409", "40902", "状态不允许该操作", "重复下架、已完成订单再取消、删除有商品的分类"),
        ("409", "40903", "商品当前不可交易", "商品已锁定/已售出/已下架，或购买自己的商品"),
        ("409", "40904", "用户名已存在", "注册时用户名重复"),
        ("415", "41500", "Content-Type 不支持", "请求头不是 application/json"),
        ("500", "50000", "服务器内部错误", "未预期的服务端异常"),
    ]
    for row in error_rows:
        lines.append("| {0} |".format(" | ".join(row)))
    lines.append("")
    lines.append("### 3. 分页响应")
    lines.append("")
    lines.append("列表接口的分页参数：`page`（默认 1，必须 ≥ 1）、`size`（默认 10，范围 1-100）。")
    lines.append("")
    lines.append("```json")
    lines.append('{ "code": 200, "message": "操作成功", "data": {')
    lines.append('  "records": [], "total": 20, "page": 1, "size": 10, "pages": 2 } }')
    lines.append("```")
    lines.append("")
    lines.append("超过总页数时返回 200 与空 `records`（不会自动回到第一页）。")
    lines.append("")
    lines.append("### 4. 其他约定")
    lines.append("")
    lines.append("- 时间格式统一为 `yyyy-MM-dd HH:mm:ss`（东八区）；")
    lines.append("- 金额为 `DECIMAL(10,2)`，取值范围 0.01 - 999999.99；")
    lines.append("- 商品图片：先调 `POST /api/files/images` 上传，拿到 `/uploads/yyyyMMdd/xxx.png` 后再放入发布/编辑商品的 `imageUrls`；")
    lines.append("- 初始数据里的示例图片地址形如 `/seed/p01.png`，由后端静态资源提供；")
    lines.append("- 商品与订单的状态枚举见文末「状态字典」。")
    lines.append("")
    lines.append("## 三、测试账号与初始数据")
    lines.append("")
    lines.append("| 账号 | 密码 | 角色 | 说明 |")
    lines.append("| --- | --- | --- | --- |")
    lines.append("| admin | 123456 | 管理员 | 可访问管理端接口 |")
    lines.append("| seller01 | 123456 | 普通用户 | 昵称「林晓」，初始有 9 件在售 + 1 件已下架 + 1 件交易中商品 |")
    lines.append("| seller02 | 123456 | 普通用户 | 昵称「陈默」，初始有 8 件在售 + 1 件已售出商品 |")
    lines.append("| buyer01 | 123456 | 普通用户 | 昵称「苏晴」，初始有 3 条收藏、2 笔订单 |")
    lines.append("")
    lines.append("初始数据还包含：6 个商品分类、20 件商品（覆盖在售/交易中/已售出/已下架四种状态）、4 条站内消息。")
    lines.append("")

    # ---------------- 接口清单 ----------------
    lines.append("## 四、接口清单")
    lines.append("")
    for tag in ordered_tags:
        lines.append("### {0}".format(tag))
        lines.append("")
        if tags.get(tag):
            lines.append("{0}".format(tags[tag]))
            lines.append("")
        lines.append("| 方法 | 路径 | 说明 | 鉴权 |")
        lines.append("| --- | --- | --- | --- |")
        for path, method, operation in grouped[tag]:
            summary = operation.get("summary", "")
            auth = "管理员" if path.startswith("/api/admin") else ("匿名" if is_public(path, method) else "登录")
            lines.append("| {0} | `{1}` | {2} | {3} |".format(method.upper(), path, summary, auth))
        lines.append("")

    # ---------------- 接口详情 ----------------
    lines.append("## 五、接口详情")
    lines.append("")
    for tag in ordered_tags:
        lines.append("### {0}".format(tag))
        lines.append("")
        for path, method, operation in grouped[tag]:
            render_operation(lines, spec, path, method, operation, executions, folder_map)

    # ---------------- 使用说明 ----------------
    lines.append("## 六、Postman / newman 使用说明")
    lines.append("")
    lines.append("1. **导入**：Postman → Import → 选择 `docs/postman/campus-trade.postman_collection.json` 与 "
                 "`campus-trade.postman_environment.json`，右上角环境选择「校园二手交易平台-本地环境」。")
    lines.append("2. **执行顺序**：集合内按「认证 → 用户 → 分类 → 商品 → 收藏 → 订单 → 消息 → 管理端」排列，"
                 "请整体运行（Collection Runner），不要打乱顺序：登录接口会写入 `buyerToken`/`sellerToken`/`seller2Token`/`adminToken`，"
                 "发布商品会写入 `productId`，下单会写入 `orderId`，后续用例都依赖这些变量。")
    lines.append("3. **断言方式**：每条请求都断言了 HTTP 状态码、业务返回码和关键业务字段；")
    lines.append("4. **命令行执行**：在项目根目录执行 `newman run docs/postman/campus-trade.postman_collection.json -e "
                 "docs/postman/campus-trade.postman_environment.json -r cli,htmlextra`，或直接双击 `跑接口测试-newman.bat`，"
                 "HTML 报告输出到 `docs/测试执行证据/newman-report.html`。")
    lines.append("5. **上传图片用例**：集合中的上传用例引用 `docs/postman/testdata/` 下的测试素材，"
                 "因此 newman 需要在项目根目录执行（双击 bat 已自动处理）。")
    lines.append("6. **超时关闭订单**：先把某条 `PENDING_CONFIRM` 订单的 `expire_at` 改成过去时间，"
                 "再调用 `POST /api/admin/orders/timeout-scan`；具体 SQL 见《测试方案》。")
    lines.append("")

    # ---------------- 状态字典 ----------------
    lines.append("## 七、状态字典")
    lines.append("")
    lines.append("### 商品状态 ProductStatus")
    lines.append("")
    lines.append("| 值 | 中文 | 说明 |")
    lines.append("| --- | --- | --- |")
    lines.append("| ON_SALE | 在售 | 可以被下单购买 |")
    lines.append("| LOCKED | 交易中 | 已被某个买家下单锁定，其他买家不能下单 |")
    lines.append("| SOLD | 已售出 | 订单完成后自动置为该状态 |")
    lines.append("| OFF_SHELF | 已下架 | 卖家主动下架或管理员强制下架 |")
    lines.append("")
    lines.append("### 订单状态 OrderStatus")
    lines.append("")
    lines.append("| 值 | 中文 | 可执行操作 |")
    lines.append("| --- | --- | --- |")
    lines.append("| PENDING_CONFIRM | 待卖家确认 | 卖家确认、买卖双方取消、超时自动关闭 |")
    lines.append("| CONFIRMED | 交易中 | 买家确认完成、买卖双方取消 |")
    lines.append("| COMPLETED | 已完成 | 终态（不可取消），商品变为已售出 |")
    lines.append("| CANCELED | 已取消 | 终态，商品回到在售 |")
    lines.append("| TIMEOUT | 超时关闭 | 终态，卖家超时未确认，商品回到在售 |")
    lines.append("")
    lines.append("状态流转图：")
    lines.append("")
    lines.append("```")
    lines.append("                     卖家确认                买家确认完成")
    lines.append("PENDING_CONFIRM  ───────────►  CONFIRMED  ───────────►  COMPLETED（商品 SOLD）")
    lines.append("      │                            │")
    lines.append("      │ 取消 / 超时自动关闭          │ 取消")
    lines.append("      ▼                            ▼")
    lines.append("  CANCELED / TIMEOUT（商品回到 ON_SALE）")
    lines.append("```")
    lines.append("")
    lines.append("### 消息类型 MessageType")
    lines.append("")
    lines.append("| 值 | 中文 | 触发时机 |")
    lines.append("| --- | --- | --- |")
    lines.append("| ORDER | 订单消息 | 下单、卖家确认、买家完成、订单取消、超时关闭 |")
    lines.append("| FAVORITE | 收藏消息 | 卖家收到「商品被收藏」通知 |")
    lines.append("| SYSTEM | 系统消息 | 平台通知（初始数据中的欢迎消息） |")
    lines.append("")

    # ---------------- 执行结果 ----------------
    lines.append("## 八、最近一次真实接口测试执行结果")
    lines.append("")
    if executions:
        total_assertions = sum(len(item["assertions"]) for item in executions)
        failed_assertions = 0
        for item in executions:
            failed_assertions += len([a for a in item["assertions"] if not a.get("passed", True)])
        lines.append("- 请求数：{0}，断言数：{1}，失败断言：{2}".format(len(executions), total_assertions, failed_assertions))
        lines.append("- HTML 报告：`docs/测试执行证据/newman-report.html`；原始数据：`docs/测试执行证据/newman-report.json`")
    else:
        lines.append("- 还没有执行记录，运行 `跑接口测试-newman.bat` 后重新生成本文档。")
    lines.append("")

    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    print("已生成接口文档：{0}".format(OUTPUT))
    print("接口数量：{0}".format(sum(len(v) for v in grouped.values())))


PUBLIC_PATHS = {"/api/auth/register", "/api/auth/login", "/api/categories", "/api/products",
                "/api/products/{id}", "/api/users/{id}", "/api/users/{id}/products"}


def is_public(path, method):
    if path in PUBLIC_PATHS and method in ("get", "post"):
        if path in ("/api/auth/register", "/api/auth/login"):
            return method == "post"
        return method == "get"
    return False


def render_operation(lines, spec, path, method, operation, executions, folder_map):
    summary = operation.get("summary", "")
    description = operation.get("description", "")
    lines.append("#### {0} {1}".format(method.upper(), path))
    lines.append("")
    if summary:
        lines.append("**{0}**".format(summary))
        lines.append("")
    if description:
        lines.append(description)
        lines.append("")
    auth = "管理员" if path.startswith("/api/admin") else ("匿名可访问" if is_public(path, method) else "需要登录（Bearer token）")
    lines.append("- 鉴权：{0}".format(auth))

    parameters = list(operation.get("parameters", []))
    if parameters:
        lines.append("")
        lines.append("| 参数 | 位置 | 必填 | 类型 | 说明 |")
        lines.append("| --- | --- | --- | --- | --- |")
        for parameter in parameters:
            schema = parameter.get("schema", {})
            lines.append("| `{0}` | {1} | {2} | {3} | {4} |".format(
                parameter.get("name"),
                {"path": "路径", "query": "查询参数", "header": "请求头"}.get(parameter.get("in"), parameter.get("in")),
                "是" if parameter.get("required") else "否",
                schema_type(schema),
                (parameter.get("description") or "-").replace("\n", " ")))

    request_body = operation.get("requestBody")
    if request_body:
        content = (request_body.get("content") or {}).get("application/json", {})
        schema = resolve_ref(spec, content.get("schema", {}))
        properties = schema.get("properties", {})
        required = schema.get("required", [])
        lines.append("")
        lines.append("请求体字段（application/json）：")
        lines.append("")
        lines.append("| 字段 | 类型 | 必填 | 说明 |")
        lines.append("| --- | --- | --- | --- |")
        for name, prop in properties.items():
            prop = resolve_ref(spec, prop)
            lines.append("| `{0}` | {1} | {2} | {3} |".format(
                name, schema_type(prop), "是" if name in required else "否",
                (prop.get("description") or prop.get("title") or "-").replace("\n", " ")))

    regex = path_to_regex(path)
    matched = []
    for item in executions:
        if item["method"] != method.upper() or not regex.match(item["path"]):
            continue
        # /api/products/mine 这类具体路径不能被 /api/products/{id} 模板"吃掉"
        if item["path"] != path and item["path"] in LITERAL_PATHS:
            continue
        matched.append(item)
    matched.sort(key=lambda item: (item["status"] or 0))

    success = next((item for item in matched if item["status"] == 200), None)
    if success and success["body"]:
        lines.append("")
        lines.append("请求示例：")
        lines.append("")
        lines.append("```json")
        lines.append(format_json(success["body"]))
        lines.append("```")
    if success and success["response_body"]:
        lines.append("")
        lines.append("成功响应示例（真实执行结果）：")
        lines.append("")
        lines.append("```json")
        lines.append(format_json(success["response_body"], 1500))
        lines.append("```")

    errors = [item for item in matched if item["status"] != 200]
    if errors:
        lines.append("")
        lines.append("异常场景（均来自本次真实执行）：")
        lines.append("")
        lines.append("| 场景 | 实测 HTTP | 业务码 | 断言 | 返回信息 |")
        lines.append("| --- | --- | --- | --- | --- |")
        for item in errors:
            code = ""
            message = ""
            try:
                parsed = json.loads(item["response_body"])
                code = parsed.get("code", "")
                message = (parsed.get("message") or "").replace("|", "/")
            except Exception:
                message = ""
            passed = all(a.get("passed", True) for a in item["assertions"])
            lines.append("| {0} | {1} | {2} | {3} | {4} |".format(
                item["name"], item["status"], code,
                "通过" if passed else "失败", message[:60]))
    lines.append("")


if __name__ == "__main__":
    main()
