package org.carlspring.strongbox.providers.repository;

import java.util.Date;

import org.carlspring.strongbox.artifact.AsyncArtifctEntryHandler;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.artifact.ArtifactEventTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class ArtifactDownloadingEventHandler extends AsyncArtifctEntryHandler
{

    public ArtifactDownloadingEventHandler()
    {
        super(ArtifactEventTypeEnum.EVENT_ARTIFACT_FILE_DOWNLOADING);
    }

    @Override
    protected ArtifactEntry handleEvent(ArtifactEntry artifactEntry)
    {
        artifactEntry.setDownloadCount(artifactEntry.getDownloadCount() + 1);
        artifactEntry.setLastUsed(new Date());

        return artifactEntry;
    }

}
