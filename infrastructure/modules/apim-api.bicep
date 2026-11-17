@description('API を登録する API Management の名前.')
param apimName string

@description('API の名前. REST API の URL パスと MCP サーバーの名前の元になる.')
param apiName string

@description('API の表示名.')
param displayName string

@description('バックエンドの URL (https://ホスト名). 仕様の servers よりも優先される.')
param serviceUrl string

@description('取り込む OpenAPI の仕様 (JSON の文字列).')
param openApiSpecification string

@description('MCP のツールとして公開する操作の operationId.')
param toolOperationIds array

resource apim 'Microsoft.ApiManagement/service@2024-05-01' existing = {
  name: apimName
}

// 認証は行わないため, サブスクリプションキーを要求しない
resource restApi 'Microsoft.ApiManagement/service/apis@2025-09-01-preview' = {
  parent: apim
  name: apiName
  properties: {
    displayName: displayName
    path: apiName
    protocols: ['https']
    format: 'openapi+json'
    value: openApiSpecification
    serviceUrl: serviceUrl
    subscriptionRequired: false
  }
}

// REST API の操作を MCP のツールとして公開する. URL は https://<ゲートウェイ>/<apiName>-mcp/mcp
resource mcpApi 'Microsoft.ApiManagement/service/apis@2025-09-01-preview' = {
  parent: apim
  name: '${apiName}-mcp'
  properties: {
    displayName: '${displayName} (MCP)'
    type: 'mcp'
    path: '${apiName}-mcp'
    protocols: ['https']
    subscriptionRequired: false
    // サービスは endpoints を, 名前をキーにしたオブジェクトで受け取る. Bicep の型定義は配列のため, 型の検査を外す
    mcpProperties: any({
      transportType: 'streamable'
      endpoints: {
        mcp: {
          uriTemplate: '/mcp'
        }
      }
    })
  }
}

// 同じ MCP API への更新が並列だと競合する (PreconditionFailed) ため, 1 つずつ作成する
@batchSize(1)
resource tools 'Microsoft.ApiManagement/service/apis/tools@2025-09-01-preview' = [for operationId in toolOperationIds: {
  parent: mcpApi
  name: operationId
  properties: {
    displayName: operationId
    operationId: '/apis/${restApi.name}/operations/${operationId}'
  }
}]

output restPath string = restApi.properties.path
output mcpPath string = mcpApi.properties.path
