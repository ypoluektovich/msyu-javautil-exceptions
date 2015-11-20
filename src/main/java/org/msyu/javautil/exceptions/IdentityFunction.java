package org.msyu.javautil.exceptions;

final class IdentityFunction<R, X extends Exception> implements FunctionWithException<R, R, X> {

    static final IdentityFunction<?, ?> INSTANCE = new IdentityFunction<>();

    @Override
    public final R apply(R input) throws X {
        return input;
    }

}
