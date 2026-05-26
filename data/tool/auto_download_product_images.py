# -*- coding: utf-8 -*-
"""
auto_download_product_images.py - FORCE SAFE V3

Mục tiêu:
- Ảnh nào đã có trong src/main/resources/view/image/products/ thì BỎ QUA, KHÔNG GHI ĐÈ.
- Ảnh nào chưa có thì cố tìm nhiều vòng, nhiều query, nhiều nguồn.
- Chỉ lưu ảnh nếu đủ tin cậy, tránh lỗi phô mai ra baseball / xúc xích ra xe.
- Nếu tìm mãi vẫn không đủ tin cậy thì KHÔNG lưu bậy, ghi vào missing_or_approx_images.csv + image_manual_tasks.html.

Cài/chạy:
    cd "E:\\JAVA\\DoAnFinal\\SieuThiThongMinh_Java"
    python -u "data\\tool\\auto_download_product_images.py"

Chạy mạnh hơn:
    python -u "data\\tool\\auto_download_product_images.py" --force-missing --max-rounds 5 --min-score 55

Nếu muốn xóa ảnh sai rồi tải lại riêng:
    Xóa file ảnh sai trong products/
    Chạy lại script, nó chỉ tải ảnh đang thiếu.
"""


""" Lần 1:
python -u "data\tool\auto_download_product_images.py" --replace-targets --min-score 60

Các lần sau:
python -u "data\tool\auto_download_product_images.py" --min-score 60"""

from __future__ import annotations

import argparse
import csv
import html
import os
import re
import shutil
import subprocess
import sys
import time
import zipfile
import unicodedata
from io import BytesIO
from pathlib import Path
from urllib.parse import quote_plus, urlparse

def ensure_package(import_name: str, pip_name: str | None = None) -> None:
    pip_name = pip_name or import_name
    try:
        __import__(import_name)
    except ImportError:
        print(f"[*] Thiếu thư viện {pip_name}. Đang cài tự động...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", pip_name])

ensure_package("PIL", "Pillow")
ensure_package("requests", "requests")

# Package mới là ddgs. Nếu thiếu thì cài. Nếu vẫn lỗi, fallback duckduckgo_search.
try:
    from ddgs import DDGS
except Exception:
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "ddgs"])
        from ddgs import DDGS
    except Exception:
        ensure_package("duckduckgo_search", "duckduckgo_search")
        from duckduckgo_search import DDGS

import requests
from PIL import Image


SCRIPT_DIR = Path(__file__).resolve().parent

def find_project_root() -> Path:
    for p in [SCRIPT_DIR, *SCRIPT_DIR.parents]:
        if (p / "src" / "main").exists():
            return p
        if (p / ".git").exists():
            return p
        if (p / "pom.xml").exists():
            return p
    return SCRIPT_DIR.parent

PROJECT_ROOT = find_project_root()

CSV_CANDIDATES = [
    SCRIPT_DIR / "products_cat006_to_cat015_with_image_path.csv",
    SCRIPT_DIR.parent / "products_cat006_to_cat015_with_image_path.csv",
    PROJECT_ROOT / "products_cat006_to_cat015_with_image_path.csv",
    PROJECT_ROOT / "data" / "products_cat006_to_cat015_with_image_path.csv",
    PROJECT_ROOT / "data" / "tool" / "products_cat006_to_cat015_with_image_path.csv",
]

BASE_IMG_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "view" / "image"
PRODUCTS_DIR = BASE_IMG_DIR / "products"

ZIP_FILE = PROJECT_ROOT / "product_images_cat006_to_cat015_real.zip"
REPORT_CSV = PROJECT_ROOT / "product_image_auto_download_report.csv"
MISSING_CSV = PROJECT_ROOT / "missing_or_approx_images.csv"
MANUAL_HTML = PROJECT_ROOT / "image_manual_tasks.html"

# Domain ưu tiên vì thường có ảnh sản phẩm đúng.
TRUSTED_DOMAINS = [
    "bachhoaxanh.com", "cdn.tgdd.vn", "thegioididong.com",
    "winmart.vn", "winmart",
    "lottemart.vn", "aeoneshop.com",
    "tiki.vn", "shopee.vn", "lazada.vn",
    "hasaki.vn", "pharmacity.vn",
    "product.hstatic.net", "bizweb.dktcdn.net", "cdn.medigoapp.com",
    "concung.com", "kidsplaza.vn", "petmart.vn",
    "vinamilk.com.vn", "thmilk.vn", "thtrue", "yakult.vn",
    "anchorfoodprofessionals", "vissan.com.vn", "ducvietfoods.vn",
    "orion.vn", "mondelezinternational.com", "unilever", "pigeon.com",
    "smartheart", "whiskas", "pedigree",
]

