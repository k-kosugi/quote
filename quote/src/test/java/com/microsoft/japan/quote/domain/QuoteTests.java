package com.microsoft.japan.quote.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** 外部依存なしで見積もりの契約条件と計算を検証する. */
final class QuoteTests {

    private static final QuoteId ID = new QuoteId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final LocalDate CREATED_ON = LocalDate.of(2026, 6, 16);
    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final QuoteTerms TERMS = new QuoteTerms(START, START.plusYears(3).minusDays(1));
    private static final DiscountRate NO_DISCOUNT = new DiscountRate(BigDecimal.ZERO);

    /** 月末や年末およびうるう年をまたぐ場合も作成日の 14 日後を期限にする. */
    @ParameterizedTest
    @CsvSource({"2026-11-11,2026-11-25", "2026-12-25,2027-01-08", "2028-02-20,2028-03-05"})
    @DisplayName("作成日が渡された場合有効期限が14日後になる")
    void 作成日が渡された場合有効期限が14日後になる(final LocalDate createdOn, final LocalDate expected) {
        final Quote quote = Quote.createDraft(ID, createdOn, TERMS,
                List.of(line("SKU-A", "100", 1)), NO_DISCOUNT);
        assertEquals(expected, quote.getValidUntil());
    }

    /** 修正や保存を繰り返しても元の作成日を維持する. */
    @Test
    @DisplayName("見積もりに修正と保存が渡された場合有効期限が延長されない")
    void 見積もりに修正と保存が渡された場合有効期限が延長されない() {
        final Quote quote = create(List.of(line("SKU-A", "100", 1)), "0");
        final Quote revised = quote.markSaved()
                .revise(TERMS, List.of(line("SKU-B", "200", 2)), NO_DISCOUNT).markSaved();
        assertEquals(LocalDate.of(2026, 6, 30), revised.getValidUntil());
    }

    /** 作成日を指定しない集約を許可しない. */
    @Test
    @DisplayName("作成日にnullが渡された場合InvalidQuoteExceptionが発生する")
    void 作成日にnullが渡された場合InvalidQuoteExceptionが発生する() {
        assertThrows(InvalidQuoteException.class, () -> Quote.createDraft(ID, null, TERMS,
                List.of(line("SKU-A", "100", 1)), NO_DISCOUNT));
    }

    /** 集約の必須オブジェクトが欠落した状態を拒否する. */
    @ParameterizedTest
    @MethodSource("missingAggregateValues")
    @DisplayName("集約のIDと契約条件と割引率のいずれかにnullが渡された場合InvalidQuoteExceptionが発生する")
    void 集約の必須項目にnullが渡された場合InvalidQuoteExceptionが発生する(
            final QuoteId id, final QuoteTerms terms, final DiscountRate rate) {
        assertThrows(InvalidQuoteException.class, () -> Quote.createDraft(id, CREATED_ON, terms,
                List.of(line("SKU-A", "100", 1)), rate));
    }

    /** 集約が保持する必須オブジェクトの欠落パターンを供給する. */
    private static Stream<Arguments> missingAggregateValues() {
        return Stream.of(
                Arguments.of(null, TERMS, NO_DISCOUNT),
                Arguments.of(ID, null, NO_DISCOUNT),
                Arguments.of(ID, TERMS, null));
    }

    /** 解約行の年額を 0 とし数量なしと数量ありの合計を区別する. */
    @Test
    @DisplayName("正負の明細に1000円2件と2000円3件と500円の解約が渡された場合正常終了する")
    void 明細に正負の数量が渡された場合解約を除外して集計する() {
        final Quote quote = create(List.of(line("SKU-A", "1000", 2),
                line("SKU-B", "2000", 3), line("SKU-C", "500", -1)), "0");

        assertEquals(new BigDecimal("24000"), quote.getLines().get(0).calculateAnnual());
        assertEquals(new BigDecimal("72000"), quote.getLines().get(1).calculateAnnual());
        assertEquals(BigDecimal.ZERO, quote.getLines().get(2).calculateAnnual());
        assertEquals(new BigDecimal("96000"), quote.getTotals().totalAnnual());
        assertEquals(new BigDecimal("288000"), quote.getTotals().totalAmount());
    }

