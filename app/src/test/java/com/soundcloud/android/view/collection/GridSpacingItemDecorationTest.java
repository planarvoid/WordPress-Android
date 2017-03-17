package com.soundcloud.android.view.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class GridSpacingItemDecorationTest extends AndroidUnitTest {

    private static final int SPAN_COUNT = 3;
    private static final int ITEM_MARGIN = 16;

    private GridSpacingItemDecoration itemDecoration;
    private Rect outRect;

    @Mock GridLayoutManager.LayoutParams layoutParams;
    @Mock RecyclerView.State recyclerViewState;
    @Mock RecyclerView recyclerView;
    @Mock View view;

    @Before
    public void setUp() throws Exception {
        itemDecoration = new GridSpacingItemDecoration(ITEM_MARGIN, SPAN_COUNT);
        outRect = new Rect();

        when(view.getLayoutParams()).thenReturn(layoutParams);
    }

    @Test
    public void shouldNotAffectItemsWithSpanSizeDifferentThanOne() {
        when(layoutParams.getSpanSize()).thenReturn(SPAN_COUNT);

        itemDecoration.getItemOffsets(outRect, view, recyclerView, recyclerViewState);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfNotAttachedToGridLayoutManager() {
        ViewGroup.LayoutParams layoutParams = new RecyclerView.LayoutParams(0, 0);
        when(view.getLayoutParams()).thenReturn(layoutParams);

        itemDecoration.getItemOffsets(outRect, view, recyclerView, recyclerViewState);

        assertThat(outRect.isEmpty()).isTrue();
    }

    @Test
    public void shouldCalculateProperOffsetForLeftmostItem() {
        when(layoutParams.getSpanSize()).thenReturn(1);
        when(layoutParams.getSpanIndex()).thenReturn(0);

        itemDecoration.getItemOffsets(outRect, view, recyclerView, recyclerViewState);

        assertThat(outRect).isEqualTo(new Rect(ITEM_MARGIN, ITEM_MARGIN, 5, ITEM_MARGIN));
    }

    @Test
    public void shouldCalculateProperOffsetForMidMostItem() {
        when(layoutParams.getSpanSize()).thenReturn(1);
        when(layoutParams.getSpanIndex()).thenReturn(1);

        itemDecoration.getItemOffsets(outRect, view, recyclerView, recyclerViewState);

        assertThat(outRect).isEqualTo(new Rect(11, ITEM_MARGIN, 10, ITEM_MARGIN));
    }

    @Test
    public void shouldCalculateProperOffsetForRightmostItem() {
        when(layoutParams.getSpanSize()).thenReturn(1);
        when(layoutParams.getSpanIndex()).thenReturn(2);

        itemDecoration.getItemOffsets(outRect, view, recyclerView, recyclerViewState);

        assertThat(outRect).isEqualTo(new Rect(6, ITEM_MARGIN, ITEM_MARGIN, ITEM_MARGIN));
    }
}