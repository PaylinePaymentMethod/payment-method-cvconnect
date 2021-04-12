package com.payline.payment.cvconnect.utils.i18n;

import com.payline.pmapi.logger.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18nService {

    private static final Logger LOGGER = LogManager.getLogger(I18nService.class);

    /**
     * Private constructor
     */
    private I18nService() {
        Locale.setDefault(new Locale("fr"));
    }

    /**
     * Holder
     */
    private static class SingletonHolder {
        /**
         * Unique instance, not preinitializes
         */
        private static final I18nService instance = new I18nService();
    }

    /**
     * Unique access point for the singleton instance
     */
    public static I18nService getInstance() {
        return SingletonHolder.instance;
    }

    public String getMessage(final String key, final Locale locale) {
        ResourceBundle messages = ResourceBundle.getBundle("messages", locale);
        try {
            return messages.getString(key);
        } catch (MissingResourceException e) {
            LOGGER.error("Trying to get a message with a key that does not exist: {} (language: {})", key, locale.getLanguage());
            return "???" + locale + "." + key + "???";
        }
    }

    // If ever needed, implement getMessage( String, Locale, String... ) to insert values into the translation messages
}