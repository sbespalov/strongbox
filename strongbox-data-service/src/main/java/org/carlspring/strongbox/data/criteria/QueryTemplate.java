package org.carlspring.strongbox.data.criteria;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

/**
 * @author sbespalov
 *
 */
@Component
public class QueryTemplate
{

    @PersistenceContext
    protected EntityManager entityManager;

    public <T> Object select(Selector<T> selector)
    {
        return null;
    }

}
