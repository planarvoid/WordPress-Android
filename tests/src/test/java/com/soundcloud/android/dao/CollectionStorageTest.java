package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addPendingHttpResponse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

@RunWith(DefaultTestRunner.class)
public class CollectionStorageTest {
    final static long USER_ID = 1L;

    private ContentResolver resolver;
    private CollectionStorage storage;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        storage = new CollectionStorage(DefaultTestRunner.application);
    }

    @Test
    public void shouldGetIdsOfPersistedResources() {
        // regression test for exceptions we got due to http://www.sqlite.org/limits.html
        final int SQLITE_VARIABLE_LIMIT = 999;
        Long[] requestedIds = new Long[SQLITE_VARIABLE_LIMIT + 1]; // make sure we don't crash on the variable limit
        Arrays.fill(requestedIds, 1L);
        requestedIds[0] = 2L;

        Track track1 = buildCompleteTrack(1L);
        Track track2 = buildCompleteTrack(2L);

        TestHelper.bulkInsert(track1, track2);

        Set<Long> storedIds = storage.getStoredIds(Content.TRACKS, Lists.newArrayList(requestedIds));
        expect(storedIds).toContainExactly(1L, 2L);
    }

    @Test
    public void shouldGetIdsOfPersistedResourcesInBatches() {
        ContentResolver resolverMock = mock(ContentResolver.class);
        Context mockContext = mock(Context.class);
        when(mockContext.getContentResolver()).thenReturn(resolverMock);
        storage = new CollectionStorage(mockContext);

        Long[] requestedIds = new Long[1300];
        Arrays.fill(requestedIds, 1L);

        final int expectedBatchCount = 3;

        storage.getStoredIds(Content.TRACKS, Lists.newArrayList(requestedIds));

        verify(resolverMock, times(expectedBatchCount)).query(
                eq(Content.TRACKS.uri),
                eq(new String[]{BaseColumns._ID}),
                anyString(),
                any(String[].class),
                isNull(String.class));
        verifyNoMoreInteractions(resolverMock);
    }


    @Test
    public void shouldWriteMissingCollectionItems() throws Exception {
        addPendingHttpResponse(getClass(), "5_users.json");

        List<User> users = new ArrayList<User>();
        for (int i = 0; i < 2; i++){
            users.add(createUserWithId(i));
        }
        new UserDAO(resolver).createCollection(users);

        ArrayList<Long> ids = new ArrayList<Long>();
        for (long i = 0; i < 10; i++){
            ids.add(i);
        }

        int itemsStored = storage.fetchAndStoreMissingCollectionItems(DefaultTestRunner.application.getCloudAPI(), ids, Content.USERS, false);
        expect(itemsStored).toEqual(5);
    }

    @Test
    public void shouldRemoveSyncedContentForLoggedInUser() throws Exception {
        SoundAssociationHolder sounds = TestHelper.readJson(SoundAssociationHolder.class, SoundAssociationTest.class, "sounds.json");
        TestHelper.bulkInsert(Content.ME_SOUNDS.uri,sounds.collection);

        SoundAssociationHolder likes = TestHelper.readJson(SoundAssociationHolder.class, ApiSyncerTest.class, "e1_likes.json");
        TestHelper.bulkInsert(Content.ME_LIKES.uri, likes.collection);

        expect(Content.ME_SOUNDS).toHaveCount(38);
        expect(Content.ME_LIKES).toHaveCount(3);

        storage.clear();
        expect(Content.ME_SOUNDS).toHaveCount(0);
        expect(Content.ME_LIKES).toHaveCount(0);
    }

    public static List<Track> createTracks() {
        List<Track> items = new ArrayList<Track>();

        User u1 = new User();
        u1.permalink = "u1";
        u1.setId(100L);

        Track t = new Track();
        t.setId(200L);
        t.user = u1;

        User u2 = new User();
        u2.permalink = "u2";
        u2.setId(300L);

        Track t2 = new Track();
        t2.setId(400);
        t2.user = u2;

        items.add(t);
        items.add(t2);
        return items;
    }

    private User createUserWithId(long id){
        User u = new User();
        u.setId(id);
        return u;
    }

    private Track buildCompleteTrack(long id) {
        Track track = new Track(id);
        track.created_at = new Date();
        track.state = Track.State.FINISHED;
        track.duration = 100;
        expect(track.isCompleteTrack()).toBeTrue();
        return track;
    }

}
