/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DefaultMutableResourceBundleFamily
 */
public class DefaultMutableResourceBundleFamily implements MutableResourceBundleFamily {

    private final String basename;
    private ResourceBundle defaultBundle;
    private ResourceBundle defaultBundleForPreview;
    private final Map<Locale, ResourceBundle> localizedBundlesMap = new ConcurrentHashMap(new HashMap<Locale, ResourceBundle>());
    private final Map<Locale, ResourceBundle> localizedBundlesMapForPreview = new ConcurrentHashMap(new HashMap<Locale, ResourceBundle>());

    public DefaultMutableResourceBundleFamily(final String basename) {
        this.basename = basename;
    }

    @Override
    public String getBasename() {
        return basename;
    }

    @Override
    public ResourceBundle getDefaultBundle() {
        return defaultBundle;
    }

    @Override
    public ResourceBundle getDefaultBundleForPreview() {
        return defaultBundleForPreview;
    }

    @Override
    public void setDefaultBundle(ResourceBundle defaultBundle) {
        this.defaultBundle = defaultBundle;
    }

    @Override
    public void setDefaultBundleForPreview(ResourceBundle defaultBundleForPreview) {
        this.defaultBundleForPreview = defaultBundleForPreview;
    }

    @Override
    public ResourceBundle getLocalizedBundle(Locale locale) {
        return localizedBundlesMap.get(locale);
    }

    @Override
    public ResourceBundle getLocalizedBundleForPreview(Locale locale) {
        return localizedBundlesMapForPreview.get(locale);
    }

    @Override
    public void setLocalizedBundle(Locale locale, ResourceBundle bundle) {
        localizedBundlesMap.put(locale, bundle);
    }

    @Override
    public void setLocalizedBundleForPreview(Locale locale, ResourceBundle bundle) {
        localizedBundlesMapForPreview.put(locale, bundle);
    }

    @Override
    public void removeLocalizedBundle(Locale locale) {
        localizedBundlesMap.remove(locale);
    }

    @Override
    public void removeLocalizedBundleForPreview(Locale locale) {
        localizedBundlesMapForPreview.remove(locale);
    }
}
