package org.msyu.javautil.exceptions;

import org.mockito.InOrder;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.msyu.javautil.exceptions.CloseableChain.newCloseableChain;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class CloseableChainTest extends CloseableChainTestBase {

    @Test
    public void verifyOrderWithNoExceptions() throws Exception {
        CloseableChain<Dummy, Exception> chain = newCloseableChain()
                .chain(__ -> new Dummy(false, false), Dummy::close)
                .chain(prev -> new Dummy(false, false, prev), Dummy::close);

        assertNotNull(chain.getOutput());

        CloseableChain.close(chain);

        InOrder inOrder = Mockito.inOrder(tracer);
        inOrder.verify(tracer).constructorSucceeds(1);
        inOrder.verify(tracer).constructorSucceeds(2);
        inOrder.verify(tracer).destructorSucceeds(2);
        inOrder.verify(tracer).destructorSucceeds(1);
        inOrder.verifyNoMoreInteractions();
    }


    @Test(dataProvider = "exhaustiveTests")
    public void test(List<Boolean> dummyParameters, String expectedException, List<String> expectedSuppressedExceptions) {
        assert dummyParameters.size() % 2 == 0 : "dummy parameter list size must be even";
        assert (expectedException == null) == (expectedSuppressedExceptions == null) :
                "expected exception parameters must be present or absent together";

        CloseableChain<?, Exception> chain = CloseableChain.newCloseableChain();
        try {
            Iterator<Boolean> dummyParameterIterator = dummyParameters.iterator();
            while (dummyParameterIterator.hasNext()) {
                boolean constructorThrows = dummyParameterIterator.next();
                boolean destructorThrows = dummyParameterIterator.next();
                chain = chain.chain(prev -> new Dummy(constructorThrows, destructorThrows, prev), Dummy::close);
            }

            assert (chain.getOutput() == null) == dummyParameters.isEmpty() : "expected the chain to construct something";
            assertEquals(levelCounter, dummyParameters.size() / 2, "amount of constructed objects");

            CloseableChain.close(chain);

            assert expectedException == null : "expected an exception, but none was thrown";
        } catch (Exception e) {
            assert expectedException != null : "expected no exception, but one or more was thrown";
            checkRootAndSuppressedExceptions(e, expectedException, expectedSuppressedExceptions);
        }
    }

    /**
     * Runs {@linkplain #generateExhaustiveTestsForLevel(int) exhaustive tests} of levels 0 to 3, inclusive.
     */
    @DataProvider
    public static Iterator<Object[]> exhaustiveTests() {
        return IntStream.rangeClosed(0, 3)
                .mapToObj(level -> Spliterators.spliterator(
                        CloseableChainTest.generateExhaustiveTestsForLevel(level),
                        1 << (level * 2),
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.IMMUTABLE
                ))
                .flatMap(spliterator -> StreamSupport.stream(spliterator, false))
                .iterator();
    }

    /**
     * Generates an exhaustive set of tests of the specified level.
     *
     * <p>The tests are exhaustive in the sense that they cover all possible combinations of failures in
     * the chain's constructors and destructors.</p>
     *
     * <p>The level is the amount of dummies in the simulated closeable chain.
     * Not all dummies will be used in those tests that terminate early due to constructor failure;
     * no effort is made to detect and cull such redundant tests.
     * You get all the <code>2<sup>2*maxLevel</sup></code> possible combinations.
     * Therefore, to prevent the combinatorial explosion, {@code maxLevel} is artificially capped at 5.</p>
     */
    private static Iterator<Object[]> generateExhaustiveTestsForLevel(int maxLevel) {
        // Level 5 => 10 dummy parameters => 2^10 tests. You don't need more.
        assert maxLevel <= 5 : "exhaustive tests of max level 6 or more are not supported";
        // And just to be sure...
        assert maxLevel >= 0 : "exhaustive tests of negative levels are not supported";

        int parameterCount = maxLevel * 2;
        int testCount = 1 << parameterCount;
        return new Iterator<Object[]>() {
            private int currentMask = 0;

            @Override
            public boolean hasNext() {
                return currentMask < testCount;
            }

            @Override
            public Object[] next() {
                int levelSizedMask = (1 << maxLevel) - 1;
                int constructorBits = currentMask & levelSizedMask;
                int destructorBits = (currentMask >>> maxLevel) & levelSizedMask;
                List<Boolean> dummyParameters = new ArrayList<>(parameterCount);
                String expectedException = null;
                List<String> expectedSuppressedExceptions = new ArrayList<>();
                for (int i = 0; i < maxLevel; ++i) {
                    boolean constructorThrows = (constructorBits & 1) != 0;
                    boolean destructorThrows = (destructorBits & 1) != 0;

                    dummyParameters.add(constructorThrows);
                    dummyParameters.add(destructorThrows);

                    if (constructorThrows && expectedException == null) {
                        expectedException = "c" + (i + 1);
                    }
                    if (destructorThrows && expectedException == null) {
                        expectedSuppressedExceptions.add("d" + (i + 1));
                    }

                    constructorBits >>>= 1;
                    destructorBits >>>= 1;
                }
                if (expectedException == null && !expectedSuppressedExceptions.isEmpty()) {
                    expectedException = expectedSuppressedExceptions.remove(expectedSuppressedExceptions.size() - 1);
                }
                Collections.reverse(expectedSuppressedExceptions);
                ++currentMask;
                return new Object[]{
                        dummyParameters,
                        expectedException,
                        expectedException == null ? null : expectedSuppressedExceptions
                };
            }
        };
    }

}