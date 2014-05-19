package com.soundcloud.android.stream;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.PropertySet;
import rx.observables.ConnectableObservable;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class SoundStreamFragment extends Fragment implements OnRefreshListener {

    @Inject
    SoundStreamOperations soundStreamOperations;
    @Inject
    StreamItemAdapter adapter;
    @Inject
    PullToRefreshController pullToRefreshController;
    @Inject
    EventBus eventBus;

    public SoundStreamFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sound_stream_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = ((ListView) view.findViewById(android.R.id.list));
        listView.setAdapter(adapter);
        listView.setOnScrollListener(adapter);

        PullToRefreshLayout ptrLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        pullToRefreshController.attach(getActivity(), ptrLayout, this);

        final ConnectableObservable<Page<List<PropertySet>>> observable =
                soundStreamOperations.existingStreamItems().observeOn(mainThread()).publish();
        observable.subscribe(adapter);
        observable.connect();
    }

    @Override
    public void onRefreshStarted(View view) {
        soundStreamOperations.updatedStreamItems().subscribe(new StreamItemSubscriber());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private final class StreamItemSubscriber extends DefaultSubscriber<Page<List<PropertySet>>> {
        @Override
        public void onCompleted() {
            pullToRefreshController.stopRefreshing();
        }

        @Override
        public void onError(Throwable e) {
            pullToRefreshController.stopRefreshing();
            super.onError(e);
        }
    }

    // PLEASE IGNORE THIS GUY FOR NOW.
    // I just need something quick and dirty for testing right now.
    // I will fully revisit how we do adapters and row binding in a later step.
    static class StreamItemAdapter extends EndlessPagingAdapter<PropertySet> {

        @Inject
        protected StreamItemAdapter() {
            super(Consts.LIST_PAGE_SIZE);
        }

        @Override
        protected View createItemView(int position, ViewGroup parent) {
            return new TextView(parent.getContext());
        }

        @Override
        protected void bindItemView(int position, View itemView) {
            final PropertySet propertySet = getItem(position);
            final Urn soundUrn = propertySet.get(StreamItemProperty.SOUND_URN);
            final String soundTitle = propertySet.get(StreamItemProperty.SOUND_TITLE);
            final String poster = propertySet.get(StreamItemProperty.POSTER);
            final Date createdAt = propertySet.get(StreamItemProperty.CREATED_AT);
            final boolean isRepost = propertySet.get(StreamItemProperty.REPOST);

            ((TextView) itemView).setText(
                    createdAt + "\n" +
                            soundUrn + "\n" + soundTitle + "\n" + (isRepost ? "reposter: " + poster : "")
            );
        }
    }
}
