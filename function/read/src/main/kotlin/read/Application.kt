package read

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("read")
                .mainClass(Application.javaClass)
                .start()
    }
}