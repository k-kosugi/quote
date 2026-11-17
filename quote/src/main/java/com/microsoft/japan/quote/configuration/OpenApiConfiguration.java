package com.microsoft.japan.quote.configuration;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/** 見積 API の共通ルールと OpenAPI メタデータを定義する. */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "見積 API",
        version = "1.0.0",
        description = """
                ## 保存と有効期限
                見積はプロセス内メモリに保存されるため, アプリケーションの再起動で失われます.
                作成時は DRAFT となり, 保存操作で SAVED になります. 修正すると DRAFT に戻ります.
                validUntil は作成日の14日後の日付です. 日付の算出にはサーバーのデフォルトタイムゾーンを使用します.
                修正や保存では validUntil は延長されません. この値はレスポンス専用です.
                この API は認証や見積の変更履歴を提供しません.
                見積の対象地域は日本のみです. 地域を指定する項目はありません.

                ## 金額計算
                通貨は JPY です. listPrice は月額単価で, 見積作成時に SKU カタログの値が自動で適用されます. リクエストでは指定できません.
                annual は追加明細では listPrice * 12 * amount (数量を含む1年分), 解約明細では0です.
                明細の annual は割引や丸めを適用しません.
                totalAnnual は解約明細を除いた annual の合計に割引率を適用した, 数量を含む1年分の金額です.
                totalAmount は契約期間全体の総額で, 解約明細を除いた annual の合計に 契約月数 / 12 を掛けた金額です.
                どちらも合計後に割引率を適用し, HALF_UP 方式で1円単位に丸めます.
                契約月数は契約開始日から契約終了日までの暦の月数で, 終了日を含みます. 端数の日がある場合は1か月に切り上げます.
                例: 2026-07-01 から 2029-06-30 は36か月, 同日契約は1か月です.

                ## 契約種別と明細ルール
                すべての明細の amount が正の場合は NEW, すべて負の場合は CANCELLATION,
                正負が混在する場合は EARLY_RENEWAL です. amount に0は指定できません.
                SKU-STANDARD と SKU-PROFESSIONAL は同じ見積に共存できません.
                同じ SKU の明細を複数指定できます. 各明細は個別に集計され, SKU の照合では大文字と小文字を区別します.

                ## SKU カタログ
                SKU カタログは別サービス (SKU カタログ API) が提供する読み取り専用のサンプルです. SKU 一覧はこの API ではなく, SKU カタログ API で取得します.
                見積リクエストでは SKU ID と数量だけを指定します. 月額単価は SKU カタログから取得され, 存在しない SKU は拒否されます.
                SKU カタログに接続できない場合は 503 を返します.
                見積に保存される listPrice は作成または修正した時点のカタログの値で, 以降のカタログの変更は反映されません.
                """))
public class OpenApiConfiguration {
}
