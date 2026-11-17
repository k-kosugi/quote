targetScope = 'resourceGroup'

@description('リソースの配置先リージョン.')
param location string

@description('リソース名の接頭辞.')
@minLength(3)
@maxLength(20)
param workloadName string

@description('すべてのリソースに付与するタグ.')
param tags object = {}

@description('見積 API (quote) の Container App 名.')
param quoteAppName string

@description('SKU カタログ API (catalog) の Container App 名.')
param catalogAppName string

@description('見積画面 (frontend) の Container App 名.')
param frontendAppName string

@description('quote のイメージ. azd が deploy 後に記録したイメージを再利用し, 初回はプレースホルダーを指定する.')
param quoteImage string

@description('quote のコンテナーポート.')
param quoteTargetPort int

@description('quote の vCPU 数.')
param quoteCpu string

@description('quote のメモリ.')
param quoteMemory string

@description('quote の最小レプリカ数. 見積をプロセス内メモリに保持するため, 1 を超えるとレプリカ間でデータが共有されない.')
param quoteMinReplicas int

@description('quote の最大レプリカ数. 見積をプロセス内メモリに保持するため, 1 を超えるとレプリカ間でデータが共有されない.')
param quoteMaxReplicas int

@description('true の場合は quote を環境外へ公開する. false の場合は frontend など同一環境内からのみ呼び出せる.')
param quoteExternalIngress bool

@description('catalog のイメージ. azd が deploy 後に記録したイメージを再利用し, 初回はプレースホルダーを指定する.')
param catalogImage string

@description('catalog のコンテナーポート.')
param catalogTargetPort int

@description('catalog の vCPU 数.')
param catalogCpu string

@description('catalog のメモリ.')
param catalogMemory string

@description('catalog の最小レプリカ数.')
param catalogMinReplicas int

@description('catalog の最大レプリカ数.')
param catalogMaxReplicas int

@description('true の場合は catalog を環境外へ公開する. false の場合は quote と frontend など同一環境内からのみ呼び出せる.')
param catalogExternalIngress bool

@description('frontend のイメージ. azd が deploy 後に記録したイメージを再利用し, 初回はプレースホルダーを指定する.')
param frontendImage string

@description('frontend のコンテナーポート.')
param frontendTargetPort int

@description('frontend の vCPU 数.')
param frontendCpu string

@description('frontend のメモリ.')
param frontendMemory string

@description('frontend の最小レプリカ数.')
param frontendMinReplicas int

@description('frontend の最大レプリカ数.')
param frontendMaxReplicas int

@description('Azure Container Registry の SKU.')
@allowed(['Basic', 'Standard', 'Premium'])
param acrSku string

@description('Log Analytics のログ保持日数.')
param logRetentionDays int

@description('AI Search の SKU.')
param searchSku string

@description('SKU のインデックスの名前.')
param searchIndexName string

@description('true の場合は AI Search の API キー認証を無効にする. その場合は Microsoft Entra ID 認証とロールで接続する.')
param searchDisableLocalAuth bool

@description('API Management の SKU. REST API を MCP サーバーにできる SKU に限る.')
@allowed(['Developer', 'Basic', 'BasicV2', 'Standard', 'StandardV2', 'Premium'])
param apimSku string

@description('API Management の発行者のメールアドレス. 通知の送信先になる.')
param apimPublisherEmail string

@description('API Management の発行者の組織名.')
param apimPublisherName string

@description('Foundry アカウントの SKU.')
param foundrySku string

@description('埋め込みモデルのデプロイ名.')
param embeddingDeploymentName string

@description('埋め込みモデルの名前. 出力の次元数は, AI Search のインデックスの SUMMARY_VECTOR の次元数と一致させる.')
param embeddingModelName string

@description('埋め込みモデルのバージョン.')
param embeddingModelVersion string

@description('埋め込みモデルのデプロイの SKU.')
param embeddingSkuName string

@description('埋め込みモデルの容量 (1 あたり 1,000 トークン/分).')
param embeddingCapacity int

@description('ナレッジベースが使うチャットモデルのデプロイ名.')
param chatDeploymentName string

@description('チャットモデルの名前.')
param chatModelName string

@description('チャットモデルのバージョン.')
param chatModelVersion string

@description('チャットモデルのデプロイの SKU.')
param chatSkuName string

@description('チャットモデルの容量 (1 あたり 1,000 トークン/分).')
param chatCapacity int

@description('Foundry プロジェクトの名前.')
param foundryProjectName string

@description('Foundry プロジェクトの表示名.')
param foundryProjectDisplayName string

@description('true の場合は Foundry の API キー認証を無効にする.')
param foundryDisableLocalAuth bool

