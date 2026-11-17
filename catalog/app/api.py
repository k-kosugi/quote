"""SKU カタログを HTTP API として公開する."""

from typing import Annotated, Any, Union

from fastapi import APIRouter, FastAPI, HTTPException, Path, Request
from fastapi.openapi.docs import get_swagger_ui_html
from fastapi.responses import HTMLResponse, JSONResponse, PlainTextResponse
from pydantic import BaseModel, ConfigDict, Field

from app.domain import Sku, SkuId, SkuRepository

TAG = "SKU カタログ"


class SkuResponse(BaseModel):
    """SKU の ID と円建て月額価格および概要を公開する DTO."""

    model_config = ConfigDict(title="SkuResponse")

    id: str = Field(min_length=1, description="一意の SKU ID. 見積の SKU 照合では大文字と小文字を区別します.")
    listPrice: Union[int, float] = Field(
        json_schema_extra={"minimum": 0},
        description="サンプルの月額単価 (JPY). 見積作成時にはこの値が listPrice として自動で適用されます.",
    )
    description: str = Field(min_length=1, description="SKU の概要説明")

    @classmethod
    def from_sku(cls, sku: Sku) -> "SkuResponse":
        """カタログの項目を公開用 DTO に変換する. 整数の価格は整数のまま返す."""
        price = sku.list_price
        list_price = int(price) if price == price.to_integral_value() else float(price)
        return cls(id=sku.id.value, listPrice=list_price, description=sku.description)


class ErrorResponse(BaseModel):
    """エラーの理由を伝える DTO."""

    model_config = ConfigDict(title="ErrorResponse")

    detail: str = Field(description="エラーの理由")


SKU_ID_PATH = Annotated[
    str,
    Path(description="SKU ID. 前後の空白は取り除き, 大文字と小文字を区別して照合します.", examples=["SKU-A"]),
]


def create_router(repository: SkuRepository) -> APIRouter:
    """カタログの取得元を受け取り, SKU の API を組み立てる."""
    router = APIRouter(prefix="/api/skus", tags=[TAG])

    @router.get(
        "",
        operation_id="listSkus",
        summary="SKU ID, 月額単価, 説明の一覧を取得",
        description=(
            "リクエストボディやパラメーターは不要です.\n"
            "読み取り専用のインメモリサンプルカタログを SKU ID の昇順で返します.\n"
            "見積作成時は, ここに載っている SKU ID の月額単価が自動で適用されます."
        ),
        response_model=list[SkuResponse],
        responses={200: {"description": "SKU ID, 月額単価 (JPY), 説明を含む一覧. カタログが空の場合は空の配列を返します."}},
    )
    def list_skus() -> list[SkuResponse]:
        return [SkuResponse.from_sku(sku) for sku in repository.find_all()]

    @router.get(
        "/{sku_id}",
        operation_id="getSku",
        summary="SKU ID を指定して SKU を取得",
        description="SKU ID が完全に一致する SKU を返します. 大文字と小文字は区別します.",
        response_model=SkuResponse,
        responses={
            200: {"description": "SKU ID, 月額単価 (JPY), 説明."},
            404: {"model": ErrorResponse, "description": "指定した SKU ID がカタログに存在しません."},
        },
    )
    def get_sku(sku_id: SKU_ID_PATH) -> SkuResponse:
        try:
            requested = SkuId(sku_id)
        except ValueError:
            raise HTTPException(status_code=404, detail="SKU not found.")
        sku = repository.find_by_id(requested)
        if sku is None:
            raise HTTPException(status_code=404, detail="SKU not found.")
        return SkuResponse.from_sku(sku)

    return router


def build_openapi(app: FastAPI, server_url: str) -> dict[str, Any]:
    """実装から OpenAPI の仕様を組み立て, 呼び出し先の URL を servers に入れる.

    パスの値は文字列でどの値も受け付けるため, 到達しない検証エラー (422) は仕様から外す.
    """
    specification = app.openapi()
    for path in specification["paths"].values():
        for operation in path.values():
            operation["responses"].pop("422", None)
    for name in ("HTTPValidationError", "ValidationError"):
        specification["components"]["schemas"].pop(name, None)
    return {**specification, "servers": [{"url": server_url, "description": "Generated server url"}]}


def create_app(repository: SkuRepository) -> FastAPI:
    """カタログの取得元から FastAPI アプリケーションを構成する."""
    app = FastAPI(
        title="SKU カタログ API",
        version="1.0.0",
        description=(
            "見積で利用可能なサンプル SKU 情報を提供する読み取り専用の API です.\n"
            "カタログはプロセス内メモリに保持し, 認証は行いません.\n"
            "SKU の月額単価は JPY で, 見積 API が見積の作成と修正のたびにこの API から取得します."
        ),
        openapi_url=None,
    )
    app.include_router(create_router(repository))

    @app.get("/v3/api-docs", include_in_schema=False)
    def api_docs(request: Request) -> JSONResponse:
        # Foundry の OpenAPI ツールが呼び出し先を判別できるよう, 取得元の URL を servers に入れる
        return JSONResponse(build_openapi(app, str(request.base_url).rstrip("/")))

    @app.get("/swagger-ui", include_in_schema=False)
    def swagger_ui() -> HTMLResponse:
        return get_swagger_ui_html(openapi_url="/v3/api-docs", title=f"{app.title} - Swagger UI")

    @app.get("/healthz", include_in_schema=False, response_class=PlainTextResponse)
    def healthz() -> str:
        return "ok"

    return app
