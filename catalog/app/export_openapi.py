"""実装から OpenAPI の仕様を JSON として標準出力へ書き出す. 使い方: python -m app.export_openapi [サーバーの URL]."""

import json
import sys

from app.api import build_openapi
from app.main import app

DEFAULT_SERVER_URL = "http://localhost:8000"


def main() -> None:
    server_url = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SERVER_URL
    sys.stdout.write(json.dumps(build_openapi(app, server_url), ensure_ascii=False, indent=4) + "\n")


if __name__ == "__main__":
    main()
