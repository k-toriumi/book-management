package read.model

import io.micronaut.core.annotation.Introspected

@Introspected
class Read {
	lateinit var displayPage: String
	lateinit var authorName: String
}
