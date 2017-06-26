package com.soundcloud.android.discovery.systemplaylist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.newforyou.NewForYou;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(JUnit4.class)
public class SystemPlaylistMapperTest {

    private static final Urn URN = Urn.forSystemPlaylist("123");
    private static final Optional<Integer> TRACK_COUNT = Optional.of(10);
    private static final Optional<Date> LAST_UPDATED = Optional.of(new Date(123456L));
    private static final Optional<String> TITLE = Optional.of("Title 1");
    private static final Optional<String> DESCRIPTION = Optional.of("Desc");
    private static final Optional<String> ARTWORK_URL_TEMPLATE = Optional.of("hht://test.com");
    private static final Optional<String> TRACKING_FEATURE_NAME = Optional.of("The Upload");
    private static final List<ApiTrack> API_TRACKS = ModelFixtures.apiTracks(TRACK_COUNT.get());
    private static final ModelCollection<ApiTrack> API_TRACK_MODEL_COLLECTION = new ModelCollection<>(API_TRACKS);
    private static final List<Track> TRACKS = ModelFixtures.tracks(TRACK_COUNT.get());
    private static final Optional<String> NEW_FOR_YOU_TITLE = Optional.of("The Upload");
    private static final Optional<String> NEW_FOR_YOU_DESCRIPTION = Optional.of("Some awesome tracks");

    @Test
    public void map() throws Exception {
        final ApiSystemPlaylist input = ApiSystemPlaylist.create(URN, TRACK_COUNT, LAST_UPDATED, TITLE, DESCRIPTION, ARTWORK_URL_TEMPLATE, TRACKING_FEATURE_NAME, API_TRACK_MODEL_COLLECTION);

        final SystemPlaylist result = SystemPlaylistMapper.map(input);

        assertThat(result.urn()).isEqualTo(URN);
        assertThat(result.queryUrn().isPresent()).isFalse();
        assertThat(result.lastUpdated()).isEqualTo(LAST_UPDATED);
        assertThat(result.title()).isEqualTo(TITLE);
        assertThat(result.description()).isEqualTo(DESCRIPTION);
        assertThat(result.artworkUrlTemplate()).isEqualTo(ARTWORK_URL_TEMPLATE);
        assertThat(result.tracks()).hasSize(TRACK_COUNT.get());
        assertThat(result.tracks()).containsExactlyElementsOf(Lists.transform(API_TRACKS, Track::from));
    }

    @Test
    public void mapNewForYou() throws Exception {
        final Resources resources = mock(Resources.class);
        when(resources.getString(R.string.new_for_you_title)).thenReturn(NEW_FOR_YOU_TITLE.get());
        when(resources.getString(R.string.new_for_you_intro)).thenReturn(NEW_FOR_YOU_DESCRIPTION.get());
        final NewForYou input = NewForYou.create(LAST_UPDATED.get(), URN, TRACKS);

        final SystemPlaylist result = SystemPlaylistMapper.map(resources, input);

        assertThat(result.urn()).isEqualTo(Urn.NOT_SET);
        assertThat(result.queryUrn()).isEqualTo(Optional.of(URN));
        assertThat(result.lastUpdated()).isEqualTo(LAST_UPDATED);
        assertThat(result.title()).isEqualTo(NEW_FOR_YOU_TITLE);
        assertThat(result.description()).isEqualTo(NEW_FOR_YOU_DESCRIPTION);
        assertThat(result.artworkUrlTemplate()).isEqualTo(Optional.absent());
        assertThat(result.tracks()).hasSize(TRACK_COUNT.get());
        assertThat(result.tracks()).containsExactlyElementsOf(TRACKS);
    }

    @Test
    public void mapNoTracks() throws Exception {
        final ApiSystemPlaylist input = ApiSystemPlaylist.create(URN, Optional.of(0), LAST_UPDATED, TITLE, DESCRIPTION, ARTWORK_URL_TEMPLATE, TRACKING_FEATURE_NAME, new ModelCollection<>());

        final SystemPlaylist result = SystemPlaylistMapper.map(input);

        assertThat(result.urn()).isEqualTo(URN);
        assertThat(result.queryUrn().isPresent()).isFalse();
        assertThat(result.lastUpdated()).isEqualTo(LAST_UPDATED);
        assertThat(result.title()).isEqualTo(TITLE);
        assertThat(result.description()).isEqualTo(DESCRIPTION);
        assertThat(result.artworkUrlTemplate()).isEqualTo(ARTWORK_URL_TEMPLATE);
        assertThat(result.tracks()).hasSize(0);
    }

    @Test
    public void mapNewForYouNoTracks() throws Exception {
        final Resources resources = mock(Resources.class);
        when(resources.getString(R.string.new_for_you_title)).thenReturn(NEW_FOR_YOU_TITLE.get());
        when(resources.getString(R.string.new_for_you_intro)).thenReturn(NEW_FOR_YOU_DESCRIPTION.get());
        final NewForYou input = NewForYou.create(LAST_UPDATED.get(), URN, Lists.newArrayList());

        final SystemPlaylist result = SystemPlaylistMapper.map(resources, input);

        assertThat(result.urn()).isEqualTo(Urn.NOT_SET);
        assertThat(result.queryUrn()).isEqualTo(Optional.of(URN));
        assertThat(result.lastUpdated()).isEqualTo(LAST_UPDATED);
        assertThat(result.title()).isEqualTo(NEW_FOR_YOU_TITLE);
        assertThat(result.description()).isEqualTo(NEW_FOR_YOU_DESCRIPTION);
        assertThat(result.artworkUrlTemplate().isPresent()).isFalse();
        assertThat(result.tracks()).hasSize(0);
    }

    @Test
    public void mapSystemPlaylistEntity() throws Exception {
        final Optional<Urn> queryUrn = Optional.of(Urn.forSystemPlaylist("456"));
        final SystemPlaylistEntity systemPlaylistEntity = SystemPlaylistEntity.create(URN, queryUrn, TITLE, DESCRIPTION, new ArrayList<>(), LAST_UPDATED, ARTWORK_URL_TEMPLATE, TRACKING_FEATURE_NAME);

        final SystemPlaylist result = SystemPlaylistMapper.map(systemPlaylistEntity, TRACKS);

        assertThat(result.urn()).isEqualTo(URN);
        assertThat(result.queryUrn()).isEqualTo(queryUrn);
        assertThat(result.lastUpdated()).isEqualTo(LAST_UPDATED);
        assertThat(result.title()).isEqualTo(TITLE);
        assertThat(result.description()).isEqualTo(DESCRIPTION);
        assertThat(result.artworkUrlTemplate()).isEqualTo(ARTWORK_URL_TEMPLATE);
        assertThat(result.tracks()).isEqualTo(TRACKS);
    }

}
