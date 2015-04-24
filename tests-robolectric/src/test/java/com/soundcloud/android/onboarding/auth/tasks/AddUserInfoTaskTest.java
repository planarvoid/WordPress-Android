package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;

@RunWith(SoundCloudTestRunner.class)
public class AddUserInfoTaskTest  {

    @Mock private ApiClient apiClient;
    @Mock private UserStorage userStorage;
    @Mock private SoundCloudApplication application;
    @Mock private AccountOperations accountOperations;

    private PublicApiUser user;

    @Before
    public void setup() {
        user = ModelFixtures.create(PublicApiUser.class);
    }

    @Test
    public void shouldWorkWithNullFile() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.CURRENT_USER)),
                eq(PublicApiUser.class))).thenReturn(user);

        AddUserInfoTask task = new AddUserInfoTask(application, "name", null, userStorage, apiClient, accountOperations);
        AuthTaskResult result = task.doInBackground();
        expect(result.wasSuccess()).toBeTrue();
        expect(result.getUser().username).toEqual(user.getUsername());
    }

    @Test
    public void shouldWorkWithNonexistentFile() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.CURRENT_USER)),
                eq(PublicApiUser.class))).thenReturn(user);

        AddUserInfoTask task = new AddUserInfoTask(
                application, "name", new File("doesntexist"), userStorage, apiClient, accountOperations);
        AuthTaskResult result = task.doInBackground();
        expect(result.wasSuccess()).toBeTrue();
        expect(result.getUser().username).toEqual(user.getUsername());
    }

    @Test
    public void shouldWorkWithFile() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.CURRENT_USER)),
                eq(PublicApiUser.class))).thenReturn(user);

        File tmp = File.createTempFile("test", "tmp");
        AddUserInfoTask task = new AddUserInfoTask(application, "name", tmp, userStorage, apiClient, accountOperations);
        AuthTaskResult result = task.doInBackground();
        expect(result.wasSuccess()).toBeTrue();
        expect(result.getUser().username).toEqual(user.getUsername());
    }

    @Test
    public void shouldHandleBadEntity() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.CURRENT_USER)),
                eq(PublicApiUser.class))).thenThrow(TestApiResponses.validationError().getFailure());

        AddUserInfoTask task = new AddUserInfoTask(application, "name", null, userStorage, apiClient, accountOperations);
        AuthTaskResult result = task.doInBackground();
        expect(result.wasSuccess()).toBeFalse();
        expect(result.wasValidationError()).toBeTrue();
        expect(result.getUser()).toBeNull();
    }
}
