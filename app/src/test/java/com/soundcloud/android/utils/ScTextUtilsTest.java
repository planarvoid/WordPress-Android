package com.soundcloud.android.utils;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ScTextUtilsTest extends AndroidUnitTest {

    @Test
    public void testHexString() throws Exception {
        assertThat(ScTextUtils.hexString(new byte[]{0, 12, 32, 0, 16})).isEqualTo("000c200010");
    }

    @Test
    public void shouldGetClippedStringWhenLongerThanMaxLength() throws Exception {
        assertThat(ScTextUtils.getClippedString("1234567890", 5)).isEqualTo("12345");
    }

    @Test
    public void shouldGetClippedStringWhenSmallerThanMaxLength() throws Exception {
        assertThat(ScTextUtils.getClippedString("123", 5)).isEqualTo("123");
    }

    @Test
    public void shouldGetClippedStringWhenLengthEqualToMaxLength() throws Exception {
        assertThat(ScTextUtils.getClippedString("1234567890", 10)).isEqualTo("1234567890");
    }

    @Test
    public void shouldFormatLocation() throws Exception {
        assertThat(ScTextUtils.getLocation(null, null)).isEqualTo("");
        assertThat(ScTextUtils.getLocation("Berlin", null)).isEqualTo("Berlin");
        assertThat(ScTextUtils.getLocation("Berlin", "Germany")).isEqualTo("Berlin, Germany");
        assertThat(ScTextUtils.getLocation(null, "Germany")).isEqualTo("Germany");
    }

    @Test
    public void shouldFormatTimeString() throws Exception {
        assertThat(ScTextUtils.formatTimestamp(5, TimeUnit.SECONDS)).isEqualTo("0:05");
        assertThat(ScTextUtils.formatTimestamp(5, TimeUnit.MINUTES)).isEqualTo("5:00");
        assertThat(ScTextUtils.formatTimestamp(3, TimeUnit.HOURS)).isEqualTo("3:00:00");
        assertThat(ScTextUtils.formatTimestamp(3661, TimeUnit.SECONDS)).isEqualTo("1:01:01");
    }

    @Test
    public void shouldGetTimeString() throws Exception {
        expectTime(1, "1 second");
        expectTime(20, "20 seconds");
        expectTime(60, "1 minute");
        expectTime(60 * 60 * 2.5, "2 hours");
        expectTime(60 * 60 * 24 * 2.5, "2 days");
        expectTime(60 * 60 * 24 * 31, "1 month");
        expectTime(60 * 60 * 24 * 31 * 12 * 2, "2 years");
    }

    @Test
    public void shouldUseSameTimeElapsedString() throws Exception {
        assertThat(ScTextUtils.usesSameTimeElapsedString(10, 10)).isTrue();
        assertThat(ScTextUtils.usesSameTimeElapsedString(10, 30)).isFalse();
        assertThat(ScTextUtils.usesSameTimeElapsedString(60, 61)).isTrue();
        assertThat(ScTextUtils.usesSameTimeElapsedString(124, 150)).isTrue();
        assertThat(ScTextUtils.usesSameTimeElapsedString(124, 200)).isFalse();
    }

    @Test
    public void shouldGetElapsedTime() throws Exception {
        assertThat(ScTextUtils.formatTimeElapsed(
                resources(),
                System.currentTimeMillis() - 1000 * 60)).isEqualTo("1 minute");
    }

    @Test
    public void shouldGetElapsedTimeSinceTimestamp() throws Exception {
        final long timestamp = System.currentTimeMillis() - 1000 * 60;
        assertThat(ScTextUtils.formatTimeElapsedSince(resources(), timestamp, false)).isEqualTo("1 minute");
    }

    private void expectTime(double seconds, String text) {
        assertThat(ScTextUtils.formatTimeElapsed(resources(), seconds, false)).isEqualTo(text);
        assertThat(ScTextUtils.formatTimeElapsed(resources(), seconds, true)).isEqualTo(text + " ago");
    }

    @Test
    public void shouldValidateEmailWithDot() {
        expectEmailValid("test.domain@gmail.com");
    }

    @Test
    public void shouldValidateEmailWithOneWord() {
        expectEmailValid("test@gmail.com");
    }

    @Test
    public void shouldValidateEmailWithOneLetterDomain() {
        expectEmailValid("test@g.com");
    }

    @Test
    public void shouldValidateEmailWithUppercaseLetters() {
        expectEmailValid("TEST@GMAIL.COM");
    }

    @Test
    public void shouldValidateEmailWithPlusSign() {
        expectEmailValid("test+test@gmail.com");
    }

    @Test
    public void shouldNotBeValidForDotChar() {
        expectEmailInvalid("test@gmail.c");
    }

    @Test
    public void shouldNotBeValidWithoutDomain() {
        expectEmailInvalid("foo@barcom");
    }

    @Test
    public void shouldNotBeValidIfEmptyEmail() {
        expectEmailInvalid("");
    }

    @Test
    public void shouldNotBeValidIfNull() {
        expectEmailInvalid(null);
    }

    @Test
    public void shouldNotBeValidIfBlankEmail() {
        expectEmailInvalid("     ");
    }

    @Test
    public void shouldNotBeValidWithSpecialCharacters() {
        expectEmailInvalid("test*w@gmail.com");
    }

    @Test
    public void shouldNotBeValidWithTrailingDot() {
        expectEmailInvalid(".@gmail.com");
    }

    @Test
    public void shouldNotBeValidWithMissplacedDot() {
        expectEmailInvalid("test.@gmail.com");
    }

    @Test
    public void shouldNotBeValidWithMoreAtSymbols() {
        expectEmailInvalid("email@address@example.com");
    }

    @Test
    public void shouldNotBeValidWithoutAtSymbol(){
        expectEmailInvalid("emailaddressexample.com");
    }

    @Test
    public void shouldNotBeValidIfStartsWithDot() {
        expectEmailInvalid(".email.address@example.com");
    }

    @Test
    public void shouldReturnTrueIfStringIsWhitespaceOnly(){
        assertThat(ScTextUtils.isBlank("  ")).isTrue();
    }

    @Test
    public void shouldReturnTrueIfStringIsEmpty(){
        assertThat(ScTextUtils.isBlank("")).isTrue();
    }

    @Test
    public void shouldReturnTrueIfStringIsNull(){
        assertThat(ScTextUtils.isBlank(null)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfStringHasCharacters(){
        assertThat(ScTextUtils.isBlank("  a ")).isFalse();
    }

    @Test
    public void shouldReturnLongWhenParsingValidLongString() {
        assertThat(ScTextUtils.safeParseLong("1")).isEqualTo(1L);
        assertThat(ScTextUtils.safeParseLong(Long.toString(Long.MIN_VALUE))).isEqualTo(Long.MIN_VALUE);
        assertThat(ScTextUtils.safeParseLong(Long.toString(Long.MAX_VALUE))).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void shouldReturnNegativeOneWhenFailingToParseLongString() {
        assertThat(ScTextUtils.safeParseLong("")).isEqualTo(-1L);
        assertThat(ScTextUtils.safeParseLong("not a long")).isEqualTo(-1L);
        assertThat(ScTextUtils.safeParseLong(null)).isEqualTo(-1L);
    }

    @Test
    public void shouldFormatFollowingMessage() {
        assertThat(ScTextUtils.formatFollowingMessage(resources(), 0)).isEqualTo("Followed by you");
        assertThat(ScTextUtils.formatFollowingMessage(resources(), 1)).isEqualTo("Followed by you and 1 other person");
        assertThat(ScTextUtils.formatFollowingMessage(resources(), 100001)).isEqualTo("Followed by you and 100,001 other people");
    }

    @Test
    public void shouldFormatFollowersMessage() {
        assertThat(ScTextUtils.formatFollowersMessage(resources(), 0)).isEqualTo("Followed by 0 people");
        assertThat(ScTextUtils.formatFollowersMessage(resources(), 1)).isEqualTo("Followed by 1 person");
        assertThat(ScTextUtils.formatFollowersMessage(resources(), 100001)).isEqualTo("Followed by 100,001 people");
    }

    @Test
    public void shouldShortenLargeNumbers() {
        assertThat(ScTextUtils.shortenLargeNumber(999)).isEqualTo("999");
        assertThat(ScTextUtils.shortenLargeNumber(1000)).isEqualTo("1K+");
        assertThat(ScTextUtils.shortenLargeNumber(1999)).isEqualTo("1K+");
        assertThat(ScTextUtils.shortenLargeNumber(2000)).isEqualTo("2K+");
        assertThat(ScTextUtils.shortenLargeNumber(9999)).isEqualTo("9K+");
        assertThat(ScTextUtils.shortenLargeNumber(10000)).isEqualTo("9K+"); // 4 chars would make the text spill over again
    }

    @Test
    public void shouldFormatLargeNumbers() {
        assertThat(ScTextUtils.formatLargeNumber(0)).isEqualTo("");
        assertThat(ScTextUtils.formatLargeNumber(999)).isEqualTo("999");
        assertThat(ScTextUtils.formatLargeNumber(9999)).isEqualTo("9,999");

        assertThat(ScTextUtils.formatLargeNumber(10000)).isEqualTo("10K");
        assertThat(ScTextUtils.formatLargeNumber(11412)).isEqualTo("11.4K");
        assertThat(ScTextUtils.formatLargeNumber(999999)).isEqualTo("999.9K");

        assertThat(ScTextUtils.formatLargeNumber(1000000)).isEqualTo("1M");
        assertThat(ScTextUtils.formatLargeNumber(1200000)).isEqualTo("1.2M");
        assertThat(ScTextUtils.formatLargeNumber(999200000)).isEqualTo("999.2M");

        assertThat(ScTextUtils.formatLargeNumber(1000000000)).isEqualTo("1BN");
    }

    @Test
    public void shouldFormatSeconds() {
        assertThat(ScTextUtils.formatSecondsOrMinutes(resources(), 30, TimeUnit.SECONDS)).isEqualTo("30 sec.");
        assertThat(ScTextUtils.formatSecondsOrMinutes(resources(), 30, TimeUnit.MILLISECONDS)).isEqualTo("0 sec.");
    }

    @Test
    public void shouldFormatMinutes() {
        assertThat(ScTextUtils.formatSecondsOrMinutes(resources(), 60, TimeUnit.SECONDS)).isEqualTo("1 min.");
        assertThat(ScTextUtils.formatSecondsOrMinutes(resources(), 72, TimeUnit.SECONDS)).isEqualTo("1 min.");
        assertThat(ScTextUtils.formatSecondsOrMinutes(resources(), 1, TimeUnit.HOURS)).isEqualTo("60 min.");
    }

    private void expectEmailValid(String string){
        assertThat(ScTextUtils.isEmail(string)).isTrue();
    }

    private void expectEmailInvalid(String string){
        assertThat(ScTextUtils.isEmail(string)).isFalse();
    }
}
