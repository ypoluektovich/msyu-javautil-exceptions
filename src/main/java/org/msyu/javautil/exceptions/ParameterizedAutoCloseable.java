package org.msyu.javautil.exceptions;

public interface ParameterizedAutoCloseable<X extends Exception> extends AutoCloseable {

    @Override
    void close() throws X;

    static <T, X extends Exception> ParameterizedAutoCloseable<X> wrap(
            T object,
            ConsumerWithException<? super T, X> destructor
    ) {
        return () -> destructor.accept(object);
    }

}
