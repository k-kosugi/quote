package com.microsoft.japan.quote.application;

import java.util.List;

import com.microsoft.japan.quote.domain.DiscountRate;
import com.microsoft.japan.quote.domain.QuoteTerms;

/** 作成と修正に必要な検証済みの入力値. 明細の単価は含まない. */
public record QuoteCommand(QuoteTerms terms, List<RequestedLine> lines, DiscountRate discountRate) {
}
