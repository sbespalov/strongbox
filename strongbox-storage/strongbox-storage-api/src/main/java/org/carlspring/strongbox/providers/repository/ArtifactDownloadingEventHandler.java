package org.carlspring.strongbox.providers.repository;

import java.io.IOException;
import java.util.Date;

import org.carlspring.strongbox.artifact.AsyncArtifactEntryHandler;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.artifact.ArtifactEventTypeEnum;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.springframework.stereotype.Component;

@Component
public class ArtifactDownloadingEventHandler extends AsyncArtifactEntryHandler
{

    public ArtifactDownloadingEventHandler()
    {
        super(ArtifactEventTypeEnum.EVENT_ARTIFACT_FILE_DOWNLOADING);
    }

    @Override
    protected ArtifactEntry handleEvent(RepositoryPath repositoryPath) throws IOException
    {
        ArtifactEntry artifactEntry = repositoryPath.getArtifactEntry().getArtifactEntry();
        
        artifactEntry.setDownloadCount(artifactEntry.getDownloadCount() + 1);
        artifactEntry.setLastUsed(new Date());

        return artifactEntry;
    }

}
