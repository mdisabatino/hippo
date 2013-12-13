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
package org.hippoecm.repository.decorating;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.HippoVersionManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.xml.sax.ContentHandler;

/**
 * Simple workspace decorator.
 */
public abstract class WorkspaceDecorator extends AbstractDecorator implements HippoWorkspace {

    /** The underlying workspace instance. */
    protected final Workspace workspace;

    /**
     * Creates a workspace decorator.
     *
     * @param factory
     * @param session
     * @param workspace
     */
    protected WorkspaceDecorator(DecoratorFactory factory, Session session, Workspace workspace) {
        super(factory, session);
        this.workspace = workspace;
    }

    public static Workspace unwrap(Workspace workspace) {
        while (workspace instanceof WorkspaceDecorator) {;
            workspace = ((WorkspaceDecorator)workspace).workspace;
        }
        return workspace;
    }

    /** {@inheritDoc} */
    public Session getSession() {
        return session;
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public String getName() {
        return workspace.getName();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.copy(srcAbsPath, destAbsPath);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException,
            ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        workspace.copy(srcWorkspace, srcAbsPath, destAbsPath);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.move(srcAbsPath, destAbsPath);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException,
            UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException,
            RepositoryException {
        Version[] tmp = new Version[versions.length];
        for (int i = 0; i < versions.length; i++) {
            tmp[i] = VersionDecorator.unwrap(versions[i]);
        }
        workspace.restore(tmp, removeExisting);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public QueryManager getQueryManager() throws RepositoryException {
        return factory.getQueryManagerDecorator(session, workspace.getQueryManager());
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        return workspace.getNamespaceRegistry();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        return workspace.getNodeTypeManager();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        return workspace.getObservationManager();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        return workspace.getAccessibleWorkspaceNames();
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehaviour)
            throws PathNotFoundException, ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        return workspace.getImportContentHandler(parentAbsPath, uuidBehaviour);
    }

    /**
     * Forwards the method call to the underlying workspace.
     */
    public void importXML(String parentAbsPath, InputStream in, int uuidBehaviour) throws IOException,
            PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException,
            LockException, RepositoryException {
        workspace.importXML(parentAbsPath, in, uuidBehaviour);
    }

    public LockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return workspace.getLockManager();
    }

    public void createWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        workspace.createWorkspace(name);
    }

    public void createWorkspace(String name, String srcWorkspace) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        workspace.createWorkspace(name, srcWorkspace);
    }

    public void deleteWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        workspace.deleteWorkspace(name);
    }
}