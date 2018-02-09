package org.carlspring.strongbox.data.criteria;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * @author sbespalov
 *
 */
public class Predicate<T>
{

    private T criteria;

    private BooleanOperator operator;

    private List<Predicate<T>> predicate = new ArrayList<>();

    public Predicate()
    {
        super();
    }

    public T getCriteria()
    {
        return criteria;
    }

    public BooleanOperator getOperator()
    {
        return operator;
    }

    public List<Predicate<T>> getPredicate()
    {
        return predicate;
    }

    public Predicate<T> eq(T c)
    {
        this.criteria = c;
        return this;
    }

    public Predicate<T> or(Predicate<T> p)
    {
        Assert.state(!BooleanOperator.AND.equals(this.operator), "Only disjunction allowed.");

        this.operator = BooleanOperator.OR;
        this.predicate.add(p);

        return this;
    }

    public Predicate<T> and(Predicate<T> p)
    {
        Assert.state(!BooleanOperator.OR.equals(this.operator), "Only conjunction allowed.");

        this.operator = BooleanOperator.AND;
        this.predicate.add(p);

        return this;
    }

    public static enum BooleanOperator
    {
        AND, OR
    }
    
}
