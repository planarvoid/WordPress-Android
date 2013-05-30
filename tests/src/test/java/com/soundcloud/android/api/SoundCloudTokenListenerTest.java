package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SoundCloudTokenListenerTest {

    private SoundCloudTokenListener tokenListener;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private Token newToken;
    @Mock
    private Token expiredToken;

    @Before
    public void setUp(){
        initMocks(this);
        tokenListener = new SoundCloudTokenListener(accountOperations);
    }

    @Test
    public void shouldReturnNewTokenIfSCAccountExistsAndTokenIsNotTheSameAsTheExpiredOne(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(newToken);
        expect(tokenListener.onTokenInvalid(expiredToken)).toBe(newToken);
        verify(accountOperations).invalidateSoundCloudToken(expiredToken);
    }

    @Test
    public void shouldReturnNullIfSCAccountExistsAndTokenIsTheSameAsTheExpiredOne(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(expiredToken);
        expect(tokenListener.onTokenInvalid(expiredToken)).toBeNull();
        verify(accountOperations).invalidateSoundCloudToken(expiredToken);
    }

    @Test
    public void shouldReturnNullIfSoundCloudDoesAccountNotExist(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        expect(tokenListener.onTokenInvalid(expiredToken)).toBeNull();
        verify(accountOperations).invalidateSoundCloudToken(expiredToken);
    }

    @Test
    public void shouldStoreSoundCloudTokenIfAccountExistsAndTokenIsValidAndDefaultScoped(){
        when(newToken.valid()).thenReturn(true);
        when(newToken.defaultScoped()).thenReturn(true);
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        tokenListener.onTokenRefreshed(newToken);
        verify(accountOperations).storeSoundCloudTokenData(newToken);
    }

    @Test
    public void shouldNotStoreSoundCloudTokenIfAccountDoesNotExistAndTokenIsValidAndDefaultScoped(){
        when(newToken.valid()).thenReturn(true);
        when(newToken.defaultScoped()).thenReturn(true);
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        tokenListener.onTokenRefreshed(newToken);
        verify(accountOperations, never()).storeSoundCloudTokenData(newToken);
    }

    @Test
    public void shouldNotStoreSoundCloudTokenIfAccountExistsAndTokenIsNotValidButIsDefaultScoped(){
        when(newToken.valid()).thenReturn(false);
        when(newToken.defaultScoped()).thenReturn(true);
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        tokenListener.onTokenRefreshed(newToken);
        verify(accountOperations, never()).storeSoundCloudTokenData(newToken);
    }

    @Test
    public void shouldNotStoreSoundCloudTokenIfAccountExistsAndTokenIsValidButIsNotDefaultScoped(){
        when(newToken.valid()).thenReturn(true);
        when(newToken.defaultScoped()).thenReturn(false);
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        tokenListener.onTokenRefreshed(newToken);
        verify(accountOperations, never()).storeSoundCloudTokenData(newToken);
    }

}
