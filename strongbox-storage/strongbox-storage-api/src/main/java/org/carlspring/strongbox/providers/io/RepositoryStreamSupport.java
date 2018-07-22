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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author sbespalov
 *
 */
public abstract class RepositoryStreamSupport implements RepositoryStreamCallback
{
    private static final Logger logger = LoggerFactory.getLogger(RepositoryStreamSupport.class);

    @Inject
    private PlatformTransactionManager transactionManager;

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
            //TODO: write lock currently managed within ArtifactManagementService, but we should think to manage it here, as for InputStream. 
            lock = null;
        }
        else
        {
            lock = lockSource.readLock();
        }

        ctx.setLock(lock);
        Optional.ofNullable(lock).ifPresent(l -> l.lock());

        try
        {
            doOpen();
        }
        catch (IOException e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            ctx.getTransactionStatus().setRollbackOnly();
            ctx.getTransactionStatus().setRollbackOnly();

            throw e;
        }
        catch (Exception e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            ctx.getTransactionStatus().setRollbackOnly();

            throw new IOException(e);
        }
    }

    private void doOpen()
        throws IOException
    {
        RepositoryStreamContext ctx = getContext();
        ctx.setOpened(true);
        ctx.setTransactionStatus(transactionManager.getTransaction(new DefaultTransactionDefinition()));

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
        RepositoryPath path = (RepositoryPath) ctx.getPath();

        try
        {
            if (!ctx.isOpened())
            {
                return;
            }

            doClose();
        }
        catch (IOException e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            ctx.getTransactionStatus().setRollbackOnly();

            throw e;
        }
        catch (Exception e)
        {
            logger.error(String.format("Callback failed for [%s]", path), e);
            ctx.getTransactionStatus().setRollbackOnly();

            throw new IOException(e);
        } finally
        {
            Optional.ofNullable(ctx.getLock()).ifPresent(l -> l.unlock());
            clearContext();
        }
    }

    private void doClose()
        throws IOException
    {
        RepositoryStreamContext ctx = getContext();

        if (ctx instanceof RepositoryStreamWriteContext)
        {
            onAfterWrite((RepositoryStreamWriteContext) ctx);
        }
        else
        {
            onAfterRead((RepositoryStreamReadContext) ctx);
        }

        TransactionStatus transactionStatus = ctx.getTransactionStatus();
        if (transactionStatus.isRollbackOnly())
        {
            logger.error(String.format("Transaction for [%s] set to rollback.", ctx.getPath()));
            transactionManager.rollback(transactionStatus);
            return;
        }

        transactionManager.commit(transactionStatus);
    }

    private void rollbackOnException(Throwable ex)
        throws TransactionException
    {

        RepositoryStreamContext ctx = getContext();
        if (!ctx.isOpened())
        {
            return;
        }
        else if (ctx.getTransactionStatus().isRollbackOnly())
        {
            return;
        }

        logger.debug("Initiating transaction rollback on application exception", ex);
        try
        {
            transactionManager.rollback(ctx.getTransactionStatus());
        }
        catch (TransactionSystemException ex2)
        {
            logger.error("Application exception overridden by rollback exception", ex);
            ex2.initApplicationException(ex);
            throw ex2;
        }
        catch (RuntimeException | Error ex2)
        {
            logger.error("Application exception overridden by rollback exception", ex);
            throw ex2;
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

            try
            {
                super.write(bts);
            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            }
        }

        @Override
        public void write(int idx)
            throws IOException
        {
            open();

            try
            {
                super.write(idx);
            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            }
        }

        @Override
        public void write(byte[] bts,
                          int st,
                          int end)
            throws IOException
        {
            open();

            try
            {
                super.write(bts, st, end);
            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            }
        }

        @Override
        public void close()
            throws IOException
        {
            try
            {
                super.close();

            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            } finally
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
        public int read()
            throws IOException
        {
            try
            {
                return super.read();
            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            }
        }

        @Override
        public int read(byte[] bts)
            throws IOException
        {
            try
            {
                return super.read(bts);
            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            }
        }

        @Override
        public int read(byte[] bts,
                        int off,
                        int len)
            throws IOException
        {
            try
            {
                return super.read(bts, off, len);
            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            }
        }

        @Override
        public void close()
            throws IOException
        {
            try
            {
                super.close();
            }
            catch (IOException e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw e;
            }
            catch (Exception e)
            {
                getContext().getTransactionStatus().setRollbackOnly();

                throw new IOException(e);
            } finally
            {
                RepositoryStreamSupport.this.close();
            }
        }

    }

}
