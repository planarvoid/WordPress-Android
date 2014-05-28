package com.soundcloud.android.stream;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import java.util.List;

public class SoundStreamFragment extends Fragment
        implements RefreshableListComponent<ConnectableObservable<Page<List<PropertySet>>>> {

    @Inject
    SoundStreamOperations soundStreamOperations;
    @Inject
    SoundStreamAdapter adapter;
    @Inject
    ListViewController listViewController;
    @Inject
    PullToRefreshController pullToRefreshController;
    @Inject
    EventBus eventBus;

    private ConnectableObservable<Page<List<PropertySet>>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public SoundStreamFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public ConnectableObservable<Page<List<PropertySet>>> buildObservable() {
        final ConnectableObservable<Page<List<PropertySet>>> observable =
                soundStreamOperations.existingStreamItems().observeOn(mainThread()).replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<Page<List<PropertySet>>> observable) {
        this.observable = observable;
        this.connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public ConnectableObservable<Page<List<PropertySet>>> refreshObservable() {
        return soundStreamOperations.updatedStreamItems().observeOn(mainThread()).replay();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectObservable(buildObservable());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sound_stream_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.onViewCreated(this, observable, view, adapter, adapter);
        pullToRefreshController.onViewCreated(this, observable, adapter);
    }

    @Override
    public void onDestroyView() {
        listViewController.onDestroyView();
        pullToRefreshController.onDestroyView();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

}
