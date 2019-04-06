package io.luisdeltoro.arrow.effects.retry

import arrow.Kind
import arrow.core.Either
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.instances.io.async.async
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

interface Timer<F> {

    fun sleep(duration: Long, timeUnit: TimeUnit): Kind<F, Unit>

}

object IOTimer : Timer<ForIO> {

    override fun sleep(duration: Long, timeUnit: TimeUnit): IO<Unit> = sleep(duration, timeUnit, scheduler)

    fun sleep(duration: Long, timeUnit: TimeUnit, sc: ScheduledExecutorService): IO<Unit> {
        return IO.async().async { callback -> sc.schedule(Tick(callback), duration, timeUnit) }
    }

    val scheduler = Executors.newScheduledThreadPool(2, object : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Thread(r)
            t.name = "io-timer"
            t.isDaemon = true
            return t
        }
    })

    data class Tick(val callback: (Either<Throwable, Unit>) -> Unit) : Runnable {

        override fun run() {
            callback(Either.right(Unit))
        }

    }
}