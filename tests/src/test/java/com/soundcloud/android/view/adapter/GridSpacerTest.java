package com.soundcloud.android.view.adapter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class GridSpacerTest {

    @Mock
    View convertView;
    @Mock
    Resources resources;

    private final static int COL_RES_ID = 1;
    private final static int LEFT_RIGHT_RES_ID = 2;
    private final static int TOP_BOTTOM_RES_ID = 3;

    private final static int SPACING_LEFT_RIGHT = 10;
    private final static int SPACING_TOP_BOTTOM = 5;

    @Before
    public void setUp() throws Exception {
        when(convertView.getResources()).thenReturn(resources);
        when(resources.getDimensionPixelSize(LEFT_RIGHT_RES_ID)).thenReturn(SPACING_LEFT_RIGHT);
        when(resources.getDimensionPixelSize(TOP_BOTTOM_RES_ID)).thenReturn(SPACING_TOP_BOTTOM);
    }

    @Test
    public void shouldSetPaddingOnOneColumnListTop(){
        configureItem(1, 0, 3);
        verify(convertView).setPadding(SPACING_LEFT_RIGHT, SPACING_TOP_BOTTOM, SPACING_LEFT_RIGHT, 0);
    }

    @Test
    public void shouldSetPaddingOnOneColumnListMiddle(){
        configureItem(1, 1, 3);
        verify(convertView).setPadding(SPACING_LEFT_RIGHT, 0, SPACING_LEFT_RIGHT, 0);
    }

    @Test
    public void shouldSetPaddingOnOneColumnListBottom(){
        configureItem(1, 2, 3);
        verify(convertView).setPadding(SPACING_LEFT_RIGHT, 0, SPACING_LEFT_RIGHT, SPACING_TOP_BOTTOM);
    }

    @Test
    public void shouldSetPaddingOnTwoColumnListTopLeft(){
        configureItem(2, 0, 6);
        verify(convertView).setPadding(SPACING_LEFT_RIGHT, SPACING_TOP_BOTTOM, 0, 0);
    }

    @Test
    public void shouldSetPaddingOnTwoColumnListTopRight(){
        configureItem(2, 1, 6);
        verify(convertView).setPadding(0, SPACING_TOP_BOTTOM, SPACING_LEFT_RIGHT, 0);
    }

    @Test
    public void shouldSetPaddingOnTwoColumnListMiddleLeft(){
        configureItem(2, 2, 6);
        verify(convertView).setPadding(SPACING_LEFT_RIGHT, 0, 0, 0);
    }

    @Test
    public void shouldSetPaddingOnTwoColumnListMiddleRight(){
        configureItem(2, 3, 6);
        verify(convertView).setPadding(0, 0, SPACING_LEFT_RIGHT, 0);
    }

    @Test
    public void shouldSetPaddingOnTwoColumnListBottomLeft(){
        configureItem(2, 4, 6);
        verify(convertView).setPadding(SPACING_LEFT_RIGHT, 0, 0, SPACING_TOP_BOTTOM);
    }

    @Test
    public void shouldSetPaddingOnTwoColumnListBottomRight(){
        configureItem(2, 5, 6);
        verify(convertView).setPadding(0, 0, SPACING_LEFT_RIGHT, SPACING_TOP_BOTTOM);
    }

    private void configureItem(int columns, int position, int totalItems){
        when(resources.getInteger(COL_RES_ID)).thenReturn(columns);
        new GridSpacer(COL_RES_ID, LEFT_RIGHT_RES_ID, TOP_BOTTOM_RES_ID).configureItemPadding(convertView, position, totalItems);
    }

}
