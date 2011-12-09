package com.soundcloud.android.provider;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import android.database.Cursor;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void testFromJSON() throws Exception {
        CollectionHolder<Track> tracks = getUserFavorites();
        assertThat(tracks.size(), is(15));

        for (Track t : tracks) {
            System.out.println(provider.insert(Content.USERS.uri, t.user.buildContentValues(false)));
            System.out.println(provider.insert(Content.ME_FAVORITES.uri, t.buildContentValues()));
        }

        Cursor c = provider.query(Content.ME_FAVORITES.uri, null, null, null, null);
        assertThat(tracks.size(), is(c.getCount()));
        if (c != null && c.moveToFirst()){
            do {
                System.out.println(new Track(c).toString());
            } while (c.moveToNext());
        }
    }


    public CollectionHolder<Track> getUserFavorites() throws IOException {
        return AndroidCloudAPI.Mapper.readValue(getClass().getResourceAsStream("user_favorites.json"), Track.TrackHolder.class);
    }
}
