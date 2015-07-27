package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Pattern;

@RunWith(SoundCloudTestRunner.class)
public class NetworkConnectionHelperTest {

    private NetworkConnectionHelper networkConnectionHelper;

    @Mock
    private TelephonyManager telephonyManager;
    @Mock
    private ConnectivityManager connectivityManager;
    @Mock
    private NetworkInfo networkInfo;

    @Before
    public void setUp() throws Exception {
        networkConnectionHelper = new NetworkConnectionHelper(connectivityManager, telephonyManager);
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    }

    @Test
    public void returnsWifiWhenNetworkInfoTypeIsWifi() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.WIFI);
    }

    @Test
    public void returnsUnknownConnectionTypeWhenActiveNetworkInfoIsNull() throws Exception {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(null);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.UNKNOWN);
    }

    @Test
    public void returnsWifiWhenNetworkInfoTypeIsWiMax() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIMAX);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.WIFI);
    }

    @Test
    public void returnsTwoGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsGprs() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_GPRS);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.TWO_G);
    }

    @Test
    public void returnsTwoGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsEdge() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EDGE);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.TWO_G);
    }

    @Test
    public void returnsTwoGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsCdma() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_CDMA);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.TWO_G);
    }

    @Test
    public void returnsTwoGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIs1xRTT() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_1xRTT);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.TWO_G);
    }

    @Test
    public void returnsTwoGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsIden() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_IDEN);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.TWO_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsEvdo0() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EVDO_0);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsEvdoA() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EVDO_A);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsEvdoB() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EVDO_B);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsHsdpa() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSDPA);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsHsupa() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSUPA);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsHspa() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSPA);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsHspap() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_HSPAP);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsThreeGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsEhrpd() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_EHRPD);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.THREE_G);
    }

    @Test
    public void returnsFourGWhenNetworkInfoTypeIsMobileAndTelephonyNetworkTypeIsLTE() throws Exception {
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(telephonyManager.getNetworkType()).thenReturn(TelephonyManager.NETWORK_TYPE_LTE);
        expect(networkConnectionHelper.getCurrentConnectionType()).toBe(ConnectionType.FOUR_G);
    }

    @Test
    public void shouldReturnFalseWhenCheckingConnectivityAndNetworkInfoIsNull() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(null);
        expect(networkConnectionHelper.isNetworkConnected()).toBeFalse();
    }

    @Test
    public void shouldReturnFalseWhenCheckingConnectivityAndNetworkIsNotConnectedOrConnecting() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(false);
        expect(networkConnectionHelper.isNetworkConnected()).toBeFalse();
    }

    @Test
    public void shouldReturnTrueWhenCheckingConnectivityAndNetworkIsConnectedOrConnecting() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        expect(networkConnectionHelper.isNetworkConnected()).toBeTrue();
    }

    @Test
    public void shouldReturnFalseWhenCheckingWifiConnectivityAndNetworkInfoIsNull() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(null);
        expect(networkConnectionHelper.isWifiConnected()).toBeFalse();
    }

    @Test
    public void shouldReturnFalseWhenCheckingWifiConnectivityAndItIsNotConnected() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.isConnected()).thenReturn(false);
        expect(networkConnectionHelper.isWifiConnected()).toBeFalse();
    }

    @Test
    public void shouldReturnFalseWhenCheckingWifiConnectivityAndItIsConnectedButNotAWifiConnectionType() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(networkInfo.isConnected()).thenReturn(true);
        expect(networkConnectionHelper.isWifiConnected()).toBeFalse();
    }

    @Test
    public void shouldReturnTrueWhenCheckingWifiConnectivityAndItIsConnectedAndIsAWifiConnectionType() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIFI);
        when(networkInfo.isConnected()).thenReturn(true);
        expect(networkConnectionHelper.isWifiConnected()).toBeTrue();
    }

    @Test
    public void shouldReturnTrueWhenCheckingWifiConnectivityAndItIsConnectedAndIsAWimaxConnectionType() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_WIMAX);
        when(networkInfo.isConnected()).thenReturn(true);
        expect(networkConnectionHelper.isWifiConnected()).toBeTrue();
    }

    @Test
    public void shouldSupportAllNetworkConnectionTypesFromConnectivityManager() throws IllegalAccessException {
        Pattern networkTypeFieldPattern = Pattern.compile("^NETWORK_TYPE_\\w*");
        Class<TelephonyManager> telephonyManagerClass = TelephonyManager.class;
        Field[] fields = telephonyManagerClass.getDeclaredFields();
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);

        List<String> unrecognisedNetworkTypes = newArrayList();
        for (Field field : fields) {
            if (networkTypeFieldPattern.matcher(field.getName()).matches()) {

                int networkTypeValue = field.getInt(null);

                if(TelephonyManager.NETWORK_TYPE_UNKNOWN == networkTypeValue){
                    continue;
                }

                when(telephonyManager.getNetworkType()).thenReturn(networkTypeValue);
                ConnectionType connectionType = networkConnectionHelper.getCurrentConnectionType();

                if (ConnectionType.UNKNOWN.equals(connectionType)) {
                    unrecognisedNetworkTypes.add(field.getName());
                }

            }
        }

        if (!unrecognisedNetworkTypes.isEmpty()) {
            fail("Unrecognised Network Types detected : " + unrecognisedNetworkTypes.toString());
        }
    }
}
