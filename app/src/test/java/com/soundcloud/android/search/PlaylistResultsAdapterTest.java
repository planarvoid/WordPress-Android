package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.search.PlaylistResultsAdapter.*;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistResultsAdapterTest {

    private PlaylistResultsAdapter adapter;

    @Mock
    private ImageOperations imageOperations;

    @Before
    public void setUp() throws Exception {
        adapter = new PlaylistResultsAdapter(imageOperations);
    }

    @Test
    public void shouldCreateItemView() {
        View itemView = adapter.createItemView(0, new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(itemView.getTag()).not.toBeNull(); // contains the private ViewHolder instance
        expect(itemView.findViewById(R.id.suggested_track_image)).not.toBeNull();
        expect(itemView.findViewById(R.id.username)).not.toBeNull();
        expect(itemView.findViewById(R.id.title)).not.toBeNull();
    }

    @Test
    public void shouldBindItemView() throws CreateModelException {
        PlaylistSummary playlist = TestHelper.getModelFactory().createModel(PlaylistSummary.class);
        adapter.addItem(playlist);

        View itemView = adapter.createItemView(0, new FrameLayout(Robolectric.application));
        adapter.bindItemView(0, itemView);

        ItemViewHolder viewHolder = (ItemViewHolder) itemView.getTag();
        expect(viewHolder.title.getText()).toEqual(playlist.getTitle());
        expect(viewHolder.username.getText()).toEqual(playlist.getUsername());
        expect(viewHolder.tagList.getText()).toEqual("tag1, tag2");
        expect(viewHolder.trackCount.getText()).toEqual("5");
    }

}