# Domain/keyword rất hay ra sai.
NEGATIVE_WORDS = [
    "baseball", "football", "soccer", "basketball", "nba", "mlb", "player",
    "pitcher", "wallpaper", "car", "lamborghini", "toyota", "youtube",
    "thumbnail", "alamy", "shutterstock", "getty", "movie", "actor",
    "fashion", "girl", "boy", "meme", "logo", "clipart", "facebook",
    "pinterest", "wikipedia", "wikimedia",
]

STOPWORDS = {
    "san", "pham", "chinh", "hang", "anh", "hinh", "hop", "goi", "loc",
    "chai", "lon", "cai", "mieng", "size", "vi", "co", "khong", "duong",
    "tuoi", "dong", "lanh", "thuc", "an", "dung", "mot", "lan", "pack",
    "ml", "kg", "g", "l", "lit", "to", "bo", "x", "cm", "va", "cho",
    "meo", "em", "be", "rau", "cu", "loai", "mau",
}

# Một số alias giúp query đúng hơn.
ALIASES = {
    "Phô mai Con Bò Cười hộp 8 miếng": [
        "Phô mai Con Bò Cười 8 miếng",
        "La Vache Qui Rit 8 portions Vietnam",
        "Con Bò Cười hộp 8 miếng phô mai",
    ],
    "Bơ lạt Anchor hộp 227g": [
        "Anchor Unsalted Butter 227g",
        "Bơ lạt Anchor 227g hộp",
        "Anchor butter 227g Vietnam",
    ],
    "Xúc xích Đức Việt gói 500g": [
        "Xúc xích Đức Việt 500g",
        "Duc Viet sausage 500g",
        "Xúc xích Đức Việt gói 500g sản phẩm",
    ],
    "Hộp nhựa Lock Lock 1L": [
        "Hộp nhựa Lock&Lock 1L",
        "LocknLock food container 1L",
        "Lock Lock hộp nhựa 1L",
    ],
    "Pate mèo Me-O cá ngừ 80g": [
        "Pate mèo Me-O cá ngừ 80g",
        "Me-O tuna cat food 80g",
    ],
    "Hạt mèo Whiskas cá ngừ 1.2kg": [
        "Whiskas cá ngừ 1.2kg",
        "Whiskas tuna 1.2kg cat food",
    ],
    "Hạt chó Pedigree vị bò 1.5kg": [
        "Pedigree vị bò 1.5kg",
        "Pedigree beef 1.5kg dog food",
    ],
}

