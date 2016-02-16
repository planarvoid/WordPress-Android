package com.soundcloud.android.offline;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class OfflineContentOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> {
    public static final String EXTRA_PAGE = "EXTRA_PAGE";
    public static final int PAGE_1 = 1;
    public static final int PAGE_2 = 2;

    private final EventBus eventBus;
    private final Navigator navigator;
    private final OfflineContentOperations offlineContentOperations;

    @Inject
    OfflineContentOnboardingPresenter(EventBus eventBus, Navigator navigator,
                                      OfflineContentOperations offlineContentOperations) {
        this.eventBus = eventBus;
        this.navigator = navigator;
        this.offlineContentOperations = offlineContentOperations;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        setUpPage(activity);
    }

    private void setUpPage(AppCompatActivity activity) {
        final int expectedPage = activity.getIntent().getIntExtra(EXTRA_PAGE, PAGE_1);
        switch (expectedPage) {
            case PAGE_1:
                setPage1(activity);
                break;
            case PAGE_2:
                setPage2(activity);
                break;
            default:
                throw new IllegalStateException("Unknown page:" + expectedPage);
        }
    }

    private void setPage1(AppCompatActivity activity) {
        LayoutInflater.from(activity).inflate(R.layout.offline_content_onboarding_page_1, container(activity));
        setGoToPage1Listener(activity);
    }

    private ViewGroup container(AppCompatActivity activity) {
        return (ViewGroup) activity.findViewById(R.id.container);
    }

    private void setGoToPage1Listener(AppCompatActivity activity) {
        activity.findViewById(R.id.next_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigator.openOfflineContentOnboarding(view.getContext(), PAGE_2);
            }
        });
    }

    private void setPage2(AppCompatActivity activity) {
        LayoutInflater.from(activity).inflate(R.layout.offline_content_onboarding_page_2, container(activity));
        setSaveEverythingListener(activity);
        setChooseWhatToSaveListener(activity);
    }

    private void setChooseWhatToSaveListener(final AppCompatActivity activity) {
        activity.findViewById(R.id.page_1_choose_what_to_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigator.openCollectionAsRootScreen(activity);
                eventBus.publish(EventQueue.TRACKING,
                        OfflineInteractionEvent.fromOnboardingWithManualSync());
            }
        });
    }

    private void setSaveEverythingListener(final AppCompatActivity activity) {
        activity.findViewById(R.id.page_1_save_everything).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fireAndForget(offlineContentOperations.enableOfflineCollection());
                navigator.openCollectionAsRootScreen(activity);
                eventBus.publish(EventQueue.TRACKING,
                        OfflineInteractionEvent.fromOnboardingWithAutomaticSync());
            }
        });
    }
}
