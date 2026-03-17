#!/usr/bin/env python3
"""
Populate `puja_samagri_master` images with base64 data URIs.

Flow:
- Read active samagri rows from MySQL.
- Search image candidates from web using item name.
- Download image, compress to JPEG, convert to base64 data URI.
- Save into:
    - puja_samagri_master.image_url
    - puja_samagri_master_images.image_url (display_order=0)

Usage:
  python3 scripts/fill_puja_samagri_images_base64.py
  python3 scripts/fill_puja_samagri_images_base64.py --force --limit 20
"""

from __future__ import annotations

import argparse
import base64
import html
import io
import re
import time
import warnings
from dataclasses import dataclass
from typing import Iterable

import pymysql
import requests
from PIL import Image, ImageOps, UnidentifiedImageError


requests.packages.urllib3.disable_warnings()  # type: ignore[attr-defined]
warnings.filterwarnings(
    "ignore",
    message="Unverified HTTPS request is being made to host .*",
)


USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)


@dataclass
class SamagriRow:
    item_id: int
    name: str
    hi_name: str
    image_url: str | None


def normalize_spaces(value: str) -> str:
    return re.sub(r"\s+", " ", (value or "").strip())


def is_data_uri(value: str | None) -> bool:
    if not value:
        return False
    return value.strip().lower().startswith("data:image")


def build_queries(name: str, hi_name: str) -> list[str]:
    clean_name = normalize_spaces(name)
    clean_hi = normalize_spaces(hi_name)
    queries: list[str] = []
    if clean_name:
        queries.extend(
            [
                f"{clean_name} puja samagri",
                f"{clean_name} hindu pooja item",
                clean_name,
            ]
        )
    if clean_hi:
        queries.extend(
            [
                f"{clean_hi} पूजा सामग्री",
                clean_hi,
            ]
        )
    seen: set[str] = set()
    unique: list[str] = []
    for query in queries:
        key = query.lower().strip()
        if not key or key in seen:
            continue
        seen.add(key)
        unique.append(query)
    return unique


def search_image_candidates(query: str, max_results: int) -> list[str]:
    response = requests.get(
        "https://www.bing.com/images/search",
        params={
            "q": query,
            "form": "HDRSC2",
        },
        headers={"User-Agent": USER_AGENT},
        timeout=20,
        verify=False,
    )
    if response.status_code != 200:
        return []

    raw_matches = re.findall(r"murl&quot;:&quot;(http[^&]+)&quot;", response.text)
    urls: list[str] = []
    for encoded in raw_matches:
        url = html.unescape(encoded).strip()
        if not (url.startswith("http://") or url.startswith("https://")):
            continue
        urls.append(url)
        if len(urls) >= max_results:
            break

    # stable unique
    dedup: list[str] = []
    seen: set[str] = set()
    for url in urls:
        if url in seen:
            continue
        seen.add(url)
        dedup.append(url)
    return dedup


def download_image(url: str, timeout_seconds: int, max_bytes: int) -> tuple[bytes, str] | None:
    try:
        response = requests.get(
            url,
            timeout=timeout_seconds,
            headers={"User-Agent": USER_AGENT},
            verify=False,
        )
    except Exception:
        return None
    if response.status_code != 200:
        return None
    raw = response.content or b""
    if not raw:
        return None
    if len(raw) > max_bytes:
        return None
    content_type = (response.headers.get("Content-Type") or "").split(";")[0].strip().lower()
    if content_type and not content_type.startswith("image/"):
        return None
    return raw, content_type


