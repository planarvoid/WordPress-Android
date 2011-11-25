package com.soundcloud.android.provider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.codehaus.jackson.JsonParseException;
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
            System.out.println(provider.insert(ScContentProvider.Content.USERS, t.user.buildContentValues(false)));
            System.out.println(provider.insert(ScContentProvider.Content.ME_FAVORITES, t.buildContentValues()));
        }

        Cursor c = provider.query(ScContentProvider.Content.ME_FAVORITES, null, null, null, null);
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
