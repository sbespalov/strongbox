package org.carlspring.strongbox.artifact;

import java.io.IOException;

import javax.inject.Inject;

import org.carlspring.strongbox.data.service.EntityLock;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.AsyncEventListener;
import org.carlspring.strongbox.event.artifact.ArtifactEvent;
import org.carlspring.strongbox.event.artifact.ArtifactEventTypeEnum;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.services.ArtifactEntryService;

import com.orientechnologies.common.concur.ONeedRetryException;

public abstract class AsyncArtifctEntryHandler
{

    private static final int RETRY_COUNT = 10;

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Inject
    private EntityLock entityLock;

    private final ArtifactEventTypeEnum eventType;

    public AsyncArtifctEntryHandler(ArtifactEventTypeEnum eventType)
    {
        super();
        this.eventType = eventType;
    }

    @AsyncEventListener
    public void handleUpdated(final ArtifactEvent<RepositoryPath> event)
        throws IOException
    {
        if (eventType.getType() != event.getType())
        {
            return;
        }

        RepositoryPath repositoryPath = (RepositoryPath) event.getPath();
        if (!RepositoryFiles.isArtifact(repositoryPath))
        {
            return;
        }

        ArtifactEntry artifactEntry = repositoryPath.getArtifactEntry();
        entityLock.lock(artifactEntry);
        try
        {

            for (int i = 0; i < RETRY_COUNT; i++)
            {
                artifactEntry = artifactEntryService.lockOne(artifactEntry.getObjectId());
                artifactEntry = handleEvent(artifactEntry);

                try
                {
                    artifactEntryService.save(artifactEntry);
                    break;
                }
                catch (ONeedRetryException e)
                {
                    continue;
                }
            }
        } finally
        {
            entityLock.unlock(artifactEntry);
        }
    }

    protected abstract ArtifactEntry handleEvent(ArtifactEntry artifactEntry);

}
