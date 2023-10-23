package com.carupdateprovider.process.model;

/**
 * Enum for the historydb status which should be persisted through sending a SQS message to cup-history.
 */
public enum Status {

    REQUESTED,
    TRIGGERD,
    FETCHED,
    UNFETCHABLE,
    CREATED,
    ROLLEDOUT,
    NONE
}
