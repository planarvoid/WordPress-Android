package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes;
import static com.soundcloud.android.robolectric.TestHelper.readJson;

import com.soundcloud.android.model.Like;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationDAOTest extends AbstractDAOTest<SoundAssociationDAO> {

    private static final long USER_ID = 100L;
    private static final long TRACK_ID = 1L;
    private static final long PLAYLIST_ID = 2L;

    private SQLiteDatabase database;

    public SoundAssociationDAOTest() {
        super(new SoundAssociationDAO(Robolectric.application.getContentResolver()));
        DatabaseManager helper = new DatabaseManager(DefaultTestRunner.application);
        database = helper.getWritableDatabase();
    }

    @Before
    public void before() {
        super.before();
        TestHelper.setUserId(100L);

        // add a collection item that is not a SoundAssociation, so that we can test for
        // proper separation of other data stored in the CollectionItems table
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.CollectionItems.ITEM_ID, 1L);
        cv.put(TableColumns.CollectionItems.USER_ID, USER_ID);
        cv.put(TableColumns.CollectionItems.COLLECTION_TYPE, CollectionItemTypes.FRIEND);
        DatabaseManager helper = new DatabaseManager(DefaultTestRunner.application);
        Content.COLLECTION_ITEMS.table.insertOrReplace(helper.getWritableDatabase(), cv);
        expect(resolver.query(Content.COLLECTION_ITEMS.uri, null, null, null, null).getCount()).toBe(1);

        insertTrack();
        insertPlaylist();
    }

    @Test
    public void shouldQueryForAll() {
        expect(getDAO().queryAll()).toNumber(0);

        TestHelper.insertAsSoundAssociation(new Track(TRACK_ID), SoundAssociation.Type.TRACK);
        TestHelper.insertAsSoundAssociation(new Track(TRACK_ID), SoundAssociation.Type.TRACK_LIKE);
        TestHelper.insertAsSoundAssociation(new Track(TRACK_ID), SoundAssociation.Type.TRACK_REPOST);

        TestHelper.insertAsSoundAssociation(new Playlist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST);
        TestHelper.insertAsSoundAssociation(new Playlist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_LIKE);
        TestHelper.insertAsSoundAssociation(new Playlist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_REPOST);

        expect(getDAO().queryAll()).toNumber(6);
    }

    @Test
    public void shouldInsertOwnTrack() {
        expect(Content.ME_SOUNDS).toHaveCount(0);

        Track track = new Track(1);
        SoundAssociation sa = new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK);
        sa.owner = new User(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_SOUNDS).toHaveCount(1);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, track.getId());
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.TRACK);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_TRACK);
    }

    @Test
    public void shouldInsertOwnPlaylist() {
        expect(Content.ME_SOUNDS).toHaveCount(0);

        Playlist playlist = new Playlist(1);
        SoundAssociation sa = new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST);
        sa.owner = new User(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_SOUNDS).toHaveCount(1);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, playlist.getId());
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.PLAYLIST);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldInsertLikeForTrack() {
        expect(Content.ME_LIKES).toHaveCount(0);

        Track track = new Track(1);
        SoundAssociation sa = new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK_LIKE);
        sa.owner = new User(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_LIKES).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, track.getId());
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.LIKE);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_TRACK);
    }

    @Test
    public void shouldInsertRepostForTrack() {
        expect(Content.ME_REPOSTS).toHaveCount(0);

        Track track = new Track(1);
        SoundAssociation sa = new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK_REPOST);
        sa.owner = new User(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, track.getId());
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.REPOST);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_TRACK);
    }

    @Test
    public void shouldInsertLikeForPlaylist() {
        expect(Content.ME_LIKES).toHaveCount(0);

        Playlist playlist = new Playlist(1);
        SoundAssociation sa = new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST_LIKE);
        sa.owner = new User(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_LIKES).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, playlist.getId());
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.LIKE);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldInsertRepostForPlaylist() {
        expect(Content.ME_REPOSTS).toHaveCount(0);

        Playlist playlist = new Playlist(1);
        SoundAssociation sa = new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST_REPOST);
        sa.owner = new User(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, playlist.getId());
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.REPOST);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldRemoveLikeForTrack() {
        SoundAssociation trackLike = TestHelper.insertAsSoundAssociation(new Track(TRACK_ID), SoundAssociation.Type.TRACK_LIKE);
        TestHelper.insertAsSoundAssociation(new Playlist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_LIKE);
        expect(Content.ME_LIKES).toHaveCount(2);

        expect(getDAO().delete(trackLike)).toBeTrue();

        expect(Content.TRACKS).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldRemoveRepostForTrack() {
        SoundAssociation trackRepost = TestHelper.insertAsSoundAssociation(new Track(TRACK_ID), SoundAssociation.Type.TRACK_REPOST);
        TestHelper.insertAsSoundAssociation(new Playlist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_REPOST);
        expect(Content.ME_REPOSTS).toHaveCount(2);

        expect(getDAO().delete(trackRepost)).toBeTrue();

        expect(Content.TRACKS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldRemoveLikeForPlaylist() {
        SoundAssociation playlistLike = TestHelper.insertAsSoundAssociation(new Playlist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_LIKE);
        TestHelper.insertAsSoundAssociation(new Track(TRACK_ID), SoundAssociation.Type.TRACK_LIKE);
        expect(Content.ME_LIKES).toHaveCount(2);

        expect(getDAO().delete(playlistLike)).toBeTrue();

        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_TRACK);
    }

    @Test
    public void shouldRemoveRepostForPlaylist() {
        SoundAssociation playlistRepost = TestHelper.insertAsSoundAssociation(new Playlist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_REPOST);
        TestHelper.insertAsSoundAssociation(new Track(TRACK_ID), SoundAssociation.Type.TRACK_REPOST);
        expect(Content.ME_REPOSTS).toHaveCount(2);

        expect(getDAO().delete(playlistRepost)).toBeTrue();

        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_TRACK);
    }

    @Test
    public void shouldInsertAndQueryLikes() throws Exception {
        SoundAssociationHolder holder = readJson(SoundAssociationHolder.class,
                "/com/soundcloud/android/sync/e1_likes.json");

        // 3 likes, 12 dependencies
        expect(getDAO().createCollection(holder.collection)).toEqual(3 + 12);

        List<SoundAssociation> likes = getDAO().queryAll();
        expect(likes).toNumber(3);

        expect(likes.get(0).getPlayable().title).toEqual("LOL");
        expect(likes.get(1).getPlayable().title).toEqual("freddie evans at Excel Exhibition Centre");
    }

    @Test
    public void shouldInsertQueryAndDeleteLikes() throws Exception {
        SoundAssociationHolder holder = readJson(SoundAssociationHolder.class,
                "/com/soundcloud/android/sync/e1_likes.json");

        // 3 likes, 12 dependencies
        expect(getDAO().createCollection(holder.collection)).toEqual(3 + 12);

        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(3);

        List<Like> likes = new ArrayList<Like>();
        while (c.moveToNext()) {
            Like like = new Like(c);
            likes.add(like);
        }
        expect(resolver.delete(Content.ME_LIKES.uri, TableColumns.CollectionItems.ITEM_ID + " = ?",
                new String[]{String.valueOf(likes.get(0).getPlayable().getId())})).toEqual(1);

        c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(2);
    }

    @Test
    public void shouldQuerySounds() throws Exception {
        SoundAssociationHolder holder = readJson(SoundAssociationHolder.class,
                "/com/soundcloud/android/storage/provider/e1_sounds.json");

        // 50 sounds, 100 dependencies
        expect(getDAO().createCollection(holder.collection)).toEqual(50 + 100);

        Cursor c = resolver.query(Content.ME_SOUNDS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(50);

        List<SoundAssociation> associations = new ArrayList<SoundAssociation>();
        while (c.moveToNext()) {
            associations.add(new SoundAssociation(c));
        }
        expect(associations).toNumber(50);

        expect(associations.get(0).getPlayable().title).toEqual("A trimmed test upload");
        expect(associations.get(1).getPlayable().title).toEqual("A faded + trimmed test upload");
    }


    /*
    @Test
    public void shouldPersistStreamItems() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        final ScModelManager manager = DefaultTestRunner.application.sModelManager;

        SoundAssociationHolder sounds  = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds_with_sets.json"),
                SoundAssociationHolder.class);

        expect(sounds).not.toBeNull();

        expect(sounds.size()).toEqual(41);


        expect(manager.writeCollection(sounds, ScResource.CacheUpdateMode.NONE)).toEqual(41); // 38 tracks, 3 sets


        expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                Content.ME_SOUNDS.uri, 0, 50).size()).toEqual(41);

        CollectionHolder<SoundAssociation> newItems = SoundCloudApplication.sModelManager.loadLocalContent(
                DefaultTestRunner.application.getContentResolver(), SoundAssociation.class, Content.ME_SOUNDS.uri);

        expect(newItems.size()).toEqual(41);

        expect(Content.ME_PLAYLISTS).toHaveCount(2); // does not include the repost

    }


    @Test
    public void shouldInsertNewSoundAssociation() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        ScModelManager manager = DefaultTestRunner.application.sModelManager;

        //initial population
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds.json"),
                SoundAssociationHolder.class);

        expect(manager.writeCollection(old, ScResource.CacheUpdateMode.NONE)).toEqual(38); // 38 tracks and 3 diff users

        Playlist p = manager.getModelFromStream(SyncAdapterServiceTest.class.getResourceAsStream("playlist.json"));
        SoundAssociation soundAssociation1 = new SoundAssociation(p, new Date(System.currentTimeMillis()),SoundAssociation.Type.PLAYLIST);

        final Uri uri = soundAssociation1.insert(DefaultTestRunner.application.getContentResolver(),Content.ME_SOUNDS.uri);
        expect(uri).toEqual(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/me/sounds/39"));
    }
    */

    private void insertTrack() {
        Track track = new Track(TRACK_ID);
        track.user_id = USER_ID;
        TestHelper.insertWithDependencies(track);
        expect(Content.TRACKS).toHaveCount(1);
    }

    private void insertPlaylist() {
        Playlist playlist = new Playlist(PLAYLIST_ID);
        playlist.user_id = USER_ID;
        TestHelper.insertWithDependencies(playlist);
        expect(Content.PLAYLISTS).toHaveCount(1);
    }
}
