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

    private val DEFAULT_DURATION = 1L
    private val DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS

    init {

        var functions = mutableListOf(
            { println("Call 1"); throw TestException("Failed") },
            { println("Call 2"); throw TestException("Failed") },
            { println("Call 3"); throw TestException("Failed") },
            { println("Call 4"); "Success" }

        )
        val io = IO {
            val current = functions[0]
            functions.removeAt(0)
            current()
        }

        "Using a retry strategy with 3 maxRetries on IO that fails three times should succeed" {
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(DEFAULT_DURATION, DEFAULT_TIMEUNIT), 3)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beRight("Success")
        }

        "Using a retry strategy with 2 maxRetries on IO that fails three times should fail" {
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(DEFAULT_DURATION, DEFAULT_TIMEUNIT), 2)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beLeftOfType<TestException>()
        }

        "Using a retry strategy with 1 maxRetries on IO that fails three times should fail" {
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(DEFAULT_DURATION, DEFAULT_TIMEUNIT), 1)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beLeftOfType<TestException>()
        }

        "Using a retry strategy with 3 maxRetries on IO that fails three times with non-retriable error should fail" {
            val nonRetriableError = { _: Throwable -> false}
            val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(DEFAULT_DURATION, DEFAULT_TIMEUNIT), 3, nonRetriableError)
            val rio = io.retry(retryPolicy)
            val result = rio.attempt().unsafeRunSync()
            result should beLeftOfType<TestException>()
        }

    }

    data class TestException(override val message: String) : RuntimeException(message)

}
