package org.carlspring.strongbox.nuget.filter;

import org.antlr.v4.runtime.tree.RuleNode;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.FilterOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.LogicalOpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpFunctionContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpLeftContext;
import org.carlspring.strongbox.nuget.filter.NugetODataFilterParser.TokenExpRightContext;

public class NugetODataFilterVisitorImpl extends NugetODataFilterBaseVisitor<String>
{

    @Override
    public String visitFilter(FilterContext ctx)
    {
        return super.visitFilter(ctx);
    }

    @Override
    public String visitFilterExp(FilterExpContext ctx)
    {
        return super.visitFilterExp(ctx);
    }

    @Override
    public String visitTokenExp(TokenExpContext ctx)
    {
        System.out.println(ctx.TAG());
        return super.visitTokenExp(ctx);
    }

    @Override
    public String visitTokenExpRight(TokenExpRightContext ctx)
    {
        return super.visitTokenExpRight(ctx);
    }

    @Override
    public String visitTokenExpLeft(TokenExpLeftContext ctx)
    {
        return super.visitTokenExpLeft(ctx);
    }

    @Override
    public String visitTokenExpFunction(TokenExpFunctionContext ctx)
    {
        return super.visitTokenExpFunction(ctx);
    }

    @Override
    public String visitFilterOp(FilterOpContext ctx)
    {
        return super.visitFilterOp(ctx);
    }

    @Override
    public String visitLogicalOp(LogicalOpContext ctx)
    {
        return super.visitLogicalOp(ctx);
    }

    @Override
    public String visitChildren(RuleNode node)
    {
        return super.visitChildren(node);
    }

    
}
