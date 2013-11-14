package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.creators.record.SoundRecorderService;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.LocalBinder;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class SoundRecorderServiceTest {
    @Test
    public void shouldUseLocalService() throws Exception {
        expect(new SoundRecorderService().onBind(null) instanceof LocalBinder).toBeTrue();
    }

    @Test
    public void shouldGetCreated() throws Exception {
        SoundRecorderService svc = new SoundRecorderService();
        svc.onCreate();
    }
}
