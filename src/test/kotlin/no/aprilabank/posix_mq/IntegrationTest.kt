package no.aprilabank.posix_mq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset
import java.util.UUID

data class Input(
        val name: String,
        val id: String
)

internal class IntegrationTest {
    @Test
    fun shouldCreateAndDeleteQueue() {
        Queue.create(testQueueName()).use { queue ->
            queue.delete()
        }
    }

    @Test
    fun shouldSendAndReceiveSimpleMessage() {
        val name = UUID.randomUUID().toString()

        Queue.create(testQueueName()).use { queue ->
            val message = Message("test message".toByteArray(), 0)
            queue.send(message)

            val result = queue.receive()
            val resultMessage = result.data.toString(Charset.forName("UTF-8"))
            queue.delete()

            assertEquals("test message", resultMessage)
        }
    }

    @Test
    fun shouldSendAndReceiveSerialisedMessage() {
        val json = jacksonObjectMapper()
        val input = Input("test-input", UUID.randomUUID().toString())

        Queue.create(testQueueName()).use { queue ->
            val data = json.writeValueAsBytes(input)
            val message = Message(data, 0)
            queue.send(message)

            val resultMessage = queue.receive()
            queue.delete()

            val output: Input = json.readValue(resultMessage.data)

            assertEquals(input, output)

        }
    }

    @Test
    fun shouldReceiveAfterReopening() {
        val name = testQueueName()

        Queue.create(name).use { queue ->
            val input = Message("test-message".toByteArray(), 0)
            queue.send(input)
        }

        Queue.open(name).use { queue ->
            val output = queue.receive()
            val outMessage = output.data.toString(charset("UTF-8"))
            queue.delete()

            assertEquals("test-message", outMessage)
        }
    }

    private fun testQueueName(): String {
        return "/test-${UUID.randomUUID()}"
    }
}
