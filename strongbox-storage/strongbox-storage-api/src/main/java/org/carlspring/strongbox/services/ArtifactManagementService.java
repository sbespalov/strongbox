package org.carlspring.strongbox.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.domain.ArtifactEntryRead;
import org.carlspring.strongbox.event.artifact.ArtifactEventListenerRegistry;
import org.carlspring.strongbox.io.ArtifactOutputStream;
import org.carlspring.strongbox.io.StreamUtils;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RepositoryPathLock;
import org.carlspring.strongbox.providers.io.RepositoryPathResolver;
import org.carlspring.strongbox.providers.io.RepositoryStreamSupport.RepositoryOutputStream;
import org.carlspring.strongbox.providers.layout.LayoutProviderRegistry;
import org.carlspring.strongbox.storage.ArtifactStorageException;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.checksum.ArtifactChecksum;
import org.carlspring.strongbox.storage.checksum.ChecksumCacheManager;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.validation.ArtifactCoordinatesValidator;
import org.carlspring.strongbox.storage.validation.artifact.ArtifactCoordinatesValidationException;
import org.carlspring.strongbox.storage.validation.artifact.ArtifactCoordinatesValidatorRegistry;
import org.carlspring.strongbox.storage.validation.artifact.version.VersionValidationException;
import org.carlspring.strongbox.storage.validation.resource.ArtifactOperationsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

/**
 * @author mtodorov
 */
@Component
public class ArtifactManagementService
{
    private static final Logger logger = LoggerFactory.getLogger(ArtifactManagementService.class);

    @Inject
    protected ArtifactOperationsValidator artifactOperationsValidator;

    @Inject
    protected ArtifactCoordinatesValidatorRegistry artifactCoordinatesValidatorRegistry;

    @Inject
    protected ConfigurationManager configurationManager;

    @Inject
    protected ArtifactEntryService artifactEntryService;

    @Inject
    protected LayoutProviderRegistry layoutProviderRegistry;

    @Inject
    protected ArtifactResolutionService artifactResolutionService;

    @Inject
    protected ChecksumCacheManager checksumCacheManager;

    @Inject
    protected ArtifactEventListenerRegistry artifactEventListenerRegistry;

    @Inject
    protected RepositoryPathResolver repositoryPathResolver;
    
    @Inject
    protected RepositoryPathLock repositoryPathLock;

    @Transactional
    public long validateAndStore(RepositoryPath repositoryPath,
                                 InputStream is)
        throws IOException,
        ProviderImplementationException,
        NoSuchAlgorithmException,
        ArtifactCoordinatesValidationException
    {
        ReadWriteLock lock = repositoryPathLock.lock(repositoryPath);
        lock.writeLock().lock();

        try
        {
            performRepositoryAcceptanceValidation(repositoryPath);

            return doStore(repositoryPath, is);
        } 
        finally
        {
            lock.writeLock().unlock();
        }
    }
    
    @Deprecated
    @Transactional
    public long validateAndStore(String storageId,
                                 String repositoryId,
                                 String path,
                                 InputStream is)
            throws IOException,
                   ProviderImplementationException,
                   NoSuchAlgorithmException,
                   ArtifactCoordinatesValidationException
    {
        RepositoryPath repositoryPath = repositoryPathResolver.resolve(storageId, repositoryId, path);

        return validateAndStore(repositoryPath, is);
    }
    
    @Transactional
    public long store(RepositoryPath repositoryPath,
                      InputStream is)
        throws IOException
    {
        ReadWriteLock lockSource = repositoryPathLock.lock(repositoryPath);
        Lock lock = lockSource.writeLock();
        lock.lock();
        
        try
        {
            return doStore(repositoryPath, is);
        } 
        finally
        {
            lock.unlock();
        }
    }

    private long doStore(RepositoryPath repositoryPath,
                         InputStream is)
            throws IOException
    {
        long result;
        boolean updatedMetadataFile = false;
        boolean updatedArtifactFile = false;

        if (Files.exists(repositoryPath))
        {
            updatedMetadataFile = RepositoryFiles.isMetadata(repositoryPath);
            updatedArtifactFile = RepositoryFiles.isArtifact(repositoryPath);
        }
        
        try (final RepositoryOutputStream aos = artifactResolutionService.getOutputStream(repositoryPath))
        {
            result = writeArtifact(repositoryPath, is, aos);
        }
        catch (IOException e)
        {
           throw e; 
        }
        catch (Exception e)
        {
            throw new ArtifactStorageException(e);
        }

        if (updatedArtifactFile)
        {
            artifactEventListenerRegistry.dispatchArtifactUpdatedEvent(repositoryPath);
        }
        else
        {
            artifactEventListenerRegistry.dispatchArtifactStoredEvent(repositoryPath);
        }
        
        if (updatedMetadataFile)
        {
            artifactEventListenerRegistry.dispatchArtifactMetadataFileUpdatedEvent(repositoryPath);
            // If this is a metadata file and it has been updated:
        }

        
        return result;
    }

