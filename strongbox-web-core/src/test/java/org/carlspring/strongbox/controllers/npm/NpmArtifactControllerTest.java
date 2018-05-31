package org.carlspring.strongbox.controllers.npm;

import org.carlspring.strongbox.artifact.coordinates.NpmArtifactCoordinates;
import org.carlspring.strongbox.artifact.generator.NpmPackageGenerator;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.providers.layout.NpmLayoutProvider;
import org.carlspring.strongbox.storage.repository.NpmRepositoryFactory;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryPolicyEnum;
import org.carlspring.strongbox.testing.NpmRepositoryTestCase;

import javax.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.equalTo;

@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NpmArtifactControllerTest extends NpmRepositoryTestCase
{

    private static final String STORAGE_ID = "storage-common-npm";

    private static final String REPOSITORY_RELEASES_1 = "npm-releases-test";

    @Inject
    private NpmRepositoryFactory npmRepositoryFactory;

    @Inject
    @Qualifier("contextBaseUrl")
    private String contextBaseUrl;

    @BeforeClass
    public static void cleanUp()
        throws Exception
    {
        cleanUp(getRepositoriesToClean());
    }

    public static Set<Repository> getRepositoriesToClean()
    {
        Set<Repository> repositories = new LinkedHashSet<>();
        repositories.add(createRepositoryMock(STORAGE_ID, REPOSITORY_RELEASES_1));

        return repositories;
    }

    @Before
    public void init()
        throws Exception
    {
        createStorage(STORAGE_ID);

        Repository repository = npmRepositoryFactory.createRepository(STORAGE_ID, REPOSITORY_RELEASES_1);
        repository.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repository.setLayout(NpmLayoutProvider.ALIAS);

        createRepository(repository);
    }

    @Test
    public void testPackageWithScopeShouldBeResolvableAfterUpload()
        throws Exception
    {
        NpmArtifactCoordinates coordinates = NpmArtifactCoordinates.of("@carlspring/npm-test-release", "1.0.0");
        NpmPackageGenerator packageGenerator = NpmPackageGenerator.newInstance();
        Path publishJsonPath = packageGenerator.of(coordinates).buildPublishJson();
        Path packagePath = packageGenerator.getPackagePath();

        uploadPackage(coordinates, publishJsonPath);

        downloadPackage(coordinates, packagePath);
    }

    @Test
    public void testPackageWithoutScopeShouldBeResolvableAfterUpload()
        throws Exception
    {
        NpmArtifactCoordinates coordinates = NpmArtifactCoordinates.of("npm-test-release", "1.0.0");
        NpmPackageGenerator packageGenerator = NpmPackageGenerator.newInstance();
        Path publishJsonPath = packageGenerator.of(coordinates).buildPublishJson();
        Path packagePath = packageGenerator.getPackagePath();

        uploadPackage(coordinates, publishJsonPath);

        downloadPackage(coordinates, packagePath);
    }

    private void downloadPackage(NpmArtifactCoordinates coordinates,
                                 Path packagePath)
        throws IOException
    {
        given().header("User-Agent", "npm/*")
               .header("Content-Type", "application/json")
               .when()
               .get(contextBaseUrl + "/storages/" + STORAGE_ID + "/" + REPOSITORY_RELEASES_1 + "/" +
                       coordinates.toResource())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .assertThat()
               .header("Content-Length", equalTo(String.valueOf(Files.size(packagePath))));
    }

    private void uploadPackage(NpmArtifactCoordinates coordinates,
                               Path publishJsonPath)
        throws IOException
    {
        byte[] publishJsonContent = Files.readAllBytes(publishJsonPath);

        given().header("User-Agent", "npm/*")
               .header("Content-Type", "application/json")
               .body(publishJsonContent)
               .when()
               .put(contextBaseUrl + "/storages/" + STORAGE_ID + "/" + REPOSITORY_RELEASES_1 + "/" +
                       coordinates.getId())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value());
    }

}
