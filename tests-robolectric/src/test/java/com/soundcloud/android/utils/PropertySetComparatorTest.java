package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PropertySetComparatorTest {

    private static final Property<String> STRING_PROP = Property.of(PropertySetComparatorTest.class, String.class);

    private final PropertySetComparator<String> comparator = new PropertySetComparator<>(STRING_PROP);

    @Test
    public void shouldCompareTwoPropertySetsFromGivenProperty() {
        final PropertySet a = PropertySet.from(STRING_PROP.bind("a"));
        final PropertySet b = PropertySet.from(STRING_PROP.bind("b"));

        expect(comparator.compare(a, a)).toBe(0);
        expect(comparator.compare(a, b)).toBeLessThan(0);
        expect(comparator.compare(b, a)).toBeGreaterThan(0);
    }
}