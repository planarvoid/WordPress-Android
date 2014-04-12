package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.SoundStreamOperations.StreamItemModel;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class SoundStreamFragment extends ListFragment {

    @Inject
    SoundStreamOperations soundStreamOperations;

    private StreamItemAdapter adapter;

    public SoundStreamFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        adapter = new StreamItemAdapter();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(adapter);
        soundStreamOperations.getStreamItems().subscribe(new StreamItemSubscriber());
    }

    private final class StreamItemSubscriber extends DefaultSubscriber<StreamItemModel> {

        @Override
        public void onNext(StreamItemModel streamItem) {
            adapter.addItem(streamItem);
        }

        @Override
        public void onCompleted() {
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
            super.onError(e);
        }
    }

    private static final class StreamItemAdapter extends ItemAdapter<StreamItemModel> {

        protected StreamItemAdapter() {
            super(10);
        }

        @Override
        protected View createItemView(int position, ViewGroup parent) {
            return new TextView(parent.getContext());
        }

        @Override
        protected void bindItemView(int position, View itemView) {
            final StreamItemModel item = getItem(position);
            ((TextView) itemView).setText(
                    item.soundUrn.toString() + "\n" + item.trackTitle + "\n" + "repost: " + item.isRepost);
        }
    }
}
