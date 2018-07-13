package org.carlspring.strongbox.providers.repository;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.carlspring.strongbox.data.service.EntityLock;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.AsyncEventListener;
import org.carlspring.strongbox.event.artifact.ArtifactEvent;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class RepositoryProviderEventListener
{

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Inject
    private EntityLock entityLock;

    @AsyncEventListener(condition = "#root.event.type == 10")
    @Transactional
    public void handleUpdated(final ArtifactEvent<RepositoryPath> event)
        throws IOException
    {
        RepositoryPath repositoryPath = (RepositoryPath) event.getPath();

        ArtifactEntry artifactEntryLock = repositoryPath.getArtifactEntry();
        entityLock.lock(artifactEntryLock);
        try
        {
            ArtifactEntry artifactEntry = artifactEntryService.findOne(artifactEntryLock.getObjectId()).get();

            artifactEntry.setLastUpdated(new Date());

            artifactEntry = artifactEntryService.save(artifactEntry);
        } finally
        {
            entityLock.unlock(artifactEntryLock);
        }
    }

    @AsyncEventListener(condition = "#root.event.type == 8")
    @Transactional
    public void handleDownloading(final ArtifactEvent<RepositoryPath> event)
        throws IOException
    {
        RepositoryPath repositoryPath = (RepositoryPath) event.getPath();

        ArtifactEntry artifactEntryLock = repositoryPath.getArtifactEntry();
        entityLock.lock(artifactEntryLock);
        try
        {
            ArtifactEntry artifactEntry = artifactEntryService.findOne(artifactEntryLock.getObjectId()).get();

            artifactEntry.setDownloadCount(artifactEntry.getDownloadCount() + 1);
            artifactEntry.setLastUsed(new Date());

            artifactEntry = artifactEntryService.save(artifactEntry);
        } finally
        {
            entityLock.unlock(artifactEntryLock);
        }
    }

}
