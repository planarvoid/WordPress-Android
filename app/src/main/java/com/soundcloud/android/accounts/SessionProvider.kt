package com.soundcloud.android.accounts

import android.accounts.Account
import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.model.Urn
import com.soundcloud.android.rx.observers.LambdaMaybeObserver
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@OpenForTesting
class SessionProvider
@Inject internal constructor(private val scAccountManager: ScAccountManager,
                     @param:Named(ApplicationModule.RX_HIGH_PRIORITY) private val scheduler: Scheduler ) {

    private val currentSession: Subject<UserSession> = BehaviorSubject.create()
    private var disposable = CompositeDisposable()

    /***
     * Emits the current user session. Will keep emitting as it is updated
     */
    fun currentSession(): Observable<UserSession> = sessionWithLazyInit()

    /**
     * Emits the current user urn. Will only emit once
     */
    fun currentUserUrn(): Maybe<Urn> = sessionWithLazyInit().firstElement().map(UserSession::loggedInUserUrn)

    /**
     * Emits true if the current session belongs to a user or the crawler
     */
    fun isUserLoggedIn(): Single<Boolean> = currentUserUrn().map { it != Urn.NOT_SET }.toSingle()

    /**
     * Emits the current user's account. Will only emit once
     */
    fun currentAccount(): Maybe<Account> = sessionWithLazyInit().firstElement().filter { it.loggedInUserAccount.isPresent }.map { it.loggedInUserAccount.get() }


    fun updateSession(session: UserSession) {
        cancelInitSession()
        currentSession.onNext(session)
    }

    private fun cancelInitSession() {
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
    }

    private fun sessionWithLazyInit() = currentSession.doOnSubscribe {
        if (!disposable.isDisposed && disposable.size() == 0) {
            disposable.add(Maybe.fromCallable { sessionFromManager(scAccountManager) }
                    .subscribeOn(scheduler)
                    .subscribeWith(LambdaMaybeObserver.onNext(currentSession::onNext)))
        }
    }

    private fun sessionFromManager(scAccountManager: ScAccountManager): UserSession {
        val soundCloudAccount = scAccountManager.getSoundCloudAccount()
        if (soundCloudAccount.isPresent) {
            val userUrn = scAccountManager.getUserUrn(soundCloudAccount.get())
            if (!userUrn.equals(Urn.NOT_SET)){
                return UserSession.forAuthenticatedUser(userUrn, soundCloudAccount.get())
            }
        }
        return UserSession.anonymous()
    }

    data class UserSession constructor(val loggedInUserUrn: Urn,
                                       val loggedInUserAccount: Optional<Account>) {
        companion object {

            @JvmStatic
            fun forAuthenticatedUser(userUrn: Urn, account: Account): UserSession = UserSession(userUrn, Optional.of(account))

            @JvmStatic
            fun forCrawler(): UserSession = UserSession(AccountOperations.CRAWLER_USER_URN, Optional.absent())

            @JvmStatic
            fun anonymous(): UserSession = UserSession(Urn.NOT_SET, Optional.absent())
        }
    }
}