def normalize_text(s: str) -> str:
    s = s or ""
    s = s.replace("Đ", "D").replace("đ", "d")
    s = unicodedata.normalize("NFD", s)
    s = "".join(ch for ch in s if unicodedata.category(ch) != "Mn")
    s = s.lower()
    s = re.sub(r"[^a-z0-9]+", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s

def product_tokens(product_name: str) -> list[str]:
    norm = normalize_text(product_name)
    toks = []
    for t in norm.split():
        if len(t) < 3:
            continue
        if t in STOPWORDS:
            continue
        if t.isdigit():
            continue
        toks.append(t)

    seen = set()
    out = []
    for t in toks:
        if t not in seen:
            out.append(t)
            seen.add(t)
    return out

def domain_of(url: str) -> str:
    try:
        return urlparse(url).netloc.lower()
    except Exception:
        return ""

def is_existing_image_ok(path: Path) -> bool:
    if not path.exists() or path.stat().st_size < 2000:
        return False
    try:
        with Image.open(path) as img:
            w, h = img.size
            return w >= 100 and h >= 100
    except Exception:
        return False

def score_result(product_name: str, result: dict) -> tuple[int, list[str]]:
    title = result.get("title") or ""
    image = result.get("image") or result.get("thumbnail") or ""
    url = result.get("url") or result.get("source") or ""

    haystack = normalize_text(" ".join([title, image, url]))
    full_url = (image + " " + url).lower()

    reasons = []
    score = 0

    toks = product_tokens(product_name)
    matched = 0

    for t in toks:
        if t in haystack:
            matched += 1
            score += 10
            reasons.append(f"+token:{t}")

    # Token đầu thường là brand / nhãn hàng.
    for t in toks[:3]:
        if t in haystack:
            score += 12
            reasons.append(f"+brand:{t}")

    # Thưởng mạnh domain đáng tin.
    for d in TRUSTED_DOMAINS:
        if d in full_url:
            score += 35
            reasons.append(f"+trusted:{d}")
            break

    # Nếu title chứa gần đầy đủ tên sau normalize.
    norm_name = normalize_text(product_name)
    compact_name_tokens = [t for t in norm_name.split() if t not in STOPWORDS and not t.isdigit()]
    if compact_name_tokens:
        ratio = matched / max(1, len(set(compact_name_tokens)))
        if ratio >= 0.70:
            score += 25
            reasons.append("+high_token_ratio")
        elif ratio >= 0.50:
            score += 12
            reasons.append("+mid_token_ratio")
        else:
            score -= 35
            reasons.append("-low_token_ratio")

    # Phạt keyword sai.
    for bad in NEGATIVE_WORDS:
        if bad in full_url or bad in haystack:
            score -= 100
            reasons.append(f"-bad:{bad}")

    # Nếu match quá ít thì phạt để tránh sai chủ đề.
    if toks and matched < max(1, min(2, len(toks))):
        score -= 45
        reasons.append("-too_few_tokens")

    return score, reasons

def find_csv_file() -> Path:
    for p in CSV_CANDIDATES:
        if p.exists():
            return p
    checked = "\n".join(f" - {p}" for p in CSV_CANDIDATES)
    raise FileNotFoundError(f"Không tìm thấy CSV. Đã check:\n{checked}")

def read_products(csv_path: Path) -> list[dict[str, str]]:
    rows = []
    with open(csv_path, "r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        required = {"product_name", "category_id", "image_path"}
        missing = required - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"CSV thiếu cột: {missing}")

        for row in reader:
            name = (row.get("product_name") or "").strip()
            cat = (row.get("category_id") or "").strip()
            img = (row.get("image_path") or "").strip().replace("\\", "/")
            if name and img:
                rows.append({"product_name": name, "category_id": cat, "image_path": img})
    return rows

def ddg_image_results(query: str, max_results: int = 30) -> list[dict]:
    try:
        with DDGS() as ddgs:
            return list(ddgs.images(
                query,
                region="vn-vi",
                safesearch="moderate",
                max_results=max_results,
            ))
    except TypeError:
        with DDGS() as ddgs:
            return list(ddgs.images(query, max_results=max_results))
    except Exception as e:
        print(f"   [WARN] Search lỗi: {e}")
        return []

def build_queries(product_name: str, round_idx: int) -> list[str]:
    aliases = ALIASES.get(product_name, [])
    base = [product_name, *aliases]

    domains = [
        "",
        "site:bachhoaxanh.com",
        "site:winmart.vn",
        "site:tiki.vn",
        "site:shopee.vn",
        "site:lazada.vn",
        "site:lottemart.vn",
        "site:hasaki.vn",
        "site:pharmacity.vn",
    ]

    suffixes = [
        "ảnh sản phẩm",
        "sản phẩm chính hãng",
        "mua online",
        "giá bao bì",
        "png",
    ]

    queries = []

    # Vòng đầu: query mạnh nhất, ít spam.
    if round_idx == 1:
        for b in base:
            queries.append(f'"{b}" ảnh sản phẩm')
            queries.append(f'"{b}" bách hóa xanh')
            queries.append(f'"{b}" chính hãng')
    else:
        for b in base:
            for s in suffixes:
                queries.append(f'"{b}" {s}')
            for d in domains:
                if d:
                    queries.append(f'{d} "{b}"')

    # unique
    seen = set()
    out = []
    for q in queries:
        if q not in seen:
            out.append(q)
            seen.add(q)
    return out

def search_best_candidate(product_name: str, max_rounds: int, min_score: int) -> tuple[dict | None, int, str, list[str]]:
    best = None
    best_score = -10**9
    best_query = ""
    best_reasons = []

    for round_idx in range(1, max_rounds + 1):
        print(f"   --- Round {round_idx}/{max_rounds} ---")
        queries = build_queries(product_name, round_idx)

        for q_i, q in enumerate(queries, start=1):
            print(f"   -> Search: {q}")
            results = ddg_image_results(q, max_results=30)

            for r in results:
                score, reasons = score_result(product_name, r)
                if score > best_score:
                    best = r
                    best_score = score
                    best_query = q
                    best_reasons = reasons

            # đủ chắc thì return luôn
            if best_score >= min_score:
                return best, best_score, best_query, best_reasons

            # tránh rate limit
            time.sleep(0.8)

        # nghỉ dài hơn sau mỗi vòng
        if round_idx < max_rounds:
            print("   [WAIT] Nghỉ để tránh rate-limit...")
            time.sleep(4 + round_idx * 2)

    return best, best_score, best_query, best_reasons

def download_image(url: str) -> bytes:
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept": "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
        "Referer": "https://www.google.com/",
    }
    resp = requests.get(url, headers=headers, timeout=25)
    resp.raise_for_status()
    if len(resp.content) < 2000:
        raise ValueError("Ảnh quá nhỏ/không hợp lệ")
    return resp.content

