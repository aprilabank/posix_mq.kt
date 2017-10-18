posix_mq
========

[![Build Status](https://travis-ci.org/aprilabank/posix_mq.kt.svg?branch=master)](https://travis-ci.org/aprilabank/posix_mq.kt)

This is a relatively high-level FFI-binding library for the POSIX [message queue API][]. It wraps the lower-level API
in a simpler and safer interface.

Check out this project's [sister library][] in Rust.

Usage example:

```kotlin
// Queue creation with system defaults is simple:
val queue = Queue.create("/test-queue")

// Opening a queue requires the user to specify the queue size if it deviates from the system default
val queue = Queue.open("/test-queue", 8192)

// Sending a message:
val message = Message("test-message".toByteArray(), 0)
queue.send(message)

// ... and receiving it!
val result = queue.receive()
```

[message queue API]: https://linux.die.net/man/7/mq_overview
[sister library]: https://github.com/aprilabank/posix_mq.rs
