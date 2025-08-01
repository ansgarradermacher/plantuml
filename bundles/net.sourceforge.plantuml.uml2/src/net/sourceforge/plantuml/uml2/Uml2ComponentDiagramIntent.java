/**
 * Copyright (c) 2025 CEA LIST and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package net.sourceforge.plantuml.uml2;

import static net.sourceforge.plantuml.uml2.IndentUtils.indentBlock;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ConnectableElement;
import org.eclipse.uml2.uml.Connector;
import org.eclipse.uml2.uml.ConnectorEnd;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import net.sourceforge.plantuml.text.AbstractClassDiagramIntent;

/**
 * Produce a PlantUML component diagram from a UML package
 * TODO: PlantUML currently does not have a support for parts. Apply workaround described
 * in https://forum.plantuml.net/18813/support-for-composite-structure-diagram-notably-parts
 */
public class Uml2ComponentDiagramIntent extends AbstractClassDiagramIntent<Collection<Class>> {

	private Map<String, String> skinParams = null;

	private Map<Classifier, Boolean> alreadyHandled = null;

	protected Uml2ComponentDiagramIntent(final Collection<Class> source) {
		super(source, "Component diagram"); //$NON-NLS-1$
	}

	public Uml2ComponentDiagramIntent(final Class source) {
		this(Collections.singletonList(source));
	}

	@Override
	public String getDiagramText() {
		alreadyHandled = new HashMap<Classifier, Boolean>();
		return getDiagramText("", getSource().iterator().next());
	}

	/**
	 * @param prefix handle "parts", i.e. kind of instances
	 * @param clazz the class/component to produce data for
	 * @return the string
	 */
	protected String getDiagramText(String prefix, Class clazz) {
		final StringBuilder buffer = new StringBuilder();
		if (skinParams != null) {
			appendSkinParams(skinParams, buffer);
		}
		String name = prefix + clazz.getName();
		if (name.contains(" ")) {
			buffer.append(String.format("component \"%s\" {\n", name));
		} else {
			buffer.append(String.format("component %s {\n", name));
		}
		buffer.append(StereotypeUtils.stereoNames(clazz,  false));
		for (Property port : clazz.getAllAttributes()) {
			if (port instanceof Port) {
				String portDecl = String.format("port %s", port.getName());
				if (prefix.length() > 0) {
					portDecl = String.format("%s as %s\n", portDecl, prefix.replace(" ", "").replace(":", ".") + port.getName());		
				}
				indentBlock(buffer, String.format("%s\n", portDecl));
			}
		}

		for (final Property attribute : clazz.getAllAttributes()) {
			if (attribute instanceof Port) {
				continue;
			}
			final Type type = attribute.getType();
			if (type instanceof Class) {
				// TODO: combine hierarchically with prefix?
				String atName = String.format("%s: ", attribute.getName());
				// the alreadyHandled list avoids a potential stack overflow, if attribute within a class is typed with this class
				if (!alreadyHandled.containsKey(type)) {
					String sub = getDiagramText(atName, (Class) type);
					alreadyHandled.put((Classifier) type, true);
					indentBlock(buffer, sub);
				}
			}
		}
		for (final Connector connector : clazz.getOwnedConnectors()) {
			if (connector.getEnds().size() == 2) {
				String txtS = getPortRef(connector.getEnds().get(0));
				String txtT = getPortRef(connector.getEnds().get(1));
				indentBlock(buffer, String.format("%s -- %s\n", txtS, txtT));
			}
		}


		buffer.append("}\n");
		return buffer.toString();
	}

	protected String getPortRef(ConnectorEnd end) {
		Property part = end.getPartWithPort();
		ConnectableElement port = end.getRole();
		if (part != null) {
			return String.format("%s.%s", NamingUtils.refName(part.getName()), NamingUtils.refName(port.getName()));
		}
		else {
			return NamingUtils.refName(port.getName());
		}	
	}
	
	protected void appendGeneralisation(final Classifier subClass, final Classifier superClass, final boolean isImplements, final StringBuilder buffer) {
		appendGeneralisation(subClass.getName(), superClass.getName(), isImplements, buffer);
	}

	protected String getClassifierColor(final Classifier classifier) {
		return null;
	}
}
