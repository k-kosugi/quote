package com.microsoft.japan.quote.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import com.microsoft.japan.quote.application.QuoteCommand;
import com.microsoft.japan.quote.application.RequestedLine;
import com.microsoft.japan.quote.domain.DiscountRate;
import com.microsoft.japan.quote.domain.InvalidQuoteException;
import com.microsoft.japan.quote.domain.QuoteTerms;

/** 計算結果を含まない見積もりの入力 DTO. */
@Schema(name = "QuoteRequest",
        description = "見積の作成および修正に使用する入力. 計算済み項目や未知の項目は指定できません.")
public record QuoteRequest(
        @Schema(description = "契約開始日. contractEnd と同じ日付, またはそれ以前の日付を指定してください.",
                requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
        LocalDate contractStart,
        @Schema(description = "契約終了日. contractStart より前の日付は指定できません.",
                requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
        LocalDate contractEnd,
        @ArraySchema(arraySchema = @Schema(
                description = "見積に含める SKU 明細. 1件以上必要です. 同じ SKU の重複は許可され, 別々に集計されます.",
                requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(implementation = LineRequest.class), minItems = 1)
        List<LineRequest> skuList,
        @Schema(description = "見積全体に適用する割引率 (%). 0から100までの値を指定します. 各明細の annual 自体には適用しません.",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "100")
        BigDecimal discountRate) {

    /** 入力を検証してフレームワーク非依存のコマンドに変換する. */
    public QuoteCommand toCommand() {
        if (skuList == null || skuList.isEmpty() || skuList.stream().anyMatch(line -> line == null)) {
            throw new InvalidQuoteException("At least one non-null SKU line is required.");
        }
        return new QuoteCommand(
                new QuoteTerms(this.contractStart, this.contractEnd),
                this.skuList.stream().map(LineRequest::toRequestedLine).toList(),
                new DiscountRate(this.discountRate));
    }

    /** SKU と数量を受け取る明細 DTO. 単価はカタログから決まるため受け取らない. */
    @Schema(name = "LineRequest",
            description = "SKU ID と数量を指定する明細. 月額単価は SKU カタログの値が自動で適用されます. "
                    + "SKU-STANDARD と SKU-PROFESSIONAL は同じ見積に含められません.")
    public record LineRequest(
            @Schema(description = "SKU ID. SKU カタログに存在する値のみ指定できます. 空文字や空白のみの値は指定できません. 前後の空白を除去し, 大文字と小文字を区別して照合します.",
                    requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, pattern = "\\S")
            String sku,
            @Schema(description = "追加は正の整数, 解約は負の整数で指定します. 0は指定できません. 数量の正負から見積の契約種別を判定します.",
                    requiredMode = Schema.RequiredMode.REQUIRED, minimum = "-2147483648",
                    maximum = "2147483647")
            Integer amount) {

        /** 必須の数量を検証して依頼明細に変換する. */
        public RequestedLine toRequestedLine() {
            if (amount == null) {
                throw new InvalidQuoteException("Amount is required.");
            }
            return new RequestedLine(this.sku, this.amount);
        }
    }
}
