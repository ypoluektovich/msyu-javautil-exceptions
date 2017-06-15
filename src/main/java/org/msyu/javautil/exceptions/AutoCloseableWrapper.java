package org.msyu.javautil.exceptions;

public final class AutoCloseableWrapper<X extends Exception> implements AutoCloseable {

    public static <X extends Exception> AutoCloseableWrapper<X> runOnClose(
            RunnableWithException<? extends X> closeAction
    ) {
        return new AutoCloseableWrapper<>(closeAction);
    }

    private final RunnableWithException<? extends X> closeAction;

    public AutoCloseableWrapper(RunnableWithException<? extends X> closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    public final void close() throws X {
        if (closeAction != null) {
            closeAction.run();
        }
    }

}
