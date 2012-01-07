package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    ScContentProvider provider;

    @Before
    public void setup() {
        provider = new ScContentProvider();
        provider.onCreate();
        DefaultTestRunner.application.setCurrentUserId(1);
    }

    @Test
    public void testInsertAndQueryFavorites() throws Exception {
        CollectionHolder<Track> tracks = getUserFavorites();
        expect(tracks.size()).toBe(15);

        for (Track t : tracks) {
            Uri user = provider.insert(Content.USERS.uri, t.user.buildContentValues(false));
            Uri track = provider.insert(Content.ME_FAVORITES.uri, t.buildContentValues());
            expect(user).not.toBeNull();
            expect(track).not.toBeNull();
        }

        Cursor c = provider.query(Content.ME_FAVORITES.uri, null, null, null, null);
        expect(c).not.toBeNull();
        expect(c.getCount()).toBe(tracks.size());
    }

    public CollectionHolder<Track> getUserFavorites() throws IOException {
        return AndroidCloudAPI.Mapper.readValue(getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);
    }
}
