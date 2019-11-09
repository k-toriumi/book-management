package server.component

import io.reactiverse.reactivex.pgclient.PgPool
import io.reactiverse.reactivex.pgclient.Tuple
import server.model.BookInfo
import server.model.Suggest
import javax.inject.Singleton


/**
 * データベースマネージャクラス
 */
@Singleton
class DbManager(var client: PgPool) {

    /**
     * 書籍情報取得処理
     *
     * @param type 検索条件 1:条件なし、2:著者指定
     * @param authorName 著者
     *
     * @return 書籍情報
     */
    fun select(type: Int, authorName: String?): List<BookInfo> {
        return when (type) {
            1 -> client.rxPreparedQuery("SELECT ROW_NUMBER() OVER () AS no, * FROM book_info")
            else -> client.rxPreparedQuery("SELECT ROW_NUMBER() OVER () AS no, * FROM book_info WHERE author_name LIKE $1", Tuple.of("$authorName%"))
        }.map<List<BookInfo>> {
                    val result = mutableListOf<BookInfo>()
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
}