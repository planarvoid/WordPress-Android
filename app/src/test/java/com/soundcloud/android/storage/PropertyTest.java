package com.soundcloud.android.storage;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

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
    public void twoPropertiesAreEqualIfTheirInternalIdMatches() {
        Property<String> property1 = Property.of(String.class);
        Property<String> property2 = Property.of(String.class);

        assertThat(property1, equalTo(property1));
        assertThat(property1.hashCode(), equalTo(property1.hashCode()));

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

    @Test
    public void aPropertyIsParcelable() {
        Parcel parcel = Parcel.obtain();
        Property property = Property.of(String.class);
        property.writeToParcel(parcel, 0);

        Property restored = Property.CREATOR.createFromParcel(parcel);
        assertThat(restored, equalTo(property));
    }

    @Test
    public void aPropertyBindingIsParcelable() {
        Parcel parcel = Parcel.obtain();
        Property.Binding binding = Property.of(String.class).bind("test");
        binding.writeToParcel(parcel, 0);

        Property.Binding restored = Property.Binding.CREATOR.createFromParcel(parcel);
        assertThat(restored.property, equalTo(binding.property));
        assertThat(restored.value, equalTo(binding.value));
    }
}
