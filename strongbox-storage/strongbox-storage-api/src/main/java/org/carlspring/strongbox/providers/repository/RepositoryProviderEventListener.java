package org.carlspring.strongbox.providers.repository;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;

import org.carlspring.strongbox.data.service.EntityLock;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.AsyncEventListener;
import org.carlspring.strongbox.event.artifact.ArtifactEvent;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class RepositoryProviderEventListener
{

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Inject
    private EntityLock entityLock;

    @Inject
    private PlatformTransactionManager transactionManager;

    @AsyncEventListener(condition = "#root.event.type == 10")
    public void handleUpdated(final ArtifactEvent<RepositoryPath> event)
        throws IOException
    {
        RepositoryPath repositoryPath = (RepositoryPath) event.getPath();

        ArtifactEntry artifactEntryLock = repositoryPath.getArtifactEntry();
        entityLock.lock(artifactEntryLock);
        try
        {
            new TransactionTemplate(transactionManager).execute((s) -> {
                ArtifactEntry artifactEntry = artifactEntryService.findOne(artifactEntryLock.getObjectId()).get();

                artifactEntry.setLastUpdated(new Date());

                return artifactEntryService.save(artifactEntry);
            });
        } finally
        {
            entityLock.unlock(artifactEntryLock);
        }
    }

    @AsyncEventListener(condition = "#root.event.type == 8")
    public void handleDownloading(final ArtifactEvent<RepositoryPath> event)
        throws IOException
    {
        RepositoryPath repositoryPath = (RepositoryPath) event.getPath();

        ArtifactEntry artifactEntryLock = repositoryPath.getArtifactEntry();
        entityLock.lock(artifactEntryLock);
        try
        {
            new TransactionTemplate(transactionManager).execute((s) -> {
                ArtifactEntry artifactEntry = artifactEntryService.findOne(artifactEntryLock.getObjectId()).get();

                artifactEntry.setDownloadCount(artifactEntry.getDownloadCount() + 1);
                artifactEntry.setLastUsed(new Date());

                return artifactEntryService.save(artifactEntry);
            });

        } finally
        {
            entityLock.unlock(artifactEntryLock);
        }
    }

}
