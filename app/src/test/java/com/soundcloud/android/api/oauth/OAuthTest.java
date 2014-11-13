package com.soundcloud.android.api.oauth;

import static com.soundcloud.android.Expect.expect;
import static java.util.AbstractMap.SimpleEntry;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;


@RunWith(SoundCloudTestRunner.class)
public class OAuthTest {
    private OAuth oAuth;
    private final static String TEST_CLIENT_ID = "testClientId";
    private final static String TEST_CLIENT_SECRET = "testClientSecret";

    private final Token token = new Token("access", "refresh");

    @Before
    public void setup() {
        oAuth = new OAuth(TEST_CLIENT_ID, TEST_CLIENT_SECRET, token);
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
    public void shouldBuildQueryParamsForTokenRequestFromAuthCode() {
        final Map<String, String> params = oAuth.getTokenRequestParamsFromCode("123");
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "authorization_code"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("redirect_uri", "soundcloud://auth"),
            new SimpleEntry<>("code", "123"),
            new SimpleEntry<>("scope", "non-expiring")
        );
    }

    @Test
    public void shouldBuildQueryParamsForTokenRequestFromClientCredentialsWithoutScope() {
        final Map<String, String> params = oAuth.getTokenRequestParamsFromClientCredentials();
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "client_credentials"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("scope", "non-expiring")
        );
    }

    @Test
    public void shouldBuildQueryParamsForTokenRequestFromClientCredentialsWithScope() {
        final Map<String, String> params = oAuth.getTokenRequestParamsFromClientCredentials(Token.SCOPE_SIGNUP, Token.SCOPE_NON_EXPIRING);
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "client_credentials"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("scope", "signup non-expiring")
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
        final Map<String, String> params = oAuth.getTokenRequestParamsForRefreshToken();
        expect(params.entrySet()).toContainExactly(
            new SimpleEntry<>("grant_type", "refresh_token"),
            new SimpleEntry<>("client_id", "testClientId"),
            new SimpleEntry<>("client_secret", "testClientSecret"),
            new SimpleEntry<>("refresh_token", token.getRefreshToken())
        );
    }

    @Test
    public void hasValidTokenIsTrueForValidToken() {
        expect(oAuth.hasToken()).toBeTrue();
    }

    @Test
    public void hasValidTokenIsFalseWhenNotProvidingAToken() {
        final OAuth oauth = new OAuth(TEST_CLIENT_ID, TEST_CLIENT_SECRET, null);
        expect(oauth.hasToken()).toBeFalse();
    }

    @Test
    public void hasValidTokenIsFalseForWhenSettingNullToken() {
        oAuth.setToken(null);
        expect(oAuth.hasToken()).toBeFalse();
    }

    @Test
    public void shouldBuildAuthorizationHeaderFromValidToken() {
        expect(oAuth.getAuthorizationHeaderValue()).toEqual("OAuth access");
    }

    @Test
    public void shouldBuildAuthorizationHeaderFromInvalidToken() {
        final OAuth oauth = new OAuth(TEST_CLIENT_ID, TEST_CLIENT_SECRET, null);
        expect(oauth.getAuthorizationHeaderValue()).toEqual("OAuth invalidated");
    }
}
