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

    private List<Predicate<T>> expression = new ArrayList<>();

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
        return expression;
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
        add(p);

        return this;
    }

    public Predicate<T> and(Predicate<T> p)
    {
        Assert.state(!BooleanOperator.OR.equals(this.operator), "Only conjunction allowed.");

        this.operator = BooleanOperator.AND;
        add(p);

        return this;
    }

    private void add(Predicate<T> p)
    {
        if (p == this)
        {
            return;
        }
        this.expression.add(p);
    }

    public static <T> Predicate<T> root()
    {
        Predicate<T> p = new Predicate<T>();
        p.operator = BooleanOperator.AND;
        return p;
    }

    public static enum BooleanOperator
    {
        AND, OR
    }

}
