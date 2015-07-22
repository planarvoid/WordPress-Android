package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class InlinePlaylistTracksAdapterTest {

    @InjectMocks
    private InlinePlaylistTracksAdapter adapter;

    @Mock private EmptyPlaylistTracksRenderer emptyRowRenderer;

    @Test
    public void reports2DifferentItemTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void hasCountOf1WithNoDataAndInlineEmptyViews() throws Exception {
        expect(adapter.getItemCount()).toBe(1);
    }

    @Test
    public void hasContentItemsShouldBeFalseWhenNoItemsHaveBeenSet() {
        expect(adapter.hasContentItems()).toBeFalse();
    }

    @Test
    public void hasContentItemsShouldBeTrueOnceItemsHaveBeenAdded() {
        adapter.addItem(TrackItem.from(PropertySet.create()));
        expect(adapter.hasContentItems()).toBeTrue();
    }
}
