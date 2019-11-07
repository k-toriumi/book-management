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
     * @return 書籍情報
     */
    fun read(): String {

        var bookInfo = dbManager.select()
        return ObjectMapper().writeValueAsString(bookInfo)
    }
}