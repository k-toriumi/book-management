package server.model

import java.time.LocalDateTime

/**
 * 著者データクラス
 */
data class Author(
        var id: Int?,
        var name: String,
        var note: String?,
        var update_date: LocalDateTime?
)
