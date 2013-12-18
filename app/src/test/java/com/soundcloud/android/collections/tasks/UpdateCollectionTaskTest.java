package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.UserAdapter;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Request;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class UpdateCollectionTaskTest {

    @Test
    public void shouldUpdateListAdapterWithNewItemsAfterStaleCheck() throws IOException {
        List<ScResource> staleItems = new LinkedList<ScResource>();
        User updatedUser = new User(1);
        updatedUser.setCity("refreshed");
        staleItems.add(updatedUser);

        PublicCloudAPI api = mock(PublicCloudAPI.class);
        when(api.readList(any(Request.class))).thenReturn(staleItems);

        UpdateCollectionTask task = new UpdateCollectionTask(api, "/tracks", new HashSet<Long>());

        UserAdapter adapter = new UserAdapter(Content.USERS.uri);
        User existingUser = new User(1);
        SoundCloudApplication.sModelManager.cache(existingUser);
        adapter.addItems(Arrays.asList(existingUser));
        task.setAdapter(adapter);

        task.doInBackground();

        expect(adapter.getItem(0).getCity()).toEqual("refreshed");
    }

}
