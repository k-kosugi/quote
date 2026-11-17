package com.microsoft.japan.quote.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.microsoft.japan.quote.domain.DiscountRate;
import com.microsoft.japan.quote.domain.InvalidQuoteException;
import com.microsoft.japan.quote.domain.Quote;
import com.microsoft.japan.quote.domain.QuoteId;
import com.microsoft.japan.quote.domain.QuoteLine;
import com.microsoft.japan.quote.domain.QuoteRepository;
import com.microsoft.japan.quote.domain.QuoteStatus;
import com.microsoft.japan.quote.domain.QuoteTerms;

/** フェイクのポートと固定時計で見積もりのユースケースを検証する. */
final class QuoteServiceTests {

    private static final QuoteId ID = new QuoteId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final LocalDate CREATED_ON = LocalDate.of(2026, 11, 11);
    private static final QuoteTerms TERMS = new QuoteTerms(
            LocalDate.of(2026, 12, 1), LocalDate.of(2027, 11, 30));
    private static final DiscountRate NO_DISCOUNT = new DiscountRate(BigDecimal.ZERO);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-11T15:00:00Z"), ZoneId.of("Asia/Tokyo"));

    private final FakeQuoteRepository repository = new FakeQuoteRepository();
    private final QuoteService service = new QuoteService(repository, new FakeSkuPriceProvider(), CLOCK);

    /** UTC ではなく注入した時計のタイムゾーンで作成日を決める. */
    @Test
    @DisplayName("作成に日本時間11月12日0時の時計が渡された場合期限11月26日のドラフトを登録する")
    void 作成に日本時間の日付境界の時計が渡された場合正常終了する() {
        final Quote quote = service.create(command("SKU-A", 1));
        assertEquals(LocalDate.of(2026, 11, 26), quote.getValidUntil());
        assertEquals(QuoteStatus.DRAFT, quote.getStatus());
        assertSame(quote, repository.findById(quote.getId()).orElseThrow());
    }

    /** 単価は利用者の入力ではなくカタログの値で決まる. */
    @Test
    @DisplayName("作成にSKUと数量が渡された場合単価はカタログの値で登録する")
    void 作成にSKUと数量が渡された場合カタログの単価で登録する() {
        final Quote quote = service.create(new QuoteCommand(TERMS,
                List.of(new RequestedLine("SKU-PRICED", 2)), NO_DISCOUNT));

        assertEquals(new BigDecimal("250"), quote.getLines().get(0).listPrice());
        assertEquals(new BigDecimal("6000"), quote.getTotals().totalAmount());
    }

    /** カタログに存在しない SKU を見積もりに載せない. */
    @Test
    @DisplayName("作成にカタログに存在しないSKUが渡された場合InvalidQuoteExceptionが発生し登録されない")
    void 作成に未登録SKUが渡された場合InvalidQuoteExceptionが発生する() {
        final QuoteCommand command = command("SKU-UNKNOWN", 1);

        assertThrows(InvalidQuoteException.class, () -> service.create(command));
        assertTrue(repository.findAll().isEmpty());
    }

    /** 不正な集約を作成して保存先へ渡さない. */
    @Test
    @DisplayName("作成に禁止SKUが渡された場合InvalidQuoteExceptionが発生し登録されない")
    void 作成に禁止SKUが渡された場合InvalidQuoteExceptionが発生する() {
        final QuoteCommand command = incompatibleCommand();
        assertThrows(InvalidQuoteException.class, () -> service.create(command));
        assertTrue(repository.findAll().isEmpty());
    }

    /** 全ての単件操作で未存在を例外として通知する. */
    @ParameterizedTest
    @ValueSource(strings = {"find", "revise", "save", "delete"})
    @DisplayName("単件操作に未存在のIDが渡された場合QuoteNotFoundExceptionが発生する")
    void 単件操作に未存在IDが渡された場合QuoteNotFoundExceptionが発生する(final String operation) {
        assertThrows(QuoteNotFoundException.class, () -> {
            switch (operation) {
                case "find" -> service.findById(ID);
                case "revise" -> service.revise(ID, command("SKU-A", 1));
                case "save" -> service.save(ID);
                case "delete" -> service.delete(ID);
                default -> throw new AssertionError("Unexpected operation: " + operation);
            }
        });
        assertTrue(repository.findAll().isEmpty());
    }

    /** 修正結果が保存先にも反映され、元の有効期限を維持する. */
    @Test
    @DisplayName("修正に保存済みIDと新しい明細が渡された場合ドラフトを再計算して保存する")
    void 修正に保存済みIDが渡された場合正常終了する() {
        repository.create(draft(ID).markSaved());

        final Quote revised = service.revise(ID, command("SKU-B", 2));
        final Quote stored = repository.findById(ID).orElseThrow();

        assertSame(revised, stored);
        assertEquals(QuoteStatus.DRAFT, stored.getStatus());
        assertEquals(new BigDecimal("2400"), stored.getTotals().totalAmount());
        assertEquals(LocalDate.of(2026, 11, 25), stored.getValidUntil());
    }

