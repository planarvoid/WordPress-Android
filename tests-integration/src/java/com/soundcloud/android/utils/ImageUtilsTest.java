package com.soundcloud.android.utils;

import com.soundcloud.android.tests.ScAndroidTestCase;

import java.io.File;

public class ImageUtilsTest extends ScAndroidTestCase {

    private void resize(String file) throws Exception {
        File landscape = prepareAsset(file);
        File resized = externalPath(newFilename(file, "_resized"));

        assertTrue("resizeImageFile returned false", ImageUtils.resizeImageFile(landscape, resized, 800, 800));
        assertTrue("resized file should exist", resized.exists());
        double factor = (double) resized.length() / (double) landscape.length();
        assertTrue(String.format("resized file should be smaller (factor=%.2f)", factor), factor < 1.0d);
    }

    public void testResizeLandscape() throws Exception {
        resize("image/landscape.jpg");
    }

    public void testResizePortrait() throws Exception {
        resize("image/portrait.jpg");
    }

    public void testResizeLargePortrait() throws Exception {
        resize("image/large-portrait.jpg");
    }
}
