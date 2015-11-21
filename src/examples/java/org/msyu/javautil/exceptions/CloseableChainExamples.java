package org.msyu.javautil.exceptions;

import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.msyu.javautil.exceptions.CloseableChain.newCloseableChain;
import static org.msyu.javautil.exceptions.ExampleUtils.youAreAPirate;

public class CloseableChainExamples extends CloseableChainTestBase {

    /**
     * The essence of how to use CloseableChains: construct a chain, then use the last output, then close the chain.
     */
    @Test
    public void example00_TheBasicIdea() throws Exception {
        CloseableChain<Object, Exception> chain = newCloseableChain()
                .chain(this::constructor, this::destructor)
                .chain(this::constructor, this::destructor);

        youAreAPirate(chain.getOutput());

        CloseableChain.close(chain);
    }

    /**
     * This is closer to real life, with exception handling added.
     * The three parts can happen in different contexts and even on different threads
     * (don't forget to synchronize properly though!).
     */
    @Test
    public void example01_HowToHandleExceptions() {
        CloseableChain<Object, Exception> chain;

        try {
            chain = newCloseableChain()
                    .chain(this::constructor, this::destructor)
                    .chain(this::constructor, this::destructor);
        } catch (Exception e) {
            youAreAPirate(e);
            return;
        }

        try {
            youAreAPirate(chain.getOutput());
        } catch (Throwable t) {
            CloseableChain.close(chain, t);
            youAreAPirate(t);
            return;
        }

        try {
            CloseableChain.close(chain);
        } catch (Exception e) {
            youAreAPirate(e);
            return;
        }
    }

    /**
     * If the first constructor dies, nothing else gets executed.
     */
    @Test(expectedExceptions = Exception.class)
    public void example02_FirstConstructorDies() throws Exception {
        try {
            CloseableChain<Dummy, Exception> chain = newCloseableChain()
                    .chain(__ -> new Dummy(true, false), Dummy::close)
                    .chain(prev -> new Dummy(false, false, prev), Dummy::close);
            youAreAPirate(chain);
        } catch (Exception e) {
            Mockito.verifyZeroInteractions(tracer);
            checkRootAndSuppressedExceptions(e, "c1", Collections.emptyList());
            throw e;
        }
    }

    /**
     * If a subsequent constructor dies, all the previously opened chain links are closed in reverse order.
     */
    @Test(expectedExceptions = Exception.class)
    public void example03_NonFirstConstructorDies() throws Exception {
        try {
            CloseableChain<Dummy, Exception> chain = newCloseableChain()
                    .chain(__ -> new Dummy(false, false), Dummy::close)
                    .chain(prev -> new Dummy(false, false, prev), Dummy::close)
                    .chain(prev -> new Dummy(true, false, prev), Dummy::close)
                    .chain(prev -> new Dummy(false, false, prev), Dummy::close);
            youAreAPirate(chain);
        } catch (Exception e) {
            InOrder inOrder = Mockito.inOrder(tracer);
            inOrder.verify(tracer).constructorSucceeds(1);
            inOrder.verify(tracer).constructorSucceeds(2);
            inOrder.verify(tracer).destructorSucceeds(2);
            inOrder.verify(tracer).destructorSucceeds(1);
            inOrder.verifyNoMoreInteractions();
            checkRootAndSuppressedExceptions(e, "c3", Collections.emptyList());
            throw e;
        }
    }

    /**
     * All exceptions from destructors are suppressed by the constructor exception.
     */
    @Test(expectedExceptions = Exception.class)
    public void example04_FailureSuppressionInUnfinishedChains() throws Exception {
        try {
            CloseableChain<Dummy, Exception> chain = newCloseableChain()
                    .chain(__ -> new Dummy(false, true), Dummy::close)
                    .chain(prev -> new Dummy(false, true), Dummy::close)
                    .chain(prev -> new Dummy(true, false, prev), Dummy::close);
            youAreAPirate(chain);
        } catch (Exception e) {
            InOrder inOrder = Mockito.inOrder(tracer);
            inOrder.verify(tracer).constructorSucceeds(1);
            inOrder.verify(tracer).constructorSucceeds(2);
            inOrder.verifyNoMoreInteractions();
            checkRootAndSuppressedExceptions(e, "c3", Arrays.asList("d2", "d1"));
            throw e;
        }
    }

    /**
     * The first exception from an orderly shutdown suppresses the following ones.
     */
    @Test(expectedExceptions = Exception.class)
    public void example05_FailureSuppressionInOrderlyShutdowns() throws Exception {
        try {
            CloseableChain<Dummy, Exception> chain = newCloseableChain()
                    .chain(__ -> new Dummy(false, true), Dummy::close)
                    .chain(prev -> new Dummy(false, true, prev), Dummy::close)
                    .chain(prev -> new Dummy(false, false, prev), Dummy::close);

            tracer.chainComplete(chain.getOutput());

            CloseableChain.close(chain);
        } catch (Exception e) {
            InOrder inOrder = Mockito.inOrder(tracer);
            inOrder.verify(tracer).constructorSucceeds(1);
            inOrder.verify(tracer).constructorSucceeds(2);
            inOrder.verify(tracer).constructorSucceeds(3);
            inOrder.verify(tracer).chainComplete(Matchers.notNull());
            inOrder.verify(tracer).destructorSucceeds(3);
            inOrder.verifyNoMoreInteractions();
            checkRootAndSuppressedExceptions(e, "d2", Collections.singletonList("d1"));
            throw e;
        }
    }

    private Object constructor(Object arg) throws Exception {
        return null;
    }

    private void destructor(Object arg) throws Exception {
    }

}