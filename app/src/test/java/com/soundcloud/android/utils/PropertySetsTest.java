package com.soundcloud.android.utils;

import static com.soundcloud.android.utils.Urns.playlistPredicate;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import android.support.annotation.NonNull;

import java.util.List;

// uses AndroidUnitTest because PropertySet uses Android framework classes
public class PropertySetsTest extends AndroidUnitTest {
    @Test
    public void shouldExtractAllIdsFromPropertySets() throws Exception {
        List<Long> ids = PropertySets.extractIds(samplePropertySets(), Optional.absent());

        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void shouldExtractPlaylistIdsFromPropertySets() throws Exception {
        List<Long> ids = PropertySets.extractIds(samplePropertySets(), Optional.of(playlistPredicate()));

        assertThat(ids).containsExactly(2L);
    }

    @NonNull
    private List<Urn> samplePropertySets() {
        return asList(
                Urn.forTrack(1L),
                Urn.forPlaylist(2L),
                Urn.forUser(3L)
        );
    }
}
