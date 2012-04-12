package com.soundcloud.android.service.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.LocalBinder;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(DefaultTestRunner.class)
public class CloudCreateServiceTest {
    @Test
    public void shouldUseLocalService() throws Exception {
        expect(new CloudCreateService().onBind(null) instanceof LocalBinder).toBeTrue();
    }

    @Test
    public void shouldGetCreated() throws Exception {
        CloudCreateService svc = new CloudCreateService();
        svc.onCreate();
    }

    @Test
    public void shouldGetUserIdFromFile() throws Exception {
        expect(CloudCreateService.getUserIdFromFile(new File("_/foo"))).toEqual(-1l);
        expect(CloudCreateService.getUserIdFromFile(new File("_/foo/12232_1234"))).toEqual(1234l);
        expect(CloudCreateService.getUserIdFromFile(new File("/foo/12232_1234.ogg"))).toEqual(1234l);
        expect(CloudCreateService.getUserIdFromFile(new File("foo/12232_1234.ogg_"))).toEqual(1234l);
        expect(CloudCreateService.getUserIdFromFile(new File("_foo/12232_1234.ogg_"))).toEqual(1234l);
    }
}
