package org.msyu.javautil.exceptions;

@FunctionalInterface
public interface FunctionWithException<I, O, X extends Exception> {

    O apply(I input) throws X;

    @SuppressWarnings("unchecked")
    static <R, X extends Exception> FunctionWithException<R, R, X> identity() {
        return (FunctionWithException<R, R, X>) IdentityFunction.INSTANCE;
    }

}
