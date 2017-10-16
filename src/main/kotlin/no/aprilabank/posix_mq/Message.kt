package no.aprilabank.posix_mq

/**
 * High-level representation of a POSIX message queue message.
 *
 * @property data     The content of the message.
 * @property priority The priority of the message.
 */
data class Message(
        val data: ByteArray,
        val priority: Int
)
