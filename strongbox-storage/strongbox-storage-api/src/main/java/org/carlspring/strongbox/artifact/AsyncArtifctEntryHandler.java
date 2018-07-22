package org.carlspring.strongbox.artifact;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;

import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.AsyncEventListener;
import org.carlspring.strongbox.event.artifact.ArtifactEvent;
import org.carlspring.strongbox.event.artifact.ArtifactEventTypeEnum;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RepositoryPathLock;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.orientechnologies.common.concur.ONeedRetryException;

public abstract class AsyncArtifctEntryHandler
{

    private static final int RETRY_COUNT = 10;

    private static final Logger logger = LoggerFactory.getLogger(AsyncArtifctEntryHandler.class);

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Inject
    private RepositoryPathLock repositoryPathLock;

    @Inject
    private PlatformTransactionManager transactionManager;

    private final ArtifactEventTypeEnum eventType;

    public AsyncArtifctEntryHandler(ArtifactEventTypeEnum eventType)
    {
        super();
        this.eventType = eventType;
    }

    @AsyncEventListener
    //@Transactional
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

        ReadWriteLock lock = repositoryPathLock.lock(repositoryPath, AsyncArtifctEntryHandler.class.getSimpleName());
        lock.writeLock().lock();

        try
        {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(t -> {
                try
                {
                    return handleWithinTransaction(repositoryPath);
                }
                catch (IOException e)
                {
                    return null;
                }
            });
        } finally
        {
            lock.writeLock().unlock();
        }
    }

    private ArtifactEntry handleWithinTransaction(RepositoryPath repositoryPath)
        throws IOException
    {
        ArtifactEntry artifactEntry = repositoryPath.getArtifactEntry();
        for (int i = 0; i < RETRY_COUNT; i++)
        {
            artifactEntry = handleEvent(repositoryPath);

            try
            {
                return artifactEntryService.save(artifactEntry);
            }
            catch (ONeedRetryException e)
            {
                logger.warn(String.format("Retry [%s] save operation, iteration [%s] in [%s].",
                                          ArtifactEntry.class.getSimpleName(), i,
                                          this.getClass().getSimpleName()));
                continue;
            }
        }
        return null;
    }

    protected abstract ArtifactEntry handleEvent(RepositoryPath repositoryPath)
        throws IOException;

}
