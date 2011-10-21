package com.soundcloud.android.streaming;


import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

@RunWith(DefaultTestRunner.class)
public class ScStreamStorageTest {
    ScStreamStorage storage;
    ScStreamItem item;
    File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");

    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);
        storage = new ScStreamStorage(DefaultTestRunner.application, baseDir, 1028);
        item = new ScStreamItem(DefaultTestRunner.application, "fred.mp3");
    }

    private long mSampleContentLength;
    private LinkedHashMap<Integer, byte[]> mSampleBuffers;
    private ArrayList<Integer> mSampleChunkIndexes;

    private long setupChunkArray() throws IOException {

        InputStream inputStream = getClass().getResourceAsStream("fred.mp3");
        mSampleContentLength = new File(getClass().getResource("fred.mp3").getFile()).length();

        long chunks = 0;
        mSampleBuffers = new LinkedHashMap<Integer, byte[]>();
        mSampleChunkIndexes = new ArrayList<Integer>();
        do {
            byte[] buffer = new byte[storage.chunkSize];
            if (inputStream.read(buffer) == -1) {
                break;
            } else {
                mSampleBuffers.put((int) chunks, buffer);
                mSampleChunkIndexes.add((int) chunks);
                chunks++;
            }
        } while (true);
        return chunks;
    }

    @Test
    public void testSetDataShouldNotStoreIfContentLengthZero() throws IOException {
        assertThat(item.getContentLength(), is(0l));
        assertThat(storage.setData(new byte[]{1, 2, 3}, 0, item), is(false));
    }

    @Test
    public void testSetDataShouldNotStoreIfDataNull() throws IOException {
        item.setContentLength(10);
        assertThat(storage.setData(null, 0, item), is(false));
    }

    @Test
    public void shouldWriteAndReadIndex() throws Exception {
        item.setContentLength(100);
        storage.writeIndex(item, Arrays.asList(1, 2, 3, 4));

        ScStreamStorage other = new ScStreamStorage(DefaultTestRunner.application, baseDir);

        assertThat(other.isMetaDataLoaded(item), is(false));

        other.readIndex(item);
        assertThat(other.getIncompleteIndexes().get(item.getURLHash()), hasItems(1, 2, 3, 4));
        assertThat(other.getIncompleteContentLengths().get(item.getURLHash()), equalTo(100L));

        assertThat(other.isMetaDataLoaded(item), is(true));
    }


    @Test
    public void shouldSetData() throws Exception {
        item.setContentLength(storage.chunkSize * 2);

        assertThat(storage.setData(new byte[]{1, 2, 3}, 0, item), is(true));
        byte[] data = storage.getChunkData(item, 0);
        assertThat(data, notNullValue());
        assertThat(data.length, is(storage.chunkSize));
        assertThat(data[0], is((byte) 1));
        assertThat(data[1], is((byte) 2));
        assertThat(data[2], is((byte) 3));
    }

    @Test
    public void shouldSetContentLength() throws Exception {
        storage.setContentLength(item.getURLHash(), 2 * storage.chunkSize);
        assertThat(storage.getIncompleteContentLengths().get(item.getURLHash()), is(2L * storage.chunkSize));
        assertThat(storage.numberOfChunksForKey(item), is(2L));
    }

    @Test
    public void shouldCalculateFileMetrics() throws Exception {
        storage.calculateFileMetrics();
    }

    @Test
    public void shouldTestIncompleteSequentialWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);

        final int writing = mSampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.setData(mSampleBuffers.get(i), i, item);
        }

        for (int i = 0; i < writing; i++) {
            assertNotNull(storage.getChunkData(item, i));
        }

        assertNull(storage.getChunkData(item, writing));
    }

    @Test
    public void shouldTestIncompleteRandomWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        final int writing = mSampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.setData(mSampleBuffers.get(mSampleChunkIndexes.get(i)), mSampleChunkIndexes.get(i), item);
        }

        for (int i = 0; i < writing; i++) {
            assertNotNull(storage.getChunkData(item, mSampleChunkIndexes.get(i)));
        }

        assertNull(storage.getChunkData(item, mSampleChunkIndexes.get(writing)));
    }

    @Test
    public void shouldTestCompleteSequentialWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);

        for (int i = 0; i < mSampleChunkIndexes.size(); i++) {
            storage.setData(mSampleBuffers.get(i), i, item);
        }

        for (int i = 0; i < mSampleChunkIndexes.size(); i++) {
            assertNotNull(storage.getChunkData(item, i));
        }

        assertNull(storage.getChunkData(item, mSampleChunkIndexes.size()));
    }

    @Test
    public void shouldTestCompleteRandomWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        for (Integer mSampleChunkIndexe1 : mSampleChunkIndexes) {
            storage.setData(mSampleBuffers.get(mSampleChunkIndexe1), mSampleChunkIndexe1, item);
        }

        for (Integer mSampleChunkIndexe : mSampleChunkIndexes) {
            assertNotNull(storage.getChunkData(item, mSampleChunkIndexe));
        }
    }

    @Test
    public void shouldTestCompleteFileConstruction() throws Exception {
        long chunks = setupChunkArray();

        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        for (Integer aChunkArray : mSampleChunkIndexes) {
            storage.setData(mSampleBuffers.get(aChunkArray), aChunkArray, item);
        }

        assertThat(storage.numberOfChunksForKey(item), is(chunks));

        File assembled = storage.completeFileForKey(item.getURLHash());
        assertThat(mSampleContentLength, is(assembled.length()));

        String original = CloudUtils.md5(getClass().getResourceAsStream("fred.mp3"));
        assertThat(CloudUtils.md5(new FileInputStream(assembled)), equalTo(original));
    }
}
