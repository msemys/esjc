package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if there is an attempt to operate inside a transaction which does not exist.
 */
public class InvalidTransactionException extends EventStoreException {
}
