package org.msyu.javautil.exceptions;

import java.util.Objects;

public final class CloseableChain<I, C extends Exception> {

    public static <C extends Exception> CloseableChain<Void, C> newCloseableChain() {
        return new CloseableChain<>(null, null, null);
    }

    private final I output;

    private final ConsumerWithException<? super I, ? extends C> destructor;

    private final CloseableChain<?, ? extends C> prev;

    private CloseableChain(
            I output,
            ConsumerWithException<? super I, ? extends C> destructor,
            CloseableChain<?, ? extends C> prev
    ) {
        this.output = output;
        this.destructor = destructor;
        this.prev = prev;
    }

    public final <O, X extends Exception>
    CloseableChain<O, C> chain(
            FunctionWithException<? super I, O, X> constructor,
            ConsumerWithException<? super O, ? extends C> destructor
    ) throws X {
        O newOutput;
        try {
            newOutput = constructor.apply(output);
        } catch (Throwable x) {
            close(this, x);
            throw x;
        }
        return new CloseableChain<>(newOutput, destructor, this);
    }

    public final <X extends Exception>
    CloseableChain<I, C> chainEffects(
            ConsumerWithException<? super I, X> constructor,
            ConsumerWithException<? super I, ? extends C> destructor
    ) throws X {
        try {
            constructor.accept(output);
        } catch (Throwable x) {
            close(this, x);
            throw x;
        }
        return new CloseableChain<>(output, destructor, this);
    }

    public final I getOutput() {
        return output;
    }

    private void closeThis() throws C {
        destructor.accept(output);
    }

    /**
     * @throws NullPointerException if {@code suppressor == null}.
     */
    public static void close(CloseableChain<?, ?> chain, Throwable suppressor) {
        Objects.requireNonNull(suppressor, "suppressor is null");
        while (chain != null) {
            if (chain.output != null && chain.destructor != null) {
                try {
                    chain.closeThis();
                } catch (Throwable t) {
                    suppressor.addSuppressed(t);
                }
            }
            chain = chain.prev;
        }
    }

    public static <C extends Exception> void close(CloseableChain<?, C> chain) throws C {
        Throwable throwable = null;
        CloseableChain<?, ? extends C> link = chain;
        while (link != null) {
            if (link.output != null && link.destructor != null) {
                try {
                    link.closeThis();
                } catch (Throwable t) {
                    if (throwable == null) {
                        throwable = t;
                    } else {
                        throwable.addSuppressed(t);
                    }
                }
            }
            link = link.prev;
        }
        if (throwable != null) {
            throw (C) throwable;
        }
    }

}
