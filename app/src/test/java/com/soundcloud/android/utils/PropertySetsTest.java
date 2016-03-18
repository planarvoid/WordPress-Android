package com.soundcloud.android.utils;

import static com.soundcloud.android.utils.Urns.playlistPredicate;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import android.support.annotation.NonNull;

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

    @Test
    public void shouldExtractAllIdsFromPropertySets() throws Exception {
        List<Long> ids = PropertySets.extractIds(samplePropertySets(), Optional.<Predicate<Urn>>absent());

        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void shouldExtractPlaylistIdsFromPropertySets() throws Exception {
        List<Long> ids = PropertySets.extractIds(samplePropertySets(), Optional.of(playlistPredicate()));

        assertThat(ids).containsExactly(2L);
    }

    @NonNull
    private List<PropertySet> samplePropertySets() {
        return asList(
                PropertySet.from(EntityProperty.URN.bind(Urn.forTrack(1L))),
                PropertySet.from(EntityProperty.URN.bind(Urn.forPlaylist(2L))),
                PropertySet.from(EntityProperty.URN.bind(Urn.forUser(3L)))
        );
    }
}
