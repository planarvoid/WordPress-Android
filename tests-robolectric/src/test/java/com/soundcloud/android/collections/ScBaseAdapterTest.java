package com.soundcloud.android.collections;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.api.legacy.model.Shortcut;
import com.soundcloud.android.api.legacy.model.activities.TrackActivity;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.LinkedList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ScBaseAdapterTest {

    private ScBaseAdapter<PublicApiUser> adapter;

    @Before
    public void setup() throws Exception {
        adapter = new ScBaseAdapter<PublicApiUser>(Content.USER.uri) {
            @Override
            protected LinearLayout createRow(Context context, int position, ViewGroup parent) {
                return null;
            }

            @Override
            public int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
                return ItemClickResults.IGNORE;
            }
        };
    }

    @Test
    public void shouldRefreshStaleItems() {
        TestHelper.connectedViaWifi(true);

        TestHelper.addIdResponse("/tracks?linked_partitioning=1&ids=1%2C2", 1, 2);
        TestHelper.addIdResponse("/users?linked_partitioning=1&ids=1", 1);
        TestHelper.addIdResponse("/playlists?linked_partitioning=1&ids=1&representation=compact", 1);

        List<ScModel> staleModels = new LinkedList<>();

        // tracks
        staleModels.add(new PublicApiTrack(1) {
            @Override
            public boolean isStale() {
                return true;
            }
        });
        staleModels.add(new TrackActivity() {
            @Override
            public boolean isStale() {
                return true;
            }

            @Override
            public Refreshable getRefreshableResource() {
                return new PublicApiTrack(2);
            }
        });
        staleModels.add(new PublicApiTrack(3) {
            @Override
            public boolean isStale() {
                return false; // not stale, should not appear
            }
            @Override
            public boolean isIncomplete() {
                return false; // not stale, should not appear
            }
        });

        // users
        staleModels.add(new PublicApiUser(1) {
            @Override
            public boolean isStale() {
                return true;
            }
        });

        // playlists
        staleModels.add(new PublicApiPlaylist(1) {
            @Override
            public boolean isStale() {
                return true;
            }
        });

        // should not appear, not refreshable
        staleModels.add(new Shortcut());

        adapter.checkForStaleItems(Robolectric.application, staleModels);
    }

    @Test
    public void shouldRequestNextPage() {
        adapter.setIsLoadingData(false);

        expect(adapter.shouldRequestNextPage(0, 5, 5)).toBeTrue();
    }

    @Test
    public void shouldRequestNextPageWithOnePageLookAhead() {
        adapter.setIsLoadingData(false);

        expect(adapter.shouldRequestNextPage(0, 5, 2 * 5)).toBeTrue();
    }

    @Test
    public void shouldNotRequestNextPageIfAlreadyLoading() {
        adapter.setIsLoadingData(true);

        expect(adapter.shouldRequestNextPage(0, 5, 5)).toBeFalse();
    }

    @Test
    public void shouldNotRequestNextPageIfZeroItems() {
        adapter.setIsLoadingData(true);
        expect(adapter.shouldRequestNextPage(0, 5, 0)).toBeFalse();
        adapter.setIsLoadingData(false);
        expect(adapter.shouldRequestNextPage(0, 5, 0)).toBeFalse();
    }
}
