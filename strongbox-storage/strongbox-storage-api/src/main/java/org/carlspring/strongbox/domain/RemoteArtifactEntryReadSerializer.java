package org.carlspring.strongbox.domain;

import org.carlspring.strongbox.data.domain.EntitySerializer;
import org.springframework.stereotype.Component;

@Component
public class RemoteArtifactEntryReadSerializer extends EntitySerializer<RemoteArtifactEntryReadDto>
{

    @Override
    public int getTypeId()
    {
        return 70;
    }

    @Override
    public Class<RemoteArtifactEntryReadDto> getEntityClass()
    {
        return RemoteArtifactEntryReadDto.class;
    }

}
