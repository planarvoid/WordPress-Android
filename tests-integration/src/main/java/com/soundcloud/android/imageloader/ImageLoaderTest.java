package com.soundcloud.android.imageloader;

import static com.soundcloud.android.imageloader.ImageLoader.Options;

import com.soundcloud.android.utils.IOUtils;
import org.json.JSONArray;

import android.graphics.Bitmap;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageLoaderTest extends InstrumentationTestCase {
    static final String TAG = "ImageLoaderTest";
    static final String LARGE_AVATAR = "https://i1.sndcdn.com/avatars-000006111783-xqaxy3-large.jpg?d95e793";
    static final String RESOLVER = "https://api.soundcloud.com/resolve/image?url=soundcloud:users:1&client_id=40ccfee680a844780a41fbe23ea89934";

    ImageLoader loader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        loader = new ImageLoader(new DownloadBitmapHandler(false), null,
                ImageLoader.DEFAULT_CACHE_SIZE,
                ImageLoader.DEFAULT_TASK_LIMIT);
    }

    public void testGetBitmap() throws Throwable {
        assertNotNull(getBitmap(LARGE_AVATAR));
    }

    public void testDownloadBitmapWithRedirect() throws Throwable {
        assertNotNull(getBitmap(RESOLVER));
    }

    public void testErrorCallback() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] throwable = new Throwable[1];
        final String url = "http://example.com/not.existing.jpg";
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = loader.getBitmap(url,
                        new ImageLoader.BitmapLoadCallback() {
                            @Override
                            public void onImageLoaded(Bitmap bitmap, String uri) {
                                latch.countDown();
                            }
                            @Override
                            public void onImageError(String uri, Throwable error) {
                                latch.countDown();
                                throwable[0] = error;
                            }
                        }, getInstrumentation().getContext());
                assertNull(bmp);
            }
        });
        assertTrue("timeout", latch.await(10, TimeUnit.SECONDS));
        assertNull(loader.getBitmap(url, Options.dontLoadRemote()));
        assertTrue(throwable[0] instanceof IOException);
    }

    public void testStressLoader() throws Throwable {
        List<String> urls = getUrls("json/waveform_urls.json");
        final CountDownLatch latch = new CountDownLatch(urls.size());
        final AtomicInteger loaded = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        for (final String u :urls) {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loader.getBitmap(u, new ImageLoader.BitmapLoadCallback() {
                        @Override
                        public void onImageError(String uri, Throwable error) {
                            latch.countDown();
                            errors.incrementAndGet();
                        }
                        @Override
                        public void onImageLoaded(Bitmap bitmap, String uri) {
                            latch.countDown();
                            loaded.incrementAndGet();
                        }
                    }, getInstrumentation().getContext());
                }
            });
        }
        assertTrue("timeout waiting for threads", latch.await(45, TimeUnit.SECONDS));
        long duration = System.currentTimeMillis() - start;
        Log.d(TAG, "fetched "+urls.size()+" images in "+duration + "ms");
    }

    public void testLoadingSameUrlMultipleTimes() throws Throwable {
        List<String> urls = new ArrayList<String>();
        int N = 100;
        for (int i=0; i<N; i++) {
            urls.add(LARGE_AVATAR);
        }
        final CountDownLatch latch = new CountDownLatch(urls.size());
        final AtomicInteger loaded = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        final Set<Bitmap> bitmaps  = new HashSet<Bitmap>();
        long start = System.currentTimeMillis();
        for (final String u :urls) {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loader.getBitmap(u, new ImageLoader.BitmapLoadCallback() {
                        @Override
                        public void onImageError(String uri, Throwable error) {
                            latch.countDown();
                            errors.incrementAndGet();
                        }
                        @Override
                        public void onImageLoaded(Bitmap bitmap, String uri) {
                            bitmaps.add(bitmap);
                            latch.countDown();
                            loaded.incrementAndGet();
                        }
                    }, getInstrumentation().getContext());
                }
            });
        }
        assertTrue("timeout waiting for threads", latch.await(5, TimeUnit.SECONDS));
        long duration = System.currentTimeMillis() - start;
        Log.d(TAG, "fetched "+urls.size()+" images in "+duration + "ms");
        assertEquals(N, loaded.get());
        assertEquals(1, bitmaps.size());
    }

    public void testPrefetch() throws Throwable {
        final boolean[] prefetchCalled = new boolean[1];
        ImageLoader prefetcher = new ImageLoader(null, new PrefetchHandler() {
            @Override
            public Object getContent(URLConnection connection) throws IOException {
                prefetchCalled[0] = true;
                return null;
            }
        }, 1, 1);
        prefetcher.prefetch(LARGE_AVATAR);
        for (int i=5; i>0; i--) {
            Thread.sleep(1000);
            if (prefetchCalled[0]) break;
        }
        assertTrue("did not prefetch avatar", prefetchCalled[0]);
    }

    private Bitmap getBitmap(final String url) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = loader.getBitmap(url,
                        new ImageLoader.BitmapLoadCallback() {
                            @Override
                            public void onImageLoaded(Bitmap bitmap, String uri) {
                                latch.countDown();
                            }

                            @Override
                            public void onImageError(String uri, Throwable error) {
                                latch.countDown();
                            }
                        }, getInstrumentation().getContext());
                assertNull(bmp);
            }
        });
        assertTrue("timeout", latch.await(10, TimeUnit.SECONDS));
        return loader.getBitmap(url, Options.dontLoadRemote());
    }


    private List<String> getUrls(String file) throws Exception {
        InputStream is = getInstrumentation().getContext().getAssets().open(file);
        assertNotNull(is);
        List<String> urls = new ArrayList<String>();
        JSONArray array = new JSONArray(IOUtils.readInputStream(is));
        for (int i=0; i<array.length(); i++) {
            urls.add(array.getString(i));
        }
        return urls;
    }
}
