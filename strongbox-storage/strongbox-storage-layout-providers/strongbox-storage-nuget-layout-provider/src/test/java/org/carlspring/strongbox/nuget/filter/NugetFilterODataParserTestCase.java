package org.carlspring.strongbox.nuget.filter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;

import ru.aristar.jnuget.Version;
import ru.aristar.jnuget.query.AndExpression;
import ru.aristar.jnuget.query.Expression;
import ru.aristar.jnuget.query.IdEqIgnoreCase;
import ru.aristar.jnuget.query.LatestVersionExpression;
import ru.aristar.jnuget.query.VersionEq;

public class NugetFilterODataParserTestCase
{

    @Test
    public void testParseFilter()
        throws Exception
    {
        String packageId = "Org.Carlspring.Strongbox.Nuget.Test.LastVersion";
        String packageVersion = "1.0.0";

        Expression filter = new IdEqIgnoreCase(packageId);
        filter = new AndExpression(filter, new LatestVersionExpression());
        filter = new AndExpression(filter, new VersionEq(Version.parse(packageVersion)));

        System.out.println(filter.toString());

        CodePointCharStream is = CharStreams.fromString("(tolower(Id) eq 'org.carlspring.strongbox.nuget.test.lastversion') and (IsLatestVersion and Version eq '1.0.0')");
        NugetODataFilterLexer lexer = new NugetODataFilterLexer(is);
        CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
        NugetODataFilterParser parser = new NugetODataFilterParser(commonTokenStream);

        NugetODataFilterParser.FilterContext fileContext = parser.filter();
        NugetODataFilterVisitor<String> visitor = new NugetODataFilterVisitorImpl();
        visitor.visitFilter(fileContext);
    }

}
