@description('Foundry (AIServices) アカウントの名前. カスタムサブドメインにも使用するためグローバルで一意にする.')
param name string

@description('Foundry プロジェクトの名前.')
param projectName string

@description('Foundry プロジェクトの表示名.')
param projectDisplayName string

param location string

@description('アカウントの SKU.')
param sku string

@description('true の場合は API キー認証を無効にして Microsoft Entra ID 認証のみにする.')
param disableLocalAuth bool

@description('埋め込みモデルを呼び出す AI Search のマネージド ID のプリンシパル ID. Cognitive Services OpenAI User を付与する.')
param searchPrincipalId string

@description('埋め込みモデルのデプロイ名. 呼び出し側は, モデル名ではなくこの名前を指定する.')
param embeddingDeploymentName string

@description('埋め込みモデルの名前.')
param embeddingModelName string

@description('埋め込みモデルのバージョン.')
param embeddingModelVersion string

@description('埋め込みモデルのデプロイの SKU (例: GlobalStandard).')
param embeddingSkuName string

@description('埋め込みモデルの容量. 1 あたり 1,000 トークン/分 (TPM).')
param embeddingCapacity int

@description('ナレッジベースが使うチャットモデルのデプロイ名.')
param chatDeploymentName string

@description('チャットモデルの名前.')
param chatModelName string

@description('チャットモデルのバージョン.')
param chatModelVersion string

@description('チャットモデルのデプロイの SKU (例: GlobalStandard).')
param chatSkuName string

@description('チャットモデルの容量. 1 あたり 1,000 トークン/分 (TPM).')
param chatCapacity int

param tags object

resource account 'Microsoft.CognitiveServices/accounts@2025-06-01' = {
  name: name
  location: location
  tags: tags
  kind: 'AIServices'
  sku: {
    name: sku
  }
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    allowProjectManagement: true
    customSubDomainName: name
    disableLocalAuth: disableLocalAuth
    publicNetworkAccess: 'Enabled'
  }
}

resource project 'Microsoft.CognitiveServices/accounts/projects@2025-06-01' = {
  parent: account
  name: projectName
  location: location
  tags: tags
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    displayName: projectDisplayName
  }
}

// 同じアカウントへの更新が並列だと競合するため, プロジェクトの作成後にデプロイする
resource embeddingDeployment 'Microsoft.CognitiveServices/accounts/deployments@2025-06-01' = {
  parent: account
  name: embeddingDeploymentName
  dependsOn: [
    project
  ]
  sku: {
    name: embeddingSkuName
    capacity: embeddingCapacity
  }
  properties: {
    model: {
      format: 'OpenAI'
      name: embeddingModelName
      version: embeddingModelVersion
    }
  }
}

// 同じアカウントへのデプロイは並列だと競合するため, 埋め込みモデルの後にデプロイする
resource chatDeployment 'Microsoft.CognitiveServices/accounts/deployments@2025-06-01' = {
  parent: account
  name: chatDeploymentName
  dependsOn: [
    embeddingDeployment
  ]
  sku: {
    name: chatSkuName
    capacity: chatCapacity
  }
  properties: {
    model: {
      format: 'OpenAI'
      name: chatModelName
      version: chatModelVersion
    }
  }
}

var cognitiveServicesUserRoleId = 'a97b65f3-24c7-4388-baec-2e87135dc908'
var cognitiveServicesOpenAIUserRoleId = '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd'

// AI Search の vectorizer が, API キーを使わず, 検索クエリのテキストを埋め込みに変換できるようにする
resource searchOpenAIUser 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(account.id, searchPrincipalId, cognitiveServicesOpenAIUserRoleId)
  scope: account
  properties: {
    principalId: searchPrincipalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', cognitiveServicesOpenAIUserRoleId)
  }
}

// ナレッジベースが, AI Search のマネージド ID でチャットモデルを呼び出せるようにする
resource searchCognitiveServicesUser 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(account.id, searchPrincipalId, cognitiveServicesUserRoleId)
  scope: account
  properties: {
    principalId: searchPrincipalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', cognitiveServicesUserRoleId)
  }
}

output accountName string = account.name
output endpoint string = account.properties.endpoint
output projectName string = project.name
output embeddingDeploymentName string = embeddingDeployment.name
output embeddingModelName string = embeddingDeployment.properties.model.name
output chatDeploymentName string = chatDeployment.name
output chatModelName string = chatDeployment.properties.model.name
output accountPrincipalId string = account.identity.principalId
output projectPrincipalId string = project.identity.principalId
