package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.R;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.res.Resources;

public class SoundCloudTokenOperationsTest {


    private static final String ACCESSTOKEN = "access";
    private static final String REFRESHTOKEN = "refresh";
    private static final String SCOPE = "scope";
    private static final long EXPIRES = 2222333L;

    private SoundCloudTokenOperations tokenOperations;
    @Mock
    private AccountManager accountManager;
    @Mock
    private Resources resources;
    @Mock
    private Token token;
    @Mock
    private Account account;

    @Before
    public void setUp(){
        initMocks(this);
        tokenOperations = new SoundCloudTokenOperations(accountManager, resources);
        token.access = ACCESSTOKEN;
        token.refresh = REFRESHTOKEN;
        token.scope = SCOPE;
        token.expiresIn = EXPIRES;
    }

    @Test
    public void shouldStoreTokenInformationIfSoundCloudAccountExists(){
        tokenOperations.storeSoundCloudTokenData(account, token);
        verify(accountManager).setAuthToken(account, User.DataKeys.ACCESS_TOKEN, ACCESSTOKEN);
        verify(accountManager).setAuthToken(account, User.DataKeys.REFRESH_TOKEN, REFRESHTOKEN);
        verify(accountManager).setUserData(account, User.DataKeys.SCOPE, SCOPE);
        verify(accountManager).setPassword(account, token.access);
        verify(accountManager).setUserData(account, User.DataKeys.EXPIRES_IN, Long.toString(EXPIRES));
    }

    @Test
    public void shouldReturnTokenIfAccountExists(){
        when(accountManager.getUserData(account, User.DataKeys.SCOPE)).thenReturn(SCOPE);
        when(accountManager.peekAuthToken(account, User.DataKeys.ACCESS_TOKEN)).thenReturn(ACCESSTOKEN);
        when(accountManager.peekAuthToken(account, User.DataKeys.REFRESH_TOKEN)).thenReturn(REFRESHTOKEN);

        Token token = tokenOperations.getSoundCloudToken(account);
        expect(token.access).toEqual(ACCESSTOKEN);
        expect(token.refresh).toEqual(REFRESHTOKEN);
        expect(token.scope).toEqual(SCOPE);
    }

    @Test
    public void shouldInvalidateAccessAndRefreshTokenWhenRequested(){
        when(resources.getString(R.string.account_type)).thenReturn("accounttype");
        tokenOperations.invalidateToken(token);
        verify(accountManager).invalidateAuthToken("accounttype", ACCESSTOKEN);
        verify(accountManager).invalidateAuthToken("accounttype", REFRESHTOKEN);
    }
}
