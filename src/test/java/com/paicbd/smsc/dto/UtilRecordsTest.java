package com.paicbd.smsc.dto;

import com.paicbd.smsc.utils.GeneralSmscConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilRecordsTest {

    @Test
    void testIsFinalSegmentForSplitSmsc() {
        String messageId = System.currentTimeMillis() + "-" + System.nanoTime();
        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent = new UtilsRecords.SubmitSmResponseEvent(
                messageId,
                System.currentTimeMillis() + "-" + System.nanoTime(),
                "systemId",
                messageId,
                messageId,
                GeneralSmscConstants.SMPP_PROTOCOL,
                1,
                "SP",
                null,
                null,
                null,
                messageId,
                2,
                false,
                null,
                false
        );
        Assertions.assertTrue(submitSmResponseEvent.isFinalSegmentForSplitSmsc());


        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent2 = new UtilsRecords.SubmitSmResponseEvent(
                messageId,
                System.currentTimeMillis() + "-" + System.nanoTime(),
                "systemId",
                messageId,
                messageId,
                GeneralSmscConstants.SMPP_PROTOCOL,
                1,
                "SP",
                null,
                null,
                null,
                messageId,
                2,
                false,
                null,
                true
        );
        Assertions.assertTrue(submitSmResponseEvent2.isFinalSegmentForSplitSmsc());

        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent3 = new UtilsRecords.SubmitSmResponseEvent(
                messageId,
                System.currentTimeMillis() + "-" + System.nanoTime(),
                "systemId",
                messageId,
                messageId,
                GeneralSmscConstants.SMPP_PROTOCOL,
                1,
                "SP",
                "00",
                3,
                1,
                messageId,
                2,
                false,
                null,
                true
        );
        Assertions.assertFalse(submitSmResponseEvent3.isFinalSegmentForSplitSmsc());


        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent4 = new UtilsRecords.SubmitSmResponseEvent(
                messageId,
                System.currentTimeMillis() + "-" + System.nanoTime(),
                "systemId",
                messageId,
                messageId,
                GeneralSmscConstants.SMPP_PROTOCOL,
                1,
                "SP",
                "00",
                3,
                3,
                messageId,
                3,
                false,
                null,
                true
        );
        Assertions.assertTrue(submitSmResponseEvent4.isFinalSegmentForSplitSmsc());
    }
}
