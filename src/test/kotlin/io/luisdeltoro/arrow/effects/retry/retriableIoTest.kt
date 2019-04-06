package io.luisdeltoro.arrow.effects.retry

import arrow.effects.IO
import io.kotlintest.IsolationMode
import io.kotlintest.assertions.arrow.either.beLeftOfType
import io.kotlintest.assertions.arrow.either.beRight
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import java.util.concurrent.TimeUnit

class RetriableIoTest : StringSpec() {

    override fun isolationMode() = IsolationMode.InstancePerTest

    init {

        var functions = mutableListOf(
            { throw TestException("Failed") },
            { throw TestException("Failed") },
            { throw TestException("Failed") },
            { "Success" }

        )
        val io = IO {
            val current = functions[0]
            functions.removeAt(0)
            current()
        }

        "Using a retry strategy with 3 maxRetries on IO that fails three times should succeed" {
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(1, TimeUnit.MILLISECONDS), 3)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beRight("Success")
        }

        "Using a retry strategy with 2 maxRetries on IO that fails three times should fail" {
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(1, TimeUnit.MILLISECONDS), 2)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beLeftOfType<TestException>()
        }

        "Using a retry strategy with 1 maxRetries on IO that fails three times should fail" {
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(1, TimeUnit.MILLISECONDS), 1)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beLeftOfType<TestException>()
        }

        "Using a retry strategy with 3 maxRetries on IO that fails three times with non-retriable error should fail" {
            val nonRetriableError = { _: Throwable -> false}
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(1, TimeUnit.MILLISECONDS), 3, nonRetriableError)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beLeftOfType<TestException>()
        }

    }

    data class TestException(override val message: String) : RuntimeException(message)

}