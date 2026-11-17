package com.microsoft.japan.quote.adapter.in.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.microsoft.japan.quote.application.CatalogUnavailableException;
import com.microsoft.japan.quote.application.QuoteNotFoundException;
import com.microsoft.japan.quote.domain.InvalidQuoteException;

/** 入力エラーと見積もり未存在を Problem Details 形式で通知する. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class QuoteExceptionHandler {

    /** 業務制約違反の具体的な理由を返す. */
    @ExceptionHandler(InvalidQuoteException.class)
    public ProblemDetail handleInvalidQuote(final InvalidQuoteException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /** 対象が見つからなかったことを通知する. */
    @ExceptionHandler(QuoteNotFoundException.class)
    public ProblemDetail handleNotFound(final QuoteNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** SKU カタログに接続できず単価を確認できないことを通知する. */
    @ExceptionHandler(CatalogUnavailableException.class)
    public ProblemDetail handleCatalogUnavailable(final CatalogUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    /** JSON の構文と型の問題を通知し、内部情報は公開しない. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(final HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body must be valid JSON with correctly typed fields.");
    }

    /** パスに指定された識別子の型の問題を通知する. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleInvalidId(final MethodArgumentTypeMismatchException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Quote ID must be a UUID.");
    }
}
