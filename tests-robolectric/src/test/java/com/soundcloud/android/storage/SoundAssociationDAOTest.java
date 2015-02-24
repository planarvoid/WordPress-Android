package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes;
import static com.soundcloud.android.testsupport.TestHelper.readJson;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.SoundAssociationHolder;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.DatabaseFixtures;
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

    private final SQLiteDatabase database;
    private final DatabaseFixtures testFixtures;

    public SoundAssociationDAOTest() {
        super(new SoundAssociationDAO(Robolectric.application.getContentResolver()));
        DatabaseManager helper = DatabaseManager.getInstance(Robolectric.application);
        database = helper.getWritableDatabase();
        testFixtures = new DatabaseFixtures(database);
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
        Content.COLLECTION_ITEMS.table.insertOrReplace(database, cv);
        expect(resolver.query(Content.COLLECTION_ITEMS.uri, null, null, null, null).getCount()).toBe(1);
    }

    @Test
    public void shouldQueryForAll() {
        expect(getDAO().queryAll()).toNumber(0);

        TestHelper.insertAsSoundAssociation(new PublicApiTrack(TRACK_ID), SoundAssociation.Type.TRACK);
        TestHelper.insertAsSoundAssociation(new PublicApiTrack(TRACK_ID), SoundAssociation.Type.TRACK_REPOST);
        testFixtures.insertLikedTrack(new Date());

        TestHelper.insertAsSoundAssociation(new PublicApiPlaylist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST);
        TestHelper.insertAsSoundAssociation(new PublicApiPlaylist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_REPOST);
        testFixtures.insertLikedPlaylist(new Date());

        expect(getDAO().queryAll().size()).toBe(6);
    }

    @Test
    public void shouldInsertOwnTrack() {
        expect(Content.ME_SOUNDS).toHaveCount(0);

        PublicApiTrack track = new PublicApiTrack(1);
        SoundAssociation sa = new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK);
        sa.owner = new PublicApiUser(USER_ID);
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

        PublicApiPlaylist playlist = new PublicApiPlaylist(1);
        SoundAssociation sa = new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST);
        sa.owner = new PublicApiUser(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_SOUNDS).toHaveCount(1);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, playlist.getId());
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.PLAYLIST);
        expect(Content.ME_SOUNDS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldFindLikeForTrack() {
        expect(Content.ME_LIKES).toHaveCount(0);

        ApiTrack track = testFixtures.insertLikedTrack(new Date());

        expect(Content.ME_LIKES).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, track.getId());
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.LIKE);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_TRACK);
    }

    @Test
    public void shouldInsertRepostForTrack() {
        expect(Content.ME_REPOSTS).toHaveCount(0);

        PublicApiTrack track = new PublicApiTrack(1);
        SoundAssociation sa = new SoundAssociation(track, new Date(), SoundAssociation.Type.TRACK_REPOST);
        sa.owner = new PublicApiUser(USER_ID);
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

        ApiPlaylist playlist = testFixtures.insertLikedPlaylist(new Date());

        expect(Content.ME_LIKES).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, playlist.getId());
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.LIKE);
        expect(Content.ME_LIKES).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldInsertRepostForPlaylist() {
        expect(Content.ME_REPOSTS).toHaveCount(0);

        PublicApiPlaylist playlist = new PublicApiPlaylist(1);
        SoundAssociation sa = new SoundAssociation(playlist, new Date(), SoundAssociation.Type.PLAYLIST_REPOST);
        sa.owner = new PublicApiUser(USER_ID);
        getDAO().create(sa);

        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._ID, playlist.getId());
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_OWNER_ID, USER_ID);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE, CollectionItemTypes.REPOST);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldRemoveRepostForTrack() {
        SoundAssociation trackRepost = TestHelper.insertAsSoundAssociation(new PublicApiTrack(TRACK_ID), SoundAssociation.Type.TRACK_REPOST);
        TestHelper.insertAsSoundAssociation(new PublicApiPlaylist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_REPOST);
        expect(Content.ME_REPOSTS).toHaveCount(2);

        expect(getDAO().delete(trackRepost)).toBeTrue();

        expect(Content.TRACKS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_PLAYLIST);
    }

    @Test
    public void shouldRemoveRepostForPlaylist() {
        SoundAssociation playlistRepost = TestHelper.insertAsSoundAssociation(new PublicApiPlaylist(PLAYLIST_ID), SoundAssociation.Type.PLAYLIST_REPOST);
        TestHelper.insertAsSoundAssociation(new PublicApiTrack(TRACK_ID), SoundAssociation.Type.TRACK_REPOST);
        expect(Content.ME_REPOSTS).toHaveCount(2);

        expect(getDAO().delete(playlistRepost)).toBeTrue();

        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveColumnAt(0, TableColumns.SoundAssociationView._TYPE, Playable.DB_TYPE_TRACK);
    }

    @Test
    public void shouldInsertAndQueryLikes() throws Exception {
        final ApiTrack track = testFixtures.insertLikedTrack(new Date(100));
        final ApiPlaylist playlist1 = testFixtures.insertLikedPlaylist(new Date(200));
        final ApiPlaylist playlist2 = testFixtures.insertLikedPlaylist(new Date(300));

        List<SoundAssociation> likes = getDAO().queryAll();
        expect(likes).toNumber(3);

        expect(likes.get(0).getPlayable().getId()).toEqual(playlist2.getId());
        expect(likes.get(1).getPlayable().getId()).toEqual(playlist1.getId());
        expect(likes.get(2).getPlayable().getId()).toEqual(track.getId());
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
}
