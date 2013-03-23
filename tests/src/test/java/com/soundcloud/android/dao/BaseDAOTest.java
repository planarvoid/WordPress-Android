package com.soundcloud.android.dao;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

@RunWith(DefaultTestRunner.class)
public class BaseDAOTest extends AbstractDAOTest<BaseDAO<Track>> {

    public BaseDAOTest() {
        super(new TestDAO(mock(ContentResolver.class)));
    }

    //TODO 3/23/13 When gotten rid of ContentProvider, verify interaction against DB instead
    @Test
    public void shouldStoreSingleRecord() {
        ContentResolver resolverMock = getDAO().getContentResolver();
        Track record = new Track();

        when(resolverMock.insert(eq(record.toUri()), any(ContentValues.class))).thenReturn(record.toUri());

        getDAO().create(record);

        verify(resolverMock).insert(eq(record.toUri()), any(ContentValues.class));
    }

    private static class TestDAO extends BaseDAO<Track> {

        protected TestDAO(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        public Content getContent() {
            return Content.TRACKS;
        }
    }
}
