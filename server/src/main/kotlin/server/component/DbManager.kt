package server.component

import io.reactiverse.reactivex.pgclient.PgPool
import io.reactiverse.reactivex.pgclient.PgTransaction
import io.reactiverse.reactivex.pgclient.Tuple
import server.model.Author
import server.model.Book
import server.model.BookInfo
import server.model.Suggest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Singleton


/**
 * データベースマネージャクラス
 */
@Singleton
class DbManager(var client: PgPool) {

    /**
     * 書籍情報取得処理
     *
     * @param type 検索条件 1:ページ指定、2:ページ・著者指定、3:書籍ID指定
     * @param page 表示ページ
     * @param authorName 著者
     * @param id 書籍ID
     *
     * @return 書籍情報
     */
    fun select(type: Int, page: Int?, authorName: String?, id: Int?): List<BookInfo> {
        return when (type) {
            1 -> client.rxPreparedQuery("SELECT ROW_NUMBER() OVER () AS no, * FROM (SELECT * FROM book_info ORDER BY book_id) AS a OFFSET (($1 - 1) * 10) LIMIT 10", Tuple.of(page))
            2 -> client.rxPreparedQuery("SELECT ROW_NUMBER() OVER () AS no, * FROM (SELECT * FROM book_info WHERE author_name LIKE $1 ORDER BY book_id) AS a OFFSET (($2 - 1) * 10) LIMIT 10", Tuple.of("$authorName%", page))
            else -> client.rxPreparedQuery("SELECT ROW_NUMBER() OVER () AS no, * FROM book_info WHERE book_id = $1", Tuple.of(id))
        }.map<List<BookInfo>> {
            val result = ArrayList<BookInfo>()
            val ite = it.iterator()
            while (ite.hasNext()) {
                val row = ite.next()
                var bookInfo = BookInfo(
                        row.getInteger("no"),
                        row.getInteger("book_id"),
                        row.getString("book_name"),
                        row.getString("page"),
                        row.getString("publisher"),
                        row.getString("sale_date"),
                        row.getString("isbn"),
                        row.getString("book_note"),
                        row.getString("author_id"),
                        row.getString("author_name"),
                        row.getString("author_note"),
                        row.getString("book_update_date"),
                        row.getString("author_update_date")
                )
                result.add(bookInfo)
            }
            return@map result
        }.blockingGet()
    }

    /**
     * 書籍件数取得処理
     *
     * @param authorName 著者（未指定の場合は書籍の全件数を返却）
     *
     * @return 書籍件数
     */
    fun selectCount(authorName: String): Int {
        return when (authorName) {
            "" -> client.rxPreparedQuery("SELECT COUNT(*) AS total_count FROM book_info")
            else -> client.rxPreparedQuery("SELECT COUNT(*) AS total_count FROM book_info WHERE author_name LIKE $1", Tuple.of("$authorName%"))
        }.map<Int> {
            var result = 0
            val ite = it.iterator()
            while (ite.hasNext()) {
                val row = ite.next()
                result = row.getInteger("total_count")
            }
            return@map result
        }.blockingGet()
    }

    /**
     * 重複チェック処理
     * （同じ書籍がすでに登録されているかチェックを行う）
     *
     * @param bookName 書籍名
     * @param authorName 著者
     *
     * @return チェック結果 true:OK false:NG
     */
    fun duplicateCheck(bookName: String, authorName: String): Boolean {
        var count = client.rxPreparedQuery("SELECT COUNT(*) AS count FROM book_info WHERE book_name = $1 AND author_name = $2", Tuple.of(bookName, authorName))
                .map<Int> {
                    var result = 0
                    val ite = it.iterator()
                    while (ite.hasNext()) {
                        val row = ite.next()
                        result = row.getInteger("count")
                    }
                    return@map result
                }.blockingGet()
        if (count > 0) {
            return false
        }
        return true
    }

    /**
     * 著者取得処理（サジェスト用）
     *
     * @param authorName 著者
     * @param equal true:完全一致、false:部分一致
     *
     * @return 著者情報
     */
    fun selectAuthor(authorName: String, equal: Boolean): List<Suggest> {

        return if (equal) {
            client.rxPreparedQuery("SELECT * FROM author WHERE name = $1", Tuple.of(authorName))
        } else {
            client.rxPreparedQuery("SELECT * FROM author WHERE name LIKE $1", Tuple.of("$authorName%"))
        }.map<List<Suggest>> {
            val result = ArrayList<Suggest>()
            val ite = it.iterator()
            while (ite.hasNext()) {
                val row = ite.next()
                var suggest = Suggest(row.getString("name"), row.getString("note"))
                result.add(suggest)
            }
            return@map result
        }.blockingGet()
    }

