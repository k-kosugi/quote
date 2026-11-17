"""アプリケーションの起点. サンプルカタログを接続する."""

from app.api import create_app
from app.repository import InMemorySkuRepository, sample_skus

app = create_app(InMemorySkuRepository(sample_skus()))
