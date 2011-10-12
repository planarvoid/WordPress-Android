package com.soundcloud.android.streaming;


import android.util.Log;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import com.sun.corba.se.spi.orbutil.fsm.Input;
import com.sun.servicetag.SystemEnvironment;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import android.app.Activity;
import org.junit.matchers.JUnitMatchers;
import org.junit.runner.RunWith;

import javax.naming.Context;
import java.io.*;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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
        storage = new ScStreamStorage(DefaultTestRunner.application, baseDir);
        item = new ScStreamItem(DefaultTestRunner.application, "captainstorm.mp3");
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
        storage.writeIndex(item, Arrays.asList(1,2,3,4));

        ScStreamStorage other = new ScStreamStorage(DefaultTestRunner.application, baseDir );

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
        assertThat(data[1], is((byte)2));
        assertThat(data[2], is((byte)3));
    }

    @Test
    public void shouldSetContentLength() throws Exception {
        storage.setContentLength(item.getURLHash(), 2*storage.chunkSize);
        assertThat(storage.getIncompleteContentLengths().get(item.getURLHash()), is(2L*storage.chunkSize));
        assertThat(storage.numberOfChunksForKey(item), is(2L));
    }

    @Test
    public void shouldCalculateFileMetrics() throws Exception {
        storage.calculateFileMetrics();
    }

     @Test
     public void shouldTest() throws Exception {

         InputStream inputStream = getClass().getResourceAsStream("captainstorm.mp3");

         long contentLength = new File(getClass().getResource("captainstorm.mp3").getFile()).length();

         long chunks = 0;
         LinkedHashMap<Integer, byte[]> buffers = new LinkedHashMap<Integer, byte[]>();
         ArrayList<Integer> chunkArray = new ArrayList<Integer>();
         do {
             byte[] buffer = new byte[storage.chunkSize];
             if (inputStream.read(buffer) == -1) {
                 break;
             } else {
                 buffers.put((int) chunks, buffer);
                 chunkArray.add((int) chunks);
                 chunks++;
             }
         } while (true);

         item.setContentLength(contentLength);
         Collections.shuffle(chunkArray);

         for (Integer aChunkArray : chunkArray) {
             storage.setData(buffers.get(aChunkArray), aChunkArray, item);
         }

         assertThat(storage.numberOfChunksForKey(item), is(chunks));

         File assembled = storage.completeFileForKey(item.getURLHash());

         assertThat(contentLength, is(assembled.length()));

         String original = CloudUtils.md5(getClass().getResourceAsStream("captainstorm.mp3"));
         assertThat(CloudUtils.md5(new FileInputStream(assembled)), equalTo(original));

         assertThat(storage.getChunkData(item,2), equalTo(storage.completeDataForChunk(item,2)));


//         assertThat(storage.getChunkData(item, chunkArray.get(0)), notNullValue());


         /*
         File dataOut = new File(System.getProperty("java.tmp.dir"), "tmp/streaming/captainstorm_data");
         File indexOut = new File(System.getProperty("java.tmp.dir"), "tmp/streaming/captainstorm_index");
         FileOutputStream dataFos = new FileOutputStream(dataOut, true);
         DataOutputStream indexDos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indexOut)));



         for (int chunkIndex : chunkArray) {
             fos.write();
         }


         System.out.print("chunks " + buffers.size());
         System.out.print("length " + contentLength);

         /*

         out.mkdirs();



         assertTrue(original.exists());
         /*
         try {

                         bin = new BufferedInputStream(new FileInputStream(chunkFile));

                         completeFile.createNewFile();
                         byte[] buffer = new byte[chunkSize];

                         for (int chunkNumber = 0; chunkNumber < mIndexes.size(); chunkNumber++) {
                             bin.read(buffer, mIndexes.indexOf(chunkNumber), chunkSize);
                             if (chunkNumber == mIndexes.size() - 1) {
                                 fos.write(buffer, 0, (int) (mContentLength % chunkSize));
                             } else {
                                 fos.write(buffer);
                             }

                         }

         */
     }
}
