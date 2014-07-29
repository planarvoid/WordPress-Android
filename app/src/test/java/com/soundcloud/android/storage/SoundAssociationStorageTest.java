package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Sets;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.SoundAssociationHolder;
import com.soundcloud.android.api.legacy.model.SoundAssociationTest;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncerTest;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;

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
        PublicApiTrack track = new PublicApiTrack(1);
        track.likes_count = 0;
        track.user_like = false;
        expect(Content.ME_LIKES).toHaveCount(0);

        storage.addLike(track);

        expect(Content.ME_LIKES).toHaveCount(1);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.likes_count).toBe(1);
        expect(storedTrack.user_like).toBeTrue();
    }

    @Test
    public void shouldNotReturnLikeIfTrackRemoved() {
        storage.addLike(new PublicApiTrack(1));
        expect(Content.ME_LIKES).toHaveCount(1);

        ContentResolver resolver = Robolectric.application.getContentResolver();
        expect(resolver.delete(Content.TRACK.forId(1), null, null)).toBe(1);

        expect(Content.ME_LIKES).toHaveCount(0);
    }

    @Test
     public void shouldStoreLikeAndUpdateLikesCountForUninitializedTrack() {
        PublicApiTrack track = new PublicApiTrack(1);
        expect(Content.ME_LIKES).toHaveCount(0);

        storage.addLike(track);

        expect(Content.ME_LIKES).toHaveCount(1);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.likes_count).toBe(1);
        expect(storedTrack.user_like).toBeTrue();
    }

    @Test
    public void shouldRemoveLikeAndUpdateLikesCount() {
        PublicApiTrack track = new PublicApiTrack(1);
        track.likes_count = 1;
        track.user_like = true;
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);
        expect(Content.ME_LIKES).toHaveCount(1);

        storage.removeLike(track);

        expect(Content.ME_LIKES).toHaveCount(0);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.likes_count).toBe(0);
        expect(storedTrack.user_like).toBeFalse();
    }

    @Test
    public void shouldRemoveLikeAndUpdateLikesCountForUninitializedTrack() {
        PublicApiTrack track = new PublicApiTrack(1);
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);
        expect(Content.ME_LIKES).toHaveCount(1);

        storage.removeLike(track);

        expect(Content.ME_LIKES).toHaveCount(0);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.likes_count).toBe(0);
        expect(storedTrack.user_like).toBeFalse();
    }

    @Test
    public void shouldStoreRepostAndUpdateRepostsCount() {
        PublicApiTrack track = new PublicApiTrack(1);
        track.reposts_count = 0;
        track.user_repost = true;
        expect(Content.ME_REPOSTS).toHaveCount(0);

        storage.addRepost(track);

        expect(Content.ME_REPOSTS).toHaveCount(1);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.reposts_count).toBe(1);
        expect(storedTrack.user_repost).toBeTrue();
    }

    @Test
    public void shouldStoreRepostAndUpdateRepostsCountForUninitializedTrack() {
        PublicApiTrack track = new PublicApiTrack(1);
        expect(Content.ME_REPOSTS).toHaveCount(0);

        storage.addRepost(track);

        expect(Content.ME_REPOSTS).toHaveCount(1);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.reposts_count).toBe(1);
        expect(storedTrack.user_repost).toBeTrue();
    }

    @Test
    public void shouldRemoveRepostAndUpdateLikesCount() {
        PublicApiTrack track = new PublicApiTrack(1);
        track.reposts_count = 1;
        track.user_repost = true;
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_REPOST);
        expect(Content.ME_REPOSTS).toHaveCount(1);

        storage.removeRepost(track);

        expect(Content.ME_REPOSTS).toHaveCount(0);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.reposts_count).toBe(0);
        expect(storedTrack.user_repost).toBeFalse();
    }

    @Test
    public void shouldRemoveRepostAndUpdateLikesCountForUninitializedTrack() {
        PublicApiTrack track = new PublicApiTrack(1);
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_REPOST);
        expect(Content.ME_REPOSTS).toHaveCount(1);

        storage.removeRepost(track);

        expect(Content.ME_REPOSTS).toHaveCount(0);
        final PublicApiTrack storedTrack = TestHelper.reload(track);
        expect(storedTrack.reposts_count).toBe(0);
        expect(storedTrack.user_repost).toBeFalse();
    }

    @Test
    public void shouldPersistPlaylistCreation() throws Exception {
        final List<PublicApiTrack> tracks = createTracks(2);
        PublicApiPlaylist p = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        SoundAssociation playlistCreation = storage.addCreation(p);
        expect(playlistCreation).not.toBeNull();
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
        PublicApiTrack track = new PublicApiTrack(1);
        PublicApiPlaylist playlist = new PublicApiPlaylist(1);

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
        PublicApiTrack track = new PublicApiTrack(1);
        PublicApiPlaylist playlist = new PublicApiPlaylist(1);

        insertSoundAssociations(track, playlist);

        List<SoundAssociation> result = storage.getLikesForCurrentUser();
        expect(result).toNumber(2);
        expect(result).toContainExactlyInAnyOrder(
                new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK_LIKE),
                new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST_LIKE));
    }

    @Test
    public void shouldLoadPlaylistCreationsForUser() {
        PublicApiTrack track = new PublicApiTrack(1);
        PublicApiPlaylist playlist = new PublicApiPlaylist(1);

        insertSoundAssociations(track, playlist);

        List<SoundAssociation> result = storage.getPlaylistCreationsForCurrentUser();
        expect(result).toNumber(1);
        expect(result).toContain(new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST));
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingLikes() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.addLike(new PublicApiPlaylist(1));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/likes/1");
    }

    @Test
    public void shouldNotifyContentObserverWhenRemovingLikes() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.removeLike(new PublicApiPlaylist(1L));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/likes");
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingReposts() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.addRepost(new PublicApiPlaylist(1L));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/reposts/1");
    }

    @Test
    public void shouldNotifyContentObserverWhenRemovingReposts() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        storage.removeRepost(new PublicApiPlaylist(1L));

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/reposts");
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingTrackCreation() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        PublicApiTrack track = new PublicApiTrack(1L);
        track.created_at = new Date();
        storage.addCreation(track);

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/sounds/1");
    }

    @Test
    public void shouldNotifyContentObserverWhenAddingPlaylistCreation() {
        ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();

        PublicApiPlaylist playlist = PublicApiPlaylist.newUserPlaylist(new PublicApiUser(1L), "playlist", false, Collections.<PublicApiTrack>emptyList());
        storage.addCreation(playlist);

        expect(contentResolver).toNotifyUri("content://com.soundcloud.android.provider.ScContentProvider/me/playlists/1");
    }

    @Test
    public void shouldReturnTrackLikesIds() throws CreateModelException {
        PublicApiTrack track1 = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        PublicApiTrack track2 = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        PublicApiPlaylist playlist = TestHelper.getModelFactory().createModel(PublicApiPlaylist.class);

        TestHelper.insertAsSoundAssociation(track1, SoundAssociation.Type.TRACK_LIKE);
        TestHelper.insertAsSoundAssociation(track2, SoundAssociation.Type.TRACK_LIKE);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_LIKE);

        expect(Sets.newHashSet(storage.getTrackLikesAsIds())).toContainExactly(track1.getId(), track2.getId());
    }

    private void insertSoundAssociations(PublicApiTrack track, PublicApiPlaylist playlist) {
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK);
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_REPOST);
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_REPOST);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_LIKE);
    }

    private SoundAssociation createAssociation(long id, Association.Type type) {
        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playable = new PublicApiTrack(id);
        soundAssociation1.setType(type.name());
        soundAssociation1.created_at = new Date(System.currentTimeMillis());
        return soundAssociation1;
    }

    private List<PublicApiTrack> createTracks(int n) {
        List<PublicApiTrack> items = new ArrayList<PublicApiTrack>(n);

        for (int i=0; i<n; i++) {
            PublicApiUser user = new PublicApiUser();
            user.permalink = "u"+i;
            user.setId(i);

            PublicApiTrack track = new PublicApiTrack();
            track.setId(i);
            track.user = user;
            items.add(track);
        }
        return items;
    }

}
