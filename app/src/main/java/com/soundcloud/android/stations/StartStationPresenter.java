package com.soundcloud.android.stations;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.DelayedLoadingDialogPresenter;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import android.content.Context;
import android.content.DialogInterface;

import javax.inject.Inject;

public class StartStationPresenter {
    private final Func1<Station, Observable<PlaybackResult>> toPlaybackResult = new Func1<Station, Observable<PlaybackResult>>() {
        @Override
        public Observable<PlaybackResult> call(Station station) {
            checkArgument(!station.getTracks().isEmpty(), "The station does not have any tracks.");
            final PlaySessionSource playSessionSource = PlaySessionSource.forStation(screenProvider.getLastScreenTag(), station.getUrn());
            return playbackOperations.playStation(station.getUrn(), station.getTracks(), playSessionSource, station.getPreviousPosition());
        }
    };

    private final DelayedLoadingDialogPresenter.Builder dialogBuilder;
    private final StationsOperations stationsOperations;
    private final PlaybackOperations playbackOperations;
    private final EventBus eventBus;
    private final PlaybackToastHelper playbackToastHelper;
    private final ScreenProvider screenProvider;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public StartStationPresenter(
            DelayedLoadingDialogPresenter.Builder dialogBuilder,
            StationsOperations stationsOperations,
            PlaybackOperations playbackOperations,
            EventBus eventBus,
            PlaybackToastHelper playbackToastHelper,
            ScreenProvider screenProvider) {
        this.dialogBuilder = dialogBuilder;
        this.stationsOperations = stationsOperations;
        this.playbackOperations = playbackOperations;
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
        this.screenProvider = screenProvider;
    }

    public void startStation(Context context, Urn stationUrn) {
        DelayedLoadingDialogPresenter delayedLoadingDialogPresenter = dialogBuilder
                .setLoadingMessage(context.getString(R.string.starting_radio))
                .setOnErrorToastText(context.getString(R.string.unable_to_start_radio))
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        subscription.unsubscribe();
                    }
                })
                .create()
                .show(context);

        subscription = stationsOperations
                .station(stationUrn)
                .flatMap(toPlaybackResult)
                .subscribe(new ExpandAndDismissDialogSubscriber(context, eventBus, playbackToastHelper, delayedLoadingDialogPresenter));
    }

    private static class ExpandAndDismissDialogSubscriber extends ExpandPlayerSubscriber {

        private final Context context;
        private final DelayedLoadingDialogPresenter delayedLoadingDialogPresenter;

        public ExpandAndDismissDialogSubscriber(Context context,
                                                EventBus eventBus,
                                                PlaybackToastHelper playbackToastHelper,
                                                DelayedLoadingDialogPresenter delayedLoadingDialogPresenter) {
            super(eventBus, playbackToastHelper);
            this.context = context;
            this.delayedLoadingDialogPresenter = delayedLoadingDialogPresenter;
        }

        @Override
        public void onError(Throwable e) {
            delayedLoadingDialogPresenter.onError(context);
            // Call on error after dismissing the dialog in order to report errors to Fabric.
            super.onError(e);
        }

        @Override
        public void onNext(PlaybackResult result) {
            if (result.isSuccess()) {
                expandPlayer();
                delayedLoadingDialogPresenter.onSuccess();
            } else {
                delayedLoadingDialogPresenter.onError(context);
            }
        }
    }
}
