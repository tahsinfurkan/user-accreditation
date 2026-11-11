package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.query.GetUserAccreditationsQuery;
import com.willowwealth.accreditation.application.port.in.usecase.GetUserAccreditationsUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.exception.UserNotFoundException;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class GetUserAccreditationsService implements GetUserAccreditationsUseCase {

    private final AccreditationRepository repository;

    @Override
    public List<AccreditationSummary> get(GetUserAccreditationsQuery query) {
        var accreditations = repository.findSummariesByUserId(query.userId());
        if (accreditations.isEmpty()) {
            throw new UserNotFoundException(query.userId());
        }
        log.debug("Found {} accreditations for user {}", accreditations.size(), query.userId());
        return accreditations;
    }
}
