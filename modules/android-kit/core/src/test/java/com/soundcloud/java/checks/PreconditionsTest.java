package com.soundcloud.java.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PreconditionsTest {

    private static final String NON_NULL_STRING = "foo";

    private static final Object IGNORE_ME = new Object() {
        @Override
        public String toString() {
            throw new AssertionError();
        }
    };

    private static void verifySimpleMessage(Exception e) {
        assertThat(e.getMessage()).isEqualTo("A message");
    }

    @Test
    public void testCheckArgument_simple_success() {
        Preconditions.checkArgument(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgument_simple_failure() {
        Preconditions.checkArgument(false);
    }

    @Test
    public void testCheckArgument_simpleMessage_success() {
        Preconditions.checkArgument(true, IGNORE_ME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckArgument_simpleMessage_failure() {
        Preconditions.checkArgument(false, new Message());
    }

    @Test
    public void testCheckArgument_nullMessage_failure() {
        try {
            Preconditions.checkArgument(false, null);
            fail("no exception thrown");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void testCheckState_simple_success() {
        Preconditions.checkState(true);
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckState_simple_failure() {
        Preconditions.checkState(false);
    }

    @Test
    public void testCheckState_simpleMessage_success() {
        Preconditions.checkState(true, IGNORE_ME);
    }

    @Test
    public void testCheckState_simpleMessage_failure() {
        try {
            Preconditions.checkState(false, new Message());
            fail("no exception thrown");
        } catch (IllegalStateException expected) {
            verifySimpleMessage(expected);
        }
    }

    @Test
    public void testCheckState_nullMessage_failure() {
        try {
            Preconditions.checkState(false, null);
            fail("no exception thrown");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void testCheckNotNull_simple_success() {
        String result = Preconditions.checkNotNull(NON_NULL_STRING);
        assertThat(NON_NULL_STRING).isSameAs(result);
    }

    @Test(expected = NullPointerException.class)
    public void testCheckNotNull_simple_failure() {
        Preconditions.checkNotNull(null);
    }

    @Test
    public void testCheckNotNull_simpleMessage_success() {
        String result = Preconditions.checkNotNull(NON_NULL_STRING, IGNORE_ME);
        assertThat(NON_NULL_STRING).isSameAs(result);
    }

    @Test
    public void testCheckNotNull_simpleMessage_failure() {
        try {
            Preconditions.checkNotNull(null, new Message());
            fail("no exception thrown");
        } catch (NullPointerException expected) {
            verifySimpleMessage(expected);
        }
    }

    @Test
    public void testCheckElementIndex_ok() {
        assertThat(Preconditions.checkElementIndex(0, 1)).isEqualTo(0);
        assertThat(Preconditions.checkElementIndex(0, 2)).isEqualTo(0);
        assertThat(Preconditions.checkElementIndex(1, 2)).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckElementIndex_badSize() {
        Preconditions.checkElementIndex(1, -1);
    }

    @Test
    public void testCheckElementIndex_negative() {
        try {
            Preconditions.checkElementIndex(-1, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (-1) must not be negative");
        }
    }

    @Test
    public void testCheckElementIndex_tooHigh() {
        try {
            Preconditions.checkElementIndex(1, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (1) must be less than size (1)");
        }
    }

    @Test
    public void testCheckElementIndex_withDesc_negative() {
        try {
            Preconditions.checkElementIndex(-1, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (-1) must not be negative");
        }
    }

    @Test
    public void testCheckElementIndex_withDesc_tooHigh() {
        try {
            Preconditions.checkElementIndex(1, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (1) must be less than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndex_ok() {
        assertThat(Preconditions.checkPositionIndex(0, 0)).isEqualTo(0);
        assertThat(Preconditions.checkPositionIndex(0, 1)).isEqualTo(0);
        assertThat(Preconditions.checkPositionIndex(1, 1)).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPositionIndex_badSize() {
        Preconditions.checkPositionIndex(1, -1);
    }

    @Test
    public void testCheckPositionIndex_negative() {
        try {
            Preconditions.checkPositionIndex(-1, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (-1) must not be negative");
        }
    }

    @Test
    public void testCheckPositionIndex_tooHigh() {
        try {
            Preconditions.checkPositionIndex(2, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (2) must not be greater than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndex_withDesc_negative() {
        try {
            Preconditions.checkPositionIndex(-1, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (-1) must not be negative");
        }
    }

    @Test
    public void testCheckPositionIndex_withDesc_tooHigh() {
        try {
            Preconditions.checkPositionIndex(2, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (2) must not be greater than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndexes_ok() {
        Preconditions.checkPositionIndexes(0, 0, 0);
        Preconditions.checkPositionIndexes(0, 0, 1);
        Preconditions.checkPositionIndexes(0, 1, 1);
        Preconditions.checkPositionIndexes(1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPositionIndexes_badSize() {
        Preconditions.checkPositionIndexes(1, 1, -1);
    }

    @Test
    public void testCheckPositionIndex_startNegative() {
        try {
            Preconditions.checkPositionIndexes(-1, 1, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("start index (-1) must not be negative");
        }
    }

    @Test
    public void testCheckPositionIndexes_endTooHigh() {
        try {
            Preconditions.checkPositionIndexes(0, 2, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("end index (2) must not be greater than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndexes_reversed() {
        try {
            Preconditions.checkPositionIndexes(1, 0, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("end index (0) must not be less than start index (1)");
        }
    }

    @Test
    public void testFormat() {
        assertThat(Preconditions.format("%s")).isEqualTo("%s");
        assertThat(Preconditions.format("%s", 5)).isEqualTo("5");
        assertThat(Preconditions.format("foo", 5)).isEqualTo("foo [5]");
        assertThat(Preconditions.format("foo", 5, 6, 7)).isEqualTo("foo [5, 6, 7]");
        assertThat(Preconditions.format("%s %s %s", "%s", 1, 2)).isEqualTo("%s 1 2");
        assertThat(Preconditions.format("", 5, 6)).isEqualTo(" [5, 6]");
        assertThat(Preconditions.format("%s%s%s", 1, 2, 3)).isEqualTo("123");
        assertThat(Preconditions.format("%s%s%s", 1)).isEqualTo("1%s%s");
        assertThat(Preconditions.format("%s + 6 = 11", 5)).isEqualTo("5 + 6 = 11");
        assertThat(Preconditions.format("5 + %s = 11", 6)).isEqualTo("5 + 6 = 11");
        assertThat(Preconditions.format("5 + 6 = %s", 11)).isEqualTo("5 + 6 = 11");
        assertThat(Preconditions.format("%s + %s = %s", 5, 6, 11)).isEqualTo("5 + 6 = 11");
        assertThat(Preconditions.format("%s", null, null, null)).isEqualTo("null [null, null]");
        assertThat(Preconditions.format(null, 5, 6)).isEqualTo("null [5, 6]");
    }

    private static class Message {
        boolean invoked;

        @Override
        public String toString() {
            assertThat(invoked).isFalse();
            invoked = true;
            return "A message";
        }
    }
}