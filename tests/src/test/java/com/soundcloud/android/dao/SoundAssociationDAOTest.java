package com.soundcloud.android.dao;

import com.soundcloud.android.model.Like;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.SoundAssociationTest;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes;
import static com.soundcloud.android.robolectric.TestHelper.readJson;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationDAOTest extends AbstractDAOTest<SoundAssociationDAO> {

    private static final long USER_ID = 100L;
    private static final long TRACK_ID = 1L;
    private static final long PLAYLIST_ID = 2L;

    private SQLiteDatabase database;

    public SoundAssociationDAOTest() {
        super(new SoundAssociationDAO(Robolectric.application.getContentResolver()));
        DBHelper helper = new DBHelper(DefaultTestRunner.application);
        database = helper.getWritableDatabase();
    }

    @Before
    public void before() {
        super.before();
        DefaultTestRunner.application.setCurrentUserId(100L);

        // add a collection item that is not a SoundAssociation, so that we can test for
        // proper separation of other data stored in the Collections table
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CollectionItems.ITEM_ID, 1L);
        cv.put(DBHelper.CollectionItems.USER_ID, USER_ID);
        cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, CollectionItemTypes.FRIEND);
        DBHelper helper = new DBHelper(DefaultTestRunner.application);
        Content.COLLECTION_ITEMS.table.insertOrReplace(helper.getWritableDatabase(), cv);
        expect(resolver.query(Content.COLLECTION_ITEMS.uri, null, null, null, null).getCount()).toBe(1);

        insertTrack();
        insertPlaylist();
    }

    @Test
    public void shouldQueryForAll() {
        expect(getDAO().queryAll()).toNumber(0);

        insertTrackCreation();
        insertPlaylistCreation();
        insertTrackLike();
        insertPlaylistLike();
        insertTrackRepost();
        insertPlaylistRepost();

        expect(getDAO().queryAll()).toNumber(6);
    }

    @Test
    public void shouldInsertAndQueryLikes() throws Exception {
        SoundAssociationHolder holder = readJson(SoundAssociationHolder.class,
                "/com/soundcloud/android/service/sync/e1_likes.json");

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
                "/com/soundcloud/android/service/sync/e1_likes.json");

        // 3 likes, 12 dependencies
        expect(getDAO().createCollection(holder.collection)).toEqual(3 + 12);

        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(3);

        List<Like> likes = new ArrayList<Like>();
        while (c.moveToNext()) {
            Like like = new Like(c);
            likes.add(like);
        }
        expect(resolver.delete(Content.ME_LIKES.uri, DBHelper.CollectionItems.ITEM_ID + " = ?",
                new String[]{String.valueOf(likes.get(0).getPlayable().id)})).toEqual(1);

        c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(2);
    }

    @Test
    public void shouldQuerySounds() throws Exception {
        SoundAssociationHolder holder = readJson(SoundAssociationHolder.class,
                "/com/soundcloud/android/provider/e1_sounds.json");

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
        final ScModelManager manager = DefaultTestRunner.application.MODEL_MANAGER;

        SoundAssociationHolder sounds  = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sounds_with_sets.json"),
                SoundAssociationHolder.class);

        expect(sounds).not.toBeNull();

        expect(sounds.size()).toEqual(41);


        expect(manager.writeCollection(sounds, ScResource.CacheUpdateMode.NONE)).toEqual(41); // 38 tracks, 3 sets


        expect(SoundCloudDB.getStoredIds(DefaultTestRunner.application.getContentResolver(),
                Content.ME_SOUNDS.uri, 0, 50).size()).toEqual(41);

        CollectionHolder<SoundAssociation> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(
                DefaultTestRunner.application.getContentResolver(), SoundAssociation.class, Content.ME_SOUNDS.uri);

        expect(newItems.size()).toEqual(41);

        expect(Content.ME_PLAYLISTS).toHaveCount(2); // does not include the repost

    }


    @Test
    public void shouldInsertNewSoundAssociation() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);
        ScModelManager manager = DefaultTestRunner.application.MODEL_MANAGER;

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
        Content.SOUNDS.table.insertOrReplace(database, track.buildContentValues());
    }

    private void insertPlaylist() {
        Playlist playlist = new Playlist(PLAYLIST_ID);
        playlist.user_id = USER_ID;
        Content.SOUNDS.table.insertOrReplace(database, playlist.buildContentValues());
    }

    private void insertSoundAssociation(int associationType, long itemId, int resType) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CollectionItems.USER_ID, USER_ID);
        cv.put(DBHelper.CollectionItems.ITEM_ID, itemId);
        cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, associationType);
        cv.put(DBHelper.CollectionItems.RESOURCE_TYPE, resType);
        Content.COLLECTION_ITEMS.table.insertOrReplace(database, cv);
    }

    private void insertPlaylistRepost() {
        insertSoundAssociation(CollectionItemTypes.REPOST, PLAYLIST_ID, Playable.DB_TYPE_PLAYLIST);
    }

    private void insertTrackRepost() {
        insertSoundAssociation(CollectionItemTypes.REPOST, TRACK_ID, Playable.DB_TYPE_TRACK);
    }

    private void insertPlaylistLike() {
        insertSoundAssociation(CollectionItemTypes.LIKE, PLAYLIST_ID, Playable.DB_TYPE_PLAYLIST);
    }

    private void insertTrackLike() {
        insertSoundAssociation(CollectionItemTypes.LIKE, TRACK_ID, Playable.DB_TYPE_TRACK);
    }

    private void insertPlaylistCreation() {
        insertSoundAssociation(CollectionItemTypes.PLAYLIST, PLAYLIST_ID, Playable.DB_TYPE_PLAYLIST);
    }

    private void insertTrackCreation() {
        insertSoundAssociation(CollectionItemTypes.TRACK, TRACK_ID, Playable.DB_TYPE_TRACK);
    }
}
