package org.carlspring.strongbox.data.service;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.carlspring.strongbox.data.domain.GenericEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * @author sbespalov
 *
 */
@Component
public class EntityLock
{

    private static final String STRONGBOX_ENTITY_MAP_LOCKS = "strongbox-entity-map-locks";

    @Inject
    private HazelcastInstance hazelcastInstance;

    public void lock(@Nonnull GenericEntity entity)
    {
        final IMap mapLocks = hazelcastInstance.getMap(STRONGBOX_ENTITY_MAP_LOCKS);
        final String lock = getLock(entity);

        mapLocks.lock(lock);
    }

    public void unlock(@Nonnull GenericEntity entity)
    {
        final IMap mapLocks = hazelcastInstance.getMap(STRONGBOX_ENTITY_MAP_LOCKS);
        final String lock = getLock(entity);
        
        mapLocks.unlock(lock);
    }

    private String getLock(final @Nonnull GenericEntity entity)
    {
        Assert.notNull(entity.getUuid(), String.format("Unable to lock entity %s", entity.getClass().getSimpleName()));
        return entity.getUuid();
    }
}
