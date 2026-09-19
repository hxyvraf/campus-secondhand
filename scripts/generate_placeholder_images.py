"""生成商品占位图（纯标准库手写 PNG，不依赖任何第三方库）。

注意：占位图只用于「还没有提供真实照片」的商品。提供真实照片请用
scripts/import-product-photos.py（配合 scripts/process-photos.mjs），
处理后的照片会以 product-NN.jpg 命名放在同一目录下，不会覆盖这里的占位图。

用在两处：
1) 初始数据里的商品配图（backend/uploads/seed/*.png）；
2) 接口测试上传图片用例的测试素材（docs/测试素材/test-upload.png）。
"""

import os
import struct
import zlib

BACKEND = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "backend")
# 示例图片放在 resources/seed-images 下，随源码一起提交，通过 /seed/** 访问；
# uploads 目录只放用户实际上传的图片。
SEED_DIR = os.path.join(BACKEND, "src", "main", "resources", "seed-images")
MATERIAL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                            "docs", "postman", "testdata")


def write_png(path, width, height, pixel):
    rows = []
    for y in range(height):
        row = bytearray(b"\x00")
        for x in range(width):
            row.extend(pixel(x, y))
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    payload = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
               + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(payload)
    return len(payload)


def palette_placeholder(base, size=(480, 360)):
    """生成带斜条纹与渐变斜角的占位图，视觉上能区分不同商品。"""
    width, height = size
    r0, g0, b0 = base

    def pixel(x, y):
        # 渐变：左下到右上逐渐变亮
        t = (x / width * 0.55) + ((height - y) / height * 0.45)
        r = int(r0 + (255 - r0) * t * 0.55)
        g = int(g0 + (255 - g0) * t * 0.55)
        b = int(b0 + (255 - b0) * t * 0.55)
        # 斜条纹：每 60 像素一条淡纹，让图片有层次
        if (x + y) % 60 < 6:
            r = min(255, r + 14)
            g = min(255, g + 14)
            b = min(255, b + 14)
        # 四周留白边框
        if x < 6 or y < 6 or x >= width - 6 or y >= height - 6:
            r, g, b = 245, 246, 250
        return bytes((r, g, b))

    return pixel


COLORS = [
    (86, 119, 178),   # 蓝 - 教材书籍
    (108, 156, 121),  # 绿 - 数码电子
    (196, 138, 92),   # 橙 - 生活用品
    (168, 108, 152),  # 紫 - 服饰鞋包
    (92, 158, 176),   # 青 - 运动户外
    (188, 168, 96),   # 黄 - 其他闲置
    (150, 118, 190),  # 淡紫
    (200, 118, 118),  # 砖红
]


def main():
    created = []
    for index, color in enumerate(COLORS, start=1):
        path = os.path.join(SEED_DIR, f"p{index:02d}.png")
        size = write_png(path, 480, 360, palette_placeholder(color))
        created.append((path, size))

    # 上传接口测试用的素材（12x12 纯色小图 + 非法类型文件）
    test_png = os.path.join(MATERIAL_DIR, "test-upload.png")
    write_png(test_png, 120, 90, palette_placeholder((110, 140, 200), (120, 90)))
    created.append((test_png, os.path.getsize(test_png)))

    bad_file = os.path.join(MATERIAL_DIR, "test-upload.txt")
    os.makedirs(MATERIAL_DIR, exist_ok=True)
    with open(bad_file, "w", encoding="utf-8") as fh:
        fh.write("这个文件用于测试上传接口对非法文件类型的校验。\n")
    created.append((bad_file, os.path.getsize(bad_file)))

    # 6MB 大文件，用于测试文件大小限制（接口限制 5MB）
    big_file = os.path.join(MATERIAL_DIR, "test-upload-6mb.png")
    with open(big_file, "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n")
        fh.write(b"\x00" * (6 * 1024 * 1024))
    created.append((big_file, os.path.getsize(big_file)))

    for path, size in created:
        print(f"{size:>9} bytes  {path}")


if __name__ == "__main__":
    main()
