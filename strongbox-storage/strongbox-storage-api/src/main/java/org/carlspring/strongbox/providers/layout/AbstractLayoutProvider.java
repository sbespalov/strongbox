package org.carlspring.strongbox.providers.layout;

import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.client.ArtifactTransportException;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.io.*;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.providers.search.SearchException;
import org.carlspring.strongbox.providers.storage.StorageProvider;
import org.carlspring.strongbox.providers.storage.StorageProviderRegistry;
import org.carlspring.strongbox.repository.RepositoryFeatures;
import org.carlspring.strongbox.repository.RepositoryManagementStrategy;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.util.ArtifactFileUtils;
import org.carlspring.strongbox.util.MessageDigestUtils;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mtodorov
 */
public abstract class AbstractLayoutProvider<T extends ArtifactCoordinates,
                                             U extends RepositoryFeatures,
                                             V extends RepositoryManagementStrategy>
        implements LayoutProvider<T>
{

    private static final Logger logger = LoggerFactory.getLogger(AbstractLayoutProvider.class);

    @Inject
    protected LayoutProviderRegistry layoutProviderRegistry;
    @Inject
    protected StorageProviderRegistry storageProviderRegistry;
    @Inject
    private ConfigurationManager configurationManager;

    public LayoutProviderRegistry getLayoutProviderRegistry()
    {
        return layoutProviderRegistry;
    }

    public void setLayoutProviderRegistry(LayoutProviderRegistry layoutProviderRegistry)
    {
        this.layoutProviderRegistry = layoutProviderRegistry;
    }

    public StorageProviderRegistry getStorageProviderRegistry()
    {
        return storageProviderRegistry;
    }

    public void setStorageProviderRegistry(StorageProviderRegistry storageProviderRegistry)
    {
        this.storageProviderRegistry = storageProviderRegistry;
    }

    public ConfigurationManager getConfigurationManager()
    {
        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

    public Storage getStorage(String storageId)
    {
        return configurationManager.getConfiguration()
                                   .getStorage(storageId);
    }

    @Override
    public ArtifactInputStream getInputStream(String storageId,
                                              String repositoryId,
                                              String path)
            throws IOException,
                   NoSuchAlgorithmException
    {
        Storage storage = getConfiguration().getStorage(storageId);

        logger.debug("Checking in " + storage.getId() + ":" + repositoryId + "...");

        Repository repository = storage.getRepository(repositoryId);
        StorageProvider storageProvider = storageProviderRegistry.getProvider(repository.getImplementation());

        InputStream is;
        T artifactCoordinates = null;
        if (isArtifact(repository, path, true))
        {
            artifactCoordinates = getArtifactCoordinates(path);
            ArtifactPath artifactPath = resolve(repository, artifactCoordinates);
            is = storageProvider.getInputStreamImplementation(artifactPath);
        }
        else
        {
            RepositoryPath repositoryPath = resolve(repository);
            is = storageProvider.getInputStreamImplementation(repositoryPath, path);
        }

        logger.debug("Resolved " + path + "!");

        return decorateStream(storageId, repositoryId, path, is, artifactCoordinates);
    }

    @Override
    public ArtifactOutputStream getOutputStream(String storageId,
                                                String repositoryId,
                                                String path)
            throws IOException,
                   NoSuchAlgorithmException
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);
        StorageProvider storageProvider = storageProviderRegistry.getProvider(repository.getImplementation());

        OutputStream os;
        T artifactCoordinates = null;
        if (isArtifact(repository, path, false))
        {
            artifactCoordinates = getArtifactCoordinates(path);
            ArtifactPath artifactPath = resolve(repository, artifactCoordinates);
            os = storageProvider.getOutputStreamImplementation(artifactPath);
        }
        else
        {
            RepositoryPath repositoryPath = resolve(repository);
            os = storageProvider.getOutputStreamImplementation(repositoryPath, path);
        }

        return decorateStream(path, os, artifactCoordinates);
    }

    @Override
    public boolean isExistChecksum(Repository repository,
                                   String path)
    {
        return getDigestAlgorithmSet()
                       .stream()
                       .map(algorithm ->
                            {
                                String checksumPath = path.concat(".")
                                                          .concat(algorithm.toLowerCase()
                                                                           .replaceAll(
                                                                                   "-",
                                                                                   ""));
                                RepositoryPath checksum = null;
                                try
                                {
                                    checksum = resolve(repository, checksumPath);
                                }
                                catch (IOException e)
                                {
                                    logger.error(e.getMessage());
                                }
                                return checksum;
                            })
                       .allMatch(checksum -> Files.exists(checksum));

    }

    protected ArtifactOutputStream decorateStream(String path,
                                                  OutputStream os,
                                                  T artifactCoordinates)
            throws NoSuchAlgorithmException
    {
        ArtifactOutputStream result = new ArtifactOutputStream(os, artifactCoordinates);
        // Add digest algorithm only if it is not a Checksum (we don't need a Checksum of Checksum).
        if (!ArtifactFileUtils.isChecksum(path))
        {
            getDigestAlgorithmSet().stream()
                                   .forEach(e ->
                                            {
                                                try
                                                {
                                                    result.addAlgorithm(e);
                                                }
                                                catch (NoSuchAlgorithmException t)
                                                {
                                                    logger.error(
                                                            String.format("Digest algorithm not supported: alg-[%s]",
                                                                          e), t);
                                                }
                                            });
        }
        return result;
    }

    protected ArtifactInputStream decorateStream(String storageId,
                                                 String repositoryId,
                                                 String path,
                                                 InputStream is,
                                                 T artifactCoordinates)
        throws NoSuchAlgorithmException
    {
        ArtifactInputStream result = new ArtifactInputStream(artifactCoordinates, is, getDigestAlgorithmSet());
        // Add digest algorithm only if it is not a Checksum (we don't need a Checksum of Checksum).
        if (!ArtifactFileUtils.isChecksum(path))
        {
            getDigestAlgorithmSet().stream()
                                   .forEach(a -> {
                                       String checksum = getChecksum(storageId, repositoryId, path, result, a);
                                       if (checksum == null)
                                       {
                                           return;
                                       }
                                       result.getHexDigests()
                                             .put(a, checksum);
                                   });
        }
        return result;
    }

    private String getChecksum(String storageId,
                               String repositoryId,
                               String path,
                               ArtifactInputStream is,
                               String digestAlgorithm)
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);

        String checksumExtension = ".".concat(digestAlgorithm.toLowerCase()
                                                             .replaceAll("-", ""));
        String checksumPath = path.concat(checksumExtension);
        String checksum = null;

            try
            {
                if (Files.exists(resolve(repository, checksumPath)) && new File(checksumPath).length() != 0)
                {
                checksum = MessageDigestUtils.readChecksumFile(getInputStream(storageId, repositoryId, checksumPath));
                }
                else
                {
                    checksum = is.getMessageDigestAsHexadecimalString(digestAlgorithm);
                }
            }
            catch (IOException | NoSuchAlgorithmException e)
            {
                logger.error(String.format("Failed to read checksum: alg-[%s]; path-[%s];",
                                           digestAlgorithm, path + "." + checksumExtension), e);
            }

        return checksum;
    }

    public Set<String> getDigestAlgorithmSet()
    {
        return Stream.of(MessageDigestAlgorithms.MD5, MessageDigestAlgorithms.SHA_1)
                     .collect(Collectors.toSet());
    }

    protected abstract boolean isMetadata(String path);

    protected boolean isChecksum(String path)
    {
        return ArtifactFileUtils.isChecksum(path);
    }

    protected boolean isTrash(String path)
    {
        return path.contains(".trash");
    }

    protected boolean isTemp(String path)
    {
        return path.contains(".temp");
    }

    protected boolean isIndex(String path)
    {
        return path.contains(".index");
    }

    protected boolean isArtifact(Repository repository,
                                 String path,
                                 boolean strict)
            throws IOException
    {
        RepositoryPath artifactPath = resolve(repository, path);
        boolean exists = Files.exists(artifactPath);
        if (!exists && strict)
        {
            throw new FileNotFoundException(artifactPath.toString());
        }
        if (exists && Files.isDirectory(artifactPath))
        {
            throw new FileNotFoundException(String.format("The artifact path is a directory: [%s]",
                                                          artifactPath.toString()));
        }

        return !isMetadata(path) && !isChecksum(path) && !isServiceFolder(path);
    }

    protected boolean isServiceFolder(String path)
    {
        return isTemp(path) || isTrash(path) || isIndex(path);
    }

    protected RepositoryPath resolve(Repository repository)
            throws IOException
    {
        StorageProvider storageProvider = storageProviderRegistry.getProvider(repository.getImplementation());

        return storageProvider.resolve(repository);
    }

    protected RepositoryPath resolve(Repository repository,
                                     String path)
            throws IOException
    {
        StorageProvider storageProvider = storageProviderRegistry.getProvider(repository.getImplementation());

        return storageProvider.resolve(repository, path);
    }

    protected ArtifactPath resolve(String storageId,
                                   String repositoryId,
                                   String path)
            throws IOException
    {
        return resolve(storageId, repositoryId, getArtifactCoordinates(path));
    }

    protected ArtifactPath resolve(Repository repository,
                                   ArtifactCoordinates coordinates)
            throws IOException
    {
        StorageProvider storageProvider = storageProviderRegistry.getProvider(repository.getImplementation());

        return storageProvider.resolve(repository, coordinates);
    }

    protected ArtifactPath resolve(String storageId,
                                   String repositoryId,
                                   ArtifactCoordinates coordinates)
            throws IOException
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);

        return resolve(repository, coordinates);
    }

    protected RepositoryFileSystemProvider getProvider(RepositoryPath artifactPath)
    {
        return (RepositoryFileSystemProvider) artifactPath.getFileSystem()
                                                          .provider();
    }

    @Override
    public void copy(String srcStorageId,
                     String srcRepositoryId,
                     String destStorageId,
                     String destRepositoryId,
                     String path)
            throws IOException
    {
        // TODO: Implement
    }

    @Override
    public void move(String srcStorageId,
                     String srcRepositoryId,
                     String destStorageId,
                     String destRepositoryId,
                     String path)
            throws IOException
    {
        // TODO: Implement
    }

    @Override
    public void delete(String storageId,
                       String repositoryId,
                       String path,
                       boolean force)
            throws IOException, SearchException
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);

        RepositoryPath repositoryBasePath = resolve(repository);
        RepositoryPath repositoryPath = repositoryBasePath.resolve(path);

        logger.debug("Checking in " + storageId + ":" + repositoryId + "(" + path + ")...");
        if (!Files.exists(repositoryPath))
        {
            logger.warn(String.format("Path not found: path-[%s]", repositoryPath));
            return;
        }

        if (!Files.isDirectory(repositoryPath))
        {
            doDeletePath(repositoryPath, force, true);
        }
        else
        {
            Files.walkFileTree(repositoryPath, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs)
                        throws IOException
                {
                    doDeletePath((RepositoryPath) file, force, false);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException exc)
                        throws IOException
                {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        logger.debug("Removed /" + repositoryId + "/" + path);
    }

    protected void doDeletePath(RepositoryPath repositoryPath,
                                boolean force,
                                boolean deleteChecksum)
            throws IOException
    {
        Files.delete(repositoryPath);

        Repository repository = repositoryPath.getFileSystem().getRepository();
        RepositoryFileSystemProvider provider = getProvider(repositoryPath);
        if (force && repository.allowsForceDeletion())
        {
            provider.deleteTrash(repositoryPath);
        }
    }

    @Override
    public void deleteTrash(String storageId,
                            String repositoryId)
            throws IOException
    {
        logger.debug("Emptying trash for repositoryId " + repositoryId + "...");

        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);
        RepositoryPath path = resolve(repository);
        RepositoryFileSystemProvider provider = (RepositoryFileSystemProvider) path.getFileSystem()
                                                                                   .provider();

        provider.deleteTrash(path);
    }

    @Override
    public void deleteTrash()
            throws IOException
    {
        for (Map.Entry entry : getConfiguration().getStorages()
                                                 .entrySet())
        {
            Storage storage = (Storage) entry.getValue();

            final Map<String, Repository> repositories = storage.getRepositories();
            for (Repository repository : repositories.values())
            {
                if (!repository.allowsDeletion())
                {
                    logger.warn("Repository " + repository.getId() + " does not support removal of trash.");
                }
                deleteTrash(storage.getId(), repository.getId());
            }
        }
    }

    @Override
    public void undelete(String storageId,
                         String repositoryId,
                         String path)
            throws IOException
    {
        logger.debug(String.format("Attempting to restore: storageId-[%s]; repoId-[%s]; path-[%s]; ",
                                   storageId,
                                   repositoryId,
                                   path));

        ArtifactPath artifactPath = resolve(storageId, repositoryId, path);

        RepositoryFileSystemProvider provider = getProvider(artifactPath);
        provider.restoreTrash(artifactPath);
    }

    @Override
    public void undeleteTrash(String storageId,
                              String repositoryId)
            throws IOException
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);

        logger.debug("Restoring all artifacts from the trash of " + storageId + ":" + repository.getId() + "...");

        if (!repository.isTrashEnabled())
        {
            logger.warn("Repository " + repository.getId() + " does not support removal of trash.");
        }

        RepositoryPath path = resolve(repository);
        getProvider(path).restoreTrash(path);
    }

    @Override
    public void undeleteTrash()
            throws IOException
    {
        for (Map.Entry entry : getConfiguration().getStorages()
                                                 .entrySet())
        {
            Storage storage = (Storage) entry.getValue();

            final Map<String, Repository> repositories = storage.getRepositories();
            for (Repository repository : repositories.values())
            {
                undeleteTrash(storage.getId(), repository.getId());
            }
        }
    }

    @Override
    public boolean contains(String storageId,
                            String repositoryId,
                            String path)
            throws IOException
    {
        ArtifactPath artifactPath = resolve(storageId, repositoryId, path);
        return Files.exists(artifactPath);
    }

    @Override
    public boolean containsArtifact(Repository repository,
                                    ArtifactCoordinates coordinates)
            throws IOException
    {
        ArtifactPath artifactPath = resolve(repository, coordinates);
        return Files.exists(artifactPath);
    }

    @Override
    public boolean containsPath(Repository repository,
                                String path)
            throws IOException
    {
        RepositoryPath repositoryPath = resolve(repository);

        return Files.exists(repositoryPath.resolve(path));
    }

    protected void storeChecksum(Repository repository,
                                 RepositoryPath basePath,
                                 boolean forceRegeneration)
            throws IOException,
                   NoSuchAlgorithmException,
                   ArtifactTransportException,
                   ProviderImplementationException

    {
        File file = basePath.toFile();

        List<File> list = Arrays.asList(file.listFiles());

        list.stream()
            .filter(File::isFile)
            .filter(e -> !ArtifactFileUtils.isChecksum(e.getPath()))
            .forEach(e ->
                     {
                         if (!isExistChecksum(repository, e.getPath()) || forceRegeneration)
                         {
                             ArtifactInputStream is = null;
                             try
                             {
                                 String artifactPath = e.getPath().substring(repository.getBasedir().length() + 1);
                                 is = getInputStream(repository.getStorage().getId(), repository.getId(), artifactPath);
                             }
                             catch (IOException | NoSuchAlgorithmException e1)
                             {
                                 logger.error(e1.getMessage(), e1);
                             }

                             writeChecksum(is, e);
                         }
                     });
    }

    private void writeChecksum(ArtifactInputStream is,
                               File filePath)

    {
        getDigestAlgorithmSet()
                .stream()
                .forEach(e ->
                         {
                             String checksum = is.getHexDigests().get(e);
                             String checksumExtension = ".".concat(e.toLowerCase().replaceAll("-", ""));

                             try
                             {
                                 MessageDigestUtils.writeChecksum(filePath, checksumExtension, checksum);
                             }
                             catch (IOException e1)
                             {
                                 logger.error(String.format("Failed to write checksum: alg-[%s]; path-[%s];",
                                                            e, filePath + "." + checksumExtension), e1);
                             }
                         });
    }

}
