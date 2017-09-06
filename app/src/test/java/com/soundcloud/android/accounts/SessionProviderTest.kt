package com.soundcloud.android.accounts

import android.accounts.Account
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.java.optional.Optional
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class SessionProviderTest : AndroidUnitTest() {

    @Mock private lateinit var accountManager: ScAccountManager

    lateinit var sessionProvider: SessionProvider

    @Before
    fun setUp() {
        sessionProvider = SessionProvider(accountManager, Schedulers.trampoline());
    }

    @Test
    fun `session is anonymous if never set`() {
        `when`(accountManager.getSoundCloudAccount()).thenReturn(Optional.absent())

        sessionProvider.currentSession().test().assertValue(SessionProvider.UserSession.anonymous())
    }

    @Test
    fun `urn is emitted after updating and clearing`() {
        `when`(accountManager.getSoundCloudAccount()).thenReturn(Optional.of(ACCOUNT))
        `when`(accountManager.getUserUrn(ACCOUNT)).thenReturn(USER)

        sessionProvider.currentSession().test().assertValue(AUTHENTICATED_SESSION)
    }

    @Test
    fun `session emits changes`() {
        `when`(accountManager.getSoundCloudAccount()).thenReturn(Optional.absent())

        val test = sessionProvider.currentSession().test()

        sessionProvider.updateSession(SessionProvider.UserSession.forCrawler())
        sessionProvider.updateSession(SessionProvider.UserSession.forAuthenticatedUser(USER, ACCOUNT))
        sessionProvider.updateSession(SessionProvider.UserSession.anonymous())

        test.assertValues(SessionProvider.UserSession.anonymous(),
                          SessionProvider.UserSession.forCrawler(),
                          SessionProvider.UserSession.forAuthenticatedUser(USER, ACCOUNT),
                          SessionProvider.UserSession.anonymous())
    }

    @Test
    fun `session is for user after updating`() {
        `when`(accountManager.getSoundCloudAccount()).thenReturn(Optional.absent())

        sessionProvider.updateSession(AUTHENTICATED_SESSION)

        sessionProvider.currentSession().test().assertValue(AUTHENTICATED_SESSION)
        sessionProvider.currentAccount().test().assertValue(ACCOUNT).assertComplete()
        sessionProvider.currentUserUrn().test().assertValue(USER).assertComplete()
    }

    @Test
    fun `session is anonymous after updating`() {
        `when`(accountManager.getSoundCloudAccount()).thenReturn(Optional.of(ACCOUNT))
        `when`(accountManager.getUserUrn(ACCOUNT)).thenReturn(USER)

        sessionProvider.updateSession(SessionProvider.UserSession.anonymous())

        sessionProvider.currentSession().test().assertValue(SessionProvider.UserSession.anonymous())
        sessionProvider.currentAccount().test().assertNoValues().assertComplete()
        sessionProvider.currentUserUrn().test().assertValue(Urn.NOT_SET).assertComplete()
    }

    @Test
    fun `user is logged in with real session`() {
        sessionProvider.updateSession(SessionProvider.UserSession.forAuthenticatedUser(USER, ACCOUNT))

        sessionProvider.isUserLoggedIn().test().assertValue(true).assertComplete()
    }

    @Test
    fun `user is logged in with crawler session`() {
        sessionProvider.updateSession(SessionProvider.UserSession.forCrawler())

        sessionProvider.isUserLoggedIn().test().assertValue { true }.assertComplete()
    }

    @Test
    fun `user is not logged in by default`() {
        `when`(accountManager.getSoundCloudAccount()).thenReturn(Optional.absent())

        sessionProvider.isUserLoggedIn().test().assertValue(false).assertComplete()
    }

    @Test
    fun `user is not logged in after updating to anonymous session`() {
        `when`(accountManager.getSoundCloudAccount()).thenReturn(Optional.absent())

        sessionProvider.updateSession(SessionProvider.UserSession.forCrawler())
        sessionProvider.updateSession(SessionProvider.UserSession.anonymous())

        sessionProvider.isUserLoggedIn().test().assertValue(false).assertComplete()
    }

    companion object {
        private val ACCOUNT = Account("acct", "type")
        private val USER = Urn.forUser(1)
        private val AUTHENTICATED_SESSION = SessionProvider.UserSession.forAuthenticatedUser(USER, ACCOUNT)
    }
}
