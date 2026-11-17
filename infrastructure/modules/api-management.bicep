@description('API Management の名前 (英数字とハイフン. グローバルで一意).')
param name string

param location string

@description('API Management の SKU. REST API を MCP サーバーにできる SKU に限る (Consumption は不可).')
@allowed(['Developer', 'Basic', 'BasicV2', 'Standard', 'StandardV2', 'Premium'])
param sku string

@description('SKU のユニット数.')
param capacity int = 1

@description('通知の送信先となる発行者のメールアドレス.')
param publisherEmail string

@description('発行者の組織名.')
param publisherName string

param tags object

resource apim 'Microsoft.ApiManagement/service@2024-05-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: sku
    capacity: capacity
  }
  properties: {
    publisherEmail: publisherEmail
    publisherName: publisherName
  }
}

output name string = apim.name
output gatewayUrl string = apim.properties.gatewayUrl
