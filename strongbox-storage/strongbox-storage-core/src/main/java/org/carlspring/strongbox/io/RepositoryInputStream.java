package org.carlspring.strongbox.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

public class RepositoryInputStream
        extends BufferedInputStream
        implements RepositoryStreamContext
{

    private static final Logger logger = LoggerFactory.getLogger(RepositoryInputStream.class);

    protected RepositoryStreamCallback callback = new EmptyRepositoryStreamCallback();

    private Path path;

    private ReadWriteLock lock;

    private PlatformTransactionManager transactionManager;

    private TransactionStatus transactionStatus;

    protected RepositoryInputStream(Path path,
                                    ReadWriteLock lock,
                                    PlatformTransactionManager transactionManager,
                                    InputStream in)
    {
        super(new CountingInputStream(in));

        this.path = path;
        this.lock = lock;
        this.transactionManager = transactionManager;
    }

    @Override
    public ReadWriteLock getLock()
    {
        return lock;
    }

    public Path getPath()
    {
        return path;
    }

    public RepositoryInputStream with(RepositoryStreamCallback callback)
    {
        this.callback = callback;
        return this;
    }

    @Override
    public synchronized int read(byte[] b,
                                 int off,
                                 int len)
        throws IOException
    {
        open();
        return super.read(b, off, len);
    }

    @Override
    public int read()
        throws IOException
    {
        open();
        return super.read();
    }

    private void open()
        throws IOException
    {
        if (getBytesCount() > 0l)
        {
            return;
        }
        
        try
        {
            doOpen();
        }
        catch (IOException e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            throw e;
        }
        catch (Exception e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            throw new IOException(e);
        }
    }

    private void doOpen()
        throws IOException
    {

        getLock().readLock().lock();

        transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());

        callback.onBeforeRead(this);
    }

    @Override
    public void close()
        throws IOException
    {
        if (getBytesCount() == 0l)
        {
            return;
        }
        
        try
        {
            doClose();
        } 
        finally
        {
            getLock().readLock().unlock();
        }
    }

    private void doClose()
        throws IOException
    {
        super.close();

        callback.onAfterRead(this);

        transactionManager.commit(transactionStatus);
    }

    public long getBytesCount()
    {
        return ((CountingInputStream) this.in).getByteCount();
    }

    public static RepositoryInputStream of(Path path,
                                           ReadWriteLock lock,
                                           PlatformTransactionManager tm,
                                           InputStream is)
    {
        ArtifactInputStream source = is instanceof ArtifactInputStream ? (ArtifactInputStream) is
                : StreamUtils.findSource(ArtifactInputStream.class, is);
        Assert.notNull(source, String.format("Source should be [%s]", ArtifactInputStream.class.getSimpleName()));

        return new RepositoryInputStream(path, lock, tm, is);
    }

}
