/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.jcr2spi.SessionImpl;
import org.apache.jackrabbit.jcr2spi.util.ReferenceChangeTracker;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.OperationVisitor;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.Clone;
import org.apache.jackrabbit.jcr2spi.operation.Copy;
import org.apache.jackrabbit.jcr2spi.operation.Move;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetPropertyValue;
import org.apache.jackrabbit.jcr2spi.operation.ReorderNodes;
import org.apache.jackrabbit.jcr2spi.operation.Checkout;
import org.apache.jackrabbit.jcr2spi.operation.Checkin;
import org.apache.jackrabbit.jcr2spi.operation.Update;
import org.apache.jackrabbit.jcr2spi.operation.Restore;
import org.apache.jackrabbit.jcr2spi.operation.ResolveMergeConflict;
import org.apache.jackrabbit.jcr2spi.operation.Merge;
import org.apache.jackrabbit.jcr2spi.operation.LockOperation;
import org.apache.jackrabbit.jcr2spi.operation.LockRefresh;
import org.apache.jackrabbit.jcr2spi.operation.LockRelease;
import org.apache.jackrabbit.jcr2spi.operation.AddLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveVersion;
import org.apache.jackrabbit.jcr2spi.operation.WorkspaceImport;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.MergeException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.lock.LockException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;

/**
 * <code>HippoSessionItemStateManager</code> ...
 */
public class HippoSessionItemStateManager extends SessionItemStateManager implements UpdatableItemStateManager, OperationVisitor {

    private static Logger log = LoggerFactory.getLogger(SessionItemStateManager.class);

    /**
     * State manager that allows updates
     */
    private final UpdatableItemStateManager workspaceItemStateMgr;

    /**
     * State manager for the transient items
     */
    private final TransientItemStateManager transientStateMgr;

    private final ItemStateValidator validator;

    private final QValueFactory qValueFactory;

    private final ManagerProvider mgrProvider;

    /**
     * Creates a new <code>HippoSessionItemStateManager</code> instance.
     *
     * @param workspaceItemStateMgr
     * @param validator
     * @param qValueFactory
     * @param isf
     * @param mgrProvider
     */
    public HippoSessionItemStateManager(UpdatableItemStateManager workspaceItemStateMgr,
                                   ItemStateValidator validator,
                                   QValueFactory qValueFactory,
                                   ItemStateFactory isf, SessionImpl mgrProvider) {
        super(workspaceItemStateMgr, validator, qValueFactory, isf, mgrProvider);

        this.workspaceItemStateMgr = workspaceItemStateMgr;
        this.transientStateMgr = new TransientItemStateManager();
        isf.addCreationListener(transientStateMgr);

        this.validator = validator;
        this.qValueFactory = qValueFactory;
        this.mgrProvider = mgrProvider;
    }

    /**
     * @return <code>true</code> if this manager has any transient state;
     * <code>false</code> otherwise.
     */
    public boolean hasPendingChanges() {
        return transientStateMgr.hasPendingChanges();
    }

    public java.util.Set pendingChanges(ItemState target) throws InvalidItemStateException, ConstraintViolationException, RepositoryException  {
        return transientStateMgr.getChangeLog(target, false).getAffectedStates();
    }

    /**
     * This will save <code>state</code> and all descendants items of
     * <code>state</code> that are transiently modified in a single step. If
     * this operation fails, no item will have been saved.
     *
     * @param state the root state of the update operation
     */
    public void save(ItemState state) throws ReferentialIntegrityException,
            InvalidItemStateException, RepositoryException {
        // shortcut, if no modifications are present
        if (!transientStateMgr.hasPendingChanges()) {
            return;
        }
        // collect the changes to be saved
        ChangeLog changeLog = transientStateMgr.getChangeLog(state, true);
        if (!changeLog.isEmpty()) {
            // only pass changelog if there are transient modifications available
            // for the specified item and its decendants.
            workspaceItemStateMgr.execute(changeLog);
            // remove states and operations just processed from the transient ISM
            transientStateMgr.dispose(changeLog);
            // now its save to clear the changeLog
            changeLog.reset();
        }
    }

