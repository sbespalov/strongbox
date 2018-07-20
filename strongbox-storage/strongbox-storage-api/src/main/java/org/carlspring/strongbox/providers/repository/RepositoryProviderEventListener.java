package org.carlspring.strongbox.providers.repository;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;

import org.carlspring.strongbox.data.service.EntityLock;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.AsyncEventListener;
import org.carlspring.strongbox.event.artifact.ArtifactEvent;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.orientechnologies.common.concur.ONeedRetryException;

@Component
public class RepositoryProviderEventListener
{

    private static final int RETRY_COUNT = 3;

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Inject
    private EntityLock entityLock;

    @AsyncEventListener(condition = "#root.event.type == 10")
    public void handleUpdated(final ArtifactEvent<RepositoryPath> event)
        throws IOException
    {
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
                artifactEntry = artifactEntryService.findOne(artifactEntry.getObjectId()).get();
                artifactEntry.setLastUpdated(new Date());

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
        } 
        finally
        {
            entityLock.unlock(artifactEntry);
        }
    }

    @AsyncEventListener(condition = "#root.event.type == 8")
    @Transactional
    public void handleDownloading(final ArtifactEvent<RepositoryPath> event)
        throws IOException
    {
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
                artifactEntry = artifactEntryService.findOne(artifactEntry.getObjectId()).get();
                artifactEntry.setLastUpdated(new Date());

                artifactEntry.setDownloadCount(artifactEntry.getDownloadCount() + 1);
                artifactEntry.setLastUsed(new Date());

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
        } 
        finally
        {
            entityLock.unlock(artifactEntry);
        }
    }

}
