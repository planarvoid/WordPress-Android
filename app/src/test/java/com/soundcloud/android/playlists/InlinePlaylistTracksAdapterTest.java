package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class InlinePlaylistTracksAdapterTest {

    @InjectMocks
    private InlinePlaylistTracksAdapter adapter;

    @Mock
    private EmptyPlaylistTracksPresenter emptyRowPresenter;
    @Mock
    private CellPresenter<Track> trackRowPresenter;

    @Test
    public void reports2DifferentItemTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void hasCountOf1WithNoDataAndInlineEmptyViews() throws Exception {
        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void hasContentItemsShouldBeFalseWhenNoItemsHaveBeenSet() {
        expect(adapter.hasContentItems()).toBeFalse();
    }

    @Test
    public void hasContentItemsShouldBeTrueOnceItemsHaveBeenAdded() {
        adapter.addItem(PropertySet.create(1));
        expect(adapter.hasContentItems()).toBeTrue();
    }
}
