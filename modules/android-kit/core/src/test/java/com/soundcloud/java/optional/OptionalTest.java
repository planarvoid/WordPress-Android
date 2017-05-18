package com.soundcloud.java.optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.soundcloud.java.functions.Consumer;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Functions;
import com.soundcloud.java.test.SerializableTester;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class OptionalTest {
    private static final Function<String, Boolean> NOT_EMPTY_PREDICATE = new Function<String, Boolean>() {
        @Nullable
        @Override
        public Boolean apply(String input) {
            return !input.isEmpty();
        }
    };

    private static final Function<String, Boolean> NULL_PREDICATE = new Function<String, Boolean>() {
        @Nullable
        @Override
        public Boolean apply(String input) {
            return null;
        }
    };

    @Test
    public void testAbsent() {
        Optional<String> optionalName = Optional.absent();
        assertFalse(optionalName.isPresent());
    }

    @Test
    public void testOf() {
        assertEquals("training", Optional.of("training").get());
    }

    @Test(expected = NullPointerException.class)
    public void testOf_null() {
        Optional.of(null);
    }

    @Test
    public void testFromNullable() {
        Optional<String> optionalName = Optional.fromNullable("bob");
        assertEquals("bob", optionalName.get());
    }

    @Test
    public void testFromNullable_null() {
        // not promised by spec, but easier to test
        assertSame(Optional.absent(), Optional.fromNullable(null));
    }

    @Test
    public void testIsPresent_no() {
        assertFalse(Optional.absent().isPresent());
    }

    @Test
    public void testIsPresent_yes() {
        assertTrue(Optional.of("training").isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testGet_absent() {
        Optional.absent().get();
    }

    @Test
    public void testGet_present() {
        assertEquals("training", Optional.of("training").get());
    }

    @Test
    public void testOr_T_present() {
        assertEquals("a", Optional.of("a").or("default"));
    }

    @Test
    public void testOr_T_absent() {
        assertEquals("default", Optional.absent().or("default"));
    }

    @Test
    public void testOr_Optional_present() {
        assertEquals(Optional.of("a"), Optional.of("a").or(Optional.of("fallback")));
    }

    @Test
    public void testOr_Optional_absent() {
        assertEquals(Optional.of("fallback"), Optional.absent().or(Optional.of("fallback")));
    }

    @Test
    public void testOrNull_present() {
        assertEquals("a", Optional.of("a").orNull());
    }

    @Test
    public void testOrNull_absent() {
        assertNull(Optional.absent().orNull());
    }

    @Test
    public void testAsSet_present() {
        Set<String> expected = Collections.singleton("a");
        assertEquals(expected, Optional.of("a").asSet());
    }

    @Test
    public void testAsSet_absent() {
        assertTrue("Returned set should be empty", Optional.absent().asSet().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAsSet_presentIsImmutable() {
        Set<String> presentAsSet = Optional.of("a").asSet();
        presentAsSet.add("b");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAsSet_absentIsImmutable() {
        Set<Object> absentAsSet = Optional.absent().asSet();
        absentAsSet.add("foo");
    }

    @Test
    public void testTransform_absent() {
        assertEquals(Optional.absent(), Optional.absent().transform(Functions.identity()));
        assertEquals(Optional.absent(), Optional.absent().transform(Functions.toStringFunction()));
    }

    @Test
    public void testTransform_presentIdentity() {
        assertEquals(Optional.of("a"), Optional.of("a").transform(Functions.identity()));
    }

    @Test
    public void testTransform_presentToString() {
        assertEquals(Optional.of("42"), Optional.of(42).transform(Functions.toStringFunction()));
    }

    @Test(expected = NullPointerException.class)
    public void testTransform_present_functionReturnsNull() {
        Optional.of("a").transform(
                new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                        return null;
                    }
                });
    }

    @Test
    public void testTransform_abssent_functionReturnsNull() {
        assertEquals(Optional.absent(),
                     Optional.absent().transform(
                             new Function<Object, Object>() {
                                 @Override
                                 public Object apply(Object input) {
                                     return null;
                                 }
                             }));
    }

    @Test
    public void testEqualsAndHashCode_absent() {
        assertEquals(Optional.<String>absent(), Optional.<Integer>absent());
        assertEquals(Optional.absent().hashCode(), Optional.absent().hashCode());
    }

    @Test
    public void testEqualsAndHashCode_present() {
        assertEquals(Optional.of("training"), Optional.of("training"));
        assertFalse(Optional.of("a").equals(Optional.of("b")));
        assertFalse(Optional.of("a").equals(Optional.absent()));
        assertEquals(Optional.of("training").hashCode(), Optional.of("training").hashCode());
    }

    @Test
    public void testToString_absent() {
        assertEquals("Optional.absent()", Optional.absent().toString());
    }

    @Test
    public void testToString_present() {
        assertEquals("Optional.of(training)", Optional.of("training").toString());
    }

    @Test
    public void testPresentInstances_allPresent() {
        List<Optional<String>> optionals =
                Arrays.asList(Optional.of("a"), Optional.of("b"), Optional.of("c"));
        assertThat(Optional.presentInstances(optionals)).containsExactly("a", "b", "c");
    }

    @Test
    public void testPresentInstances_allAbsent() {
        List<Optional<Object>> optionals =
                Arrays.asList(Optional.absent(), Optional.absent());
        assertThat(Optional.presentInstances(optionals)).isEmpty();
    }

    @Test
    public void testPresentInstances_somePresent() {
        List<Optional<String>> optionals =
                Arrays.asList(Optional.of("a"), Optional.<String>absent(), Optional.of("c"));
        assertThat(Optional.presentInstances(optionals)).containsExactly("a", "c");
    }

    @Test
    public void testPresentInstances_callingIteratorTwice() {
        List<Optional<String>> optionals =
                Arrays.asList(Optional.of("a"), Optional.<String>absent(), Optional.of("c"));
        Iterable<String> onlyPresent = Optional.presentInstances(optionals);
        assertThat(onlyPresent).containsExactly("a", "c");
        assertThat(onlyPresent).containsExactly("a", "c");
    }

    @Test
    public void testPresentInstances_wildcards() {
        List<Optional<? extends Number>> optionals =
                Arrays.<Optional<? extends Number>>asList(Optional.<Double>absent(), Optional.of(2));
        Iterable<Number> onlyPresent = Optional.presentInstances(optionals);
        assertThat(onlyPresent).containsExactly(2);
    }

    @Test
    public void testSerialization() {
        SerializableTester.reserializeAndAssert(Optional.absent());
        SerializableTester.reserializeAndAssert(Optional.of("foo"));
    }

    @Test
    public void testFilter() {
        assertThat(Optional.of("").filter(NOT_EMPTY_PREDICATE)).isEqualTo(Optional.absent());
        assertThat(Optional.of("foo").filter(NOT_EMPTY_PREDICATE)).isEqualTo(Optional.of("foo"));
    }

    @Test(expected = NullPointerException.class)
    public void testFilterNPEFunction() {
        Optional.of("").filter(NULL_PREDICATE);
    }

    @Test
    public void testIfPresent() {
        Optional<String> absent = Optional.absent();
        Optional<String> fromNull = Optional.fromNullable(null);
        Consumer<String> alwaysFail = new Consumer<String>() {
            @Override
            public void accept(String input) {
                fail();
            }
        };

        // alwaysFail must never be called.
        absent.ifPresent(alwaysFail);
        fromNull.ifPresent(alwaysFail);
        final AtomicReference<String> reference = new AtomicReference<>();
        String foo = "foo";
        Optional.of(foo).ifPresent(new Consumer<String>() {
            @Override
            public void accept(String input) {
                reference.set(input);
            }
        });
        assertSame(foo, reference.get());
    }
}
