@description('ロールを付与する AI Search の名前.')
param searchName string

@description('インデックスの検索 (読み取り) だけを行う Foundry のプリンシパル ID. Foundry のアカウントとプロジェクトのマネージド ID.')
param foundryPrincipalIds array

@description('インデックスの読み書きを行う catalog のプリンシパル ID.')
param catalogPrincipalId string

@description('azd up を実行した人 (デプロイの実行者) のプリンシパル ID. インデックスの作成とドキュメントの登録に使う.')
param deployerPrincipalId string

// https://learn.microsoft.com/azure/role-based-access-control/built-in-roles
var searchIndexDataReaderRoleId = '1407120a-92aa-4202-b7e9-c0e197c71c8f'
var searchIndexDataContributorRoleId = '8ebe5a00-799e-43f5-93ac-243d3dce84a7'
var searchServiceContributorRoleId = '7ca78c08-252a-4471-8644-bb5ff32d4ba0'

resource search 'Microsoft.Search/searchServices@2024-06-01-preview' existing = {
  name: searchName
}

// Foundry がインデックスを検索し, 接続の検証でサービスの情報を参照できるようにする
resource foundryIndexReader 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for principalId in foundryPrincipalIds: {
  name: guid(search.id, principalId, searchIndexDataReaderRoleId)
  scope: search
  properties: {
    principalId: principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', searchIndexDataReaderRoleId)
  }
}]

resource foundryServiceContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for principalId in foundryPrincipalIds: {
  name: guid(search.id, principalId, searchServiceContributorRoleId)
  scope: search
  properties: {
    principalId: principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', searchServiceContributorRoleId)
  }
}]

// フック (setup-search.sh) が, 実行者の権限でインデックスを作成し, ドキュメントを登録する
resource deployerServiceContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(search.id, deployerPrincipalId, searchServiceContributorRoleId)
  scope: search
  properties: {
    principalId: deployerPrincipalId
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', searchServiceContributorRoleId)
  }
}

resource deployerIndexContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(search.id, deployerPrincipalId, searchIndexDataContributorRoleId)
  scope: search
  properties: {
    principalId: deployerPrincipalId
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', searchIndexDataContributorRoleId)
  }
}

resource catalogIndexContributor 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(search.id, catalogPrincipalId, searchIndexDataContributorRoleId)
  scope: search
  properties: {
    principalId: catalogPrincipalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', searchIndexDataContributorRoleId)
  }
}
