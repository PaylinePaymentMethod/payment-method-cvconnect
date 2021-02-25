package com.payline.payment.cvconnect.service.impl;

import com.payline.payment.cvconnect.MockUtils;
import com.payline.payment.cvconnect.bean.common.Transaction;
import com.payline.payment.cvconnect.bean.response.PaymentResponse;
import com.payline.payment.cvconnect.exception.PluginException;
import com.payline.payment.cvconnect.utils.http.HttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.common.FailureTransactionStatus;
import com.payline.pmapi.bean.common.SuccessTransactionStatus;
import com.payline.pmapi.bean.notification.request.NotificationRequest;
import com.payline.pmapi.bean.notification.response.NotificationResponse;
import com.payline.pmapi.bean.notification.response.impl.IgnoreNotificationResponse;
import com.payline.pmapi.bean.notification.response.impl.PaymentResponseByNotificationResponse;
import com.payline.pmapi.bean.notification.response.impl.TransactionStateChangedResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.Email;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.stream.Stream;

import static com.payline.payment.cvconnect.bean.common.Transaction.State.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceImplTest {
    private NotificationServiceImpl service = new NotificationServiceImpl();

    private static Stream<Arguments> statusSet() {
        return Stream.of(
                Arguments.of(VALIDATED, "123123123", TransactionStateChangedResponse.class),
                Arguments.of(VALIDATED, null, PaymentResponseByNotificationResponse.class),
                Arguments.of(ABORTED, "123123123", PaymentResponseByNotificationResponse.class),
                Arguments.of(EXPIRED, "123123123", PaymentResponseByNotificationResponse.class),
                Arguments.of(CONSIGNED, "123123123", TransactionStateChangedResponse.class),
                Arguments.of(REJECTED, "123123123", TransactionStateChangedResponse.class),
                Arguments.of(PAID, "123123123", TransactionStateChangedResponse.class),
                Arguments.of(CANCELLED, "123123123", TransactionStateChangedResponse.class),
                Arguments.of(INITIALIZED, "123123123", IgnoreNotificationResponse.class),
                Arguments.of(PROCESSING, "123123123", IgnoreNotificationResponse.class),
                Arguments.of(AUTHORIZED, "123123123", PaymentResponseByNotificationResponse.class)
        );
    }

    @ParameterizedTest
    @MethodSource("statusSet")
    void parse(Transaction.State status, String transactionId, Class responseClass) {
        String json = MockUtils.aCVCoResponse(status);

        NotificationRequest request = MockUtils.aPaylineNotificationRequestBuilder()
                .withContent(new ByteArrayInputStream(json.getBytes()))
                .withTransactionId(transactionId)
                .build();

        NotificationResponse response = service.parse(request);

        assertNotNull(response);
        Assertions.assertEquals(responseClass, response.getClass());
    }


    @Test
    void onTransactionValidated() {
        final String json = MockUtils.aCVCoResponse(VALIDATED);
        NotificationRequest request = MockUtils.aPaylineNotificationRequestBuilder()
                .withContent(new ByteArrayInputStream(json.getBytes()))
                .withTransactionId(null)
                .build();
        NotificationResponse response = service.parse(request);
        assertNotNull(response);
        assertTrue(response instanceof PaymentResponseByNotificationResponse);
        final PaymentResponseByNotificationResponse notificationResponse =
                (PaymentResponseByNotificationResponse) response;
        assertTrue(notificationResponse.getPaymentResponse() instanceof PaymentResponseSuccess);
        final PaymentResponseSuccess paymentResponseSuccess  = (PaymentResponseSuccess) notificationResponse.getPaymentResponse();
        assertEquals("fiawa31zot", paymentResponseSuccess.getPartnerTransactionId());
        assertEquals("ntrupond71@yopmail.com", ((Email) paymentResponseSuccess.getTransactionDetails()).getEmail());
        assertEquals(new BigInteger("2000"), paymentResponseSuccess.getReservedAmount().getAmountInSmallestUnit());
    }

}