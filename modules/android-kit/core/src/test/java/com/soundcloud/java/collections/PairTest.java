package com.soundcloud.java.collections;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.java.test.EqualsTester;
import org.junit.Test;

public class PairTest {

    @Test
    public void shouldCreateNewPairOfObjects() {
        final Integer a = 1;
        final String b = "b";
        final Pair<Integer, String> pair = Pair.of(a, b);
        assertThat(pair.first()).isSameAs(a);
        assertThat(pair.second()).isSameAs(b);

    }

    @Test(expected = NullPointerException.class)
    public void shouldDisallowNullForFirstElement() {
        Pair.of(null, 1);
    }

    @Test(expected = NullPointerException.class)
    public void shouldDisallowNullForSecondElement() {
        Pair.of(1, null);
    }

    @Test
    public void shouldImplementEqualsAndHashCode() {
        new EqualsTester()
                .addEqualityGroup(Pair.of(1, "a"), Pair.of(1, "a"))
                .addEqualityGroup(Pair.of("a", 1))
                .addEqualityGroup(Pair.of(1, 1))
                .testEquals();
    }
}