def encode_image_to_data_uri(
    raw: bytes,
    content_type: str,
    max_dimension: int,
    jpeg_quality: int,
) -> str | None:
    try:
        image = Image.open(io.BytesIO(raw))
        image = ImageOps.exif_transpose(image)
        if max(image.size) > max_dimension:
            image.thumbnail((max_dimension, max_dimension), Image.Resampling.LANCZOS)

        if image.mode in ("RGBA", "LA", "P"):
            rgb = Image.new("RGB", image.size, (255, 255, 255))
            if image.mode in ("RGBA", "LA"):
                rgb.paste(image, mask=image.split()[-1])
            else:
                rgb.paste(image)
            image = rgb
        elif image.mode != "RGB":
            image = image.convert("RGB")

        output = io.BytesIO()
        image.save(output, format="JPEG", quality=jpeg_quality, optimize=True)
        encoded = base64.b64encode(output.getvalue()).decode("ascii")
        return f"data:image/jpeg;base64,{encoded}"
    except (UnidentifiedImageError, OSError, ValueError):
        pass

    # fallback: keep original image bytes as-is
    if content_type.startswith("image/"):
        encoded = base64.b64encode(raw).decode("ascii")
        return f"data:{content_type};base64,{encoded}"
    return None


def choose_data_uri_for_item(
    name: str,
    hi_name: str,
    max_results: int,
    timeout_seconds: int,
    max_download_bytes: int,
    max_dimension: int,
    jpeg_quality: int,
    max_encoded_length: int,
) -> tuple[str | None, str | None]:
    for query in build_queries(name, hi_name):
        try:
            candidates = search_image_candidates(query, max_results=max_results)
        except Exception:
            candidates = []
        for url in candidates:
            downloaded = download_image(url, timeout_seconds=timeout_seconds, max_bytes=max_download_bytes)
            if downloaded is None:
                continue
            raw, content_type = downloaded
            data_uri = encode_image_to_data_uri(
                raw=raw,
                content_type=content_type,
                max_dimension=max_dimension,
                jpeg_quality=jpeg_quality,
            )
            if not data_uri:
                continue
            if len(data_uri) > max_encoded_length:
                continue
            return data_uri, url
    return None, None


def ensure_schema(cursor: pymysql.cursors.Cursor) -> None:
    cursor.execute(
        """
        ALTER TABLE puja_samagri_master
            MODIFY COLUMN image_url LONGTEXT NULL
        """
    )
    cursor.execute(
        """
        ALTER TABLE puja_samagri_master_images
            MODIFY COLUMN image_url LONGTEXT NOT NULL
        """
    )


def fetch_rows(
    cursor: pymysql.cursors.Cursor,
    *,
    force: bool,
    limit: int | None,
) -> list[SamagriRow]:
    sql = """
        SELECT id, name, COALESCE(hi_name, ''), image_url
        FROM puja_samagri_master
        WHERE is_active = 1
    """
    params: list[object] = []
    if not force:
        sql += " AND (image_url IS NULL OR TRIM(image_url) = '' OR image_url NOT LIKE 'data:image%%')"
    sql += " ORDER BY id ASC"
    if limit is not None and limit > 0:
        sql += " LIMIT %s"
        params.append(limit)
    cursor.execute(sql, params)
    rows: list[SamagriRow] = []
    for row in cursor.fetchall():
        rows.append(
            SamagriRow(
                item_id=int(row[0]),
                name=str(row[1] or ""),
                hi_name=str(row[2] or ""),
                image_url=None if row[3] is None else str(row[3]),
            )
        )
    return rows


def upsert_images(
    cursor: pymysql.cursors.Cursor,
    *,
    item_id: int,
    data_uri: str,
) -> None:
    cursor.execute(
        """
        UPDATE puja_samagri_master
        SET image_url = %s, updated_at = NOW()
        WHERE id = %s
        """,
        (data_uri, item_id),
    )

    cursor.execute(
        """
        SELECT id
        FROM puja_samagri_master_images
        WHERE samagri_master_id = %s
        ORDER BY display_order ASC, id ASC
        """,
        (item_id,),
    )
    rows = cursor.fetchall()
    if rows:
        first_id = int(rows[0][0])
        cursor.execute(
            """
            UPDATE puja_samagri_master_images
            SET image_url = %s, display_order = 0, is_active = 1, updated_at = NOW()
            WHERE id = %s
            """,
            (data_uri, first_id),
        )
        if len(rows) > 1:
            extra_ids = [int(row[0]) for row in rows[1:]]
            placeholders = ",".join(["%s"] * len(extra_ids))
            cursor.execute(
                f"""
                UPDATE puja_samagri_master_images
                SET is_active = 0, updated_at = NOW()
                WHERE id IN ({placeholders})
                """,
                extra_ids,
            )
    else:
        cursor.execute(
            """
            INSERT INTO puja_samagri_master_images (
                samagri_master_id, image_url, display_order, is_active, created_at, updated_at
            ) VALUES (%s, %s, 0, 1, NOW(), NOW())
            """,
            (item_id, data_uri),
        )