    /**
     * This will undo all changes made to <code>state</code> and descendant
     * items of <code>state</code> inside this item state manager.
     *
     * @param itemState the root state of the cancel operation.
     * @throws ConstraintViolationException
     * @throws RepositoryException if undoing changes made to <code>state</code>
     * and descendant items is not a closed set of changes. That is, at least
     * another item needs to be canceled as well in another sub-tree.
     */
    public void undo(ItemState itemState) throws ConstraintViolationException, RepositoryException {
        // short cut
        if (!transientStateMgr.hasPendingChanges()) {
            return;
        }
        ChangeLog changeLog = transientStateMgr.getChangeLog(itemState, false);
        if (!changeLog.isEmpty()) {
            // let changelog revert all changes
            changeLog.undo();
            // remove transient states and related operations from the t-statemanager
            transientStateMgr.dispose(changeLog);
            changeLog.reset();
        }
    }

    /**
     * Adjust references at the end of a successful
     * {@link Session#importXML(String, InputStream, int) XML import}.
     *
     * @param refTracker
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void adjustReferences(ReferenceChangeTracker refTracker) throws ConstraintViolationException, RepositoryException {
        Iterator it = refTracker.getReferences();
        while (it.hasNext()) {
            PropertyState propState = (PropertyState) it.next();
            boolean modified = false;
            QValue[] values = propState.getValues();
            QValue[] newVals = new QValue[values.length];
            for (int i = 0; i < values.length; i++) {
                QValue val = values[i];
                QValue adjusted = refTracker.getMappedReference(val, qValueFactory);
                if (adjusted != null) {
                    newVals[i] = adjusted;
                    modified = true;
                } else {
                    // reference doesn't need adjusting, just copy old value
                    newVals[i] = val;
                }
            }
            if (modified) {
                int options = ItemStateValidator.CHECK_LOCK |
                    ItemStateValidator.CHECK_VERSIONING |
                    ItemStateValidator.CHECK_CONSTRAINTS;
                setPropertyStateValue(propState, newVals, PropertyType.REFERENCE, options);
            }
        }
        // make sure all entries are removed
        refTracker.clear();
    }

    //------------------------------------------< UpdatableItemStateManager >---
    /**
     * {@inheritDoc}
     * @see UpdatableItemStateManager#execute(Operation)
     */
    public void execute(Operation operation) throws RepositoryException {
        operation.accept(this);
    }

    /**
     * {@inheritDoc}
     * @see UpdatableItemStateManager#execute(ChangeLog)
     */
    public void execute(ChangeLog changes) throws RepositoryException {
        throw new UnsupportedOperationException("Not implemented for SessionItemStateManager");
    }

    /**
     * {@inheritDoc}
     * @see UpdatableItemStateManager#dispose()
     */
    public void dispose() {
        // discard all transient changes
        transientStateMgr.dispose();
        // dispose our (i.e. 'local') state manager
        workspaceItemStateMgr.dispose();
        super.dispose();
    }

