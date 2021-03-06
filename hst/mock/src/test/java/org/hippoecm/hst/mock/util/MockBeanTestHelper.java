/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.mock.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Ignore;

@Ignore
public class MockBeanTestHelper {
    
    private MockBeanTestHelper() {
        
    }
    
    public static void verifyReadOnlyProperty(Object bean, String name, Object value) throws Exception {
        assertTrue("The property '" + name + "' of " + bean + " is not readable!", PropertyUtils.isReadable(bean, name));
        assertFalse("The property '" + name + "' of " + bean + " is writeble!", PropertyUtils.isWriteable(bean, name));
        assertEquals("The property, '" + name + "' of " + bean + ", read is different from the write!", value, PropertyUtils.getProperty(bean, name));
    }
    
    public static void verifyWriteOnlyProperty(Object bean, String name, Object value) throws Exception {
        assertFalse("The property '" + name + "' of " + bean + " is readable!", PropertyUtils.isReadable(bean, name));
        assertTrue("The property '" + name + "' of " + bean + " is not writeble!", PropertyUtils.isWriteable(bean, name));
        PropertyUtils.setProperty(bean, name, value);
    }
    
    public static void verifyReadWriteProperty(Object bean, String name, Object value) throws Exception {
        assertTrue("The property '" + name + "' of " + bean + " is not readable!", PropertyUtils.isReadable(bean, name));
        assertTrue("The property '" + name + "' of " + bean + " is not writeble!", PropertyUtils.isWriteable(bean, name));
        PropertyUtils.setProperty(bean, name, value);
        assertEquals("The property, '" + name + "' of " + bean + ", read is different from the write!", value, PropertyUtils.getProperty(bean, name));
    }
}
