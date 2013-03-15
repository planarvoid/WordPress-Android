package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncServiceTest;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class AssociationManagerTest {
    AssociationManager associationManager;
    ScModelManager modelManager;
    static final long USER_ID = 1L;

    @Before
    public void before() {
        modelManager = SoundCloudApplication.MODEL_MANAGER;
        associationManager = new AssociationManager(Robolectric.application, modelManager);
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldCreateNewLike() throws Exception {
        Track t = createTrack();
        int likesCount = t.likes_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setLike(t, true);

        expect(modelManager.getTrack(t.id).user_like).toBeTrue();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount + 1);

        Track trackFromDb = (Track) modelManager.getModel(t.toUri(), null);
        expect(trackFromDb.user_like).toBeTrue();
        expect(trackFromDb.likes_count).toEqual(likesCount + 1);
    }

    @Test
    public void shouldNotChangeLikeCountIfAlreadyLiked() throws Exception {
        Track t = createTrack();
        t.user_like = true;
        int likesCount = t.likes_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setLike(t, true);

        expect(modelManager.getTrack(t.id).user_like).toBeTrue();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount);

        Track trackFromDb = (Track) modelManager.getModel(t.toUri(), null);
        expect(trackFromDb.user_like).toBeTrue();
        expect(trackFromDb.likes_count).toEqual(likesCount);
    }

    @Test
    public void shouldNotChangeLikeCountWhenAddLikeApiCallFails() throws Exception {
        Track t = createTrack();
        t.user_like = false;
        int likesCount = t.likes_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(404, "FAIL"));
        associationManager.setLike(t, true);

        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount);

        Track trackFromDb = (Track) modelManager.getModel(t.toUri(), null);
        expect(trackFromDb.user_like).toBeFalse();
        expect(trackFromDb.likes_count).toEqual(likesCount);
    }

    @Test
    public void shouldRemoveLikeStateOfSound() throws Exception {
        Track t = createTrack();
        int likesCount = t.likes_count;

        DefaultTestRunner.application.getContentResolver().insert(Content.ME_LIKES.uri, t.buildContentValues());

        expect(((Track) modelManager.getModel(t.toUri(), null)).user_like).toBeTrue(); // make sure db has liked state

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setLike(t, false);

        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount - 1);

        Track trackFromDb = (Track) modelManager.getModel(t.toUri(), null);
        expect(trackFromDb.user_like).toBeFalse();
        expect(trackFromDb.likes_count).toEqual(likesCount - 1);
    }

    @Test
    public void removingLikeForTrackShouldNotRemoteLikeForPlaylistWithSameId() {
        Track t = createTrack();
        DefaultTestRunner.application.getContentResolver().insert(Content.ME_LIKES.uri, t.buildContentValues());

        Playlist p = new Playlist(t.id);
        p.likes_count = 1;
        DefaultTestRunner.application.getContentResolver().insert(Content.ME_LIKES.uri, p.buildContentValues());

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setLike(t, false);

        expect(modelManager.getPlaylist(p.id).user_like).toBeTrue();
        expect(modelManager.getPlaylist(p.id).likes_count).toEqual(1);

        Playlist playlistFromDb = (Playlist) modelManager.getModel(p.toUri(), null);
        expect(playlistFromDb.user_like).toBeTrue();
        expect(playlistFromDb.likes_count).toEqual(1);
    }

    @Test
    public void shouldAddTrackRepost() throws Exception {
        Track t = createTrack();
        expect(t.user_repost).toBeFalse();
        int repostsCount = t.reposts_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, t.id).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(t, true);

        expect(t.user_repost).toBeTrue();
        expect(modelManager.getTrack(t.id).user_repost).toBeTrue();
        expect(modelManager.getTrack(t.id).reposts_count).toEqual(repostsCount + 1);

        Track trackFromDb = (Track) modelManager.getModel(t.toUri(), null);
        expect(trackFromDb.user_repost).toBeTrue();
        expect(trackFromDb.reposts_count).toEqual(repostsCount + 1);
    }

    @Test
    public void shouldAddPlaylistRepost() throws Exception {
        Playlist p = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        expect(p.user_repost).toBeFalse();
        int repostsCount = p.reposts_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_PLAYLIST_REPOST, p.id).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(p, true);

        expect(p.user_repost).toBeTrue();
        expect(modelManager.getPlaylist(p.id).user_repost).toBeTrue();
        expect(modelManager.getPlaylist(p.id).reposts_count).toEqual(repostsCount + 1);

        Playlist playlistFromDb = (Playlist) modelManager.getModel(p.toUri(), null);
        expect(playlistFromDb.user_repost).toBeTrue();
        expect(playlistFromDb.reposts_count).toEqual(repostsCount + 1);
    }

    @Test
    public void shouldRemoveRepostActivity() throws Exception {
        Activities a = modelManager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_playlist_repost.json"), false);
        a.get(0).getUser().id = USER_ID; // needs to be the logged in user

        Playlist playlist = (Playlist) a.get(0).getPlayable();

        expect(new ActivitiesStorage(Robolectric.application.getContentResolver()).insert(Content.ME_SOUND_STREAM, a)).toBe(1);
        DefaultTestRunner.application.getContentResolver().insert(Content.ME_REPOSTS.uri, playlist.buildContentValues());
        expect(Content.ME_SOUND_STREAM).toHaveCount(1);

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_PLAYLIST_REPOST, playlist.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setRepost(playlist, false);
        expect(Content.ME_SOUND_STREAM).toHaveCount(0);
    }

    private Track createTrack() {
        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        Track t = new Track();
        t.id = 200L;
        t.user = u1;
        t.likes_count = t.reposts_count = 5;
        return t;
    }

}
