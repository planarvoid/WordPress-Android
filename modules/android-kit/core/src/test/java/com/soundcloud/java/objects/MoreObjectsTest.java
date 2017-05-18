package com.soundcloud.java.objects;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MoreObjectsTest {

    @Test
    public void testEqual() throws Exception {
        assertThat(MoreObjects.equal(1, 1)).isTrue();
        assertThat(MoreObjects.equal(null, null)).isTrue();

        // test distinct string objects
        String s1 = "foobar";
        String s2 = new String(s1);
        assertThat(MoreObjects.equal(s1, s2)).isTrue();

        assertThat(MoreObjects.equal(s1, null)).isFalse();
        assertThat(MoreObjects.equal(null, s1)).isFalse();
        assertThat(MoreObjects.equal("foo", "bar")).isFalse();
        assertThat(MoreObjects.equal("1", 1)).isFalse();
    }

    @Test
    public void testHashCode() throws Exception {
        int h1 = MoreObjects.hashCode(1, "two", 3.0);
        int h2 = MoreObjects.hashCode(
                Integer.valueOf(1), new String("two"), Double.valueOf(3.0));
        // repeatable
        assertThat(h1).isEqualTo(h2);

        // These don't strictly need to be true, but they're nice properties.
        assertThat(MoreObjects.hashCode(1, 2, null)).isNotEqualTo(MoreObjects.hashCode(1, 2));
        assertThat(MoreObjects.hashCode(1, 2, null)).isNotEqualTo(MoreObjects.hashCode(1, null, 2));
        assertThat(MoreObjects.hashCode(1, null, 2)).isNotEqualTo(MoreObjects.hashCode(1, 2));
        assertThat(MoreObjects.hashCode(1, 2, 3)).isNotEqualTo(MoreObjects.hashCode(3, 2, 1));
        assertThat(MoreObjects.hashCode(1, 2, 3)).isNotEqualTo(MoreObjects.hashCode(2, 3, 1));
    }

    @Test
    public void testFirstNonNull_withNonNull() throws Exception {
        String s1 = "foo";
        String s2 = MoreObjects.firstNonNull(s1, "bar");
        assertThat(s1).isSameAs(s2);

        Long n1 = Long.valueOf(42);
        Long n2 = MoreObjects.firstNonNull(null, n1);
        assertThat(n1).isSameAs(n2);
    }

    @Test(expected = NullPointerException.class)
    public void testFirstNonNull_throwsNullPointerException() throws Exception {
        MoreObjects.firstNonNull(null, null);
    }

}
