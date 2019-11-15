package server

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotlintest.*
import io.kotlintest.specs.StringSpec
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import server.model.BookInfo
import java.net.URLEncoder


@MicronautTest
class ControllerTest(ctx: ApplicationContext) : StringSpec() {

    private val embeddedServer = autoClose(
            ctx.getBean(EmbeddedServer::class.java)
    )

    private val client = autoClose(
            embeddedServer.applicationContext.createBean(HttpClient::class.java, embeddedServer.url)
    )

    override fun beforeSpec(spec: Spec): Unit {
//        println("${spec.description().name}の開始")
    }

    override fun beforeTest(testCase: TestCase): Unit {
//        println("${testCase.name}の開始")
        cleanup()
    }

    override fun afterTest(testCase: TestCase, result: TestResult): Unit {
//        println("${testCase.name}の終了")
        cleanup()
    }

    override fun afterSpec(spec: Spec): Unit {
//        println("${spec.description().name}の終了")
    }

    private fun cleanup() {
        // テストデータが登録済の場合は削除する
        var obj = read(1, "テスト著者")
        var count = obj.get("total_count").intValue()
        while (count > 0) {
            count = if (count > 10) 10 else count
            for (j in 0 until count) {
                var id = obj.get("book_info")[j].get("book_id").intValue()
                var bookUpdateDate = obj.get("book_info")[j].get("book_update_date").textValue()
                delete(id, bookUpdateDate)
            }
            obj = read(1, "テスト著者")
            count = obj.get("total_count").intValue()
        }
    }

    private fun create(parameter: BookInfo): HttpResponse<String> {
        return client.toBlocking().exchange(
                HttpRequest.POST("/create", parameter), String::class.java)
    }

    private fun read(page: Int, author: String): JsonNode {
        return ObjectMapper().readTree(client.toBlocking().retrieve("/read/${page}/" + URLEncoder.encode(author, "UTF-8")))
    }

    private fun readBook(parameter: Int): JsonNode {
        return ObjectMapper().readTree(client.toBlocking().retrieve("/read/book/$parameter"))
    }

    private fun readAuthorComplete(parameter: String): JsonNode {
        return ObjectMapper().readTree(client.toBlocking().retrieve("/read/author/1/" + URLEncoder.encode(parameter, "UTF-8")))
    }

    private fun readAuthorLike(parameter: String): JsonNode {
        return ObjectMapper().readTree(client.toBlocking().retrieve("/read/author/0/" + URLEncoder.encode(parameter, "UTF-8")))
    }

    private fun update(parameter: BookInfo): HttpResponse<String> {
        return client.toBlocking().exchange(HttpRequest.POST("/update", parameter), String::class.java)
    }

    private fun delete(id: Int, updateDate: String): HttpResponse<String> {
        return client.toBlocking().exchange(HttpRequest.POST("/delete", object {
            var id = id.toString()
            var update_date = updateDate
        }), String::class.java)
    }

    private fun getBookInfoForCreate(bookName: String, authorName: String): BookInfo {
        return BookInfo(1, 1, bookName, "1234", "テスト出版社", "2019/01/01", "1234567890123",
                "テスト備考＿書籍", null, authorName, "テスト備考＿著者", "", "")
    }

    private fun getBookInfoForUpdate(bookId: Int, authorId: String, bookUpdateDate: String, authorUpdateDate: String): BookInfo {
        return BookInfo(1, bookId, "テスト書籍名＿更新", "4321", "テスト出版社＿更新", "2020/12/31", "3210987654321",
                "テスト備考＿書籍＿更新", authorId, "テスト著者＿更新", "テスト備考＿著者＿更新", bookUpdateDate, authorUpdateDate)
    }

    private fun getServerErrorMsg(exception: HttpClientResponseException): String {
        return ObjectMapper().readTree(exception.response.body().toString()).get("_embedded").get("errors")[0].get("message").textValue()
    }

