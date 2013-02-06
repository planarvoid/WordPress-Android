package com.soundcloud.android.view;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

@RunWith(DefaultTestRunner.class)
public class PlayerActionButtonsControllerTest {

    private PlayableActionButtonsController controller;

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) DefaultTestRunner.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.player_action_bar, null);
        controller = new PlayableActionButtonsController(rootView);
    }

    @Test
    public void shouldShortenCountsOnToggleButtons() {

        String smallNumberLabel = controller.labelForCount(999);
        expect(smallNumberLabel).toEqual("999");

        String largeNumberLabel = controller.labelForCount(1000);
        expect(largeNumberLabel).toEqual("1k+");

    }


}
