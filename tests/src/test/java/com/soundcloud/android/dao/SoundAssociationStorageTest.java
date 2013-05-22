package com.soundcloud.android.dao;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationStorageTest {

    private SoundAssociationStorage storage;
    
    @Before
    public void initTest() {
        storage = new SoundAssociationStorage();
    }
    
    @Test
    public void shouldStoreLikeAndUpdateLikesCount() {
        Track track = new Track(1);
        expect(track.likes_count).not.toBe(1);

        track.likes_count = 1;
        storage.addLike(track);

        expect(Content.ME_LIKES).toHaveCount(1);
        expect(TestHelper.reload(track).likes_count).toBe(1);
    }

    @Test
    public void shouldRemoveLikeAndUpdateLikesCount() {
        Track track = new Track(1);
        track.likes_count = 1;
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);
        expect(Content.ME_LIKES).toHaveCount(1);
        expect(TestHelper.reload(track).likes_count).toBe(1);

        track.likes_count = 0;
        storage.removeLike(track);

        expect(Content.ME_LIKES).toHaveCount(0);
        expect(TestHelper.reload(track).likes_count).toBe(0);
    }

    @Test
    public void shouldPersistPlaylistCreation() throws Exception {
        final List<Track> tracks = createTracks(2);
        Playlist p = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        storage.addCreation(p);
        expect(p.toUri()).not.toBeNull();
        expect(Content.ME_PLAYLISTS).toHaveCount(1);
    }

    @Test
    public void shouldSyncSoundAssociationsMeSounds() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                SoundAssociationTest.class.getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(storage.syncToLocal(old.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        // expect no change, syncing to itself
        expect(storage.syncToLocal(old.collection, Content.ME_SOUNDS.uri)).toBeFalse();
        expect(Content.ME_SOUNDS).toHaveCount(38);

        // expect change, syncing with 2 items
        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK_REPOST));
        holder.collection.add(createAssociation(66376067l, SoundAssociation.Type.TRACK));

        expect(storage.syncToLocal(holder.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(2);

        // remove the repost and make sure it gets removed locally
        holder.collection.remove(0);
        expect(storage.syncToLocal(holder.collection, Content.ME_SOUNDS.uri)).toBeTrue();
        expect(Content.ME_SOUNDS).toHaveCount(1);
    }

    @Test
    public void shouldSyncSoundAssociationsMeLikes() throws Exception {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                ApiSyncerTest.class.getResourceAsStream("e1_likes.json"),
                SoundAssociationHolder.class);

        expect(storage.syncToLocal(old.collection, Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(3);

        // expect no change, syncing to itself
        expect(storage.syncToLocal(old.collection, Content.ME_LIKES.uri)).toBeFalse();
        expect(Content.ME_LIKES).toHaveCount(3);

        SoundAssociationHolder holder = new SoundAssociationHolder();
        holder.collection = new ArrayList<SoundAssociation>();
        holder.collection.add(createAssociation(56143158l, SoundAssociation.Type.TRACK_LIKE));

        expect(storage.syncToLocal(holder.collection, Content.ME_LIKES.uri)).toBeTrue();
        expect(Content.ME_LIKES).toHaveCount(1);
    }

    @Test
    public void shouldLoadSoundStreamItemsForUser() {
        Track track = new Track(1);
        Playlist playlist = new Playlist(1);

        insertSoundAssociations(track, playlist);

        List<SoundAssociation> result = storage.getSoundStreamItemsForCurrentUser();
        expect(result).toNumber(4);
        expect(result).toContainExactlyInAnyOrder(
                new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK),
                new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK_REPOST),
                new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST),
                new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST_REPOST));
    }

    @Test
    public void shouldLoadLikesForUser() {
        Track track = new Track(1);
        Playlist playlist = new Playlist(1);

        insertSoundAssociations(track, playlist);

        List<SoundAssociation> result = storage.getLikesForCurrentUser();
        expect(result).toNumber(2);
        expect(result).toContainExactlyInAnyOrder(
                new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK_LIKE),
                new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST_LIKE));
    }

    @Test
    public void shouldLoadPlaylistCreationsForUser() {
        Track track = new Track(1);
        Playlist playlist = new Playlist(1);

        insertSoundAssociations(track, playlist);

        List<SoundAssociation> result = storage.getPlaylistCreationsForCurrentUser();
        expect(result).toNumber(1);
        expect(result).toContain(new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST));
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingLikes() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.addLike(new Playlist(1L));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/likes/1");
    }

    @Test
    public void shouldNotifyContentObserverWhenRemovingLikes() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.removeLike(new Playlist(1L));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/likes");
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingReposts() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.addRepost(new Playlist(1L));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/reposts/1");
    }

    @Test
    public void shouldNotifyContentObserverWhenRemovingReposts() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.removeRepost(new Playlist(1L));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/reposts");
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingTrackCreation() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        Track track = new Track(1L);
        track.created_at = new Date();
        storage.addCreation(track);

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/sounds/1");
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingPlaylistCreation() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.addCreation(Playlist.newUserPlaylist(new User(1L), "playlist", false, Collections.<Track>emptyList()));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/playlists/1");
    }

    private void insertSoundAssociations(Track track, Playlist playlist) {
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK);
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_REPOST);
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_REPOST);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_LIKE);
    }

    private SoundAssociation createAssociation(long id, Association.Type type) {
        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playable = new Track(id);
        soundAssociation1.setType(type.name());
        soundAssociation1.created_at = new Date(System.currentTimeMillis());
        return soundAssociation1;
    }

    private List<Track> createTracks(int n) {
        List<Track> items = new ArrayList<Track>(n);

        for (int i=0; i<n; i++) {
            User user = new User();
            user.permalink = "u"+i;
            user.id = i;

            Track track = new Track();
            track.id = i;
            track.user = user;
            items.add(track);
        }
        return items;
    }

}
