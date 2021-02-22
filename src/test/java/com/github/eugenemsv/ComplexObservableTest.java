package com.github.eugenemsv;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class ComplexObservableTest {

    @Test
    public void testConnect() throws InterruptedException {
        String[] result = {""};
        ConnectableObservable<Long> connectable
                = Observable.interval(200, TimeUnit.MILLISECONDS).publish();
        connectable.subscribe(i -> result[0] += i);
        assertNotEquals("01", result[0]);

        connectable.connect();
        TimeUnit.MILLISECONDS.sleep(1100);

        assertEquals("01234", result[0]);
    }

    @Test
    public void testPublishSubject() {
        StringBuilder firstSubscriberResult = new StringBuilder();
        StringBuilder secondSubscriberResult = new StringBuilder();

        PublishSubject<Object> publishSubject = PublishSubject.create();

        publishSubject.subscribe(firstSubscriberResult::append);
        publishSubject.onNext(1);
        publishSubject.onNext(2);
        publishSubject.subscribe(secondSubscriberResult::append);
        publishSubject.onNext(3);

        assertEquals("123", firstSubscriberResult.toString());
        assertEquals("3", secondSubscriberResult.toString());
    }


    @Test
    public void testZipWith() {
        StringBuilder successResult = new StringBuilder();
        Observable.fromArray("Hello", " ", "World", "!")
                .zipWith(Arrays.asList(1, 2, 3), (observedEl, iterableEl) -> observedEl + iterableEl)
                .blockingSubscribe(successResult::append);

        assertEquals("Hello1 2World3", successResult.toString());
    }


    @Test
    public void testUsingResource() {
        StringBuilder successResult = new StringBuilder();
        Observable<Character> values = Observable.using(
                () -> "Hello World!",
                resource -> Observable.create(o -> {
                    for (Character c : resource.toCharArray()) {
                        System.out.println("Resource still alive, and emits: " + c);
                        o.onNext(c);
                    }
                    o.onComplete();
                }),
                resource -> System.out.println("Resource closed: " + resource)
        );
        values.blockingSubscribe(successResult::append);

        assertEquals("Hello World!", successResult.toString());
    }


    @Test
    void testObtainObservedValuesInNewThread() throws InterruptedException {
        StringBuilder successResult = new StringBuilder();
        AtomicLong threadId = new AtomicLong();
        Observable.fromArray("Hello", " ", "World", "!")
                .observeOn(Schedulers.newThread())
                .subscribe(element ->
                        {
                            successResult.append(element);
                            threadId.set(Thread.currentThread().getId());
                        }
                );

        TimeUnit.SECONDS.sleep(5);
        assertEquals("Hello World!", successResult.toString());
        assertNotEquals(Thread.currentThread().getId(), threadId.get());
    }


    @Test
    public void testSchedulerFromExecutorWorkInParallel() throws InterruptedException {
        StringBuilder successResult = new StringBuilder();
        Set<Long> threadIds = new CopyOnWriteArraySet<>();

        ExecutorService executorThreadPool = Executors.newFixedThreadPool(4);
        Observable.fromArray("Hello", " ", "World", "!")
                //Because we want to modify each element concurrently we are using flatMap
                .flatMap(val -> Observable.just(val)
                        .subscribeOn(Schedulers.from(executorThreadPool))
                        .map(element -> {
                            System.out.println(Thread.currentThread().getName() + " got element = " + element);
                            TimeUnit.SECONDS.sleep(2);
                            threadIds.add(Thread.currentThread().getId());
                            return element.toUpperCase();
                        }))
                .subscribe(successResult::append);

        TimeUnit.SECONDS.sleep(5);
        String successResultString = successResult.toString();
        assertTrue(successResultString.contains("!"));
        assertTrue(successResultString.contains("HELLO"));
        assertTrue(successResultString.contains("WORLD"));
        assertTrue(successResultString.contains(" "));
        assertTrue(threadIds.size() > 1);
        assertFalse(threadIds.contains(Thread.currentThread().getId()));
    }


    @Test
    public void testSchedulerDelay() throws InterruptedException {
        StringBuilder successResult = new StringBuilder();
        AtomicLong threadId = new AtomicLong();

        Observable.fromArray("Hello", " ", "World", "!")
                .delay(1, TimeUnit.SECONDS, Schedulers.newThread())
                .subscribe(element -> {
                    successResult.append(element);
                    threadId.set(Thread.currentThread().getId());
                });

        TimeUnit.SECONDS.sleep(5);
        assertEquals("Hello World!", successResult.toString());
        assertNotEquals(Thread.currentThread().getId(), threadId.get());
    }

}
