package read

import io.micronaut.function.FunctionBean
import io.micronaut.function.executor.FunctionInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import read.model.Read
import read.service.Service
import java.util.function.Function
import javax.inject.Inject

@FunctionBean("read")
class ReadFunction : FunctionInitializer(), Function<Read, String> {

    @Inject
    lateinit var service: Service

    var logger: Logger = LogManager.getLogger(Read::class.java)

    override fun apply(msg: Read): String {
        logger.info("Read Function Start")
        logger.info("displayPage:" + msg.displayPage)
        logger.info("authorName:" + msg.authorName)
        return service.read(msg.displayPage.toInt(), msg.authorName)
    }
}

/**
 * This main method allows running the function as a CLI application using: echo '{}' | java -jar function.jar
 * where the argument to echo is the JSON to be parsed.
 */
fun main(args: Array<String>) {
    val function = ReadFunction()
    function.run(args, { context -> function.apply(context.get(Read::class.java)) })
}