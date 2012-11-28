package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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
        modelManager = SoundCloudApplication.MODEL_MANAGER; // new ScModelManager(Robolectric.application, AndroidCloudAPI.Mapper);
        associationManager = new AssociationManager(Robolectric.application);
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldAddLikeLikeSuccess() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        associationManager.addLike(t);
        expect(modelManager.getTrack(t.id).user_like).toBeTrue();
        // clear out state
        modelManager.clear();
        expect(modelManager.getTrack(t.id).user_like).toBeTrue();
    }

    @Test
    public void shouldNotChangeLikeStateWhenAddLikeApiCallFails() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(404, "FAIL"));

        associationManager.addLike(t);
        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
    }

    @Test
    public void shouldRemoveLikeStateOfSound() throws Exception {
        Track t = createTrack();
        t.user_like = true;
        modelManager.write(t);

        addHttpResponseRule("DELETE", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        associationManager.removeLike(t);
        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
        // clear out state
        modelManager.clear();
        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
    }

    @Test
    public void shouldAddRepost() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_REPOST, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.addRepost(t);
        expect(t.user_repost).toBeTrue();
        expect(modelManager.getTrack(t.id).user_repost).toBeTrue();

        // clear out cached state, read from db
        modelManager.clear();
        expect(modelManager.getTrack(t.id).user_repost).toBeTrue();
    }

    @Test
    public void shouldAddAndDeleteRepost() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_REPOST, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.addRepost(t);
        expect(modelManager.getTrack(t.id).user_repost).toBeTrue();

        addHttpResponseRule("DELETE", Request.to(TempEndpoints.e1.MY_REPOST, t.id).toUrl(), new TestHttpResponse(200, "OK"));
        associationManager.removeRepost(t);
        expect(t.user_repost).toBeFalse();
        expect(modelManager.getTrack(t.id).user_repost).toBeFalse();

        // clear out cached state, read from db
        modelManager.clear();
        expect(modelManager.getTrack(t.id).user_repost).toBeFalse();
    }


    @Test
    public void shouldNotChangeLikeStatusWhenRemoveApiCallFails() throws Exception {
        Track t = createTrack();
        t.user_like = true;
        modelManager.write(t);
        addHttpResponseRule("DELETE", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(400, "FAIL"));

        associationManager.removeLike(t);
        expect(t.user_like).toBeTrue();

        expect(modelManager.getTrack(t.id)).toBe(t);
    }

    private Track createTrack() {
        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        Track t = new Track();
        t.id = 200L;
        t.user = u1;
        return t;
    }
}
