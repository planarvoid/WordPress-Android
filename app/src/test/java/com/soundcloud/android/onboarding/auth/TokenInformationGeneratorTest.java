package com.soundcloud.android.onboarding.auth;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.api.PublicCloudAPI;
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
    public void shouldObtainAuthorisationCodeIfBundleContainsCodeExtra() throws IOException {
        when(bundle.containsKey("code")).thenReturn(true);
        when(bundle.getString("code")).thenReturn("codeExtra");
        tokenInformationGenerator.getToken(bundle);
        verify(cloudApi).authorizationCode("codeExtra", NON_EXPIRING_SCOPE);
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
        verify(cloudApi).login(user,pass,NON_EXPIRING_SCOPE);
    }

    @Test
    public void shouldObtainAuthorizationCodeIfBundleContainsGrantTYPEExtra() throws IOException {
        when(bundle.containsKey("extensionGrantType")).thenReturn(true);
        String grant = "grant";
        when(bundle.getString("extensionGrantType")).thenReturn(grant);
        tokenInformationGenerator.getToken(bundle);
        verify(cloudApi).extensionGrantType(grant, NON_EXPIRING_SCOPE);

    }

}
