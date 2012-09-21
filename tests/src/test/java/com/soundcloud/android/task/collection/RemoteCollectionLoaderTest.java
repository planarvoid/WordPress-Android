package com.soundcloud.android.task.collection;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class RemoteCollectionLoaderTest {

    private Request buildRequest(Uri contentUri){
        Content c = Content.match(contentUri);
        return c.request(contentUri).add("linked_partitioning", "1").add("limit", Consts.COLLECTION_PAGE_SIZE);
    }

    @Test
    public void shouldLoadTrackCollection() throws Exception {

        TestHelper.addCannedResponse(getClass(), "/users/123/favorites?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "me_favorites.json");

        final Uri contentUri1 = Content.USER_FAVORITES.forId(123l);
        ReturnData<ScResource> returnData = new RemoteCollectionLoader<ScResource>().load((AndroidCloudAPI) Robolectric.application,new CollectionParams<ScResource>(){
            {
                contentUri = contentUri1;
                request = buildRequest(contentUri1);
                loadModel = ScResource.class;
            }
        });

        expect(returnData.success).toBeTrue();
        expect(returnData.newItems).not.toBeEmpty();

    }

    @Test
    public void shouldLoadMixedCollection() throws Exception {

        final String url = "/search?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE;
        TestHelper.addCannedResponse(getClass(), url, "mixed.json");

        final Uri contentUri1 = Content.SEARCH.uri;
        ReturnData<ScResource> returnData = new RemoteCollectionLoader<ScResource>().load((AndroidCloudAPI) Robolectric.application, new CollectionParams<ScResource>() {
            {
                contentUri = contentUri1;
                request = new Request(url);
                loadModel = ScResource.class;
            }
        });

        expect(returnData.success).toBeTrue();
        expect(returnData.newItems).not.toBeEmpty();

        List<Track> t = new ArrayList<Track>();
        List<User> u = new ArrayList<User>();

        for (ScResource newItem : returnData.newItems){
            if (newItem instanceof Track){
                t.add((Track) newItem);
            } else if (newItem instanceof User){
                u.add((User) newItem);
            }
        }

        expect(t).not.toBeEmpty();
        expect(u).not.toBeEmpty();

    }
}