    /** 全行を合算した後に全体割引と四捨五入を適用する. */
    @Test
    @DisplayName("月額0.625円の2明細に割引10が渡された場合合算後に円単位で四捨五入する")
    void 割引に10が渡された場合全体集計後に四捨五入する() {
        final Quote quote = create(List.of(line("SKU-A", "0.625", 2),
                line("SKU-A", "0.625", 3)), "10");

        assertEquals(new BigDecimal("34"), quote.getTotals().totalAnnual());
        assertEquals(new BigDecimal("101"), quote.getTotals().totalAmount());
    }

    /** 割引後の 1.5 円を境に切り捨てと切り上げを区別する. */
    @Test
    @DisplayName("割引後の年額合計の丸め境界値に1.47円と1.50円と1.53円が渡された場合1円と2円と2円になる")
    void 割引後金額の丸め境界値前後が渡された場合四捨五入する() {
        final List<QuoteLine> lines = List.of(line("SKU-A", "0.25", 1));
        assertEquals(new QuoteTotals(BigDecimal.ONE, new BigDecimal("4")), create(lines, "51").getTotals());
        assertEquals(new QuoteTotals(new BigDecimal("2"), new BigDecimal("5")),
                create(lines, "50").getTotals());
        assertEquals(new QuoteTotals(new BigDecimal("2"), new BigDecimal("5")),
                create(lines, "49").getTotals());
    }

    /** 解約のみの見積もりを負の請求額にしない. */
    @Test
    @DisplayName("全明細に負数が渡された場合年額合計は0になる")
    void 明細に解約のみが渡された場合合計が0になる() {
        final Quote quote = create(List.of(line("SKU-A", "1000", -3)), "30");
        assertEquals(new QuoteTotals(BigDecimal.ZERO, BigDecimal.ZERO), quote.getTotals());
    }

    /** 契約タイプを入力値ではなく数量の組み合わせから判定する. */
    @ParameterizedTest
    @CsvSource({"1,2,NEW", "-1,-2,CANCELLATION", "-1,1,EARLY_RENEWAL", "1,-1,EARLY_RENEWAL"})
    @DisplayName("明細に正負の組み合わせが渡された場合対応する契約タイプになる")
    void 明細に正負の数量が渡された場合契約タイプを判定する(
            final int first, final int second, final ContractType expected) {
        final Quote quote = create(List.of(line("SKU-A", "1000", first),
                line("SKU-A", "2000", second)), "0");
        assertEquals(expected, quote.getContractType());
    }

