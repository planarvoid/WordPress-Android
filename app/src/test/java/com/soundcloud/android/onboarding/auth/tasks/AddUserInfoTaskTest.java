package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;

public class AddUserInfoTaskTest extends AndroidUnitTest {

    @Mock private ApiClient apiClient;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private SoundCloudApplication application;
    @Mock private AccountOperations accountOperations;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;

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

        AddUserInfoTask task = new AddUserInfoTask(
                application, "permalink", "name", null, storeUsersCommand, apiClient, accountOperations, syncInitiatorBridge);
        LegacyAuthTaskResult result = task.doInBackground();
        assertThat(result.wasSuccess()).isTrue();
        assertThat(result.getUser().getUrn()).isEqualTo(user.getUrn());
        assertThat(result.getUser().getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getUser().getPermalink()).isEqualTo(user.getPermalink());
    }

    @Test
    public void shouldWorkWithNonexistentFile() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.CURRENT_USER)),
                eq(PublicApiUser.class))).thenReturn(user);

        AddUserInfoTask task = new AddUserInfoTask(
                application,
                "permalink",
                "name",
                new File("doesntexist"),
                storeUsersCommand,
                apiClient,
                accountOperations,
                syncInitiatorBridge);
        LegacyAuthTaskResult result = task.doInBackground();
        assertThat(result.wasSuccess()).isTrue();
        assertThat(result.getUser().getUrn()).isEqualTo(user.getUrn());
        assertThat(result.getUser().getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getUser().getPermalink()).isEqualTo(user.getPermalink());
    }

    @Test
    public void shouldWorkWithFile() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.CURRENT_USER)),
                eq(PublicApiUser.class))).thenReturn(user);

        File tmp = File.createTempFile("test", "tmp");
        AddUserInfoTask task = new AddUserInfoTask(application,
                                                   "permalink",
                                                   "name",
                                                   tmp,
                                                   storeUsersCommand,
                                                   apiClient,
                                                   accountOperations,
                                                   syncInitiatorBridge);
        LegacyAuthTaskResult result = task.doInBackground();
        assertThat(result.wasSuccess()).isTrue();
        assertThat(result.getUser().getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getUser().getPermalink()).isEqualTo(user.getPermalink());
    }

    @Test
    public void shouldHandleBadEntity() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("PUT", ApiEndpoints.CURRENT_USER)),
                eq(PublicApiUser.class))).thenThrow(TestApiResponses.validationError().getFailure());

        AddUserInfoTask task = new AddUserInfoTask(application,
                                                   "permalink",
                                                   "name",
                                                   null,
                                                   storeUsersCommand,
                                                   apiClient,
                                                   accountOperations,
                                                   syncInitiatorBridge);
        LegacyAuthTaskResult result = task.doInBackground();
        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.wasValidationError()).isTrue();
        assertThat(result.getUser()).isNull();
    }

}
