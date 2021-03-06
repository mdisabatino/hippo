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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;

class ReferenceEditor extends Panel {
    private static final long serialVersionUID = 1L;

    private static Pattern pattern = Pattern.compile("^\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}$");

    ReferenceEditor(String id, JcrPropertyModel propertyModel, JcrPropertyValueModel valueModel) {
        super(id);
        String stringValue = "";
        boolean isProtected = false;
        try {
            stringValue = valueModel.getValue().getString();
            isProtected = propertyModel.getProperty().getDefinition().isProtected();
            Session session = UserSession.get().getJcrSession();
            Node targetNode = session.getNodeByIdentifier(stringValue);

            // link to referenced node
            AjaxLink link = new AjaxLink("reference-link", new JcrNodeModel(targetNode)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget requestTarget) {
                    EditorPlugin plugin = (EditorPlugin) findParent(EditorPlugin.class);
                    plugin.setDefaultModel((JcrNodeModel) getModel());
                }
            };
            add(link);
            link.add(new Label("reference-link-text", new Model(targetNode.getPath())));

            // input field
            if (isProtected) {
                add(new Label("reference-edit", stringValue));
            } else {
                TextFieldWidget editor = new TextFieldWidget("reference-edit", valueModel);
                editor.setSize("40");
                add(editor);
            }

        } catch (ItemNotFoundException e) {
            if (isProtected) {
                add(new Label("reference-edit", stringValue));
            } else {
                TextFieldWidget editor = new TextFieldWidget("reference-edit", valueModel);
                editor.setSize("40");
                add(editor);
            }

            DisabledLink link = new DisabledLink("reference-link", new Model("(Broken reference)"));
            link.add(new AttributeAppender("style", new Model("color:red"), " "));
            add(link);

        } catch (RepositoryException e) {
            add(new Label("reference-edit", e.getClass().getName()));
            add(new DisabledLink("reference-link", new Model(e.getMessage())));
        }
    }

    private class DisabledLink extends AjaxLink {
        private static final long serialVersionUID = 1L;

        public DisabledLink(String id, IModel linktext) {
            super(id);
            setEnabled(false);
            add(new Label("reference-link-text", linktext));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
        }

    }

}
