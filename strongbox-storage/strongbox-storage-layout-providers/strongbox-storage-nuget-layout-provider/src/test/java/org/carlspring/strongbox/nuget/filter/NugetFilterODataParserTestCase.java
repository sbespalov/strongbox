package org.carlspring.strongbox.nuget.filter;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.carlspring.strongbox.config.NugetLayoutProviderTestConfig;
import org.carlspring.strongbox.providers.repository.RepositorySearchRequest;
import org.carlspring.strongbox.testing.TestCaseWithNugetPackageGeneration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = NugetLayoutProviderTestConfig.class)
public class NugetFilterODataParserTestCase
        extends TestCaseWithNugetPackageGeneration
{

    @Test
    public void testParseFilter()
        throws Exception
    {
        CodePointCharStream is = CharStreams.fromString("tolower(Id) eq 'org.carlspring.strongbox.nuget.test.lastversion' and IsLatestVersion and Version eq '1.0.0'");
        NugetODataFilterLexer lexer = new NugetODataFilterLexer(is);
        CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
        NugetODataFilterParser parser = new NugetODataFilterParser(commonTokenStream);

        NugetODataFilterParser.FilterContext fileContext = parser.filter();
        NugetODataFilterVisitor<RepositorySearchRequest> visitor = new NugetODataFilterVisitorImpl(new RepositorySearchRequest(null, null));
        visitor.visitFilter(fileContext);
    }

}
