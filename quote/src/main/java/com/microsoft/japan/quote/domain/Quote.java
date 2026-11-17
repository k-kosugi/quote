package com.microsoft.japan.quote.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/** 常に業務制約を満たす不変の見積もり集約. */
public final class Quote {

    private static final int VALIDITY_WEEKS = 2;

    private final QuoteId id;
    private final LocalDate createdOn;
    private final QuoteTerms terms;
    private final List<QuoteLine> lines;
    private final DiscountRate discountRate;
    private final QuoteStatus status;

    /** 全制約を検証して集約を構築する. 契約タイプと金額は入力から導けるため保持しない. */
    private Quote(final QuoteId id, final LocalDate createdOn, final QuoteTerms terms, final List<QuoteLine> lines,
            final DiscountRate discountRate, final QuoteStatus status) {
        if (id == null || createdOn == null || terms == null || discountRate == null) {
            throw new InvalidQuoteException("ID, creation date, terms and discount rate are required.");
        }
        if (lines == null || lines.isEmpty() || lines.stream().anyMatch(line -> line == null)) {
            throw new InvalidQuoteException("At least one non-null SKU line is required.");
        }
        this.id = id;
        this.createdOn = createdOn;
        this.terms = terms;
        this.lines = List.copyOf(lines);
        this.discountRate = discountRate;
        this.status = status;
        validateCombination();
    }

    /** 新しいドラフト見積もりを作成する. */
    public static Quote createDraft(final QuoteId id, final LocalDate createdOn, final QuoteTerms terms,
            final List<QuoteLine> lines, final DiscountRate discountRate) {
        return new Quote(id, createdOn, terms, lines, discountRate, QuoteStatus.DRAFT);
    }

    /** 識別子を維持したまま内容を修正し、ドラフト状態に戻す. */
    public Quote revise(final QuoteTerms newTerms, final List<QuoteLine> newLines,
            final DiscountRate newDiscountRate) {
        return new Quote(this.id, this.createdOn, newTerms, newLines, newDiscountRate, QuoteStatus.DRAFT);
    }

    /** 内容を再検証して保存済み状態の見積もりを返す. */
    public Quote markSaved() {
        return new Quote(this.id, this.createdOn, this.terms, this.lines, this.discountRate, QuoteStatus.SAVED);
    }

    /** 識別子を返す. */
    public QuoteId getId() {
        return this.id;
    }

    /** 修正や保存で延長しない、作成日から 2 週間後の有効期限を返す. */
    public LocalDate getValidUntil() {
        return this.createdOn.plusWeeks(VALIDITY_WEEKS);
    }

    /** 契約条件を返す. */
    public QuoteTerms getTerms() {
        return this.terms;
    }

    /** 変更不能な明細一覧を返す. */
    public List<QuoteLine> getLines() {
        return this.lines;
    }

    /** 全体割引率を返す. */
    public DiscountRate getDiscountRate() {
        return this.discountRate;
    }

    /** 保存状態を返す. */
    public QuoteStatus getStatus() {
        return this.status;
    }

    /** 明細の数量の符号から契約タイプを都度判定する. 保持しないため業務ルールの変更が過去の見積にも反映される. */
    public ContractType getContractType() {
        return this.determineContractType();
    }

    /** 入力値から金額を都度計算する. 保持しないため計算式の変更が過去の見積にも反映される. */
    public QuoteTotals getTotals() {
        return this.calculateTotals();
    }

    /** 同居できない SKU の組み合わせが含まれていないことを, 全明細の組み合わせで検証する. 判定は明細が持つ. */
    private void validateCombination() {
        for (int i = 0; i < this.lines.size(); i++) {
            for (int j = i + 1; j < this.lines.size(); j++) {
                final QuoteLine first = this.lines.get(i);
                final QuoteLine second = this.lines.get(j);
                if (first.isIncompatibleWith(second)) {
                    throw new InvalidQuoteException(Stream.of(first.sku(), second.sku()).sorted()
                            .collect(Collectors.joining(" and ")) + " cannot be combined.");
                }
            }
        }
    }

    /** 明細の数量の正負から契約タイプを決定する. */
    private ContractType determineContractType() {
        final boolean hasCancellation = lines.stream().anyMatch(QuoteLine::isCancellation);
        final boolean hasAddition = lines.stream().anyMatch(line -> !line.isCancellation());
        if (hasCancellation && hasAddition) {
            return ContractType.EARLY_RENEWAL;
        }
        return hasCancellation ? ContractType.CANCELLATION : ContractType.NEW;
    }

    /**
     * 解約を除外して集計し, 全体割引を適用した後に円単位で四捨五入する.
     * 年額合計は数量を含む 1 年分, 合計金額はそれに契約月数 / 12 を掛けた契約期間の総額とする.
     */
    private QuoteTotals calculateTotals() {
        BigDecimal annual = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        final BigDecimal months = BigDecimal.valueOf(terms.contractMonths());
        for (final QuoteLine line : lines) {
            if (!line.isCancellation()) {
                final BigDecimal lineAnnual = line.calculateAnnual();
                annual = annual.add(lineAnnual);
                amount = amount.add(line.listPrice().multiply(BigDecimal.valueOf(line.amount())).multiply(months));
            }
        }
        final BigDecimal multiplier = discountRate.calculateMultiplier();
        return new QuoteTotals(
                annual.multiply(multiplier).setScale(0, RoundingMode.HALF_UP),
                amount.multiply(multiplier).setScale(0, RoundingMode.HALF_UP));
    }
}
