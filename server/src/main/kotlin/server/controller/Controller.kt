package server.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import server.component.DbManager
import server.model.BookInfo
import server.service.Service
import javax.inject.Inject
import javax.validation.Valid

/**
 * 書籍情報管理コントローラクラス
 */
@Validated
@Controller("/")
class Controller(var dbManager: DbManager) {

    @Inject
    lateinit var service: Service

    /**
     * 書籍情報登録処理
     *
     * @param bookInfo 書籍情報
     *
     * @return HTTPレスポンス OK : 200、ERROR:400
     */
    @Post("/create")
    fun create(@Body @Valid bookInfo: BookInfo): HttpResponse<String> {
        var message = service.create(bookInfo)
        if (message.isNotEmpty()) {
            return HttpResponse.badRequest(message)
        }
        return HttpResponse.ok("{}")
    }
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
     * 書籍情報取得処理
     *
     * @param id 書籍ID
     *
     * @return 書籍情報（JSON）
     */
    @Get("/read/book/{id}")
    fun read(id: Int): String {
        return service.read(id)
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

    /**
     * 書籍情報更新処理
     *
     * @param bookInfo 書籍情報
     *
     * @return HTTPレスポンス OK : 200、ERROR:400
     */
    @Post("/update")
    fun update(@Body @Valid bookInfo: BookInfo): HttpResponse<String> {
        var message = service.update(bookInfo)
        if (message.isNotEmpty()) {
            return HttpResponse.badRequest(message)
        }

        return HttpResponse.ok("{}")
    }

    /**
     * 書籍情報削除処理
     *
     * @param id 書籍ID
     *
     * @return HTTPレスポンス OK : 200、ERROR:400
     */
    @Post("/delete")
    fun delete(id: String, update_date : String): HttpResponse<String> {
        var message = service.delete(id, update_date)
        if (message.isNotEmpty()) {
            return HttpResponse.badRequest(message)
        }
        return HttpResponse.ok("{}")
    }
}