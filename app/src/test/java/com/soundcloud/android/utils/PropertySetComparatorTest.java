package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

public class PropertySetComparatorTest extends AndroidUnitTest {

    private static final Property<String> STRING_PROP = Property.of(PropertySetComparatorTest.class, String.class);

    @Test
    public void shouldCompareTwoPropertySetsFromGivenPropertyInAscendingOrder() {
        PropertySetComparator<String> comparator = new PropertySetComparator<>(STRING_PROP);
        PropertySet a = PropertySet.from(STRING_PROP.bind("a"));
        PropertySet b = PropertySet.from(STRING_PROP.bind("b"));

        assertThat(comparator.compare(a, a)).isEqualTo(0);
        assertThat(comparator.compare(a, b)).isLessThan(0);
        assertThat(comparator.compare(b, a)).isGreaterThan(0);
    }

    @Test
    public void shouldCompareTwoPropertySetsFromGivenPropertyInDescendingOrder() {
        PropertySetComparator<String> comparator = new PropertySetComparator<>(STRING_PROP, PropertySetComparator.DESC);
        PropertySet a = PropertySet.from(STRING_PROP.bind("a"));
        PropertySet b = PropertySet.from(STRING_PROP.bind("b"));

        assertThat(comparator.compare(a, a)).isEqualTo(0);
        assertThat(comparator.compare(a, b)).isGreaterThan(0);
        assertThat(comparator.compare(b, a)).isLessThan(0);
    }
}
