package org.msyu.javautil.exceptions;

@FunctionalInterface
public interface FunctionWithException<I, O, X extends Exception> {

    O apply(I input) throws X;

}
