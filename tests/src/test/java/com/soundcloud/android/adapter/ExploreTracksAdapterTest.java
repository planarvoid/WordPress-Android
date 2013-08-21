package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksAdapterTest {

    private ExploreTracksAdapter adapter;

    @Test
    public void shouldCreateItemView() {
        adapter = new ExploreTracksAdapter();
        View itemView = adapter.createItemView(0, new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(itemView.getTag()).not.toBeNull(); // contains the private ViewHolder instance
        expect(itemView.findViewById(R.id.suggested_track_image)).not.toBeNull();
        expect(itemView.findViewById(R.id.username)).not.toBeNull();
        expect(itemView.findViewById(R.id.title)).not.toBeNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        adapter = new ExploreTracksAdapter();
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        adapter.addItem(track);

        View itemView = mock(View.class);
        ExploreTracksAdapter.ItemViewHolder viewHolder = mock(ExploreTracksAdapter.ItemViewHolder.class);
        viewHolder.imageView = new ImageView(Robolectric.application);
        viewHolder.title = new TextView(Robolectric.application);
        viewHolder.username = new TextView(Robolectric.application);
        when(itemView.getTag()).thenReturn(viewHolder);

        adapter.bindItemView(0, itemView);

        expect(viewHolder.title.getText()).toEqual(track.getTitle());
        expect(viewHolder.username.getText()).toEqual(track.getUserName());
    }
}
