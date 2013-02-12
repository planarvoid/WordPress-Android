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
public class PlayableActionButtonsControllerTest {

    private PlayableActionButtonsController controller;

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) DefaultTestRunner.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.player_action_bar, null);
        controller = new PlayableActionButtonsController(rootView);
    }

    @Test
    public void shouldShortenCountsOnToggleButtons() {
        expect(controller.labelForCount(999)).toEqual("999");
        expect(controller.labelForCount(1000)).toEqual("1k+");
        expect(controller.labelForCount(1999)).toEqual("1k+");
        expect(controller.labelForCount(2000)).toEqual("2k+");
        expect(controller.labelForCount(9999)).toEqual("9k+");
        expect(controller.labelForCount(10000)).toEqual("9k+"); // 4 chars would make the text spill over again
    }


}
