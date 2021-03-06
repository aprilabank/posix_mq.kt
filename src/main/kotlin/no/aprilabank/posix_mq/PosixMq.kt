package no.aprilabank.posix_mq

import com.sun.jna.LastErrorException
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Structure

/**
 * The mqd_t type is used for queue descriptors.
 */
typealias mqd_t = Int

/**
 * The mode_t type represents file system modes (e.g. permission bits)
 */
typealias mode_t = Int

// Possible error codes. Please see the mapping in 'PosixMqException.kt' for more details on what these mean.
const val SUCCESS = -1
const val ENOENT = 2
const val EINTR  = 4
const val EBADF  = 9
const val ENOMEM = 12
const val EACCES = 13
const val EEXIST = 17
const val ENFILE = 23
const val EMFILE = 24
const val ENOSPC = 28
const val EMSGSIZE = 90

// Possible queue options
const val O_RDONLY = 0
const val O_WRONLY = 1
const val O_RDWR = 2
const val O_CREAT = 64 // 0o0100
const val O_EXCL = 128 // 0o0200

/**
 * This class represents the attributes that can be set on a POSIX message queue. See mq_open(3) for more information.
 *
 * @property mq_flags Flags (ignored for mq_open())
 * @property mq_maxmsg Max. # of messages on queue
 * @property mq_msgsize Max. message size (bytes)
 * @property mq_curmsgs # of messages currently enqueued
 */
class MqAttr(
        @JvmField @Volatile var mq_flags: NativeLong,
        @JvmField @Volatile var mq_maxmsg: NativeLong,
        @JvmField @Volatile var mq_msgsize: NativeLong,
        @JvmField @Volatile var mq_curmsgs: NativeLong
) : Structure() {
    override fun getFieldOrder(): List<String> {
        // This order has to match the C-struct definition.
        return listOf("mq_flags", "mq_maxmsg", "mq_msgsize", "mq_curmsgs")
    }
}

/**
 * This interface defines the low-level JNA bindings to the POSIX message queues.
 *
 * See mq_overview(7) for more information.
 *
 * Note that this is a low-level interface and should usually not be used directly. Look at the `Queue` class instead!
 *
 * @see Queue
 */
interface PosixMq : Library {
    companion object {
        /**
         * This method is used to instantiate the JNA library bindings.
         */
        fun getInstance(): PosixMq {
            return Native.loadLibrary("rt", PosixMq::class.java)
        }
    }

    /**
     * Open an existing message queue in the specified mode.
     * See mq_open(3).
     *
     * @param name  Name of the queue to open
     * @param oflag Flags to open the queue with
     * @return Queue descriptor
     */
    @Throws(LastErrorException::class)
    fun mq_open(name: String, oflag: Int): mqd_t

    /**
     * Open an existing message queue or create a new one with the specified attributes.
     * See mq_open(3).
     *
     * @param name    Name of the queue to open
     * @param oflag   Flags to open the queue with
     * @param mode    Filesystem mode for newly created descriptor
     * @param mq_attr Queue attributes for newly created queue
     */
    @Throws(LastErrorException::class)
    fun mq_open(name: String, oflag: Int, mode: mode_t, mq_attr: MqAttr?): mqd_t

    /**
     * Close a queue descriptor. See mq_close(3).
     *
     * @param mqdes The queue descriptor to close
     * @return error code
     */
    @Throws(LastErrorException::class)
    fun mq_close(mqdes: mqd_t): Int

    /**
     * Unlink (i.e. delete) a message queue. See mq_unlink(3).
     *
     * This will stop new clients from connecting to the message queue, but all existing queue descriptors must be
     * closed before the queue is actually deallocated.
     */
    @Throws(LastErrorException::class)
    fun mq_unlink(name: String): Int

    /**
     * Receive a message from the message queue. See mq_receive(3).
     *
     * @param mqdes    The queue descriptor
     * @param char     The byte array to write message data into
     * @param msg_len  The expected length of the message
     * @param msg_prio The Int to write the message priority into
     * @return Number of bytes in message or error code
     */
    @Throws(LastErrorException::class)
    fun mq_receive(mqdes: mqd_t, char: ByteArray, msg_len: Long, msg_prio: Int): Int


    /**
     * Send a message to the message queue. See mq_send(3).
     *
     * @param mqdes    The queue descriptor
     * @param char     The message to send
     * @param msg_len  Length of the message
     * @param msg_prio Priority of the message
     */
    @Throws(LastErrorException::class)
    fun mq_send(mqdes: mqd_t, char: ByteArray, msg_len: Long, msg_prio: Int): Int
}

/**
 * Calling any library function wrapped inside of this method will cause a PosixMqException to be thrown on library
 * errors.
 *
 * The exception will contain a description of the error that has been mapped from the C-errnos.
 */
fun <R> withMappedException(f: () -> R): R {
    try {
        return f()
    } catch (e: LastErrorException) {
        val mqError = e.errorCode.toPosixMqError()
        throw PosixMqException(mqError)
    }
}
