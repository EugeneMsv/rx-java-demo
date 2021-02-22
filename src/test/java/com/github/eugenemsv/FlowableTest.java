package com.github.eugenemsv;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class FlowableTest {

    @Test
    void testFromObservable() {
        StringBuilder successResult = new StringBuilder();

        Observable<String> observable = Observable.fromArray("Hello", " ", "World", "!");
        observable.toFlowable(BackpressureStrategy.BUFFER)
                .blockingSubscribe(successResult::append);

        assertEquals("Hello World!", successResult.toString());
    }

    @Test
    public void testBufferBackpressureStrategy() throws InterruptedException {
        List<Integer> testList = IntStream.range(0, 30)
                .boxed()
                .collect(Collectors.toList());

        AtomicLong bufferOverflows = new AtomicLong();
        List<Integer> result = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        Observable.fromIterable(testList)
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer(5, bufferOverflows::incrementAndGet)
                .observeOn(Schedulers.computation())
                .subscribe(element -> {
                            TimeUnit.SECONDS.sleep(1);
                            result.add(element);
                        },
                        throwable::set
                );

        TimeUnit.SECONDS.sleep(10);
        assertNotEquals(30, result.size());
        assertFalse(result.isEmpty());
        assertNotNull(throwable.get());
    }

    @Test
    public void testLatestBackpressureStrategy() throws InterruptedException {
        List<Integer> testList = IntStream.range(0, 30)
                .boxed()
                .collect(Collectors.toList());

        List<Integer> result = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        Observable.fromIterable(testList)
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation())
                .subscribe(element -> {
                            TimeUnit.SECONDS.sleep(1);
                            result.add(element);
                        },
                        throwable::set
                );

        TimeUnit.SECONDS.sleep(10);
        assertNotEquals(30, result.size());
        assertFalse(result.isEmpty());
        assertNull(throwable.get());
    }


    @Test
    public void testMissingBackpressureStrategy() throws InterruptedException {
        List<Integer> testList = IntStream.range(0, 30)
                .boxed()
                .collect(Collectors.toList());

        List<Integer> result = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        Observable.fromIterable(testList)
                //Have to deal with overflow in subscriber
                .toFlowable(BackpressureStrategy.MISSING)
                .observeOn(Schedulers.computation())
                .subscribe(element -> {
                            TimeUnit.SECONDS.sleep(1);
                            result.add(element);
                        },
                        throwable::set
                );

        TimeUnit.SECONDS.sleep(40);
        assertEquals(30, result.size());
        assertFalse(result.isEmpty());
        assertNull(throwable.get());
    }
}
