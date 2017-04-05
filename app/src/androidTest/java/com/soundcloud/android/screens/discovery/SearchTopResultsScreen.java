package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.discovery.SearchPresenter;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import java.util.regex.Pattern;

public class SearchTopResultsScreen extends Screen {

    public enum Bucket {
        GO_TRACKS(R.string.top_results_soundcloud_go_tracks),
        TRACKS(R.string.top_results_tracks),
        PEOPLE(R.string.top_results_people),
        ALBUMS(R.string.top_results_albums),
        PLAYLISTS(R.string.top_results_playlists);

        private final int headerTitleId;

        Bucket(int headerTitleId) {
            this.headerTitleId = headerTitleId;
        }

        public int getHeaderTitleId() {
            return this.headerTitleId;
        }
    }

    private static final Class ACTIVITY = SearchActivity.class;
    private static final String FRAGMENT = SearchPresenter.SEARCH_RESULTS_TAG;

    public SearchTopResultsScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag(FRAGMENT);
    }

    public ViewElement playlistHeader() {
        return scrollToItemInRecyclerView(With.text(Bucket.PLAYLISTS.getHeaderTitleId()));
    }

    public ViewElement peopleHeader() {
        return scrollToItemInRecyclerView(With.text(Bucket.PEOPLE.getHeaderTitleId()));
    }

    public ViewElement albumHeader() {
        return scrollToItemInRecyclerView(With.text(Bucket.ALBUMS.getHeaderTitleId()));
    }

    public ViewElement goTracksHeader() {
        return scrollToItemInRecyclerView(With.text(Bucket.GO_TRACKS.getHeaderTitleId()));
    }

    public ViewElement tracksHeader() {
        return scrollToItemInRecyclerView(With.text(Bucket.TRACKS.getHeaderTitleId()));
    }

    public SearchTrackResultsScreen clickSeeAllGoTracksButton() {
        seeAllGoTracksButton().click();
        return new SearchTrackResultsScreen(testDriver);
    }

    private ViewElement seeAllGoTracksButton() {
        return scrollToItemInRecyclerView(With.textMatching(Pattern.compile("See all .+ Go[+] tracks", Pattern.CASE_INSENSITIVE)));
    }

    public SearchScreen clickSearch() {
        testDriver.findOnScreenElement(With.id(R.id.search_edit_text)).click();
        return new SearchScreen(testDriver);
    }

    public PlaylistDetailsScreen findAndClickFirstAlbumItem() {
        scrollToBucketAndClickFirstItem(Bucket.ALBUMS, R.id.playlist_list_item);
        return new PlaylistDetailsScreen(testDriver);
    }

    public ProfileScreen findAndClickFirstUserItem() {
        scrollToBucketAndClickFirstItem(Bucket.PEOPLE, R.id.user_list_item);
        return new ProfileScreen(testDriver);
    }

    public VisualPlayerElement findAndClickFirstTrackItem() {
        scrollToBucketAndClickFirstItem(Bucket.TRACKS, R.id.track_list_item);
        final VisualPlayerElement visualPlayer = new VisualPlayerElement(testDriver);
        visualPlayer.waitForExpandedPlayer();

        return visualPlayer;
    }

    private void scrollToBucketAndClickFirstItem(final Bucket bucket, final int elementsId) {
        testDriver.scrollToFirstItemUnderHeader(With.text(bucket.getHeaderTitleId()), With.id(elementsId)).click();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT);
    }

}
