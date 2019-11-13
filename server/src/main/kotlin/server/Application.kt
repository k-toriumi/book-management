package server

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("server")
                .mainClass(Application.javaClass)
                .start()
    }
}
