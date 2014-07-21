package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.res.Resources;

import java.util.concurrent.TimeUnit;

@RunWith(DefaultTestRunner.class)
public class ScTextUtilsTest {

    @Test
    public void testHexString() throws Exception {
        expect(ScTextUtils.hexString(new byte[]{0, 12, 32, 0, 16})).toEqual("000c200010");
    }

    @Test
    public void shouldGetClippedStringWhenLongerThanMaxLength() throws Exception {
        expect(ScTextUtils.getClippedString("1234567890", 5)).toEqual("12345");
    }

    @Test
    public void shouldGetClippedStringWhenSmallerThanMaxLength() throws Exception {
        expect(ScTextUtils.getClippedString("123", 5)).toEqual("123");
    }

    @Test
    public void shouldGetClippedStringWhenLengthEqualToMaxLength() throws Exception {
        expect(ScTextUtils.getClippedString("1234567890", 10)).toEqual("1234567890");
    }

    @Test
    public void shouldFormatLocation() throws Exception {
        expect(ScTextUtils.getLocation(null, null)).toEqual("");
        expect(ScTextUtils.getLocation("Berlin", null)).toEqual("Berlin");
        expect(ScTextUtils.getLocation("Berlin", "Germany")).toEqual("Berlin, Germany");
        expect(ScTextUtils.getLocation(null, "Germany")).toEqual("Germany");
    }

    @Test
    public void shouldFormatTimeString() throws Exception {
        expect(ScTextUtils.formatTimestamp(5, TimeUnit.SECONDS)).toEqual("0:05");
        expect(ScTextUtils.formatTimestamp(5, TimeUnit.MINUTES)).toEqual("5:00");
        expect(ScTextUtils.formatTimestamp(3, TimeUnit.HOURS)).toEqual("3:00:00");
        expect(ScTextUtils.formatTimestamp(3661, TimeUnit.SECONDS)).toEqual("1:01:01");
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
        expect(ScTextUtils.usesSameTimeElapsedString(10, 10)).toBeTrue();
        expect(ScTextUtils.usesSameTimeElapsedString(10, 30)).toBeFalse();
        expect(ScTextUtils.usesSameTimeElapsedString(60, 61)).toBeTrue();
        expect(ScTextUtils.usesSameTimeElapsedString(124, 150)).toBeTrue();
        expect(ScTextUtils.usesSameTimeElapsedString(124, 200)).toBeFalse();
    }

    @Test
    public void shouldGetElapsedTime() throws Exception {
        expect(ScTextUtils.formatTimeElapsed(
                Robolectric.application.getResources(),
                System.currentTimeMillis() - 1000 * 60)).toEqual("1 minute");
    }

    private void expectTime(double seconds, String text) {
        expect(ScTextUtils.formatTimeElapsed(Robolectric.application.getResources(), seconds, false)).toEqual(text);
        expect(ScTextUtils.formatTimeElapsed(Robolectric.application.getResources(), seconds, true)).toEqual(text + " ago");
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
        expect(ScTextUtils.isBlank("  ")).toBeTrue();
    }

    @Test
    public void shouldReturnTrueIfStringIsEmpty(){
        expect(ScTextUtils.isBlank("")).toBeTrue();
    }

    @Test
    public void shouldReturnTrueIfStringIsNull(){
        expect(ScTextUtils.isBlank(null)).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfStringHasCharacters(){
        expect(ScTextUtils.isBlank("  a ")).toBeFalse();
    }

    @Test
    public void shouldReturnLongWhenParsingValidLongString() {
        expect(ScTextUtils.safeParseLong("1")).toEqual(1L);
        expect(ScTextUtils.safeParseLong(Long.toString(Long.MIN_VALUE))).toEqual(Long.MIN_VALUE);
        expect(ScTextUtils.safeParseLong(Long.toString(Long.MAX_VALUE))).toEqual(Long.MAX_VALUE);
    }

    @Test
    public void shouldReturnNegativeOneWhenFailingToParseLongString() {
        expect(ScTextUtils.safeParseLong("")).toEqual(-1L);
        expect(ScTextUtils.safeParseLong("not a long")).toEqual(-1L);
        expect(ScTextUtils.safeParseLong(null)).toEqual(-1L);
    }

    @Test
    public void shouldFormatFollowingMessage() {
        Resources r = Robolectric.application.getResources();
        expect(ScTextUtils.formatFollowingMessage(r, 0)).toEqual("Followed by you");
        expect(ScTextUtils.formatFollowingMessage(r, 1)).toEqual("Followed by you and 1 other person");
        expect(ScTextUtils.formatFollowingMessage(r, 100001)).toEqual("Followed by you and 100,001 other people");
    }

    @Test
    public void shouldFormatFollowersMessage() {
        Resources r = Robolectric.application.getResources();
        expect(ScTextUtils.formatFollowersMessage(r, 0)).toEqual("Followed by 0 people");
        expect(ScTextUtils.formatFollowersMessage(r, 1)).toEqual("Followed by 1 person");
        expect(ScTextUtils.formatFollowersMessage(r, 100001)).toEqual("Followed by 100,001 people");
    }

    @Test
    public void shouldShortenLargeNumbers() {
        expect(ScTextUtils.shortenLargeNumber(999)).toEqual("999");
        expect(ScTextUtils.shortenLargeNumber(1000)).toEqual("1k+");
        expect(ScTextUtils.shortenLargeNumber(1999)).toEqual("1k+");
        expect(ScTextUtils.shortenLargeNumber(2000)).toEqual("2k+");
        expect(ScTextUtils.shortenLargeNumber(9999)).toEqual("9k+");
        expect(ScTextUtils.shortenLargeNumber(10000)).toEqual("9k+"); // 4 chars would make the text spill over again
    }

    private void expectEmailValid(String string){
        expect(ScTextUtils.isEmail(string)).toBeTrue();
    }

    private void expectEmailInvalid(String string){
        expect(ScTextUtils.isEmail(string)).toBeFalse();
    }
}
