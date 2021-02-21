package com.github.eugenemsv;

import io.reactivex.rxjava3.core.Observable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleObservableTest {

    @Test
    public void testFromArray() {
        StringBuilder successResult = new StringBuilder();
        Observable.fromArray("Hello", " ", "World", "!")
                .blockingSubscribe(successResult::append);

        assertEquals("Hello World!", successResult.toString());
    }

    @Test
    public void testFromCallable() {
        StringBuilder successResult = new StringBuilder();
        Observable.fromCallable(() -> "Hello World!")
                .blockingSubscribe(successResult::append);

        assertEquals("Hello World!", successResult.toString());
    }

    @Test
    public void testMap() {
        StringBuilder successResult = new StringBuilder();
        Observable.fromArray("Hello", " ", "World", "!")
                .map(String::toUpperCase)
                .blockingSubscribe(successResult::append);

        assertEquals("HELLO WORLD!", successResult.toString());
    }

    @Test
    public void testFilter() {
        StringBuilder successResult = new StringBuilder();
        Observable.fromArray("Hello", " ", "World", "!")
                .filter(str -> !str.isBlank())
                .blockingSubscribe(successResult::append);

        assertEquals("HelloWorld!", successResult.toString());
    }

    @Test
    public void testOnError() {
        StringBuilder successResult = new StringBuilder();
        StringBuilder errorResult = new StringBuilder();
        Observable.fromSupplier(() -> {
            throw new RuntimeException("Expected message");
        }).blockingSubscribe(
                successResult::append,
                exception -> errorResult.append(exception.getMessage()));

        assertEquals("Expected message", errorResult.toString());
        assertEquals(0, successResult.length());
    }

    @Test
    public void testTakeWhile() {
        StringBuilder successResult = new StringBuilder();
        Observable.fromArray("Hello", " ", "World", "!")
                .takeWhile(str -> !str.isBlank())
                .blockingSubscribe(successResult::append);

        assertEquals("Hello", successResult.toString());
    }

}
