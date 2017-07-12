package com.soundcloud.android.analytics.eventlogger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.playback.playqueue.SmoothScrollLinearLayoutManager;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;

import javax.inject.Inject;

class DevEventLoggerMonitorPresenter extends DefaultActivityLightCycle<AppCompatActivity>
        implements DevTrackingRecordAdapter.Listener {

    private static final String TAG = "DevEventLoggerMonitorDetailsDialog";

    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.delete_all) Button deleteAll;

    private final SmoothScrollLinearLayoutManager layoutManager;
    private final DevTrackingRecordsProvider trackingRecordsProvider;
    private final DevTrackingRecordAdapter adapter;

    private AppCompatActivity activity;
    private Unbinder unbinder;
    private Disposable disposable = RxUtils.emptyDisposable();

    @Inject
    DevEventLoggerMonitorPresenter(SmoothScrollLinearLayoutManager layoutManager,
                                   DevTrackingRecordsProvider trackingRecordsProvider,
                                   DevTrackingRecordAdapter adapter) {
        this.layoutManager = layoutManager;
        this.trackingRecordsProvider = trackingRecordsProvider;
        this.adapter = adapter;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        unbinder = ButterKnife.bind(this, activity);
        setupRecyclerView();
        setupDeleteAllButton();
        setupTrackingRecordsAction();
    }

    @Override
    public void onItemClicked(TrackingRecord trackingRecord) {
        DevEventLoggerMonitorDetailsDialog.create(trackingRecord)
                                          .show(activity.getSupportFragmentManager(), TAG);
    }

    private void setupRecyclerView() {
        adapter.setListener(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void setupDeleteAllButton() {
        deleteAll.setOnClickListener(v -> trackingRecordsProvider.deleteAll());
    }

    private void setupTrackingRecordsAction() {
        disposable = trackingRecordsProvider.action()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeWith(new DevTrackingRecordsProviderSubscriber());
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        unbinder.unbind();
        disposable.dispose();
        this.activity = null;
    }

    private final class DevTrackingRecordsProviderSubscriber extends DefaultObserver<DevTrackingRecordsProvider.Action> {
        @Override
        public void onNext(DevTrackingRecordsProvider.Action action) {
            super.onNext(action);
            adapter.replaceItems(trackingRecordsProvider.latest());
        }
    }
}
