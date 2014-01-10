package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.createRegexRequestMatcherForUriWithClientId;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.TempEndpoints;
import com.soundcloud.android.associations.AssociationManager;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.activities.Activities;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import rx.Observer;

@RunWith(DefaultTestRunner.class)
public class AssociationManagerTest {
    AssociationManager associationManager;

    static final long USER_ID = 1L;

    @Before
    public void before() {
        associationManager = new AssociationManager(Robolectric.application,  SoundCloudApplication.sModelManager);
        TestHelper.setUserId(USER_ID);
    }

    @Test
    public void shouldCreateNewLike() throws Exception {
        Track t = createTrack();
        long likesCount = t.likes_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.getId()).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setLike(t, true, "screen_tag");

        expect(TestHelper.reload(t).user_like).toBeTrue();
        expect(TestHelper.reload(t).likes_count).toEqual(likesCount + 1);
    }

    @Test
    public void shouldCreateNewLikeWithoutLikesCountSet() throws Exception {
        Track t = createTrack();
        t.likes_count = Track.NOT_SET;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.getId()).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setLike(t, true, "screen_tag");
        expect(TestHelper.reload(t).user_like).toBeTrue();
    }

    @Test
    public void shouldNotChangeLikeCountIfAlreadyLiked() throws Exception {
        Track t = createTrack();
        TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE);
        expect(TestHelper.reload(t).user_like).toBeTrue();
        expect(TestHelper.reload(t).likes_count).toEqual(5L);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.getId()).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.setLike(t, true, "screen_tag");

        expect(TestHelper.reload(t).user_like).toBeTrue();
        expect(TestHelper.reload(t).likes_count).toEqual(5L);
    }

    @Test
    public void shouldNotChangeLikeCountWhenAddLikeApiCallFails() throws Exception {
        Track t = createTrack();
        t.user_like = false;
        long likesCount = t.likes_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.getId()).toUrl(), new TestHttpResponse(404, "FAIL"));
        associationManager.setLike(t, true, "screen_tag");

        expect(TestHelper.reload(t).user_like).toBeFalse();
        expect(TestHelper.reload(t).likes_count).toEqual(likesCount);
    }

    @Test
    public void shouldRemoveLikeStateOfSound() throws Exception {
        Track track = createTrack();
        TestHelper.insertAsSoundAssociation(track, SoundAssociation.Type.TRACK_LIKE);

        String trackLikeUrl = Request.to(TempEndpoints.e1.MY_TRACK_LIKE, track.getId()).toUrl();
        addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpDelete.METHOD_NAME, trackLikeUrl), new TestHttpResponse(200, "OK"));
        associationManager.setLike(track, false, "screen_tag");

        expect(TestHelper.reload(track).user_like).toBeFalse();
        expect(TestHelper.reload(track).likes_count).toEqual(4L);
    }

    @Test
    public void removingLikeForTrackShouldNotRemoteLikeForPlaylistWithSameId() {
        Track t = createTrack();
        TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE);

        Playlist p = new Playlist(t.getId());
        p.likes_count = 1;
        TestHelper.insertAsSoundAssociation(p, SoundAssociation.Type.PLAYLIST_LIKE);

        String trackLikeUrl = Request.to(TempEndpoints.e1.MY_TRACK_LIKE, t.getId()).toUrl();
        addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpDelete.METHOD_NAME, trackLikeUrl), new TestHttpResponse(200, "OK"));
        associationManager.setLike(t, false, "screen_tag");

        expect(TestHelper.reload(p).user_like).toBeTrue();
        expect(TestHelper.reload(p).likes_count).toEqual(1L);
    }

    @Test
    public void shouldAddTrackRepost() throws Exception {
        Track t = createTrack();
        expect(t.user_repost).toBeFalse();
        long repostsCount = t.reposts_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, t.getId()).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(t, true, "screen_tag");

        expect(t.user_repost).toBeTrue();
        expect(TestHelper.reload(t).user_repost).toBeTrue();
        expect(TestHelper.reload(t).reposts_count).toEqual(repostsCount + 1);
    }

    @Test
    public void shouldAddNewRepostWithoutRepostCountSet() throws Exception {
        Track t = createTrack();
        t.reposts_count = Track.NOT_SET;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, t.getId()).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(t, true, "screen_tag");

        expect(t.user_repost).toBeTrue();
        expect(TestHelper.reload(t).user_repost).toBeTrue();
    }

    @Test
    public void shouldAddPlaylistRepost() throws Exception {
        Playlist p = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/sync/playlist.json");
        TestHelper.insertWithDependencies(p);
        expect(p.user_repost).toBeFalse();
        long repostsCount = p.reposts_count;

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_PLAYLIST_REPOST, p.getId()).toUrl(), new TestHttpResponse(201, "OK"));
        associationManager.setRepost(p, true, "screen_tag");

        expect(p.user_repost).toBeTrue();
        expect(TestHelper.reload(p).user_repost).toBeTrue();
        expect(TestHelper.reload(p).reposts_count).toEqual(repostsCount + 1);
    }

    @Test
    public void shouldRemoveRepostActivity() throws Exception {
        Activities a = TestHelper.readJson(Activities.class, "/com/soundcloud/android/sync/e1_playlist_repost.json");
        a.get(0).getUser().setId(USER_ID); // needs to be the logged in user

        Playlist playlist = (Playlist) a.get(0).getPlayable();

        expect(new ActivitiesStorage().insert(Content.ME_SOUND_STREAM, a)).toBe(1);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_REPOST);
        expect(Content.ME_SOUND_STREAM).toHaveCount(1);

        String playlistRepostUrl = Request.to(TempEndpoints.e1.MY_PLAYLIST_REPOST, playlist.getId()).toUrl();
        addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpDelete.METHOD_NAME, playlistRepostUrl), new TestHttpResponse(200, "OK"));
        associationManager.setRepost(playlist, false, "screen_tag");
        expect(Content.ME_SOUND_STREAM).toHaveCount(0);
    }

    @Test
    public void shouldRemovePostActivityIfNoRepostCountAvailable() throws Exception {
        Activities a = TestHelper.readJson(Activities.class, "/com/soundcloud/android/sync/e1_playlist_repost.json");
        a.get(0).getUser().setId(USER_ID); // needs to be the logged in user

        Playlist playlist = (Playlist) a.get(0).getPlayable();
        playlist.reposts_count = Playlist.NOT_SET;

        expect(new ActivitiesStorage().insert(Content.ME_SOUND_STREAM, a)).toBe(1);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_REPOST);
        expect(Content.ME_SOUND_STREAM).toHaveCount(1);

        String playlistRepostUrl = Request.to(TempEndpoints.e1.MY_PLAYLIST_REPOST, playlist.getId()).toUrl();
        addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpDelete.METHOD_NAME, playlistRepostUrl), new TestHttpResponse(200, "OK"));
        associationManager.setRepost(playlist, false, "screen_tag");
        expect(Content.ME_SOUND_STREAM).toHaveCount(0);
    }

    @Test
    public void shouldPublishSocialEventWhenCreatingNewPlayableLike() throws Exception {
        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_LIKE, 1L).toUrl(), new TestHttpResponse(201, "OK"));

        Observer<SocialEvent> eventObserver = mock(Observer.class);
        Event.SOCIAL.subscribe(eventObserver);

        associationManager.setLike(new Track(1L), true, "screen_tag");

        ArgumentCaptor<SocialEvent> socialEvent = ArgumentCaptor.forClass(SocialEvent.class);
        verify(eventObserver).onNext(socialEvent.capture());
        expect(socialEvent.getValue().getType()).toBe(SocialEvent.TYPE_LIKE);
        expect(socialEvent.getValue().getAttributes().get("context")).toEqual("screen_tag");
    }

    @Test
    public void shouldPublishSocialEventWhenCreatingPlayableUnlike() throws Exception {
        String trackLikeUrl = Request.to(TempEndpoints.e1.MY_TRACK_LIKE, 1L).toUrl();
        addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpDelete.METHOD_NAME, trackLikeUrl), new TestHttpResponse(200, "OK"));

        Observer<SocialEvent> eventObserver = mock(Observer.class);
        Event.SOCIAL.subscribe(eventObserver);

        associationManager.setLike(new Track(1L), false, "screen_tag");

        ArgumentCaptor<SocialEvent> socialEvent = ArgumentCaptor.forClass(SocialEvent.class);
        verify(eventObserver).onNext(socialEvent.capture());
        expect(socialEvent.getValue().getType()).toBe(SocialEvent.TYPE_UNLIKE);
        expect(socialEvent.getValue().getAttributes().get("context")).toEqual("screen_tag");
    }

    @Test
    public void shouldPublishSocialEventWhenCreatingNewPlayableRepost() throws Exception {
        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_TRACK_REPOST, 1L).toUrl(), new TestHttpResponse(201, "OK"));

        Observer<SocialEvent> eventObserver = mock(Observer.class);
        Event.SOCIAL.subscribe(eventObserver);

        associationManager.setRepost(new Track(1L), true, "screen_tag");

        ArgumentCaptor<SocialEvent> socialEvent = ArgumentCaptor.forClass(SocialEvent.class);
        verify(eventObserver).onNext(socialEvent.capture());
        expect(socialEvent.getValue().getType()).toBe(SocialEvent.TYPE_REPOST);
        expect(socialEvent.getValue().getAttributes().get("context")).toEqual("screen_tag");
    }

    @Test
    public void shouldPublishSocialEventWhenCreatingPlayableUnrepost() throws Exception {
        String trackLikeUrl = Request.to(TempEndpoints.e1.MY_TRACK_REPOST, 1L).toUrl();
        addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpDelete.METHOD_NAME, trackLikeUrl), new TestHttpResponse(200, "OK"));

        Observer<SocialEvent> eventObserver = mock(Observer.class);
        Event.SOCIAL.subscribe(eventObserver);

        associationManager.setRepost(new Track(1L), false, "screen_tag");

        ArgumentCaptor<SocialEvent> socialEvent = ArgumentCaptor.forClass(SocialEvent.class);
        verify(eventObserver).onNext(socialEvent.capture());
        expect(socialEvent.getValue().getType()).toBe(SocialEvent.TYPE_UNREPOST);
        expect(socialEvent.getValue().getAttributes().get("context")).toEqual("screen_tag");
    }

    private Track createTrack() {
        User u1 = new User();
        u1.permalink = "u1";
        u1.setId(100L);

        Track t = new Track();
        t.setId(200L);
        t.user = u1;
        t.likes_count = t.reposts_count = 5;

        TestHelper.insertWithDependencies(t);

        return t;
    }

}
