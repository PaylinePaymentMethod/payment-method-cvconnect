package com.payline.payment.cvconnect.service.impl;

import com.payline.payment.cvconnect.bean.common.Transaction;
import com.payline.payment.cvconnect.exception.PluginException;
import com.payline.payment.cvconnect.utils.PluginUtils;
import com.payline.pmapi.bean.common.*;
import com.payline.pmapi.bean.notification.request.NotificationRequest;
import com.payline.pmapi.bean.notification.response.NotificationResponse;
import com.payline.pmapi.bean.notification.response.impl.IgnoreNotificationResponse;
import com.payline.pmapi.bean.notification.response.impl.PaymentResponseByNotificationResponse;
import com.payline.pmapi.bean.notification.response.impl.TransactionStateChangedResponse;
import com.payline.pmapi.bean.payment.request.NotifyTransactionStatusRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.Email;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.EmptyTransactionDetails;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.NotificationService;
import org.apache.logging.log4j.Logger;

import java.util.Date;

public class NotificationServiceImpl implements NotificationService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);

    @Override
    public NotificationResponse parse(NotificationRequest request) {
        NotificationResponse notificationResponse;
        String partnerTransactionId = "UNKNOWN";
        try {
            // init data
            String content = PluginUtils.inputStreamToString(request.getContent());
            com.payline.payment.cvconnect.bean.response.PaymentResponse notificationPaymentResponse = com.payline.payment.cvconnect.bean.response.PaymentResponse.fromJson(content);
            Transaction transaction = notificationPaymentResponse.getTransaction();
            partnerTransactionId = transaction.getId();

            String transactionId = transaction.getOrder().getId();

            TransactionCorrelationId correlationId = TransactionCorrelationId.TransactionCorrelationIdBuilder
                    .aCorrelationIdBuilder()
                    .withType(TransactionCorrelationId.CorrelationIdType.TRANSACTION_ID)
                    .withValue(transactionId)
                    .build();

            switch (transaction.getState()) {
                case AUTHORIZED:
                    // PaymentResponseByNotificationResponse => Success
                    Amount reservedAmount = new Amount(
                            transaction.getPayer().getFirstAuthorization().getAmount().getTotal()
                            , PluginUtils.getCurrencyFromCode(transaction.getPayer().getFirstAuthorization().getAmount().getCurrency())
                    );

                    PaymentResponse paymentResponse = PaymentResponseSuccess.PaymentResponseSuccessBuilder
                            .aPaymentResponseSuccess()
                            .withPartnerTransactionId(partnerTransactionId)
                            .withStatusCode(transaction.getFullState())
                            .withReservedAmount(reservedAmount)  //
                            .withTransactionDetails(PluginUtils.buildEmail(transaction.getPayer().getBeneficiaryId()))
                            .build();

                    notificationResponse = createPaymentResponseByNotification(correlationId, paymentResponse);
                    break;
                case ABORTED:
                    // PaymentResponseByNotificationResponse => Failure
                    paymentResponse = PaymentResponseFailure.PaymentResponseFailureBuilder
                            .aPaymentResponseFailure()
                            .withPartnerTransactionId(partnerTransactionId)
                            .withErrorCode(transaction.getFullState())
                            .withFailureCause(FailureCause.CANCEL)
                            .withTransactionDetails(new EmptyTransactionDetails())
                            .build();

                    notificationResponse = createPaymentResponseByNotification(correlationId, paymentResponse);
                    break;
                case EXPIRED:
                    // PaymentResponseByNotificationResponse => Failure
                    paymentResponse = PaymentResponseFailure.PaymentResponseFailureBuilder
                            .aPaymentResponseFailure()
                            .withPartnerTransactionId(partnerTransactionId)
                            .withErrorCode(transaction.getFullState())
                            .withFailureCause(FailureCause.SESSION_EXPIRED)
                            .withTransactionDetails(new EmptyTransactionDetails())
                            .build();

                    notificationResponse = createPaymentResponseByNotification(correlationId, paymentResponse);
                    break;

                // TransactionStateChangedResponse
                case VALIDATED:
                    TransactionStatus paylineStatus = SuccessTransactionStatus.builder().build();
                    notificationResponse = createTransactionStateChanged(transactionId, partnerTransactionId, paylineStatus);
                    break;
                case CONSIGNED:
                    paylineStatus = OnHoldTransactionStatus.builder().onHoldCause(OnHoldCause.ASYNC_RETRY).build();
                    notificationResponse = createTransactionStateChanged(transactionId, partnerTransactionId, paylineStatus);
                    break;
                case REJECTED:
                    paylineStatus = FailureTransactionStatus.builder().failureCause(FailureCause.REFUSED).build();
                    notificationResponse = createTransactionStateChanged(transactionId, partnerTransactionId, paylineStatus);
                    break;
                case PAID:
                case CANCELLED:
                    paylineStatus = SuccessTransactionStatus.builder().build();
                    notificationResponse = createTransactionStateChanged(transactionId, partnerTransactionId, paylineStatus);
                    break;
                case INITIALIZED:
                case PROCESSING:
                default:
                    notificationResponse = new IgnoreNotificationResponse();
                    break;

            }

        } catch (PluginException e) {
            TransactionCorrelationId correlationId = TransactionCorrelationId.TransactionCorrelationIdBuilder
                    .aCorrelationIdBuilder()
                    .withType(TransactionCorrelationId.CorrelationIdType.PARTNER_TRANSACTION_ID)
                    .withValue(partnerTransactionId)
                    .build();

            PaymentResponse failureResponse = e.toPaymentResponseFailureBuilder()
                    .withPartnerTransactionId(partnerTransactionId)
                    .build();

            notificationResponse = createPaymentResponseByNotification(correlationId, failureResponse);

        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            TransactionCorrelationId correlationId = TransactionCorrelationId.TransactionCorrelationIdBuilder
                    .aCorrelationIdBuilder()
                    .withType(TransactionCorrelationId.CorrelationIdType.PARTNER_TRANSACTION_ID)
                    .withValue(partnerTransactionId)
                    .build();

            PaymentResponse failureResponse = PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .withPartnerTransactionId(partnerTransactionId)
                    .build();

            notificationResponse = createPaymentResponseByNotification(correlationId, failureResponse);
        }

        return notificationResponse;
    }

    @Override
    public void notifyTransactionStatus(NotifyTransactionStatusRequest notifyTransactionStatusRequest) {
        // does nothing
    }


    private PaymentResponseByNotificationResponse createPaymentResponseByNotification(TransactionCorrelationId correlationId, PaymentResponse paymentResponse) {
        return PaymentResponseByNotificationResponse.PaymentResponseByNotificationResponseBuilder
                .aPaymentResponseByNotificationResponseBuilder()
                .withPaymentResponse(paymentResponse)
                .withTransactionCorrelationId(correlationId)
                .build();
    }

    private TransactionStateChangedResponse createTransactionStateChanged(String transactionId, String partnerTransactionId, TransactionStatus status) {
        return TransactionStateChangedResponse.TransactionStateChangedResponseBuilder
                .aTransactionStateChangedResponse()
                .withPartnerTransactionId(partnerTransactionId)
                .withTransactionId(transactionId)
                .withTransactionStatus(status)
                .withStatusDate(new Date())
                .build();
    }
}
