package read.service

import com.fasterxml.jackson.databind.ObjectMapper
import read.component.DbManager
import read.model.BookInfo
import javax.inject.Inject


/**
 * 書籍情報管理サービスクラス
 */
class Service {

    @Inject
    lateinit var dbManager: DbManager

    /**
     * 書籍情報取得処理
     *
     * @param displayPage 表示するページ
     * @param authorName 筆者
     *
     * @return 書籍情報
     */
    fun read(displayPage: Int, authorName: String?): String {
        var totalCount = dbManager.selectCount(authorName ?: "")

        var bookInfo = when (authorName) {
            null -> dbManager.select(1, displayPage, null, null)
            else -> dbManager.select(2, displayPage, authorName, null)
        }

        return ObjectMapper().writeValueAsString(
                object {
                    var total_count = totalCount
                    var book_info = if (totalCount > 0) bookInfo else ArrayList<BookInfo>()
                }
        )
    }
}