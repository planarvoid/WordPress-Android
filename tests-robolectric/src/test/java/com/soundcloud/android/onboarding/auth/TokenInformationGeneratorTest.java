package com.soundcloud.android.onboarding.auth;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.StringPart;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.matchers.ApiRequestTo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TokenInformationGeneratorTest {
    private static final List<StringPart> COMMON_FORM_PARTS = Arrays.asList(
            StringPart.from(OAuth.PARAM_CLIENT_ID, "client_id"),
            StringPart.from(OAuth.PARAM_CLIENT_SECRET, "client_secret"),
            StringPart.from(OAuth.PARAM_SCOPE, Token.SCOPE_NON_EXPIRING));

    private TokenInformationGenerator tokenInformationGenerator;

    @Mock private Bundle bundle;
    @Mock private ApiClient apiClient;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws IOException {
        tokenInformationGenerator = new TokenInformationGenerator(apiClient,
                new OAuth("client_id", "client_secret", accountOperations));
    }

    @Test
    public void shouldReturnGrantBundleWithSpecifiedGrantTypeAndToken() {
        Bundle bundle = tokenInformationGenerator.getGrantBundle("granttype", "token");
        assertThat(bundle.containsKey("extensionGrantType"), is(true));
        assertThat(bundle.getString("extensionGrantType"), is("granttypetoken"));
    }

    @Test
    public void shouldObtainAuthorizationCodeIfBundleContainsUsernameAndPassword() throws ApiRequestException, IOException {
        stubTokenResponse(
                StringPart.from(OAuth.PARAM_GRANT_TYPE, OAuth.GRANT_TYPE_PASSWORD),
                StringPart.from(OAuth.PARAM_USERNAME, "user"),
                StringPart.from(OAuth.PARAM_PASSWORD, "pass"));
        addToBundle("username", "user");
        addToBundle("password", "pass");

        Token token = tokenInformationGenerator.getToken(bundle);

        assertThat(token.getAccessToken(), is("04u7h-4cc355-70k3n"));
    }

    @Test
    public void shouldObtainAuthorizationCodeIfBundleContainsGrantTYPEExtra() throws ApiRequestException {
        stubTokenResponse(StringPart.from(OAuth.PARAM_GRANT_TYPE, "grant"));
        addToBundle("extensionGrantType", "grant");

        Token token = tokenInformationGenerator.getToken(bundle);

        assertThat(token.getAccessToken(), is("04u7h-4cc355-70k3n"));
    }

    @Test
    public void shouldRecognizeFacebookGrants() throws IOException {
        Bundle bundle = tokenInformationGenerator.getGrantBundle(OAuth.GRANT_TYPE_FACEBOOK, "some_token_string_here");

        expect(tokenInformationGenerator.isFromFacebook(bundle)).toBeTrue();
    }

    @Test
    public void shouldRecognizeNonFacebookGrants() throws IOException {
        Bundle bundle = tokenInformationGenerator.getGrantBundle(OAuth.GRANT_TYPE_GOOGLE_PLUS, "some_token_string_here");

        expect(tokenInformationGenerator.isFromFacebook(bundle)).toBeFalse();
    }

    @Test
    public void shouldHandleBundleWithNoGrantType() throws IOException {
        Bundle bundle = new Bundle();

        expect(tokenInformationGenerator.isFromFacebook(bundle)).toBeFalse();
    }

    private void addToBundle(String key, String value) {
        when(bundle.containsKey(key)).thenReturn(true);
        when(bundle.getString(key)).thenReturn(value);
    }

    private void stubTokenResponse(StringPart... parts) {
        ApiRequestTo post = isPublicApiRequestTo("POST", ApiEndpoints.OAUTH2_TOKEN)
                .withFormParts((StringPart[]) ArrayUtils.addAll(parts, COMMON_FORM_PARTS.toArray()));

        when(apiClient.fetchResponse(argThat(post))).thenReturn(
                TestApiResponses.resource(getClass(), 201, "token.json"));
    }
}
