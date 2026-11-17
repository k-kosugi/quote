@description('認証を設定する Container App の名前.')
param appName string

@description('保護対象 API のアプリ登録のクライアント ID.')
param clientId string

@description('保護対象 API の識別子 URI (トークンの audience).')
param audience string

@description('呼び出しを許可するマネージド ID のオブジェクト ID.')
param allowedPrincipalIds array

@description('呼び出しを許可するクライアントアプリケーション ID. 空の場合は検証しない.')
param allowedApplicationIds array = []

resource app 'Microsoft.App/containerApps@2024-03-01' existing = {
  name: appName
}

// API 専用のため, 未認証は 401 を返しログイン画面へは転送しない
resource authConfig 'Microsoft.App/containerApps/authConfigs@2024-03-01' = {
  parent: app
  name: 'current'
  properties: {
    platform: {
      enabled: true
    }
    globalValidation: {
      unauthenticatedClientAction: 'Return401'
    }
    identityProviders: {
      azureActiveDirectory: {
        enabled: true
        registration: {
          clientId: clientId
          openIdIssuer: '${environment().authentication.loginEndpoint}${tenant().tenantId}/v2.0'
        }
        validation: {
          allowedAudiences: [
            audience
          ]
          defaultAuthorizationPolicy: {
            allowedApplications: allowedApplicationIds
            allowedPrincipals: {
              identities: allowedPrincipalIds
            }
          }
        }
      }
    }
    login: {
      preserveUrlFragmentsForLogins: false
    }
  }
}
