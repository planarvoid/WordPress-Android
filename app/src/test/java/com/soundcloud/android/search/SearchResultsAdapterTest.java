package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsAdapterTest {

    @InjectMocks
    private SearchResultsAdapter adapter;

    @Mock
    private SearchResultPresenter presenter;

    @Test
    public void shouldDifferentiateItemViewTypes() {
        adapter.addItem(new User());
        adapter.addItem(new Track());

        expect(adapter.getItemViewType(0)).toEqual(1);
        expect(adapter.getItemViewType(1)).toEqual(0);
    }

}
