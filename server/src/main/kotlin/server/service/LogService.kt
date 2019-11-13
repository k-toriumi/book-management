package server.service

import io.micronaut.http.HttpRequest
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Singleton

/**
 * ログサービスクラス
 */
@Singleton
class LogService {

    private val LOG = LoggerFactory.getLogger(LogService::class.java)

    internal fun requestLogging(request: HttpRequest<*>): Flowable<Boolean> {
        return Flowable.fromCallable {

            if (LOG.isDebugEnabled) {
                request.body.map {
                    LOG.debug("REQUEST URI: " + request.uri + ", PARAM: " + it)
                }
            }
            true
        }.subscribeOn(Schedulers.io())
    }
}
