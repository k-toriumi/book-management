package server.component

import io.reactiverse.reactivex.pgclient.PgPool
import server.model.BookInfo
import javax.inject.Singleton


/**
 * データベースマネージャクラス
 */
@Singleton
class DbManager(var client: PgPool) {

    /**
     * 書籍情報取得処理
     *
     * @return 書籍情報
     */
    fun select(): List<BookInfo> {
        return client.rxPreparedQuery("SELECT * FROM book_info")
                .map<List<BookInfo>> {
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
}