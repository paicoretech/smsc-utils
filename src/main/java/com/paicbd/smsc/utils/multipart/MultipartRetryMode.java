package com.paicbd.smsc.utils.multipart;

/**
 * Defines how a multipart message must be processed when it enters a retry flow.
 */
public enum MultipartRetryMode {
    /** Retry every part of the multipart message from the first segment. */
    RETRY_ALL,

    /** Resume from the first failed/pending part without resending already successful parts. */
    FAILED_ONLY
}