    /** 数量 0 の前後の境界値を検証する. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    @DisplayName("数量の境界値に-1と0と1が渡された場合0だけInvalidQuoteExceptionが発生する")
    void 数量の境界値前後が渡された場合0だけInvalidQuoteExceptionが発生する(final int amount) {
        if (amount == 0) {
            assertThrows(InvalidQuoteException.class, () -> line("SKU-A", "100", amount));
        } else {
            assertEquals(amount < 0 ? BigDecimal.ZERO : new BigDecimal("1200"),
                    line("SKU-A", "100", amount).calculateAnnual());
        }
    }

    /** 数量の型の限界でも乗算をオーバーフローさせない. */
    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE})
    @DisplayName("数量の型境界値前後が渡された場合金額がオーバーフローしない")
    void 数量に型の境界値前後が渡された場合正常終了する(final int amount) {
        final Quote quote = create(List.of(line("SKU-A", "1000", amount)), "0");
        final BigDecimal expected = amount < 0 ? BigDecimal.ZERO
                : new BigDecimal("36000").multiply(BigDecimal.valueOf(amount));
        assertEquals(expected, quote.getTotals().totalAmount());
    }

    /** 非負の月額単価の境界値を検証する. */
    @ParameterizedTest
    @ValueSource(strings = {"-1", "0", "1"})
    @DisplayName("単価の境界値に-1と0と1が渡された場合-1だけInvalidQuoteExceptionが発生する")
    void 単価の境界値前後が渡された場合負数だけInvalidQuoteExceptionが発生する(final String price) {
        if (new BigDecimal(price).signum() < 0) {
            assertThrows(InvalidQuoteException.class, () -> line("SKU-A", price, 1));
        } else {
            assertEquals(new BigDecimal(price).multiply(new BigDecimal("12")),
                    line("SKU-A", price, 1).calculateAnnual());
        }
    }

    /** 割引率の上下限それぞれの境界値を検証する. */
    @ParameterizedTest
    @CsvSource({"-1,invalid", "0,3600", "1,3564", "99,36", "100,0", "101,invalid"})
    @DisplayName("割引率の境界値前後が渡された場合範囲に応じた結果になる")
    void 割引率の境界値前後が渡された場合範囲に応じた結果になる(
            final String rate, final String expected) {
        if ("invalid".equals(expected)) {
            assertThrows(InvalidQuoteException.class, () -> create(List.of(line("SKU-A", "100", 1)), rate));
        } else {
            assertEquals(new BigDecimal(expected),
                    create(List.of(line("SKU-A", "100", 1)), rate).getTotals().totalAmount());
        }
    }

    /** 同日契約を含む開始日と終了日の境界を検証する. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    @DisplayName("終了日の境界値に開始日前日と同日と翌日が渡された場合前日だけ拒否する")
    void 契約日の境界値前後が渡された場合逆転だけInvalidQuoteExceptionが発生する(final int offset) {
        if (offset < 0) {
            assertThrows(InvalidQuoteException.class,
                    () -> new QuoteTerms(START, START.plusDays(offset)));
        } else {
            final Quote quote = Quote.createDraft(ID, CREATED_ON,
                    new QuoteTerms(START, START.plusDays(offset)),
                    List.of(line("SKU-A", "100", 1)), NO_DISCOUNT);
            assertEquals(new BigDecimal("100"), quote.getTotals().totalAmount());
        }
    }

    /** SKU の排他制約は解約行も含めた組み合わせに適用する. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    @DisplayName("明細にSKU-STANDARDとSKU-PROFESSIONALが渡された場合InvalidQuoteExceptionが発生する")
    void 明細に禁止SKUの組み合わせが渡された場合InvalidQuoteExceptionが発生する(final int amount) {
        assertThrows(InvalidQuoteException.class, () -> create(List.of(
                line("SKU-STANDARD", "100", amount), line("SKU-PROFESSIONAL", "200", 1)), "0"));
    }

    /** 排他 SKU をそれぞれ単独で使用することは許可する. */
    @ParameterizedTest
    @ValueSource(strings = {"SKU-STANDARD", "SKU-PROFESSIONAL"})
    @DisplayName("明細に排他SKUが単独で渡された場合正常終了する")
    void 明細に排他SKUが単独で渡された場合正常終了する(final String sku) {
        assertDoesNotThrow(() -> create(List.of(line(sku, "100", 1)), "0"));
    }

    /** 前後の空白で SKU の排他制約を迂回できないことを検証する. */
    @Test
    @DisplayName("明細に空白付きの禁止SKUが渡された場合InvalidQuoteExceptionが発生する")
    void 明細に空白付きの禁止SKUが渡された場合InvalidQuoteExceptionが発生する() {
        assertThrows(InvalidQuoteException.class, () -> create(List.of(
                line(" SKU-STANDARD ", "100", 1), line(" SKU-PROFESSIONAL ", "200", 1)), "0"));
    }

    /** 明細の件数の境界を検証する. */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    @DisplayName("明細件数の境界値に0と1と2が渡された場合0だけ拒否する")
    void 明細件数の境界値前後が渡された場合0だけInvalidQuoteExceptionが発生する(final int count) {
        final List<QuoteLine> lines = java.util.Collections.nCopies(count, line("SKU-A", "100", 1));
        if (count == 0) {
            assertThrows(InvalidQuoteException.class, () -> create(lines, "0"));
        } else {
            assertEquals(new BigDecimal("3600").multiply(BigDecimal.valueOf(count)),
                    create(lines, "0").getTotals().totalAmount());
        }
    }

    /** null の明細を集約へ入れない. */
    @ParameterizedTest
    @MethodSource("invalidLines")
    @DisplayName("明細にnullが渡された場合InvalidQuoteExceptionが発生する")
    void 明細にnullが渡された場合InvalidQuoteExceptionが発生する(final List<QuoteLine> lines) {
        assertThrows(InvalidQuoteException.class, () -> create(lines, "0"));
    }

    /** null の一覧と null を含む一覧を供給する. */
    private static Stream<List<QuoteLine>> invalidLines() {
        return Stream.of(null, Arrays.asList((QuoteLine) null));
    }

    /** SKU の空入力を拒否する. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("SKUに空入力が渡された場合InvalidQuoteExceptionが発生する")
    void SKUに空入力が渡された場合InvalidQuoteExceptionが発生する(final String sku) {
        assertThrows(InvalidQuoteException.class, () -> line(sku, "100", 1));
    }

    /** 必須条件が欠けた値を拒否する. */
    @Test
    @DisplayName("必須の値にnullが渡された場合InvalidQuoteExceptionが発生する")
    void 必須の値にnullが渡された場合InvalidQuoteExceptionが発生する() {
        assertThrows(InvalidQuoteException.class, () -> new QuoteId(null));
        assertThrows(InvalidQuoteException.class, () -> new DiscountRate(null));
        assertThrows(InvalidQuoteException.class, () -> new QuoteLine("SKU-A", null, 1));
        assertThrows(InvalidQuoteException.class, () -> new QuoteTerms(null, START));
        assertThrows(InvalidQuoteException.class, () -> new QuoteTerms(START, null));
    }

    /** 保存後の修正は識別子を維持してドラフトへ戻す. */
    @Test
    @DisplayName("保存済み見積もりに修正内容が渡された場合ドラフトに戻り再計算される")
    void 保存済み見積もりに修正内容が渡された場合ドラフトに戻る() {
        final Quote saved = create(List.of(line("SKU-A", "100", 1)), "0").markSaved();
        final Quote revised = saved.revise(TERMS, List.of(line("SKU-B", "200", 2)), NO_DISCOUNT);

        assertEquals(QuoteStatus.SAVED, saved.getStatus());
        assertEquals(ID, revised.getId());
        assertEquals(QuoteStatus.DRAFT, revised.getStatus());
        assertEquals(new BigDecimal("14400"), revised.getTotals().totalAmount());
    }

    /** 呼び出し側の一覧変更で制約を迂回できないことを検証する. */
    @Test
    @DisplayName("作成後に入力一覧が変更された場合でも見積もりは変更されない")
    void 入力一覧に作成後の変更が渡された場合元の見積もりを維持する() {
        final List<QuoteLine> lines = new ArrayList<>(List.of(line("SKU-A", "100", 1)));
        final Quote quote = create(lines, "0");
        lines.clear();
        assertEquals(1, quote.getLines().size());
        assertThrows(UnsupportedOperationException.class, () -> quote.getLines().clear());
    }

    /** 一定の契約条件で集約を構築する. */
    private static Quote create(final List<QuoteLine> lines, final String rate) {
        return Quote.createDraft(ID, CREATED_ON, TERMS, lines, new DiscountRate(new BigDecimal(rate)));
    }

    /** テスト用の明細を生成する. */
    private static QuoteLine line(final String sku, final String price, final int amount) {
        return new QuoteLine(sku, new BigDecimal(price), amount);
    }
}
