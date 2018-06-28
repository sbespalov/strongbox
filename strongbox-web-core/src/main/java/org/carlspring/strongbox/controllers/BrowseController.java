package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.domain.DirectoryListing;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.services.ConfigurationManagementService;
import org.carlspring.strongbox.services.DirectoryListingService;
import org.carlspring.strongbox.services.DirectoryListingServiceImpl;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * REST API for browsing storage/repository/filesystem structures.
 *
 * @author Guido Grazioli <guido.grazioli@gmail.com>
 */
@RestController
@RequestMapping(path = BrowseController.ROOT_CONTEXT)
public class BrowseController extends BaseArtifactController
{

    private static final Logger logger = LoggerFactory.getLogger(BrowseController.class);

    // must be the same as @RequestMapping value on the class definition
    public final static String ROOT_CONTEXT = "/api/browse";

    @Inject
    private ObjectMapper objectMapper;

    private volatile DirectoryListingService directoryListingService;
    
    public DirectoryListingService getDirectoryListingService()
    {
        return Optional.ofNullable(directoryListingService).orElseGet(() -> {
            String baseUrl = StringUtils.chomp(configurationManager.getConfiguration().getBaseUrl(), "/");
            return directoryListingService = new DirectoryListingServiceImpl(String.format("%s/api/browse", baseUrl));
        });
    }
    
    @ApiOperation(value = "List configured storages.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The list was returned."),
                            @ApiResponse(code = 500, message = "An error occurred.") })
    @PreAuthorize("hasAuthority('ARTIFACTS_RESOLVE')")
    @RequestMapping(method = RequestMethod.GET,
                    produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Object storages(ModelMap model,
                           HttpServletRequest request,
                           @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader)
    {
        logger.debug("Requested browsing for storages");
        
        try
        {
            DirectoryListing directoryListing = getDirectoryListingService().fromStorages(configurationManager.getConfiguration().getStorages());

            if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE))
            {
                return ResponseEntity.ok(objectMapper.writer().writeValueAsString(directoryListing));
            }

            model.addAttribute("showBack", false);
            model.addAttribute("currentUrl", StringUtils.chomp(request.getRequestURI(), "/"));
            model.addAttribute("directories", directoryListing.getDirectories());
            model.addAttribute("files", directoryListing.getFiles());

            return new ModelAndView("directoryListing", model);
        }
        catch (Exception e)
        {
            String message = "Attempt to browse storages failed. Check server logs for more information.";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }
    }

    @ApiOperation(value = "List configured repositories for a storage.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The list was returned."),
                            @ApiResponse(code = 404, message = "The requested storage was not found."),
                            @ApiResponse(code = 500, message = "An error occurred.") })
    @PreAuthorize("hasAuthority('ARTIFACTS_RESOLVE')")
    @RequestMapping(value="/{storageId}",
                    method = RequestMethod.GET,
                    produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Object repositories(@ApiParam(value = "The storageId", required = true) @PathVariable("storageId") String storageId,
                               HttpServletRequest request,
                               ModelMap model,
                               @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader)
    {
        logger.debug("Requested browsing for repositories in storage : " + storageId);

        try
        {
            Storage storage = configurationManager.getConfiguration().getStorage(storageId);
            if (storage == null)
            {
                return getNotFoundResponseEntity("The requested storage was not found.", acceptHeader);
            }

            DirectoryListing directoryListing = getDirectoryListingService().fromRepositories(storage.getRepositories());

            if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE))
            {
                return ResponseEntity.ok(objectMapper.writer().writeValueAsString(directoryListing));
            }

            model.addAttribute("currentUrl", StringUtils.chomp(request.getRequestURI(), "/"));
            model.addAttribute("directories", directoryListing.getDirectories());
            model.addAttribute("files", directoryListing.getFiles());

            return new ModelAndView("directoryListing", model);
        }
        catch (Exception e)
        {
            String message = "Attempt to browse repositories failed. Check server logs for more information.";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }
    }

    @ApiOperation(value = "List the contents for a repository.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The list was returned."),
                            @ApiResponse(code = 404, message = "The requested storage, repository, or path was not found."),
                            @ApiResponse(code = 500, message = "An error occurred.") })
    @PreAuthorize("hasAuthority('ARTIFACTS_RESOLVE')")
    @RequestMapping(value = { "{storageId}/{repositoryId}/{path:.+}" },
                    method = RequestMethod.GET,
                    produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Object repositoryContent(@ApiParam(value = "The storageId", required = true)
                                    @PathVariable("storageId") String storageId,
                                    @ApiParam(value = "The repositoryId", required = true)
                                    @PathVariable("repositoryId") String repositoryId,
                                    @ApiParam(value = "The repository path", required = false)
                                    @PathVariable("path") String rawPath,
                                    HttpServletRequest request,
                                    ModelMap model,
                                    @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader)
    {
        logger.debug("Requested browsing repository content at {}/{}/{} ", storageId, repositoryId, rawPath);

        try
        {
            Storage storage = configurationManager.getConfiguration().getStorage(storageId);
            if (storage == null)
            {
                return getNotFoundResponseEntity("The requested storage was not found.", acceptHeader);
            }

            Repository repository = storage.getRepository(repositoryId);
            if (repository == null)
            {
                return getNotFoundResponseEntity("The requested repository was not found.", acceptHeader);
            }

            final RepositoryPath repositoryPath = repositoryPathResolver.resolve(repository, rawPath);
            if (repositoryPath == null || !Files.exists(repositoryPath))
            {
                return getNotFoundResponseEntity("The requested repository path was not found.", acceptHeader);
            }

            if (!repository.isInService())
            {
                return getServiceUnavailableResponseEntity("Repository is not in service...", acceptHeader);
            }

            if (!repository.allowsDirectoryBrowsing() || !probeForDirectoryListing(repositoryPath))
            {
                return getNotFoundResponseEntity("Requested repository doesn't allow browsing.", acceptHeader);
            }

            DirectoryListing directoryListing = getDirectoryListingService().fromRepositoryPath(repositoryPath);

            if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE))
            {
                return ResponseEntity.ok(objectMapper.writer().writeValueAsString(directoryListing));
            }
            
            URL resourceUrl = RepositoryFiles.readResourceUrl(repositoryPath);
            
            String downloadBaseUrl = StringUtils.chomp(resourceUrl.toString(), "/");
            String currentUrl = StringUtils.chomp(request.getRequestURI(), "/");
            

            model.addAttribute("currentUrl", currentUrl);
            model.addAttribute("downloadBaseUrl", downloadBaseUrl);
            model.addAttribute("directories", directoryListing.getDirectories());
            model.addAttribute("files", directoryListing.getFiles());

            return new ModelAndView("directoryListing", model);
        }
        catch (Exception e)
        {
            String message = "Failed to generate repository directory listing.";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }
    }

    protected boolean probeForDirectoryListing(final RepositoryPath repositoryPath)
            throws IOException
    {
        return Files.exists(repositoryPath) &&
               Files.isDirectory(repositoryPath) &&
               isPermittedForDirectoryListing(repositoryPath);
    }

    protected boolean isPermittedForDirectoryListing(final RepositoryPath repositoryPath)
            throws IOException
    {
        //TODO: RepositoryFiles.isIndex(repositoryPath) || (
        return !Files.isHidden(repositoryPath) && !RepositoryFiles.isTrash(repositoryPath)
                && !RepositoryFiles.isTemp(repositoryPath);
    }

}
