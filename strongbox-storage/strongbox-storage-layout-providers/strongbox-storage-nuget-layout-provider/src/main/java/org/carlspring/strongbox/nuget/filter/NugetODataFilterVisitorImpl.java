package org.carlspring.strongbox.nuget.filter;

import org.antlr.v4.runtime.ParserRuleContext;
import org.carlspring.strongbox.artifact.criteria.ArtifactEntryCriteria;
import org.carlspring.strongbox.data.criteria.Predicate;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.LogicalOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpFunctionContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpLeftContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpRightContext;

/**
 * @author sbespalov
 *
 */
public class NugetODataFilterVisitorImpl extends NugetODataFilterBaseVisitor<Predicate<ArtifactEntryCriteria>>
{

    private Predicate<ArtifactEntryCriteria> c;

    public NugetODataFilterVisitorImpl(Predicate<ArtifactEntryCriteria> c)
    {
        super();
        this.c = c;
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitFilter(FilterContext ctx)
    {
        trace(ctx);
        return super.visitFilter(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitFilterExp(FilterExpContext ctx)
    {
        trace(ctx);
        return super.visitFilterExp(ctx);
    }

    private void trace(ParserRuleContext ctx)
    {
        System.out.println();
        System.out.println(ctx.getClass().getSimpleName());
        System.out.println(ctx.getText());
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExp(TokenExpContext ctx)
    {
        trace(ctx);
        return super.visitTokenExp(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExpRight(TokenExpRightContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpRight(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExpLeft(TokenExpLeftContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpLeft(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitTokenExpFunction(TokenExpFunctionContext ctx)
    {
        trace(ctx);
        return super.visitTokenExpFunction(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitFilterOp(FilterOpContext ctx)
    {
        trace(ctx);
        return super.visitFilterOp(ctx);
    }

    @Override
    public Predicate<ArtifactEntryCriteria> visitLogicalOp(LogicalOpContext ctx)
    {
        trace(ctx);
        return super.visitLogicalOp(ctx);
    }

}
