package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SharedPreferences;
import android.os.Parcel;

import java.util.Arrays;

public class PlaySessionSourceTest extends AndroidUnitTest {
    private static final String ORIGIN_PAGE = "origin:page";
    private static final String EXPLORE_TAG = "explore:123";
    private PublicApiPlaylist playlist;

    @Mock SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        playlist = ModelFixtures.create(PublicApiPlaylist.class);
    }

    @Test
    public void shouldCreateEmptyPlaySessionSource() throws Exception {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ScTextUtils.EMPTY_STRING);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getInitialSource()).isEqualTo(ScTextUtils.EMPTY_STRING);
        assertThat(playSessionSource.getInitialSourceVersion()).isEqualTo(ScTextUtils.EMPTY_STRING);
        assertThat(playSessionSource.getSearchQuerySourceInfo()).isNull();
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPage() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getInitialSource()).isEqualTo(ScTextUtils.EMPTY_STRING);
        assertThat(playSessionSource.getInitialSourceVersion()).isEqualTo(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndSetId() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(playlist.getUrn());
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(playlist.getUserUrn());
        assertThat(playSessionSource.getInitialSource()).isEqualTo(ScTextUtils.EMPTY_STRING);
        assertThat(playSessionSource.getInitialSourceVersion()).isEqualTo(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndExploreTag() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setExploreVersion(EXPLORE_TAG);
        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getInitialSource()).isEqualTo(PlaySessionSource.DiscoverySource.EXPLORE.value());
        assertThat(playSessionSource.getInitialSourceVersion()).isEqualTo(EXPLORE_TAG);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageTrackSourceInfoAndSetId() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playSessionSource.setExploreVersion(EXPLORE_TAG);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(playlist.getUrn());
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(playlist.getUserUrn());
        assertThat(playSessionSource.getInitialSource()).isEqualTo(PlaySessionSource.DiscoverySource.EXPLORE.value());
        assertThat(playSessionSource.getInitialSourceVersion()).isEqualTo(EXPLORE_TAG);
    }

    @Test
    public void shouldCreateQuerySourceInfoFromTrackSourceInfo() throws Exception {
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"));
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_EVERYTHING);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        assertThat(playSessionSource.getSearchQuerySourceInfo()).isEqualTo(searchQuerySourceInfo);
        assertThat(playSessionSource.isFromQuery()).isEqualTo(true);
    }

    @Test
    public void shouldBeParcelable() {
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"));
        PromotedSourceInfo promotedSourceInfo = new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.<Urn>absent(), Arrays.asList("url"));
        PlaySessionSource original = new PlaySessionSource(ORIGIN_PAGE);
        original.setExploreVersion(EXPLORE_TAG);
        original.setPlaylist(playlist.getUrn(), playlist.getUserUrn());
        original.setSearchQuerySourceInfo(searchQuerySourceInfo);
        original.setPromotedSourceInfo(promotedSourceInfo);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        assertThat(copy.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(copy.getCollectionUrn()).isEqualTo(playlist.getUrn());
        assertThat(copy.getCollectionOwnerUrn()).isEqualTo(playlist.getUserUrn());
        assertThat(copy.getInitialSource()).isEqualTo(PlaySessionSource.DiscoverySource.EXPLORE.value());
        assertThat(copy.getInitialSourceVersion()).isEqualTo(EXPLORE_TAG);
        assertThat(copy.getSearchQuerySourceInfo()).isEqualTo(searchQuerySourceInfo);
        assertThat(copy.getPromotedSourceInfo()).isEqualTo(promotedSourceInfo);
    }

    @Test
    public void shouldParcelAbsentMetadata() {
        PlaySessionSource original = new PlaySessionSource(ORIGIN_PAGE);
        original.setExploreVersion(EXPLORE_TAG);
        original.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        assertThat(copy.isFromPromotedItem()).isFalse();
        assertThat(copy.isFromQuery()).isFalse();
    }

    @Test
    public void createsPlaySessionSourceFromPreferences() {
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG), anyString())).thenReturn("screen");
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_COLLECTION_URN), anyString())).thenReturn("soundcloud:tracks:123");
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_COLLECTION_OWNER_URN), anyString())).thenReturn("soundcloud:users:123");

        PlaySessionSource playSessionSource = new PlaySessionSource(sharedPreferences);

        assertThat(playSessionSource.getOriginScreen()).isEqualTo("screen");
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.forTrack(123));
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(Urn.forUser(123));
    }

    @Test
    public void shouldOriginateFromExploreWithExploreOrigin() {
        assertThat(new PlaySessionSource("explore:something").originatedInExplore()).isTrue();
    }

    @Test
    public void shouldNotOriginateFromExploreWithNonExploreOrigin() {
        assertThat(new PlaySessionSource("stream:something").originatedInExplore()).isFalse();
    }

    @Test
    public void shouldNotOriginateFromExploreWithEmptyOrigin() {
        assertThat(new PlaySessionSource(ScTextUtils.EMPTY_STRING).originatedInExplore()).isFalse();
    }
}
