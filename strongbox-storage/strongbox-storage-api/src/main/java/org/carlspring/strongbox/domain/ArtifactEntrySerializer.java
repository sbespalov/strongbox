package org.carlspring.strongbox.domain;

import org.carlspring.strongbox.data.domain.EntitySerializer;
import org.springframework.stereotype.Component;

import com.esotericsoftware.kryo.Kryo;

@Component
public class ArtifactEntrySerializer extends EntitySerializer<ArtifactEntry>
{

    @Override
    protected void init(Kryo kryo)
    {
        super.init(kryo);

        kryo.register(ArtifactArchiveListing.class);
    }

    @Override
    public int getTypeId()
    {
        return 10;
    }

    @Override
    public Class<ArtifactEntry> getEntityClass()
    {
        return ArtifactEntry.class;
    }

}
