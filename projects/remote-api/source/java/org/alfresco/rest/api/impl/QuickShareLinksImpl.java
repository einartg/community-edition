/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.rest.api.impl;

import org.alfresco.model.ContentModel;
import org.alfresco.model.QuickShareModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.repo.quickshare.QuickShareClientNotFoundException;
import org.alfresco.repo.quickshare.QuickShareServiceImpl.QuickShareEmailRequest;
import org.alfresco.repo.search.QueryParameterDefImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.tenant.TenantUtil;
import org.alfresco.rest.antlr.WhereClauseParser;
import org.alfresco.rest.api.Nodes;
import org.alfresco.rest.api.People;
import org.alfresco.rest.api.QuickShareLinks;
import org.alfresco.rest.api.Renditions;
import org.alfresco.rest.api.model.ContentInfo;
import org.alfresco.rest.api.model.Node;
import org.alfresco.rest.api.model.QuickShareLink;
import org.alfresco.rest.api.model.QuickShareLinkEmailRequest;
import org.alfresco.rest.api.model.Rendition;
import org.alfresco.rest.api.model.UserInfo;
import org.alfresco.rest.framework.core.exceptions.ConstraintViolatedException;
import org.alfresco.rest.framework.core.exceptions.DisabledServiceException;
import org.alfresco.rest.framework.core.exceptions.EntityNotFoundException;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.rest.framework.core.exceptions.PermissionDeniedException;
import org.alfresco.rest.framework.resource.content.BinaryResource;
import org.alfresco.rest.framework.resource.parameters.CollectionWithPagingInfo;
import org.alfresco.rest.framework.resource.parameters.Paging;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import org.alfresco.rest.framework.resource.parameters.Params;
import org.alfresco.rest.framework.resource.parameters.where.Query;
import org.alfresco.rest.framework.resource.parameters.where.QueryHelper;
import org.alfresco.rest.framework.webscripts.ResourceWebScriptHelper;
import org.alfresco.rest.workflow.api.impl.MapBasedQueryWalker;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.quickshare.InvalidSharedIdException;
import org.alfresco.service.cmr.quickshare.QuickShareDTO;
import org.alfresco.service.cmr.quickshare.QuickShareService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.SearchLanguageConversion;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.I18NUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralises access to shared link (public "quick share") services and maps between representations.
 *
 * @author janv
 * @author Jamal Kaabi-Mofrad
 * 
 * @since publicapi1.0
 */
public class QuickShareLinksImpl implements QuickShareLinks, InitializingBean
{
    private static final Log logger = LogFactory.getLog(QuickShareLinksImpl.class);

    private final static String DISABLED = "QuickShare is disabled system-wide";
    private boolean enabled = true;

    private ServiceRegistry sr;
    private QuickShareService quickShareService;
    private Nodes nodes;
    private Renditions renditions;

    private NodeService nodeService;
    private PersonService personService;
    private AuthorityService authorityService;
    private MimetypeService mimeTypeService;
    private SearchService searchService;
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private SiteService siteService;

    public void setServiceRegistry(ServiceRegistry sr)
    {
        this.sr = sr;
    }


    public void setQuickShareService(QuickShareService quickShareService)
    {
        this.quickShareService = quickShareService;
    }

    public void setNodes(Nodes nodes)
    {
        this.nodes = nodes;
    }

