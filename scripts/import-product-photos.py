"""商品照片替换工具：读对照表 -> 校验 -> 调用 sharp 处理图片 -> 写回 data.sql 与数据库。

常用命令：
    # 1) 生成桌面「商品照片」文件夹与对照表模板.xlsx
    python scripts/import-product-photos.py --init

    # 2) 预演（只处理图片到临时目录并打印替换计划，不改任何数据）
    python scripts/import-product-photos.py --dry-run

    # 3) 正式替换（处理图片到 seed-images、改写 data.sql、执行 UPDATE 更新数据库）
    python scripts/import-product-photos.py --apply

对照表格式：xlsx（模板） / csv / txt，第一行为表头，至少包含「商品ID」与「照片文件名」两列。
未填照片文件的商品保持原有占位图不变；单行有问题只跳过该行并在清单中说明。
"""

import argparse
import csv
import json
import os
import re
import shutil
import subprocess
import sys
from datetime import datetime

from openpyxl import Workbook, load_workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, "backend")
SEED_DIR = os.path.join(BACKEND, "src", "main", "resources", "seed-images")
DATA_SQL = os.path.join(BACKEND, "src", "main", "resources", "db", "data.sql")
EVIDENCE_DIR = os.path.join(ROOT, "docs", "测试执行证据")
NODE_SCRIPT = os.path.join(ROOT, "scripts", "process-photos.mjs")
DEFAULT_PHOTO_DIR = os.path.join(os.path.expanduser("~"), "Desktop", "商品照片")
DEFAULT_OUTPUT_DIR = SEED_DIR

DB_NAME = "campus_trade"
DB_USER = "root"
DB_PASSWORD = "123456"

IMAGE_EXTENSIONS = [".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".avif", ".tif", ".tiff"]
HEADER_FILL = PatternFill("solid", fgColor="1F9D76")
HEADER_FONT = Font(color="FFFFFF", bold=True)
WRAP = Alignment(vertical="top", wrap_text=True)


def log(message=""):
    print(message, flush=True)


def find_mysql():
    found = shutil.which("mysql") or shutil.which("mysql.exe")
    if found:
        return found
    for candidate in (
        r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
    ):
        if os.path.exists(candidate):
            return candidate
    return None


def run_sql(sql, database=DB_NAME):
    """执行 SQL（查询用 batch 模式返回 TSV 文本；写操作用 --execute）。"""
    mysql = find_mysql()
    if not mysql:
        raise RuntimeError("没有找到 mysql 客户端（mysql.exe）")
    env = dict(os.environ)
    env["MYSQL_PWD"] = DB_PASSWORD
    command = [mysql, "--host=127.0.0.1", "--port=3306", f"--user={DB_USER}",
               "--default-character-set=utf8mb4", "--batch", "--raw", "--skip-column-names"]
    if database:
        command.append(database)
    command += ["-e", sql]
    completed = subprocess.run(command, capture_output=True, env=env)
    if completed.returncode != 0:
        error = completed.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"mysql 执行失败：{error}")
    return completed.stdout.decode("utf-8", errors="replace")


def load_products():
    """优先从数据库读取商品清单，数据库不可用时回退到解析 data.sql。"""
    try:
        sql = (
            "SELECT p.id, p.title, COALESCE(c.name, ''), "
            "COALESCE((SELECT pi.url FROM product_image pi WHERE pi.product_id = p.id "
            "ORDER BY pi.sort, pi.id LIMIT 1), '') "
            "FROM product p LEFT JOIN category c ON c.id = p.category_id "
            "WHERE p.deleted = 0 AND p.id <= 20 ORDER BY p.id;"
        )
        rows = [line.split("\t") for line in run_sql(sql).strip().splitlines() if line.strip()]
        rows = [row for row in rows if row and row[0].strip().isdigit()]
        if rows:
            return [{"id": int(row[0]), "title": row[1], "category": row[2], "image": row[3]} for row in rows]
    except Exception as error:  # noqa: BLE001 - 数据库不可用时回退解析脚本
        log(f"  （读取数据库失败，改用 data.sql 解析：{error}）")

    if not os.path.exists(DATA_SQL):
        raise RuntimeError("既读不到数据库，也找不到 data.sql")
    with open(DATA_SQL, "r", encoding="utf-8") as fh:
        content = fh.read()
    categories = {}
    for match in re.finditer(r"\((\d+),\s*'([^']+)',\s*(\d+)\)", content):
        categories[int(match.group(1))] = match.group(2)
    images = {}
    for match in re.finditer(r"\(\s*\d+\s*,\s*(\d+)\s*,\s*'(/seed/[^']+)'", content):
        images.setdefault(int(match.group(1)), match.group(2))
    products = []
    for match in re.finditer(r"^\s*\((\d+),\s*\d+,\s*(\d+),\s*'([^']*)'", content, re.M):
        product_id = int(match.group(1))
        if product_id > 20:
            continue
        products.append({
            "id": product_id,
            "title": match.group(3),
            "category": categories.get(int(match.group(2)), ""),
            "image": images.get(product_id, ""),
        })
    return sorted(products, key=lambda item: item["id"])