// グローバルで一意な名前が必要なリソース向けの接尾辞
var uniqueSuffix = take(uniqueString(resourceGroup().id), 6)
var registryName = '${replace(workloadName, '-', '')}${uniqueSuffix}'
var searchName = '${workloadName}-search-${uniqueSuffix}'
var apimName = '${workloadName}-apim-${uniqueSuffix}'
// API Management のゲートウェイの URL. API Management が quote と catalog の URL を参照するため, 循環しないよう名前から組み立てる
var apimGatewayUrl = 'https://${apimName}.azure-api.net'
var foundryName = '${workloadName}-foundry-${uniqueSuffix}'

resource pullIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: '${workloadName}-pull-id'
  location: location
  tags: tags
}

module registry 'modules/container-registry.bicep' = {
  name: 'container-registry'
  params: {
    name: registryName
    location: location
    sku: acrSku
    pullPrincipalId: pullIdentity.properties.principalId
    tags: tags
  }
}

module environment 'modules/container-apps-environment.bicep' = {
  name: 'container-apps-environment'
  params: {
    name: '${workloadName}-env'
    logAnalyticsName: '${workloadName}-logs'
    location: location
    logRetentionDays: logRetentionDays
    tags: tags
  }
}

module search 'modules/search.bicep' = {
  name: 'search'
  params: {
    name: searchName
    location: location
    sku: searchSku
    disableLocalAuth: searchDisableLocalAuth
    tags: tags
  }
}

module catalog 'modules/container-app.bicep' = {
  name: 'catalog-app'
  params: {
    name: catalogAppName
    location: location
    environmentId: environment.outputs.id
    identityId: pullIdentity.id
    registryServer: registry.outputs.loginServer
    image: catalogImage
    targetPort: catalogTargetPort
    externalIngress: catalogExternalIngress
    cpu: catalogCpu
    memory: catalogMemory
    minReplicas: catalogMinReplicas
    maxReplicas: catalogMaxReplicas
    // AI Search へは, ACR 取得用の共有 ID ではなく catalog 専用のシステム割り当て ID で接続する
    enableSystemAssignedIdentity: true
    env: [
      {
        name: 'AZURE_SEARCH_ENDPOINT'
        value: search.outputs.endpoint
      }
      {
        name: 'AZURE_SEARCH_INDEX_NAME'
        value: searchIndexName
      }
    ]
    // azd が azure.yaml のサービス名とこのリソースを対応づけるタグ
    tags: union(tags, { 'azd-service-name': 'catalog' })
  }
}

module quote 'modules/container-app.bicep' = {
  name: 'quote-app'
  params: {
    name: quoteAppName
    location: location
    environmentId: environment.outputs.id
    identityId: pullIdentity.id
    registryServer: registry.outputs.loginServer
    image: quoteImage
    targetPort: quoteTargetPort
    externalIngress: quoteExternalIngress
    cpu: quoteCpu
    memory: quoteMemory
    minReplicas: quoteMinReplicas
    maxReplicas: quoteMaxReplicas
    // 単価は API Management 経由で catalog から取得する. API Management が作成されるまでの間は, 取得に失敗して 503 になる
    env: [
      {
        name: 'CATALOG_URL'
        value: '${apimGatewayUrl}/catalog'
      }
    ]
    // azd が azure.yaml のサービス名とこのリソースを対応づけるタグ
    tags: union(tags, { 'azd-service-name': 'quote' })
  }
}

module frontend 'modules/container-app.bicep' = {
  name: 'frontend-app'
  // 転送先の API が登録されてから起動する
  dependsOn: [
    apimQuote
    apimCatalog
  ]
  params: {
    name: frontendAppName
    location: location
    environmentId: environment.outputs.id
    identityId: pullIdentity.id
    registryServer: registry.outputs.loginServer
    image: frontendImage
    targetPort: frontendTargetPort
    externalIngress: true
    cpu: frontendCpu
    memory: frontendMemory
    minReplicas: frontendMinReplicas
    maxReplicas: frontendMaxReplicas
    env: [
      {
        name: 'QUOTE_URL'
        value: '${apimGatewayUrl}/quote'
      }
      {
        name: 'CATALOG_URL'
        value: '${apimGatewayUrl}/catalog'
      }
    ]
    // Liveness は nginx 自身だけを見る. quote に依存する判定は Readiness だけに持たせ, quote の停止で再起動を繰り返さない
    probes: [
      {
        type: 'Startup'
        httpGet: { path: '/healthz', port: frontendTargetPort }
        periodSeconds: 3
        failureThreshold: 20
      }
      {
        type: 'Liveness'
        httpGet: { path: '/healthz', port: frontendTargetPort }
        periodSeconds: 10
        timeoutSeconds: 2
        failureThreshold: 3
      }
      {
        // quote に届かない間は, この replica へトラフィックを流さない
        type: 'Readiness'
        httpGet: { path: '/readyz', port: frontendTargetPort }
        periodSeconds: 5
        timeoutSeconds: 3
        failureThreshold: 2
        successThreshold: 1
      }
    ]
    tags: union(tags, { 'azd-service-name': 'frontend' })
  }
}

