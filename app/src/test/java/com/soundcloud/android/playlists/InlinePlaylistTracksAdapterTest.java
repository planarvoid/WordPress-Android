package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class InlinePlaylistTracksAdapterTest extends AndroidUnitTest {

    @InjectMocks
    private InlinePlaylistTracksAdapter adapter;

    @Mock private EmptyPlaylistTracksRenderer emptyRowRenderer;

    @Test
    public void reports2DifferentItemTypes() {
        assertThat(adapter.getViewTypeCount()).isEqualTo(2);
    }

    @Test
    public void hasCountOf1WithNoDataAndInlineEmptyViews() throws Exception {
        assertThat(adapter.getItemCount()).isEqualTo(1);
    }

    @Test
    public void hasContentItemsShouldBeFalseWhenNoItemsHaveBeenSet() {
        assertThat(adapter.hasContentItems()).isFalse();
    }

    @Test
    public void hasContentItemsShouldBeTrueOnceItemsHaveBeenAdded() {
        adapter.addItem(TrackItem.from(PropertySet.create()));
        assertThat(adapter.hasContentItems()).isTrue();
    }
}
