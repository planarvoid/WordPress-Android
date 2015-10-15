package com.soundcloud.android.you;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.android.schedulers.AndroidSchedulers;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

public class YouPresenter extends DefaultSupportFragmentLightCycle<YouFragment> {

    private final YouViewFactory youViewFactory;
    private final UserRepository userRepository;
    private final AccountOperations accountOperations;
    private final ImageOperations imageOperations;
    private final Resources resources;

    private Optional<YouView> headerViewOpt = Optional.absent();
    private Optional<PropertySet> youOpt = Optional.absent();

    @Inject
    public YouPresenter(YouViewFactory youViewFactory, UserRepository userRepository, AccountOperations accountOperations, ImageOperations imageOperations, Resources resources) {
        this.youViewFactory = youViewFactory;
        this.userRepository = userRepository;
        this.accountOperations = accountOperations;
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public void onCreate(YouFragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        userRepository.localAndSyncedUserInfo(accountOperations.getLoggedInUserUrn())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new YouSubscriber());
    }

    @Override
    public void onViewCreated(YouFragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        headerViewOpt = Optional.of(youViewFactory.create(view));
        bindUserIfPresent();
    }

    @Override
    public void onDestroyView(YouFragment fragment) {
        if (headerViewOpt.isPresent()) {
            headerViewOpt.get().unbind();
        }
        super.onDestroyView(fragment);
    }

    private void bindUserIfPresent() {
        if (headerViewOpt.isPresent() && youOpt.isPresent()) {
            final YouView headerView = headerViewOpt.get();
            final PropertySet you = youOpt.get();
            bindUser(headerView, you);
        }
    }

    private void bindUser(YouView headerView, PropertySet you) {
        headerView.setUsername(you.get(UserProperty.USERNAME));
        headerView.setUrn(you.get(UserProperty.URN));
        imageOperations.display(you.get(UserProperty.URN),
                ApiImageSize.getFullImageSize(resources),
                headerView.getProfileImageView());
    }

    private class YouSubscriber extends  DefaultSubscriber<PropertySet>{
        @Override
        public void onNext(PropertySet user) {
            youOpt = Optional.of(user);
            bindUserIfPresent();
        }
    }
}
