package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import android.support.v4.app.Fragment;

public class NavigationModelTest {

    public NavigationModel navigationModel;

    @Before
    public void setUp() throws Exception {
        navigationModel = buildTestNavigationModel();
    }

    @Test
    public void getPositionFindsTargetBasedOnScreen() {
        assertThat(navigationModel.getPosition(Screen.COLLECTIONS)).isEqualTo(1);
    }

    @Test
    public void getPositionNotFoundIfScreenIsNotInModel() {
        assertThat(navigationModel.getPosition(Screen.WIDGET)).isEqualTo(NavigationModel.NOT_FOUND);
    }

    private NavigationModel buildTestNavigationModel() {
        return new NavigationModel(new NavigationModel.Target() {
            @Override
            public int getName() {
                return 0;
            }

            @Override
            public int getIcon() {
                return 0;
            }

            @Override
            public Fragment createFragment() {
                return null;
            }

            @Override
            public Screen getScreen() {
                return Screen.STREAM;
            }
        }, new NavigationModel.Target() {
            @Override
            public int getName() {
                return 0;
            }

            @Override
            public int getIcon() {
                return 0;
            }

            @Override
            public Fragment createFragment() {
                return null;
            }

            @Override
            public Screen getScreen() {
                return Screen.COLLECTIONS;
            }
        });
    }

}