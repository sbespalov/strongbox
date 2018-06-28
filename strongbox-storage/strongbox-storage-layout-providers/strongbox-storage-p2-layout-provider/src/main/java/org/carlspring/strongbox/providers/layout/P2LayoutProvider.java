package org.carlspring.strongbox.providers.layout;


import org.carlspring.strongbox.artifact.coordinates.P2ArtifactCoordinates;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.repository.P2RepositoryFeatures;
import org.carlspring.strongbox.repository.P2RepositoryManagementStrategy;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2LayoutProvider
        extends AbstractLayoutProvider<P2ArtifactCoordinates>
{

    private static final Logger logger = LoggerFactory.getLogger(P2LayoutProvider.class);

    public static final String ALIAS = "P2 Repository";

    @Inject
    private LayoutProviderRegistry layoutProviderRegistry;

    @Inject
    private P2RepositoryManagementStrategy p2RepositoryManagementStrategy;

    @Inject
    private P2RepositoryFeatures p2RepositoryFeatures;

    public void register()
    {
        layoutProviderRegistry.addProvider(ALIAS, this);

        logger.info("Registered layout provider '" + getClass().getCanonicalName() + "' with alias '" + ALIAS + "'.");
    }

    protected P2ArtifactCoordinates getArtifactCoordinates(RepositoryPath path) throws IOException
    {
        return P2ArtifactCoordinates.create(RepositoryFiles.relativizePath(path));
    }

    public boolean isArtifactMetadata(RepositoryPath path)
    {
        String fileName = path.getFileName().toString();
        
        return "content.xml".equals(fileName) || "artifacts.xml".equals(fileName) || "artifacts.jar".equals(fileName) ||
                "content.jar".equals(fileName);
    }

    @Override
    public void deleteMetadata(RepositoryPath repositoryPath)
    {

    }

    @Override
    public Set<String> getDefaultArtifactCoordinateValidators()
    {
        return p2RepositoryFeatures.getDefaultArtifactCoordinateValidators();
    }

    @Override
    public P2RepositoryManagementStrategy getRepositoryManagementStrategy()
    {
        return p2RepositoryManagementStrategy;
    }

}