def write_template(photo_dir, products, force=False):
    os.makedirs(photo_dir, exist_ok=True)
    path = os.path.join(photo_dir, "对照表模板.xlsx")
    if os.path.exists(path) and not force:
        return path, False

    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "对照表"
    headers = ["商品ID", "商品标题", "分类", "当前图片", "照片文件名（填这里）", "备注"]
    sheet.append(headers)
    for cell in sheet[1]:
        cell.fill = HEADER_FILL
        cell.font = HEADER_FONT
        cell.alignment = Alignment(horizontal="center", vertical="center")
    for product in products:
        sheet.append([product["id"], product["title"], product["category"], product["image"], "", ""])
    widths = [10, 40, 14, 22, 30, 26]
    for index, width in enumerate(widths, start=1):
        sheet.column_dimensions[get_column_letter(index)].width = width
    for row in sheet.iter_rows(min_row=2, max_row=sheet.max_row, max_col=len(headers)):
        for cell in row:
            cell.alignment = WRAP
    sheet.freeze_panes = "A2"

    guide = workbook.create_sheet("填写说明")
    guide_rows = [
        ["这一步要做什么", "把每件商品对应的照片文件名填到「照片文件名（填这里）」列，保存后告诉 Codex 一声即可。"],
        ["照片放哪里", f"照片放在与这张表相同的文件夹：{photo_dir}"],
        ["文件名怎么写", "要和实际文件完全一致，例如：鼠标.jpg、教材01.png（区分大小写不敏感，支持中文文件名）"],
        ["只写名字可以吗", "可以。如果同名的图片只有一种格式，只写「鼠标」也能自动找到 鼠标.jpg / 鼠标.png"],
        ["不想换的商品", "该行留空即可，商品会继续使用现在的占位图"],
        ["一个商品多张图", "本轮只做每件 1 张；以后想加轮播图，改成 product-03-1.jpg / product-03-2.jpg 即可扩展"],
        ["图片会被怎么处理", "自动纠正手机拍摄方向 → 居中裁成 4:3 → 缩放到 800x600（小于该尺寸不放大）→ JPEG 质量 85，单张约 50-150KB"],
        ["支持的格式", "jpg / jpeg / png / webp / gif / bmp / avif / tif"],
        ["填错了怎么办", "把这一行的文件名改掉或清空重新运行即可，不会破坏其他商品"],
        ["素材版权提醒", "请使用可自由使用的图片；本项目仅用于个人练习与简历演示"],
    ]
    guide.append(["项目", "说明"])
    for cell in guide[1]:
        cell.fill = HEADER_FILL
        cell.font = HEADER_FONT
    for row in guide_rows:
        guide.append(row)
    guide.column_dimensions["A"].width = 20
    guide.column_dimensions["B"].width = 100
    for row in guide.iter_rows(min_row=2, max_row=guide.max_row, max_col=2):
        for cell in row:
            cell.alignment = WRAP

    workbook.save(path)
    return path, True


def find_mapping_file(photo_dir, explicit=None):
    if explicit:
        if not os.path.exists(explicit):
            raise RuntimeError(f"指定的对照表不存在：{explicit}")
        return explicit
    candidates = []
    for name in sorted(os.listdir(photo_dir)):
        lower = name.lower()
        full = os.path.join(photo_dir, name)
        if os.path.isfile(full) and "对照表" in name and lower.endswith((".xlsx", ".csv", ".txt")):
            candidates.append(full)
    if not candidates:
        return None
    candidates.sort(key=lambda path: (1 if "模板" in os.path.basename(path) else 0,
                                      0 if path.lower().endswith(".xlsx") else 1))
    return candidates[0]


