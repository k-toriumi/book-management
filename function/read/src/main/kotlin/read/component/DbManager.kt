package read.component

import io.reactiverse.reactivex.pgclient.PgPool
import io.reactiverse.reactivex.pgclient.Tuple
import read.model.BookInfo
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * データベースマネージャクラス
 */
@Singleton
class DbManager {

    @Inject
    lateinit var client: PgPool

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
}