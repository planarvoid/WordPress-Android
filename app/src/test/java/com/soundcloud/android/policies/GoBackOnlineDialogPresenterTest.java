package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.policies.GoBackOnlineDialogPresenter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;

import java.util.concurrent.TimeUnit;

public class GoBackOnlineDialogPresenterTest {

    @Mock private Resources resources;

    private GoBackOnlineDialogPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new GoBackOnlineDialogPresenter(resources);
    }

    @Test
    public void returns0WhenLastUpdateBeforeWasBeforeTheLast30Days() {
        assertThat(presenter.getRemainingDaysToGoOnline(0)).isEqualTo(0);
    }

    @Test
    public void returns0WhenLastUpdateBeforeWas30DaysAgo() {
        long lastTimeUpdate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        assertThat(presenter.getRemainingDaysToGoOnline(lastTimeUpdate)).isEqualTo(0);
    }

    @Test
    public void returns2WhenLastUpdateBeforeWas28DaysAgo() {
        long lastTimeUpdate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(28);
        assertThat(presenter.getRemainingDaysToGoOnline(lastTimeUpdate)).isEqualTo(2);
    }

    @Test
    public void returns30WhenLastUpdateBeforeWasToday() {
        assertThat(presenter.getRemainingDaysToGoOnline(System.currentTimeMillis())).isEqualTo(30);
    }
}