package server.controller

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import server.component.DbManager
import server.service.Service
import javax.inject.Inject

/**
 * 書籍情報管理コントローラクラス
 */
@Controller("/")
class Controller(var dbManager: DbManager) {

    @Inject
    lateinit var service: Service

    /**
     * 書籍情報取得処理
     *
     * @param authorName 筆者
     *
     * @return 書籍情報（JSON）
     */
    @Get("/read{/authorName}")
    fun read(authorName: String?): String {
        return service.read(authorName)
    }

    /**
     * 著者情報取得処理
     *
     * @param authorName 著者
     *
     * @return 著者情報（JSON）
     */
    @Get("/read/author/{authorName}")
    fun readAuthor(authorName: String): String {
        return service.readAuthor(authorName)
    }
}