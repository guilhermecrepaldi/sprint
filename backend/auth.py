import os
import secrets
import hmac
from fastapi import Header, HTTPException, status
from db import settings

_API_KEY: str | None = None


def get_api_key() -> str:
    global _API_KEY
    if _API_KEY is None:
        raw = os.environ.get("SPRINT_API_KEY", "")
        _API_KEY=*** if raw else secrets.token_hex(32)
    return _API_KEY


async def verify_api_key(x_api_key: str | None = Header(None)) -> None:
    if getattr(settings, "auth_enabled", True) is False:
        return
    expected = get_api_key()
    if x_api_key is None:
        raise HTTPException(status_code=401, detail="Missing X-API-Key header")
    if not hmac.compare_digest(x_api_key, expected):
        raise HTTPException(status_code=403, detail="Invalid API key")


async def no_auth() -> None:
    return None
