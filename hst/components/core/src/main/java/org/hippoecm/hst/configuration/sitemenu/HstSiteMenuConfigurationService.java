/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.configuration.sitemenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.internal.StringPool;

public class HstSiteMenuConfigurationService implements HstSiteMenuConfiguration {

    private String name;
    private HstSiteMenusConfiguration hstSiteMenusConfiguration;
    private List<HstSiteMenuItemConfiguration> siteMenuItems = new ArrayList<HstSiteMenuItemConfiguration>();

    public HstSiteMenuConfigurationService(HstSiteMenusConfiguration hstSiteMenusConfiguration, HstNode siteMenu) {
       this.hstSiteMenusConfiguration = hstSiteMenusConfiguration;
       this.name = StringPool.get(siteMenu.getValueProvider().getName());
        for(HstNode siteMenuItem : siteMenu.getNodes()) {
            HstSiteMenuItemConfiguration siteMenuItemConfiguration = new HstSiteMenuItemConfigurationService(siteMenuItem, null, this);
            siteMenuItems.add(siteMenuItemConfiguration);
        }
    }
    

    public String getName() {
        return this.name;
    }

    public List<HstSiteMenuItemConfiguration> getSiteMenuConfigurationItems() {
        return Collections.unmodifiableList(siteMenuItems);
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return hstSiteMenusConfiguration;
    }

}