def process_rows(
    connection: pymysql.connections.Connection,
    rows: Iterable[SamagriRow],
    *,
    max_results: int,
    timeout_seconds: int,
    max_download_bytes: int,
    max_dimension: int,
    jpeg_quality: int,
    max_encoded_length: int,
    sleep_seconds: float,
) -> tuple[int, int]:
    updated = 0
    failed = 0
    for row in rows:
        display_name = normalize_spaces(row.name) or f"ID-{row.item_id}"
        data_uri, source_url = choose_data_uri_for_item(
            name=row.name,
            hi_name=row.hi_name,
            max_results=max_results,
            timeout_seconds=timeout_seconds,
            max_download_bytes=max_download_bytes,
            max_dimension=max_dimension,
            jpeg_quality=jpeg_quality,
            max_encoded_length=max_encoded_length,
        )
        if not data_uri:
            failed += 1
            print(f"[FAIL] {row.item_id} {display_name} -> no usable image")
            continue

        with connection.cursor() as cursor:
            upsert_images(cursor, item_id=row.item_id, data_uri=data_uri)
        connection.commit()
        updated += 1
        source_preview = (source_url or "")[:120]
        print(
            f"[OK] {row.item_id} {display_name} -> saved base64 (len={len(data_uri)})"
            + (f" source={source_preview}" if source_preview else "")
        )
        if sleep_seconds > 0:
            time.sleep(sleep_seconds)
    return updated, failed


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Fill Puja Samagri images as base64 data URIs in DB",
    )
    parser.add_argument("--db-host", default="localhost")
    parser.add_argument("--db-port", type=int, default=3306)
    parser.add_argument("--db-user", default="root")
    parser.add_argument("--db-password", default="root")
    parser.add_argument("--db-name", default="astrodb")
    parser.add_argument("--force", action="store_true", help="Rebuild even if image already exists")
    parser.add_argument("--limit", type=int, default=None, help="Process only first N rows")
    parser.add_argument("--max-results", type=int, default=8, help="Search results per query")
    parser.add_argument("--timeout-seconds", type=int, default=20)
    parser.add_argument("--max-download-bytes", type=int, default=4_000_000)
    parser.add_argument("--max-dimension", type=int, default=900)
    parser.add_argument("--jpeg-quality", type=int, default=82)
    parser.add_argument("--max-encoded-length", type=int, default=900_000)
    parser.add_argument("--sleep-seconds", type=float, default=0.25)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    connection = pymysql.connect(
        host=args.db_host,
        port=args.db_port,
        user=args.db_user,
        password=args.db_password,
        database=args.db_name,
        charset="utf8mb4",
        autocommit=False,
    )
    try:
        with connection.cursor() as cursor:
            ensure_schema(cursor)
            rows = fetch_rows(cursor, force=args.force, limit=args.limit)
        connection.commit()

        if not rows:
            print("No samagri rows pending for image update.")
            return

        print(f"Processing {len(rows)} samagri rows...")
        updated, failed = process_rows(
            connection,
            rows,
            max_results=args.max_results,
            timeout_seconds=args.timeout_seconds,
            max_download_bytes=args.max_download_bytes,
            max_dimension=args.max_dimension,
            jpeg_quality=args.jpeg_quality,
            max_encoded_length=args.max_encoded_length,
            sleep_seconds=args.sleep_seconds,
        )
        print(f"Done. updated={updated}, failed={failed}")
    finally:
        connection.close()


if __name__ == "__main__":
    main()
