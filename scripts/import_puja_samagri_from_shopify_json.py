#!/usr/bin/env python3
# Import Puja Samagri products into backend.
#
# Notes:
# - Only run this if you have permission to copy product data/images.
# - This tries Shopify's public `products.json` endpoint. If your store is not Shopify,
#   export products to JSON/CSV and write a small mapper.
#
# Usage:
#   python3 scripts/import_puja_samagri_from_shopify_json.py #     --base-url https://adhyaystore.com #     --admin-endpoint http://localhost:1234/api/web/puja-samagri/master

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.request


def strip_html(value: str) -> str:
    if not value:
        return ""
    text = re.sub(r"<[^>]+>", " ", value)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def fetch_json(url: str) -> dict:
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "AstrologerSamagriImporter/1.0",
            "Accept": "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=40) as resp:
        return json.loads(resp.read().decode("utf-8", errors="ignore"))


def post_json(url: str, payload: dict, token: str | None = None) -> dict:
    data = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=40) as resp:
        return json.loads(resp.read().decode("utf-8", errors="ignore"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--admin-endpoint", required=True)
    parser.add_argument(
        "--admin-token",
        default="",
        help="Admin JWT token for /api/web/** endpoints (Bearer <token>)",
    )
    parser.add_argument("--limit", type=int, default=250)
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    products_url = f"{base_url}/products.json?limit={args.limit}"
    admin_token = (args.admin_token or "").strip()
    if not admin_token:
        print(
            "Missing --admin-token. /api/web/** requires ADMIN JWT.",
            file=sys.stderr,
        )
        print(
            "Get token via POST /api/web/auth/login, then rerun with --admin-token <token>.",
            file=sys.stderr,
        )
        return 5

    try:
        data = fetch_json(products_url)
    except Exception as exc:
        print(f"Failed to fetch {products_url}: {exc}", file=sys.stderr)
        return 2

    products = data.get("products") or []
    if not products:
        print(
            "No products found in products.json. If store is not Shopify, export a JSON and map manually.",
            file=sys.stderr,
        )
        return 3

    ok = 0
    failed = 0
    for p in products:
        title = (p.get("title") or "").strip()
        if not title:
            continue

        body = strip_html(p.get("body_html") or "")
        variants = p.get("variants") or []
        price = None
        discount = None
        if variants:
            try:
                price = float(variants[0].get("price") or 0)
            except Exception:
                price = 0.0
            try:
                compare_at = variants[0].get("compare_at_price")
                if compare_at:
                    compare_at = float(compare_at)
                    if compare_at > 0 and price is not None and compare_at > price:
                        discount = round(((compare_at - price) / compare_at) * 100.0, 2)
            except Exception:
                discount = None

        images = p.get("images") or []
        image_urls = []
        for img in images:
            if not isinstance(img, dict):
                continue
            src = (img.get("src") or "").strip()
            if src:
                image_urls.append(src)
        image_url = image_urls[0] if image_urls else None

        payload = {
            "name": title,
            "hiName": title,
            "description": body,
            "hiDescription": body,
            "price": price,
            "discountPercentage": discount,
            "currency": "INR",
            "imageUrl": image_url,
            "imageUrls": image_urls,
            "shopEnabled": True,
            "isActive": True,
        }

        try:
            post_json(args.admin_endpoint, payload, token=admin_token)
            ok += 1
            print(f"Imported: {title}")
        except Exception as exc:
            failed += 1
            print(f"Failed: {title}: {exc}", file=sys.stderr)

    print(f"Done. ok={ok} failed={failed}")
    return 0 if failed == 0 else 4


if __name__ == "__main__":
    raise SystemExit(main())
