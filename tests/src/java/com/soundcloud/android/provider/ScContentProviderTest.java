package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.FileMap;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.util.DatabaseConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentProvider;
import android.database.Cursor;

@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    private ContentProvider provider;

    @Before
    public void before() {
        provider = new ScContentProvider();
        provider.onCreate();
    }

    @Test @Ignore
    public void shouldSearch() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            provider.insert(Content.TRACKS.uri, t.buildContentValues());
            provider.insert(Content.USERS.uri, t.user.buildContentValues());
        }

        Cursor cursor = provider.query(Content.ANDROID_SEARCH_SUGGEST.uri,
                null, null, new String[] { "plaid"}, null);

        // TODO: needs matrix cursor
        expect(cursor.getCount()).toEqual(1);
    }
}
