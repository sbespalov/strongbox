package org.carlspring.strongbox.data.criteria;

import org.carlspring.strongbox.data.domain.GenericEntity;

/**
 * @author sbespalov
 *
 */
public class Selector<T>
{

    private Class<? extends GenericEntity> targetClass;

    private Projection projection = Projection.ROWS;

    private Predicate<T> predicate;

    public Selector(Class<? extends GenericEntity> targetClass)
    {
        super();
        this.targetClass = targetClass;
    }

    public Class<? extends GenericEntity> getTargetClass()
    {
        return targetClass;
    }

    public Predicate<T> getPredicate()
    {
        return predicate;
    }

    public Projection getProjection()
    {
        return projection;
    }

    public Selector<T> select(Projection p)
    {
        this.projection = p;
        return this;
    }

    public Predicate<T> where()
    {
        return this.predicate = new Predicate<T>();
    }

    public Predicate<T> eq(T c)
    {
        return new Predicate<T>().eq(c);
    }

    public Predicate<T> and(Predicate<T> p1,
                            Predicate<T> p2)
    {
        return p1.and(p2);
    }

    public Predicate<T> or(Predicate<T> p1,
                           Predicate<T> p2)
    {
        return p1.or(p2);
    }

}
