package com.soundcloud.android.search.topresults;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import javax.inject.Inject;

public class TopResultsBucketPresenter {

    private final EventTracker eventTracker;
    private final ReferringEventProvider referringEventProvider;
    private Disposable disposable;

    @Inject
    public TopResultsBucketPresenter(EventTracker eventTracker, ReferringEventProvider referringEventProvider) {
        this.eventTracker = eventTracker;
        this.referringEventProvider = referringEventProvider;
    }

    public void attachView(TopResultsBucketView topResultsBucketActivity) {
        final Screen screen = topResultsBucketActivity.getKind().toSearchType().getScreen(topResultsBucketActivity.isPremium());
        final Optional<Urn> queryUrn = topResultsBucketActivity.getQueryUrn();
        disposable = topResultsBucketActivity.enterScreenTimestamp()
                                             .subscribeWith(LambdaObserver.onNext(event -> eventTracker.trackScreen(ScreenEvent.create(screen.get(), queryUrn),
                                                                                                                    referringEventProvider.getReferringEvent())));
    }

    public void detachView() {
        disposable.dispose();
    }

    interface TopResultsBucketView {

        TopResults.Bucket.Kind getKind();

        boolean isPremium();

        Optional<Urn> getQueryUrn();

        Observable<Long> enterScreenTimestamp();
    }
}
