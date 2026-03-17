#!/usr/bin/env python3
"""
Import Puja Samagri products into backend from a WordPress/WooCommerce product listing page.

This is useful when Shopify's `products.json` endpoint is not available.

Notes:
- Only run this if you have permission to copy product data/images.
- This script scrapes a product listing like:
    https://adhyaystore.com/?post_type=product&paged=1
- It then imports into:
    http://localhost:1234/api/web/puja-samagri/master
  which requires an ADMIN JWT (`--admin-token`).
"""

from __future__ import annotations

import argparse
import html as html_lib
import json
import re
import ssl
import sys
import urllib.parse
import urllib.request
from dataclasses import dataclass
from html.parser import HTMLParser


def fetch_html(url: str, *, insecure: bool = False) -> str:
    context = ssl._create_unverified_context() if insecure else None
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "AstrologerSamagriImporter/1.0",
            "Accept": "text/html,*/*",
        },
    )
    with urllib.request.urlopen(req, timeout=40, context=context) as resp:
        return resp.read().decode("utf-8", errors="ignore")


def post_json(url: str, payload: dict, token: str) -> dict:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=40) as resp:
        raw = resp.read().decode("utf-8", errors="ignore")
        if not raw.strip():
            return {"status": True}
        return json.loads(raw)


def parse_price(text: str) -> float | None:
    if not text:
        return None
    cleaned = text.strip()
    cleaned = cleaned.replace(",", "")
    cleaned = re.sub(r"[^0-9.]", "", cleaned)
    if not cleaned:
        return None
    try:
        return float(cleaned)
    except Exception:
        return None


def normalize_url(base_url: str, href: str) -> str:
    if href.startswith("//"):
        return "https:" + href
    if href.startswith("/"):
        return base_url.rstrip("/") + href
    return href


def is_product_url(href: str) -> bool:
    return "/product/" in href


@dataclass
class ScrapedProduct:
    url: str
    title: str
    image_url: str | None = None
    original_price: float | None = None
    sale_price: float | None = None
    discount_pct: float | None = None

    def resolved_price(self) -> float:
        if self.original_price and self.original_price > 0:
            return self.original_price
        if self.sale_price and self.sale_price > 0:
            return self.sale_price
        return 0.0

    def resolved_discount_pct(self) -> float | None:
        if self.discount_pct and self.discount_pct > 0:
            return self.discount_pct
        if (
            self.original_price
            and self.sale_price
            and self.original_price > 0
            and self.sale_price > 0
            and self.sale_price < self.original_price
        ):
            return round(((self.original_price - self.sale_price) / self.original_price) * 100.0, 2)
        return None


class WooListingParser(HTMLParser):
    def __init__(self, base_url: str) -> None:
        super().__init__()
        self.base_url = base_url.rstrip("/")
        self.current_a_href: str | None = None
        self.in_h3 = False
        self.in_del = False
        self.in_ins = False
        self.in_bdi = False
        self.active_product_url: str | None = None
        self.products: dict[str, ScrapedProduct] = {}

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attr = {k: (v or "") for k, v in attrs}
        if tag == "h3":
            self.in_h3 = True
            return
        if tag == "a":
            href = attr.get("href", "").strip()
            self.current_a_href = normalize_url(self.base_url, href) if href else None
            return
        if tag == "img":
            if not self.current_a_href:
                return
            href = self.current_a_href
            if not is_product_url(href):
                return
            src = attr.get("src", "").strip()
            if not src:
                return
            src = normalize_url(self.base_url, src)
            prod = self.products.get(href)
            if prod is None:
                prod = ScrapedProduct(url=href, title="")
                self.products[href] = prod
            if not prod.image_url:
                prod.image_url = src
            return
        if tag == "del":
            self.in_del = True
            return
        if tag == "ins":
            self.in_ins = True
            return
        if tag == "bdi":
            self.in_bdi = True
            return

    def handle_endtag(self, tag: str) -> None:
        if tag == "h3":
            self.in_h3 = False
            return
        if tag == "a":
            self.current_a_href = None
            return
        if tag == "del":
            self.in_del = False
            return
        if tag == "ins":
            self.in_ins = False
            return
        if tag == "bdi":
            self.in_bdi = False
            return

    def handle_data(self, data: str) -> None:
        text = (data or "").strip()
        if not text:
            return

        if self.current_a_href and is_product_url(self.current_a_href) and not self.in_h3:
            m = re.search(r"-\s*(\d{1,2})\s*%", text)
            if m:
                prod = self.products.get(self.current_a_href)
                if prod is None:
                    prod = ScrapedProduct(url=self.current_a_href, title="")
                    self.products[self.current_a_href] = prod
                if prod.discount_pct is None:
                    try:
                        prod.discount_pct = float(m.group(1))
                    except Exception:
                        prod.discount_pct = None

        if self.in_h3 and self.current_a_href and is_product_url(self.current_a_href):
            url = self.current_a_href
            title = html_lib.unescape(text)
            prod = self.products.get(url)
            if prod is None:
                prod = ScrapedProduct(url=url, title=title)
                self.products[url] = prod
            if not prod.title:
                prod.title = title
            self.active_product_url = url
            return

        if self.in_bdi and self.active_product_url:
            price = parse_price(text)
            if price is None:
                return
            prod = self.products.get(self.active_product_url)
            if prod is None:
                prod = ScrapedProduct(url=self.active_product_url, title="")
                self.products[self.active_product_url] = prod
            if self.in_del:
                prod.original_price = price
            elif self.in_ins:
                prod.sale_price = price


