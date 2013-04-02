package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class BulkInsertMapTest {

    @Test
    public void shouldNotInsertMultipleInstancesOfTheSameResource() {
        User user = new User(1);

        Track track = new Track();
        track.user = user;

        // SA and Track share the same user
        SoundAssociation sa = new SoundAssociation();
        sa.playable = track;
        sa.created_at = new Date();
        sa.user = user;

        BulkInsertMap map = new BulkInsertMap();
        sa.putFullContentValues(map);

        ContentResolver resolver = mock(ContentResolver.class);
        map.insert(resolver);

        expect(map.get(Content.TRACKS.uri)).toNumber(1);
        expect(map.get(Content.COLLECTION_ITEMS.uri)).toNumber(1);
        expect(map.get(Content.USERS.uri)).toNumber(1);

        verify(resolver).bulkInsert(eq(Content.COLLECTION_ITEMS.uri), any(ContentValues[].class));
        verify(resolver).bulkInsert(eq(Content.TRACKS.uri), any(ContentValues[].class));
        verify(resolver).bulkInsert(eq(Content.USERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

}
