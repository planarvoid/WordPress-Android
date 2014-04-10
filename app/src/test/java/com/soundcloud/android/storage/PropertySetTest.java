package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("ConstantConditions")
@RunWith(SoundCloudTestRunner.class)
public class PropertySetTest {

    private static final Property<String> TEST_PROP_STRING = Property.of(String.class);
    private static final Property<Integer> TEST_PROP_INT = Property.of(Integer.class);

    @Test
    public void aPropertySetCanAddAndGetPropertiesInATypeSafeWay() {
        PropertySet propertySet = PropertySet.create(2);
        propertySet.add(TEST_PROP_STRING, "Test");
        propertySet.add(TEST_PROP_INT, 1);

        expect(propertySet.get(TEST_PROP_STRING)).toEqual("Test");
        expect(propertySet.get(TEST_PROP_INT)).toEqual(1);
    }

    @Test
    public void aPropertySetReturnsNullWhenGettingPropertyThatDoesNotExist() {
        PropertySet set = PropertySet.create(2);
        expect(set.get(TEST_PROP_STRING)).toBeNull();
    }
}
