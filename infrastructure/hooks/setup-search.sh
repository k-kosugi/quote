#!/bin/sh
# azd の postprovision フックとして, AI Search にインデックスを作り直し, ナレッジのドキュメントを登録し,
# ナレッジソース (インデックス, API Management の MCP) と, それらを束ねるナレッジベースを作成する.
# インデックスは, 削除してから作成する. フィールドの属性 (searchable など) は, 既存のインデックスでは変更できないため.
# 直後にドキュメントを登録し直すので, 失われるデータはない.
# Bicep ではインデックスもドキュメントも作成できないため, データプレーンの REST API を呼び出す.
# 実行者には, AI Search の Search Service Contributor と Search Index Data Contributor のロールが必要.
# Bicep (search-roles.bicep) が実行者に付与するが, 反映に数分かかるため, 失敗した呼び出しは再試行する.
set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
API_VERSION="2025-09-01"
# MCP サーバーのナレッジソースと, LLM を使うナレッジベースは, プレビューの API が必要
KNOWLEDGE_API_VERSION="2026-08-01-preview"
KNOWLEDGE_DIR="$SCRIPT_DIR/../search/knowledge"
DOCUMENTS_FILE="$SCRIPT_DIR/../../catalog/knowledge/sku-documents.json"

: "${AZURE_SEARCH_ENDPOINT:?AZURE_SEARCH_ENDPOINT is not set}"
: "${AZURE_SEARCH_INDEX_NAME:?AZURE_SEARCH_INDEX_NAME is not set}"
: "${foundryEndpoint:?foundryEndpoint is not set}"
: "${foundryEmbeddingDeploymentName:?foundryEmbeddingDeploymentName is not set}"
: "${foundryEmbeddingModelName:?foundryEmbeddingModelName is not set}"
: "${foundryChatDeploymentName:?foundryChatDeploymentName is not set}"
: "${foundryChatModelName:?foundryChatModelName is not set}"
: "${apimGatewayUrl:?apimGatewayUrl is not set}"

# azd と同じ認証でトークンを取得する. 版によって, トークンだけを返す場合と JSON (token) で返す場合がある
output=$(azd auth token --scope https://search.azure.com/.default)
case "$output" in
  '{'*) token=$(printf '%s' "$output" | tr -d '\n' | sed -n 's/.*"token" *: *"\([^"]*\)".*/\1/p') ;;
  *) token=$output ;;
esac
if [ -z "$token" ]; then
  echo "Failed to get an access token for Azure AI Search. Run 'azd auth login'." >&2
  exit 1
fi

# 標準入力を本文として PUT または POST する. ロールの反映待ちを含め, 失敗したら再試行する
# 3 つ目の引数で API のバージョンを指定できる
call_search() {
  method=$1
  path=$2
  version=${3:-$API_VERSION}
  curl --silent --show-error --fail-with-body --retry 20 --retry-delay 15 --retry-all-errors \
    --request "$method" "$AZURE_SEARCH_ENDPOINT/$path?api-version=$version" \
    --header "Authorization: Bearer $token" \
    --header "Content-Type: application/json" \
    --data @-
}

# インデックスを参照するナレッジソースとナレッジベースを, 先に削除する. 存在しない場合 (404) は何もしない
delete_knowledge() {
  curl --silent --show-error --output /dev/null \
    --request DELETE "$AZURE_SEARCH_ENDPOINT/$1?api-version=$KNOWLEDGE_API_VERSION" \
    --header "Authorization: Bearer $token"
}
delete_knowledge "knowledgebases/catalog-kb"
delete_knowledge "knowledgesources/sku-index-ks"
delete_knowledge "knowledgesources/catalog-mcp-ks"

echo "Recreating index '$AZURE_SEARCH_INDEX_NAME' on $AZURE_SEARCH_ENDPOINT"
# 存在しない場合 (404) は, そのまま作成する
curl --silent --show-error --output /dev/null --retry 20 --retry-delay 15 --retry-all-errors \
  --request DELETE "$AZURE_SEARCH_ENDPOINT/indexes/$AZURE_SEARCH_INDEX_NAME?api-version=$API_VERSION" \
  --header "Authorization: Bearer $token"
# vectorizer は, AI Search のマネージド ID で Foundry の埋め込みモデルを呼び出す. 呼び出し先はテンプレートの置換で渡す
sed -e "s|__INDEX_NAME__|$AZURE_SEARCH_INDEX_NAME|" \
    -e "s|__FOUNDRY_ENDPOINT__|$foundryEndpoint|" \
    -e "s|__EMBEDDING_DEPLOYMENT__|$foundryEmbeddingDeploymentName|" \
    -e "s|__EMBEDDING_MODEL__|$foundryEmbeddingModelName|" \
    "$SCRIPT_DIR/../search/sku-index.json" \
  | call_search PUT "indexes/$AZURE_SEARCH_INDEX_NAME" > /dev/null
echo "Index '$AZURE_SEARCH_INDEX_NAME' is ready."

if [ ! -f "$DOCUMENTS_FILE" ]; then
  echo "Skipped uploading documents: $DOCUMENTS_FILE not found." >&2
  exit 0
fi

echo "Uploading documents from $DOCUMENTS_FILE"
response=$(call_search POST "indexes/$AZURE_SEARCH_INDEX_NAME/docs/index" < "$DOCUMENTS_FILE")
# 一部のドキュメントだけ失敗した場合は 207 で返り, curl では失敗にならない
if printf '%s' "$response" | grep -q '"status" *: *false'; then
  echo "Some documents failed to upload: $response" >&2
  exit 1
fi
echo "Documents are uploaded."

echo "Creating knowledge sources and knowledge base"
# ナレッジソースは, ナレッジベースより先に作成する
sed -e "s|__INDEX_NAME__|$AZURE_SEARCH_INDEX_NAME|" "$KNOWLEDGE_DIR/sku-index-ks.json" \
  | call_search PUT "knowledgesources/sku-index-ks" "$KNOWLEDGE_API_VERSION" > /dev/null
sed -e "s|__CATALOG_MCP_URL__|$apimGatewayUrl/catalog-mcp/mcp|" "$KNOWLEDGE_DIR/catalog-mcp-ks.json" \
  | call_search PUT "knowledgesources/catalog-mcp-ks" "$KNOWLEDGE_API_VERSION" > /dev/null
sed -e "s|__FOUNDRY_ENDPOINT__|$foundryEndpoint|" \
    -e "s|__CHAT_DEPLOYMENT__|$foundryChatDeploymentName|" \
    -e "s|__CHAT_MODEL__|$foundryChatModelName|" \
    "$KNOWLEDGE_DIR/catalog-kb.json" \
  | call_search PUT "knowledgebases/catalog-kb" "$KNOWLEDGE_API_VERSION" > /dev/null
echo "Knowledge base 'catalog-kb' is ready."
