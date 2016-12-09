package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.CollectionsTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.settings.SettingKey;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PlaylistAutoplayRecommendationsTest extends TrackingActivityTest<MainActivity> {

    public static final String TEST_PLAYLIST_AUTO_RECOMMENDATION = "audio-events-v1-my-playlist-auto-recommendation";
    public static final String TEST_PLAYLIST_AUTO_RECOMMENDATION_OFF = "audio-events-v1-my-playlist-auto-recommendation-off";

    private CollectionScreen collectionScreen;

    public PlaylistAutoplayRecommendationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playlistUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        collectionScreen = mainNavHelper.goToCollections();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        setAutoPlayEnabled(true);
    }

    @CollectionsTest
    public void testAutoPlaysRecommendationsAfterPlaylist() {
        final PlaylistDetailsScreen playlistDetailsScreen = collectionScreen.clickPlaylistsPreview()
                                                                            .clickOnFirstPlaylist();

        startEventTracking();

        final VisualPlayerElement playerElement = playlistDetailsScreen.clickLastTrack();
        assertIsPlaying(playerElement);
        String trackName = playerElement.getTrackTitle();

        playerElement.swipeNext(); // swipe to first recommendation

        assertNotSame(trackName, playerElement.getTrackTitle());
        assertIsPlaying(playerElement);

        playerElement.clickArtwork(); // stop playback

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_PLAYLIST_AUTO_RECOMMENDATION);
    }

    @CollectionsTest
    public void testDoesNotAutoPlaysRecommendationsAfterPlaylistIfSettingOff() {
        setAutoPlayEnabled(false);

        final PlaylistDetailsScreen playlistDetailsScreen = collectionScreen.clickPlaylistsPreview()
                                                                            .clickOnFirstPlaylist();

        startEventTracking();

        final VisualPlayerElement playerElement = playlistDetailsScreen.clickLastTrack();
        assertIsPlaying(playerElement);
        String trackName = playerElement.getTrackTitle();

        playerElement.swipeNext(); // swipe to first recommendation will not change anything

        assertSame(trackName, playerElement.getTrackTitle());
        assertIsPlaying(playerElement);

        playerElement.clickArtwork(); // stop playback

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_PLAYLIST_AUTO_RECOMMENDATION_OFF);
    }

    private void setAutoPlayEnabled(boolean enabled) {
        getDefaultSharedPreferences().edit().putBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, enabled).apply();
    }

    private SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getTargetContext());
    }

    private void assertIsPlaying(VisualPlayerElement playerElement) {
        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));
    }

}
