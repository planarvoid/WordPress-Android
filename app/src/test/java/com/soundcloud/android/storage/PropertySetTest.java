package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

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

    @Test
    public void aPropertySetCanBeConstructedFromAListOfBindings() {
        PropertySet propertySet = PropertySet.from(TEST_PROP_STRING.bind("Test"), TEST_PROP_INT.bind(1));
        expect(propertySet.get(TEST_PROP_STRING)).toEqual("Test");
        expect(propertySet.get(TEST_PROP_INT)).toEqual(1);
    }

    @Test
    public void twoPropertySetsAreEqualIfTheirContentsAreEqual() {
        PropertySet propertySet1 = PropertySet.from(TEST_PROP_STRING.bind("Test"), TEST_PROP_INT.bind(1));
        PropertySet propertySet2 = PropertySet.from(TEST_PROP_STRING.bind("Test"), TEST_PROP_INT.bind(1));
        assertThat(propertySet1, equalTo(propertySet2));
        assertThat(propertySet1.hashCode(), equalTo(propertySet2.hashCode()));
    }

    @Test
    public void twoPropertySetsAreInequalIfTheirPropertiesMatchButValuesDiffer() {
        PropertySet propertySet1 = PropertySet.from(TEST_PROP_STRING.bind("Foo"), TEST_PROP_INT.bind(1));
        PropertySet propertySet2 = PropertySet.from(TEST_PROP_STRING.bind("Bar"), TEST_PROP_INT.bind(1));
        assertThat(propertySet1, not(equalTo(propertySet2)));
        assertThat(propertySet1.hashCode(), not(equalTo(propertySet2.hashCode())));
    }

    @Test
    public void twoPropertySetsAreInequalIfTheirPropertiesDiffer() {
        PropertySet propertySet1 = PropertySet.from(Property.of(String.class).bind("Foo"));
        PropertySet propertySet2 = PropertySet.from(Property.of(String.class).bind("Foo"));
        assertThat(propertySet1, not(equalTo(propertySet2)));
        assertThat(propertySet1.hashCode(), not(equalTo(propertySet2.hashCode())));
    }

    @Test
    public void aPropertySetHasAProperToStringImplementation() {
        final Property.Binding<String> binding1 = TEST_PROP_STRING.bind("Test");
        final Property.Binding<Integer> binding2 = TEST_PROP_INT.bind(1);
        PropertySet propertySet = PropertySet.from(binding1, binding2);
        assertThat(propertySet.toString(), startsWith("PropertySet{"));
        assertThat(propertySet.toString(), endsWith("}"));
        assertThat(propertySet.toString(), containsString(binding1.toString()));
        assertThat(propertySet.toString(), containsString(binding2.toString()));
    }
}
