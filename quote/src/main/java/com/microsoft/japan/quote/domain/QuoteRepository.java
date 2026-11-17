package com.microsoft.japan.quote.domain;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** 見積もり集約の保存先を抽象化するポート. */
public interface QuoteRepository {

    /** 新しい識別子の見積もりを登録する. */
    Quote create(Quote quote);

    /** 識別子に一致する見積もりを検索する. */
    Optional<Quote> findById(QuoteId id);

    /** 見積もり一覧を返す. */
    List<Quote> findAll();

    /** 存在する集約に対する変更を原子的に実行し、不正な変更は保存しない. */
    Optional<Quote> update(QuoteId id, UnaryOperator<Quote> change);

    /** 見積もりを削除し、削除対象が存在したかを返す. */
    boolean delete(QuoteId id);
}
