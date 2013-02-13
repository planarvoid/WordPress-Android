package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Endpoints;
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
        associationManager = new AssociationManager(Robolectric.application);
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldCreateNewLike() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(201, "OK"));

        int likesCount = modelManager.getTrack(t.id).likes_count;
        associationManager.setLike(t, true);
        expect(modelManager.getTrack(t.id).user_like).toBeTrue();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount + 1);
        // clear out state

        /*
        TODO: fix state bug

        modelManager.clear();
        expect(modelManager.getPlayable(t.id).user_like).toBeTrue();

        */
    }

    @Test
    public void shouldNotCreateLikeIfAlreadyLiked() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        int likesCount = modelManager.getTrack(t.id).likes_count;
        associationManager.setLike(t, true);
        expect(modelManager.getTrack(t.id).user_like).toBeTrue();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount);
    }

    @Test
    public void shouldNotChangeLikeCountWhenAddLikeApiCallFails() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(404, "FAIL"));

        int likesCount = modelManager.getTrack(t.id).likes_count;
        associationManager.setLike(t, true);
        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount);
    }

    @Test
    public void shouldRemoveLikeStateOfSound() throws Exception {
        Track t = createTrack();
        t.user_like = true;
        modelManager.write(t);

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        int likesCount = modelManager.getTrack(t.id).likes_count;
        associationManager.setLike(t, false);
        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(likesCount - 1);

        // clear out state

        /*

        TODO fix state bug

        modelManager.clear();
        expect(modelManager.getPlayable(t.id).user_like).toBeFalse();

        */
    }

    @Test
    public void shouldAddTrackRepost() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, t.id).toUrl(), new TestHttpResponse(201, "OK"));

        int repostsCount = modelManager.getTrack(t.id).reposts_count;
        associationManager.setRepost(t, true);
        expect(t.user_repost).toBeTrue();
        expect(modelManager.getTrack(t.id).user_repost).toBeTrue();
        expect(modelManager.getTrack(t.id).reposts_count).toEqual(repostsCount + 1);

        // clear out cached state, read from db

        /*
        TODO fix state bug

        modelManager.clear();
        expect(modelManager.getPlayable(t.id).user_repost).toBeTrue();
        */
    }

    @Test
    public void shouldAddPlaylistRepost() throws Exception {
        Playlist p = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        modelManager.write(p);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_PLAYLIST_REPOST, p.id).toUrl(), new TestHttpResponse(201, "OK"));

        int repostsCount = modelManager.getPlaylist(p.id).reposts_count;

        associationManager.setRepost(p, true);
        expect(p.user_repost).toBeTrue();
        expect(modelManager.getPlaylist(p.id).user_repost).toBeTrue();
        expect(modelManager.getPlaylist(p.id).reposts_count).toEqual(repostsCount + 1);
    }

    @Test
    public void shouldAddAndDeleteRepost() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, t.id).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(t, true);
        expect(modelManager.getTrack(t.id).user_repost).toBeTrue();
        expect(modelManager.getTrack(t.id).reposts_count).toEqual(6);

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setRepost(t, false);
        expect(t.user_repost).toBeFalse();
        expect(modelManager.getTrack(t.id).user_repost).toBeFalse();
        expect(modelManager.getTrack(t.id).reposts_count).toEqual(5);

        // clear out cached state, read from db
        /*
        TODO FIX state bug

        modelManager.clear();
        expect(modelManager.getPlayable(t.id).user_repost).toBeFalse();

        */
    }


    @Test
    public void shouldNotChangeLikeStatusWhenRemoveApiCallFails() throws Exception {
        Track t = createTrack();
        t.user_like = true;
        modelManager.write(t);
        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(400, "FAIL"));

        associationManager.setLike(t, false);
        expect(t.user_like).toBeTrue();
        expect(modelManager.getTrack(t.id).likes_count).toEqual(5);

        expect(modelManager.getTrack(t.id)).toBe(t);
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
