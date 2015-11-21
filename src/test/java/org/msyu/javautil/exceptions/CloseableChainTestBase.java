package org.msyu.javautil.exceptions;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

public class CloseableChainTestBase {

    protected Tracer tracer;

    protected int levelCounter;

    @BeforeMethod
    public void setUp() {
        tracer = Mockito.mock(Tracer.class);
        levelCounter = 0;
    }

    protected interface Tracer {
        void constructorSucceeds(int level);
        void chainComplete(Object finalOutput);
        void destructorSucceeds(int level);
    }

    protected class Dummy {

        private final boolean throwFromDestructor;

        protected Dummy(boolean throwFromConstructor, boolean throwFromDestructor, Object... args) throws Exception {
            ++levelCounter;
            if (throwFromConstructor) {
                throw new Exception("c" + (levelCounter--));
            }
            tracer.constructorSucceeds(levelCounter);
            this.throwFromDestructor = throwFromDestructor;
        }

        protected void close() throws Exception {
            try {
                if (throwFromDestructor) {
                    throw new Exception("d" + levelCounter);
                }
                tracer.destructorSucceeds(levelCounter);
            } finally {
                --levelCounter;
            }
        }

    }

    static void checkRootAndSuppressedExceptions(Exception actualException, String expectedException, List<String> expectedSuppressedExceptions) {
        assertEquals(actualException.getMessage(), expectedException, "root exception");
        assertEquals(
                Arrays.stream(actualException.getSuppressed()).map(Throwable::getMessage).collect(Collectors.toList()),
                expectedSuppressedExceptions,
                "suppressed exceptions"
        );
    }

}
