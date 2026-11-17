import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


def test_SKU一覧APIが呼び出された場合IDと月額価格と概要が返る(client):
    response = client.get("/api/skus")

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("application/json")
    assert response.json() == [
        {"id": "SKU-A", "listPrice": 1000, "description": "Core service plan."},
        {"id": "SKU-B", "listPrice": 2000, "description": "Extended service plan."},
        {"id": "SKU-C", "listPrice": 500, "description": "Optional add-on."},
        {"id": "SKU-D", "listPrice": 1500, "description": "Additional add-on."},
        {"id": "SKU-PROFESSIONAL", "listPrice": 3000, "description": "Professional subscription plan."},
        {"id": "SKU-STANDARD", "listPrice": 1000, "description": "Standard subscription plan."},
    ]


def test_月額価格は小数点なしの数値で返る(client):
    assert client.get("/api/skus/SKU-A").text == (
        '{"id":"SKU-A","listPrice":1000,"description":"Core service plan."}'
    )


def test_登録済みのSKU_IDが渡された場合SKUが返る(client):
    response = client.get("/api/skus/SKU-STANDARD")

    assert response.status_code == 200
    assert response.json()["listPrice"] == 1000


@pytest.mark.parametrize("sku_id", ["SKU-UNKNOWN", "sku-a", "%20"])
def test_存在しないSKU_IDが渡された場合404が返る(client, sku_id):
    assert client.get(f"/api/skus/{sku_id}").status_code == 404


def test_ヘルスチェックが呼び出された場合okが返る(client):
    response = client.get("/healthz")

    assert response.status_code == 200
    assert response.text == "ok"


def test_OpenAPI仕様取得APIが呼び出された場合実装に基づく仕様が返る(client):
    specification = client.get("/v3/api-docs").json()

    assert specification["info"]["title"] == "SKU カタログ API"
    assert specification["paths"]["/api/skus"]["get"]["operationId"] == "listSkus"
    assert specification["paths"]["/api/skus/{sku_id}"]["get"]["operationId"] == "getSku"
    assert "/healthz" not in specification["paths"]
    assert "/v3/api-docs" not in specification["paths"]


def test_OpenAPI仕様のserversに取得元のURLが入る(client):
    specification = client.get("/v3/api-docs", headers={"host": "catalog.example.com"}).json()

    assert specification["servers"][0]["url"] == "http://catalog.example.com"


def test_Swagger_UIが呼び出された場合仕様を参照する画面が返る(client):
    response = client.get("/swagger-ui")

    assert response.status_code == 200
    assert "swagger-ui" in response.text
    assert "/v3/api-docs" in response.text


def test_OpenAPI仕様に到達しない検証エラーが含まれない(client):
    specification = client.get("/v3/api-docs").json()

    assert "422" not in specification["paths"]["/api/skus/{sku_id}"]["get"]["responses"]
    assert set(specification["components"]["schemas"]) == {"ErrorResponse", "SkuResponse"}


def test_openapi_jsonが実装と一致する(client):
    exported = json.loads((Path(__file__).parent.parent / "openapi.json").read_text(encoding="utf-8"))
    generated = client.get("/v3/api-docs").json()

    # 呼び出し先の URL は環境ごとに異なるため比較しない
    exported.pop("servers")
    generated.pop("servers")
    assert exported == generated
