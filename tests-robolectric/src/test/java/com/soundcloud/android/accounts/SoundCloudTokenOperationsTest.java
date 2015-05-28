package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.api.oauth.Token;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.accounts.Account;
import android.accounts.AccountManager;

@RunWith(SoundCloudTestRunner.class)
public class SoundCloudTokenOperationsTest {


    private static final String ACCESSTOKEN = "access";
    private static final String REFRESHTOKEN = "refresh";
    private static final String SCOPE = "scope";
    private static final long EXPIRES = 2222333L;
    private static final String ACCOUNTTYPE = "accounttype";
    private static final String ACCOUNTNAME = "accountname";

    private SoundCloudTokenOperations tokenOperations;
    private Token token;

    @Mock private AccountManager accountManager;
    @Mock private Account account;

    @Before
    public void setUp(){
        initMocks(this);
        tokenOperations = new SoundCloudTokenOperations(accountManager);
        token = new Token(ACCESSTOKEN, REFRESHTOKEN, SCOPE, EXPIRES);
        account = new Account(ACCOUNTNAME, ACCOUNTTYPE);
    }

    @Test
    public void shouldStoreTokenInformationIfSoundCloudAccountExists(){
        tokenOperations.storeSoundCloudTokenData(account, token);

        verify(accountManager).setAuthToken(account, "access_token", ACCESSTOKEN);
        verify(accountManager).setAuthToken(account, "refresh_token", REFRESHTOKEN);
        verify(accountManager).setUserData(account, "scope", SCOPE);
        verify(accountManager).setUserData(account, "expires_in", Long.toString(EXPIRES));
    }

    @Test
    public void shouldReturnTokenIfAccountExists(){
        when(accountManager.getUserData(account, "scope")).thenReturn(SCOPE);
        when(accountManager.peekAuthToken(account, "access_token")).thenReturn(ACCESSTOKEN);
        when(accountManager.peekAuthToken(account, "refresh_token")).thenReturn(REFRESHTOKEN);

        Token token = tokenOperations.getTokenFromAccount(account);
        expect(token.getAccessToken()).toEqual(ACCESSTOKEN);
        expect(token.getRefreshToken()).toEqual(REFRESHTOKEN);
        expect(token.getScope()).toEqual(SCOPE);
    }

    @Test
    public void shouldInvalidateTokensAndData(){
        tokenOperations.invalidateToken(token, account);
        verify(accountManager).invalidateAuthToken(ACCOUNTTYPE, ACCESSTOKEN);
        verify(accountManager).invalidateAuthToken(ACCOUNTTYPE, REFRESHTOKEN);

        verify(accountManager).setUserData(account, "scope", null);
        verify(accountManager).setUserData(account, "expires_in", null);
    }

    @Test
    public void shouldNotReloadTokenFromAccountIfAlreadyLoaded() {
        Token token1 = tokenOperations.getTokenFromAccount(account);
        Token token2 = tokenOperations.getTokenFromAccount(account);
        expect(token1).toBe(token2);
    }

    @Test
    public void shouldReloadTokenFromAccountAfterInvalidating() {
        Token token1 = tokenOperations.getTokenFromAccount(account);
        tokenOperations.invalidateToken(token, account);
        Token token2 = tokenOperations.getTokenFromAccount(account);
        expect(token1).not.toBe(token2);
        verify(accountManager, times(2)).peekAuthToken(account, "access_token");
        verify(accountManager, times(2)).peekAuthToken(account, "refresh_token");
    }

    @Test
    public void shouldReloadTokenFromAccountAfterResetting() {
        Token token1 = tokenOperations.getTokenFromAccount(account);
        tokenOperations.resetToken();
        Token token2 = tokenOperations.getTokenFromAccount(account);
        expect(token1).not.toBe(token2);
        verify(accountManager, times(2)).peekAuthToken(account, "access_token");
        verify(accountManager, times(2)).peekAuthToken(account, "refresh_token");
    }
}
