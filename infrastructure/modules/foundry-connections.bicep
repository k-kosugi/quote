@description('Foundry アカウントの名前.')
param accountName string

@description('Foundry プロジェクトの名前.')
param projectName string

@description('quote の URL. API Management 経由の URL (https://<ゲートウェイ>/quote). quote-agent の OpenAPI ツールの接続先.')
param quoteUrl string

@description('AI Search のエンドポイント.')
param searchEndpoint string

@description('catalog-agent が使うナレッジベースの名前.')
param knowledgeBaseName string

@description('Foundry のプロジェクトを実行する人 (デプロイの実行者) のプリンシパル ID. エージェントの作成に使う.')
param deployerPrincipalId string

// https://learn.microsoft.com/azure/role-based-access-control/built-in-roles
var foundryUserRoleId = '53ca6127-db72-4b80-b1b0-d745d6d5456d'

resource account 'Microsoft.CognitiveServices/accounts@2025-06-01' existing = {
  name: accountName
}

resource project 'Microsoft.CognitiveServices/accounts/projects@2025-06-01' existing = {
  parent: account
  name: projectName
}

// quote-agent の OpenAPI ツール. quote は認証なしのため, 匿名 (authType: None) の接続にする
resource quoteOpenApiConnection 'Microsoft.CognitiveServices/accounts/projects/connections@2025-06-01' = {
  parent: project
  name: 'quote-openapi'
  properties: {
    category: 'OpenAPI'
    authType: 'None'
    target: quoteUrl
    metadata: {
      type: 'openapi'
    }
  }
}

// catalog-agent の Foundry IQ (ナレッジベースの MCP エンドポイント). プロジェクトのマネージド ID で AI Search に接続する
resource knowledgeBaseConnection 'Microsoft.CognitiveServices/accounts/projects/connections@2025-06-01' = {
  parent: project
  name: 'catalog-kb-mcp'
  properties: {
    category: 'RemoteTool'
    // Bicep の型定義にはまだ無いが, サービスが受け付ける値 (公式ドキュメントの手順)
    #disable-next-line BCP036
    authType: 'ProjectManagedIdentity'
    target: '${searchEndpoint}/knowledgebases/${knowledgeBaseName}/mcp?api-version=2026-08-01-preview'
    isSharedToAll: true
    audience: 'https://search.azure.com/'
    metadata: {
      ApiType: 'Azure'
    }
  }
}

// デプロイの実行者が, プロジェクトでエージェントを作成して使えるようにする
resource deployerFoundryUser 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(project.id, deployerPrincipalId, foundryUserRoleId)
  scope: project
  properties: {
    principalId: deployerPrincipalId
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', foundryUserRoleId)
  }
}

output quoteOpenApiConnectionName string = quoteOpenApiConnection.name
output knowledgeBaseConnectionName string = knowledgeBaseConnection.name
