package com.microsoft.japan.quote.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import com.microsoft.japan.quote.domain.ContractType;
import com.microsoft.japan.quote.domain.Quote;
import com.microsoft.japan.quote.domain.QuoteStatus;

/** サーバーが計算した年額と保存状態を含む見積もりレスポンス. */
@Schema(name = "QuoteResponse",
        description = "見積の識別子, 契約条件, 明細, 保存状態, サーバー計算の有効期限と合計金額を含むレスポンス.")
public record QuoteResponse(
        @Schema(description = "見積の UUID", requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
        UUID id,
        @Schema(description = "サーバーのデフォルトタイムゾーンに基づく作成日の14暦日後. 修正や保存では延長されません.",
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, format = "date")
        LocalDate validUntil,
        @Schema(description = "契約開始日", requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
        LocalDate contractStart,
        @Schema(description = "契約終了日", requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
        LocalDate contractEnd,
        @Schema(description = "明細数量の符号から決まる契約種別. 正のみは NEW, 負のみは CANCELLATION, 正負混在は EARLY_RENEWAL.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        ContractType contractType,
        @Schema(description = "作成または修正後は DRAFT, 保存後は SAVED.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        QuoteStatus status,
        @ArraySchema(arraySchema = @Schema(
                description = "見積の SKU 明細. 同じ SKU を複数含む場合, 各明細を個別に集計します.",
                requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(implementation = LineResponse.class), minItems = 1)
        List<LineResponse> skuList,
        @Schema(description = "合計金額に適用した見積全体の割引率 (%). 各明細の annual には適用されません.",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "100")
        BigDecimal discountRate,
        @Schema(description = "解約明細を除いた annual の合計に割引率を適用し, HALF_UP 方式で1円単位に丸めた, 数量を含む1年分の金額. 契約期間は乗算しません.",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        BigDecimal totalAnnual,
        @Schema(description = "契約期間全体の総額. 解約明細を除いた annual の合計に 契約月数 / 12 を掛け, 割引率を適用し, HALF_UP 方式で1円単位に丸めた金額. 契約月数は終了日を含む暦の月数で, 端数の日は1か月に切り上げます.",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        BigDecimal totalAmount) {

    /** ドメイン集約を公開用 DTO に変換する. */
    public static QuoteResponse from(final Quote quote) {
        return new QuoteResponse(
                quote.getId().value(),
                quote.getValidUntil(),
                quote.getTerms().contractStart(),
                quote.getTerms().contractEnd(),
                quote.getContractType(),
                quote.getStatus(),
                quote.getLines().stream()
                        .map(line -> new LineResponse(line.sku(), line.listPrice(),
                                line.amount(), line.calculateAnnual()))
                        .toList(),
                quote.getDiscountRate().value(),
                quote.getTotals().totalAnnual(),
                quote.getTotals().totalAmount());
    }

    /** 数量を含む割引前の年額を含む明細レスポンス. */
    @Schema(name = "LineResponse", description = "SKU, 月額単価, 数量, 割引前の年額 (数量込み) を含む見積明細.")
    public record LineResponse(
            @Schema(description = "前後の空白を除去した SKU ID. 大文字と小文字を区別します.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String sku,
            @Schema(description = "月額単価 (JPY). 0以上の値です.",
                    requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
            BigDecimal listPrice,
            @Schema(description = "追加は正の整数, 解約は負の整数です. 0にはなりません.",
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    minimum = "-2147483648", maximum = "2147483647")
            int amount,
            @Schema(description = "割引前の年額 (数量込み). 追加明細では listPrice * 12 * amount, 解約明細では0です.",
                    requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
            BigDecimal annual) {
    }
}
