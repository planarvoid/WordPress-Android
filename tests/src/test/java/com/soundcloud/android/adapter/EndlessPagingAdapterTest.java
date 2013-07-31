package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class EndlessPagingAdapterTest {

    private EndlessPagingAdapter<Track> adapter = new EndlessPagingAdapter<Track>(10, R.layout.list_loading_item) {
        @Override
        protected void bindItemView(int position, View itemView) {
            ((TextView) itemView).setText(getItem(position).getTitle());
        }

        @Override
        protected View createItemView(int position, ViewGroup parent) {
            return new TextView(parent.getContext());
        }
    };

    @Test
    public void shouldReportAllItemsEnabledAsFalseSinceLoadingItemIsDisabled() {
        expect(adapter.areAllItemsEnabled()).toBeFalse();
    }

    @Test
    public void shouldLoadNextPage() {
        adapter.setLoading(false);
        expect(adapter.shouldLoadNextPage(0, 5, 5)).toBeTrue();
    }

    @Test
    public void shouldLoadNextPageWithOnePageLookAhead() {
        adapter.setLoading(false);
        expect(adapter.shouldLoadNextPage(0, 5, 2 * 5)).toBeTrue();
    }

    @Test
    public void shouldNotLoadNextPageIfAlreadyLoading() {
        adapter.setLoading(true);
        expect(adapter.shouldLoadNextPage(0, 5, 5)).toBeFalse();
    }

    @Test
    public void shouldNotLoadNextPageIfZeroItems() {
        adapter.setLoading(true);
        expect(adapter.shouldLoadNextPage(0, 5, 0)).toBeFalse();
        adapter.setLoading(false);
        expect(adapter.shouldLoadNextPage(0, 5, 0)).toBeFalse();
    }

    @Test
    public void shouldAddItems() {
        expect(adapter.getCount()).toBe(0);
        adapter.addItem(new Track());
        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void shouldGetItem() {
        Track item = new Track();
        adapter.addItem(item);
        expect(adapter.getItem(0)).toBe(item);
    }

    @Test
    public void shouldAdjustItemCountBasedOnLoadingState() {
        adapter.setLoading(true);
        expect(adapter.getCount()).toBe(1);
        adapter.setLoading(false);
        expect(adapter.getCount()).toBe(0);
    }

    @Test
    public void shouldDefaultToIdentityForItemIdFunction() {
        expect(adapter.getItemId(1)).toBe(1L);
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldCreateAndBindNewItemView() {
        Track item = new Track();
        item.setTitle("New track");
        adapter.addItem(item);

        View itemView = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(itemView).not.toBeNull();
        expect(((TextView) itemView).getText()).toEqual("New track");
    }

    @Test
    public void shouldConvertItemView() {
        Track item = new Track();
        item.setTitle("New track");
        adapter.addItem(item);

        TextView convertView = new TextView(Robolectric.application);
        convertView.setText("Old track");
        View itemView = adapter.getView(0, convertView, new FrameLayout(Robolectric.application));
        expect(itemView).toBe(convertView);
        expect(((TextView) itemView).getText()).toEqual("New track");
    }

    @Test
    public void shouldCreateProgressView() {
        adapter.setLoading(true);
        View progressView = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.list_loading)).not.toBeNull();
    }

    @Test
    public void shouldConvertProgressView() {
        adapter.setLoading(true);
        View convertView = LayoutInflater.from(Robolectric.application).inflate(R.layout.list_loading_item, null);
        View progressView = adapter.getView(0, convertView, new FrameLayout(Robolectric.application));
        expect(progressView).toBe(convertView);
    }
}
