package com.willowwealth.accreditation.adapter.out.persistence.mapper;

import com.willowwealth.accreditation.adapter.out.persistence.entity.DocumentEntity;
import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.ZonedDateTime;

@Mapper
public interface DocumentEntityMapper {

    @Mapping(target = "content", source = "content")
    @Mapping(target = "createdAt", source = "createdAt")
    DocumentEntity toEntity(DocumentReference reference, byte[] content, ZonedDateTime createdAt);

    Document toDomain(DocumentEntity entity);
}
