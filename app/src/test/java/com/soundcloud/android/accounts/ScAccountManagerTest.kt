package com.soundcloud.android.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.BuildConfig
import com.soundcloud.android.accounts.exception.OperationFailedException
import com.soundcloud.android.api.oauth.Token
import com.soundcloud.android.model.Urn
import com.soundcloud.android.onboarding.auth.SignupVia
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.java.optional.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import kotlin.reflect.KClass

class ScAccountManagerTest : AndroidUnitTest() {

    private val scAccountType = BuildConfig.APPLICATION_ID + ".account"
    private val userUrn = Urn.forUser(123)
    private val userPermalink = "permalink"
    private val token = Token("access", "refresh")
    private val account = Account(userPermalink, scAccountType)

    private lateinit var accountManager: ScAccountManager

    @Mock internal lateinit var androidAccountManager: AccountManager
    @Mock internal lateinit var accountManagerFuture: AccountManagerFuture<Boolean>

    @Before
    fun setUp() {
        accountManager = ScAccountManager(androidAccountManager, context())

        whenever(accountManagerFuture.getResult()).thenReturn(true)
        whenever(androidAccountManager.removeAccount(account, null, null)).thenReturn(accountManagerFuture)
    }

    @Test
    fun `sets User Data if account addition succeeds`() {
        whenever(androidAccountManager.getAccountsByType(scAccountType)).thenReturn(arrayOf())
        whenever(androidAccountManager.addAccountExplicitly(account, null, null)).thenReturn(true)

        accountManager.addOrReplaceSoundCloudAccount(userUrn, userPermalink, token, SignupVia.API)

        verify(androidAccountManager).setUserData(account, "currentUserId", userUrn.numericId.toString())
    }

    @Test
    fun `add or replace replaces account with different name`() {
        val old = Account("oldUsername", scAccountType)

        whenever(androidAccountManager.getAccountsByType(scAccountType)).thenReturn(arrayOf(old))
        whenever(androidAccountManager.removeAccount(old, null, null)).thenReturn(accountManagerFuture)
        whenever(androidAccountManager.addAccountExplicitly(account, null, null)).thenReturn(true)

        val actual = accountManager.addOrReplaceSoundCloudAccount(userUrn, userPermalink, token, SignupVia.API)

        assertThat(actual).isEqualTo(Optional.of(account))
        verify(androidAccountManager).removeAccount(old, null, null)

    }

    @Test
    fun `addOrReplace does not replace account with same name`() {
        val old = Account(userPermalink, scAccountType)
        whenever(androidAccountManager.getAccountsByType(scAccountType)).thenReturn(arrayOf(old))

        val actual = accountManager.addOrReplaceSoundCloudAccount(userUrn, userPermalink, token, SignupVia.API)
        assertThat(actual).isEqualTo(Optional.of(old))
        verify(androidAccountManager, Mockito.never()).removeAccount(any(), any(), any())
        verify(androidAccountManager, Mockito.never()).addAccountExplicitly(any(), any(), any())
    }

    @Test(expected = OperationFailedException::class)
    fun `remove throws exception with false removal result`() {
        whenever(accountManagerFuture.getResult()).thenReturn(false)
        whenever(androidAccountManager.removeAccount(account, null, null)).thenReturn(accountManagerFuture)

        accountManager.remove(account)
    }

    @Test
    fun `remove removes account`() {
        val accountManagerFuture: AccountManagerFuture<Boolean> = mock()

        whenever(accountManagerFuture.getResult()).thenReturn(true)
        whenever(androidAccountManager.removeAccount(account, null, null)).thenReturn(accountManagerFuture)

        accountManager.remove(account)

        verify(androidAccountManager).removeAccount(account, null, null)
    }


}
