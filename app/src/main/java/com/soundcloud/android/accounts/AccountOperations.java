package com.soundcloud.android.accounts;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.ClearOfflineContentCommand;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.PlaySessionStateStorage;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class AccountOperations {

    private static final String CRAWLER_USER_PERMALINK = "SoundCloud";
    public static final Urn CRAWLER_USER_URN = Urn.forUser(-2);
    private static final String TOKEN_TYPE = "access_token";

    private final Context context;
    private final ScAccountManager accountManager;
    private final SoundCloudTokenOperations tokenOperations;
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private final SessionProvider sessionProvider;

    private final GooglePlayServicesWrapper googlePlayServicesWrapper;
    private final PlaySessionStateStorage playSessionStateStorage;
    private final Lazy<ConfigurationOperations> configurationOperations;
    private final Lazy<AccountCleanupAction> accountCleanupAction;
    private final Lazy<ClearOfflineContentCommand> clearOfflineContentCommand;
    private final Lazy<LoginManager> facebookLoginManager;

    @Inject
    AccountOperations(Context context,
                      ScAccountManager accountManager,
                      SoundCloudTokenOperations tokenOperations,
                      EventBusV2 eventBus,
                      PlaySessionStateStorage playSessionStateStorage,
                      Lazy<ConfigurationOperations> configurationOperations,
                      Lazy<AccountCleanupAction> accountCleanupAction,
                      Lazy<ClearOfflineContentCommand> clearOfflineContentCommand,
                      Lazy<LoginManager> facebookLoginManager,
                      GooglePlayServicesWrapper googlePlayServicesWrapper,
                      @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                      SessionProvider sessionProvider) {
        this.context = context;
        this.accountManager = accountManager;
        this.tokenOperations = tokenOperations;
        this.eventBus = eventBus;
        this.playSessionStateStorage = playSessionStateStorage;
        this.configurationOperations = configurationOperations;
        this.accountCleanupAction = accountCleanupAction;
        this.clearOfflineContentCommand = clearOfflineContentCommand;
        this.facebookLoginManager = facebookLoginManager;
        this.googlePlayServicesWrapper = googlePlayServicesWrapper;
        this.scheduler = scheduler;
        this.sessionProvider = sessionProvider;
    }

    /**
     * @deprecated use {@link SessionProvider#currentUserUrn} instead.
     */
    @Deprecated
    public Urn getLoggedInUserUrn() {
        return sessionProvider.currentUserUrn().blockingGet();
    }

    /**
     * @deprecated use {@link SessionProvider#isUserLoggedIn()} instead.
     */
    @Deprecated
    public boolean isLoggedInUser(Urn user) {
        return user.equals(getLoggedInUserUrn());
    }

    @VisibleForTesting
    void clearLoggedInUser() {
        sessionProvider.updateSession(SessionProvider.UserSession.anonymous());
    }

    public String getGoogleAccountToken(String accountName,
                                        String scope,
                                        Bundle bundle) throws GoogleAuthException, IOException {
        return googlePlayServicesWrapper.getAuthToken(context, accountName, scope, bundle);
    }

    public void invalidateGoogleAccountToken(String token) {
        GoogleAuthUtil.invalidateToken(context, token);
    }

    /**
     * @deprecated use {@link SessionProvider#isUserLoggedIn} ()} instead.
     */
    public boolean isUserLoggedIn() {
        return sessionProvider.isUserLoggedIn().blockingGet();
    }

    public void triggerLoginFlow(Activity currentActivityContext) {
        accountManager.addAccount(TOKEN_TYPE, currentActivityContext);
    }

    /**
     * Adds the given user as a SoundCloud account to the device's list of accounts. Idempotent, will be a no op if
     * already called.
     *
     * @return the new account, or null if account already existed or adding it failed
     */
    @Nullable
    public Account addOrReplaceSoundCloudAccount(ApiUser user, Token token, SignupVia via) {
        Optional<Account> accountOptional = accountManager.addOrReplaceSoundCloudAccount(user.getUrn(), user.getPermalink(), token, via);
        if (accountOptional.isPresent()) {
            tokenOperations.storeSoundCloudTokenData(accountOptional.get(), token);
            sessionProvider.updateSession(SessionProvider.UserSession.forAuthenticatedUser(user.getUrn(), accountOptional.get()));
            eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(user.getUrn()));
            return accountOptional.get();
        } else {
            return null;
        }
    }

    public void loginCrawlerUser() {
        Account account = new Account(CRAWLER_USER_PERMALINK, context.getString(R.string.account_type));
        sessionProvider.updateSession(SessionProvider.UserSession.forCrawler());
        tokenOperations.storeSoundCloudTokenData(account, Token.EMPTY);
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(CRAWLER_USER_URN));
    }

    public Optional<Account> getSoundCloudAccount() {
        return accountManager.getSoundCloudAccount();
    }

    public Completable logout() {
        Optional<Account> soundCloudAccount = getSoundCloudAccount();
        if (!soundCloudAccount.isPresent()) {
            throw new IllegalStateException("Missing Account. One does not simply remove something that does not exist");
        }
        return RxJava.toV2Observable(configurationOperations.get().deregisterDevice())
                     .flatMapCompletable(o -> Completable.fromAction(() -> accountManager.remove(soundCloudAccount.get())))
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribeOn(scheduler);
    }

    public Completable purgeUserData() {
        return Completable.fromAction(() -> {
            clearOfflineContentCommand.get().call(null);
            accountCleanupAction.get().call();
            tokenOperations.resetToken();
            clearFacebookStorage();
            clearLoggedInUser();
            eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
            resetPlaybackService();
            playSessionStateStorage.clear();
        }).subscribeOn(scheduler);
    }

    private void clearFacebookStorage() {
        facebookLoginManager.get().logOut();
    }

    // TODO: This should be made in the playback operations, which is not used at the moment, since it will cause a circular dependency
    private void resetPlaybackService() {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(PlaybackService.Action.RESET_ALL);
        context.startService(intent);
    }

    public Token getSoundCloudToken() {
        return tokenOperations.getTokenFromAccount(getSoundCloudAccount().orNull());
    }

    public void updateToken(Token token) {
        tokenOperations.setToken(token);
    }

    public boolean hasValidToken() {
        return getSoundCloudToken().valid();
    }

    public boolean isCrawler() {
        return getLoggedInUserUrn().equals(CRAWLER_USER_URN);
    }

    public void clearCrawler() {
        if (isCrawler()) {
            clearLoggedInUser();
        }
    }

    public static boolean isAnonymousUser(Urn urn) {
        return urn.equals(Urn.NOT_SET);
    }
}
