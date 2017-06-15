package org.msyu.javautil.exceptions;

import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.msyu.javautil.exceptions.AutoCloseableWrapper.runOnClose;
import static org.msyu.javautil.exceptions.ExampleUtils.youAreAPirate;

public class AutoCloseableWrapperExamples {

    @Test
    public void ex1_TheIdea() {
        Runnable objectWithCloseMethod = mock(Runnable.class);
        try (AutoCloseableWrapper<RuntimeException> acw = new AutoCloseableWrapper<>(objectWithCloseMethod::run)) {
            youAreAPirate(objectWithCloseMethod, acw);
        }
        verify(objectWithCloseMethod).run();
    }

    @Test
    public void ex2_LessWordsWithStaticImport() {
        Runnable objectWithCloseMethod = mock(Runnable.class);
        try (AutoCloseableWrapper<RuntimeException> acw = runOnClose(objectWithCloseMethod::run)) {
            youAreAPirate(objectWithCloseMethod, acw);
        }
        verify(objectWithCloseMethod).run();
    }

    @Test
    public void ex3_NullCloseAction() {
        Runnable objectWithCloseMethod = mock(Runnable.class);
        try (AutoCloseableWrapper<RuntimeException> acw = runOnClose(null)) {
            youAreAPirate(objectWithCloseMethod, acw);
        }
        verifyZeroInteractions(objectWithCloseMethod);
    }

    /**
     * Just a few examples of how exceptions could be handled with this pattern.
     * Use your imagination!
     */
    @Test
    public void ex3_ExceptionHandling() {
        try {
            Runnable object = null;
            try (AutoCloseableWrapper<RuntimeException> acw = runOnClose(null)) {
                youAreAPirate(object, acw);
            }
        } catch (Exception e) {
            // catch-all
        }

        {
            Runnable object;
            try {
                object = null;
            } catch (Exception e) {
                // handle creation error
                return;
            }
            try (AutoCloseableWrapper<RuntimeException> acw = runOnClose(null)) {
                youAreAPirate(object, acw);
            } catch (Exception e) {
                // handle (usage and) closing error
            }
        }
    }

}
