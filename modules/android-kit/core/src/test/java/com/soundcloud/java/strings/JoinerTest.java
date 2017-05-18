package com.soundcloud.java.strings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.soundcloud.java.strings.Joiner;
import com.soundcloud.java.strings.Strings;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class JoinerTest {

    private static final Joiner J = Strings.joinOn("-");

    private static final Iterable<Integer> ITERABLE_ = Arrays.asList();
    private static final Iterable<Integer> ITERABLE_1 = Arrays.asList(1);
    private static final Iterable<Integer> ITERABLE_12 = Arrays.asList(1, 2);
    private static final Iterable<Integer> ITERABLE_123 = Arrays.asList(1, 2, 3);
    private static final Iterable<Integer> ITERABLE_NULL = Arrays.asList((Integer) null);
    private static final Iterable<Integer> ITERABLE_NULL_NULL
            = Arrays.asList((Integer) null, null);
    private static final Iterable<Integer> ITERABLE_NULL_1 = Arrays.asList(null, 1);
    private static final Iterable<Integer> ITERABLE_1_NULL = Arrays.asList(1, null);
    private static final Iterable<Integer> ITERABLE_1_NULL_2 = Arrays.asList(1, null, 2);
    private static final Iterable<Integer> ITERABLE_FOUR_NULLS
            = Arrays.asList((Integer) null, null, null, null);

    private static final Appendable NASTY_APPENDABLE = new Appendable() {
        @Override
        public Appendable append(CharSequence csq) throws IOException {
            throw new IOException();
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
            throw new IOException();
        }

        @Override
        public Appendable append(char c) throws IOException {
            throw new IOException();
        }
    };

    @Test
    public void testNoSpecialNullBehavior() {
        checkNoOutput(J, ITERABLE_);
        checkResult(J, ITERABLE_1, "1");
        checkResult(J, ITERABLE_12, "1-2");
        checkResult(J, ITERABLE_123, "1-2-3");

        try {
            J.join(ITERABLE_NULL);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            J.join(ITERABLE_1_NULL_2);
            fail();
        } catch (NullPointerException ignored) {
        }

        try {
            J.join(ITERABLE_NULL.iterator());
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            J.join(ITERABLE_1_NULL_2.iterator());
            fail();
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testOnCharOverride() {
        Joiner onChar = Strings.joinOn('-');
        checkNoOutput(onChar, ITERABLE_);
        checkResult(onChar, ITERABLE_1, "1");
        checkResult(onChar, ITERABLE_12, "1-2");
        checkResult(onChar, ITERABLE_123, "1-2-3");
    }

    @Test
    public void testSkipNulls() {
        Joiner skipNulls = J.skipNulls();
        checkNoOutput(skipNulls, ITERABLE_);
        checkNoOutput(skipNulls, ITERABLE_NULL);
        checkNoOutput(skipNulls, ITERABLE_NULL_NULL);
        checkNoOutput(skipNulls, ITERABLE_FOUR_NULLS);
        checkResult(skipNulls, ITERABLE_1, "1");
        checkResult(skipNulls, ITERABLE_12, "1-2");
        checkResult(skipNulls, ITERABLE_123, "1-2-3");
        checkResult(skipNulls, ITERABLE_NULL_1, "1");
        checkResult(skipNulls, ITERABLE_1_NULL, "1");
        checkResult(skipNulls, ITERABLE_1_NULL_2, "1-2");
    }

    @Test
    public void testUseForNull() {
        Joiner zeroForNull = J.useForNull("0");
        checkNoOutput(zeroForNull, ITERABLE_);
        checkResult(zeroForNull, ITERABLE_1, "1");
        checkResult(zeroForNull, ITERABLE_12, "1-2");
        checkResult(zeroForNull, ITERABLE_123, "1-2-3");
        checkResult(zeroForNull, ITERABLE_NULL, "0");
        checkResult(zeroForNull, ITERABLE_NULL_NULL, "0-0");
        checkResult(zeroForNull, ITERABLE_NULL_1, "0-1");
        checkResult(zeroForNull, ITERABLE_1_NULL, "1-0");
        checkResult(zeroForNull, ITERABLE_1_NULL_2, "1-0-2");
        checkResult(zeroForNull, ITERABLE_FOUR_NULLS, "0-0-0-0");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_useForNull_skipNulls() {
        Strings.joinOn("x").useForNull("y").skipNulls();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_skipNulls_useForNull() {
        Strings.joinOn("x").skipNulls().useForNull("y");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_useForNull_twice() {
        Strings.joinOn("x").useForNull("y").useForNull("y");
    }

    @Test
    public void testDontConvertCharSequenceToString() {
        assertThat(Strings.joinOn(",").join(
                new DontStringMeBro(), new DontStringMeBro())).isEqualTo("foo,foo");
        assertThat(Strings.joinOn(",").useForNull("bar").join(
                new DontStringMeBro(), null, new DontStringMeBro())).isEqualTo("foo,bar,foo");
    }

    private static void checkNoOutput(Joiner joiner, Iterable<Integer> set) {
        assertThat(joiner.join(set)).isEmpty();
        assertThat(joiner.join(set.iterator())).isEmpty();

        Object[] array = Lists.newArrayList(set).toArray(new Integer[0]);
        assertThat(joiner.join(array)).isEmpty();

        StringBuilder sb1FromIterable = new StringBuilder();
        assertThat(joiner.appendTo(sb1FromIterable, set)).isSameAs(sb1FromIterable);
        assertThat(sb1FromIterable).isEmpty();

        StringBuilder sb1FromIterator = new StringBuilder();
        assertThat(joiner.appendTo(sb1FromIterator, set)).isSameAs(sb1FromIterator);
        assertThat(sb1FromIterator).isEmpty();

        StringBuilder sb2 = new StringBuilder();
        assertThat(joiner.appendTo(sb2, array)).isSameAs(sb2);
        assertThat(sb2).isEmpty();

        try {
            joiner.appendTo(NASTY_APPENDABLE, set);
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        try {
            joiner.appendTo(NASTY_APPENDABLE, set.iterator());
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        try {
            joiner.appendTo(NASTY_APPENDABLE, array);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void checkResult(Joiner joiner, Iterable<Integer> parts, String expected) {
        assertThat(joiner.join(parts)).isEqualTo(expected);
        assertThat(joiner.join(parts.iterator())).isEqualTo(expected);

        StringBuilder sb1FromIterable = new StringBuilder().append('x');
        joiner.appendTo(sb1FromIterable, parts);
        assertThat(sb1FromIterable.toString()).isEqualTo("x" + expected);

        StringBuilder sb1FromIterator = new StringBuilder().append('x');
        joiner.appendTo(sb1FromIterator, parts.iterator());
        assertThat(sb1FromIterator.toString()).isEqualTo("x" + expected);

        Integer[] partsArray = Lists.newArrayList(parts).toArray(new Integer[0]);
        assertThat(joiner.join(partsArray)).isEqualTo(expected);

        StringBuilder sb2 = new StringBuilder().append('x');
        joiner.appendTo(sb2, partsArray);
        assertThat(sb2.toString()).isEqualTo("x" + expected);

        int num = partsArray.length - 2;
        if (num >= 0) {
            Object[] rest = new Integer[num];
            for (int i = 0; i < num; i++) {
                rest[i] = partsArray[i + 2];
            }

            assertThat(joiner.join(partsArray[0], partsArray[1], rest)).isEqualTo(expected);

            StringBuilder sb3 = new StringBuilder().append('x');
            joiner.appendTo(sb3, partsArray[0], partsArray[1], rest);
            assertThat(sb3.toString()).isEqualTo("x" + expected);
        }
    }

    private static class DontStringMeBro implements CharSequence {
        @Override
        public int length() {
            return 3;
        }

        @Override
        public char charAt(int index) {
            return "foo".charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return "foo".subSequence(start, end);
        }

        @Override
        public String toString() {
            throw new AssertionError("shouldn't be invoked");
        }
    }

}