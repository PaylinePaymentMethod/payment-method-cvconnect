package com.payline.payment.cvconnect.service.impl;

import com.payline.payment.cvconnect.bean.common.Transaction;
import com.payline.payment.cvconnect.bean.configuration.RequestConfiguration;
import com.payline.payment.cvconnect.bean.request.CancelRequest;
import com.payline.payment.cvconnect.bean.response.PaymentResponse;
import com.payline.payment.cvconnect.exception.PluginException;
import com.payline.payment.cvconnect.utils.http.HttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.refund.response.RefundResponse;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseFailure;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseSuccess;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.RefundService;
import org.apache.logging.log4j.Logger;

public class RefundServiceImpl implements RefundService {
    private static final Logger LOGGER = LogManager.getLogger(RefundServiceImpl.class);
    private HttpClient client = HttpClient.getInstance();

    @Override
    public RefundResponse refundRequest(RefundRequest refundRequest) {
        String partnerTransactionId = refundRequest.getPartnerTransactionId();
        try {
            CancelRequest cancelRequest = new CancelRequest(refundRequest);
            RequestConfiguration configuration = new RequestConfiguration(
                    refundRequest.getContractConfiguration()
                    , refundRequest.getEnvironment()
                    , refundRequest.getPartnerConfiguration()
            );
            PaymentResponse response = client.cancelTransaction(configuration, cancelRequest);

            if (!response.isOk()) {
                // return a failure
                return RefundResponseFailure.RefundResponseFailureBuilder
                        .aRefundResponseFailure()
                        .withErrorCode(response.getErrorCode())
                        .withFailureCause(response.getFailureCause())
                        .withPartnerTransactionId(partnerTransactionId)
                        .build();
            }

            if (!Transaction.State.CANCELLED.equals(response.getTransaction().getState())) {
                // return a failure
                String errorMessage = "Invalid transaction State";
                LOGGER.info(errorMessage);
                return RefundResponseFailure.RefundResponseFailureBuilder
                        .aRefundResponseFailure()
                        .withErrorCode(errorMessage)
                        .withFailureCause(FailureCause.INVALID_DATA)
                        .withPartnerTransactionId(partnerTransactionId)
                        .build();
            }

            // return a success
            return RefundResponseSuccess.RefundResponseSuccessBuilder
                    .aRefundResponseSuccess()
                    .withPartnerTransactionId(partnerTransactionId)
                    .withStatusCode(response.getTransaction().getFullState())
                    .build();
        } catch (PluginException e) {
            return RefundResponseFailure.RefundResponseFailureBuilder
                    .aRefundResponseFailure()
                    .withErrorCode(e.getErrorCode())
                    .withFailureCause(e.getFailureCause())
                    .withPartnerTransactionId(refundRequest.getPartnerTransactionId())
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            return RefundResponseFailure.RefundResponseFailureBuilder
                    .aRefundResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .withPartnerTransactionId(partnerTransactionId)
                    .build();
        }
    }

    @Override
    public boolean canMultiple() {
        return false;
    }

    @Override
    public boolean canPartial() {
        return false;
    }
}
