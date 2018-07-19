package org.carlspring.strongbox.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

public class RepositoryInputStream
        extends CountingInputStream
        implements RepositoryStreamContext
{

    private static final Logger logger = LoggerFactory.getLogger(RepositoryInputStream.class);

    protected RepositoryStreamCallback callback = new EmptyRepositoryStreamCallback();

    private Path path;

    private ReadWriteLock lock;

    private PlatformTransactionManager transactionManager;

    private TransactionStatus transactionStatus;
    
    private volatile boolean opened = false;

    protected RepositoryInputStream(Path path,
                                    ReadWriteLock lock,
                                    PlatformTransactionManager transactionManager,
                                    InputStream in)
    {
        super(in);

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
    protected void beforeRead(int n)
        throws IOException
    {
        open();
    }

    private void open()
        throws IOException
    {
        if (opened || getByteCount() > 0l)
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

        opened = true;
        
        transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());

        callback.onBeforeRead(this);
    }

    @Override
    public void close()
            throws IOException
    {
        try
        {
            super.close();
            
            if (opened)
            {
                doClose();
            }
        }
        catch (IOException e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            rollbackOnException(transactionStatus, e);
            
            throw e;
        }
        catch (Exception e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            rollbackOnException(transactionStatus, e);
            
            throw new IOException(e);
        }        
        finally
        {
            if (opened)
            {
                getLock().writeLock().unlock();
            }
        }
    }
    
    private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException 
    {
        
        if (!opened)
        {
            return;
        }
        
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

        logger.debug("Initiating transaction rollback on application exception", ex);
        try {
            transactionManager.rollback(status);
        }
        catch (TransactionSystemException ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            ex2.initApplicationException(ex);
            throw ex2;
        }
        catch (RuntimeException | Error ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            throw ex2;
        }
    }
    
    private void doClose()
        throws IOException
    {
        if (!opened)
        {
            return;
        }
        
        callback.onAfterRead(this);

        transactionManager.commit(transactionStatus);
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
