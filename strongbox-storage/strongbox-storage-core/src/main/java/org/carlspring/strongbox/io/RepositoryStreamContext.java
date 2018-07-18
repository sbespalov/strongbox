package org.carlspring.strongbox.io;

import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;

public interface RepositoryStreamContext
{

    ReadWriteLock getLock();
    
    Path getPath();
    
}
