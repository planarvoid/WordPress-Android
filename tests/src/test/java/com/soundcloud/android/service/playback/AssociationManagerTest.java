package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;

@RunWith(DefaultTestRunner.class)
public class AssociationManagerTest {
    AssociationManager associationManager;
    ScModelManager modelManager;
    static final long USER_ID = 1L;

    @Before
    public void before() {
        associationManager = new AssociationManager(Robolectric.application);
        modelManager = new ScModelManager(Robolectric.application, AndroidCloudAPI.Mapper);
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldLike() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        Robolectric.addHttpResponseRule("PUT", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        associationManager.addLike(t);
        expect(modelManager.getTrack(t.id).user_like).toBeTrue();
    }

    @Test
    public void shouldFailToLike() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        Robolectric.addHttpResponseRule("PUT", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(404, "FAIL"));

        associationManager.addLike(t);
        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
    }

    @Test
    public void shouldRemoveLike() throws Exception {
        Track t = createTrack();
        t.user_like = true;
        modelManager.write(t);

        Robolectric.addHttpResponseRule("DELETE", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        associationManager.removeLike(t);
        expect(modelManager.getTrack(t.id).user_like).toBeFalse();
    }

    @Test
    public void shouldFailToRemoveLike() throws Exception {
        Track t = createTrack();
        t.user_like = true;
        modelManager.write(t);
        Robolectric.addHttpResponseRule("DELETE", Request.to(Endpoints.MY_FAVORITE, t.id).toUrl(), new TestHttpResponse(400, "FAIL"));

        associationManager.removeLike(t);
        expect(t.user_like).toBeTrue();

        expect(modelManager.getTrack(t.id)).toBe(t);
    }

    @Test
    public void shouldRepost() throws Exception {
        Track t = createTrack();
        modelManager.write(t);

        Robolectric.addHttpResponseRule("PUT", Request.to(TempEndpoints.e1.MY_REPOST, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        associationManager.addRepost(t);
        expect(modelManager.getTrack(t.id).user_repost).toBeTrue();

        Robolectric.addHttpResponseRule("DELETE",  Request.to(TempEndpoints.e1.MY_REPOST, t.id).toUrl(), new TestHttpResponse(200, "OK"));

        associationManager.removeRepost(t);
        expect(modelManager.getTrack(t.id).user_repost).toBeFalse();
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
