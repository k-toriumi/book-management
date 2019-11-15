package server.filter

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import server.service.LogService

/**
 * リクエストFilterクラス
 */
@Filter("/**")
class RequestFilter(private val logService: LogService) : HttpServerFilter {

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        return logService.requestLogging(request).switchMap { chain.proceed(request) }
                .doOnNext {
                    it.headers.add("X-Trace-Enabled", "true")
                }
    }
}