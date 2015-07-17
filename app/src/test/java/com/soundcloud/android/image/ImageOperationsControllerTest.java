package com.soundcloud.android.image;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class ImageOperationsControllerTest {

    private ImageOperationsController imageOperationsController;

    @Mock private ImageOperations imageOperations;
    @Mock private AppCompatActivity activity;

    @Before
    public void setUp() throws Exception {
        imageOperationsController = new ImageOperationsController(imageOperations);
    }

    @Test
    public void resumeImageOperationsOnResume() {
        imageOperationsController.onResume(activity);

        verify(imageOperations).resume();
    }
}