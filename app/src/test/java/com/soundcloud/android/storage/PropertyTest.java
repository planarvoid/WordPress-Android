package com.soundcloud.android.storage;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PropertyTest {

    @Test
    public void aPropertyIsBoundToAJavaType() {
        final Property<String> property = Property.of(String.class);
        assertThat(property.type, sameInstance(String.class));
    }

    @Test
    public void aPropertyCanBindAValueOfTheWrappedType() {
        final Property<String> property = Property.of(String.class);
        final Property.Binding<String> binding = property.bind("a string");
        assertThat(binding.property, sameInstance(property));
        assertThat(binding.value, equalTo("a string"));
    }
}
