"""knowledge 配下の Markdown をベクトル化し, AI Search に登録できる JSON を書き出す.

使い方 (リポジトリの catalog ディレクトリで実行する):
    eval "$(azd env get-values | sed 's/^/export /')"
    python scripts/build_knowledge_json.py

認証は Azure CLI (az login) のトークンを使う. 実行者には, Foundry アカウントの
Cognitive Services OpenAI User などの, 埋め込みを呼び出せるロールが必要.
"""

import argparse
import json
import os
import subprocess
import urllib.request
from pathlib import Path

KNOWLEDGE_DIR = Path(__file__).resolve().parent.parent / "knowledge"
DEFAULT_OUTPUT = KNOWLEDGE_DIR / "sku-documents.json"
DEFAULT_DEPLOYMENT = "text-embedding-3-small"
EMBEDDING_DIMENSIONS = 1536
EMBEDDING_API_VERSION = "2024-10-21"
TOKEN_RESOURCE = "https://cognitiveservices.azure.com"


def get_access_token() -> str:
    """Azure CLI から, 埋め込みの呼び出しに使うアクセストークンを取得する."""
    result = subprocess.run(
        ["az", "account", "get-access-token", "--resource", TOKEN_RESOURCE, "--query", "accessToken", "-o", "tsv"],
        check=True, capture_output=True, text=True,
    )
    return result.stdout.strip()


def embed(texts: list[str], endpoint: str, deployment: str, token: str) -> list[list[float]]:
    """テキストの一覧を, 1 回の呼び出しで埋め込みに変換する. 入力と同じ順序で返す."""
    url = f"{endpoint.rstrip('/')}/openai/deployments/{deployment}/embeddings?api-version={EMBEDDING_API_VERSION}"
    request = urllib.request.Request(
        url,
        data=json.dumps({"input": texts}).encode("utf-8"),
        headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        data = json.load(response)["data"]
    return [item["embedding"] for item in sorted(data, key=lambda item: item["index"])]


def build_documents(endpoint: str, deployment: str) -> list[dict]:
    """Markdown ごとに 1 件のドキュメントを作る. ファイル名 (拡張子なし) を SKU_ID にする."""
    files = sorted(KNOWLEDGE_DIR.glob("*.md"))
    if not files:
        raise SystemExit(f"No markdown files in {KNOWLEDGE_DIR}")
    summaries = [file.read_text(encoding="utf-8").strip() for file in files]
    vectors = embed(summaries, endpoint, deployment, get_access_token())
    for vector in vectors:
        if len(vector) != EMBEDDING_DIMENSIONS:
            raise SystemExit(f"Expected {EMBEDDING_DIMENSIONS} dimensions but got {len(vector)}.")
    return [
        {"@search.action": "mergeOrUpload", "SKU_ID": file.stem, "SUMMARY": summary, "SUMMARY_VECTOR": vector}
        for file, summary, vector in zip(files, summaries, vectors)
    ]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--endpoint", default=os.environ.get("foundryEndpoint"),
                        help="Foundry のエンドポイント. 未指定の場合は環境変数 foundryEndpoint")
    parser.add_argument("--deployment", default=os.environ.get("foundryEmbeddingDeploymentName", DEFAULT_DEPLOYMENT),
                        help="埋め込みモデルのデプロイ名")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="書き出す JSON のパス")
    args = parser.parse_args()
    if not args.endpoint:
        raise SystemExit("Foundry endpoint is required (--endpoint or the foundryEndpoint environment variable).")

    documents = build_documents(args.endpoint, args.deployment)
    args.output.write_text(json.dumps({"value": documents}, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {len(documents)} documents to {args.output}")


if __name__ == "__main__":
    main()
