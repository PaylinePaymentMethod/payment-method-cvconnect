package com.payline.payment.cvconnect.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class PluginUtilsTest {

    @Test
    void truncate() {
        Assertions.assertEquals("foo", PluginUtils.truncate("foo", 3));
        Assertions.assertEquals("foo", PluginUtils.truncate("fooooooooo", 3));
        Assertions.assertEquals("foo", PluginUtils.truncate("foo", 5));
    }

    @Test
    void isEmpty() {
        Assertions.assertTrue(PluginUtils.isEmpty(null));
        Assertions.assertTrue(PluginUtils.isEmpty(""));
        Assertions.assertFalse(PluginUtils.isEmpty("foo"));
    }

    @Test
    void getCurrencyFromCode() {
        Assertions.assertEquals(Currency.getInstance("USD"), PluginUtils.getCurrencyFromCode(840));
        Assertions.assertEquals(Currency.getInstance("EUR"), PluginUtils.getCurrencyFromCode(978));
    }

    @Test
    void buildEmail(){
        Assertions.assertEquals("foo@bar.baz", PluginUtils.buildEmail("foo@bar.baz").getEmail());
        Assertions.assertEquals("foo@id.com", PluginUtils.buildEmail("foo").getEmail());
    }
}