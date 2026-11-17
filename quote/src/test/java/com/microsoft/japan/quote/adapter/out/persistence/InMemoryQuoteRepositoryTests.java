package com.microsoft.japan.quote.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.japan.quote.domain.DiscountRate;
import com.microsoft.japan.quote.domain.InvalidQuoteException;
import com.microsoft.japan.quote.domain.Quote;
import com.microsoft.japan.quote.domain.QuoteId;
import com.microsoft.japan.quote.domain.QuoteLine;
import com.microsoft.japan.quote.domain.QuoteTerms;

/** メモリ保存先が保証する更新と削除の契約を検証する. */
final class InMemoryQuoteRepositoryTests {

    private static final QuoteId FIRST = new QuoteId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final QuoteId SECOND = new QuoteId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final QuoteTerms TERMS = new QuoteTerms(
            LocalDate.of(2026, 12, 1), LocalDate.of(2027, 11, 30));
    private static final DiscountRate NO_DISCOUNT = new DiscountRate(BigDecimal.ZERO);

    private final InMemoryQuoteRepository repository = new InMemoryQuoteRepository();

    /** 同じ ID の登録で既存の集約を上書きしない. */
    @Test
    @DisplayName("登録に重複IDが渡された場合IllegalStateExceptionが発生し元の集約を維持する")
    void 登録に重複IDが渡された場合IllegalStateExceptionが発生する() {
        final Quote original = repository.create(draft(FIRST));

        assertThrows(IllegalStateException.class, () -> repository.create(original.markSaved()));
        assertSame(original, repository.findById(FIRST).orElseThrow());
    }

    /** 未存在の更新で変更関数を呼び出したりデータを作成したりしない. */
    @Test
    @DisplayName("更新に未存在IDが渡された場合変更処理を実行せず空を返す")
    void 更新に未存在IDが渡された場合正常終了する() {
        assertTrue(repository.update(FIRST, quote -> {
            throw new AssertionError("Missing quote must not be updated.");
        }).isEmpty());
        assertTrue(repository.findAll().isEmpty());
    }

    /** 更新関数が失敗した場合は保存先の元の値を維持する. */
    @Test
    @DisplayName("更新に禁止SKUの変更が渡された場合InvalidQuoteExceptionが発生し元の集約を維持する")
    void 更新に禁止SKUが渡された場合InvalidQuoteExceptionが発生する() {
        final Quote original = repository.create(draft(FIRST).markSaved());
        final List<QuoteLine> invalidLines = List.of(
                new QuoteLine("SKU-STANDARD", BigDecimal.ONE, 1),
                new QuoteLine("SKU-PROFESSIONAL", BigDecimal.ONE, 1));

        assertThrows(InvalidQuoteException.class,
                () -> repository.update(FIRST, quote -> quote.revise(TERMS, invalidLines, NO_DISCOUNT)));
        assertSame(original, repository.findById(FIRST).orElseThrow());
    }

    /** 更新関数の戻り値を保存先に反映する. */
    @Test
    @DisplayName("更新に保存操作が渡された場合変更後の集約を保持する")
    void 更新に保存操作が渡された場合正常終了する() {
        repository.create(draft(FIRST));
        final Quote saved = repository.update(FIRST, Quote::markSaved).orElseThrow();
        assertSame(saved, repository.findById(FIRST).orElseThrow());
    }

    /** 削除済みデータは更新操作で復活しない. */
    @Test
    @DisplayName("削除済みIDが更新に渡された場合データは復活しない")
    void 更新に削除済みIDが渡された場合正常終了する() {
        repository.create(draft(FIRST));

        assertTrue(repository.delete(FIRST));
        assertFalse(repository.delete(FIRST));
        assertTrue(repository.update(FIRST, Quote::markSaved).isEmpty());
        assertTrue(repository.findById(FIRST).isEmpty());
    }

    /** 登録順ではなく ID 順で一覧を取得する. */
    @Test
    @DisplayName("逆順のIDが登録された場合一覧はIDの昇順になる")
    void 登録に逆順のIDが渡された場合正常終了する() {
        repository.create(draft(SECOND));
        repository.create(draft(FIRST));
        assertEquals(List.of(FIRST, SECOND), repository.findAll().stream().map(Quote::getId).toList());
    }

    /** 一定の内容を持つドラフトを生成する. */
    private static Quote draft(final QuoteId id) {
        return Quote.createDraft(id, LocalDate.of(2026, 11, 11), TERMS,
                List.of(new QuoteLine("SKU-A", new BigDecimal("100"), 1)), NO_DISCOUNT);
    }
}
