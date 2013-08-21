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
    public void shouldAdjustItemCountBasedOnLoadingState() {
        adapter.setDisplayProgressItem(true);
        expect(adapter.getCount()).toBe(1);
        adapter.setDisplayProgressItem(false);
        expect(adapter.getCount()).toBe(0);
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldCreateProgressView() {
        adapter.setDisplayProgressItem(true);
        View progressView = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.list_loading)).not.toBeNull();
    }

    @Test
    public void shouldConvertProgressView() {
        adapter.setDisplayProgressItem(true);
        View convertView = LayoutInflater.from(Robolectric.application).inflate(R.layout.list_loading_item, null);
        View progressView = adapter.getView(0, convertView, new FrameLayout(Robolectric.application));
        expect(progressView).toBe(convertView);
    }
}
