package com.willowwealth.accreditation.application.port.in.command;

import java.time.Duration;

public record ExpireConfirmedAccreditationsCommand(Duration confirmedTtl, int batchSize) {
}
