package com.soundcloud.android.users;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.stations.StartStationHandler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.view.View;

import javax.inject.Inject;

public class UserMenuPresenter implements UserMenuRenderer.Listener {

    private final UserRepository userRepository;
    private final StartStationHandler stationHandler;
    private final FollowingOperations followingOperations;
    private final UserMenuRendererFactory rendererFactory;

    private UserMenuRenderer renderer;
    private Subscription userSubscription = RxUtils.invalidSubscription();

    @Inject
    UserMenuPresenter(UserMenuRendererFactory rendererFactory,
                      FollowingOperations followingOperations,
                      UserRepository userRepository,
                      StartStationHandler stationHandler) {
        this.rendererFactory = rendererFactory;
        this.followingOperations = followingOperations;
        this.userRepository = userRepository;
        this.stationHandler = stationHandler;
    }

    public void show(View button, Urn stationUrn) {
        renderer = rendererFactory.create(this, button);
        loadUser(stationUrn);
    }

    @Override
    public void handleToggleFollow(UserItem user) {
        boolean isFollowed = !user.isFollowedByMe();
        fireAndForget(followingOperations.toggleFollowing(user.getUrn(), isFollowed));
    }

    @Override
    public void handleOpenStation(Context context, UserItem user) {
        final Urn userUrn = user.getUrn();
        stationHandler.startStation(context, Urn.forArtistStation(userUrn.getNumericId()));
    }

    @Override
    public void onDismiss() {
        userSubscription.unsubscribe();
        userSubscription = RxUtils.invalidSubscription();
    }

    private void loadUser(Urn urn) {
        userSubscription.unsubscribe();
        userSubscription = userRepository.localUserInfo(urn)
                                         .first()
                                         .map(UserItem.fromPropertySet())
                                         .observeOn(AndroidSchedulers.mainThread())
                                         .subscribe(new UserSubscriber());
    }

    private class UserSubscriber extends DefaultSubscriber<UserItem> {
        @Override
        public void onNext(UserItem user) {
            renderer.render(user);
        }
    }
}