    init {

        "書籍の登録(/create)のテスト" {
            val response = create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            // ステータスが正常であること
            response.status shouldBe HttpStatus.OK

            var obj = read(1, "テスト著者")
            var bookInfo = obj.get("book_info")[0]

            // 登録した書籍が1件取得できること
            obj.get("total_count").intValue() shouldBe 1
            bookInfo.get("no").intValue() shouldBe 1
            bookInfo.get("book_name").textValue() shouldBe "テスト書籍名"
            bookInfo.get("page").textValue() shouldBe "1234"
            bookInfo.get("publisher").textValue() shouldBe "テスト出版社"
            bookInfo.get("sale_date").textValue() shouldBe "2019/01/01"
            bookInfo.get("isbn").textValue() shouldBe "1234567890123"
            bookInfo.get("book_note").textValue() shouldBe "テスト備考＿書籍"
            bookInfo.get("author_name").textValue() shouldBe "テスト著者"
            bookInfo.get("author_note").textValue() shouldBe "テスト備考＿著者"
        }

        "書籍の取得(/read/{displayPage}{/authorName})のテスト" {

            // 3ページ分登録
            for (i in 1..30) {
                create(getBookInfoForCreate("テスト書籍名$i", "テスト著者$i"))
            }

            // 登録したデータの取得結果を確認
            for (i in 1..3) {
                var obj = read(i, "テスト著者")
                var bookInfoList = obj.get("book_info")

                // 登録した書籍の指定ページの情報が取得できること
                obj.get("total_count").intValue() shouldBe 30
                for (j in 1..10) {
                    bookInfoList[j - 1].get("no").intValue() shouldBe ((i - 1) * 10) + j
                    bookInfoList[j - 1].get("book_name").textValue() shouldBe "テスト書籍名${((i - 1) * 10) + j}"
                    bookInfoList[j - 1].get("page").textValue() shouldBe "1234"
                    bookInfoList[j - 1].get("publisher").textValue() shouldBe "テスト出版社"
                    bookInfoList[j - 1].get("sale_date").textValue() shouldBe "2019/01/01"
                    bookInfoList[j - 1].get("isbn").textValue() shouldBe "1234567890123"
                    bookInfoList[j - 1].get("book_note").textValue() shouldBe "テスト備考＿書籍"
                    bookInfoList[j - 1].get("author_name").textValue() shouldBe "テスト著者${((i - 1) * 10) + j}"
                    bookInfoList[j - 1].get("author_note").textValue() shouldBe "テスト備考＿著者"
                }
            }
        }

        "書籍の取得(/read/book/{id})のテスト" {

            create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            // 登録したデータの書籍IDを取得する
            var bookInfo = read(1, "テスト著者").get("book_info")[0]

            var id = bookInfo.get("book_id").intValue()
            var authorId = bookInfo.get("author_id").textValue()
            var bookUpdateDate = bookInfo.get("book_update_date").textValue()
            var authorUpdateDate = bookInfo.get("author_update_date").textValue()

            // 指定IDの書籍が1件取得できること
            var book = readBook(id)

            book.get("no").intValue() shouldBe 1
            book.get("book_id").intValue() shouldBe id
            book.get("book_name").textValue() shouldBe "テスト書籍名"
            book.get("page").textValue() shouldBe "1234"
            book.get("publisher").textValue() shouldBe "テスト出版社"
            book.get("sale_date").textValue() shouldBe "2019/01/01"
            book.get("isbn").textValue() shouldBe "1234567890123"
            book.get("book_note").textValue() shouldBe "テスト備考＿書籍"
            book.get("author_id").textValue() shouldBe authorId
            book.get("author_name").textValue() shouldBe "テスト著者"
            book.get("author_note").textValue() shouldBe "テスト備考＿著者"
            book.get("book_update_date").textValue() shouldBe bookUpdateDate
            book.get("author_update_date").textValue() shouldBe authorUpdateDate
        }

        "著者の取得(/read/author/{complete}{authorName})のテスト" {

            create(getBookInfoForCreate("テスト書籍名1", "テスト著者1"))
            create(getBookInfoForCreate("テスト書籍名2", "テスト著者2"))

            // 著者名の完全一致検索
            var authorList = readAuthorComplete("テスト著者1")

            // 著者が1件取得できること
            authorList.size() shouldBe 1
            authorList[0].get("title").textValue() shouldBe "テスト著者1"
            authorList[0].get("description").textValue() shouldBe "テスト備考＿著者"

            // 著者名の前方一致検索
            authorList = readAuthorLike("テスト著者")

            // 著者が2件取得できること
            authorList.size() shouldBe 2
            authorList[0].get("title").textValue() shouldBe "テスト著者1"
            authorList[0].get("description").textValue() shouldBe "テスト備考＿著者"
            authorList[1].get("title").textValue() shouldBe "テスト著者2"
            authorList[1].get("description").textValue() shouldBe "テスト備考＿著者"
        }

        "書籍の更新(/update)のテスト" {

            create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            var obj = read(1, "テスト著者")
            var bookInfo = obj.get("book_info")[0]

            var bookId = bookInfo.get("book_id").intValue()
            var authorId = bookInfo.get("author_id").textValue()
            var bookUpdateDate = bookInfo.get("book_update_date").textValue()
            var authorUpdateDate = bookInfo.get("author_update_date").textValue()

            // 書籍の更新
            val response = update(getBookInfoForUpdate(bookId, authorId, bookUpdateDate, authorUpdateDate))

            // ステータスが正常であること
            response.status shouldBe HttpStatus.OK

            // 更新後の書籍を取得
            var book = readBook(bookId)

            // 書籍が更新されていること
            book.get("no").intValue() shouldBe 1
            book.get("book_id").intValue() shouldBe bookId
            book.get("book_name").textValue() shouldBe "テスト書籍名"
            book.get("page").textValue() shouldBe "4321"
            book.get("publisher").textValue() shouldBe "テスト出版社＿更新"
            book.get("sale_date").textValue() shouldBe "2020/12/31"
            book.get("isbn").textValue() shouldBe "3210987654321"
            book.get("book_note").textValue() shouldBe "テスト備考＿書籍＿更新"
            book.get("author_id").textValue() shouldNotBe authorId
            book.get("author_name").textValue() shouldBe "テスト著者＿更新"
            book.get("author_note").textValue() shouldBe "テスト備考＿著者＿更新"
            book.get("book_update_date").textValue() shouldNotBe bookUpdateDate
            book.get("author_update_date").textValue() shouldNotBe authorUpdateDate

            // 著者が削除されていること
            var authorList = readAuthorComplete("テスト著者")
            authorList.size() shouldBe 0
        }

        "書籍の削除(/delete)のテスト" {

            create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            var obj = read(1, "テスト著者")
            var bookInfo = obj.get("book_info")[0]

            var id = bookInfo.get("book_id").intValue()
            var updateDate = bookInfo.get("book_update_date").textValue()

            // 書籍を削除する
            val response = delete(id, updateDate)

            // ステータスが正常であること
            response.status shouldBe HttpStatus.OK

            // 削除されていることを確認
            var book = readBook(id)

            // 書籍が削除されていること
            book.size() shouldBe 0

            var authorList = readAuthorComplete("テスト著者")

            // 著者が削除されていること
            authorList.size() shouldBe 0
        }

        "書籍の登録(/create)の重複チェックのテスト" {

            // 二回同じデータを登録する
            var exception = HttpClientResponseException("", HttpResponse.ok(""))
            try {
                create(getBookInfoForCreate("テスト書籍名", "テスト著者"))
                create(getBookInfoForCreate("テスト書籍名", "テスト著者"))
            } catch (e: HttpClientResponseException) {
                exception = e
            }

            // エラーがサーバから返却されること
            exception.status shouldBe HttpStatus.BAD_REQUEST
            getServerErrorMsg(exception) shouldBe "該当の著者の書籍はすでに登録済です"
        }

        "書籍の更新(/update)の排他エラーのテスト" {

            create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            var bookInfo = read(1, "テスト著者").get("book_info")[0]

            var bookId = bookInfo.get("book_id").intValue()
            var authorId = bookInfo.get("author_id").textValue()
            var bookUpdateDate = bookInfo.get("book_update_date").textValue()
            var authorUpdateDate = bookInfo.get("author_update_date").textValue()

            // 更新日時を最新化せずに同じデータを更新する
            var exception = HttpClientResponseException("", HttpResponse.ok(""))
            try {
                update(getBookInfoForUpdate(bookId, authorId, bookUpdateDate, authorUpdateDate))
                update(getBookInfoForUpdate(bookId, authorId, bookUpdateDate, authorUpdateDate))
            } catch (e: HttpClientResponseException) {
                exception = e
            }

            // エラーがサーバから返却されること
            exception.status shouldBe HttpStatus.BAD_REQUEST
            getServerErrorMsg(exception) shouldBe "該当の書籍は他のユーザによって変更されました。<br>最新の情報を確認してください"
        }

        "書籍の更新(/update)の排他エラーのテスト２" {

            create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            var bookInfo = read(1, "テスト著者").get("book_info")[0]

            var bookId = bookInfo.get("book_id").intValue()
            var authorId = bookInfo.get("author_id").textValue()
            var bookUpdateDate = bookInfo.get("book_update_date").textValue()
            var authorUpdateDate = bookInfo.get("author_update_date").textValue()

            // 削除したデータを更新する
            var exception = HttpClientResponseException("", HttpResponse.ok(""))
            try {
                delete(bookId, bookUpdateDate)
                update(getBookInfoForUpdate(bookId, authorId, bookUpdateDate, authorUpdateDate))
            } catch (e: HttpClientResponseException) {
                exception = e
            }

            // エラーがサーバから返却されること
            exception.status shouldBe HttpStatus.BAD_REQUEST
            getServerErrorMsg(exception) shouldBe "該当の書籍は他のユーザによって削除されました。<br>最新の情報を確認してください"
        }

        "書籍の更新(/update)の重複エラーのテスト" {

            // 著者の異なる同名の書籍を登録する
            create(getBookInfoForCreate("テスト書籍名", "テスト著者A"))
            create(getBookInfoForCreate("テスト書籍名", "テスト著者B"))

            var bookInfo = read(1, "テスト著者B").get("book_info")[0]

            var bookId = bookInfo.get("book_id").intValue()
            var authorId = bookInfo.get("author_id").textValue()
            var bookUpdateDate = bookInfo.get("book_update_date").textValue()
            var authorUpdateDate = bookInfo.get("author_update_date").textValue()

            // 書籍名、著者が一致するように更新する
            var bookInfoForUpdate = BookInfo(1, bookId, "テスト書籍名", "4321", "テスト出版社＿更新", "2020/12/31", "3210987654321",
                    "テスト備考＿書籍＿更新", authorId, "テスト著者A", "テスト備考＿著者＿更新", bookUpdateDate, authorUpdateDate)

            var exception = HttpClientResponseException("", HttpResponse.ok(""))
            try {
                update(bookInfoForUpdate)
            } catch (e: HttpClientResponseException) {
                exception = e
            }

            // エラーがサーバから返却されること
            exception.status shouldBe HttpStatus.BAD_REQUEST
            getServerErrorMsg(exception) shouldBe "該当の著者の書籍はすでに登録済です"
        }

        "書籍の削除(/delete)の排他エラーのテスト" {

            create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            var bookInfo = read(1, "テスト著者").get("book_info")[0]

            var bookId = bookInfo.get("book_id").intValue()
            var authorId = bookInfo.get("author_id").textValue()
            var bookUpdateDate = bookInfo.get("book_update_date").textValue()
            var authorUpdateDate = bookInfo.get("author_update_date").textValue()

            // 更新したデータを削除する
            var exception = HttpClientResponseException("", HttpResponse.ok(""))
            try {
                update(getBookInfoForUpdate(bookId, authorId, bookUpdateDate, authorUpdateDate))
                delete(bookId, bookUpdateDate)
            } catch (e: HttpClientResponseException) {
                exception = e
            }

            // エラーがサーバから返却されること
            exception.status shouldBe HttpStatus.BAD_REQUEST
            getServerErrorMsg(exception) shouldBe "該当の書籍は他のユーザによって変更されました。<br>最新の情報を確認してください"
        }

        "書籍の削除(/delete)の排他エラーのテスト２" {

            create(getBookInfoForCreate("テスト書籍名", "テスト著者"))

            var bookInfo = read(1, "テスト著者").get("book_info")[0]

            var bookId = bookInfo.get("book_id").intValue()
            var bookUpdateDate = bookInfo.get("book_update_date").textValue()

            // 削除したデータを削除する
            var exception = HttpClientResponseException("", HttpResponse.ok(""))
            try {
                delete(bookId, bookUpdateDate)
                delete(bookId, bookUpdateDate)
            } catch (e: HttpClientResponseException) {
                exception = e
            }

            // エラーがサーバから返却されること
            exception.status shouldBe HttpStatus.BAD_REQUEST
            getServerErrorMsg(exception) shouldBe "該当の書籍は他のユーザによって削除されました。<br>最新の情報を確認してください"
        }
    }
}
