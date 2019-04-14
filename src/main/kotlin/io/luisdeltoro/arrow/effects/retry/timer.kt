package io.luisdeltoro.arrow.effects.retry

import arrow.Kind
import arrow.core.Either
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.instances.io.async.async
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

interface Timer<F> {

    fun sleep(duration: Long, timeUnit: TimeUnit): Kind<F, Unit>

}

object IOTimer : Timer<ForIO> {

    override fun sleep(duration: Long, timeUnit: TimeUnit): IO<Unit> {
        return IO.async().async { callback -> callAfterDelay(timeUnit.toMillis(duration), callback) }
    }

    fun callAfterDelay(delayInMillis: Long, cb: ((Either<Throwable, Unit>) -> Unit)) {
        GlobalScope.launch {
            delay(delayInMillis)
            cb(Either.right(Unit))
        }
    }
}