package com.soundcloud.android.utils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.List;

// uses AndroidUnitTest because PropertySet uses Android framework classes
public class PropertySetsTest extends AndroidUnitTest {

    @Test
    public void shouldConvertPropertySetSourcesToPropertySets() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        List<PropertySet> propertySets = PropertySets.toPropertySets(track);
        assertThat(propertySets).containsExactly(track.toPropertySet());
    }

    @Test
    public void shouldExtractUrnsFromPropertySets() {
        List<PropertySet> source = asList(
                PropertySet.from(EntityProperty.URN.bind(new Urn("a"))),
                PropertySet.from(EntityProperty.URN.bind(new Urn("b")))
        );
        assertThat(PropertySets.extractUrns(source)).containsExactly(
                new Urn("a"), new Urn("b")
        );
    }
}