def normalize_header(value):
    text = str(value or "").strip().lower()
    return re.sub(r"[\s（）()_\-]+", "", text)


def parse_mapping(path):
    """解析对照表，返回 [(商品ID, 照片文件名原始文本)]"""
    name = path.lower()
    if name.endswith(".xlsx"):
        workbook = load_workbook(path, data_only=True)
        sheet = workbook.worksheets[0]
        rows = [list(row) for row in sheet.iter_rows(values_only=True)]
    elif name.endswith(".csv"):
        text = None
        for encoding in ("utf-8-sig", "gbk", "utf-8"):
            try:
                with open(path, "r", encoding=encoding, newline="") as fh:
                    text = list(csv.reader(fh))
                break
            except UnicodeDecodeError:
                continue
        rows = text or []
    else:
        with open(path, "r", encoding="utf-8-sig", errors="replace") as fh:
            raw_lines = [line.rstrip("\n") for line in fh if line.strip()]
        rows = [re.split(r"\t|,|\s{2,}", line.strip()) for line in raw_lines]

    if not rows:
        return []

    id_index, file_index = None, None
    for index, cell in enumerate(rows[0]):
        header = normalize_header(cell)
        if id_index is None and ("商品id" in header or header == "id"):
            id_index = index
        if file_index is None and ("照片文件名" in header or "文件名" in header or "照片" in header):
            file_index = index
    data_rows = rows
    if id_index is None or file_index is None:
        id_index, file_index = 0, 1
    else:
        data_rows = rows[1:]

    parsed = []
    for row in data_rows:
        if not row or all(cell is None or str(cell).strip() == "" for cell in row):
            continue
        raw_id = row[id_index] if id_index < len(row) else None
        raw_file = row[file_index] if file_index < len(row) else None
        try:
            product_id = int(str(raw_id).strip())
        except (TypeError, ValueError):
            product_id = None
        parsed.append((product_id, str(raw_file or "").strip()))
    return parsed


def resolve_photo(photo_dir, raw_name):
    """把对照表里写的文件名解析成真实路径（大小写不敏感、可自动补扩展名）"""
    name = raw_name.strip().strip('"').strip("'").strip()
    if not name:
        return None, "未填写"
    direct = os.path.join(photo_dir, name)
    if os.path.isfile(direct):
        return direct, ""

    flattened = []
    for root, _dirs, files in os.walk(photo_dir):
        for file_name in files:
            flattened.append(os.path.join(root, file_name))
    lowered = name.lower()
    for full in flattened:
        if os.path.basename(full).lower() == lowered:
            return full, ""
    if not os.path.splitext(name)[1]:
        for full in flattened:
            base, ext = os.path.splitext(full)
            if os.path.basename(base).lower() == lowered and ext.lower() in IMAGE_EXTENSIONS:
                return full, ""
    return None, "照片文件不存在"


def update_data_sql(url_by_product):
    with open(DATA_SQL, "r", encoding="utf-8") as fh:
        lines = fh.readlines()
    line_pattern = re.compile(r"^(\s*)\(\s*(\d+)\s*,\s*(\d+)\s*,\s*'(/seed/[^']+)'\s*,")
    url_pattern = re.compile(r"'(/seed/[^']+)'")
    changed = 0
    for index, line in enumerate(lines):
        match = line_pattern.match(line)
        if not match:
            continue
        product_id = int(match.group(3))
        if product_id in url_by_product:
            lines[index] = url_pattern.sub(f"'{url_by_product[product_id]}'", line, count=1)
            changed += 1
    with open(DATA_SQL, "w", encoding="utf-8") as fh:
        fh.writelines(lines)
    return changed


