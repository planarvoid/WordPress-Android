package com.soundcloud.android.storage.provider;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(DefaultTestRunner.class)
public class BulkInsertMapTest {

    @Test
    public void shouldNotInsertMultipleInstancesOfTheSameResource() {
        PublicApiUser user = new PublicApiUser(1);

        PublicApiTrack track = new PublicApiTrack();
        track.user = user;

        // SA and Track share the same user
        SoundAssociation sa = new SoundAssociation();
        sa.playable = track;
        sa.created_at = new Date();
        sa.owner = user;

        BulkInsertMap map = new BulkInsertMap();
        sa.putFullContentValues(map);

        ContentResolver resolver = mock(ContentResolver.class);
        map.insert(resolver);

        expect(map.get(Content.TRACKS.uri)).toNumber(1);
        expect(map.get(Content.ME_SOUNDS.uri)).toNumber(1);
        expect(map.get(Content.USERS.uri)).toNumber(1);

        verify(resolver).bulkInsert(eq(Content.ME_SOUNDS.uri), any(ContentValues[].class));
        verify(resolver).bulkInsert(eq(Content.TRACKS.uri), any(ContentValues[].class));
        verify(resolver).bulkInsert(eq(Content.USERS.uri), any(ContentValues[].class));
        verifyNoMoreInteractions(resolver);
    }

    @Test
    public void shouldUseLinkedHashSetToPreserveOrder() throws Exception {
        final BulkInsertMap map = new BulkInsertMap();
        new PublicApiTrack().putFullContentValues(map);
        final Set<BulkInsertMap.ResourceValues> actual = map.get(Content.TRACKS.uri);
        expect(LinkedHashSet.class.isAssignableFrom(actual.getClass())).toBeTrue();
    }
}
