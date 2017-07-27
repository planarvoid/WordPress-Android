package com.soundcloud.java.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

public class IndexHelperTest {

    @Test
    public void testCheckElementIndex_ok() {
        assertThat(IndexHelper.checkElementIndex(0, 1, "index")).isEqualTo(0);
        assertThat(IndexHelper.checkElementIndex(0, 2, "index")).isEqualTo(0);
        assertThat(IndexHelper.checkElementIndex(1, 2, "index")).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckElementIndex_badSize() {
        IndexHelper.checkElementIndex(1, -1, "index");
    }

    @Test
    public void testCheckElementIndex_negative() {
        try {
            IndexHelper.checkElementIndex(-1, 1, "index");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (-1) must not be negative");
        }
    }

    @Test
    public void testCheckElementIndex_tooHigh() {
        try {
            IndexHelper.checkElementIndex(1, 1, "index");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (1) must be less than size (1)");
        }
    }

    @Test
    public void testCheckElementIndex_withDesc_negative() {
        try {
            IndexHelper.checkElementIndex(-1, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (-1) must not be negative");
        }
    }

    @Test
    public void testCheckElementIndex_withDesc_tooHigh() {
        try {
            IndexHelper.checkElementIndex(1, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (1) must be less than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndex_ok() {
        assertThat(IndexHelper.checkPositionIndex(0, 0)).isEqualTo(0);
        assertThat(IndexHelper.checkPositionIndex(0, 1)).isEqualTo(0);
        assertThat(IndexHelper.checkPositionIndex(1, 1)).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPositionIndex_badSize() {
        IndexHelper.checkPositionIndex(1, -1);
    }

    @Test
    public void testCheckPositionIndex_negative() {
        try {
            IndexHelper.checkPositionIndex(-1, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (-1) must not be negative");
        }
    }

    @Test
    public void testCheckPositionIndex_tooHigh() {
        try {
            IndexHelper.checkPositionIndex(2, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("index (2) must not be greater than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndex_withDesc_negative() {
        try {
            IndexHelper.checkPositionIndex(-1, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (-1) must not be negative");
        }
    }

    @Test
    public void testCheckPositionIndex_withDesc_tooHigh() {
        try {
            IndexHelper.checkPositionIndex(2, 1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("foo (2) must not be greater than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndexes_ok() {
        IndexHelper.checkPositionIndexes(0, 0, 0);
        IndexHelper.checkPositionIndexes(0, 0, 1);
        IndexHelper.checkPositionIndexes(0, 1, 1);
        IndexHelper.checkPositionIndexes(1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPositionIndexes_badSize() {
        IndexHelper.checkPositionIndexes(1, 1, -1);
    }

    @Test
    public void testCheckPositionIndex_startNegative() {
        try {
            IndexHelper.checkPositionIndexes(-1, 1, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("start index (-1) must not be negative");
        }
    }

    @Test
    public void testCheckPositionIndexes_endTooHigh() {
        try {
            IndexHelper.checkPositionIndexes(0, 2, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("end index (2) must not be greater than size (1)");
        }
    }

    @Test
    public void testCheckPositionIndexes_reversed() {
        try {
            IndexHelper.checkPositionIndexes(1, 0, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertThat(expected.getMessage()).isEqualTo("end index (0) must not be less than start index (1)");
        }
    }

    @Test
    public void testFormat() {
        assertThat(IndexHelper.format("%s")).isEqualTo("%s");
        assertThat(IndexHelper.format("%s", 5)).isEqualTo("5");
        assertThat(IndexHelper.format("foo", 5)).isEqualTo("foo [5]");
        assertThat(IndexHelper.format("foo", 5, 6, 7)).isEqualTo("foo [5, 6, 7]");
        assertThat(IndexHelper.format("%s %s %s", "%s", 1, 2)).isEqualTo("%s 1 2");
        assertThat(IndexHelper.format("", 5, 6)).isEqualTo(" [5, 6]");
        assertThat(IndexHelper.format("%s%s%s", 1, 2, 3)).isEqualTo("123");
        assertThat(IndexHelper.format("%s%s%s", 1)).isEqualTo("1%s%s");
        assertThat(IndexHelper.format("%s + 6 = 11", 5)).isEqualTo("5 + 6 = 11");
        assertThat(IndexHelper.format("5 + %s = 11", 6)).isEqualTo("5 + 6 = 11");
        assertThat(IndexHelper.format("5 + 6 = %s", 11)).isEqualTo("5 + 6 = 11");
        assertThat(IndexHelper.format("%s + %s = %s", 5, 6, 11)).isEqualTo("5 + 6 = 11");
        assertThat(IndexHelper.format("%s", null, null, null)).isEqualTo("null [null, null]");
        assertThat(IndexHelper.format(null, 5, 6)).isEqualTo("null [5, 6]");
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