    private long writeArtifact(RepositoryPath repositoryPath,
                               InputStream is,
                               OutputStream os)
            throws IOException
    {
        ArtifactOutputStream aos = StreamUtils.findSource(ArtifactOutputStream.class, os);

        Repository repository = repositoryPath.getRepository();

        Boolean checksumAttribute = RepositoryFiles.isChecksum(repositoryPath);

        // If we have no digests, then we have a checksum to store.
        if (Boolean.TRUE.equals(checksumAttribute))
        {
            aos.setCacheOutputStream(new ByteArrayOutputStream());
        }

        if (repository.isHostedRepository())
        {
            artifactEventListenerRegistry.dispatchArtifactUploadingEvent(repositoryPath);
        }
        
        long totalAmountOfBytes = IOUtils.copy(is, os);

        URI repositoryPathId = repositoryPath.toUri();
        Map<String, String> digestMap = aos.getDigestMap();
        if (Boolean.FALSE.equals(checksumAttribute) && !digestMap.isEmpty())
        {
            // Store artifact digests in cache if we have them.
            addChecksumsToCacheManager(digestMap, repositoryPathId);
        }

        if (Boolean.TRUE.equals(checksumAttribute))
        {
            byte[] checksumValue = ((ByteArrayOutputStream) aos.getCacheOutputStream()).toByteArray();
            if (checksumValue != null && checksumValue.length > 0)
            {
                // Validate checksum with artifact digest cache.
                validateUploadedChecksumAgainstCache(checksumValue, repositoryPathId);
            }
        }
        
        return totalAmountOfBytes;
    }

    private void validateUploadedChecksumAgainstCache(byte[] checksum,
                                                      URI artifactPathId)
    {
        logger.debug("Received checksum: " + new String(checksum, StandardCharsets.UTF_8));

        String artifactPath = artifactPathId.toString();
        String artifactBasePath = artifactPath.substring(0, artifactPath.lastIndexOf('.'));
        String checksumExtension = artifactPath.substring(artifactPath.lastIndexOf('.') + 1, artifactPath.length());

        if (!matchesChecksum(checksum, artifactBasePath, checksumExtension))
        {
            logger.error(String.format("The checksum for %s [%s] is invalid!",
                                       artifactPath,
                                       new String(checksum, StandardCharsets.UTF_8)));
        }

        checksumCacheManager.removeArtifactChecksum(artifactBasePath, checksumExtension);
    }

    private boolean matchesChecksum(byte[] pChecksum,
                                    String artifactBasePath,
                                    String checksumExtension)
    {
        String checksum = new String(pChecksum, StandardCharsets.UTF_8);
        ArtifactChecksum artifactChecksum = checksumCacheManager.getArtifactChecksum(artifactBasePath);

        if (artifactChecksum == null)
        {
            return false;
        }

        Map<Boolean, Set<String>> matchingMap = artifactChecksum.getChecksums()
                                                                .entrySet()
                                                                .stream()
                                                                .collect(Collectors.groupingBy(e -> e.getValue()
                                                                                                     .equals(checksum),
                                                                                               Collectors.mapping(
                                                                                                       e -> e.getKey(),
                                                                                                       Collectors.toSet())));

        Set<String> matched = matchingMap.get(Boolean.TRUE);
        Set<String> unmatched = matchingMap.get(Boolean.FALSE);

        logger.debug(String.format("Artifact checksum matchings: artifact-[%s]; ext-[%s]; matched-[%s];" +
                                   " unmatched-[%s]; checksum-[%s]",
                                   artifactBasePath,
                                   checksumExtension,
                                   matched,
                                   unmatched,
                                   checksum));

        return matched != null && !matched.isEmpty();
    }

    private void addChecksumsToCacheManager(Map<String, String> digestMap,
                                            URI artifactPath)
    {
        digestMap.entrySet()
                 .stream()
                 .forEach(e -> checksumCacheManager.addArtifactChecksum(artifactPath.toString(), e.getKey(), e.getValue()));
    }

    private boolean performRepositoryAcceptanceValidation(RepositoryPath path)
            throws IOException, ProviderImplementationException, ArtifactCoordinatesValidationException
    {
        logger.info(String.format("Validate artifact with path [%s]", path));

        Repository repository = path.getFileSystem().getRepository();

        artifactOperationsValidator.validate(path);

        if (!RepositoryFiles.isArtifact(path))
        {
            return true;
        }
        
        ArtifactCoordinates coordinates = RepositoryFiles.readCoordinates(path);
        logger.info(String.format("Validate artifact with coordinates [%s]", coordinates));

        try
        {
            for (String validatorKey : repository.getArtifactCoordinateValidators().keySet())
            {
                ArtifactCoordinatesValidator validator = artifactCoordinatesValidatorRegistry.getProvider(
                        validatorKey);
                if (validator.supports(repository))
                {
                    validator.validate(repository, coordinates);
                }
            }
        }
        catch (VersionValidationException e)
        {
            throw new ArtifactStorageException(e);
        }

        artifactOperationsValidator.checkAllowsRedeployment(repository, coordinates);
        artifactOperationsValidator.checkAllowsDeployment(repository);

        return true;
    }

    protected Storage getStorage(String storageId)
    {
        return getConfiguration().getStorages().get(storageId);
    }

    protected Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

    @Transactional
    public void delete(RepositoryPath repositoryPath,
                       boolean force)
            throws IOException
    {
        artifactOperationsValidator.validate(repositoryPath);

        final Repository repository = repositoryPath.getRepository();

        artifactOperationsValidator.checkAllowsDeletion(repository);

        if (!Files.isDirectory(repositoryPath) && RepositoryFiles.artifactDoesNotExist(repositoryPath))
        {
            throw new IOException(String.format("Corresponding [%s] record not found for path [%s]",
                                                ArtifactEntry.class.getSimpleName(), repositoryPath));
        }

        try
        {           
            RepositoryFiles.delete(repositoryPath, force);
        }
        catch (IOException e)
        {
            throw new ArtifactStorageException(e.getMessage(), e);
        }
    }

    public void copy(RepositoryPath srcPath, RepositoryPath destPath)
            throws IOException
    {
        artifactOperationsValidator.validate(srcPath);

        if (Files.isDirectory(srcPath))
        {
            FileSystemUtils.copyRecursively(srcPath, destPath);
        }
        else
        {
            Files.copy(srcPath, destPath);
        }
    }

}