    //---------------------------------------------------< OperationVisitor >---
    /**
     * @inheritDoc
     * @see OperationVisitor#visit(AddNode)
     */
    public void visit(AddNode operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_COLLISION
            | ItemStateValidator.CHECK_VERSIONING | ItemStateValidator.CHECK_CONSTRAINTS;

        NodeState parent = operation.getParentState();
        ItemDefinitionProvider defProvider = mgrProvider.getItemDefinitionProvider();
        QNodeDefinition def = defProvider.getQNodeDefinition(parent.getAllNodeTypeNames(), operation.getNodeName(), operation.getNodeTypeName());
        List newStates = addNodeState(parent, operation.getNodeName(), operation.getNodeTypeName(), operation.getUuid(), def, options);
        operation.addedState(newStates);

        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     * @see OperationVisitor#visit(AddProperty)
     */
    public void visit(AddProperty operation) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        NodeState parent = operation.getParentState();
        Name propertyName = operation.getPropertyName();
        QPropertyDefinition pDef = operation.getDefinition();
        int targetType = pDef.getRequiredType();
        if (targetType == PropertyType.UNDEFINED) {
            targetType = operation.getPropertyType();
            if (targetType == PropertyType.UNDEFINED) {
                targetType = PropertyType.STRING;
            }
        }
        int options = ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_COLLISION
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;
        addPropertyState(parent, propertyName, targetType, operation.getValues(), pDef, options);

        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     * @see OperationVisitor#visit(Move)
     */
    public void visit(Move operation) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {

        // retrieve states and assert they are modifiable
        NodeState srcState = operation.getSourceState();
        NodeState srcParent = operation.getSourceParentState();
        NodeState destParent = operation.getDestinationParentState();

        // state validation: move-Source can be removed from old/added to new parent
        validator.checkRemoveItem(srcState,
            ItemStateValidator.CHECK_ACCESS
            | ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS);
        validator.checkAddNode(destParent, operation.getDestinationName(),
            srcState.getNodeTypeName(),
            ItemStateValidator.CHECK_ACCESS
            | ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS);

        // retrieve applicable definition at the new place
        ItemDefinitionProvider defProvider = mgrProvider.getItemDefinitionProvider();
        QNodeDefinition newDefinition = defProvider.getQNodeDefinition(destParent.getAllNodeTypeNames(), operation.getDestinationName(), srcState.getNodeTypeName());

        // perform the move (modifying states)
        srcParent.moveChildNodeEntry(destParent, srcState, operation.getDestinationName(), newDefinition);

        // remember operation
        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     * @see OperationVisitor#visit(Remove)
     */
    public void visit(Remove operation) throws ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        ItemState state = operation.getRemoveState();
        int options = ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;
        removeItemState(state, options);
        
        transientStateMgr.addOperation(operation);
        operation.getParentState().markModified();
    }

    /**
     * @inheritDoc
     * @see OperationVisitor#visit(SetMixin)
     */
    public void visit(SetMixin operation) throws ConstraintViolationException, AccessDeniedException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        // NOTE: nodestate is only modified upon save of the changes!
        Name[] mixinNames = operation.getMixinNames();
        NodeState nState = operation.getNodeState();
        NodeEntry nEntry = nState.getNodeEntry();

        // assert the existence of the property entry and set the array of
        // mixinNames to be set on the corresponding property state
        PropertyEntry mixinEntry = nEntry.getPropertyEntry(NameConstants.JCR_MIXINTYPES);
        if (mixinNames.length > 0) {
            // update/create corresponding property state
            if (mixinEntry != null) {
                // execute value of existing property
                PropertyState pState = mixinEntry.getPropertyState();
                int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
                setPropertyStateValue(pState, getQValues(mixinNames, qValueFactory), PropertyType.NAME, options);
            } else {
                // create new jcr:mixinTypes property
                ItemDefinitionProvider defProvider = mgrProvider.getItemDefinitionProvider();
                QPropertyDefinition pd = defProvider.getQPropertyDefinition(nState.getAllNodeTypeNames(), NameConstants.JCR_MIXINTYPES, PropertyType.NAME, true);
                QValue[] mixinValue = getQValues(mixinNames, qValueFactory);
                int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
                addPropertyState(nState, pd.getName(), pd.getRequiredType(), mixinValue, pd, options);
            }
            nState.markModified();
            transientStateMgr.addOperation(operation);
        } else if (mixinEntry != null) {
            // remove the jcr:mixinTypes property state if already present
            PropertyState pState = mixinEntry.getPropertyState();
            boolean newMixinState = pState.getStatus() == Status.NEW;
            int options = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_VERSIONING;
            removeItemState(pState, options);

            nState.markModified();
            transientStateMgr.addOperation(operation);
        } // else: empty Name array and no mixin-prop-entry (should not occur)
    }

