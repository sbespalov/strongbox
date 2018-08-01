package org.carlspring.strongbox.io;

import java.io.IOException;

public interface RepositoryStreamCallback
{

    void onBeforeRead(RepositoryStreamReadContext ctx)
        throws IOException;

    void onBeforeWrite(RepositoryStreamWriteContext ctx)
        throws IOException;

    void onAfterWrite(RepositoryStreamWriteContext ctx)
        throws IOException;

    void onAfterRead(RepositoryStreamReadContext ctx);

}
