package server.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.runtime.http.scope.RequestScope
import server.component.DbManager
import server.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 書籍情報管理サービスクラス
 */
@RequestScope
class Service(var dbManager: DbManager) {

    /**
     * 書籍情報登録処理
     *
     * @param bookInfo 書籍情報
     *
     * @return エラーメッセージ
     */
    fun create(bookInfo: BookInfo): String {

        var book = Book(bookInfo.book_id, bookInfo.book_name, toInt(bookInfo.page), bookInfo.publisher, bookInfo.isbn, toLocalDate(bookInfo.sale_date), bookInfo.book_note, toInt(bookInfo.author_id), toLocalDateTime(bookInfo.book_update_date))
        var author = Author(toInt(bookInfo.author_id), bookInfo.author_name, bookInfo.author_note, toLocalDateTime(bookInfo.author_update_date))

        // 重複チェック
        if (!dbManager.duplicateCheck(book.name, author.name)) {
            return getErrorJsonMessage("該当の著者の書籍はすでに登録済です")
        }

        var result = dbManager.insert(book, author)
        if (0 < result) {
            return getErrorJsonMessage("書籍の登録に失敗しました<br>再度やり直してください")
        }
        return ""
    }

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

    /**
     * 書籍情報取得処理
     *
     * @param id 書籍ID
     *
     * @return 書籍情報
     */
    fun read(id: Int): String {
        var bookList = dbManager.select(3, null, null, id)

        if (bookList.isEmpty()) {
            return "{}"
        }
        return ObjectMapper().writeValueAsString(bookList[0])
    }

    /**
     * 著者情報取得処理
     *
     * @param complete '0':部分一致 '1':完全一致
     * @param authorName 著者
     *
     * @return 著者情報
     */
    fun readAuthor(complete: String, authorName: String): String {
        var result = dbManager.selectAuthor(authorName, complete == "1")
        return ObjectMapper().writeValueAsString(result)
    }

    /**
     * 書籍情報更新処理
     *
     * @param bookInfo 書籍情報
     *
     * @return エラーメッセージ
     */
    fun update(bookInfo: BookInfo): String {

        var book = Book(bookInfo.book_id, bookInfo.book_name, toInt(bookInfo.page), bookInfo.publisher, bookInfo.isbn, toLocalDate(bookInfo.sale_date), bookInfo.book_note, toInt(bookInfo.author_id), toLocalDateTime(bookInfo.book_update_date))
        var author = Author(toInt(bookInfo.author_id), bookInfo.author_name, bookInfo.author_note, toLocalDateTime(bookInfo.author_update_date))

        // 変更前の書籍情報を取得する
        var bookList = dbManager.select(3, null, null, book.id)
        if (bookList.isEmpty()) {
            return getErrorJsonMessage("該当の書籍は他のユーザによって削除されました。<br>最新の情報を確認してください")
        }
        val beforeBook = bookList[0]
        if (author.name != beforeBook.author_name) {
            // 重複チェック
            if (!dbManager.duplicateCheck(book.name, author.name)) {
                return getErrorJsonMessage("該当の著者の書籍はすでに登録済です")
            }
        }

        var beforeAuthor = Author(toInt(beforeBook.author_id), beforeBook.author_name, beforeBook.author_note, toLocalDateTime(beforeBook.author_update_date))

        if (!dbManager.update(book, author, beforeAuthor)) {
            return getErrorJsonMessage("該当の書籍は他のユーザによって変更されました。<br>最新の情報を確認してください")
        }
        return ""
    }

    /**
     * 書籍情報削除処理
     *
     * @param id 書籍ID
     * @param update_date 書籍更新日時
     *
     * @return エラーメッセージ
     */
    fun delete(id: String, update_date: String): String {
        var bookList = dbManager.select(3, null, null, toInt(id))
        if (bookList.isEmpty()) {
            return getErrorJsonMessage("該当の書籍は他のユーザによって削除されました。<br>最新の情報を確認してください")
        }
        var bookInfo = bookList[0]
        var book = Book(toInt(id), bookInfo.book_name, toInt(bookInfo.page), bookInfo.publisher, bookInfo.isbn, toLocalDate(bookInfo.sale_date), bookInfo.book_note, toInt(bookInfo.author_id), toLocalDateTime(update_date))
        var author = Author(toInt(bookInfo.author_id), bookInfo.author_name, bookInfo.author_note, toLocalDateTime(bookInfo.author_update_date))

        if (!dbManager.deleteBook(book, author)) {
            return getErrorJsonMessage("該当の書籍は他のユーザによって変更されました。<br>最新の情報を確認してください")
        }
        return ""
    }


    /**
     * String → Int 変換処理
     *
     * @param data 文字列
     *
     * @return 数値
     */
    private fun toInt(data: String?): Int? {
        return if (data == null || data.isEmpty()) {
            null
        } else {
            data.toInt()
        }
    }

    /**
     * String → LocalDate 変換処理
     *
     * @param data 文字列
     *
     * @return 年月日
     */
    private fun toLocalDate(data: String?): LocalDate? {
        return if (data == null || data.isEmpty()) {
            null
        } else {
            LocalDate.parse(data, DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        }
    }

    /**
     * String → LocalDateTime 変換処理
     *
     * @param data 文字列
     *
     * @return 日時（ナノミリ）
     */
    private fun toLocalDateTime(data: String?): LocalDateTime? {
        return if (data == null || data.isEmpty()) {
            null
        } else {
            LocalDateTime.parse(data, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
        }
    }

    /**
     * エラーJSON変換用データ取得処理
     *
     * @param message 文字列
     *
     * @return JSON文字列
     */
    private fun getErrorJsonMessage(message: String): String {
        return ObjectMapper().writeValueAsString(Error(Errors(arrayOf(Message(message)))))
    }
}