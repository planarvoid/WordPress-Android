package com.soundcloud.android.onboarding.auth;


import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class TokenInformationGeneratorTest {
    public static final String[] NON_EXPIRING_SCOPE = new String[]{"non-expiring"};
    private TokenInformationGenerator tokenInformationGenerator;
    @Mock
    private Bundle bundle;
    @Mock
    private PublicCloudAPI cloudApi;

    @Before
    public void setUp(){
        initMocks(this);
        tokenInformationGenerator = new TokenInformationGenerator(cloudApi);
    }

    @Test
    public void shouldReturnGrantBundleWithSpecifiedGrantTypeAndToken(){
        Bundle bundle = tokenInformationGenerator.getGrantBundle("granttype", "token");
        assertThat(bundle.containsKey("extensionGrantType"), is(true));
        assertThat(bundle.getString("extensionGrantType"), is("granttypetoken"));
    }

    @Test
    public void shouldObtainAuthorizationCodeIfBundleContainsUsernameAndPassword() throws IOException {
        when(bundle.containsKey("username")).thenReturn(true);
        when(bundle.containsKey("password")).thenReturn(true);
        String user = "user";
        String pass = "pass";
        when(bundle.getString("username")).thenReturn(user);
        when(bundle.getString("password")).thenReturn(pass);
        tokenInformationGenerator.getToken(bundle);
        verify(cloudApi).login(user,pass);
    }

    @Test
    public void shouldObtainAuthorizationCodeIfBundleContainsGrantTYPEExtra() throws IOException {
        when(bundle.containsKey("extensionGrantType")).thenReturn(true);
        String grant = "grant";
        when(bundle.getString("extensionGrantType")).thenReturn(grant);
        tokenInformationGenerator.getToken(bundle);
        verify(cloudApi).extensionGrantType(grant);

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
}
