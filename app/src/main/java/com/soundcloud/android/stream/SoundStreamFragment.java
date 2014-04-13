package com.soundcloud.android.stream;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.PropertySet;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;

public class SoundStreamFragment extends ListFragment {

    @Inject
    SoundStreamOperations soundStreamOperations;

    private final StreamItemAdapter adapter;

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

    private final class StreamItemSubscriber extends DefaultSubscriber<PropertySet> {

        @Override
        public void onNext(PropertySet streamItem) {
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

    private static final class StreamItemAdapter extends ItemAdapter<PropertySet> {

        protected StreamItemAdapter() {
            super(10);
        }

        @Override
        protected View createItemView(int position, ViewGroup parent) {
            return new TextView(parent.getContext());
        }

        @Override
        protected void bindItemView(int position, View itemView) {
            final PropertySet propertySet = getItem(position);
            final String soundUrn = propertySet.get(StreamItemProperty.SOUND_URN);
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
