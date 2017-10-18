package no.aprilabank.posix_mq

data class Queue(
        val name: String,
        val descriptor: mqd_t,
        val maxPending: Long,
        val maxSize: Long
) : AutoCloseable {
    companion object {
        // Private static instance of the POSIX MQ library.
        private val POSIX_MQ = PosixMq.getInstance()

        /**
         * Creates a new queue and fails if it already exists.
         *
         * By default the queue will be read/writable by the current user with no
         * access for other users.
         *
         * Linux users can change this setting themselves by modifying the queue
         * file in /dev/mqueue.
         *
         * The queue limits (message count and size) will use the system defaults. This is due to an FFI issue at the
         * moment, they will be configurable in the future.
         *
         * @param name Name to use for the message queue. Must start with a '/', but not contain more than one '/'.
         *
         * @throws PosixMqException This exception is thrown with an error description if the C library call fails.
         */
        fun create(name: String): Queue {
            validateName(name)
            val oflag = (O_RDWR or O_CREAT or O_EXCL)
            val mode = 384 // 0600 (Kotlin does not support octal literals)
            val descriptor = withMappedException { POSIX_MQ.mq_open(name, oflag, mode, null) }

            val attr = withMappedException {
                val attr = MqAttr.empty()
                POSIX_MQ.mq_getattr(descriptor, attr)
                return@withMappedException attr
            }

            return Queue(name, descriptor, attr.mq_maxmsg.toLong(), attr.mq_msgsize.toLong())
        }

        /**
         * Opens an existing queue.
         *
         * @throws PosixMqException This exception is thrown with an error description if the C library call fails.
         */
        fun open(name: String): Queue {
            validateName(name)
            val descriptor = withMappedException { POSIX_MQ.mq_open(name, O_RDWR) }

            val attr = withMappedException {
                val attr = MqAttr.empty()
                POSIX_MQ.mq_getattr(descriptor, attr)
                return@withMappedException attr
            }

            return Queue(name, descriptor, attr.mq_maxmsg.toLong(), attr.mq_msgsize.toLong())
        }

        /**
         * Opens an existing queue or creates a new queue with the OS default settings.
         *
         * @param name Name to use for the message queue. Must start with a '/', but not contain more than one '/'.
         *
         * @throws PosixMqException This exception is thrown with an error description if the C library call fails.
         */
        fun openOrCreate(name: String): Queue {
            validateName(name)
            val oflag = (O_RDWR or O_CREAT)
            val mode = 384 // 0600 (Kotlin does not support octal literals)
            val descriptor = withMappedException { POSIX_MQ.mq_open(name, oflag, mode, null) }
            val attr = withMappedException {
                val attr = MqAttr.empty()
                POSIX_MQ.mq_getattr(descriptor, attr)
                return@withMappedException attr
            }

            return Queue(name, descriptor, attr.mq_maxmsg.toLong(), attr.mq_msgsize.toLong())
        }

        @Throws(PosixMqException::class)
        private fun validateName(name: String) {
            if (!name.startsWith('/')) {
                throw PosixMqException("Queue name must start with '/'")
            }

            if (name.length == 1) {
                throw PosixMqException("Queue name must be a slash followed by one or more characters")
            }

            if (name.length > 255) {
                throw PosixMqException("Queue name must not exceed 255 characters")
            }

            if (name.count({ c -> c.equals('/') }) > 1) {
                throw PosixMqException("Queue name can not contain more than one slash")
            }
        }
    }

    /**
     * Delete a message queue from the system. This method will make the queue unavailable for other processes after
     * their current queue descriptors have been closed.
     *
     * Calling this method multiple times is an error.
     */
    fun delete() {
        withMappedException { POSIX_MQ.mq_unlink(name) }
    }

    /**
     * Send a message to the message queue.
     * If the queue is full this call will block until a message has been consumed.
     */
    fun send(msg: Message) {
        val size = msg.data.size
        if (size > maxSize) {
            throw PosixMqException("Message size ($size) exceeds maximum for queue '$name' ($maxSize)")
        }

        withMappedException {
            POSIX_MQ.mq_send(descriptor, msg.data, size.toLong(), msg.priority)
        }
    }

    /**
     * Receive a message from the message queue.
     * If the queue is empty this call will block until a message arrives.
     */
    fun receive(): Message {
        return withMappedException {
            val bytes = ByteArray(maxSize.toInt())
            val priority = 0
            val size = withMappedException {
                POSIX_MQ.mq_receive(descriptor, bytes, maxSize, priority)
            }

            Message(bytes.sliceArray(IntRange(0, size - 1)), priority)
        }
    }

    // Close the queue descriptor before the object is garbage collected.
    override fun close() {
        withMappedException {
            POSIX_MQ.mq_close(descriptor)
        }
    }
}