def scrape_page(base_url: str, url: str, *, insecure: bool = False) -> list[ScrapedProduct]:
    html_text = fetch_html(url, insecure=insecure)
    parser = WooListingParser(base_url=base_url)
    parser.feed(html_text)
    products = []
    for p in parser.products.values():
        if not p.title.strip():
            continue
        products.append(p)
    return products


def build_listing_url(base_url: str, page: int) -> str:
    base = base_url.rstrip("/")
    query = urllib.parse.urlencode({"post_type": "product", "paged": str(page)})
    return f"{base}/?{query}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True, help="e.g. https://adhyaystore.com")
    parser.add_argument(
        "--admin-endpoint",
        required=True,
        help="e.g. http://localhost:1234/api/web/puja-samagri/master",
    )
    parser.add_argument(
        "--admin-token",
        default="",
        help="Admin JWT token for /api/web/** endpoints (Bearer <token>)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Scrape and print items without importing to backend",
    )
    parser.add_argument(
        "--insecure",
        action="store_true",
        help="Disable SSL certificate verification when scraping (use only if required).",
    )
    parser.add_argument("--max-pages", type=int, default=20)
    parser.add_argument("--limit", type=int, default=500)
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    token = args.admin_token.strip()
    if not args.dry_run and not token:
        print("Missing --admin-token (or use --dry-run).", file=sys.stderr)
        return 5

    seen: set[str] = set()
    imported = 0
    failed = 0

    for page in range(1, max(1, args.max_pages) + 1):
        if imported >= args.limit:
            break
        listing_url = build_listing_url(base_url, page)
        try:
            products = scrape_page(base_url, listing_url, insecure=args.insecure)
        except Exception as exc:
            print(f"Failed to fetch/scrape page {page} ({listing_url}): {exc}", file=sys.stderr)
            break

        new_items = [p for p in products if p.url not in seen]
        if not new_items:
            break

        for p in new_items:
            if imported >= args.limit:
                break
            seen.add(p.url)

            price = p.resolved_price()
            discount = p.resolved_discount_pct()
            image_url = p.image_url or ""

            payload = {
                "name": p.title,
                "hiName": p.title,
                "description": "",
                "hiDescription": "",
                "price": price,
                "discountPercentage": discount,
                "currency": "INR",
                "imageUrl": image_url,
                "imageUrls": [image_url] if image_url else [],
                "shopEnabled": True,
                "isActive": True,
            }

            try:
                if args.dry_run:
                    imported += 1
                    print(
                        f"Scraped: {p.title} price={price} discount={discount or 0} image={image_url}"
                    )
                else:
                    post_json(args.admin_endpoint, payload, token=token)
                    imported += 1
                    print(f"Imported: {p.title}")
            except Exception as exc:
                failed += 1
                print(f"Failed: {p.title}: {exc}", file=sys.stderr)

    print(f"Done. imported={imported} failed={failed}")
    return 0 if failed == 0 else 4


if __name__ == "__main__":
    raise SystemExit(main())
