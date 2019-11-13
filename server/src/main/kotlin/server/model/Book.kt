package server.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 書籍データクラス
 */
data class Book(
        var id: Int?,
        var name: String,
        var page: Int?,
        var publisher: String?,
        var isbn: String?,
        var sale_date: LocalDate?,
        var note: String?,
        var author_id: Int?,
        var update_date: LocalDateTime?
)
