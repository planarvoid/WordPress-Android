package com.soundcloud.android.storage;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.util.Arrays;
import java.util.Collection;

@RunWith(SoundCloudTestRunner.class)
public class BulkStorageTest {

    @Mock
    private ContentResolver contentResolver;
    @Mock
    private Observer<Collection<? extends PublicApiResource>> observer;

    private BulkStorage bulkStorage;

    @Before
    public void setup() {
        bulkStorage = new BulkStorage(Schedulers.immediate(), contentResolver);
    }

    @Test
    public void shouldStoreMixedResourceCollectionAsBulkInsert() {
        final PublicApiTrack track = new PublicApiTrack();
        final PublicApiUser user = new PublicApiUser();
        final PublicApiPlaylist playlist = new PublicApiPlaylist();
        Collection<? extends PublicApiResource> resources = Arrays.asList(track, user, playlist);
        bulkStorage.bulkInsert(resources);

        verify(contentResolver).bulkInsert(eq(Content.TRACKS.uri), any(ContentValues[].class));
        verify(contentResolver).bulkInsert(eq(Content.USERS.uri), any(ContentValues[].class));
        verify(contentResolver).bulkInsert(eq(Content.PLAYLISTS.uri), any(ContentValues[].class));
    }

    @Test
    public void shouldStoreMixedResourceCollectionAsBulkInsertObservable() {
        final PublicApiTrack track = new PublicApiTrack();
        final PublicApiUser user = new PublicApiUser();
        final PublicApiPlaylist playlist = new PublicApiPlaylist();
        Collection<? extends PublicApiResource> resources = Arrays.asList(track, user, playlist);
        bulkStorage.bulkInsertAsync(resources).subscribe(observer);

        verify(contentResolver).bulkInsert(eq(Content.TRACKS.uri), any(ContentValues[].class));
        verify(contentResolver).bulkInsert(eq(Content.USERS.uri), any(ContentValues[].class));
        verify(contentResolver).bulkInsert(eq(Content.PLAYLISTS.uri), any(ContentValues[].class));
        verify(observer).onNext(resources);
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }
}
