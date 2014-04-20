package com.soundcloud.android.stream;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.model.Urn;
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

        getListView().setOnScrollListener(adapter);

        soundStreamOperations.getStreamItems().observeOn(mainThread()).subscribe(adapter);
    }

    private static final class StreamItemAdapter extends EndlessPagingAdapter<PropertySet> {

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
