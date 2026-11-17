@description('Azure Container Registry の名前 (英数字のみ, グローバルで一意).')
param name string

param location string

@description('ACR の SKU.')
@allowed(['Basic', 'Standard', 'Premium'])
param sku string

@description('イメージの取得 (AcrPull) を許可するマネージド ID のプリンシパル ID.')
param pullPrincipalId string

param tags object

var acrPullRoleDefinitionId = '7f951dda-4ed3-4680-a7ca-43fe172d538d'

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: name
  location: location
  tags: tags
  sku: {
    name: sku
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
  }
}

resource acrPullAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, pullPrincipalId, acrPullRoleDefinitionId)
  scope: registry
  properties: {
    principalId: pullPrincipalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', acrPullRoleDefinitionId)
  }
}

output id string = registry.id
output name string = registry.name
output loginServer string = registry.properties.loginServer
output acrPullAssignmentId string = acrPullAssignment.id