    /** 更新途中の検証失敗で元の保存状態を失わない. */
    @Test
    @DisplayName("修正にカタログに存在しないSKUが渡された場合InvalidQuoteExceptionが発生し元の集約を維持する")
    void 修正に未登録SKUが渡された場合InvalidQuoteExceptionが発生する() {
        final Quote saved = repository.create(draft(ID).markSaved());

        assertThrows(InvalidQuoteException.class, () -> service.revise(ID, command("SKU-UNKNOWN", 1)));
        assertSame(saved, repository.findById(ID).orElseThrow());
    }

    /** 更新途中の検証失敗で元の保存状態を失わない. */
    @Test
    @DisplayName("修正に禁止SKUが渡された場合InvalidQuoteExceptionが発生し元の集約を維持する")
    void 修正に禁止SKUが渡された場合InvalidQuoteExceptionが発生する() {
        final Quote saved = repository.create(draft(ID).markSaved());

        assertThrows(InvalidQuoteException.class, () -> service.revise(ID, incompatibleCommand()));
        assertSame(saved, repository.findById(ID).orElseThrow());
    }

    /** 保存操作が戻り値だけでなく保存先の状態を更新する. */
    @Test
    @DisplayName("保存にドラフトのIDが渡された場合保存済み状態を保持する")
    void 保存にドラフトのIDが渡された場合正常終了する() {
        repository.create(draft(ID));

        final Quote saved = service.save(ID);

        assertEquals(QuoteStatus.SAVED, repository.findById(ID).orElseThrow().getStatus());
        assertSame(saved, repository.findById(ID).orElseThrow());
        assertEquals(LocalDate.of(2026, 11, 25), saved.getValidUntil());
    }

    /** 削除対象以外の見積もりを保持する. */
    @Test
    @DisplayName("削除に登録済みIDが渡された場合対象だけを削除する")
    void 削除に登録済みIDが渡された場合正常終了する() {
        final QuoteId otherId = new QuoteId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        repository.create(draft(ID));
        final Quote other = repository.create(draft(otherId));

        service.delete(ID);

        assertFalse(repository.findById(ID).isPresent());
        assertSame(other, repository.findById(otherId).orElseThrow());
    }

    /** テスト用の変更コマンドを生成する. */
    private static QuoteCommand command(final String sku, final int amount) {
        return new QuoteCommand(TERMS, List.of(new RequestedLine(sku, amount)), NO_DISCOUNT);
    }

    /** 組み合わせ制約に違反するコマンドを生成する. */
    private static QuoteCommand incompatibleCommand() {
        return new QuoteCommand(TERMS, List.of(
                new RequestedLine("SKU-STANDARD", 1),
                new RequestedLine("SKU-PROFESSIONAL", 1)), NO_DISCOUNT);
    }

    /** 固定の作成日を持つドラフトを生成する. */
    private static Quote draft(final QuoteId id) {
        return Quote.createDraft(id, CREATED_ON, TERMS,
                List.of(new QuoteLine("SKU-A", new BigDecimal("100"), 1)), NO_DISCOUNT);
    }

    /** カタログに依存せず固定の単価を返すフェイク. */
    private static final class FakeSkuPriceProvider implements SkuPriceProvider {

        private static final Map<String, BigDecimal> PRICES = Map.of(
                "SKU-A", new BigDecimal("100"),
                "SKU-B", new BigDecimal("100"),
                "SKU-PRICED", new BigDecimal("250"),
                "SKU-STANDARD", BigDecimal.ONE,
                "SKU-PROFESSIONAL", BigDecimal.ONE);

        /** 登録済みの SKU の単価だけを返す. */
        @Override
        public Optional<BigDecimal> findMonthlyListPrice(final String sku) {
            return Optional.ofNullable(PRICES.get(sku));
        }
    }

    /** ユースケースを実際の保存アダプターから独立させるフェイク. */
    private static final class FakeQuoteRepository implements QuoteRepository {

        private final Map<QuoteId, Quote> quotes = new HashMap<>();

        /** 見積もりをテスト用の保存先に登録する. */
        @Override
        public Quote create(final Quote quote) {
            quotes.put(quote.getId(), quote);
            return quote;
        }

        /** 識別子で保存済みの集約を取得する. */
        @Override
        public Optional<Quote> findById(final QuoteId id) {
            return Optional.ofNullable(quotes.get(id));
        }

        /** 保存済みの集約一覧を返す. */
        @Override
        public List<Quote> findAll() {
            return List.copyOf(quotes.values());
        }

        /** 変更処理に成功した集約のみ置換する. */
        @Override
        public Optional<Quote> update(final QuoteId id, final UnaryOperator<Quote> change) {
            final Quote current = quotes.get(id);
            if (current == null) {
                return Optional.empty();
            }
            final Quote changed = change.apply(current);
            quotes.put(id, changed);
            return Optional.of(changed);
        }

        /** 対象の集約を削除する. */
        @Override
        public boolean delete(final QuoteId id) {
            return quotes.remove(id) != null;
        }
    }
}
