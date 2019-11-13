package server.model

/**
 * エラー情報クラス
 */
data class Error(
        var _embedded: Errors
)

data class Errors(
        var errors: Array<Message>
)

data class Message(
        var message: String
)
