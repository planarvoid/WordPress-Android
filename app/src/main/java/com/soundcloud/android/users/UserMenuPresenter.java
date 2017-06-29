package com.soundcloud.android.users;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.stations.StartStationHandler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

import android.app.Activity;
import android.view.View;

import javax.inject.Inject;

public class UserMenuPresenter implements UserMenuRenderer.Listener {

    private final UserRepository userRepository;
    private final StartStationHandler stationHandler;
    private final EngagementsTracking engagementsTracking;
    private final AccountOperations accountOperations;
    private final FollowingOperations followingOperations;
    private final UserMenuRendererFactory rendererFactory;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private UserMenuRenderer renderer;
    private EventContextMetadata eventContextMetadata;

    @Inject
    UserMenuPresenter(UserMenuRendererFactory rendererFactory,
                      FollowingOperations followingOperations,
                      UserRepository userRepository,
                      StartStationHandler stationHandler,
                      EngagementsTracking engagementsTracking,
                      AccountOperations accountOperations) {
        this.rendererFactory = rendererFactory;
        this.followingOperations = followingOperations;
        this.userRepository = userRepository;
        this.stationHandler = stationHandler;
        this.engagementsTracking = engagementsTracking;
        this.accountOperations = accountOperations;
    }

    public void show(View button, Urn userUrn, EventContextMetadata eventContextMetadata) {
        renderer = rendererFactory.create(this, button);
        this.eventContextMetadata = eventContextMetadata;
        loadUser(userUrn);
    }

    @Override
    public void handleToggleFollow(User user) {
        boolean isFollowed = !user.isFollowing();
        followingOperations.toggleFollowing(user.urn(), isFollowed).subscribe(new DefaultDisposableCompletableObserver());
        engagementsTracking.followUserUrn(user.urn(), isFollowed, eventContextMetadata);
    }

    @Override
    public void handleOpenStation(Activity activity, User user) {
        stationHandler.startStation(activity, Urn.forArtistStation(user.urn().getNumericId()));
    }

    @Override
    public void onDismiss() {
        compositeDisposable.clear();
    }

    private void loadUser(Urn urn) {
        compositeDisposable.clear();
        compositeDisposable.add(userRepository.localUserInfo(urn)
                                              .observeOn(AndroidSchedulers.mainThread())
                                              .subscribeWith(new UserObserver()));
    }

    private class UserObserver extends DefaultMaybeObserver<User> {
        @Override
        public void onSuccess(User user) {
            renderer.render(user, accountOperations.isLoggedInUser(user.urn()));
        }
    }
}
