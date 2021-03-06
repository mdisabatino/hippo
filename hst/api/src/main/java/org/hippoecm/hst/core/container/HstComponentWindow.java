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
package org.hippoecm.hst.core.container;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseState;

/**
 * HST Component Window.
 * This interface represents a fragment window of a page which is generated by a HstComponent.
 * 
 * @version $Id: HstComponentWindow.java 37754 2013-01-09 16:14:45Z mdenburger $
 */
public interface HstComponentWindow {

    /**
     * The name of the component window.
     * 
     * @return the name of the component window
     */
    String getName();
    
    /**
     * The reference name of the component window.
     * 
     * @return the reference name of the component window
     */
    String getReferenceName();
    
    /**
     * The reference namespace of the component window.
     * 
     * @return the reference namespace of the component window
     */
    String getReferenceNamespace();

    /**
     * The HstComponent name. Normally the component name is fully qualified class name.
     * 
     * @return the HstComponent name
     */
    String getComponentName();
    
    /**
     * The actual HstComponent instance.
     * 
     * @return the actual HstComponent instance
     */
    HstComponent getComponent();
    
    /**
     * The metadata of the actual HstComponent class.
     * 
     * @return the metadata of the actual HstComponent class.
     */
    HstComponentMetadata getComponentMetadata();
    
    /**
     * Whether it has component exceptions or not
     * 
     * @return
     */
    boolean hasComponentExceptions();
    
    /**
     * The component exceptions during initialization or runtime.
     * 
     * @return the possible component exception list
     */
    List<HstComponentException> getComponentExceptions();
    
    /**
     * Adds a component exceptions during initialization or runtime.
     */
    void addComponentExcpetion(HstComponentException e);
    
    /**
     * Adds a component exceptions during initialization or runtime.
     */
    void clearComponentExceptions();
    
    /**
     * The dispatching path path to render this component window.
     * 
     * @return the dispatching path to render this component window
     */
    String getRenderPath();
    
    /**
     * @see #getRenderPath()
     * @return the name of the renderer, when using named servlet. Returns <code>null</code> when {@link #getRenderPath()} does not return <code>null</code>
     */
    String getNamedRenderer();
    
    /**
     * The dispatching path path to serve resource in this component window.
     * 
     * @return the dispatching path to serve resource in this component window
     */
    String getServeResourcePath();
    
    /**
     * @see #getServeResourcePath()
     * @return the name of the resource server, when using named servlet. Returns <code>null</code> when {@link #getServeResourcePath()} does not return <code>null</code>
     */
    String getNamedResourceServer();
    
    /**
     * @see {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration#getParameter(String)}
     * @param name the name of the parameter
     * @return the configured parameter value for this <code>name</code> and <code>null</code> if not existing
     */
    String getParameter(String name);
    
    /**
     * @see {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration#getLocalParameter(String)}
     * @param paramName the name of the parameter
     * @return the configured parameter value for this <code>name</code> and <code>null</code> if not existing
     */
    String getLocalParameter(String paramName);
    
    /**
     * The parent component window containing this component window.
     * 
     * @return the component window containing this component window, or <code>null</code> when there is no parent window
     */
    HstComponentWindow getParentWindow();
    
    /**
     * The child component windows contained in this component window.
     * 
     * @return the component windows contained in this component window
     */
    Map<String, HstComponentWindow> getChildWindowMap();
    
    /**
     * The child component window names contained in this component window.
     * 
     * @return the component window names contained in this component window
     */
    List<String> getChildWindowNames();
    
    /**
     * The child component window which can be accessed by the name.
     * 
     * @param name the name of the child component window
     * @return the child component window which has the referenceName
     */
    HstComponentWindow getChildWindow(String name);
    
    /**
     * The child component window which can be accessed by the reference name.
     * 
     * @param referenceName the referenceName of the child component window
     * @return the child component window which has the referenceName
     */
    HstComponentWindow getChildWindowByReferenceName(String referenceName);
    
    /**
     * Returns the response state of this component window
     */
    HstResponseState getResponseState();
    
    /**
     * 
     * @return
     */
    HstComponentInfo getComponentInfo();
    
    /**
     * 
     * @param name
     * @return
     */
    Object getAttribute(String name);
    
    /**
     * 
     * @param name
     * @param value
     */
    void setAttribute(String name, Object value);
    
    /**
     * 
     * @param name
     * @return
     */
    Object removeAttribute(String name);
    
    /**
     * 
     * @return
     */
    Enumeration<String> getAttributeNames();

    /**
     * @return the fully classified className of the class implementing {@link PageErrorHandler}
     */
    String getPageErrorHandlerClassName();

    /**
     * When this method returns <code>true<code>, the {@link HstComponentWindow} is still part of the hierarchy of {@link HstComponentWindow}s, but
     * the doBeforeRender of the {@link HstComponentWindow#getComponent()} and dispatched from the {@link HstResponse} will be skipped. 
     * @return <code>true</code> when this {@link HstComponentWindow} should be visible, <code>false</code> otherwise
     */
    boolean isVisible();
    
    /**
     * @param visible sets whether this {@link HstComponentWindow} is visible or not. Setting this {@link HstComponentWindow} 
     *                visibility to false, automatically sets all descendant {@link HstComponentWindow}s visibility to false as well
     */
    void setVisible(boolean visible);

}
