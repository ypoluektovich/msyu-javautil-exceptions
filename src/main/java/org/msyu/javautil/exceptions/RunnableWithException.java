package org.msyu.javautil.exceptions;

@FunctionalInterface
public interface RunnableWithException<X extends Exception> {

    void run() throws X;

}
