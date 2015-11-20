package org.msyu.javautil.exceptions;

@FunctionalInterface
public interface ConsumerWithException<I, X extends Exception> {

    void accept(I input) throws X;

}
