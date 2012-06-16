package com.soundcloud.android.utils;

import com.soundcloud.android.tests.ScAndroidTestCase;

import android.graphics.BitmapFactory;
import android.test.FlakyTest;

import java.io.File;

public class ImageUtilsTest extends ScAndroidTestCase {
    private static final String IMAGE_LANDSCAPE_JPG      = "image/landscape.jpg";
    private static final String IMAGE_PORTRAIT_JPG       = "image/portrait.jpg";
    private static final String IMAGE_LARGE_PORTRAIT_JPG = "image/large-portrait.jpg";

    private File resize(String file) throws Exception {
        System.gc();

        File input = prepareAsset(file);
        File resized = externalPath(newFilename(file, "_resized"));

        assertTrue("resizeImageFile returned false", ImageUtils.resizeImageFile(input, resized, 800, 800));
        assertTrue("resized file should exist", resized.exists());
        double factor = (double) resized.length() / (double) input.length();
        assertTrue(String.format("resized file should be smaller (factor=%.2f)", factor), factor < 1.0d);
        return resized;
    }

    public void testResizeLandscape() throws Exception {
        File resized = resize(IMAGE_LANDSCAPE_JPG);
        BitmapFactory.Options opts = ImageUtils.decode(resized);
        assertEquals(600, opts.outHeight);
        assertEquals(800, opts.outWidth);
        assertEquals("image/jpeg", opts.outMimeType);
    }

    public void testResizePortrait() throws Exception {
        File resized = resize(IMAGE_PORTRAIT_JPG);
        BitmapFactory.Options opts = ImageUtils.decode(resized);
        assertEquals(800, opts.outHeight);
        assertEquals(600, opts.outWidth);
        assertEquals("image/jpeg", opts.outMimeType);
    }

    @FlakyTest // OutOfMemory
    public void testResizeLargePortrait() throws Exception {
        File resized = resize(IMAGE_LARGE_PORTRAIT_JPG);
        BitmapFactory.Options opts = ImageUtils.decode(resized);
        assertEquals(1296, opts.outHeight);
        assertEquals(972, opts.outWidth);
        assertEquals("image/jpeg", opts.outMimeType);
    }

    public void testGetExifRotation() throws Exception {
        File file = prepareAsset(IMAGE_LARGE_PORTRAIT_JPG);
        assertEquals(0, ImageUtils.getExifRotation(file));
    }

    public void testDetermineResizeOptionsLargePortrait() throws Exception {
        File file = prepareAsset(IMAGE_LARGE_PORTRAIT_JPG);
        BitmapFactory.Options options = ImageUtils.determineResizeOptions(file, 800, 800, false);

        assertEquals(2592, options.outHeight);
        assertEquals(1944, options.outWidth);
        assertEquals(2, options.inSampleSize);
    }

    public void testDetermineResizeOptionsLandscape() throws Exception {
        File file = prepareAsset(IMAGE_LANDSCAPE_JPG);
        BitmapFactory.Options options = ImageUtils.determineResizeOptions(file, 800, 800, false);

        assertEquals(1200, options.outHeight);
        assertEquals(1600, options.outWidth);
        assertEquals(2, options.inSampleSize);
    }

    public void testDetermineResizeOptionsPortrait() throws Exception {
        File file = prepareAsset(IMAGE_PORTRAIT_JPG);
        BitmapFactory.Options options = ImageUtils.determineResizeOptions(file, 800, 800, false);

        assertEquals(1600, options.outHeight);
        assertEquals(1200, options.outWidth);
        assertEquals(2, options.inSampleSize);
    }
}
