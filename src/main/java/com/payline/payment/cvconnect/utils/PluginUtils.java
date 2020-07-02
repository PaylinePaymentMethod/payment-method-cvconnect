package com.payline.payment.cvconnect.utils;


import com.payline.payment.cvconnect.bean.configuration.RequestConfiguration;
import com.payline.payment.cvconnect.exception.InvalidDataException;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.Email;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PluginUtils {
    private static final String SEAL_ALGORITHM = "HmacSHA256";
    public static final String PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private PluginUtils() {
        // ras.
    }

    public static String truncate(String value, int length) {
        if (value != null && value.length() > length) {
            value = value.substring(0, length);
        }
        return value;
    }

    /**
     * Convert an InputStream into a String
     *
     * @param stream the InputStream to convert
     * @return the converted String encoded in UTF-8
     */
    public static String inputStreamToString(InputStream stream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        return br.lines().collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Encode in Base64 a list of field joined by a "." and hash it
     *
     * @param fields the list of field to convert
     * @return the hashed String
     */
    public static String getSealHeader(RequestConfiguration configuration, List<String> fields) {
        String sealKey = configuration.getPartnerConfiguration().getProperty(Constants.PartnerConfigurationKeys.SEAL_KEY);
        String sealKeyVersion = configuration.getPartnerConfiguration().getProperty(Constants.PartnerConfigurationKeys.SEAL_KEY_VERSION);

        try {
            Mac hmac = Mac.getInstance(SEAL_ALGORITHM);
            hmac.init(new SecretKeySpec(sealKey.getBytes(StandardCharsets.UTF_8), SEAL_ALGORITHM));
            byte[] seal = hmac.doFinal(String.join("&", fields).getBytes(StandardCharsets.UTF_8));

            String base64Seal = Base64.getUrlEncoder().withoutPadding().encodeToString(seal);

            return String.join(".", SEAL_ALGORITHM, sealKeyVersion, base64Seal);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new InvalidDataException(e.getMessage());
        }
    }


    /**
     * Check if a String is null or empty
     *
     * @param value the String to check
     * @return True if the String is null or empty
     */
    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }


    /**
     * find the Currency to create from an ISO 4217 numeric currency code
     *
     * @param code an ISO 4217 numeric currency code
     * @return The right Currency
     */
    public static Currency getCurrencyFromCode(int code) {
        Optional<Currency> currency = Currency.getAvailableCurrencies().stream().filter(c -> c.getNumericCode() == code).findAny();
        return currency.get();
    }


    /**
     * create an Email object from val
     * @param val the String to convert into an Email, if val is not a valid email, add '@id.com' at the end
     * @return
     */
    public static Email buildEmail(String val) {

        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(val);
        if (!matcher.matches()) {
            val += "@id.com";
        }

        return Email.EmailBuilder.anEmail().withEmail(val).build();
    }
}