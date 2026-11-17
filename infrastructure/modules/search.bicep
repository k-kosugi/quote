@description('Azure AI Search の名前 (小文字, 数字, ハイフン. グローバルで一意).')
param name string

param location string

@description('AI Search の SKU.')
@allowed(['free', 'basic', 'standard', 'standard2', 'standard3'])
param sku string

@description('true の場合は API キー認証を無効にして Microsoft Entra ID 認証のみにする.')
param disableLocalAuth bool

param tags object

resource search 'Microsoft.Search/searchServices@2024-06-01-preview' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: sku
  }
  // vectorizer が Foundry の埋め込みモデルを呼び出すときに使う ID. Foundry 側でロールを付与する
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    replicaCount: 1
    partitionCount: 1
    hostingMode: 'default'
    publicNetworkAccess: 'enabled'
    disableLocalAuth: disableLocalAuth
    // セマンティックランカー (Foundry IQ のナレッジソースに必須). 無料枠は 1,000 リクエスト/月
    semanticSearch: 'free'
    // API キーを無効にする場合は, Entra ID の認証だけを受け付ける
    authOptions: disableLocalAuth ? null : {
      aadOrApiKey: {
        aadAuthFailureMode: 'http401WithBearerChallenge'
      }
    }
  }
}

output id string = search.id
output principalId string = search.identity.principalId
output name string = search.name
output endpoint string = 'https://${search.name}.search.windows.net'
