using './main.bicep'

param location = 'swedencentral'
param workloadName = 'iandm'
param tags = {
  application: 'quote'
}

// Container Apps
param quoteAppName = 'quote'
param catalogAppName = 'catalog'
param frontendAppName = 'frontend'

// azd が deploy 後に SERVICE_<サービス名>_IMAGE_NAME へ記録したイメージを再利用する.
// 初回は ACR が空のため, 公開のプレースホルダーを指定し, azd deploy が実際のイメージに差し替える.
var placeholderImage = 'mcr.microsoft.com/k8se/quickstart:latest'
param quoteImage = readEnvironmentVariable('SERVICE_QUOTE_IMAGE_NAME', placeholderImage)
param catalogImage = readEnvironmentVariable('SERVICE_CATALOG_IMAGE_NAME', placeholderImage)
param frontendImage = readEnvironmentVariable('SERVICE_FRONTEND_IMAGE_NAME', placeholderImage)

param quoteTargetPort = 8080
param quoteCpu = '0.5'
param quoteMemory = '1Gi'
param quoteMinReplicas = 1
param quoteMaxReplicas = 1
// quote は Foundry のエージェントから匿名で呼び出すため外部公開する. 認証は行わない.
param quoteExternalIngress = true

param catalogTargetPort = 8080
param catalogCpu = '0.25'
param catalogMemory = '0.5Gi'
param catalogMinReplicas = 1
param catalogMaxReplicas = 3
// catalog は Foundry のエージェントから SKU 一覧を匿名で取得するため外部公開する. 読み取り専用で, 認証は行わない.
param catalogExternalIngress = true

param frontendTargetPort = 8080
param frontendCpu = '0.25'
param frontendMemory = '0.5Gi'
param frontendMinReplicas = 1
param frontendMaxReplicas = 3

// Azure Container Registry
param acrSku = 'Basic'

// Log Analytics
param logRetentionDays = 30

// API Management
// MCP サーバーにできる SKU のうち, Developer (SLA なし) を除いて最安の Basic. 作成に 30 分ほどかかる.
param apimSku = 'Basic'
// 発行者のメールアドレスは通知の送信先. 実際のアドレスは, 環境変数 APIM_PUBLISHER_EMAIL (azd env set) で指定する.
param apimPublisherEmail = readEnvironmentVariable('APIM_PUBLISHER_EMAIL', 'noreply@example.com')
param apimPublisherName = 'quote'

// Azure AI Search
param searchSku = 'basic'
param searchIndexName = 'sku-catalog'
// API キーは使わず, Foundry と catalog にロールを付与して Microsoft Entra ID で接続する.
param searchDisableLocalAuth = true

// Foundry
param foundrySku = 'S0'
param foundryProjectName = 'iandm-project'
param foundryProjectDisplayName = 'quote'
param foundryDisableLocalAuth = true
// 埋め込みモデル. 1536 次元で, AI Search のインデックス (SUMMARY_VECTOR) の次元数と一致する.
param embeddingDeploymentName = 'text-embedding-3-small'
param embeddingModelName = 'text-embedding-3-small'
param embeddingModelVersion = '1'
param embeddingSkuName = 'GlobalStandard'
param embeddingCapacity = 50
// ナレッジベース (catalog-kb) が, クエリの分解と回答の生成に使うチャットモデル.
param chatDeploymentName = 'gpt-5.4-mini'
param chatModelName = 'gpt-5.4-mini'
param chatModelVersion = '2026-03-17'
param chatSkuName = 'GlobalStandard'
param chatCapacity = 50
