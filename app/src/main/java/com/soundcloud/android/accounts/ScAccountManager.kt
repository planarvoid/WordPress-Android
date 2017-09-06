package com.soundcloud.android.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.Context
import android.util.Log
import com.soundcloud.android.R
import com.soundcloud.android.accounts.exception.OperationFailedException
import com.soundcloud.android.api.oauth.Token
import com.soundcloud.android.model.Urn
import com.soundcloud.android.onboarding.auth.SignupVia
import com.soundcloud.android.utils.AndroidUtils
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import java.io.IOException
import javax.inject.Inject

@OpenForTesting
internal class ScAccountManager
@Inject constructor(private val accountManager: AccountManager,
                    private val context: Context) {

    internal val userIdKey = "currentUserId"

    fun addAccount(string: String, currentActivityContext: Activity) {
        accountManager.addAccount(getAccountType(), string, null, null, currentActivityContext, null, null)
    }

    @Throws(android.accounts.OperationCanceledException::class, IOException::class, android.accounts.AuthenticatorException::class, OperationFailedException::class)
    fun remove(account: Account) {
        val accountRemovalFuture = removeAccount(account)
        if (!accountRemovalFuture.getResult()) {
            throw OperationFailedException()
        }
    }

    fun removeAccount(account: Account): AccountManagerFuture<Boolean> = accountManager.removeAccount(account, null, null)

    fun getSoundCloudAccount(): Optional<Account> {
        val accounts = AndroidUtils.getAccounts(accountManager, getAccountType())
        return when {
            accounts.size == 1 -> Optional.of(accounts[0])
            else -> Optional.absent()
        }
    }

    fun getUserUrn(account: Account) : Urn {
        val userData = accountManager.getUserData(account, userIdKey)
        userData?.let {
            return Urn.forUser(userData.toLong())
        }
        return Urn.NOT_SET
    }

    /**
     * Adds the given user as a SoundCloud account to the device's list of accounts. Idempotent, will be a no op if
     * already called.
     *
     * @return the new account, or null if account already existed or adding it failed
     */
    fun addOrReplaceSoundCloudAccount(urn: Urn, permalink: String, token: Token, via: SignupVia): Optional<Account> {
        var accountExists = false
        var account = getSoundCloudAccount()
        if (account.isPresent) {
            if (account.get().name == permalink) {
                accountExists = true // same username, do not replace account
            } else {
                removeAccount(account.get())
            }
        }

        if (!accountExists) {
            val accountType = getAccountType()
            account = Optional.of(Account(permalink, accountType))
            accountExists = accountManager.addAccountExplicitly(account.get(), null, null)

            // workaround for Sony Xperia XZ devices that corrupted their AccountManager databases
            // for Accounts that were added prior to the OS update to Android 7.1.1.
            // Adding this different account should relieve users facing this issue.
            // See: https://stackoverflow.com/questions/43664484/accountmanager-fails-to-add-account-on-sony-xz-7-1-1/44824516#44824516
            if (!accountExists) {
                ErrorUtils.log(Log.ERROR, "AccountOperations", "Failed to add account in first try.")
                ErrorUtils.handleSilentException(AddAccountFailure("Failed to add account in first try."))
                account = Optional.of(Account(permalink + "\n", accountType))
                accountExists = accountManager.addAccountExplicitly(account.get(), null, null)

                if (!accountExists) {
                    ErrorUtils.log(Log.ERROR, "AccountOperations", "Failed to add account in second try.")
                    ErrorUtils.handleSilentException(AddAccountFailure("Failed to add account in second try."))
                }
            }
        }

        if (accountExists) {
            // this should probably use URN, but we have to setup a migration for that
            accountManager.setUserData(account.get(), userIdKey, java.lang.Long.toString(urn.numericId))
        }
        return account;
    }

    private fun getAccountType() = context.getString(R.string.account_type)

    private class AddAccountFailure internal constructor(message: String) : Throwable(message)

}
