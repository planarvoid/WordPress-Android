package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class SQLiteErrorsTest {
    @Test
    public void shouldConvertExceptionToError() throws Exception {
        expect(SQLiteErrors.convertToErrorMessage("")).toBeNull();
        expect(SQLiteErrors.convertToErrorMessage((String) null)).toBeNull();
        expect(SQLiteErrors.convertToErrorMessage("disk I/O error (code 778)")).toEqual("IOERR_WRITE");
    }
}
