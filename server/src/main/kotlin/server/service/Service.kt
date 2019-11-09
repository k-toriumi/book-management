package server.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.http.scope.RequestScope
import server.component.DbManager
import server.model.BookInfo

/**
 * 書籍情報管理サービスクラス
 */
@RequestScope
class Service(var dbManager: DbManager) {

    /**
     * 書籍情報取得処理
     *
     * @param authorName 筆者
     *
     * @return 書籍情報
     */
    fun read(authorName: String?): String {

        var bookInfo = when (authorName) {
            null -> dbManager.select(1, null)
            else -> dbManager.select(2, authorName)
        }
        return ObjectMapper().writeValueAsString(bookInfo)
    }

    /**
     * 著者情報取得処理
     *
     * @param authorName 著者
     *
     * @return 著者情報
     */
    fun readAuthor(authorName: String): String {
        var result = dbManager.selectAuthor(authorName, false)
        return ObjectMapper().writeValueAsString(result)
    }
}