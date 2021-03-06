package com.payline.payment.cvconnect.service.impl;

import com.payline.payment.cvconnect.bean.common.Transaction;
import com.payline.payment.cvconnect.bean.configuration.RequestConfiguration;
import com.payline.payment.cvconnect.bean.request.ConfirmTransactionRequest;
import com.payline.payment.cvconnect.bean.request.CreateTransactionRequest;
import com.payline.payment.cvconnect.bean.response.PaymentResponse;
import com.payline.payment.cvconnect.exception.PluginException;
import com.payline.payment.cvconnect.utils.http.HttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseActiveWaiting;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.Logger;

public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);
    private HttpClient client = HttpClient.getInstance();


    @Override
    public com.payline.pmapi.bean.payment.response.PaymentResponse paymentRequest(PaymentRequest paymentRequest) {
        try {
            // init data
            RequestConfiguration requestConfiguration = new RequestConfiguration(
                    paymentRequest.getContractConfiguration()
                    , paymentRequest.getEnvironment()
                    , paymentRequest.getPartnerConfiguration()
            );

            //  call httpClient to create the transaction
            CreateTransactionRequest createTransactionRequest = new CreateTransactionRequest(paymentRequest);
            PaymentResponse createResponse = client.createTransaction(requestConfiguration, createTransactionRequest);

            // check response object
            if (!createResponse.isOk()) {
                return PaymentResponseFailure.PaymentResponseFailureBuilder
                        .aPaymentResponseFailure()
                        .withErrorCode(createResponse.getErrorCode())
                        .withFailureCause(createResponse.getFailureCause())
                        .build();
            }

            // get transactionId and state
            String partnerTransactionId = createResponse.getTransaction().getId();


            if (!Transaction.State.INITIALIZED.equals(createResponse.getTransaction().getState())) {
                String errorMessage = "Invalid transaction State";
                LOGGER.info(errorMessage);
                return PaymentResponseFailure.PaymentResponseFailureBuilder
                        .aPaymentResponseFailure()
                        .withPartnerTransactionId(partnerTransactionId)
                        .withErrorCode(errorMessage)
                        .withFailureCause(FailureCause.INVALID_DATA)
                        .build();
            }

            // call httpClient to confirm the transaction
            ConfirmTransactionRequest confirmTransactionRequest = new ConfirmTransactionRequest(paymentRequest, partnerTransactionId);
            PaymentResponse confirmResponse = client.confirmTransaction(requestConfiguration, confirmTransactionRequest);

            // check response object
            if (!confirmResponse.isOk()) {
                return PaymentResponseFailure.PaymentResponseFailureBuilder
                        .aPaymentResponseFailure()
                        .withPartnerTransactionId(partnerTransactionId)
                        .withErrorCode(confirmResponse.getErrorCode())
                        .withFailureCause(confirmResponse.getFailureCause())
                        .build();
            }

            return new PaymentResponseActiveWaiting();

        } catch (PluginException e) {
            return e.toPaymentResponseFailureBuilder().build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            return PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }

    }

}