module foundry 'modules/foundry.bicep' = {
  name: 'foundry'
  params: {
    name: foundryName
    projectName: foundryProjectName
    projectDisplayName: foundryProjectDisplayName
    location: location
    sku: foundrySku
    disableLocalAuth: foundryDisableLocalAuth
    searchPrincipalId: search.outputs.principalId
    chatDeploymentName: chatDeploymentName
    chatModelName: chatModelName
    chatModelVersion: chatModelVersion
    chatSkuName: chatSkuName
    chatCapacity: chatCapacity
    embeddingDeploymentName: embeddingDeploymentName
    embeddingModelName: embeddingModelName
    embeddingModelVersion: embeddingModelVersion
    embeddingSkuName: embeddingSkuName
    embeddingCapacity: embeddingCapacity
    tags: tags
  }
}

module apim 'modules/api-management.bicep' = {
  name: 'api-management'
  params: {
    name: apimName
    location: location
    sku: apimSku
    publisherEmail: apimPublisherEmail
    publisherName: apimPublisherName
    tags: tags
  }
}

// 各サービスの仕様は, 実装から生成したファイルを取り込む. API を変更したら, 仕様ファイルも再生成する
module apimQuote 'modules/apim-api.bicep' = {
  name: 'apim-quote'
  params: {
    apimName: apim.outputs.name
    apiName: 'quote'
    displayName: '見積 API'
    serviceUrl: 'https://${quote.outputs.fqdn}'
    openApiSpecification: loadTextContent('../quote/openapi.json')
    toolOperationIds: ['listQuotes', 'getQuote', 'createQuote', 'reviseQuote', 'saveQuote', 'deleteQuote']
  }
}

module apimCatalog 'modules/apim-api.bicep' = {
  name: 'apim-catalog'
  params: {
    apimName: apim.outputs.name
    apiName: 'catalog'
    displayName: 'SKU カタログ API'
    serviceUrl: 'https://${catalog.outputs.fqdn}'
    openApiSpecification: loadTextContent('../catalog/openapi.json')
    toolOperationIds: ['listSkus', 'getSku']
  }
}

module searchRoles 'modules/search-roles.bicep' = {
  name: 'search-roles'
  params: {
    searchName: search.outputs.name
    foundryPrincipalIds: [
      foundry.outputs.accountPrincipalId
      foundry.outputs.projectPrincipalId
    ]
    catalogPrincipalId: catalog.outputs.systemAssignedPrincipalId
    deployerPrincipalId: deployer().objectId
  }
}

// プロジェクトの接続. エージェントは作成しない. ポータルで手で作るときに, この接続を使う
module foundryConnections 'modules/foundry-connections.bicep' = {
  name: 'foundry-connections'
  params: {
    accountName: foundry.outputs.accountName
    projectName: foundry.outputs.projectName
    // quote は API Management 経由で呼び出す
    quoteUrl: '${apimGatewayUrl}/quote'
    searchEndpoint: search.outputs.endpoint
    knowledgeBaseName: 'catalog-kb'
    deployerPrincipalId: deployer().objectId
  }
}

output acrName string = registry.outputs.name

@description('azd がイメージの push 先として参照する.')
output AZURE_CONTAINER_REGISTRY_ENDPOINT string = registry.outputs.loginServer
output acrLoginServer string = registry.outputs.loginServer
output quoteFqdn string = quote.outputs.fqdn
output catalogFqdn string = catalog.outputs.fqdn
output frontendUrl string = 'https://${frontend.outputs.fqdn}'
output apimName string = apim.outputs.name
output apimGatewayUrl string = apim.outputs.gatewayUrl
output quoteMcpUrl string = '${apim.outputs.gatewayUrl}/${apimQuote.outputs.mcpPath}/mcp'
output catalogMcpUrl string = '${apim.outputs.gatewayUrl}/${apimCatalog.outputs.mcpPath}/mcp'
output searchName string = search.outputs.name

@description('azd の postprovision フックがインデックスの作成先として参照する.')
output AZURE_SEARCH_ENDPOINT string = search.outputs.endpoint

@description('azd の postprovision フックが作成するインデックスの名前.')
output AZURE_SEARCH_INDEX_NAME string = searchIndexName
output foundryAccountName string = foundry.outputs.accountName
output foundryEndpoint string = foundry.outputs.endpoint
output foundryProjectName string = foundry.outputs.projectName
output foundryEmbeddingDeploymentName string = foundry.outputs.embeddingDeploymentName
output foundryEmbeddingModelName string = foundry.outputs.embeddingModelName
output foundryChatDeploymentName string = foundry.outputs.chatDeploymentName
output foundryChatModelName string = foundry.outputs.chatModelName
output foundryQuoteOpenApiConnectionName string = foundryConnections.outputs.quoteOpenApiConnectionName
output foundryKnowledgeBaseConnectionName string = foundryConnections.outputs.knowledgeBaseConnectionName
