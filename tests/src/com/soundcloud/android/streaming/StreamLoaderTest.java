package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.streaming.BufferUtils.readToByteBuffer;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Stream;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.After;
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

    File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");
    StreamStorage storage = new MockStorage(baseDir, TEST_CHUNK_SIZE);
    StreamLoader loader = new StreamLoader(DefaultTestRunner.application, storage);
    File testFile = new File(getClass().getResource(TEST_MP3).getFile());
    StreamItem item = new StreamItem(TEST_URL, testFile.length(), CloudUtils.md5(testFile));

    private Map<Integer, ByteBuffer> sampleBuffers = new LinkedHashMap<Integer, ByteBuffer>();
    private List<Integer> sampleChunkIndexes = new ArrayList<Integer>();

    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);
        loader.setForceOnline(true);
    }

    @After
    public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void shouldGetAChunkFromStorage() throws Exception {
        setupChunkArray();
        storage.storeMetadata(item);
        storage.storeData(item.url, sampleBuffers.get(0), 0);
        ByteBuffer actual = loader.getDataForUrl(item.url, Range.from(0, TEST_CHUNK_SIZE)).get();
        ByteBuffer expected = readToByteBuffer(testFile, TEST_CHUNK_SIZE);

        expect(actual).toEqual(expected);
    }

    @Test(expected = RuntimeException.class)
    public void getAChunkFromStorageWithEmptyStorage() throws Exception {
        loader.getDataForUrl(item.url, Range.from(0, TEST_CHUNK_SIZE)).get();
    }

    @Test
    public void shouldGetAllBytesFromStorage() throws Exception {
        setupChunkArray();
        storage.storeMetadata(item);
        for (int i : sampleChunkIndexes) storage.storeData(item.url, sampleBuffers.get(i), i);
        expect(loader.getDataForUrl(item.url, Range.from(0, testFile.length())).get())
                .toEqual(readToByteBuffer(testFile, (int) testFile.length()));
    }

    @Test
    public void shouldReturnAFutureForMissingChunk() throws Exception {
        setupChunkArray();
        final int missingChunk = 1;
        sampleChunkIndexes.remove(missingChunk);
        storage.storeMetadata(item);
        for (Integer i : sampleChunkIndexes) storage.storeData(item.url, sampleBuffers.get(i), i);

        pendingHeadRequests(testFile);
        pendingDataRequest(item, "bytes=1024-2047", sampleBuffers.get(missingChunk));
        // playcount
        pendingDataRequest(item, "bytes=0-1", sampleBuffers.get(0).slice().limit(1));

        final Range requestedRange = Range.from(TEST_CHUNK_SIZE, 300);
        StreamFuture cb = loader.getDataForUrl(item.url, requestedRange);

        expect(loader.getHighPriorityQueue().isEmpty()).toBeTrue();
        expect(loader.getLowPriorityQueue().isEmpty()).toBeTrue();

        expect(cb.isDone()).toBeTrue();
        expect((Buffer) cb.get()).toEqual(sampleBuffers.get(missingChunk).slice().limit(300));
    }

    @Test
    public void requestingTwoDifferentMissingChunks() throws Exception {
        setupChunkArray();
        pendingHeadRequests(testFile);
        pendingDataRequest(item, "bytes=0-1023", sampleBuffers.get(0));

        final Range firstRange = Range.from(0, 700);
        StreamFuture cb = loader.getDataForUrl(item.url, firstRange);

        expect(cb.isDone()).toBeTrue();

        Buffer actual = cb.get();
        Buffer expected = sampleBuffers.get(0).slice().limit(700);

        expect(actual).toEqual(expected);

        pendingDataRequest(item, "bytes=1024-2047", sampleBuffers.get(1));

        final Range secondRange = Range.from(1024, 500);
        cb = loader.getDataForUrl(item.url, secondRange);

        expect(cb.isDone()).toBeTrue();
        expect((Buffer) cb.get()).toEqual(sampleBuffers.get(1).slice().limit(500));
    }

    @Test
    public void requestingTwoOverlappingChunks() throws Exception {
        setupChunkArray();
        pendingHeadRequests(testFile);

        pendingDataRequest(item, "bytes=0-1023", sampleBuffers.get(0));
        pendingDataRequest(item, "bytes=1024-2047", sampleBuffers.get(1));

        // needs a GET of 2 chunks (500-1500)
        StreamFuture cb = loader.getDataForUrl(item.url, Range.from(500, 1000));

        expect(cb.isDone()).toBeTrue();

        ByteBuffer expected = readToByteBuffer(testFile);
        expected.position(500).limit(1500);

        //XXX
        //expect(cb.get()).toEqual(expected.slice());
    }

    private int setupChunkArray() throws IOException {
        FileChannel fc = new FileInputStream(testFile).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(TEST_CHUNK_SIZE);
        int chunks = 0;
        while ((fc.read(buffer)) != -1) {
            sampleBuffers.put(chunks, BufferUtils.clone(buffer));
            sampleChunkIndexes.add(chunks);
            chunks++;
            buffer.clear();
        }
        return chunks;
    }

    static void pendingDataRequest(StreamItem item, String expectedRange, Buffer bytes) {
        HttpResponse stream = new TestHttpResponse(200, (byte[]) bytes.array());
        FakeHttpLayer.RequestMatcherBuilder b = new FakeHttpLayer.RequestMatcherBuilder();
        if (expectedRange != null) {
            b.header("Range", expectedRange);
        }
        addHttpResponseRule(b, stream);
    }

    static void pendingHeadRequests(File f) {
        long expires = (System.currentTimeMillis() / 1000L) + 60L;
        // first HEAD request
        addPendingHttpResponse(302, "", headers(
                "Location", "http://ak-media.soundcloud.com/foo_head.mp3?Expires=" + expires)
        );

        // second HEAD request (to S3/akamai)
        addPendingHttpResponse(200, "", headers(
                "Content-Length", String.valueOf(f.length()),
                "ETag", CloudUtils.md5(f),
                "Last-Modified", Stream.DATE_FORMAT.format(new Date(f.lastModified())),
                "x-amz-meta-bitrate", "128",
                "x-amz-meta-duration", "18998"
        ));

        // first GET request - soundcloud
        addPendingHttpResponse(302, "", headers(
                "Location", "http://ak-media.soundcloud.com/foo_get.mp3?Expires=" + expires + 1000)
        );
    }

    public static Header[] headers(String... keyValues) {
        Header[] headers = new Header[keyValues.length / 2];
        for (int i = 0; i < keyValues.length / 2; i++) {
            headers[i] = new BasicHeader(keyValues[i * 2], keyValues[i * 2 + 1]);
        }
        return headers;
    }

    static class MockStorage extends StreamStorage {
        Map<String, Map<Integer, ByteBuffer>> _storage = new HashMap<String, Map<Integer, ByteBuffer>>();
        Map<String, StreamItem> _metadata = new HashMap<String, StreamItem>();

        public MockStorage(File basedir, int chunkSize) {
            super(null, basedir, chunkSize, false);
        }

        @Override
        public StreamItem getMetadata(String url) {
            return _metadata.get(url);
        }

        @Override
        public boolean storeMetadata(StreamItem item) {
            return _metadata.put(item.url, item) != null;
        }

        @Override
        public boolean storeData(String url, ByteBuffer data, int chunkIndex) {
            Map<Integer, ByteBuffer> chunks = _storage.get(url);
            if (chunks == null) {
                chunks = new HashMap<Integer, ByteBuffer>();
                _storage.put(url, chunks);
            }
            chunks.put(chunkIndex, data);
            return true;
        }

        @Override
        public ByteBuffer getChunkData(String url, int chunkIndex) throws IOException {
            Map<Integer, ByteBuffer> chunks = _storage.get(url);
            if (chunks == null || !chunks.containsKey(chunkIndex)) throw new IOException("item not found");
            return chunks.get(chunkIndex);
        }

        @Override
        public Index getMissingChunksForItem(String url, Range chunkRange) {
            Map<Integer, ByteBuffer> chunks = _storage.get(url);
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
}