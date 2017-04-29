package org.carlspring.strongbox.services.impl;

import org.carlspring.strongbox.artifact.coordinates.ArtifactCoordinates;
import org.carlspring.strongbox.artifact.locator.ArtifactDirectoryLocator;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
<<<<<<< Upstream, based on upstream/SB-787
import org.carlspring.strongbox.io.RepositoryPath;
=======
import org.carlspring.strongbox.io.ArtifactPath;
>>>>>>> f5a9d3c SB-761: `RepositoryPath` related refactoring
import org.carlspring.strongbox.locator.handlers.MavenIndexerManagementOperation;
import org.carlspring.strongbox.providers.layout.LayoutProvider;
import org.carlspring.strongbox.providers.layout.LayoutProviderRegistry;
import org.carlspring.strongbox.providers.storage.StorageProvider;
import org.carlspring.strongbox.providers.storage.StorageProviderRegistry;
import org.carlspring.strongbox.repository.MavenRepositoryFeatures;
import org.carlspring.strongbox.services.ArtifactIndexesService;
import org.carlspring.strongbox.services.RepositoryManagementService;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.indexing.RepositoryIndexManager;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Kate Novik.
 */
@Component("artifactIndexesService")
public class ArtifactIndexesServiceImpl
        implements ArtifactIndexesService
{

    private static final Logger logger = LoggerFactory.getLogger(ArtifactIndexesServiceImpl.class);

    @Inject
    private ConfigurationManager configurationManager;
    @Inject
    private RepositoryIndexManager repositoryIndexManager;
    @Inject
    private RepositoryManagementService repositoryManagementService;
    @Inject
    private LayoutProviderRegistry layoutProviderRegistry;
<<<<<<< Upstream, based on upstream/SB-787

=======
>>>>>>> f5a9d3c SB-761: `RepositoryPath` related refactoring
    @Inject
    private StorageProviderRegistry storageProviderRegistry;

    @Override
    public void rebuildIndex(String storageId,
                             String repositoryId,
                             String artifactPath)
            throws IOException
    {
        Storage storage = getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);

        LayoutProvider layoutProvider = layoutProviderRegistry.getProvider(repository.getLayout());
        StorageProvider storageProvider = storageProviderRegistry.getProvider(repository.getImplementation());
        
        ArtifactCoordinates artifactCoordinates = layoutProvider.getArtifactCoordinates(artifactPath);
        ArtifactPath repositoryArtifactPath = storageProvider.resolve(repository, artifactCoordinates);
        
        artifactPath = artifactPath == null ? "/" : artifactPath;

        if (!repository.isIndexingEnabled())
        {
<<<<<<< Upstream, based on upstream/SB-787
            return;
=======
            MavenIndexerManagementOperation operation = new MavenIndexerManagementOperation(repositoryIndexManager);

            operation.setStorage(storage);
            //noinspection ConstantConditions
            operation.setBasePath(repositoryArtifactPath);

            ArtifactDirectoryLocator locator = new ArtifactDirectoryLocator();
            locator.setOperation(operation);
            locator.locateArtifactDirectories();

            MavenRepositoryFeatures features = (MavenRepositoryFeatures) layoutProvider.getRepositoryFeatures();

            features.pack(storageId, repositoryId);
>>>>>>> f5a9d3c SB-761: `RepositoryPath` related refactoring
        }
        LayoutProvider layoutProvider = layoutProviderRegistry.getProvider(repository.getLayout());
        StorageProvider storageProvider = storageProviderRegistry.getProvider(repository.getImplementation());
        RepositoryPath repostitoryPath = storageProvider.resolve(repository, artifactPath);
        
        MavenIndexerManagementOperation operation = new MavenIndexerManagementOperation(repositoryIndexManager);

        operation.setStorage(storage);
        operation.setRepository(repository);
        //noinspection ConstantConditions
        
        String basePath = Files.isDirectory(repostitoryPath) ? artifactPath
                : repostitoryPath.getParent().getRepositoryRelative().toString();
        operation.setBasePath(basePath);

        ArtifactDirectoryLocator locator = new ArtifactDirectoryLocator();
        locator.setOperation(operation);
        locator.locateArtifactDirectories();

        MavenRepositoryFeatures features = (MavenRepositoryFeatures) layoutProvider.getRepositoryFeatures();

        features.pack(storageId, repositoryId);
    }

    @Override
    public void rebuildIndexes(String storageId)
            throws IOException
    {
        Map<String, Repository> repositories = getRepositories(storageId);

        logger.debug("Rebuilding indexes for repositories " + repositories.keySet());

        for (String repository : repositories.keySet())
        {
            rebuildIndex(storageId, repository, null);
        }
    }

    @Override
    public void rebuildIndexes()
            throws IOException
    {
        Map<String, Storage> storages = getStorages();
        for (String storageId : storages.keySet())
        {
            rebuildIndexes(storageId);
        }
    }

    private Configuration getConfiguration()
    {
        return configurationManager.getConfiguration();
    }

    private Map<String, Storage> getStorages()
    {
        return getConfiguration().getStorages();
    }

    private Map<String, Repository> getRepositories(String storageId)
    {
        return getStorages().get(storageId).getRepositories();
    }

}
