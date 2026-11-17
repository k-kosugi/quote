package com.microsoft.japan.quote.adapter.out.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

import com.microsoft.japan.quote.domain.Quote;
import com.microsoft.japan.quote.domain.QuoteId;
import com.microsoft.japan.quote.domain.QuoteRepository;

/** アプリケーション再起動で消えるスレッドセーフなメモリ保存先. */
public final class InMemoryQuoteRepository implements QuoteRepository {

    private final ConcurrentMap<QuoteId, Quote> quotes = new ConcurrentHashMap<>();

    /** 既存データを上書きせずに見積もりを登録する. */
    @Override
    public Quote create(final Quote quote) {
        if (quotes.putIfAbsent(quote.getId(), quote) != null) {
            throw new IllegalStateException("Quote ID already exists: " + quote.getId().value());
        }
        return quote;
    }

    /** 不変の見積もりを識別子で検索する. */
    @Override
    public Optional<Quote> findById(final QuoteId id) {
        return Optional.ofNullable(this.quotes.get(id));
    }

    /** 識別子の順序で見積もり一覧を返す. */
    @Override
    public List<Quote> findAll() {
        return this.quotes.values().stream()
                .sorted(Comparator.comparing(quote -> quote.getId().value()))
                .toList();
    }

    /** 同一識別子の操作を直列化し、変更処理が失敗した場合は元の値を維持する. */
    @Override
    public Optional<Quote> update(final QuoteId id, final UnaryOperator<Quote> change) {
        return Optional.ofNullable(this.quotes.computeIfPresent(id, (key, quote) -> change.apply(quote)));
    }

    /** 対象の見積もりを原子的に削除する. */
    @Override
    public boolean delete(final QuoteId id) {
        return this.quotes.remove(id) != null;
    }
}
