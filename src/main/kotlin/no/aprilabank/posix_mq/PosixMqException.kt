package no.aprilabank.posix_mq

import no.aprilabank.posix_mq.PosixMqError.*

/**
 * This enum is used to map error codes from the low-level C calls to usable error descriptions.
 */
enum class PosixMqError {
    // Foreign errors
    PermissionDenied,
    InvalidQueueDescriptor,
    QueueCallInterrupted,
    QueueAlreadyExists,
    QueueNotFound,
    InsufficientMemory,
    InsufficientSpace,

    // These two are (hopefully) unlikely in modern systems
    ProcessFileDescriptorLimitReached,
    SystemFileDescriptorLimitReached,

    // Some of the errors that can happen in the C-library are willfully ignored here. The Rust-version of this library
    // contains more information about the reasoning.
    // If this error is encountered a bug report would be welcome.
    UnknownForeignError
}

fun PosixMqError.description(): String {
    return when (this) {
        PermissionDenied -> "permission to the specified queue was denied"
        InvalidQueueDescriptor -> "the internal queue descriptor was invalid"
        QueueCallInterrupted -> "queue method interrupted by signal"
        QueueAlreadyExists -> "the specified queue already exists"
        QueueNotFound -> "the specified queue could not be found"
        InsufficientMemory -> "insufficient memory to call queue method"
        InsufficientSpace -> "insufficient space to call queue method"
        ProcessFileDescriptorLimitReached -> "maximum number of process file descriptors reached"
        SystemFileDescriptorLimitReached -> "maximum number of system file descriptors reached"
        UnknownForeignError -> "unknown foreign error occured: please report a bug!"
    }
}

/**
 * This function maps C return errors to the various members of the PosixMqError enum.
 *
 * The definitions of these were looked up in the man pages referenced by mq_overview(7)
 * and partially depend on context.
 *
 * Please see the Rust implementation of this library for more information.
 */
fun Int.toPosixMqError(): PosixMqError {
    return when (this) {
        ENOENT -> QueueNotFound
        EINTR  -> QueueCallInterrupted
        EBADF  -> InvalidQueueDescriptor
        ENOMEM -> InsufficientMemory
        EACCES -> PermissionDenied
        EEXIST -> QueueAlreadyExists
        ENFILE -> SystemFileDescriptorLimitReached
        EMFILE -> ProcessFileDescriptorLimitReached
        ENOSPC -> InsufficientSpace
        else -> UnknownForeignError
    }
}

class PosixMqException : Exception {
    var foreignCause: PosixMqError? = null

    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(foreignError: PosixMqError) : super(foreignError.description()) {
        foreignCause = foreignError
    }
}
