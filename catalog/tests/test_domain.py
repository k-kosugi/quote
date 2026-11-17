from decimal import Decimal

import pytest

from app.domain import Sku, SkuId


def test_SKU_IDの前後の空白が取り除かれる():
    assert SkuId("  SKU-A  ").value == "SKU-A"


@pytest.mark.parametrize("value", ["", "   ", None])
def test_SKU_IDが空の場合ValueErrorが発生する(value):
    with pytest.raises(ValueError):
        SkuId(value)


def test_月額価格の境界値前後が渡された場合境界に応じた結果になる():
    assert Sku(SkuId("A"), Decimal("0"), "plan").list_price == Decimal("0")
    assert Sku(SkuId("A"), Decimal("1"), "plan").list_price == Decimal("1")
    with pytest.raises(ValueError):
        Sku(SkuId("A"), Decimal("-1"), "plan")


@pytest.mark.parametrize("description", ["", "   ", None])
def test_概要が空の場合ValueErrorが発生する(description):
    with pytest.raises(ValueError):
        Sku(SkuId("A"), Decimal("1"), description)


def test_概要の前後の空白が取り除かれる():
    assert Sku(SkuId("A"), Decimal("1"), "  plan  ").description == "plan"
