package com.soundcloud.android.presentation;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PagingItemAdapterTest {

    @Mock private CellRenderer cellRenderer;
    @Mock private AbsListView absListView;
    @Mock private View rowView;

    private PagingItemAdapter adapter;

    @Before
    public void setup() {
        adapter = new PagingItemAdapter(R.layout.list_loading_item, cellRenderer);
        Observable.just(Arrays.asList("one", "two", "three")).subscribe(adapter);
    }

    @Test
    public void shouldReportAllItemsEnabledAsFalseSinceLoadingItemIsDisabled() {
        expect(adapter.areAllItemsEnabled()).toBeFalse();
    }

    @Test
    public void itemRowsShouldBeClickable() {
        expect(adapter.getItemCount()).toBeGreaterThan(0);
        for (int index = 0; index < adapter.getItemCount(); index++) {
            expect(adapter.isEnabled(index)).toBeTrue();
        }
    }

    @Test
    public void appendRowShouldBeProgressRowWhenInLoadingState() {
        adapter.setLoading();
        expect(adapter.isEnabled(adapter.getItemCount() - 1)).toBeFalse(); // progress should not be clickable
    }

    @Test
    public void appendRowShouldBeErrorRowWhenLoadingDataFails() {
        adapter.onError(new Exception());

        expect(adapter.getItemCount()).toBe(4); // 3 data items + 1 error row
        expect(adapter.isEnabled(adapter.getItemCount() - 1)).toBeFalse(); // error has a custom click handler
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldCreateNormalItemRowUsingPresenter() {
        final FrameLayout parent = new FrameLayout(Robolectric.application);
        adapter.getView(0, null, parent);
        verify(cellRenderer).createItemView(parent);
    }

    @Test
    public void shouldCreateProgressRow() {
        adapter.setLoading();

        View progressView = adapter.getView(adapter.getItemCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.list_loading_view).getVisibility()).toBe(View.VISIBLE);
        expect(progressView.findViewById(R.id.list_loading_retry_view).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldConvertProgressRow() {
        adapter.setLoading();

        View convertView = LayoutInflater.from(Robolectric.application).inflate(R.layout.list_loading_item, null);
        View progressView = adapter.getView(adapter.getItemCount() - 1, convertView, new FrameLayout(Robolectric.application));
        expect(progressView).toBe(convertView);
    }

    @Test
    public void shouldCreateErrorRow() {
        adapter.onError(new Exception());

        View errorView = adapter.getView(adapter.getItemCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(errorView).not.toBeNull();
        expect(errorView.findViewById(R.id.list_loading_view).getVisibility()).toBe(View.GONE);
        expect(errorView.findViewById(R.id.list_loading_retry_view).getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void shouldSetCustomOnErrorRetryListenerForErrorRow() {
        adapter.onError(new Exception());

        View.OnClickListener listener = mock(View.OnClickListener.class);
        adapter.setOnErrorRetryListener(listener);

        final View errorRow = adapter.getView(adapter.getItemCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(Robolectric.shadowOf(errorRow).getOnClickListener()).toBe(listener);
    }

}
