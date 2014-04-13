package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.provider.Content;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.content.ContentResolver;
import android.content.ContentValues;


@RunWith(DefaultTestRunner.class)
public class TrackStorageTest {
    private TrackStorage storage;
    private ModelFactory modelFactory;

    @Mock
    ContentResolver contentResolver;
    @Mock
    TrackDAO trackDAO;
    @Mock
    ScModelManager modelManager;
    @Mock
    Observer<Track> observer;

    Track track;

    @Before
    public void before() throws CreateModelException {
        storage = new TrackStorage(contentResolver, trackDAO, modelManager, Schedulers.immediate());
        modelFactory = TestHelper.getModelFactory();
        track = modelFactory.createModel(Track.class);
    }

    @Test
    public void storeTrackCreatesTrackOnDAO() throws Exception {
        storage.store(track);
        verify(trackDAO).create(track);
    }

    @Test
    public void storeTrackCachesFullTrackInModelManager() throws Exception {
        storage.store(track);
        verify(modelManager).cache(track, ScResource.CacheUpdateMode.FULL);
    }

    @Test(expected = NotFoundException.class)
    public void getTrackShouldThrowNotFoundExceptionIfNoTrackFound() throws Exception {
        storage.getTrack(track.getId());
    }

    @Test
    public void getTrackShouldEmitTrackFromStorage() throws Exception {
        when(trackDAO.queryById(track.getId())).thenReturn(track);
        when(modelManager.cache(track)).thenReturn(track);
        expect(storage.getTrack(track.getId())).toBe(track);
    }

    @Test
    public void getTrackShouldCacheTrackFromStorageIfNotInCache() throws Exception {
        when(trackDAO.queryById(track.getId())).thenReturn(track);
        storage.getTrack(track.getId());
        verify(modelManager).cache(track);
    }

    @Test
    public void getTrackAsyncShouldEmitTrackFromCache() throws Exception {
        when(modelManager.getCachedTrack(track.getId())).thenReturn(track);
        storage.getTrackAsync(track.getId()).subscribe(observer);
        verify(observer).onNext(track);
        verify(observer).onCompleted();
    }

    @Test
    public void getTrackAsyncShouldEmitTrackFromStorageIfNotInCache() throws Exception {
        when(trackDAO.queryById(track.getId())).thenReturn(track);
        when(modelManager.cache(track)).thenReturn(track);
        storage.getTrackAsync(track.getId()).subscribe(observer);
        verify(observer).onNext(track);
        verify(observer).onCompleted();
    }

    @Test
    public void getTrackAsyncShouldCallOnErrorWithNotFoundException() throws Exception {
        storage.getTrackAsync(track.getId()).subscribe(observer);
        verify(observer).onError(any(NotFoundException.class));
    }

    @Test
    public void shouldMarkTrackAsPlayed() throws Exception {
        storage.createPlayImpression(track);

        ArgumentCaptor<ContentValues> contentValues = ArgumentCaptor.forClass(ContentValues.class);
        verify (contentResolver).insert(eq(Content.TRACK_PLAYS.uri), contentValues.capture());

        expect(contentValues.getValue().size()).toEqual(1);
        expect(contentValues.getValue().get(TableColumns.TrackMetadata._ID)).toEqual(track.getId());
    }
}
