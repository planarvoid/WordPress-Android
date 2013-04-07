package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class AssociationManagerTest {
    AssociationManager associationManager;

    static final long USER_ID = 1L;

    @Before
    public void before() {
        associationManager = new AssociationManager(Robolectric.application,  SoundCloudApplication.MODEL_MANAGER);
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldCreateNewLike() throws Exception {
        Track t = createTrack();
        int likesCount = t.likes_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setLike(t, true);

        expect(TestHelper.reload(t).user_like).toBeTrue();
        expect(TestHelper.reload(t).likes_count).toEqual(likesCount + 1);
    }

    @Test
    public void shouldNotChangeLikeCountIfAlreadyLiked() throws Exception {
        Track t = createTrack();
        TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE);
        expect(TestHelper.reload(t).user_like).toBeTrue();
        expect(TestHelper.reload(t).likes_count).toEqual(5);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setLike(t, true);

        expect(TestHelper.reload(t).user_like).toBeTrue();
        expect(TestHelper.reload(t).likes_count).toEqual(5);
    }

    @Test
    public void shouldNotChangeLikeCountWhenAddLikeApiCallFails() throws Exception {
        Track t = createTrack();
        t.user_like = false;
        int likesCount = t.likes_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(404, "FAIL"));
        associationManager.setLike(t, true);

        expect(TestHelper.reload(t).user_like).toBeFalse();
        expect(TestHelper.reload(t).likes_count).toEqual(likesCount);
    }

    @Test
    public void shouldRemoveLikeStateOfSound() throws Exception {
        Track track = createTrack();
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, track.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setLike(track, false);

        expect(TestHelper.reload(track).user_like).toBeFalse();
        expect(TestHelper.reload(track).likes_count).toEqual(4);
    }

    @Test
    public void removingLikeForTrackShouldNotRemoteLikeForPlaylistWithSameId() {
        Track t = createTrack();
        TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE);

        Playlist p = new Playlist(t.id);
        p.likes_count = 1;
        TestHelper.insertAsSoundAssociation(p, SoundAssociation.Type.PLAYLIST_LIKE);

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setLike(t, false);

        expect(TestHelper.reload(p).user_like).toBeTrue();
        expect(TestHelper.reload(p).likes_count).toEqual(1);
    }

    @Test
    public void shouldAddTrackRepost() throws Exception {
        Track t = createTrack();
        expect(t.user_repost).toBeFalse();
        int repostsCount = t.reposts_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, t.id).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(t, true);

        expect(t.user_repost).toBeTrue();
        expect(TestHelper.reload(t).user_repost).toBeTrue();
        expect(TestHelper.reload(t).reposts_count).toEqual(repostsCount + 1);
    }

    @Test
    public void shouldAddPlaylistRepost() throws Exception {
        Playlist p = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        TestHelper.insertWithDependencies(p);
        expect(p.user_repost).toBeFalse();
        int repostsCount = p.reposts_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_PLAYLIST_REPOST, p.id).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(p, true);

        expect(p.user_repost).toBeTrue();
        expect(TestHelper.reload(p).user_repost).toBeTrue();
        expect(TestHelper.reload(p).reposts_count).toEqual(repostsCount + 1);
    }

    @Test
    public void shouldRemoveRepostActivity() throws Exception {
        Activities a = TestHelper.readJson(Activities.class, "/com/soundcloud/android/service/sync/e1_playlist_repost.json");

        a.get(0).getUser().id = USER_ID; // needs to be the logged in user

        Playlist playlist = (Playlist) a.get(0).getPlayable();

        expect(new ActivitiesStorage(Robolectric.application).insert(Content.ME_SOUND_STREAM, a)).toBe(1);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_REPOST);
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

        TestHelper.insertWithDependencies(t);

        return t;
    }

}
