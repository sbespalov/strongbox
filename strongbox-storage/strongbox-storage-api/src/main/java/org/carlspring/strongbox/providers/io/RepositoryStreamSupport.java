package org.carlspring.strongbox.providers.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.carlspring.strongbox.io.RepositoryStreamCallback;
import org.carlspring.strongbox.io.RepositoryStreamContext;
import org.carlspring.strongbox.io.RepositoryStreamReadContext;
import org.carlspring.strongbox.io.RepositoryStreamWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sbespalov
 *
 */
public abstract class RepositoryStreamSupport implements RepositoryStreamCallback
{
    private static final Logger logger = LoggerFactory.getLogger(RepositoryStreamSupport.class);

    @Inject
    private RepositoryPathLock repositoryPathLock;

    private ThreadLocal<RepositoryStreamContext> ctx = new ThreadLocal<RepositoryStreamContext>();

    protected void initContext(RepositoryStreamContext ctx)
    {
        this.ctx.set(ctx);
    }

    protected RepositoryStreamContext getContext()
    {
        return ctx.get();
    }

    private void clearContext()
    {
        ctx.remove();
    }

    private void open()
        throws IOException
    {
        RepositoryStreamContext ctx = getContext();
        if (ctx.isOpened())
        {
            return;
        }

        RepositoryPath path = (RepositoryPath) ctx.getPath();
        ReadWriteLock lockSource = repositoryPathLock.lock(path);
        Lock lock;
        if (ctx instanceof RepositoryStreamWriteContext)
        {
            // TODO: write lock currently managed within
            // ArtifactManagementService, but we should think to manage it here,
            // as for InputStream.
            lock = null;
        }
        else
        {
            lock = lockSource.readLock();
        }

        ctx.setLock(lock);
        Optional.ofNullable(lock).ifPresent(l -> l.lock());

        doOpen(ctx);
    }

    private void doOpen(RepositoryStreamContext ctx)
        throws IOException
    {
        ctx.setOpened(true);

        if (ctx instanceof RepositoryStreamWriteContext)
        {
            onBeforeWrite((RepositoryStreamWriteContext) ctx);
        }
        else
        {
            onBeforeRead((RepositoryStreamReadContext) ctx);
        }
    }

    public void close()
        throws IOException
    {
        RepositoryStreamContext ctx = getContext();
        if (!ctx.isOpened())
        {
            return;
        }

        try
        {
            doClose(ctx);
        } 
        finally
        {
            Optional.ofNullable(ctx.getLock()).ifPresent(l -> l.unlock());
            clearContext();
        }
    }

    private void doClose(RepositoryStreamContext ctx)
        throws IOException
    {
        if (ctx instanceof RepositoryStreamWriteContext)
        {
            onAfterWrite((RepositoryStreamWriteContext) ctx);
        }
        else
        {
            onAfterRead((RepositoryStreamReadContext) ctx);
        }
    }

    public class RepositoryOutputStream extends CountingOutputStream
    {
        protected RepositoryOutputStream(Path path,
                                         OutputStream out)
        {
            super(out);

            RepositoryStreamWriteContext ctx = new RepositoryStreamWriteContext();
            ctx.setStream(this);
            ctx.setPath(path);

            initContext(ctx);
        }

        @Override
        public void write(byte[] bts)
            throws IOException
        {
            open();

            super.write(bts);
        }

        @Override
        public void write(int idx)
            throws IOException
        {
            open();

            super.write(idx);
        }

        @Override
        public void write(byte[] bts,
                          int st,
                          int end)
            throws IOException
        {
            open();

            super.write(bts, st, end);
        }

        @Override
        public void close()
            throws IOException
        {
            try
            {
                super.close();
            } 
            finally
            {
                RepositoryStreamSupport.this.close();
            }
        }

    }

    public class RepositoryInputStream
            extends CountingInputStream
    {

        protected RepositoryInputStream(Path path,
                                        InputStream in)
        {
            super(in);

            RepositoryStreamReadContext ctx = new RepositoryStreamReadContext();
            ctx.setPath(path);
            ctx.setStream(this);

            initContext(ctx);
        }

        @Override
        protected void beforeRead(int n)
            throws IOException
        {
            open();
        }

        @Override
        public void close()
            throws IOException
        {
            try
            {
                super.close();
            } 
            finally
            {
                RepositoryStreamSupport.this.close();
            }
        }

    }

}
