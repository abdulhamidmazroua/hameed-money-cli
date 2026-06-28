#!/usr/bin/env python3
import json
import sys
import re
import yfinance as yf

ISIN_RE = re.compile(r'^[A-Z]{2}[A-Z0-9]{9}\d$')


def is_isin(s):
    return bool(ISIN_RE.match(s.upper()))


def resolve_isin(isin):
    try:
        df = yf.download(isin, period='1d', progress=False)
        if not df.empty:
            return isin
    except Exception:
        pass
    try:
        for q in yf.Search(isin).quotes or []:
            return q.get('symbol', isin)
    except Exception:
        pass
    return isin


def fetch_price(identifier):
    try:
        ticker = yf.Ticker(identifier)
        info = ticker.info
        price = (info.get("regularMarketPrice") or
                 info.get("currentPrice") or
                 info.get("previousClose"))
        if price is not None:
            return float(price)
    except Exception:
        pass
    try:
        df = yf.download(identifier, period='1d', progress=False)
        if not df.empty:
            return float(df['Close'].iloc[-1])
    except Exception:
        pass
    return None


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Usage: yahoo_price.py <symbol>"}))
        sys.exit(1)

    symbol = sys.argv[1]

    if is_isin(symbol):
        symbol = resolve_isin(symbol)

    price = fetch_price(symbol)
    if price is None:
        print(json.dumps({"error": f"No price data for {symbol}"}))
        sys.exit(1)
    print(json.dumps({"price": price}))


if __name__ == "__main__":
    main()
