package no.aprilabank.posix_mq

import java.io.Closeable

class Queue(
        val name: String,
        val descriptor: mqd_t,
        val maxPending: Int,
        val maxSize: Int
) : Closeable {
    companion object {
        /**
         * Creates a new queue and fails if it already exists.
         *
         * By default the queue will be read/writable by the current user with no
         * access for other users.
         *
         * Linux users can change this setting themselves by modifying the queue
         * file in /dev/mqueue.
         *
         * @param name Name to use for the message queue. Must start with a '/', but not contain more than one '/'.
         * @param maxPending Maximum number of pending items in the queue.
         * @param maxSize    Maximum size of any single message in the queue.
         *
         * @throws PosixMqException This exception is thrown with an error description if the C library call fails.
         */
        fun create(name: String, maxPending: Int, maxSize: Int): Queue {
            TODO()
        }

        /**
         * Opens an existing queue.
         *
         * @throws PosixMqException This exception is thrown with an error description if the C library call fails.
         */
        fun open(name: String): Queue {
            TODO()
        }

        /**
         * Opens an existing queue or creates a new queue with the OS default settings.
         *
         * @param name Name to use for the message queue. Must start with a '/', but not contain more than one '/'.
         *
         * @throws PosixMqException This exception is thrown with an error description if the C library call fails.
         */
        fun openOrCreate(name: String): Queue {
            TODO()
        }
    }

    /**
     * Delete a message queue from the system. This method will make the queue unavailable for other processes after
     * their current queue descriptors have been closed.
     *
     * Calling this method multiple times is an error.
     */
    fun delete() {
        TODO()
    }

    /**
     * Send a message to the message queue.
     * If the queue is full this call will block until a message has been consumed.
     */
    fun send(msg: Message) {
        TODO()
    }

    /**
     * Receive a message from the message queue.
     * If the queue is empty this call will block until a message arrives.
     */
    fun receive(): Message {
        TODO()
    }

    // Close the queue descriptor before the object is garbage collected.
    override fun close() {
        TODO()
    }

}
