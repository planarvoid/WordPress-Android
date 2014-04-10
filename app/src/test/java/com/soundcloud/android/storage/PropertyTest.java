package com.soundcloud.android.storage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PropertyTest {

    @Test
    public void aPropertyIsBoundToAJavaType() {
        Property<String> property = Property.of(String.class);
        assertThat(property.type, sameInstance(String.class));
    }

    @Test
    public void aPropertyCanBindAValueOfTheWrappedType() {
        Property<String> property = Property.of(String.class);
        Property.Binding<String> binding = property.bind("a string");
        assertThat(binding.property, sameInstance(property));
        assertThat(binding.value, equalTo("a string"));
    }

    @Test
    public void aPropertyDefinesEqualityOverObjectIdentity() {
        Property<String> property1 = Property.of(String.class);
        Property<String> property2 = Property.of(String.class);
        assertThat(property1, not(equalTo(property2)));
        assertThat(property1.hashCode(), not(equalTo(property2.hashCode())));
    }

    @Test
    public void twoPropertyBindingsAreEqualIfPropertyAndValueEqual() {
        Property<String> property = Property.of(String.class);
        assertThat(property.bind("test"), equalTo(property.bind("test")));
        assertThat(property.bind("test").hashCode(), equalTo(property.bind("test").hashCode()));
    }

    @Test
    public void twoPropertyBindingsAreNotEqualIfPropertyDiffers() {
        Property<String> property1 = Property.of(String.class);
        Property<String> property2 = Property.of(String.class);
        assertThat(property1.bind("test"), not(equalTo(property2.bind("test"))));
        assertThat(property1.bind("test").hashCode(), not(equalTo(property2.bind("test").hashCode())));
    }

    @Test
    public void twoPropertyBindingsAreNotEqualIfPropertyMatchesButValueDiffers() {
        Property<String> property = Property.of(String.class);
        assertThat(property.bind("foo"), not(equalTo(property.bind("bar"))));
        assertThat(property.bind("foo").hashCode(), not(equalTo(property.bind("bar").hashCode())));
    }

    @Test
    public void aPropertyDefinesAProperToStringImplementation() {
        Property<String> property = Property.of(String.class);
        assertThat(property.bind("test").toString(),
                equalTo("Property.of(String.class)@" + property.hashCode() + "=>[test]"));
    }
}
