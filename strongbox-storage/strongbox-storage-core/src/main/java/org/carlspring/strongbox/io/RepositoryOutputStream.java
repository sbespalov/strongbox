package org.carlspring.strongbox.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.io.output.CountingOutputStream;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

/**
 * This class decorate {@link ArtifactOutputStream} with {@link MutableRepository} specific logic.
 *
 * @author Sergey Bespalov
 *
 */
public class RepositoryOutputStream extends FilterOutputStream implements RepositoryStreamContext
{

    private static final Logger logger = LoggerFactory.getLogger(RepositoryOutputStream.class);

    protected RepositoryStreamCallback callback = new EmptyRepositoryStreamCallback();

    private Path path;

    private ReadWriteLock lock;
    
    private PlatformTransactionManager transactionManager;
    
    private TransactionStatus transactionStatus;
    
    protected RepositoryOutputStream(Path path,
                                     ReadWriteLock lock,
                                     PlatformTransactionManager transactionManager,
                                     OutputStream out)
    {
        super(new CountingOutputStream(out));
        
        this.lock = lock;
        this.path = path;
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

    @Override
    public void write(int b)
            throws IOException
    {
        open();
        
        super.write(b);
    }

    private void open()
        throws IOException
    {
        CountingOutputStream counting = (CountingOutputStream) out;
        if (counting.getByteCount() == 0L)
        {
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
    }

    private void doOpen()
        throws IOException
    {
        Lock wLock = getLock().writeLock();
        wLock.lock();
        
        transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        
        callback.onBeforeWrite(this);
    }

    @Override
    public void close()
            throws IOException
    {
        try
        {
            doClose();
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
            getLock().writeLock().unlock();
        }
    }

    private void doClose()
        throws IOException
    {
        super.close();
        
        callback.onAfterWrite(this);
        
        transactionManager.commit(transactionStatus);
    }
    
    private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
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

    public RepositoryOutputStream with(RepositoryStreamCallback callback)
    {
        this.callback = callback;
        return this;
    }

    public static RepositoryOutputStream of(Path path,
                                            ReadWriteLock lock,
                                            PlatformTransactionManager tm,
                                            OutputStream os)
    {
        ArtifactOutputStream source = os instanceof ArtifactOutputStream ? (ArtifactOutputStream) os
                                                                         : StreamUtils.findSource(ArtifactOutputStream.class, os);
        Assert.notNull(source, String.format("Source should be [%s]", ArtifactOutputStream.class.getSimpleName()));

        return new RepositoryOutputStream(path, lock, tm, os);
    }

}
