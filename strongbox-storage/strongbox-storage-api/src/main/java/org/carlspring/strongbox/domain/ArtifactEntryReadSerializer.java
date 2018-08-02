package org.carlspring.strongbox.domain;

import org.carlspring.strongbox.data.domain.EntitySerializer;
import org.springframework.stereotype.Component;

@Component
public class ArtifactEntryReadSerializer extends EntitySerializer<ArtifactEntryReadDto>
{

    @Override
    public int getTypeId()
    {
        return 60;
    }

    @Override
    public Class<ArtifactEntryReadDto> getEntityClass()
    {
        return ArtifactEntryReadDto.class;
    }

}
