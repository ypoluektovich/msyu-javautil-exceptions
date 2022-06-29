package org.msyu.javautil.exceptions;

import java.util.concurrent.locks.Lock;

public interface ParameterizedAutoCloseable<X extends Exception> extends AutoCloseable {

    @Override
    void close() throws X;

    static <T, X extends Exception> ParameterizedAutoCloseable<X> wrap(
            T object,
            ConsumerWithException<? super T, X> destructor
    ) {
        return () -> destructor.accept(object);
    }

    static <T, C extends Exception, D extends Exception> ParameterizedAutoCloseable<D> wrap(
            T object,
            ConsumerWithException<? super T, C> ctor,
            ConsumerWithException<? super T, D> dtor
    ) throws C {
        ctor.accept(object);
        return () -> dtor.accept(object);
    }

    interface Locks {

        static ParameterizedAutoCloseable<RuntimeException> lock(Lock lock) {
            return wrap(lock, Lock::lock, Lock::unlock);
        }

    }

}
