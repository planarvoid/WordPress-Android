package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Stream;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RunWith(DefaultTestRunner.class)
public class StreamLoaderTest {
    public static final String TEST_MP3 = "fred.mp3";
    public static final String TEST_URL = "https://api.soundcloud.com/tracks/12345";
    public static final int TEST_CHUNK_SIZE = 1024;
    StreamLoader loader;
    StreamStorage storage;
    StreamItem item;
    File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");
    File testFile;

    private Map<Integer, ByteBuffer> mSampleBuffers = new LinkedHashMap<Integer, ByteBuffer>();
    private List<Integer> mSampleChunkIndexes = new ArrayList<Integer>();

    static class MockStorage extends StreamStorage {
        Map<StreamItem, Map<Integer, ByteBuffer>>  _storage = new HashMap<StreamItem, Map<Integer, ByteBuffer>>();

        public MockStorage(File basedir, int chunkSize) {
            super(null, basedir, chunkSize, false);
        }

        @Override
        public boolean storeData(ByteBuffer data, int chunkIndex, StreamItem item) {
            Map<Integer, ByteBuffer> chunks = _storage.get(item);
            if (chunks == null) {
                chunks = new HashMap<Integer, ByteBuffer>();
                _storage.put(item, chunks);
            }
            chunks.put(chunkIndex, data);
            return true;
        }

        @Override
        public ByteBuffer getChunkData(StreamItem item, int chunkIndex) throws IOException {
            Map<Integer, ByteBuffer> chunks = _storage.get(item);
            if (chunks == null || !chunks.containsKey(chunkIndex)) throw new IOException("item not found");
            return chunks.get(chunkIndex);
        }

        @Override
        public Index getMissingChunksForItem(StreamItem item, Range chunkRange) {
            Map<Integer, ByteBuffer> chunks = _storage.get(item);
            if (chunks == null) {
                return chunkRange.toIndex();
            } else {
                Index i = Index.empty();
                for (int p : chunkRange) {
                    if (!chunks.containsKey(p)) i.set(p, true);
                }
                return i;
            }
        }
    }


    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);
        testFile = new File(getClass().getResource(TEST_MP3).getFile());
        storage = new MockStorage(this.baseDir, TEST_CHUNK_SIZE);
        loader = new StreamLoader(DefaultTestRunner.application, storage);
        loader.setForceOnline(true);
        item = new StreamItem(TEST_URL, testFile.length(), CloudUtils.md5(testFile));
    }

    @Test
    public void shouldGetAChunkFromStorage() throws Exception {
        setupChunkArray();
        storage.storeData(mSampleBuffers.get(0), 0, item);
        ByteBuffer actual = loader.getDataForItem(item.url, Range.from(0, TEST_CHUNK_SIZE)).get();
        ByteBuffer expected = readToByteBuffer(testFile, TEST_CHUNK_SIZE);

        expect(actual).toEqual(expected);
    }

    @Test(expected = RuntimeException.class)
    public void getAChunkFromStorageWithEmptyStorage() throws Exception {
        loader.getDataForItem(item.url, Range.from(0, TEST_CHUNK_SIZE)).get();
    }

    @Test
    public void shouldGetAllBytesFromStorage() throws Exception {
        setupChunkArray();
        for (Integer i : mSampleChunkIndexes) {
            storage.storeData(mSampleBuffers.get(i), i, item);
        }
        expect(loader.getDataForItem(item.url, Range.from(0, testFile.length())).get())
            .toEqual(readToByteBuffer(testFile, (int) testFile.length()));
    }

    @Test
    public void shouldReturnAFutureForMissingChunk() throws Exception {
        setupChunkArray();
        final int missingChunk = 1;
        mSampleChunkIndexes.remove(missingChunk);
        for (Integer i : mSampleChunkIndexes) {
            storage.storeData(mSampleBuffers.get(i), i, item);
        }

        pendingHeadRequests(testFile);
        pendingDataRequest(item, "bytes=1024-2047", mSampleBuffers.get(missingChunk));

        final Range requestedRange = Range.from(TEST_CHUNK_SIZE, 300);
        StreamFuture cb = loader.getDataForItem(item.url, requestedRange);

        expect(loader.getHighPriorityQueue().isEmpty()).toBeTrue();
        expect(loader.getLowPriorityQueue().isEmpty()).toBeTrue();

        expect(cb.isDone()).toBeTrue();
        expect((Buffer)cb.get()).toEqual(mSampleBuffers.get(missingChunk).slice().limit(300));
    }

    @Test
    public void requestingTwoDifferentMissingChunks() throws Exception {
        setupChunkArray();
        pendingHeadRequests(testFile);
        pendingDataRequest(item, "bytes=0-1023", mSampleBuffers.get(0));

        final Range firstRange = Range.from(0, 700);
        StreamFuture cb = loader.getDataForItem(item.url, firstRange);

        expect(cb.isDone()).toBeTrue();

        Buffer actual = cb.get();
        Buffer expected = mSampleBuffers.get(0).slice().limit(700);

        expect(actual).toEqual(expected);

        pendingDataRequest(item, "bytes=1024-2047", mSampleBuffers.get(1));

        final Range secondRange = Range.from(1024, 500);
        cb = loader.getDataForItem(item.url, secondRange);

        expect(cb.isDone()).toBeTrue();
        expect((Buffer)cb.get()).toEqual(mSampleBuffers.get(1).slice().limit(500));
    }

    private int setupChunkArray() throws IOException {
        FileChannel fc = new FileInputStream(getClass().getResource(TEST_MP3).getFile()).getChannel();

        ByteBuffer buffer = ByteBuffer.allocate(TEST_CHUNK_SIZE);
        int chunks = 0;
        while ((fc.read(buffer)) != -1) {
            mSampleBuffers.put(chunks, clone(buffer));
            mSampleChunkIndexes.add(chunks);
            chunks++;
            buffer.clear();
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

    public static ByteBuffer clone(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

    static void pendingDataRequest(StreamItem item, String expectedRange, ByteBuffer bytes) {
        HttpResponse stream = new TestHttpResponse(200, bytes.array());
        FakeHttpLayer.RequestMatcherBuilder b = new FakeHttpLayer.RequestMatcherBuilder();
        if (expectedRange != null) {
            b.header("Range", expectedRange);
        }
        addHttpResponseRule(b, stream);
    }

    static void pendingHeadRequests(File f) {
        long expires = System.currentTimeMillis() + 60*1000;
        // first HEAD request
        addPendingHttpResponse(302, "", headers(
                "Location", "http://ak-media.soundcloud.com/foo_head.mp3?Expires=" + expires)
        );

        // second HEAD request (to S3/akamai)
        addPendingHttpResponse(200, "", headers(
                "Content-Length", String.valueOf(f.length()),
                "ETag",           CloudUtils.md5(f),
                "Last-Modified",  Stream.DATE_FORMAT.format(new Date(f.lastModified())),
                "x-amz-meta-bitrate", "128",
                "x-amz-meta-duration", "18998"
        ));

        // first GET request - soundcloud
        addPendingHttpResponse(302, "", headers(
            "Location", "http://ak-media.soundcloud.com/foo_get.mp3?Expires="+expires+1000)
        );
    }

    public static Header[] headers(String... keyValues) {
        Header[] headers = new Header[keyValues.length/2];
        for (int i=0; i<keyValues.length/2; i++) {
            headers[i] = new BasicHeader(keyValues[i*2], keyValues[i*2+1]);
        }
        return  headers;
    }
}