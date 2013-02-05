package com.soundcloud.android.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.net.ContentHandler;
import java.net.URLConnection;

public abstract class BitmapContentHandler extends ContentHandler {
    public abstract Bitmap getContent(URLConnection connection, BitmapFactory.Options options) throws IOException;
}
