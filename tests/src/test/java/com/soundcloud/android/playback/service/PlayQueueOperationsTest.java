package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlayQueueOperationsTest {

    private static final String ORIGIN_PAGE = "origin:page";
    private static final long SET_ID = 123L;

    @Inject
    PlayQueueOperations playQueueOperations;

    @Mock
    private Context context;
    @Mock
    private PlayQueueStorage playQueueStorage;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor sharedPreferencesEditor;

    @Before
    public void before() {

        ObjectGraph.create(new TestModule()).inject(this);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueueStorage.storeCollectionAsync(any(Collection.class))).thenReturn(Observable.<Collection<PlayQueueItem>>empty());
        when(sharedPreferences.getString(eq(PlayQueueOperations.Keys.ORIGIN_URL.name()), anyString())).thenReturn("origin:page");
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.SET_ID.name()), anyLong())).thenReturn(123L);
        when(sharedPreferences.getInt(eq(PlayQueueOperations.Keys.PLAY_POSITION.name()), anyInt())).thenReturn(1);
    }

    @Test
    public void shouldReturnLastPlaySessionSourceFromPreferences() throws Exception {
        PlaySessionSource playSessionSource = playQueueOperations.getLastStoredPlaySessionSource();
        expect(playSessionSource.getOriginPage()).toEqual(Uri.parse(ORIGIN_PAGE));
        expect(playSessionSource.getSetId()).toEqual(SET_ID);

    }

    @Test
    public void shouldReturnObservableForLoadingLastPlayQueue() throws Exception {
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(123L);

        Observable<List<PlayQueueItem>> itemObservable = Mockito.mock(Observable.class);
        when(playQueueStorage.getPlayQueueItemsAsync()).thenReturn(itemObservable);
        when(itemObservable.observeOn(AndroidSchedulers.mainThread())).thenReturn(itemObservable);

        Observable<PlayQueue> queueObservable = Mockito.mock(Observable.class);
        when(itemObservable.map(any(Func1.class))).thenReturn(queueObservable);

        expect(playQueueOperations.getLastStoredPlayQueue()).toBe(queueObservable);
    }

    @Test
    public void shouldCreateQueueFromItemsObservable() throws Exception {
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(123L);

        Observable<List<PlayQueueItem>> mockObservable = Mockito.mock(Observable.class);
        when(playQueueStorage.getPlayQueueItemsAsync()).thenReturn(mockObservable);

        PlayQueueItem playQueueItem1 = new PlayQueueItem(1L, "source1", "version1");
        PlayQueueItem playQueueItem2 = new PlayQueueItem(2L, "source2", "version2");
        List<PlayQueueItem> items = Lists.newArrayList(playQueueItem1, playQueueItem2);
        Observable<List<PlayQueueItem>> itemObservable = Observable.just(items);

        when(mockObservable.observeOn(AndroidSchedulers.mainThread())).thenReturn(itemObservable);

        PlayQueue playQueue = playQueueOperations.getLastStoredPlayQueue().toBlockingObservable().lastOrDefault(null);
        expect(playQueue.getItems()).toContainExactly(playQueueItem1, playQueueItem2);
        expect(playQueue.getPosition()).toEqual(1);
        expect(playQueue.getOriginPage()).toEqual(Uri.parse(ORIGIN_PAGE));
        expect(playQueue.getSetId()).toEqual(SET_ID);
    }


    @Test
    public void shouldReturnNullWhenReloadingWithNoValidStoredLastTrack() throws Exception {
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(-1L);
        verifyZeroInteractions(playQueueStorage);
        expect(playQueueOperations.getLastStoredPlayQueue()).toBeNull();
    }

    @Module(library = true, injects = PlayQueueOperationsTest.class)
    class TestModule {
        @Provides
        Context provideContext(){
            return context;
        }

        @Provides
        PlayQueueStorage providePlayQueueStorage(){
            return playQueueStorage;
        }

        @Provides
        SharedPreferences provideSharedPreferences(){
            return sharedPreferences;
        }

    }

}
