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
package org.hippoecm.repository.decorating.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.security.AccessControlException;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jackrabbit.rmi.client.ClientSession;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.remote.RemoteServicingXASession;
import org.onehippo.repository.security.User;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ClientServicingXASession extends ClientSession implements HippoSession {

    private RemoteServicingXASession remote;

    protected ClientServicingXASession(Repository repository, RemoteServicingXASession remote, LocalServicingAdapterFactory factory) {
        super(repository, remote, factory);
        this.remote = remote;
    }

    @Override
    public Node copy(Node original, String absPath) throws RepositoryException {
        try {
            return getNode(this, remote.copy(original.getPath(), absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                                  NoSuchNodeTypeException, RepositoryException {
        try {
            String absPath = (node != null ? node.getPath() : null);
            return getFactory().getNodeIterator(this, remote.pendingChanges(absPath, nodeType, prune));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException, NoSuchNodeTypeException,
                                                                          RepositoryException {
        return pendingChanges(node, nodeType, false);
    }

    @Override
    public NodeIterator pendingChanges() throws RepositoryException {
        return pendingChanges(null, null, false);
    }

    @Override
    public void exportDereferencedView(String path, OutputStream output, boolean binaryAsLink, boolean noRecurse)
        throws IOException, PathNotFoundException, RepositoryException {
        try {
            byte[] xml = remote.exportDereferencedView(path, binaryAsLink, noRecurse);
            output.write(xml);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public void exportDereferencedView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        try {
            byte[] xml = remote.exportDereferencedView(absPath, binaryAsLink, noRecurse);

            Source source = new StreamSource(new ByteArrayInputStream(xml));
            Result result = new SAXResult(contentHandler);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        } catch (IOException ex) {
            throw new SAXException(ex);
        } catch (TransformerConfigurationException ex) {
            throw new SAXException(ex);
        } catch (TransformerException ex) {
            throw new SAXException(ex);
        }
    }

    @Override
    public void importDereferencedXML(String path, InputStream xml, int uuidBehavior, int referenceBehavior,
            int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException,
            RepositoryException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            for (int n = xml.read(bytes); n != -1; n = xml.read(bytes)) {
                buffer.write(bytes, 0, n);
            }
            remote.importDereferencedXML(path, buffer.toByteArray(), uuidBehavior, referenceBehavior, mergeBehavior);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public ClassLoader getSessionClassLoader() throws RepositoryException {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public User getUser() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public XAResource getXAResource() {
        return new XAResource() {

            public void commit(Xid arg0, boolean arg1) throws XAException {
                try {
                    remote.commit(arg0, arg1);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public void end(Xid arg0, int arg1) throws XAException {
                try {
                    remote.end(arg0, arg1);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public void forget(Xid arg0) throws XAException {
                try {
                    remote.forget(arg0);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public int getTransactionTimeout() throws XAException {
                try {
                    return remote.getTransactionTimeout();
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public boolean isSameRM(XAResource arg0) throws XAException {
                return this == arg0;
            }

            public int prepare(Xid arg0) throws XAException {
                try {
                    return remote.prepare(arg0);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public Xid[] recover(int arg0) throws XAException {
                try {
                    return remote.recover(arg0);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public void rollback(Xid arg0) throws XAException {
                try {
                    remote.rollback(arg0);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public boolean setTransactionTimeout(int arg0) throws XAException {
                try {
                    return remote.setTransactionTimeout(arg0);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }

            public void start(Xid arg0, int arg1) throws XAException {
                try {
                    remote.start(arg0, arg1);
                } catch (RemoteException ex) {
                    throw new XAException(ex.getMessage());
                }
            }
        };
    }

    @Override
    public void checkPermission(String path, String actions)
            throws AccessControlException, RepositoryException {
        try {
            remote.checkPermission(path, actions);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public void registerSessionCloseCallback(CloseCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session createSecurityDelegate(final Session session, DomainRuleExtension... domainExtensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void localRefresh() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableVirtualLayers() {
        throw new UnsupportedOperationException();
    }
}