    /**
     * 登録処理
     *
     * @param book 書籍情報
     * @param author 著者情報
     *
     * @return 処理結果 0:正常、1:排他エラー
     */
    fun insert(book: Book, author: Author): Int {

        var result = 0

        var conn = client.rxGetConnection().blockingGet()
        var tran = conn.begin()

        runCatching {

            // 対象の著者は登録済みか確認する
            var searchResult = selectAuthor(tran, author.name)

            if (searchResult.isEmpty()) {
                // 対象の著者が未登録の場合
                book.author_id = insertAuthor(tran, author)
            } else {
                // 対象の著者が登録済の場合
                author.id = searchResult[0].id
                author.update_date = searchResult[0].update_date
                updateAuthor(tran, author)
                book.author_id = searchResult[0].id
            }
            // 書籍の登録
            insertBook(tran, book)

        }.onSuccess {
            tran.commit()
        }.onFailure {
            tran.rollback()
            result = 1
        }
        conn.close()
        return result
    }


    /**
     * 更新処理
     *
     * @param book 書籍情報
     * @param author 著者情報
     * @param beforeAuthorId 更新前の著者情報
     *
     * @return 処理結果 true:正常、false:排他エラー
     */
    fun update(book: Book, author: Author, beforeAuthor: Author): Boolean {

        var result = true

        var conn = client.rxGetConnection().blockingGet()
        var tran = conn.begin()

        runCatching {

            // 対象の著者は登録済みか確認する
            var searchResult = selectAuthor(tran, author.name)

            // 著者の更新
            if (searchResult.isEmpty()) {
                // 著者を未登録の著者に変更する場合
                book.author_id = insertAuthor(tran, author)
            } else {
                // 更新もしくは登録済の別の著者に変更する場合
                author.id = searchResult[0].id
                updateAuthor(tran, author)
                book.author_id = searchResult[0].id
            }

            // 書籍の更新
            updateBook(tran, book)

            // 更新前の著者が不要になった場合は削除
            deleteAuthor(tran, beforeAuthor)

        }.onSuccess {
            tran.rxCommit().blockingGet()
        }.onFailure {
            tran.rxRollback().blockingGet()
            result = false
        }
        conn.close()
        return result
    }

    /**
     * 削除処理
     *
     * @param book 書籍情報
     * @param author 著者情報
     *
     * @return 処理結果 0:正常、1:排他エラー
     */
    fun deleteBook(book: Book, author: Author): Boolean {

        var result = true

        var conn = client.rxGetConnection().blockingGet()
        var tran = conn.begin()

        runCatching {
            if (selectBookForUpdate(tran, book)) {
                tran.rxPreparedQuery("DELETE FROM book WHERE ID = $1", Tuple.of(book.id))
                        .map<Int> {
                            return@map it.rowCount()
                        }.blockingGet()

                // 著者が書籍に紐づかなくなった場合は削除する
                deleteAuthor(tran, author)
            } else {
                throw Exception("exclusive error")
            }
        }.onSuccess {
            tran.commit()
        }.onFailure {
            tran.rollback()
            result = false
        }
        conn.close()
        return result
    }

    /**
     * 書籍排他制御処理
     *
     * @param tran トランザクション
     * @param book 対象データ
     *
     * @return 排他制御結果 True : ロック取得、False : ロック未取得
     */
    private fun selectBookForUpdate(tran: PgTransaction, book: Book): Boolean {
        return tran.rxPreparedQuery("SELECT * FROM book WHERE id = $1 FOR UPDATE NOWAIT", Tuple.of(book.id))
                .map<Boolean> {
                    var updateDate: LocalDateTime? = null
                    val ite = it.iterator()
                    while (ite.hasNext()) {
                        val row = ite.next()
                        updateDate = row.getLocalDateTime("update_date")
                    }
                    // 更新日時を比較
                    if (updateDate != null && updateDate.truncatedTo(ChronoUnit.MILLIS).isEqual(
                                    book.update_date!!.truncatedTo(ChronoUnit.MILLIS))) {
                        return@map true
                    }
                    return@map false
                }.blockingGet()
    }

    /**
     * 著者排他制御処理
     *
     * @param tran トランザクション
     * @param author 対象データ
     *
     * @return 排他制御結果 True : ロック取得、False : ロック未取得
     */
    private fun selectAuthorForUpdate(tran: PgTransaction, author: Author): Boolean {
        return tran.rxPreparedQuery("SELECT * FROM author WHERE id = $1 FOR UPDATE NOWAIT", Tuple.of(author.id))
                .map<Boolean> {
                    if (it.size() == 1) {
                        return@map true
                    }
                    return@map false
                }.blockingGet()
    }