    /**
     * @inheritDoc
     * @see OperationVisitor#visit(SetPropertyValue)
     */
    public void visit(SetPropertyValue operation) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        PropertyState pState = operation.getPropertyState();
        int options = ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;
        setPropertyStateValue(pState, operation.getValues(), operation.getValueType(), options);
        transientStateMgr.addOperation(operation);
    }

    /**
     * @inheritDoc
     * @see OperationVisitor#visit(ReorderNodes)
     */
    public void visit(ReorderNodes operation) throws ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        NodeState parent = operation.getParentState();
        // modify the parent node state
        parent.reorderChildNodeEntries(operation.getInsertNode(), operation.getBeforeNode());
        // remember the operation
        transientStateMgr.addOperation(operation);
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Clone)
     */
    public void visit(Clone operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Clone cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Clone)
     */
    public void visit(Copy operation) throws NoSuchWorkspaceException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Copy cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Clone)
     */
    public void visit(Checkout operation) throws RepositoryException, UnsupportedRepositoryOperationException {
        throw new UnsupportedOperationException("Internal error: Checkout cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Clone)
     */
    public void visit(Checkin operation) throws UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Checkin cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Update)
     */
    public void visit(Update operation) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Update cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Restore)
     */
    public void visit(Restore operation) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Restore cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(Merge)
     */
    public void visit(Merge operation) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Merge cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(ResolveMergeConflict)
     */
    public void visit(ResolveMergeConflict operation) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Update cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(LockOperation)
     */
    public void visit(LockOperation operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: Lock cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(LockRefresh)
     */
    public void visit(LockRefresh operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: LockRefresh cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(LockRelease)
     */
    public void visit(LockRelease operation) throws AccessDeniedException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: LockRelease cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(AddLabel)
     */
    public void visit(AddLabel operation) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: AddLabel cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(RemoveLabel)
     */
    public void visit(RemoveLabel operation) throws VersionException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: RemoveLabel cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(RemoveVersion)
     */
    public void visit(RemoveVersion operation) throws VersionException, AccessDeniedException, ReferentialIntegrityException, RepositoryException {
        throw new UnsupportedOperationException("Internal error: RemoveVersion cannot be handled by session ItemStateManager.");
    }

    /**
     * @throws UnsupportedOperationException
     * @see OperationVisitor#visit(WorkspaceImport)
     */
    public void visit(WorkspaceImport operation) throws RepositoryException {
        throw new UnsupportedOperationException("Internal error: WorkspaceImport cannot be handled by session ItemStateManager.");
    }

    //--------------------------------------------< Internal State Handling >---
    /**
     *
     * @param parent
     * @param propertyName
     * @param propertyType
     * @param values
     * @param pDef
     * @param options int used to validate the given params. Note, that the options
     * differ depending if the 'addProperty' is called regularly or to create
     * auto-created (or protected) properties.
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws ItemExistsException
     * @throws NoSuchNodeTypeException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws RepositoryException
     */
    private PropertyState addPropertyState(NodeState parent, Name propertyName,
                                  int propertyType, QValue[] values,
                                  QPropertyDefinition pDef, int options)
            throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {

        validator.checkAddProperty(parent, propertyName, pDef, options);
        // create property state
        return transientStateMgr.createNewPropertyState(propertyName, parent, pDef, values, propertyType);
    }

    private List addNodeState(NodeState parent, Name nodeName, Name nodeTypeName,
                              String uuid, QNodeDefinition definition, int options)
            throws RepositoryException, ConstraintViolationException, AccessDeniedException,
            UnsupportedRepositoryOperationException, NoSuchNodeTypeException,
            ItemExistsException, VersionException {

        // check if add node is possible. note, that the options differ if
        // the 'addNode' is called from inside a regular add-node to create
        // autocreated child nodes that may be 'protected'.
        validator.checkAddNode(parent, nodeName, nodeTypeName, options);
        // a new NodeState doesn't have mixins defined yet -> ent is ent of primarytype
        EffectiveNodeType ent = mgrProvider.getEffectiveNodeTypeProvider().getEffectiveNodeType(nodeTypeName);

        if (nodeTypeName == null) {
            // no primary node type specified,
            // try default primary type from definition
            nodeTypeName = definition.getDefaultPrimaryType();
            if (nodeTypeName == null) {
                String msg = "an applicable node type could not be determined for " + nodeName;
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }

        List addedStates = new ArrayList();

        // create new nodeState. NOTE, that the uniqueID is not added to the
        // state for consistency between 'addNode' and importXML
        NodeState nodeState = transientStateMgr.createNewNodeState(nodeName, null, nodeTypeName, definition, parent);
        addedStates.add(nodeState);
        if (uuid != null) {
            QValue[] value = getQValues(uuid, qValueFactory);
            ItemDefinitionProvider defProvider = mgrProvider.getItemDefinitionProvider();
            QPropertyDefinition pDef = defProvider.getQPropertyDefinition(NameConstants.MIX_REFERENCEABLE, NameConstants.JCR_UUID, PropertyType.STRING, false);
            addedStates.add(addPropertyState(nodeState, NameConstants.JCR_UUID, PropertyType.STRING, value, pDef, 0));
        }

        // add 'auto-create' properties defined in node type
        QPropertyDefinition[] pda = ent.getAutoCreateQPropertyDefinitions();
        for (int i = 0; i < pda.length; i++) {
            QPropertyDefinition pd = pda[i];
            if (!nodeState.hasPropertyName(pd.getName())) {
                QValue[] autoValue = computeSystemGeneratedPropertyValues(nodeState, pd);
                if (autoValue != null) {
                    int propOptions = ItemStateValidator.CHECK_NONE;
                    // execute 'addProperty' without adding operation.
                    addedStates.add(addPropertyState(nodeState, pd.getName(), pd.getRequiredType(), autoValue, pd, propOptions));
                }
            }
        }

        // recursively add 'auto-create' child nodes defined in node type
        QNodeDefinition[] nda = ent.getAutoCreateQNodeDefinitions();
        for (int i = 0; i < nda.length; i++) {
            QNodeDefinition nd = nda[i];
            // execute 'addNode' without adding the operation.
            int opt = ItemStateValidator.CHECK_LOCK | ItemStateValidator.CHECK_COLLISION;
            addedStates.addAll(addNodeState(nodeState, nd.getName(), nd.getDefaultPrimaryType(), null, nd, opt));
        }
        return addedStates;
    }

    private void removeItemState(ItemState itemState, int options) throws RepositoryException {
        validator.checkRemoveItem(itemState, options);
        // recursively remove the given state and all child states.
        boolean success = false;
        try {
            itemState.getHierarchyEntry().transientRemove();
            success = true;
        } finally {
            if (!success) {
                // TODO: TOBEFIXED undo state modifications
            }
        }
    }

    /**
     *
     * @param propState
     * @param iva
     * @param valueType
     * @throws ValueFormatException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws ItemExistsException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws RepositoryException
     */
    private void setPropertyStateValue(PropertyState propState, QValue[] iva,
                                       int valueType, int options)
        throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        // assert that the property can be modified.
        validator.checkSetProperty(propState, options);
        propState.setValues(iva, valueType);
    }

    /**
     * Computes the values of well-known system (i.e. protected) properties
     * as well as auto-created properties which define default value(s)
     *
     * @param parent
     * @param def
     * @return the computed values
     */
    private QValue[] computeSystemGeneratedPropertyValues(NodeState parent,
                                                          QPropertyDefinition def) throws RepositoryException {
        QValue[] genValues = null;
        QValue[] qDefaultValues = def.getDefaultValues();
        if (qDefaultValues != null && qDefaultValues.length > 0) {
            genValues = qDefaultValues;
        } else if (def.isAutoCreated()) {
            // handle known predefined nodetypes that declare auto-created
            // properties without default values
            Name declaringNT = def.getDeclaringNodeType();
            Name name = def.getName();

            if (NameConstants.NT_BASE.equals(declaringNT) && NameConstants.JCR_PRIMARYTYPE.equals(name)) {
                // jcr:primaryType property
                genValues = new QValue[]{qValueFactory.create(parent.getNodeTypeName())};
            } else if (NameConstants.NT_BASE.equals(declaringNT) && NameConstants.JCR_MIXINTYPES.equals(name)) {
                // jcr:mixinTypes property
                Name[] mixins = parent.getMixinTypeNames();
                genValues = getQValues(mixins, qValueFactory);
            }
            else {
                // ask the SPI implementation for advice
                genValues = qValueFactory.computeAutoValues(def);
            }
        }
        return genValues;
    }

    /**
     * @param qNames
     * @param factory
     * @return An array of QValue objects from the given <code>Name</code>s
     */
    private static QValue[] getQValues(Name[] qNames, QValueFactory factory) throws RepositoryException {
        QValue[] ret = new QValue[qNames.length];
        for (int i = 0; i < qNames.length; i++) {
            ret[i] = factory.create(qNames[i]);
        }
        return ret;
    }

    private static QValue[] getQValues(String uniqueID, QValueFactory factory) throws RepositoryException {
        return new QValue[] {factory.create(uniqueID, PropertyType.STRING)};
    }
}
