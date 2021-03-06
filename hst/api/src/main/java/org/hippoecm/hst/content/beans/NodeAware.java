/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.content.beans;

import javax.jcr.Node;

/**
 * Interface to be implemented by beans that wish to be aware of its originating JCR node.
 * 
 * @version $Id: NodeAware.java 37810 2013-01-10 13:26:30Z mdenburger $
 */
public interface NodeAware {

    /**
     * Callback that supplies the originating JCR node.
     * 
     * @param node
     */
    void setNode(Node node);
    
}
