package com.soundcloud.android.api.oauth;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Map;

import static com.soundcloud.android.Expect.expect;
import static java.util.AbstractMap.SimpleEntry;
import static org.mockito.Mockito.when;


@RunWith(SoundCloudTestRunner.class)
public class OAuthTest {
    private OAuth oAuth;
    private final static String TEST_CLIENT_ID = "testClientId";
    private final static String TEST_CLIENT_SECRET = "testClientSecret";

    private final Token validToken = new Token("access", "refresh");

    @Mock private AccountOperations accountOperations;

    @Before
    public void setup() {
        when(accountOperations.getSoundCloudToken()).thenReturn(validToken);
        oAuth = new OAuth(TEST_CLIENT_ID, TEST_CLIENT_SECRET, accountOperations);
    }

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        expect(new OAuth(accountOperations).getClientSecret()).toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");
    }

    @Test
    public void shouldBuildQueryParamsForTokenRequestFromUserCredentials() {
        final Map<String, String> params = oAuth.getTokenRequestParamsFromUserCredentials("user", "pw");
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "password"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("username", "user"),
            new SimpleEntry<>("password", "pw"),
            new SimpleEntry<>("scope", "non-expiring")
        );
    }

    @Test
    public void shouldBuildQueryParamsForTokenRequestFromClientCredentialsWithoutScope() {
        final Map<String, String> params = oAuth.getTokenRequestParamsFromClientCredentials();
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "client_credentials"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret")
        );
    }

    @Test
    public void shouldBuildQueryParamsForTokenRequestFromClientCredentialsWithScope() {
        final Map<String, String> params = oAuth.getTokenRequestParamsFromClientCredentials(Token.SCOPE_SIGNUP);
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "client_credentials"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("scope", "signup")
        );
    }

    @Test
    public void shouldBuildQueryParamsForTokenRequestFromExtensionGrant() {
        final Map<String, String> params = oAuth.getTokenRequestParamsFromExtensionGrant("facebook");
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "facebook"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("scope", "non-expiring")
        );
    }

    @Test
    public void shouldBuildQueryParamsForTokenRequestForRefreshToken() {
        final Map<String, String> params = oAuth.getTokenRequestParamsForRefreshToken("refresh");
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "refresh_token"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("refresh_token", "refresh")
        );
    }

    @Test
    public void shouldBuildAuthorizationHeaderFromValidToken() {
        expect(oAuth.getAuthorizationHeaderValue()).toEqual("OAuth access");
    }

    @Test
    public void shouldBuildAuthorizationHeaderFromInvalidToken() {
        when(accountOperations.getSoundCloudToken()).thenReturn(Token.EMPTY);
        expect(oAuth.getAuthorizationHeaderValue()).toEqual("OAuth invalidated");
    }
}
