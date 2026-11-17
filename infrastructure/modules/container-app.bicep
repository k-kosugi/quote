@description('Container App の名前.')
param name string

param location string

@description('配置先の Container Apps 環境のリソース ID.')
param environmentId string

@description('イメージの取得に使用するユーザー割り当てマネージド ID のリソース ID.')
param identityId string

@description('イメージの取得元レジストリのログインサーバー.')
param registryServer string

@description('コンテナーイメージ (レジストリ/リポジトリ:タグ).')
param image string

@description('コンテナーが待ち受けるポート.')
param targetPort int

@description('true の場合は環境外からもアクセスできる ingress にする.')
param externalIngress bool

@description('vCPU 数 (例: 0.5).')
param cpu string

@description('メモリ (例: 1Gi).')
param memory string

param minReplicas int
param maxReplicas int

@description('コンテナーに渡す環境変数.')
param env array = []

@description('コンテナーの Startup, Liveness, Readiness probe. 未指定の場合は Container Apps の既定の TCP probe になる.')
param probes array = []

@description('true の場合はシステム割り当てマネージド ID も有効にする. ACR からの取得に使うユーザー割り当て ID とは別に, 他のリソースへのアクセス用に使う.')
param enableSystemAssignedIdentity bool = false

param tags object

resource app 'Microsoft.App/containerApps@2024-03-01' = {
  name: name
  location: location
  tags: tags
  identity: {
    type: enableSystemAssignedIdentity ? 'SystemAssigned, UserAssigned' : 'UserAssigned'
    userAssignedIdentities: {
      '${identityId}': {}
    }
  }
  properties: {
    environmentId: environmentId
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: externalIngress
        targetPort: targetPort
        transport: 'auto'
        allowInsecure: false
      }
      registries: [
        {
          server: registryServer
          identity: identityId
        }
      ]
    }
    template: {
      containers: [
        {
          name: name
          image: image
          env: env
          probes: probes
          resources: {
            cpu: json(cpu)
            memory: memory
          }
        }
      ]
      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas
      }
    }
  }
}

output name string = app.name
output fqdn string = app.properties.configuration.ingress.fqdn

@description('システム割り当てマネージド ID のプリンシパル ID. 無効な場合は空文字.')
output systemAssignedPrincipalId string = enableSystemAssignedIdentity ? app.identity.principalId : ''
