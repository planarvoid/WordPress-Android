package com.soundcloud.android.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import java.util.Arrays;

public class PagingListItemAdapterTest extends AndroidUnitTest {

    @Mock private CellRenderer cellRenderer;
    @Mock private AbsListView absListView;
    @Mock private View rowView;
    @Captor ArgumentCaptor<View.OnClickListener> clickListener;

    private PagingListItemAdapter adapter;

    @Before
    public void setup() {
        adapter = new PagingListItemAdapter(R.layout.ak_list_loading_item, cellRenderer);
        Observable.just(Arrays.asList("one", "two", "three")).subscribe(adapter);
    }

    @Test
    public void shouldReportAllItemsEnabledAsFalseSinceLoadingItemIsDisabled() {
        assertThat(adapter.areAllItemsEnabled()).isFalse();
    }

    @Test
    public void itemRowsShouldBeClickable() {
        assertThat(adapter.getItemCount()).isGreaterThan(0);
        for (int index = 0; index < adapter.getItemCount(); index++) {
            assertThat(adapter.isEnabled(index)).isTrue();
        }
    }

    @Test
    public void appendRowShouldBeProgressRowWhenInLoadingState() {
        adapter.setLoading();
        assertThat(adapter.isEnabled(adapter.getItemCount() - 1)).isFalse(); // progress should not be clickable
    }

    @Test
    public void appendRowShouldBeErrorRowWhenLoadingDataFails() {
        adapter.onError(new Exception());

        assertThat(adapter.getItemCount()).isEqualTo(4); // 3 data items + 1 error row
        assertThat(adapter.isEnabled(adapter.getItemCount() - 1)).isFalse(); // error has a custom click handler
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        assertThat(adapter.getViewTypeCount()).isEqualTo(2);
    }

    @Test
    public void shouldCreateNormalItemRowUsingPresenter() {
        final FrameLayout parent = new FrameLayout(context());
        adapter.getView(0, null, parent);
        verify(cellRenderer).createItemView(parent);
    }

    @Test
    public void shouldCreateProgressRow() {
        adapter.setLoading();

        View progressView = adapter.getView(adapter.getItemCount() - 1, null, new FrameLayout(context()));
        assertThat(progressView).isNotNull();
        assertThat(progressView.findViewById(R.id.ak_list_progress).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(progressView.findViewById(R.id.ak_list_retry).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldConvertProgressRow() {
        adapter.setLoading();

        View convertView = LayoutInflater.from(context()).inflate(R.layout.ak_list_loading_item, null);
        View progressView = adapter.getView(adapter.getItemCount() - 1, convertView, new FrameLayout(context()));
        assertThat(progressView).isSameAs(convertView);
    }

    @Test
    public void shouldCreateErrorRow() {
        adapter.onError(new Exception());

        View errorView = adapter.getView(adapter.getItemCount() - 1, null, new FrameLayout(context()));
        assertThat(errorView).isNotNull();
        assertThat(errorView.findViewById(R.id.ak_list_progress).getVisibility()).isEqualTo(View.GONE);
        assertThat(errorView.findViewById(R.id.ak_list_retry).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void shouldSetCustomOnErrorRetryListenerForErrorRow() {
        when(rowView.findViewById(anyInt())).thenReturn(mock(View.class));
        adapter.onError(new Exception());

        View.OnClickListener listener = mock(View.OnClickListener.class);
        adapter.setOnErrorRetryListener(listener);

        adapter.getView(adapter.getItemCount() - 1, rowView, new FrameLayout(context()));
        verify(rowView).setOnClickListener(clickListener.capture());
        assertThat(clickListener.getValue()).isSameAs(listener);
    }

}