def resize_to_square(content: bytes, dst_path: Path, size: int = 600) -> None:
    with Image.open(BytesIO(content)) as img:
        img = img.convert("RGBA")
        w, h = img.size
        if w < 120 or h < 120:
            raise ValueError(f"Ảnh quá nhỏ: {w}x{h}")

        img.thumbnail((size, size), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (size, size), (255, 255, 255, 255))
        x = (size - img.width) // 2
        y = (size - img.height) // 2
        canvas.paste(img, (x, y), img)
        dst_path.parent.mkdir(parents=True, exist_ok=True)
        canvas.convert("RGB").save(dst_path, "PNG", optimize=True)

def make_zip(target_images: list[Path]) -> None:
    if ZIP_FILE.exists():
        ZIP_FILE.unlink()

    with zipfile.ZipFile(ZIP_FILE, "w", zipfile.ZIP_DEFLATED) as z:
        for p in sorted(target_images):
            if is_existing_image_ok(p):
                z.write(p, f"products/{p.name}")

def make_manual_html(rows: list[list[str]]) -> None:
    parts = [
        "<!doctype html><html><head><meta charset='utf-8'>",
        "<title>Manual Product Image Tasks</title>",
        "<style>body{font-family:Arial;padding:20px;background:#f8fafc}table{border-collapse:collapse;width:100%;background:white}td,th{border:1px solid #ddd;padding:8px}th{background:#0f172a;color:white}a{font-weight:bold;color:#2563eb}code{color:#0f766e}</style>",
        "</head><body>",
        "<h2>Ảnh chưa tìm đủ tin cậy - tải thủ công</h2>",
        "<p>Mở link Google, chọn ảnh đúng, lưu đúng tên trong <code>src/main/resources/view/image/products/</code></p>",
        "<table><tr><th>Danh mục</th><th>Sản phẩm</th><th>File cần lưu</th><th>Google</th><th>Note</th></tr>",
    ]
    for cat, name, image_path, status, url, note in rows:
        q = quote_plus(f"{name} ảnh sản phẩm chính hãng")
        g = f"https://www.google.com/search?tbm=isch&q={q}"
        parts.append(
            f"<tr><td>{html.escape(cat)}</td><td>{html.escape(name)}</td>"
            f"<td><code>{html.escape(image_path)}</code></td>"
            f"<td><a href='{g}' target='_blank'>Tìm ảnh</a></td>"
            f"<td>{html.escape(note[:300])}</td></tr>"
        )
    parts.append("</table></body></html>")
    MANUAL_HTML.write_text("\n".join(parts), encoding="utf-8")

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--force-missing", action="store_true",
                        help="Cố tìm ảnh còn thiếu nhiều vòng hơn. Ảnh đã có vẫn bỏ qua.")
    parser.add_argument("--max-rounds", type=int, default=3,
                        help="Số vòng tìm kiếm tối đa cho mỗi ảnh thiếu.")
    parser.add_argument("--min-score", type=int, default=55,
                        help="Điểm tối thiểu để chấp nhận ảnh. Gợi ý 55-70.")
    parser.add_argument("--overwrite-existing", action="store_true",
                        help="Ghi đè cả ảnh đã có. KHÔNG khuyến nghị.")
    args = parser.parse_args()

    if args.force_missing and args.max_rounds < 5:
        args.max_rounds = 5

    csv_path = find_csv_file()
    products = read_products(csv_path)
    PRODUCTS_DIR.mkdir(parents=True, exist_ok=True)

    target_paths = [PRODUCTS_DIR / os.path.basename(p["image_path"]) for p in products]

    print("=" * 72)
    print("FORCE SAFE PRODUCT IMAGE DOWNLOADER V3")
    print("=" * 72)
    print(f"Project root       : {PROJECT_ROOT}")
    print(f"CSV                : {csv_path}")
    print(f"Save dir           : {PRODUCTS_DIR}")
    print(f"Total              : {len(products)}")
    print(f"Skip existing      : {not args.overwrite_existing}")
    print(f"Force missing      : {args.force_missing}")
    print(f"Max rounds/missing : {args.max_rounds}")
    print(f"Min score          : {args.min_score}")
    print("=" * 72)

    report_rows = []
    problem_rows = []

    downloaded = skipped_existing = missing = rejected = failed = 0

    for idx, item in enumerate(products, 1):
        name = item["product_name"]
        cat = item["category_id"]
        image_path = item["image_path"]
        filename = os.path.basename(image_path)
        target = PRODUCTS_DIR / filename

        print(f"\n[{idx}/{len(products)}] {name}")

        if is_existing_image_ok(target) and not args.overwrite_existing:
            skipped_existing += 1
            report_rows.append([cat, name, image_path, "EXISTING", "", "Ảnh đã có, bỏ qua, không ghi đè."])
            print("   [SKIP] Ảnh đã có, không ghi đè.")
            continue

        candidate, score, query, reasons = search_best_candidate(name, args.max_rounds, args.min_score)

        if not candidate:
            missing += 1
            row = [cat, name, image_path, "MISSING", "", "Không có kết quả search đủ dùng."]
            report_rows.append(row)
            problem_rows.append(row)
            print("   [MISSING] Không có kết quả.")
            continue

        img_url = candidate.get("image") or candidate.get("thumbnail") or ""
        page_url = candidate.get("url") or candidate.get("source") or ""
        title = candidate.get("title") or ""
        note = f"score={score}; query={query}; title={title}; reasons={';'.join(reasons[:15])}"

        if score < args.min_score:
            rejected += 1
            row = [cat, name, image_path, "REJECT", page_url or img_url, "Điểm thấp, không lưu để tránh sai ảnh. " + note]
            report_rows.append(row)
            problem_rows.append(row)
            print(f"   [REJECT] score={score} < {args.min_score}: {title}")
            continue

        try:
            content = download_image(img_url)
            resize_to_square(content, target, 600)
            downloaded += 1
            row = [cat, name, image_path, "DOWNLOADED", page_url or img_url, note]
            report_rows.append(row)
            print(f"   [OK] score={score} -> {target.name}")
            print(f"        title: {title}")

        except Exception as e:
            failed += 1
            row = [cat, name, image_path, "FAILED", page_url or img_url, f"Lỗi tải/lưu: {e}. {note}"]
            report_rows.append(row)
            problem_rows.append(row)
            print(f"   [FAILED] {e}")

    with open(REPORT_CSV, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["category_id", "product_name", "image_path", "status", "source_url", "note"])
        writer.writerows(report_rows)

    with open(MISSING_CSV, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["category_id", "product_name", "image_path", "status", "source_url", "note"])
        writer.writerows(problem_rows)

    make_manual_html(problem_rows)
    make_zip(target_paths)

    print("\n" + "=" * 72)
    print("KẾT QUẢ")
    print("=" * 72)
    print(f"Tổng sản phẩm       : {len(products)}")
    print(f"Đã có, bỏ qua       : {skipped_existing}")
    print(f"Tải mới             : {downloaded}")
    print(f"MISSING             : {missing}")
    print(f"REJECT              : {rejected}")
    print(f"FAILED              : {failed}")
    print(f"ZIP                 : {ZIP_FILE}")
    print(f"Report              : {REPORT_CSV}")
    print(f"File cần xử lý tay  : {MISSING_CSV}")
    print(f"Trang tìm tay       : {MANUAL_HTML}")
    print("=" * 72)
    print("Nguyên tắc: Không lưu bậy. Ảnh nào chưa chắc thì đưa vào file xử lý tay.")


if __name__ == "__main__":
    main()
