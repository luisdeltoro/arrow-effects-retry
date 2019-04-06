package io.luisdeltoro.arrow.effects.retry

import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.handleErrorWith
import java.util.concurrent.TimeUnit

fun <A> IO<A>.retry(retryPolicy: RetryPolicy): IO<A> = retriableIO(this, retryPolicy)

fun <A> retriableIO(io: IO<A>, retryPolicy: RetryPolicy): IO<A> {
    fun next(retryPolicy: RetryPolicy, current: PolicyStep.Continue): PolicyStep {
        return when (current.remaining) {
            1 -> PolicyStep.GiveUp
            else -> when (retryPolicy) {
                is RetryPolicy.ExponentialBackoff -> {
                    val retriesSoFar = retryPolicy.maxRetries - current.remaining
                    val nextDelayDuration = retryPolicy.delay.duration * Math.pow(2.0, retriesSoFar.toDouble()).toLong()
                    val nextDelay = Delay(nextDelayDuration, retryPolicy.delay.timeUnit)
                    PolicyStep.Continue(nextDelay, current.remaining - 1)
                }
                is RetryPolicy.ConstantDelay -> PolicyStep.Continue(retryPolicy.delay, current.remaining - 1)
            }
        }
    }

    fun retry(retryPolicy: RetryPolicy, policyStep: PolicyStep, timer: Timer<ForIO>): IO<A> {
        return when (policyStep) {
            is PolicyStep.GiveUp -> IO.defer { io }
            is PolicyStep.Continue -> IO.defer { io }.handleErrorWith { t ->
                when (retryPolicy.retriableError(t)) {
                    true ->timer.sleep(policyStep.delay.duration, policyStep.delay.timeUnit).fix()
                                          .flatMap { retry(retryPolicy, next(retryPolicy, policyStep), timer) }
                    false -> IO.raiseError(t)
                }
            }
        }
    }

    return when (retryPolicy) {
        is RetryPolicy.ExponentialBackoff -> retry(retryPolicy, PolicyStep.Continue(retryPolicy.delay, retryPolicy.maxRetries), IOTimer)
        is RetryPolicy.ConstantDelay -> retry(retryPolicy, PolicyStep.Continue(retryPolicy.delay, retryPolicy.maxRetries), IOTimer)
    }
}

sealed class PolicyStep {
    object GiveUp: PolicyStep()
    data class Continue(val delay: Delay, val remaining: Int): PolicyStep()
}

sealed class RetryPolicy(open val retriableError: (Throwable) -> Boolean = { _ -> true }) {
    data class ExponentialBackoff(
        val delay: Delay,
        val maxRetries: Int,
        override val retriableError: (Throwable) -> Boolean = { _ -> true }) : RetryPolicy(retriableError)
    data class ConstantDelay(
        val delay: Delay,
        val maxRetries: Int,
        override val retriableError: (Throwable) -> Boolean = { _ -> true }) : RetryPolicy(retriableError)
}

data class Delay(val duration: Long, val timeUnit: TimeUnit)
