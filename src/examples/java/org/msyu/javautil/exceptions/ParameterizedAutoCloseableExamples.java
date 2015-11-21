package org.msyu.javautil.exceptions;

import org.testng.annotations.Test;

import java.io.IOException;

import static org.msyu.javautil.exceptions.ExampleUtils.youAreAPirate;
import static org.msyu.javautil.exceptions.ParameterizedAutoCloseable.wrap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ParameterizedAutoCloseableExamples {

    /**
     * Proof of concept: the wrapper throws only the exception subclass, not Exception.
     *
     * <p>Note that the code inside the try block can't use the wrapped object.
     * See {@linkplain #example02_ConstructAndSaveReferenceBeforeWrapping() this example} for how to fix that.</p>
     */
    @Test
    public void example01_ProofOfConcept() throws IOException {
        try (ParameterizedAutoCloseable<IOException> pac = wrap(new CloseableObject(), CloseableObject::customClose)) {
            youAreAPirate(pac);
        }
    }

    /**
     * You can construct the object and save a reference to it before wrapping it with ParameterizedAutoCloseable.
     */
    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "destructor")
    public void example02_ConstructAndSaveReferenceBeforeWrapping() throws IOException {
        UncloseableObject usefulObject = new UncloseableObject();
        try (ParameterizedAutoCloseable<IOException> pac = wrap(usefulObject, CloseableObject::customClose)) {
            youAreAPirate(usefulObject);
        }
    }

    /**
     * Use an outer try-catch block to catch exceptions from both the useful object's constructor and
     * {@link ParameterizedAutoCloseable#close() close()}.
     */
    @Test
    public void example03_HandlingExceptionsFromConstructor() {
        try {
            UncloseableObject usefulObject = new InconstructibleObject();
            try (ParameterizedAutoCloseable<IOException> pac = wrap(usefulObject, CloseableObject::customClose)) {
                youAreAPirate(usefulObject);
                fail("exception should've been thrown from constructor");
            }
        } catch (IOException e) {
            assertEquals(e.getMessage(), "constructor");
        }
    }

    /**
     * RuntimeException also extends Exception, but is not checked;
     * use that when your custom {@code close()} does not throw any exceptions.
     */
    @Test
    public void example04_NoExceptionIsRuntimeException() {
        Object usefulObject = new Object();
        try (ParameterizedAutoCloseable<RuntimeException> pac = wrap(usefulObject, Object::hashCode)) {
            youAreAPirate(usefulObject);
        }
    }


    private static class CloseableObject {
        public void customClose() throws IOException {
            // this one does and throws nothing
        }
    }

    private static class UncloseableObject extends CloseableObject {
        @Override
        public void customClose() throws IOException {
            throw new IOException("destructor");
        }
    }

    private static class InconstructibleObject extends UncloseableObject {
        public InconstructibleObject() throws IOException {
            throw new IOException("constructor");
        }
    }

}
