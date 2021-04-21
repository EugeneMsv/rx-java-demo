package com.github.eugenemsv;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.processors.ReplayProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class FlowableTest {

    @Test
    void testEmitAfterSubscribe() throws InterruptedException {
        FlowableProcessor<Object> publishProcessor = ReplayProcessor.create(1);
        Flowable<Object> flowable = Flowable.create(
                emitter -> {
                    String newElement = UUID.randomUUID().toString();
                    System.out.println(Thread.currentThread() +" New element is emitting = " + newElement);
                    emitter.onNext(newElement);
                }
                , BackpressureStrategy.ERROR);

        Flowable<Object> combinedFlowable = flowable.mergeWith(publishProcessor);


        Subscriber subscriber = new DisposableSubscriber() {
            @Override
            public void onNext(Object o) {
                System.out.println(Thread.currentThread() + " got new element = " + o);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println(Thread.currentThread() + " got error " + t);
            }

            @Override
            public void onComplete() {
                System.out.println(Thread.currentThread() + " got completion");
            }
        };
        combinedFlowable.subscribeOn(Schedulers.newThread())
                .subscribe(subscriber);

        publishProcessor.onNext("after 1");
        publishProcessor.onNext("after 2");
        TimeUnit.SECONDS.sleep(10);
        publishProcessor.onNext("after 3");
    }

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
