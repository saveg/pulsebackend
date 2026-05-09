package com.pulsebackend.utils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.await;

public class Wait {
    private final long timeout;
    private final long pause;

    public Wait(long timeout, long pause) {
        this.timeout = timeout;
        this.pause = pause;
    }

    public Wait() {
        this(10_000, 500);
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Wait interrupted", exception);
        }
    }

    private <T> T waitingForInternal(Supplier<T> supplier) {
        AtomicReference<T> resultRef = new AtomicReference<>();
        await()
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(pause))
                .atMost(Duration.ofMillis(timeout))
                .ignoreExceptions()
                .until(() -> {
                    T result = supplier.get();
                    resultRef.set(result);
                    return result != null;
                });
        return resultRef.get();
    }

    public <A, T> T waitingFor(Function<A, T> function, A arg) {
        return waitingForInternal(() -> function.apply(arg));
    }

    public <A, B, T> T waitingFor(BiFunction<A, B, T> function, A arg1, B arg2) {
        return waitingForInternal(() -> function.apply(arg1, arg2));
    }

    public <T> T waitingFor(Supplier<T> supplier) {
        return waitingForInternal(supplier);
    }
}
