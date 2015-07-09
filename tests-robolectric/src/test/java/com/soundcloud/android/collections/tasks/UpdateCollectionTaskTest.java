package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.api.legacy.Request;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UpdateCollectionTaskTest {
    @Test
    public void shouldUpdateListAdapterWithNewItemsAfterStaleCheck() throws IOException {
        final PublicApiUser existingUser = new PublicApiUser(1);
        final ScBaseAdapter<PublicApiResource> adapter = createAdapterWithContent(existingUser);
        SoundCloudApplication.sModelManager.cache(existingUser);

        List<PublicApiResource> expectedItems = createStatleUserWithCity("refreshed");
        PublicApi api = mock(PublicApi.class);
        when(api.readList(any(Request.class))).thenReturn(expectedItems);

        UpdateCollectionTask task = new UpdateCollectionTask(api, "/tracks", new HashSet<Long>());
        task.setAdapter(adapter);
        task.doInBackground();

        expect(((PublicApiUser) adapter.getItem(0)).getCity()).toEqual("refreshed");
        expect(((PublicApiUser) adapter.getItem(0)).last_updated).toBeGreaterThan(0L);
    }

    private List<PublicApiResource> createStatleUserWithCity(String city) {
        List<PublicApiResource> staleItems = new LinkedList<>();
        PublicApiUser updatedUser = new PublicApiUser(1);
        updatedUser.setCity(city);
        staleItems.add(updatedUser);
        return staleItems;
    }

    private ScBaseAdapter<PublicApiResource> createAdapterWithContent(PublicApiResource user) {
        FakeAdapter adapter = new FakeAdapter(Content.USER.uri);
        adapter.addItems(Arrays.asList(user));
        return adapter;
    }

    private static class FakeAdapter extends ScBaseAdapter {

        public FakeAdapter(Uri uri) {
            super(uri);
        }

        @Override
        protected View createRow(Context context, int position, ViewGroup parent) {
            return null;
        }

        @Override
        public int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
            return 0;
        }
    }

}
