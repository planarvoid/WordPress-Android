package com.soundcloud.java.strings;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

public class StringsTest {

    @Test
    public void nullToEmpty() {
        assertThat(Strings.nullToEmpty(null)).isEqualTo("");
        assertThat(Strings.nullToEmpty("")).isEqualTo("");
        assertThat(Strings.nullToEmpty("a")).isEqualTo("a");
    }

    @Test
    public void isNullOrEmpty() {
        assertThat(Strings.isNullOrEmpty(null)).isTrue();
        assertThat(Strings.isNullOrEmpty("")).isTrue();
        assertThat(Strings.isNullOrEmpty("a")).isFalse();
    }

    @Test
    public void isBlankBehavesLikeIsNullOrEmptyForNonWhitespaceStrings() {
        assertThat(Strings.isBlank(null)).isTrue();
        assertThat(Strings.isBlank("")).isTrue();
        assertThat(Strings.isBlank("a")).isFalse();
    }

    @Test
    public void isBlankReturnsTrueIfStringOnlyContainsWhitespace() {
        assertThat(Strings.isBlank("   ")).isTrue();
        assertThat(Strings.isBlank("\t")).isTrue();
        assertThat(Strings.isBlank("\n")).isTrue();
    }

    @Test
    public void isNotBlank() {
        assertThat(Strings.isNotBlank("   ")).isFalse();
        assertThat(Strings.isNotBlank("\t")).isFalse();
        assertThat(Strings.isNotBlank("\n")).isFalse();
    }

    @Test
    public void safeToString() {
        assertThat(Strings.safeToString(1)).isEqualTo("1");
        assertThat(Strings.safeToString(null)).isEqualTo("");
    }

    @Test
    public void toHexString() {
        assertThat(Strings.toHexString(new byte[]{0, 12, 32, 0, 16})).isEqualTo("000c200010");
    }
}
