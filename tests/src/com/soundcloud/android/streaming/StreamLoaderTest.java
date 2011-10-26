package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RunWith(DefaultTestRunner.class)
public class StreamLoaderTest {
    public static final String TEST_MP3 = "fred.mp3";
    public static final String TEST_URL = "https://api.soundcloud.com/tracks/12345";
    public static final int CHUNK_SIZE = 1024;
    StreamLoader loader;
    StreamStorage storage;
    StreamItem item;
    File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");
    File testFile;

    private Map<Integer, byte[]> mSampleBuffers = new LinkedHashMap<Integer, byte[]>();
    private List<Integer> mSampleChunkIndexes = new ArrayList<Integer>();

    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);
        testFile = new File(getClass().getResource(TEST_MP3).getFile());
        storage = new StreamStorage(DefaultTestRunner.application, baseDir, CHUNK_SIZE);
        loader = new StreamLoader(DefaultTestRunner.application, storage);
        loader.setForceOnline(true);
        item = new StreamItem(TEST_URL, testFile.length());
    }

    @Test
    public void shouldGetAChunkFromStorage() throws Exception {
        setupChunkArray();
        loader.storeData(mSampleBuffers.get(0), 0, item);
        ByteBuffer actual = loader.getDataForItem(item, Range.from(0, CHUNK_SIZE)).get();
        ByteBuffer expected = readToByteBuffer(testFile, CHUNK_SIZE);

        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldGetAllBytesFromStorage() throws Exception {
        setupChunkArray();
        Collections.shuffle(mSampleChunkIndexes);

        for (Integer i : mSampleChunkIndexes) {
            loader.storeData(mSampleBuffers.get(i), i, item);
        }

        expect(loader.getDataForItem(item, Range.from(0, testFile.length())).get())
            .toEqual(readToByteBuffer(testFile, (int) testFile.length()));
    }

    @Test
    public void shouldReturnAFutureForMissingChunk() throws Exception {
        setupChunkArray();
        mSampleChunkIndexes.remove(0);

        Collections.shuffle(mSampleChunkIndexes);

        for (Integer i : mSampleChunkIndexes) {
            loader.storeData(mSampleBuffers.get(i), i, item);
        }

        pendingResponses(mSampleBuffers.get(0));

        StreamFuture cb = loader.getDataForItem(item, Range.from(0, 300));

        expect(loader.getHighPriorityQueue().isEmpty()).toBeTrue();
        expect(loader.getLowPriorityQueue().isEmpty()).toBeTrue();

        expect(cb.isDone()).toBeTrue();
    }

    private int setupChunkArray() throws IOException {
        InputStream is = getClass().getResourceAsStream(TEST_MP3);
        assert is instanceof BufferedInputStream;
        byte[] buffer = new byte[CHUNK_SIZE];
        int chunks = 0;
        while (is.read(buffer, 0, buffer.length) != -1) {
            byte[] copy = new byte[buffer.length];
            System.arraycopy(buffer, 0, copy, 0, buffer.length);
            mSampleBuffers.put(chunks, copy);
            mSampleChunkIndexes.add(chunks);
            chunks++;
        }
        return chunks;
    }

    static ByteBuffer readToByteBuffer(File f, int toRead) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(toRead);
        FileChannel fc = new FileInputStream(f).getChannel();
        fc.read(b);
        b.flip();
        return b;
    }

    static void pendingResponses(byte[] bytes) {
        long expires = System.currentTimeMillis() + 60*1000;
        // first HEAD request
        addPendingHttpResponse(302, "", headers(
            "Location", "http://ak-media.soundcloud.com/foo_head.mp3?Expires="+expires)
        );

        // second HEAD request (to S3/akamai)
        addPendingHttpResponse(200, "", headers(
            "Content-Length",      "12345",
            "ETag",                "anEtag",
            "Last-Modified",       "Tue, 25 Oct 2011 10:01:23 GMT",
            "x-amz-meta-bitrate",  "128",
            "x-amz-meta-duration", "18998"
        ));

        // first GET request - soundcloud
        addPendingHttpResponse(302, "", headers(
            "Location", "http://ak-media.soundcloud.com/foo_get.mp3?Expires="+expires+1000)
        );

        // second GET request (actual data)
        addPendingHttpResponse(new TestHttpResponse(200, bytes));
    }

    public static Header[] headers(String... keyValues) {
        Header[] headers = new Header[keyValues.length/2];
        for (int i=0; i<keyValues.length/2; i++) {
            headers[i] = new BasicHeader(keyValues[i*2], keyValues[i*2+1]);
        }
        return  headers;
    }
}