package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiObjectContentRequest;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

public class UpdateAgeCommandTest extends AndroidUnitTest{

    @Test
    public void shouldSendUpdateRequestToApiMobile() throws Exception {
        ApiClient apiClient = mock(ApiClient.class, RETURNS_SMART_NULLS);
        ApiResponse response = TestApiResponses.ok();

        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(response);

        BirthdayInfo info = BirthdayInfo.buildFrom(40);
        UpdateAgeCommand command = new UpdateAgeCommand(apiClient).with(info);
        assertThat(command.call()).isTrue();

        ArgumentCaptor<ApiRequest> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClient).fetchResponse(captor.capture());

        Map<String, Integer> content = (Map) ((ApiObjectContentRequest) captor.getValue()).getContent();
        assertThat(content.get("month")).isEqualTo(info.getMonth());
        assertThat(content.get("year")).isEqualTo(info.getYear());
    }
}
