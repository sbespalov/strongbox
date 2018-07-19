package org.carlspring.strongbox.providers.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;

import org.apache.commons.io.output.CountingOutputStream;
import org.carlspring.strongbox.artifact.ArtifactNotFoundException;
import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.data.criteria.Expression.ExpOperator;
import org.carlspring.strongbox.data.criteria.Predicate;
import org.carlspring.strongbox.data.criteria.Selector;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.event.artifact.ArtifactEventListenerRegistry;
import org.carlspring.strongbox.io.ArtifactOutputStream;
import org.carlspring.strongbox.io.RepositoryInputStream;
import org.carlspring.strongbox.io.RepositoryOutputStream;
import org.carlspring.strongbox.io.RepositoryStreamCallback;
import org.carlspring.strongbox.io.RepositoryStreamContext;
import org.carlspring.strongbox.io.StreamUtils;
import org.carlspring.strongbox.providers.datastore.StorageProviderRegistry;
import org.carlspring.strongbox.providers.layout.LayoutProviderRegistry;
import org.carlspring.strongbox.providers.repository.RepositoryProvider;
import org.carlspring.strongbox.providers.repository.RepositoryProviderRegistry;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.carlspring.strongbox.services.ArtifactTagService;
import org.carlspring.strongbox.storage.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * @author carlspring
 */
public abstract class AbstractRepositoryProvider implements RepositoryProvider, RepositoryStreamCallback
{

    private static final Logger logger = LoggerFactory.getLogger(AbstractRepositoryProvider.class);
    
    @Inject
    protected RepositoryProviderRegistry repositoryProviderRegistry;

    @Inject
    protected LayoutProviderRegistry layoutProviderRegistry;

    @Inject
    protected StorageProviderRegistry storageProviderRegistry;

    @Inject
    protected ConfigurationManager configurationManager;

    @Inject
    protected ArtifactEntryService artifactEntryService;
    
    @Inject
    protected ArtifactTagService artifactTagService;
    
    @Inject
    protected ArtifactEventListenerRegistry artifactEventListenerRegistry;
    
    @Inject
    protected RepositoryPathLock repositoryPathLock;
    
    @Inject
    private PlatformTransactionManager transactionManager;

