/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
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
package org.alfresco.module.vti.web.actions;

import org.alfresco.module.vti.handler.alfresco.UrlHelper;
import org.alfresco.module.vti.web.VtiAction;
import org.alfresco.module.vti.web.fp.PropfindMethod;
import org.alfresco.repo.webdav.WebDAVMethod;

/**
* Executes the WebDAV PROPFIND method with VTI specific
*
* @author PavelYur
*
*/
public class VtiPropfindAction extends VtiWebDavAction implements VtiAction
{
    private UrlHelper urlHelper;
    
    @Override
    public WebDAVMethod getWebDAVMethod()
    {
        return new PropfindMethod(pathHelper, urlHelper);
    }

    public void setUrlHelper(UrlHelper urlHelper)
    {
        this.urlHelper = urlHelper;
    }
}


