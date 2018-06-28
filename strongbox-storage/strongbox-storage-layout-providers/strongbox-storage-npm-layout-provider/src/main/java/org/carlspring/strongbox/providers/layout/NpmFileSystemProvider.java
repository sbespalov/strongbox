package org.carlspring.strongbox.providers.layout;

import java.nio.file.spi.FileSystemProvider;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 *
 */
public class NpmFileSystemProvider extends RepositoryLayoutFileSystemProvider
{

    private static final Logger logger = LoggerFactory.getLogger(NpmFileSystemProvider.class);

    @Inject
    private NpmLayoutProvider layoutProvider;

    public NpmFileSystemProvider(FileSystemProvider storageFileSystemProvider)
    {
        super(storageFileSystemProvider);
    }

    @Override
    protected AbstractLayoutProvider getLayoutProvider()
    {
        return layoutProvider;
    }

}
