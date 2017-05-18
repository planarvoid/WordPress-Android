package com.soundcloud.java.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.MapEntry;
import org.junit.Test;

public class TestMapsTest {

    @Test
    public void shouldCreateMapFromKeyValuePairs() {
        assertThat(TestMaps.from("one", 1, "two", 2)).containsOnly(
                MapEntry.entry("one", 1),
                MapEntry.entry("two", 2)
        );
    }

    @Test
    public void shouldCreateEmptyMap() {
        assertThat(TestMaps.from()).isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotSupportNullKeys() {
        TestMaps.from(null, 1);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotSupportNullValues() {
        TestMaps.from(1, null);
    }

    @Test(expected = AssertionError.class)
    public void shouldThrowIfNumberOfArgumentsIsOdd() {
        TestMaps.from(1);
    }

    @Test(expected = ClassCastException.class)
    public void shouldThrowIfTypesDontMatch() {
        TestMaps.from("one", 1, 2, "two");
    }
}