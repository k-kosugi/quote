from decimal import Decimal

from app.domain import Sku, SkuId
from app.repository import InMemorySkuRepository


def _repository() -> InMemorySkuRepository:
    return InMemorySkuRepository([
        Sku(SkuId("SKU-B"), Decimal("2000"), "b"),
        Sku(SkuId("SKU-A"), Decimal("1000"), "a"),
    ])


def test_一覧がSKU_ID順で返る():
    assert [sku.id.value for sku in _repository().find_all()] == ["SKU-A", "SKU-B"]


def test_登録済みのSKU_IDが渡された場合SKUを返す():
    assert _repository().find_by_id(SkuId("SKU-A")).list_price == Decimal("1000")


def test_大文字小文字が異なるSKU_IDが渡された場合Noneを返す():
    assert _repository().find_by_id(SkuId("sku-a")) is None


def test_未登録のSKU_IDが渡された場合Noneを返す():
    assert _repository().find_by_id(SkuId("SKU-X")) is None
