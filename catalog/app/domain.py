"""SKU カタログのドメインモデルと, 取得元を抽象化するポート."""

from dataclasses import dataclass
from decimal import Decimal
from typing import Optional, Protocol


@dataclass(frozen=True)
class SkuId:
    """カタログ内で SKU を一意に識別する値. 空の識別子を拒否し, 前後の空白を取り除く."""

    value: str

    def __post_init__(self) -> None:
        if not isinstance(self.value, str) or not self.value.strip():
            raise ValueError("SKU ID is required.")
        object.__setattr__(self, "value", self.value.strip())


@dataclass(frozen=True)
class Sku:
    """月額価格と概要を管理するカタログの項目. 非負の月額価格と概要を必須とする."""

    id: SkuId
    list_price: Decimal
    description: str

    def __post_init__(self) -> None:
        if self.list_price is None or self.list_price < 0:
            raise ValueError("Monthly list price must be zero or greater.")
        if not isinstance(self.description, str) or not self.description.strip():
            raise ValueError("SKU description is required.")
        object.__setattr__(self, "description", self.description.strip())


class SkuRepository(Protocol):
    """SKU カタログの取得元を抽象化するポート."""

    def find_all(self) -> list[Sku]:
        """SKU ID の昇順でカタログ一覧を返す."""

    def find_by_id(self, sku_id: SkuId) -> Optional[Sku]:
        """SKU ID が完全に一致する SKU を返す. 大文字と小文字は区別する."""
