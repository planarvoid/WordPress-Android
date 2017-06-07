package com.soundcloud.java.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.test.EqualsTester;
import com.soundcloud.java.test.SerializableTester;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class PredicatesTest {

    private static final Predicate<Integer> TRUE = Predicates.alwaysTrue();
    private static final Predicate<Integer> FALSE = Predicates.alwaysFalse();
    private static final Predicate<Integer> NEVER_REACHED =
            new Predicate<Integer>() {
                @Override
                public boolean apply(Integer i) {
                    throw new AssertionError(
                            "This predicate should never have been evaluated");
                }
            };

    /**
     * Instantiable predicate with reasonable hashCode() and equals() methods.
     */
    static class IsOdd implements Predicate<Integer>, Serializable {
        private static final long serialVersionUID = 0x150ddL;

        @Override
        public boolean apply(Integer i) {
            return (i.intValue() & 1) == 1;
        }

        @Override
        public int hashCode() {
            return 0x150dd;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof IsOdd;
        }

        @Override
        public String toString() {
            return "IsOdd";
        }
    }

    /**
     * Generates a new Predicate per call.
     *
     * <p>Creating a new Predicate each time helps catch cases where code is
     * using {@code x == y} instead of {@code x.equals(y)}.
     */
    private static IsOdd isOdd() {
        return new IsOdd();
    }

  /*
   * Tests for Predicates.alwaysTrue().
   */

    @Test
    public void testAlwaysTrue_apply() {
        assertEvalsToTrue(Predicates.alwaysTrue());
    }

    @Test
    public void testAlwaysTrue_equality() throws Exception {
        new EqualsTester()
                .addEqualityGroup(TRUE, Predicates.alwaysTrue())
                .addEqualityGroup(isOdd())
                .addEqualityGroup(Predicates.alwaysFalse())
                .testEquals();
    }

    @Test
    public void testAlwaysTrue_serialization() {
        checkSerialization(Predicates.alwaysTrue());
    }

  /*
   * Tests for Predicates.alwaysFalse().
   */

    @Test
    public void testAlwaysFalse_apply() throws Exception {
        assertEvalsToFalse(Predicates.alwaysFalse());
    }

    @Test
    public void testAlwaysFalse_equality() throws Exception {
        new EqualsTester()
                .addEqualityGroup(FALSE, Predicates.alwaysFalse())
                .addEqualityGroup(isOdd())
                .addEqualityGroup(Predicates.alwaysTrue())
                .testEquals();
    }

    @Test
    public void testAlwaysFalse_serialization() {
        checkSerialization(Predicates.alwaysFalse());
    }

  /*
   * Tests for Predicates.not(predicate).
   */

    @Test
    public void testNot_apply() {
        assertEvalsToTrue(Predicates.not(FALSE));
        assertEvalsToFalse(Predicates.not(TRUE));
        assertEvalsLikeOdd(Predicates.not(Predicates.not(isOdd())));
    }

    @Test
    public void testNot_equality() {
        new EqualsTester()
                .addEqualityGroup(Predicates.not(isOdd()), Predicates.not(isOdd()))
                .addEqualityGroup(Predicates.not(TRUE))
                .addEqualityGroup(isOdd())
                .testEquals();
    }

    @Test
    public void testNot_equalityForNotOfKnownValues() {
        new EqualsTester()
                .addEqualityGroup(TRUE, Predicates.alwaysTrue())
                .addEqualityGroup(FALSE)
                .addEqualityGroup(Predicates.not(TRUE))
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(FALSE, Predicates.alwaysFalse())
                .addEqualityGroup(TRUE)
                .addEqualityGroup(Predicates.not(FALSE))
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(Predicates.isNull(), Predicates.isNull())
                .addEqualityGroup(Predicates.notNull())
                .addEqualityGroup(Predicates.not(Predicates.isNull()))
                .testEquals();

        new EqualsTester()
                .addEqualityGroup(Predicates.notNull(), Predicates.notNull())
                .addEqualityGroup(Predicates.isNull())
                .addEqualityGroup(Predicates.not(Predicates.notNull()))
                .testEquals();
    }

    @Test
    public void testNot_serialization() {
        checkSerialization(Predicates.not(isOdd()));
    }

  /*
   * Tests for all the different flavors of Predicates.and().
   */

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_applyNoArgs() {
        assertEvalsToTrue(Predicates.and());
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_equalityNoArgs() {
        new EqualsTester()
                .addEqualityGroup(Predicates.and(), Predicates.and())
                .addEqualityGroup(Predicates.and(FALSE))
                .addEqualityGroup(Predicates.or())
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_applyOneArg() {
        assertEvalsLikeOdd(Predicates.and(isOdd()));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_equalityOneArg() {
        Object[] notEqualObjects = {Predicates.and(NEVER_REACHED, FALSE)};
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.and(NEVER_REACHED), Predicates.and(NEVER_REACHED))
                .addEqualityGroup(notEqualObjects)
                .addEqualityGroup(Predicates.and(isOdd()))
                .addEqualityGroup(Predicates.and())
                .addEqualityGroup(Predicates.or(NEVER_REACHED))
                .testEquals();
    }

    @Test
    public void testAnd_applyBinary() {
        assertEvalsLikeOdd(Predicates.and(isOdd(), TRUE));
        assertEvalsLikeOdd(Predicates.and(TRUE, isOdd()));
        assertEvalsToFalse(Predicates.and(FALSE, NEVER_REACHED));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_equalityBinary() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.and(TRUE, NEVER_REACHED),
                        Predicates.and(TRUE, NEVER_REACHED))
                .addEqualityGroup(Predicates.and(NEVER_REACHED, TRUE))
                .addEqualityGroup(Predicates.and(TRUE))
                .addEqualityGroup(Predicates.or(TRUE, NEVER_REACHED))
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_applyTernary() {
        assertEvalsLikeOdd(Predicates.and(isOdd(), TRUE, TRUE));
        assertEvalsLikeOdd(Predicates.and(TRUE, isOdd(), TRUE));
        assertEvalsLikeOdd(Predicates.and(TRUE, TRUE, isOdd()));
        assertEvalsToFalse(Predicates.and(TRUE, FALSE, NEVER_REACHED));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_equalityTernary() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.and(TRUE, isOdd(), NEVER_REACHED),
                        Predicates.and(TRUE, isOdd(), NEVER_REACHED))
                .addEqualityGroup(Predicates.and(isOdd(), NEVER_REACHED, TRUE))
                .addEqualityGroup(Predicates.and(TRUE))
                .addEqualityGroup(Predicates.or(TRUE, isOdd(), NEVER_REACHED))
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_applyIterable() {
        Collection<Predicate<Integer>> empty = Arrays.asList();
        assertEvalsToTrue(Predicates.and(empty));
        assertEvalsLikeOdd(Predicates.and(Arrays.asList(isOdd())));
        assertEvalsLikeOdd(Predicates.and(Arrays.asList(TRUE, isOdd())));
        assertEvalsToFalse(Predicates.and(Arrays.asList(FALSE, NEVER_REACHED)));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_equalityIterable() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.and(Arrays.asList(TRUE, NEVER_REACHED)),
                        Predicates.and(Arrays.asList(TRUE, NEVER_REACHED)),
                        Predicates.and(TRUE, NEVER_REACHED))
                .addEqualityGroup(Predicates.and(FALSE, NEVER_REACHED))
                .addEqualityGroup(Predicates.or(TRUE, NEVER_REACHED))
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_arrayDefensivelyCopied() {
        Predicate[] array = {Predicates.alwaysFalse()};
        Predicate<Object> predicate = Predicates.and(array);
        assertFalse(predicate.apply(1));
        array[0] = Predicates.alwaysTrue();
        assertFalse(predicate.apply(1));
    }

    @Test
    public void testAnd_listDefensivelyCopied() {
        List<Predicate<Object>> list = new ArrayList<>();
        Predicate<Object> predicate = Predicates.and(list);
        assertTrue(predicate.apply(1));
        list.add(Predicates.alwaysFalse());
        assertTrue(predicate.apply(1));
    }

    @Test
    public void testAnd_iterableDefensivelyCopied() {
        final List<Predicate<Object>> list = new ArrayList<>();
        Iterable<Predicate<Object>> iterable = new Iterable<Predicate<Object>>() {
            @Override
            public Iterator<Predicate<Object>> iterator() {
                return list.iterator();
            }
        };
        Predicate<Object> predicate = Predicates.and(iterable);
        assertTrue(predicate.apply(1));
        list.add(Predicates.alwaysFalse());
        assertTrue(predicate.apply(1));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_serializationNoArgs() {
        checkSerialization(Predicates.and());
    }

    @Test
    public void testAnd_serializationOneArg() {
        checkSerialization(Predicates.and(isOdd()));
    }

    @Test
    public void testAnd_serializationBinary() {
        checkSerialization(Predicates.and(TRUE, isOdd()));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_serializationTernary() {
        checkSerialization(Predicates.and(TRUE, isOdd(), FALSE));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testAnd_serializationIterable() {
        checkSerialization(Predicates.and(Arrays.asList(TRUE, FALSE)));
    }

  /*
   * Tests for all the different flavors of Predicates.or().
   */

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_applyNoArgs() {
        assertEvalsToFalse(Predicates.or());
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_equalityNoArgs() {
        new EqualsTester()
                .addEqualityGroup(Predicates.or(), Predicates.or())
                .addEqualityGroup(Predicates.or(TRUE))
                .addEqualityGroup(Predicates.and())
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_applyOneArg() {
        assertEvalsToTrue(Predicates.or(TRUE));
        assertEvalsToFalse(Predicates.or(FALSE));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_equalityOneArg() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.or(NEVER_REACHED), Predicates.or(NEVER_REACHED))
                .addEqualityGroup(Predicates.or(NEVER_REACHED, TRUE))
                .addEqualityGroup(Predicates.or(TRUE))
                .addEqualityGroup(Predicates.or())
                .addEqualityGroup(Predicates.and(NEVER_REACHED))
                .testEquals();
    }

    @Test
    public void testOr_applyBinary() {
        Predicate<Integer> falseOrFalse = Predicates.or(FALSE, FALSE);
        Predicate<Integer> falseOrTrue = Predicates.or(FALSE, TRUE);
        Predicate<Integer> trueOrAnything = Predicates.or(TRUE, NEVER_REACHED);

        assertEvalsToFalse(falseOrFalse);
        assertEvalsToTrue(falseOrTrue);
        assertEvalsToTrue(trueOrAnything);
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_equalityBinary() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.or(FALSE, NEVER_REACHED),
                        Predicates.or(FALSE, NEVER_REACHED))
                .addEqualityGroup(Predicates.or(NEVER_REACHED, FALSE))
                .addEqualityGroup(Predicates.or(TRUE))
                .addEqualityGroup(Predicates.and(FALSE, NEVER_REACHED))
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_applyTernary() {
        assertEvalsLikeOdd(Predicates.or(isOdd(), FALSE, FALSE));
        assertEvalsLikeOdd(Predicates.or(FALSE, isOdd(), FALSE));
        assertEvalsLikeOdd(Predicates.or(FALSE, FALSE, isOdd()));
        assertEvalsToTrue(Predicates.or(FALSE, TRUE, NEVER_REACHED));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_equalityTernary() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.or(FALSE, NEVER_REACHED, TRUE),
                        Predicates.or(FALSE, NEVER_REACHED, TRUE))
                .addEqualityGroup(Predicates.or(TRUE, NEVER_REACHED, FALSE))
                .addEqualityGroup(Predicates.or(TRUE))
                .addEqualityGroup(Predicates.and(FALSE, NEVER_REACHED, TRUE))
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_applyIterable() {
        Predicate<Integer> vacuouslyFalse =
                Predicates.or(Collections.<Predicate<Integer>>emptyList());
        Predicate<Integer> troo = Predicates.or(Collections.singletonList(TRUE));
    /*
     * newLinkedList() takes varargs. TRUE and FALSE are both instances of
     * Predicate<Integer>, so the call is safe.
     */
        Predicate<Integer> trueAndFalse = Predicates.or(Arrays.asList(TRUE, FALSE));

        assertEvalsToFalse(vacuouslyFalse);
        assertEvalsToTrue(troo);
        assertEvalsToTrue(trueAndFalse);
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_equalityIterable() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.or(Arrays.asList(FALSE, NEVER_REACHED)),
                        Predicates.or(Arrays.asList(FALSE, NEVER_REACHED)),
                        Predicates.or(FALSE, NEVER_REACHED))
                .addEqualityGroup(Predicates.or(TRUE, NEVER_REACHED))
                .addEqualityGroup(Predicates.and(FALSE, NEVER_REACHED))
                .testEquals();
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_arrayDefensivelyCopied() {
        Predicate[] array = {Predicates.alwaysFalse()};
        Predicate<Object> predicate = Predicates.or(array);
        assertFalse(predicate.apply(1));
        array[0] = Predicates.alwaysTrue();
        assertFalse(predicate.apply(1));
    }

    @Test
    public void testOr_listDefensivelyCopied() {
        List<Predicate<Object>> list = new ArrayList<>();
        Predicate<Object> predicate = Predicates.or(list);
        assertFalse(predicate.apply(1));
        list.add(Predicates.alwaysTrue());
        assertFalse(predicate.apply(1));
    }

    @Test
    public void testOr_iterableDefensivelyCopied() {
        final List<Predicate<Object>> list = new ArrayList<>();
        Iterable<Predicate<Object>> iterable = new Iterable<Predicate<Object>>() {
            @Override
            public Iterator<Predicate<Object>> iterator() {
                return list.iterator();
            }
        };
        Predicate<Object> predicate = Predicates.or(iterable);
        assertFalse(predicate.apply(1));
        list.add(Predicates.alwaysTrue());
        assertFalse(predicate.apply(1));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_serializationNoArgs() {
        checkSerialization(Predicates.or());
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_serializationOneArg() {
        checkSerialization(Predicates.or(isOdd()));
    }

    @Test
    public void testOr_serializationBinary() {
        checkSerialization(Predicates.or(isOdd(), TRUE));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_serializationTernary() {
        checkSerialization(Predicates.or(FALSE, isOdd(), TRUE));
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testOr_serializationIterable() {
        Predicate<Integer> pre = Predicates.or(Arrays.asList(TRUE, FALSE));
        Predicate<Integer> post = SerializableTester.reserializeAndAssert(pre);
        assertEquals(pre.apply(0), post.apply(0));
    }

  /*
   * Tests for Predicates.equalTo(x).
   */

    @Test
    public void testIsEqualTo_apply() {
        Predicate<Integer> isOne = Predicates.equalTo(1);

        assertTrue(isOne.apply(1));
        assertFalse(isOne.apply(2));
        assertFalse(isOne.apply(null));
    }

    @Test
    public void testIsEqualTo_equality() {
        new EqualsTester()
                .addEqualityGroup(Predicates.equalTo(1), Predicates.equalTo(1))
                .addEqualityGroup(Predicates.equalTo(2))
                .addEqualityGroup(Predicates.equalTo(null))
                .testEquals();
    }

    @Test
    public void testIsEqualToNull_apply() {
        Predicate<Integer> isNull = Predicates.equalTo(null);
        assertTrue(isNull.apply(null));
        assertFalse(isNull.apply(1));
    }

    @Test
    public void testIsEqualToNull_equality() {
        new EqualsTester()
                .addEqualityGroup(Predicates.equalTo(null), Predicates.equalTo(null))
                .addEqualityGroup(Predicates.equalTo(1))
                .addEqualityGroup(Predicates.equalTo("null"))
                .testEquals();
    }

    @Test
    public void testIsEqualTo_serialization() {
        checkSerialization(Predicates.equalTo(1));
    }

    @Test
    public void testIsEqualToNull_serialization() {
        checkSerialization(Predicates.equalTo(null));
    }

    /**
     * Tests for Predicates.instanceOf(x).
     * TODO: Fix the comment style after fixing annotation stripper to remove
     * comments properly.  Currently, all tests before the comments are removed
     * as well.
     */

    @Test
    public void testIsInstanceOf_apply() {
        Predicate<Object> isInteger = Predicates.instanceOf(Integer.class);

        assertTrue(isInteger.apply(1));
        assertFalse(isInteger.apply(2.0f));
        assertFalse(isInteger.apply(""));
        assertFalse(isInteger.apply(null));
    }

    @Test
    public void testIsInstanceOf_subclass() {
        Predicate<Object> isNumber = Predicates.instanceOf(Number.class);

        assertTrue(isNumber.apply(1));
        assertTrue(isNumber.apply(2.0f));
        assertFalse(isNumber.apply(""));
        assertFalse(isNumber.apply(null));
    }

    @Test
    public void testIsInstanceOf_interface() {
        Predicate<Object> isComparable = Predicates.instanceOf(Comparable.class);

        assertTrue(isComparable.apply(1));
        assertTrue(isComparable.apply(2.0f));
        assertTrue(isComparable.apply(""));
        assertFalse(isComparable.apply(null));
    }

    @Test
    public void testIsInstanceOf_equality() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.instanceOf(Integer.class),
                        Predicates.instanceOf(Integer.class))
                .addEqualityGroup(Predicates.instanceOf(Number.class))
                .addEqualityGroup(Predicates.instanceOf(Float.class))
                .testEquals();
    }

    @Test
    public void testIsInstanceOf_serialization() {
        checkSerialization(Predicates.instanceOf(Integer.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsAssignableFrom_apply() {
        Predicate<Class<?>> isInteger = Predicates.assignableFrom(Integer.class);

        assertTrue(isInteger.apply(Integer.class));
        assertFalse(isInteger.apply(Float.class));

        isInteger.apply(null);
    }

    @Test
    public void testIsAssignableFrom_subclass() {
        Predicate<Class<?>> isNumber = Predicates.assignableFrom(Number.class);

        assertTrue(isNumber.apply(Integer.class));
        assertTrue(isNumber.apply(Float.class));
    }

    @Test
    public void testIsAssignableFrom_interface() {
        Predicate<Class<?>> isComparable =
                Predicates.assignableFrom(Comparable.class);

        assertTrue(isComparable.apply(Integer.class));
        assertTrue(isComparable.apply(Float.class));
    }

    @Test
    public void testIsAssignableFrom_equality() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.assignableFrom(Integer.class),
                        Predicates.assignableFrom(Integer.class))
                .addEqualityGroup(Predicates.assignableFrom(Number.class))
                .addEqualityGroup(Predicates.assignableFrom(Float.class))
                .testEquals();
    }

    @Test
    public void testIsAssignableFrom_serialization() {
        Predicate<Class<?>> predicate =
                Predicates.assignableFrom(Integer.class);
        Predicate<Class<?>> reserialized =
                SerializableTester.reserializeAndAssert(predicate);

        assertEvalsLike(predicate, reserialized, Integer.class);
        assertEvalsLike(predicate, reserialized, Float.class);
        assertEvalsLike(predicate, reserialized, null);
    }

  /*
   * Tests for Predicates.isNull()
   */

    @Test
    public void testIsNull_apply() {
        Predicate<Integer> isNull = Predicates.isNull();
        assertTrue(isNull.apply(null));
        assertFalse(isNull.apply(1));
    }

    @Test
    public void testIsNull_serialization() {
        Predicate<String> pre = Predicates.isNull();
        Predicate<String> post = SerializableTester.reserializeAndAssert(pre);
        assertEquals(pre.apply("foo"), post.apply("foo"));
        assertEquals(pre.apply(null), post.apply(null));
    }

    @Test
    public void testIsNull_equality() {
        new EqualsTester()
                .addEqualityGroup(Predicates.isNull(), Predicates.isNull())
                .addEqualityGroup(Predicates.notNull())
                .testEquals();
    }

    @Test
    public void testNotNull_apply() {
        Predicate<Integer> notNull = Predicates.notNull();
        assertFalse(notNull.apply(null));
        assertTrue(notNull.apply(1));
    }

    @Test
    public void testNotNull_equality() {
        new EqualsTester()
                .addEqualityGroup(Predicates.notNull(), Predicates.notNull())
                .addEqualityGroup(Predicates.isNull())
                .testEquals();
    }

    @Test
    public void testNotNull_serialization() {
        checkSerialization(Predicates.notNull());
    }

    @Test
    public void testIn_apply() {
        Collection<Integer> nums = Arrays.asList(1, 5);
        Predicate<Integer> isOneOrFive = Predicates.in(nums);

        assertTrue(isOneOrFive.apply(1));
        assertTrue(isOneOrFive.apply(5));
        assertFalse(isOneOrFive.apply(3));
        assertFalse(isOneOrFive.apply(null));
    }

    @Ignore // not sure this is testing the right thing, or equals is broken
    // see https://github.com/google/guava/issues/2103
    @Test
    public void testIn_equality() {
        Collection<Integer> nums = Arrays.asList(1, 5);
        Collection<Integer> sameOrder = Arrays.asList(1, 5);
        Collection<Integer> differentOrder = Arrays.asList(5, 1);
        Collection<Integer> differentNums = Arrays.asList(1, 3, 5);

        new EqualsTester()
                .addEqualityGroup(Predicates.in(nums), Predicates.in(nums),
                        Predicates.in(sameOrder), Predicates.in(differentOrder))
                .addEqualityGroup(Predicates.in(differentNums))
                .testEquals();
    }

    @Test
    public void testIn_handlesNullPointerException() {
        class CollectionThatThrowsNPE<T> extends ArrayList<T> {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean contains(Object element) {
                Preconditions.checkNotNull(element);
                return super.contains(element);
            }
        }
        Collection<Integer> nums = new CollectionThatThrowsNPE<Integer>();
        Predicate<Integer> isFalse = Predicates.in(nums);
        assertFalse(isFalse.apply(null));
    }

    @Test
    public void testIn_handlesClassCastException() {
        class CollectionThatThrowsCCE<T> extends ArrayList<T> {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean contains(Object element) {
                throw new ClassCastException("");
            }
        }
        Collection<Integer> nums = new CollectionThatThrowsCCE<>();
        nums.add(3);
        Predicate<Integer> isThree = Predicates.in(nums);
        assertFalse(isThree.apply(3));
    }

    /*
     * Tests that compilation will work when applying explicit types.
     */
    @SuppressWarnings("unused") // compilation test
    public void testIn_compilesWithExplicitSupertype() {
        Collection<Number> nums = Collections.emptyList();
        Predicate<Number> p1 = Predicates.in(nums);
        Predicate<Object> p2 = Predicates.<Object>in(nums);
        // The next two lines are not expected to compile.
        // Predicate<Integer> p3 = Predicates.in(nums);
        // Predicate<Integer> p4 = Predicates.<Integer>in(nums);
    }

    @Test
    public void testIn_serialization() {
        checkSerialization(Predicates.in(Arrays.asList(1, 2, 3, null)));
    }

    // enum singleton pattern
    private enum TrimStringFunction implements Function<String, String> {
        INSTANCE;

        @Override
        public String apply(String string) {
            return string.trim();
        }
    }

    @Test
    @SuppressWarnings("unchecked") // varargs
    public void testCascadingSerialization() throws Exception {
        // Eclipse says Predicate<Integer>; javac says Predicate<Object>.
        Predicate<? super Integer> nasty = Predicates.not(Predicates.and(
                Predicates.or(
                        Predicates.equalTo((Object) 1), Predicates.equalTo(null),
                        Predicates.alwaysFalse(), Predicates.alwaysTrue(),
                        Predicates.isNull(), Predicates.notNull(),
                        Predicates.in(Arrays.asList(1)))));
        assertEvalsToFalse(nasty);

        Predicate<? super Integer> stillNasty =
                SerializableTester.reserializeAndAssert(nasty);

        assertEvalsToFalse(stillNasty);
    }

    @Test
    public void testCompose() {
        Function<String, String> trim = TrimStringFunction.INSTANCE;
        Predicate<String> equalsFoo = Predicates.equalTo("Foo");
        Predicate<String> equalsBar = Predicates.equalTo("Bar");
        Predicate<String> trimEqualsFoo = Predicates.compose(equalsFoo, trim);
        Function<String, String> identity = Functions.identity();

        assertTrue(trimEqualsFoo.apply("Foo"));
        assertTrue(trimEqualsFoo.apply("   Foo   "));
        assertFalse(trimEqualsFoo.apply("Foo-b-que"));

        new EqualsTester()
                .addEqualityGroup(trimEqualsFoo, Predicates.compose(equalsFoo, trim))
                .addEqualityGroup(equalsFoo)
                .addEqualityGroup(trim)
                .addEqualityGroup(Predicates.compose(equalsFoo, identity))
                .addEqualityGroup(Predicates.compose(equalsBar, trim))
                .testEquals();
    }

    @Test
    public void testComposeSerialization() {
        Function<String, String> trim = TrimStringFunction.INSTANCE;
        Predicate<String> equalsFoo = Predicates.equalTo("Foo");
        Predicate<String> trimEqualsFoo = Predicates.compose(equalsFoo, trim);
        SerializableTester.reserializeAndAssert(trimEqualsFoo);
    }

    /**
     * Tests for Predicates.contains(Pattern) and .containsPattern(String).
     * We assume the regex level works, so there are only trivial tests of that
     * aspect.
     * TODO: Fix comment style once annotation stripper is fixed.
     */

    @Test
    public void testContainsPattern_apply() {
        Predicate<CharSequence> isFoobar =
                Predicates.containsPattern("^Fo.*o.*bar$");
        assertTrue(isFoobar.apply("Foxyzoabcbar"));
        assertFalse(isFoobar.apply("Foobarx"));
    }

    @Test
    public void testContainsPattern_serialization() {
        Predicate<CharSequence> pre = Predicates.containsPattern("foo");
        Predicate<CharSequence> post = SerializableTester.reserializeAndAssert(pre);
        assertEquals(pre.apply("foo"), post.apply("foo"));
    }

    @Test
    public void testContains_apply() {
        Predicate<CharSequence> isFoobar =
                Predicates.contains(Pattern.compile("^Fo.*o.*bar$"));

        assertTrue(isFoobar.apply("Foxyzoabcbar"));
        assertFalse(isFoobar.apply("Foobarx"));
    }

    @Test
    public void testContains_equals() {
        new EqualsTester()
                .addEqualityGroup(
                        Predicates.contains(Pattern.compile("foo")),
                        Predicates.containsPattern("foo"))
                .addEqualityGroup(
                        Predicates.contains(
                                Pattern.compile("foo", Pattern.CASE_INSENSITIVE)))
                .addEqualityGroup(
                        Predicates.containsPattern("bar"))
                .testEquals();
    }

    private void assertEqualHashCode(
            Predicate<? super Integer> expected, Predicate<? super Integer> actual) {
        assertEquals(actual + " should hash like " + expected, expected.hashCode(), actual.hashCode());
    }

    @Test
    public void testHashCodeForBooleanOperations() {
        Predicate<Integer> p1 = Predicates.isNull();
        Predicate<Integer> p2 = isOdd();

        // Make sure that hash codes are not computed per-instance.
        assertEqualHashCode(
                Predicates.not(p1),
                Predicates.not(p1));

        assertEqualHashCode(
                Predicates.and(p1, p2),
                Predicates.and(p1, p2));

        assertEqualHashCode(
                Predicates.or(p1, p2),
                Predicates.or(p1, p2));

        // While not a contractual requirement, we'd like the hash codes for ands
        // & ors of the same predicates to not collide.
        assertTrue(Predicates.and(p1, p2).hashCode() != Predicates.or(p1, p2).hashCode());
    }

    private static void assertEvalsToTrue(Predicate<? super Integer> predicate) {
        assertTrue(predicate.apply(0));
        assertTrue(predicate.apply(1));
        assertTrue(predicate.apply(null));
    }

    private static void assertEvalsToFalse(Predicate<? super Integer> predicate) {
        assertFalse(predicate.apply(0));
        assertFalse(predicate.apply(1));
        assertFalse(predicate.apply(null));
    }

    private static void assertEvalsLikeOdd(Predicate<? super Integer> predicate) {
        assertEvalsLike(isOdd(), predicate);
    }

    private static void assertEvalsLike(
            Predicate<? super Integer> expected,
            Predicate<? super Integer> actual) {
        assertEvalsLike(expected, actual, 0);
        assertEvalsLike(expected, actual, 1);
        assertEvalsLike(expected, actual, null);
    }

    private static <T> void assertEvalsLike(
            Predicate<? super T> expected,
            Predicate<? super T> actual,
            T input) {
        Boolean expectedResult = null;
        RuntimeException expectedRuntimeException = null;
        try {
            expectedResult = expected.apply(input);
        } catch (RuntimeException e) {
            expectedRuntimeException = e;
        }

        Boolean actualResult = null;
        RuntimeException actualRuntimeException = null;
        try {
            actualResult = actual.apply(input);
        } catch (RuntimeException e) {
            actualRuntimeException = e;
        }

        assertEquals(expectedResult, actualResult);
        if (expectedRuntimeException != null) {
            assertNotNull(actualRuntimeException);
            assertEquals(
                    expectedRuntimeException.getClass(),
                    actualRuntimeException.getClass());
        }
    }

    private static void checkSerialization(Predicate<? super Integer> predicate) {
        Predicate<? super Integer> reserialized =
                SerializableTester.reserializeAndAssert(predicate);
        assertEvalsLike(predicate, reserialized);
    }
}