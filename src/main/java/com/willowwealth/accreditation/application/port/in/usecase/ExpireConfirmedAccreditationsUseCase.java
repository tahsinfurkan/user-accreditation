package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.command.ExpireConfirmedAccreditationsCommand;

public interface ExpireConfirmedAccreditationsUseCase {

    /**
     * Publishes an EXPIRED change for every CONFIRMED accreditation that has not been updated within the time to
     * live. One that could not be expired stays CONFIRMED and is picked up again by the next call.
     *
     * @return how many expiries were published
     */
    int expire(ExpireConfirmedAccreditationsCommand command);
}
