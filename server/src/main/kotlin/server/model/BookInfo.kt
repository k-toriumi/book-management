package server.model

/**
 * 書籍情報データクラス
 */
data class BookInfo(
        var no: Int?,
        var book_id: Int,
        var book_name: String,
        var page: String,
        var publisher: String,
        var sale_date: String,
        var isbn: String,
        var book_note: String,
        var author_id: String?,
        var author_name: String,
        var author_note: String,
        var book_update_date: String?,
        var author_update_date: String?
)