def write_report(results, skipped, url_by_product, products, photo_dir, mapping_path, applied):
    os.makedirs(EVIDENCE_DIR, exist_ok=True)
    path = os.path.join(EVIDENCE_DIR, "照片替换清单.md")
    by_id = {product["id"]: product for product in products}
    ok_results = [item for item in results if item.get("ok")]
    fail_results = [item for item in results if not item.get("ok")]

    lines = []
    lines.append("# 商品照片替换清单")
    lines.append("")
    lines.append(f"- 执行时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append(f"- 照片目录：`{photo_dir}`")
    lines.append(f"- 对照表：`{mapping_path}`")
    lines.append(f"- 模式：{'正式替换' if applied else '预演（未改动项目文件）'}")
    lines.append(f"- 结果：成功替换 {len(ok_results)} 件，失败 {len(fail_results)} 件，"
                 f"跳过 {len(skipped)} 行；未填写的商品保留原占位图")
    lines.append("")
    lines.append("## 一、已替换的商品")
    lines.append("")
    lines.append("| 商品ID | 商品标题 | 原图 | 新图 | 原图尺寸 | 处理后尺寸 | 体积变化 |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- |")
    for item in ok_results:
        product = by_id.get(int(item["productId"]), {})
        before, after = item["before"], item["after"]
        lines.append("| {0} | {1} | {2} | {3} | {4}x{5} {6} | {7}x{8} jpeg | {9}KB → {10}KB |".format(
            item["productId"], product.get("title", ""), product.get("image", "-"),
            url_by_product.get(int(item["productId"]), "-"),
            before["width"], before["height"], before["format"],
            after["width"], after["height"],
            round(before["size"] / 1024), round(after["size"] / 1024)))
    if not ok_results:
        lines.append("| - | - | - | - | - | - | - |")
    lines.append("")
    lines.append("## 二、未替换（保留占位图）")
    lines.append("")
    replaced_ids = {int(item["productId"]) for item in ok_results}
    rest = [product for product in products if product["id"] not in replaced_ids]
    if rest:
        lines.append("| 商品ID | 商品标题 | 当前图片 |")
        lines.append("| --- | --- | --- |")
        for product in rest:
            lines.append(f"| {product['id']} | {product['title']} | {product['image'] or '-'} |")
    else:
        lines.append("全部 20 件商品都已替换为真实照片。")
    lines.append("")
    lines.append("## 三、跳过或异常的行")
    lines.append("")
    if skipped or fail_results:
        lines.append("| 商品ID | 照片 | 原因 |")
        lines.append("| --- | --- | --- |")
        for item in fail_results:
            lines.append(f"| {item.get('productId')} | {os.path.basename(item.get('source',''))} | {item.get('error')} |")
        for product_id, reason in skipped:
            lines.append(f"| {product_id if product_id else '-'} | - | {reason} |")
    else:
        lines.append("无。")
    lines.append("")

    with open(path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    return path


def run_photo_processing(items, output_dir):
    import tempfile
    jobs_file = os.path.join(tempfile.mkdtemp(prefix="photo-jobs-"), "jobs.json")
    result_file = jobs_file.replace(".json", ".result.json")
    payload = {"outputDir": output_dir, "items": items, "resultFile": result_file}
    with open(jobs_file, "w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=2)
    print(f"  正在处理 {len(items)} 张照片（4:3 / 800x600 / JPEG q85）…", flush=True)
    subprocess.run(["node", NODE_SCRIPT, "--jobs", jobs_file], check=True)
    with open(result_file, "r", encoding="utf-8") as fh:
        return json.load(fh)["results"]


def main():
    parser = argparse.ArgumentParser(description="校园二手交易平台：用真实照片替换商品占位图")
    parser.add_argument("--dir", default=DEFAULT_PHOTO_DIR, help="照片与对照表所在目录，默认桌面「商品照片」")
    parser.add_argument("--map", default=None, help="显式指定对照表文件（xlsx/csv/txt）")
    parser.add_argument("--out", default=None, help="处理后的图片输出目录，默认写入 backend/src/main/resources/seed-images")
    parser.add_argument("--init", action="store_true", help="创建照片目录并生成对照表模板.xlsx")
    parser.add_argument("--force", action="store_true", help="生成模板时覆盖已存在的文件")
    parser.add_argument("--apply", action="store_true", help="正式替换：写图片、改 data.sql、更新数据库")
    args = parser.parse_args()

    products = load_products()
    log(f"读取到 {len(products)} 件初始商品（ID 1-20）")

    if args.init:
        path, created = write_template(args.dir, products, args.force)
        log("")
        log(f"照片目录：{args.dir}")
        log(f"对照表模板：{path} ({'已生成' if created else '已存在，未覆盖（需要覆盖加 --force）'})")
        log("")
        log("接下来：")
        log("  1) 把商品照片复制到该目录（文件名随意，支持中文）；")
        log("  2) 在「对照表模板.xlsx」的「照片文件名（填这里）」列填写对应文件名，保存；")
        log("  3) 告诉 Codex 一声，就会执行替换（预演 -> 正式替换 -> 重新打包重启 -> 截图验证）。")
        return

    photo_dir = args.dir
    if not os.path.isdir(photo_dir):
        raise SystemExit(f"照片目录不存在：{photo_dir}\n请先执行：python scripts/import-product-photos.py --init")

    mapping_path = find_mapping_file(photo_dir, args.map)
    if not mapping_path:
        raise SystemExit(f"在 {photo_dir} 里没有找到对照表（文件名需包含「对照表」，支持 xlsx/csv/txt）")
    log(f"使用对照表：{mapping_path}")

    rows = parse_mapping(mapping_path)
    by_id = {product["id"]: product for product in products}
    items, skipped, filled = [], [], 0
    for product_id, raw_name in rows:
        if product_id is None:
            skipped.append((None, "商品ID 无法识别（必须是数字）"))
            continue
        if product_id not in by_id:
            skipped.append((product_id, "商品 ID 不在 1-20 范围内"))
            continue
        if not raw_name:
            continue
        filled += 1
        photo, error = resolve_photo(photo_dir, raw_name)
        if not photo:
            skipped.append((product_id, f"「{raw_name}」{error}"))
            continue
        items.append({"productId": product_id, "source": photo, "output": f"product-{product_id:02d}.jpg"})

    log(f"对照表共 {len(rows)} 行，填写了照片 {filled} 行，可处理 {len(items)} 张")
    if not items:
        log("没有可处理的照片。请检查对照表里是否填了「照片文件名」，以及文件是否真的在目录中。")
        for product_id, reason in skipped:
            log(f"  - 跳过：商品 {product_id if product_id else '-'} —— {reason}")
        return

    if args.out:
        output_dir = args.out
    elif args.apply:
        output_dir = DEFAULT_OUTPUT_DIR
    else:
        import tempfile
        output_dir = tempfile.mkdtemp(prefix="photo-preview-")

    log("")
    results = run_photo_processing(items, output_dir)
    by_product = {int(item["productId"]): item for item in results}
    url_by_product = {}
    ok_items, fail_items = [], []
    for item in results:
        if item.get("ok"):
            product_id = int(item["productId"])
            url_by_product[product_id] = f"/seed/{item['output']}"
            ok_items.append(item)
        else:
            fail_items.append(item)

    log("")
    log("处理结果：")
    log("  商品ID | 标题                      | 原图                    | 处理后            | 体积变化")
    log("  -------|---------------------------|-------------------------|-------------------|--------------")
    for item in ok_items:
        product_id = int(item["productId"])
        title = by_id[product_id]["title"]
        before, after = item["before"], item["after"]
        log("  {0:>6} | {1:<25} | {2:<23} | {3:>4}x{4:<4} {5:<7} | {6}KB → {7}KB".format(
            product_id, title[:24], os.path.basename(item["source"])[:22],
            after["width"], after["height"], "jpeg", round(before["size"] / 1024), round(after["size"] / 1024)))
    for item in fail_items:
        log(f"  {item['productId']:>6} | 跳过：{item.get('error')}（{os.path.basename(item.get('source', ''))}）")
    for product_id, reason in skipped:
        log(f"  {('%s' % product_id) if product_id else '-':>6} | 跳过：{reason}")

    if not args.apply:
        log("")
        log(f"[预演模式] 图片已生成到：{output_dir}")
        log("没有任何项目文件被修改。确认无误后加 --apply 正式替换。")
        return

    log("")
    changed = update_data_sql(url_by_product)
    log(f"已更新 data.sql 中的图片映射：{changed} 行")
    sql = "\n".join(
        f"UPDATE product_image SET url='{url}', sort=0 WHERE product_id={product_id};"
        for product_id, url in sorted(url_by_product.items())
    )
    run_sql(sql)
    log(f"已更新数据库 product_image 表：{len(url_by_product)} 行（未重置数据，你的测试数据保留）")

    report = write_report(results, skipped, url_by_product, products, photo_dir, mapping_path, True)
    log(f"替换清单：{report}")
    log("")
    log("下一步：")
    log("  1) 重新打包后端（把新图片打进 jar）：双击「重新打包后端.bat」")
    log("  2) 启动后端：双击「启动后端.bat」，然后刷新浏览器查看新照片")
    log("  3) 如果想回到全新初始数据：双击「重置数据.bat」")


if __name__ == "__main__":
    main()
