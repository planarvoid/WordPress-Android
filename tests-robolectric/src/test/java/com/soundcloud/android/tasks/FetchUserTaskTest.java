package com.soundcloud.android.tasks;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;


@RunWith(DefaultTestRunner.class)
public class FetchUserTaskTest {
    @Mock ApiClient apiClient;

    @Test
    public void fetchLoadUserInfo() throws Exception {
        PublicApiUser user = ModelFixtures.create(PublicApiUser.class);

        when(apiClient.fetchMappedResponse(argThat(isPublicApiRequestTo("GET", ApiEndpoints.LEGACY_USER.path(12345))), eq(PublicApiUser.class)))
                .thenReturn(user);

        FetchUserTask task = new FetchUserTask(apiClient);

        final PublicApiUser[] users = {null};
        FetchModelTask.Listener<PublicApiUser> listener = new FetchModelTask.Listener<PublicApiUser>() {
            @Override
            public void onSuccess(PublicApiUser u) {
                users[0] = u;
            }

            @Override
            public void onError(Object context) {
            }
        };

        task.addListener(listener);
        task.execute(12345);
        expect(users[0]).not.toBeNull();
        expect(users[0].username).toEqual(user.username);
        expect(users[0].isPrimaryEmailConfirmed()).toBe(user.isPrimaryEmailConfirmed());

        PublicApiUser u = new LegacyUserStorage().getUser(user.getId());
        expect(u).not.toBeNull();
        expect(u.username).toEqual(user.username);
    }
}
