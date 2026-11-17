"""読み取り専用の SKU カタログをメモリで保持する."""

from decimal import Decimal
from typing import Iterable, Optional

from app.domain import Sku, SkuId


class InMemorySkuRepository:
    """入力一覧を SKU ID 順に並べて保持する読み取り専用の保存先."""

    def __init__(self, skus: Iterable[Sku]) -> None:
        self._skus = tuple(sorted(skus, key=lambda sku: sku.id.value))

    def find_all(self) -> list[Sku]:
        return list(self._skus)

    def find_by_id(self, sku_id: SkuId) -> Optional[Sku]:
        return next((sku for sku in self._skus if sku.id == sku_id), None)


def sample_skus() -> list[Sku]:
    """見積のデモに使うサンプル SKU を返す."""
    return [
        Sku(SkuId("SKU-A"), Decimal("1000"), "Core service plan."),
        Sku(SkuId("SKU-B"), Decimal("2000"), "Extended service plan."),
        Sku(SkuId("SKU-C"), Decimal("500"), "Optional add-on."),
        Sku(SkuId("SKU-D"), Decimal("1500"), "Additional add-on."),
        Sku(SkuId("SKU-STANDARD"), Decimal("1000"), "Standard subscription plan."),
        Sku(SkuId("SKU-PROFESSIONAL"), Decimal("3000"), "Professional subscription plan."),
    ]
