package com.willowwealth.accreditation.application.port.in.command;

import java.time.Duration;

public record DeleteOrphanedDocumentsCommand(Duration gracePeriod) {
}