    public void setRenditions(Renditions renditions)
    {
        this.renditions = renditions;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public void afterPropertiesSet()
    {
        ParameterCheck.mandatory("sr", this.sr);
        ParameterCheck.mandatory("quickShareService", this.quickShareService);
        ParameterCheck.mandatory("nodes", this.nodes);

        this.nodeService = sr.getNodeService();
        this.personService = sr.getPersonService();
        this.authorityService = sr.getAuthorityService();
        this.mimeTypeService = sr.getMimetypeService();
        this.searchService = sr.getSearchService();
        this.dictionaryService = sr.getDictionaryService();
        this.namespaceService = sr.getNamespaceService();
        this.siteService = sr.getSiteService();
    }

    /**
     * Returns limited metadata regarding the shared (content) link.
     * <p>
     * Note: does *not* require authenticated access for (public) shared link.
     */
    public QuickShareLink readById(final String sharedId, final Parameters parameters)
    {
        checkEnabled();

        try
        {
            Pair<String, NodeRef> pair = quickShareService.getTenantNodeRefFromSharedId(sharedId);
            String networkTenantDomain = pair.getFirst();

            return TenantUtil.runAsSystemTenant(new TenantUtil.TenantRunAsWork<QuickShareLink>()
            {
                public QuickShareLink doWork() throws Exception
                {
                    // note: assume noAuth here (rather than rely on getRunAsUser which will be null in non-MT)
                    return getQuickShareInfo(sharedId, true, parameters.getInclude());
                }
            }, networkTenantDomain);
        }
        catch (InvalidSharedIdException ex)
        {
            logger.warn("Unable to find: " + sharedId);
            throw new EntityNotFoundException(sharedId);
        }
    }

    /**
     * Download content via shared link.
     * <p>
     * Note: does *not* require authenticated access for (public) shared link.
     *
     * @param sharedId
     * @param renditionId - optional
     * @param parameters {@link Parameters}
     * @return
     * @throws EntityNotFoundException
     */
    public BinaryResource readProperty(String sharedId, final String renditionId, final Parameters parameters) throws EntityNotFoundException
    {
        checkEnabled();
        checkValidShareId(sharedId);

        try
        {
            Pair<String, NodeRef> pair = quickShareService.getTenantNodeRefFromSharedId(sharedId);

            String networkTenantDomain = pair.getFirst();
            final NodeRef nodeRef = pair.getSecond();

            return TenantUtil.runAsSystemTenant(new TenantUtil.TenantRunAsWork<BinaryResource>()
            {
                public BinaryResource doWork() throws Exception
                {
                    // belt-and-braces (similar to QuickShareContentGet)
                    if (!nodeService.hasAspect(nodeRef, QuickShareModel.ASPECT_QSHARE))
                    {
                        throw new InvalidNodeRefException(nodeRef);
                    }

                    if (renditionId != null)
                    {
                        return renditions.getContent(nodeRef, renditionId, parameters);
                    }
                    else
                    {
                        return nodes.getContent(nodeRef, parameters, false);
                    }
                }
            }, networkTenantDomain);
        }
        catch (InvalidSharedIdException ex)
        {
            logger.warn("Unable to find: " + sharedId);
            throw new EntityNotFoundException(sharedId);
        }
        catch (InvalidNodeRefException inre)
        {
            logger.warn("Unable to find: " + sharedId + " [" + inre.getNodeRef() + "]");
            throw new EntityNotFoundException(sharedId);
        }
    }

    /**
     * Delete the shared link.
     * <p>
     * Once deleted, the shared link will no longer exist hence get/download will no longer work (ie. return 404).
     * If the link is later re-created then a new unique shared id will be generated.
     * <p>
     * Requires authenticated access.
     *
     * @param sharedId String id of the quick share
     */
    public void delete(String sharedId, Parameters parameters)
    {
        checkEnabled();
        checkValidShareId(sharedId);

        try
        {
            NodeRef nodeRef = quickShareService.getTenantNodeRefFromSharedId(sharedId).getSecond();

            String sharedByUserId = (String)nodeService.getProperty(nodeRef, QuickShareModel.PROP_QSHARE_SHAREDBY);
            if (! canDeleteSharedLink(nodeRef, sharedByUserId))
            {
                throw new PermissionDeniedException("Can't perform unshare action: " + sharedId);
            }

            quickShareService.unshareContent(sharedId);
        }
        catch (InvalidSharedIdException ex)
        {
            logger.warn("Unable to find: " + sharedId);
            throw new EntityNotFoundException(sharedId);
        }
        catch (InvalidNodeRefException inre)
        {
            logger.warn("Unable to find: " + sharedId + " [" + inre.getNodeRef() + "]");
            throw new EntityNotFoundException(sharedId);
        }
    }

    /**
     * Create quick share.
     * <p>
     * Requires authenticated access.
     *
     * @param nodeIds
     * @param parameters
     * @return
     */
    public List<QuickShareLink> create(List<QuickShareLink> nodeIds, Parameters parameters)
    {
        checkEnabled();

        List<QuickShareLink> result = new ArrayList<>(nodeIds.size());

        List<String> includeParam = parameters.getInclude();

        for (QuickShareLink qs : nodeIds)
        {
            String nodeId = qs.getNodeId();

            if (nodeId == null)
            {
                throw new InvalidArgumentException("A valid nodeId must be specified !");
            }

            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

            try
            {
                // Note: will throw InvalidNodeRefException (=> 404) if node does not exist
                String sharedId = (String) nodeService.getProperty(nodeRef, QuickShareModel.PROP_QSHARE_SHAREDID);
                if (sharedId != null)
                {
                    throw new ConstraintViolatedException("sharedId already exists: "+nodeId+" ["+sharedId+"]");
                }

                // Note: will throw AccessDeniedException (=> 403) via QuickShareService (when NodeService tries to getAspects)
                // Note: since we already check node exists above, we can assume that InvalidNodeRefException (=> 404) here means not content (see type check)
                try
                {
                    QuickShareDTO qsDto = quickShareService.shareContent(nodeRef);
                    result.add(getQuickShareInfo(qsDto.getId(), false, includeParam));
                }
                catch (InvalidNodeRefException inre)
                {
                    throw new InvalidArgumentException("Unable to create shared link to non-file content: " + nodeId);
                }
            }
            catch (AccessDeniedException ade)
            {
                throw new PermissionDeniedException("Unable to create shared link to node that does not exist: " + nodeId);
            }
            catch (InvalidNodeRefException inre)
            {
                logger.warn("Unable to create shared link: [" + nodeRef + "]");
                throw new EntityNotFoundException(nodeId);
            }
        }

        return result;
    }

    @Override
    public void emailSharedLink(String sharedId, QuickShareLinkEmailRequest emailRequest, Parameters parameters)
    {
        checkEnabled();
        checkValidShareId(sharedId);
        validateEmailRequest(emailRequest);

        try
        {
            NodeRef nodeRef = quickShareService.getTenantNodeRefFromSharedId(sharedId).getSecond();
            String sharedNodeName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            QuickShareEmailRequest request = new QuickShareEmailRequest();
            request.setSharedNodeName(sharedNodeName);
            request.setClient(emailRequest.getClient());
            request.setSharedId(sharedId);
            request.setSenderMessage(emailRequest.getMessage());
            request.setLocale(I18NUtil.parseLocale(emailRequest.getLocale()));
            request.setToEmails(emailRequest.getRecipientEmails());
            quickShareService.sendEmailNotification(request);
        }
        catch (InvalidSharedIdException ex)
        {
            throw new EntityNotFoundException(sharedId);
        }
        catch (InvalidNodeRefException inre)
        {
            logger.warn("Unable to find: " + sharedId + " [" + inre.getNodeRef() + "]");
            throw new EntityNotFoundException(sharedId);
        }
        catch (QuickShareClientNotFoundException ex)
        {
            throw new InvalidArgumentException("Client is not registered [" + emailRequest.getClient() + "]");
        }
    }

    @Override
    public CollectionWithPagingInfo<Rendition> getRenditions(String sharedId)
    {
        checkEnabled();
        checkValidShareId(sharedId);

        try
        {
            Pair<String, NodeRef> pair = quickShareService.getTenantNodeRefFromSharedId(sharedId);

            String networkTenantDomain = pair.getFirst();
            final NodeRef nodeRef = pair.getSecond();

            return TenantUtil.runAsSystemTenant(new TenantUtil.TenantRunAsWork<CollectionWithPagingInfo<Rendition>>()
            {
                public CollectionWithPagingInfo<Rendition> doWork() throws Exception
                {
                    String nodeId = nodeRef.getId();

                    // hmm ... can we simplify ?
                    String filterStatusCreated = "("+Renditions.PARAM_STATUS+"='"+Rendition.RenditionStatus.CREATED+"')";
                    Query whereQuery = ResourceWebScriptHelper.getWhereClause(filterStatusCreated);
                    Params.RecognizedParams recParams = new Params.RecognizedParams(null, null, null, null, null, null, whereQuery, null, false);
                    Parameters params = Params.valueOf(recParams, null, null, null);

                    return renditions.getRenditions(nodeId, params);

                }
            }, networkTenantDomain);
        }
        catch (InvalidSharedIdException ex)
        {
            logger.warn("Unable to find: " + sharedId);
            throw new EntityNotFoundException(sharedId);
        }
        catch (InvalidNodeRefException inre)
        {
            logger.warn("Unable to find: " + sharedId + " [" + inre.getNodeRef() + "]");
            throw new EntityNotFoundException(sharedId);
        }
    }

    // Helper find (search) method

    private final static Set<String> FIND_SHARED_LINKS_QUERY_PROPERTIES =
            new HashSet<>(Arrays.asList(new String[] {PARAM_SHAREDBY}));

    public CollectionWithPagingInfo<QuickShareLink> findLinks(Parameters parameters)
    {
        checkEnabled();

        String queryString =
                "ASPECT:\"" + QuickShareModel.ASPECT_QSHARE.toString() + "\"";

        SearchParameters sp = new SearchParameters();

        // note: REST API query parameter (ie. where clause filter) - not to be confused with search query ;-)
        Query q = parameters.getQuery();
        if (q != null)
        {
            // filtering via "where" clause
            MapBasedQueryWalker propertyWalker = new MapBasedQueryWalker(FIND_SHARED_LINKS_QUERY_PROPERTIES, null);
            QueryHelper.walk(q, propertyWalker);

            String sharedByUserId = propertyWalker.getProperty(PARAM_SHAREDBY, WhereClauseParser.EQUALS, String.class);

            if (sharedByUserId != null)
            {
                if (People.DEFAULT_USER.equalsIgnoreCase(sharedByUserId))
                {
                    sharedByUserId =  AuthenticationUtil.getFullyAuthenticatedUser();
                }

                QueryParameterDefinition qpd = new QueryParameterDefImpl(QuickShareModel.PROP_QSHARE_SHAREDBY, dictionaryService.getDataType(DataTypeDefinition.TEXT),
                        true, sharedByUserId);
                sp.addQueryParameterDefinition(qpd);

                String qsharedBy = QuickShareModel.PROP_QSHARE_SHAREDBY.toPrefixString(namespaceService);
                queryString += " +@"+SearchLanguageConversion.escapeLuceneQuery(qsharedBy)+":\"${"+qsharedBy+"}\"";
            }
        }

        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(queryString);
        sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

        Paging paging = parameters.getPaging();
        PagingRequest pagingRequest = Util.getPagingRequest(paging);

        sp.setSkipCount(pagingRequest.getSkipCount());
        sp.setMaxItems(pagingRequest.getMaxItems());

        sp.addSort("@" + ContentModel.PROP_MODIFIED, false);

        ResultSet results = searchService.query(sp);

        List<QuickShareLink> qsLinks = new ArrayList<>(results.length());

        List<String> includeParam = parameters.getInclude();

        for (ResultSetRow row : results)
        {
            NodeRef nodeRef = row.getNodeRef();
            qsLinks.add(getQuickShareInfo(nodeRef, false, includeParam));
        }

        results.close();

        return CollectionWithPagingInfo.asPaged(paging, qsLinks, results.hasMore(), new Long(results.getNumberFound()).intValue());
    }

    private QuickShareLink getQuickShareInfo(String sharedId, boolean noAuth, List<String> includeParam)
    {
        checkValidShareId(sharedId);

        Map<String, Object> map = (Map<String, Object>) quickShareService.getMetaData(sharedId).get("item");
        NodeRef nodeRef = new NodeRef((String) map.get("nodeRef"));

        return getQuickShareInfo(nodeRef, map, noAuth, includeParam);
    }

    private QuickShareLink getQuickShareInfo(NodeRef nodeRef, boolean noAuth, List<String> includeParam)
    {
        Map<String, Object> map = (Map<String, Object>) quickShareService.getMetaData(nodeRef).get("item");
        return getQuickShareInfo(nodeRef, map , noAuth, includeParam);
    }

    private QuickShareLink getQuickShareInfo(NodeRef nodeRef, Map<String, Object> map, boolean noAuth, List<String> includeParam)
    {
        String sharedId = (String)map.get("sharedId");

        try
        {
            Map<QName, Serializable> nodeProps = nodeService.getProperties(nodeRef);
            ContentData cd = (ContentData)nodeProps.get(ContentModel.PROP_CONTENT);

            String mimeType = cd.getMimetype();
            String mimeTypeName = mimeTypeService.getDisplaysByMimetype().get(mimeType);
            ContentInfo contentInfo = new ContentInfo(mimeType, mimeTypeName, cd.getSize(), cd.getEncoding());

            Map<String, UserInfo> mapUserInfo = new HashMap<>(2);

            // note: if noAuth mode then don't return userids (to limit disclosure and be consistent with v0 internal)
            boolean displayNameOnly = noAuth;

            UserInfo modifiedByUser = Node.lookupUserInfo((String)nodeProps.get(ContentModel.PROP_MODIFIER), mapUserInfo, personService, displayNameOnly);

            // TODO review - should we return sharedByUser for authenticated users only ?? (not exposed by V0 but needed for "find")
            String sharedByUserId = (String)nodeProps.get(QuickShareModel.PROP_QSHARE_SHAREDBY);
            UserInfo sharedByUser = Node.lookupUserInfo(sharedByUserId, mapUserInfo, personService, displayNameOnly);

            QuickShareLink qs = new QuickShareLink(sharedId, nodeRef.getId());
            qs.setName((String) map.get("name"));
            qs.setContent(contentInfo);
            qs.setModifiedAt((Date) map.get("modified"));
            qs.setModifiedByUser(modifiedByUser);
            qs.setSharedByUser(sharedByUser);

            // note: if noAuth mode then do not return allowable operations (eg. but can be optionally returned when finding shared links)
            if ((! noAuth) && includeParam.contains(PARAM_INCLUDE_ALLOWABLEOPERATIONS))
            {
                if (canDeleteSharedLink(nodeRef, sharedByUserId))
                {
                    qs.setAllowableOperations(Collections.singletonList(Nodes.OP_DELETE));
                }
            }

            return qs;
        }
        catch (InvalidSharedIdException ex)
        {
            logger.warn("Unable to find: " + sharedId);
            throw new EntityNotFoundException(sharedId);
        }
        catch (InvalidNodeRefException inre)
        {
            logger.warn("Unable to find: " + sharedId + " [" + inre.getNodeRef() + "]");
            throw new EntityNotFoundException(sharedId);
        }
    }

    // TODO push down to QuickShareService and also update v0 webscript (UnshareContentDelete)
    private boolean canDeleteSharedLink(NodeRef nodeRef, String sharedByUserId)
    {
        boolean canDeleteSharedLink = false;

        String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
        String siteName = getSiteName(nodeRef);

        if (siteName != null)
        {
            // node belongs to a site - current user must be a manager or collaborator (irrespective of whether they shared the link or not)
            String role = siteService.getMembersRole(siteName, currentUser);
            if (role.equals(SiteModel.SITE_MANAGER) || role.equals(SiteModel.SITE_COLLABORATOR))
            {
                canDeleteSharedLink = true;
            }
        }
        else if ((currentUser.equals(sharedByUserId)) || (authorityService.isAdminAuthority(currentUser)))
        {
            // node does not belongs to a site - current user must be the person who shared the link or an admin
            canDeleteSharedLink = true;
        }

        return canDeleteSharedLink;
    }

    private String getSiteName(NodeRef nodeRef)
    {
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        while (parent != null && !nodeService.getType(parent).equals(SiteModel.TYPE_SITE))
        {
            // check that we can read parent name
            String parentName = (String) nodeService.getProperty(parent, ContentModel.PROP_NAME);

            if (nodeService.getPrimaryParent(nodeRef) != null)
            {
                parent = nodeService.getPrimaryParent(parent).getParentRef();
            }
        }

        if (parent == null)
        {
            return null;
        }

        return nodeService.getProperty(parent, ContentModel.PROP_NAME).toString();
    }

    private void checkEnabled()
    {
        if (!enabled)
        {
            throw new DisabledServiceException(DISABLED);
        }
    }

    private void checkValidShareId(String sharedId)
    {
        if (sharedId==null)
        {
            throw new InvalidArgumentException("A valid sharedId must be specified !");
        }
    }

    private void validateEmailRequest(QuickShareLinkEmailRequest emailRequest)
    {
        if (StringUtils.isEmpty(emailRequest.getClient()))
        {
            throw new InvalidArgumentException("A valid client must be specified.");
        }
        if (emailRequest.getRecipientEmails() == null || emailRequest.getRecipientEmails().isEmpty())
        {
            throw new InvalidArgumentException("A valid recipientEmail must be specified.");
        }
    }
}