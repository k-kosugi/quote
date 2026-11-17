FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN useradd --system --no-create-home catalog
WORKDIR /app

# 依存関係を先にインストールし, ソースの変更時にキャッシュを再利用する
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app app
USER catalog

EXPOSE 8080
# Container Apps の ingress 経由でも, 仕様の servers に https の URL が入るよう転送ヘッダーを信頼する
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080", "--proxy-headers", "--forwarded-allow-ips", "*"]
