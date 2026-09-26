package com.succeshub.coreinfra.exception_handler.domain;

import com.succeshub.coreinfra.exception_handler.base.AppException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a deactivated account calls an API that requires an active account.
 */
public class AccountDeactivatedException extends AppException {

    public AccountDeactivatedException() {
        super(
                "Account is scheduled for deletion. Reactivate to continue.",
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DEACTIVATED");
    }
}