    /**
     * 書籍登録処理
     *
     * @param tran トランザクション
     * @param book 登録データ
     *
     * @return 登録により採番された書籍ID
     */
    private fun insertBook(tran: PgTransaction, book: Book): Int {
        return tran.rxPreparedQuery(
                "INSERT INTO book (id, name, page, publisher, sale_date, isbn, note, author_id, create_date, update_date) VALUES (nextVal('book_id_seq'), $1, $2, $3, $4, $5, $6, $7, now(), now()) RETURNING id",
                Tuple.newInstance(io.reactiverse.pgclient.Tuple.of(book.name, book.page, book.publisher, book.sale_date, book.isbn, book.note, book.author_id)))
                .map<Int> {
                    var id = 0
                    val ite = it.iterator()
                    while (ite.hasNext()) {
                        val row = ite.next()
                        id = row.getInteger("id")
                    }
                    return@map id
                }.blockingGet()
    }

    /**
     * 著者登録処理
     *
     * @param tran トランザクション
     * @param author 登録データ
     *
     * @return 登録により採番された著者ID
     */
    private fun insertAuthor(tran: PgTransaction, author: Author): Int {
        return tran.rxPreparedQuery(
                "INSERT INTO author (id, name, note, create_date, update_date) VALUES (nextVal('author_id_seq'), $1, $2, now(), now()) RETURNING id", Tuple.of(author.name, author.note))
                .map<Int> {
                    var id = 0
                    val ite = it.iterator()
                    while (ite.hasNext()) {
                        val row = ite.next()
                        id = row.getInteger("id")
                    }
                    return@map id
                }.blockingGet()
    }

    /**
     * 書籍更新処理
     *
     * @param tran トランザクション
     * @param book 更新データ
     *
     * @throws Exception 排他エラー
     */
    private fun updateBook(tran: PgTransaction, book: Book) {

        if (selectBookForUpdate(tran, book)) {
            tran.rxPreparedQuery(
                    "UPDATE book SET page=$2, publisher=$3, sale_date=$4, isbn=$5, note=$6, author_id=$7, update_date=now() WHERE id = $1",
                    Tuple.newInstance(io.reactiverse.pgclient.Tuple.of(book.id, book.page, book.publisher, book.sale_date, book.isbn, book.note, book.author_id))
            ).map<Int> {
                return@map it.rowCount()
            }.blockingGet()
        } else {
            throw Exception("exclusive error")
        }
    }


    /**
     * 著者更新処理
     * @param tran トランザクション
     * @param author 更新データ
     *
     * @return 更新件数
     */
    private fun updateAuthor(tran: PgTransaction, author: Author) {

        if (selectAuthorForUpdate(tran, author)) {
            tran.rxPreparedQuery(
                    "UPDATE author SET name=$2, note=$3, update_date=now() WHERE id = $1", Tuple.of(author.id, author.name, author.note))
                    .map<Int> {
                        return@map it.rowCount()
                    }.blockingGet()
        }
    }

    /**
     * 著者削除処理
     *
     * @param tran トランザクション
     * @param author 削除情報
     *
     * @return 削除数
     */
    private fun deleteAuthor(tran: PgTransaction, author: Author) {
        var authorBookCount = selectAuthorBookCount(tran, author.id!!)
        if (authorBookCount == 0) {
            // 著者の書籍がない場合は削除する

            if (selectAuthorForUpdate(tran, author)) {
                tran.rxPreparedQuery("DELETE FROM author WHERE id = $1", Tuple.of(author.id))
                        .map<Int> {
                            return@map it.rowCount()
                        }.blockingGet()
            }
        }
    }

    /**
     * 著者取得処理
     *
     * @param tran トランザクション
     * @param authorName 著者
     *
     * @return 著者情報
     */
    private fun selectAuthor(tran: PgTransaction, name: String): List<Author> {
        return tran.rxPreparedQuery("SELECT * FROM author WHERE name = $1", Tuple.of(name))
                .map<List<Author>> {
                    val result = ArrayList<Author>()
                    val ite = it.iterator()
                    while (ite.hasNext()) {
                        val row = ite.next()
                        var author = Author(row.getInteger("id"), row.getString("name"), row.getString("note"), row.getLocalDateTime("update_date"))
                        result.add(author)
                    }
                    return@map result
                }.blockingGet()

    }

    /**
     * 著者書籍件数取得処理
     *
     * @param tran トランザクション
     * @param id 著者
     *
     * @return 書籍件数
     */
    private fun selectAuthorBookCount(tran: PgTransaction, id: Int): Int {
        return tran.rxPreparedQuery("SELECT COUNT(*) AS count FROM book WHERE author_id = $1", Tuple.of(id))
                .map<Int> {
                    var result = 0
                    val ite = it.iterator()
                    while (ite.hasNext()) {
                        val row = ite.next()
                        result = row.getInteger("count")
                    }
                    return@map result
                }.blockingGet()
    }
}
