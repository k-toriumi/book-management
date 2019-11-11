package server.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

/**
 * 書籍情報データクラス（クライアント・サーバ間I/F）
 */
@Introspected
data class BookInfo(
        /**
         * No.
         */
        var no: Int?,

        /**
         * 書籍ID
         */
        var book_id: Int,

        /**
         * 書籍名
         */
        @field:NotBlank(message = "書籍名を入力してください")
        @field:Size(min = 1, max = 255, message = "書籍名は255文字以内で入力してください")
        var book_name: String,

        /**
         * ページ数
         */
        @field:Size(min = 0, max = 4, message = "ページ数は4桁以内で入力してください")
        @field:Pattern(regexp = "[0-9]*", message = "ページ数は数字で入力してください")
        var page: String,

        /**
         * 出版社
         */
        @field:Size(min = 0, max = 255, message = "出版社は255文字以内で入力してください")
        var publisher: String,

        /**
         * 発売日
         */
        @field:Pattern(regexp = "^(\\d{4}/\\d{2}/\\d{2})?\$", message = "発売日は「YYYY/MM/DD」の形式で入力してください")
        var sale_date: String,

        /**
         * ISBN
         */
        @field:Size(min = 0, max = 13, message = "ISBNは10桁もしくは13桁で入力してください")
        @field:Pattern(regexp = "[0-9]*", message = "ISBNは数字で入力してください")
        var isbn: String,

        /**
         * 備考
         */
        @field:Size(min = 0, max = 1000, message = "備考は1000文字以内で入力してください")
        var book_note: String,

        /**
         * 著者ID
         */
        var author_id: String?,

        /**
         * 著者名
         */
        @field:NotBlank(message = "著者を入力してください")
        @field:Size(min = 1, max = 255, message = "著者は255文字以内で入力してください")
        var author_name: String,

        /**
         * 備考
         */
        @field:Size(min = 0, max = 1000, message = "備考は1000文字以内で入力してください")
        var author_note: String,

        /**
         * 書籍最終更新日時
         */
        var book_update_date: String?,

        /**
         * 著者最終更新日時
         */
        var author_update_date: String?
)