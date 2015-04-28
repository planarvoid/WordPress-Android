package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class GoBackOnlineDialogPresenterTest {
    private GoBackOnlineDialogPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new GoBackOnlineDialogPresenter();
    }

    @Test
    public void returns0WhenLastUpdateBeforeWasBeforeTheLast30Days() {
        expect(presenter.getRemainingDaysToGoOnline(0)).toEqual(0);
    }

    @Test
    public void returns0WhenLastUpdateBeforeWas30DaysAgo() {
        expect(presenter.getRemainingDaysToGoOnline(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))).toEqual(0);
    }

    @Test
    public void returns2WhenLastUpdateBeforeWas28DaysAgo() {
        expect(presenter.getRemainingDaysToGoOnline(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(28))).toEqual(2);
    }

    @Test
    public void returns30WhenLastUpdateBeforeWasToday() {
        expect(presenter.getRemainingDaysToGoOnline(System.currentTimeMillis())).toEqual(30);
    }
}