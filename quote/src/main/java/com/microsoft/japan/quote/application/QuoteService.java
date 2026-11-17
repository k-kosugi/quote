package com.microsoft.japan.quote.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.microsoft.japan.quote.domain.InvalidQuoteException;
import com.microsoft.japan.quote.domain.Quote;
import com.microsoft.japan.quote.domain.QuoteId;
import com.microsoft.japan.quote.domain.QuoteLine;
import com.microsoft.japan.quote.domain.QuoteRepository;

/** 見積もりのライフサイクルを提供するアプリケーションサービス. */
public final class QuoteService {

    private final QuoteRepository repository;
    private final SkuPriceProvider priceProvider;
    private final Clock clock;

    /** 保存先と SKU の価格取得のポート, 作成日の取得に使う時計を受け取る. */
    public QuoteService(final QuoteRepository repository, final SkuPriceProvider priceProvider,
            final Clock clock) {
        this.repository = repository;
        this.priceProvider = priceProvider;
        this.clock = clock;
    }

    /** 新しい識別子を発行してドラフトを登録する. */
    public Quote create(final QuoteCommand command) {
        final Quote quote = Quote.createDraft(new QuoteId(UUID.randomUUID()), LocalDate.now(clock),
                command.terms(), resolveLines(command), command.discountRate());
        return this.repository.create(quote);
    }

    /** 見積もり一覧を取得する. */
    public List<Quote> findAll() {
        return this.repository.findAll();
    }

    /** 見積もりを取得し、存在しなければ明示的に失敗する. */
    public Quote findById(final QuoteId id) {
        return this.repository.findById(id).orElseThrow(() -> new QuoteNotFoundException(id));
    }

    /** 内容を全置換してドラフト状態にする. */
    public Quote revise(final QuoteId id, final QuoteCommand command) {
        return this.repository.update(id,
                quote -> quote.revise(command.terms(), this.resolveLines(command), command.discountRate()))
                .orElseThrow(() -> new QuoteNotFoundException(id));
    }

    /** 見積もりを保存済み状態にする. */
    public Quote save(final QuoteId id) {
        return this.repository.update(id, Quote::markSaved)
                .orElseThrow(() -> new QuoteNotFoundException(id));
    }

    /** 見積もりを削除する. */
    public void delete(final QuoteId id) {
        if (!repository.delete(id)) {
            throw new QuoteNotFoundException(id);
        }
    }

    /** 依頼明細の単価をカタログから解決し, 存在しない SKU は拒否する. */
    private List<QuoteLine> resolveLines(final QuoteCommand command) {
        return command.lines().stream()
                .map(line -> new QuoteLine(line.sku(), this.priceProvider.findMonthlyListPrice(line.sku())
                        .orElseThrow(() -> new InvalidQuoteException("Unknown SKU: " + line.sku() + ".")),
                        line.amount()))
                .toList();
    }
}