    protected Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }
    
    @Override
    public RepositoryInputStream getInputStream(Path path)
        throws IOException
    {
        if (path == null)
        {
            return null;
        }
        Assert.isInstanceOf(RepositoryPath.class, path);
        RepositoryPath repositoryPath = (RepositoryPath) path;

        return decorate((RepositoryPath) path,
                        getInputStreamInternal(repositoryPath));

    }

    protected abstract InputStream getInputStreamInternal(RepositoryPath repositoryPath)
        throws IOException;

    protected RepositoryInputStream decorate(RepositoryPath repositoryPath,
                                             InputStream is)
    {
        if (is == null || is instanceof RepositoryInputStream)
        {
            return (RepositoryInputStream) is;
        }

        ReadWriteLock lock = repositoryPathLock.lock(repositoryPath);
        return RepositoryInputStream.of(repositoryPath, lock, transactionManager, is).with(this);
    }

    @Override
    public RepositoryOutputStream getOutputStream(Path path)
        throws IOException
    {
        Assert.isInstanceOf(RepositoryPath.class, path);
        OutputStream os = getOutputStreamInternal((RepositoryPath) path);
        
        return decorate((RepositoryPath) path, os);
    }
    
    protected abstract OutputStream getOutputStreamInternal(RepositoryPath repositoryPath)
        throws IOException;

    protected final RepositoryOutputStream decorate(RepositoryPath repositoryPath,
                                                    OutputStream os) throws IOException
    {
        if (os == null || os instanceof RepositoryOutputStream)
        {
            return (RepositoryOutputStream) os;
        }

        ReadWriteLock lock = repositoryPathLock.lock(repositoryPath);
        return RepositoryOutputStream.of(repositoryPath, lock, transactionManager, os).with(this);
    }

    @Override
    public void onBeforeWrite(RepositoryStreamContext ctx) throws IOException
    {
        RepositoryPath repositoryPath = (RepositoryPath) ctx.getPath();
        logger.debug(String.format("Writing [%s]", repositoryPath));
        
        if (!RepositoryFiles.isArtifact(repositoryPath))
        {
            return;
        }

        Repository repository = repositoryPath.getRepository();
        String storageId = repository.getStorage().getId();
        String repositoryId = repository.getId();

        ArtifactEntry artifactEntry = provideArtifactEntry(repositoryPath);
        
        if (!shouldStoreArtifactEntry(artifactEntry))
        {
            return;
        }
        
        artifactEntry.setStorageId(storageId);
        artifactEntry.setRepositoryId(repositoryId);

        ArtifactOutputStream aos = StreamUtils.findSource(ArtifactOutputStream.class, (OutputStream) ctx);
        ArtifactCoordinates coordinates = aos.getCoordinates();
        artifactEntry.setArtifactCoordinates(coordinates);

        Date now = new Date();
        artifactEntry.setCreated(now);
        artifactEntry.setLastUpdated(now);
        artifactEntry.setLastUsed(now);

        repositoryPath.artifactEntry = artifactEntry;
    }

    @Override
    public void onAfterWrite(RepositoryStreamContext ctx) throws IOException
    {
        RepositoryPath repositoryPath = (RepositoryPath) ctx.getPath();
        logger.debug(String.format("Closing [%s]", repositoryPath));
        
        if (!RepositoryFiles.isArtifact(repositoryPath))
        {
            return;
        }
        
        ArtifactEntry artifactEntry = repositoryPath.artifactEntry;
        
        if (artifactEntry == null)
        {
            artifactEventListenerRegistry.dispatchArtifactUpdatedEvent(repositoryPath);
            
            return;
        }
        
        CountingOutputStream cos = StreamUtils.findSource(CountingOutputStream.class, (OutputStream) ctx);
        long size = cos.getByteCount();
        
        artifactEntry.setSizeInBytes(size);

        storeArtifactEntry(artifactEntry);
        repositoryPath.artifactEntry = null;
        
        artifactEventListenerRegistry.dispatchArtifactStoredEvent(repositoryPath);
    }

    @Override
    public void onBeforeRead(RepositoryStreamContext ctx)
        throws IOException
    {
        RepositoryPath repositoryPath = (RepositoryPath) ctx.getPath();
        logger.debug(String.format("Reading %s", repositoryPath));

        if (!RepositoryFiles.isArtifact(repositoryPath))
        {
            return;
        }

        if (RepositoryFiles.artifactDoesNotExist(repositoryPath))
        {
            URI artifactResource = RepositoryFiles.resolveResource(repositoryPath);
            
            throw new ArtifactNotFoundException(artifactResource);
        }
        
        artifactEventListenerRegistry.dispatchArtifactDownloadingEvent(repositoryPath);
    }
    
    @Override
    public void onAfterRead(RepositoryStreamContext ctx)
    {
        
    }

    protected ArtifactEntry provideArtifactEntry(RepositoryPath repositoryPath) throws IOException
    {
        return Optional.ofNullable(repositoryPath.getArtifactEntry())
                       .orElse(new ArtifactEntry());
    }
    
    protected boolean shouldStoreArtifactEntry(ArtifactEntry artifactEntry)
    {
        return artifactEntry.getUuid() == null;
    }
    
    protected void storeArtifactEntry(ArtifactEntry artifactEntry)
    {
        artifactEntryService.save(artifactEntry, true);
    }
    
    @Override
    public RepositoryPath fetchPath(Path repositoryPath)
        throws IOException
    {
        return fetchPath((RepositoryPath)repositoryPath);
    }

    protected abstract RepositoryPath fetchPath(RepositoryPath repositoryPath) throws IOException;
    
    protected Predicate createPredicate(String storageId,
                                        String repositoryId,
                                        Predicate predicate)
    {
        Predicate result = Predicate.of(ExpOperator.EQ.of("storageId",
                                                          storageId))
                                    .and(Predicate.of(ExpOperator.EQ.of("repositoryId",
                                                                        repositoryId)));
        if (predicate.isEmpty())
        {
            return result;
        }
        return result.and(predicate);
    }

    protected Selector<ArtifactEntry> createSelector(String storageId,
                                                     String repositoryId,
                                                     Predicate p)
    {
        Selector<ArtifactEntry> selector = new Selector<>(ArtifactEntry.class);
        selector.where(createPredicate(storageId, repositoryId, p));
        
        return selector;
    }

    
}
