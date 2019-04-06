# arrow-effects-retry

A library for retrying effects that can fail.

Designed to work with [arrow IO](https://arrow-kt.io/docs/effects/io/) 

Inspired by the [cats-effect](https://github.com/typelevel/cats-effect) 
and [cats-retry](https://github.com/cb372/cats-retry)

Getting started
---------------

It's a simple as importing the extension function "retry" for arrow IO:

```kotlin
import io.luisdeltoro.arrow.effects.retry.*

val io = IO { "this io could potentially fail" }
val retryPolicy = RetryPolicy.ExponentialBackoff(Delay(300, TimeUnit.MILLISECONDS), 3)
val rio = io.retry(retryPolicy)
```

Currently only two retry policies are available:
* ConstantDelay: Retries using a constant delay for maximum amount of attempts.
* ExponentialBackoff: Retries using a variable delay for maximum amount of attempts.

To retry forever a negative number of maximum attempts can be used.