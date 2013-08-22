package com.soundcloud.android.activity.auth;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class TokenInformationGeneratorTest {
    private static final String[] SCOPE_EXTRAS = {"a", "b"};
    private TokenInformationGenerator tokenInformationGenerator;
    @Mock
    private Bundle bundle;
    @Mock
    private AndroidCloudAPI cloudApi;

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
    public void shouldPutDefaultScopeIntoBundleIfNoScopeSpecified(){
        when(bundle.containsKey("scopes")).thenReturn(false);
        tokenInformationGenerator.configureDefaultScopeExtra(bundle);
        verify(bundle).putStringArray("scopes", new String[]{"non-expiring"});

    }

    @Test
    public void shouldNotPutDefaultScopeIntoBundleIfScopeSpecified(){
        when(bundle.containsKey("scopes")).thenReturn(true);
        tokenInformationGenerator.configureDefaultScopeExtra(bundle);
        verify(bundle, never()).putStringArray(anyString(), any(String[].class));
    }

    @Test
    public void shouldObtainAuthorisationCodeIfBundleContainsCodeExtra() throws IOException {
        when(bundle.getStringArray("scopes")).thenReturn(SCOPE_EXTRAS);
        when(bundle.containsKey("code")).thenReturn(true);
        when(bundle.getString("code")).thenReturn("codeExtra");
        tokenInformationGenerator.getToken(bundle);
        verify(cloudApi).authorizationCode("codeExtra", SCOPE_EXTRAS);
    }

    @Test
    public void shouldObtainAuthorizationCodeIfBundleContainsUsernameAndPassword() throws IOException {
        when(bundle.getStringArray("scopes")).thenReturn(SCOPE_EXTRAS);
        when(bundle.containsKey("username")).thenReturn(true);
        when(bundle.containsKey("password")).thenReturn(true);
        String user = "user";
        String pass = "pass";
        when(bundle.getString("username")).thenReturn(user);
        when(bundle.getString("password")).thenReturn(pass);
        tokenInformationGenerator.getToken(bundle);
        verify(cloudApi).login(user,pass,SCOPE_EXTRAS);
    }

    @Test
    public void shouldObtainAuthorizationCodeIfBundleContainsGrantTYPEExtra() throws IOException {
        when(bundle.getStringArray("scopes")).thenReturn(SCOPE_EXTRAS);
        when(bundle.containsKey("extensionGrantType")).thenReturn(true);
        String grant = "grant";
        when(bundle.getString("extensionGrantType")).thenReturn(grant);
        tokenInformationGenerator.getToken(bundle);
        verify(cloudApi).extensionGrantType(grant, SCOPE_EXTRAS);

    }

}
