package com.soundcloud.java.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.soundcloud.java.collections.AbstractIterator;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class AbstractIteratorTest {

    @Test
    public void testDefaultBehaviorOfNextAndHasNext() {

        // This sample AbstractIterator returns 0 on the first call, 1 on the
        // second, then signals that it's reached the end of the data
        Iterator<Integer> iter = new AbstractIterator<Integer>() {
            private int rep;

            @Override
            public Integer computeNext() {
                switch (rep++) {
                    case 0:
                        return 0;
                    case 1:
                        return 1;
                    case 2:
                        return endOfData();
                    default:
                        fail("Should not have been invoked again");
                        return null;
                }
            }
        };

        assertTrue(iter.hasNext());
        assertEquals(0, (int) iter.next());

        // verify idempotence of hasNext()
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(1, (int) iter.next());

        assertFalse(iter.hasNext());

        // Make sure computeNext() doesn't get invoked again
        assertFalse(iter.hasNext());

        try {
            iter.next();
            fail("no exception thrown");
        } catch (NoSuchElementException ignored) {
        }
    }

    @Test
    public void testDefaultBehaviorOfPeek() {
    /*
     * This sample AbstractIterator returns 0 on the first call, 1 on the
     * second, then signals that it's reached the end of the data
     */
        AbstractIterator<Integer> iter = new AbstractIterator<Integer>() {
            private int rep;

            @Override
            public Integer computeNext() {
                switch (rep++) {
                    case 0:
                        return 0;
                    case 1:
                        return 1;
                    case 2:
                        return endOfData();
                    default:
                        fail("Should not have been invoked again");
                        return null;
                }
            }
        };

        assertEquals(0, (int) iter.peek());
        assertEquals(0, (int) iter.peek());
        assertTrue(iter.hasNext());
        assertEquals(0, (int) iter.peek());
        assertEquals(0, (int) iter.next());

        assertEquals(1, (int) iter.peek());
        assertEquals(1, (int) iter.next());

        try {
            iter.peek();
            fail("peek() should throw NoSuchElementException at end");
        } catch (NoSuchElementException ignored) {
        }

        try {
            iter.peek();
            fail("peek() should continue to throw NoSuchElementException at end");
        } catch (NoSuchElementException ignored) {
        }

        try {
            iter.next();
            fail("next() should throw NoSuchElementException as usual");
        } catch (NoSuchElementException ignored) {
        }

        try {
            iter.peek();
            fail("peek() should still throw NoSuchElementException after next()");
        } catch (NoSuchElementException ignored) {
        }
    }

    // Didn't want to add the entire Guava test-lib because of this
//    @Test
//    public void testFreesNextReference() {
//        Iterator<Object> itr = new AbstractIterator<Object>() {
//            @Override
//            public Object computeNext() {
//                return new Object();
//            }
//        };
//        WeakReference<Object> ref = new WeakReference<>(itr.next());
//        GcFinalization.awaitClear(ref);
//    }

    @Test
    public void testDefaultBehaviorOfPeekForEmptyIteration() {

        AbstractIterator<Integer> empty = new AbstractIterator<Integer>() {
            private boolean alreadyCalledEndOfData;

            @Override
            public Integer computeNext() {
                if (alreadyCalledEndOfData) {
                    fail("Should not have been invoked again");
                }
                alreadyCalledEndOfData = true;
                return endOfData();
            }
        };

        try {
            empty.peek();
            fail("peek() should throw NoSuchElementException at end");
        } catch (NoSuchElementException ignored) {
        }

        try {
            empty.peek();
            fail("peek() should continue to throw NoSuchElementException at end");
        } catch (NoSuchElementException ignored) {
        }
    }

    @Test
    public void testSneakyThrow() throws Exception {
        Iterator<Integer> iter = new AbstractIterator<Integer>() {
            boolean haveBeenCalled;

            @Override
            public Integer computeNext() {
                if (haveBeenCalled) {
                    fail("Should not have been called again");
                } else {
                    haveBeenCalled = true;
                    sneakyThrow(new SomeCheckedException());
                }
                return null; // never reached
            }
        };

        // The first time, the sneakily-thrown exception comes out
        try {
            iter.hasNext();
            fail("No exception thrown");
        } catch (Exception e) {
            if (!(e instanceof SomeCheckedException)) {
                throw e;
            }
        }

        // But the second time, AbstractIterator itself throws an ISE
        try {
            iter.hasNext();
            fail("No exception thrown");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void testException() {
        final SomeUncheckedException exception = new SomeUncheckedException();
        Iterator<Integer> iter = new AbstractIterator<Integer>() {
            @Override
            public Integer computeNext() {
                throw exception;
            }
        };

        // It should pass through untouched
        try {
            iter.hasNext();
            fail("No exception thrown");
        } catch (SomeUncheckedException e) {
            assertSame(exception, e);
        }
    }

    @Test
    public void testExceptionAfterEndOfData() {
        Iterator<Integer> iter = new AbstractIterator<Integer>() {
            @Override
            public Integer computeNext() {
                endOfData();
                throw new SomeUncheckedException();
            }
        };
        try {
            iter.hasNext();
            fail("No exception thrown");
        } catch (SomeUncheckedException ignored) {
        }
    }

    @Test
    public void testCantRemove() {
        Iterator<Integer> iter = new AbstractIterator<Integer>() {
            boolean haveBeenCalled;

            @Override
            public Integer computeNext() {
                if (haveBeenCalled) {
                    endOfData();
                }
                haveBeenCalled = true;
                return 0;
            }
        };

        assertEquals(0, (int) iter.next());

        try {
            iter.remove();
            fail("No exception thrown");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testReentrantHasNext() {
        Iterator<Integer> iter = new AbstractIterator<Integer>() {
            @Override
            protected Integer computeNext() {
                hasNext();
                return null;
            }
        };
        try {
            iter.hasNext();
            fail();
        } catch (IllegalStateException ignored) {
        }
    }

    // Technically we should test other reentrant scenarios (9 combinations of
    // hasNext/next/peek), but we'll cop out for now, knowing that peek() and
    // next() both start by invoking hasNext() anyway.

    /**
     * Throws a undeclared checked exception.
     */
    private static void sneakyThrow(Throwable t) {
        class SneakyThrower<T extends Throwable> {
            @SuppressWarnings("unchecked")
                // not really safe, but that's the point
            void throwIt(Throwable t) throws T {
                throw (T) t;
            }
        }
        new SneakyThrower<Error>().throwIt(t);
    }

    private static class SomeCheckedException extends Exception {
    }

    private static class SomeUncheckedException extends RuntimeException {
    }
}