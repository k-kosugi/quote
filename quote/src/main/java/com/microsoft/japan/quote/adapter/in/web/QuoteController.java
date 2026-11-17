package com.microsoft.japan.quote.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.microsoft.japan.quote.application.QuoteService;
import com.microsoft.japan.quote.domain.QuoteId;

/** 見積もりのユースケースを HTTP API として公開する. */
@RestController
@RequestMapping("/api/quotes")
@Tag(name = "見積", description = "見積の作成, 取得, 修正, 保存, 削除を行います.")
public final class QuoteController {

    private final QuoteService service;

    /** ユースケースを受け取る. */
    public QuoteController(final QuoteService service) {
        this.service = service;
    }

    /** ドラフトを作成して識別子と Location を返す. */
    @Operation(operationId = "createQuote", summary = "下書きの見積を作成して保持",
            description = """
                    リクエストの全項目を検証し, 新しい UUID を割り当てて DRAFT の見積を作成します.
                    成功時は作成した見積と, 取得用 URL を示す Location ヘッダーを返します.
                    見積データはプロセス内メモリに保持されます.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "下書きを作成しました",
                    headers = @Header(
                            name = "Location",
                            description = "作成した見積の相対 URL",
                            schema = @Schema(type = "string", format = "uri-reference"))),
            @ApiResponse(responseCode = "400", description = "リクエストまたは業務ルールが不正です",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<QuoteResponse> create(@RequestBody final QuoteRequest request) {
        final QuoteResponse response = QuoteResponse.from(service.create(request.toCommand()));
        return ResponseEntity.created(URI.create("/api/quotes/" + response.id())).body(response);
    }

    /** 全見積もりの一覧を返す. */
    @Operation(operationId = "listQuotes", summary = "下書きと保存済みの見積一覧を取得",
            description = "プロセス内メモリに保存されている DRAFT と SAVED の見積を UUID 順に返します.")
    @ApiResponse(responseCode = "200", description = "保存済みの見積一覧. 見積がない場合は空の配列を返します.")
    @GetMapping
    public List<QuoteResponse> findAll() {
        return this.service.findAll().stream().map(QuoteResponse::from).toList();
    }

    /** 識別子に一致する見積もりを返す. */
    @Operation(operationId = "getQuote", summary = "見積を取得",
            description = "パスの UUID に一致する見積を返します. 見積は作成日から14日後の validUntil を持ちます.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "見積を返します"),
            @ApiResponse(responseCode = "400", description = "UUID の形式が不正です",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "見積が存在しません",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public QuoteResponse findById(
            @Parameter(description = "見積の UUID", required = true) @PathVariable final UUID id) {
        return QuoteResponse.from(this.service.findById(new QuoteId(id)));
    }

    /** 入力全体を置換し、ドラフト状態に戻す. */
    @Operation(operationId = "reviseQuote",
            summary = "編集可能なすべての項目を置き換え, 見積を DRAFT に戻す",
            description = """
                    指定した見積の契約条件, SKU 明細, 割引率をリクエスト内容で全置換します.
                    ID と作成日は維持され, 成功すると状態は DRAFT に戻ります.
                    validUntil は更新されません. 入力が不正な場合, 元の内容と状態は変更されません.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修正後の見積を返します"),
            @ApiResponse(responseCode = "400", description = "リクエストまたは業務ルールが不正です",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "見積が存在しません",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public QuoteResponse revise(
            @Parameter(description = "見積の UUID", required = true) @PathVariable final UUID id,
            @RequestBody final QuoteRequest request) {
        return QuoteResponse.from(this.service.revise(new QuoteId(id), request.toCommand()));
    }

    /** 見積もりを保存済み状態にする. */
    @Operation(operationId = "saveQuote", summary = "見積を検証し, 状態を SAVED に変更",
            description = """
                    保存前に見積の業務ルールを再検証し, 状態を SAVED にします.
                    リクエストボディは不要です. 保存済みの見積を再度保存しても SAVED のままです.
                    validUntil は延長されません.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存済みの見積を返します"),
            @ApiResponse(responseCode = "400", description = "リクエストまたは業務ルールが不正です",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "見積が存在しません",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/save")
    public QuoteResponse save(
            @Parameter(description = "見積の UUID", required = true) @PathVariable final UUID id) {
        return QuoteResponse.from(this.service.save(new QuoteId(id)));
    }

    /** 見積もりを削除する. */
    @Operation(operationId = "deleteQuote", summary = "下書きまたは保存済みの見積を削除",
            description = "UUID に一致する見積を削除します. 削除後に取得すると404を返します.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "削除しました. レスポンスボディはありません"),
            @ApiResponse(responseCode = "400", description = "UUID の形式が不正です",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "見積が存在しません",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "見積の UUID", required = true) @PathVariable final UUID id) {
        service.delete(new QuoteId(id));
        return ResponseEntity.noContent().build();
    }
}
