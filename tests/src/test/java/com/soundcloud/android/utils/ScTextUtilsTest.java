package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class ScTextUtilsTest {

    @Test
    public void testHexString() throws Exception {
        expect(ScTextUtils.hexString(new byte[]{0, 12, 32, 0, 16})).toEqual("000c200010");
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
        expect(ScTextUtils.formatTimestamp(5 * 1000)).toEqual("0.05");
        expect(ScTextUtils.formatTimestamp(60 * 1000 * 5)).toEqual("5.00");
        expect(ScTextUtils.formatTimestamp(60 * 1000 * 60 * 3)).toEqual("3.00.00");
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
        expect(ScTextUtils.getTimeElapsed(
                Robolectric.application.getResources(),
                System.currentTimeMillis() - 1000 * 60)).toEqual("1 minute");
    }

    private void expectTime(double seconds, String text) {
        expect(ScTextUtils.getTimeString(Robolectric.application.getResources(), seconds, false)).toEqual(text);
        expect(ScTextUtils.getTimeString(Robolectric.application.getResources(), seconds, true)).toEqual(text + " ago");
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

    private void expectEmailValid(String string){
        expect(ScTextUtils.isEmail(string)).toBeTrue();
    }

    private void expectEmailInvalid(String string){
        expect(ScTextUtils.isEmail(string)).toBeFalse();
    }
}
