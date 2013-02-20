/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.project.web;

import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIMessage;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.context.FacesContext;
import javax.faces.validator.BeanValidator;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <strong>UIInputContainer</strong> is a supplemental component for a JSF 2.0 composite component encapsulating one or more
 * input components (<strong>EditableValueHolder</strong>), their corresponding message components (<strong>UIMessage</strong>)
 * and a label (<strong>HtmlOutputLabel</strong>). This component takes care of wiring the label to the first input and the
 * messages to each input in sequence. It also assigns two implicit attribute values, "required" and "invalid" to indicate that
 * a required input field is present and whether there are any validation errors, respectively. To determine if a input field is
 * required, both the required attribute is consulted and whether the property has Bean Validation constraints. Finally, if the
 * "label" attribute is not provided on the composite component, the label value will be derived from the id of the composite
 * component, for convenience.
 * <p/>
 * <p>
 * Composite component definition example (minus layout):
 * </p>
 * <p/>
 * <p/>
 * <pre>
 * &lt;cc:interface componentType="org.jboss.seam.faces.InputContainer"/>
 * &lt;cc:implementation>
 *   &lt;h:outputLabel id="label" value="#{cc.attrs.label}:" styleClass="#{cc.attrs.invalid ? 'invalid' : ''}">
 *     &lt;h:ouputText styleClass="required" rendered="#{cc.attrs.required}" value="*"/>
 *   &lt;/h:outputLabel>
 *   &lt;cc:insertChildren/>
 *   &lt;h:message id="message" errorClass="invalid message" rendered="#{cc.attrs.invalid}"/>
 * &lt;/cc:implementation>
 * </pre>
 * <p/>
 * <p>
 * Composite component usage example:
 * </p>
 * <p/>
 * <p/>
 * <pre>
 * &lt;example:inputContainer id="name">
 *   &lt;h:inputText id="input" value="#{person.name}"/>
 * &lt;/example:inputContainer>
 * </pre>
 * <p/>
 * <p>
 * Possible enhancements:
 * </p>
 * <ul>
 * <li>append styleClass "invalid" to label, inputs and messages when invalid</li>
 * </ul>
 * <p/>
 * <p>
 * NOTE: Firefox does not properly associate a label with the target input if the input id contains a colon (:), the default
 * separator character in JSF. JSF 2 allows developers to set the value via an initialization parameter (context-param in
 * web.xml) keyed to javax.faces.SEPARATOR_CHAR. We recommend that you override this setting to make the separator an underscore
 * (_).
 * </p>
 *
 * @author Dan Allen
 * @author <a href="http://community.jboss.org/people/spinner)">Jose Rodolfo freitas</a>
 */
@FacesComponent(UIInputContainer.COMPONENT_TYPE)
public class UIInputContainer extends UIComponentBase implements NamingContainer {
// ------------------------------ FIELDS ------------------------------

    /**
     * The standard component type for this component.
     */
    public static final String COMPONENT_TYPE = "com.project.web.UIInputContainer";

    protected static final String HTML_CLASS_ATTR_NAME = "class";

    protected static final String HTML_ID_ATTR_NAME = "id";

    protected static final String HTML_STYLE_ATTR_NAME = "style";

    private boolean beanValidationPresent = false;

// --------------------------- CONSTRUCTORS ---------------------------

    public UIInputContainer()
    {
        beanValidationPresent = isClassPresent("javax.validation.Validator");
    }

// -------------------------- OTHER METHODS --------------------------

    // assigning ids seems to break form submissions, but I don't know why
//    public void assignIds(final InputContainerElements elements, final FacesContext context)
//    {
//        boolean refreshIds = false;
//        if (getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
//            setId(elements.getPropertyName(context));
//            refreshIds = true;
//        }
//        UIComponent label = elements.getLabel();
//        if (label != null) {
//            if (label.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
//                label.setId(getDefaultLabelId());
//            } else if (refreshIds) {
//                label.setId(label.getId());
//            }
//        }
//        for (int i = 0, len = elements.getInputs().size(); i < len; i++) {
//            UIComponent input = (UIComponent) elements.getInputs().get(i);
//            if (input.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
//                input.setId(getDefaultInputId() + (i == 0 ? "" : (i + 1)));
//            } else if (refreshIds) {
//                input.setId(input.getId());
//            }
//        }
//        for (int i = 0, len = elements.getMessages().size(); i < len; i++) {
//            UIComponent msg = elements.getMessages().get(i);
//            if (msg.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
//                msg.setId(getDefaultMessageId() + (i == 0 ? "" : (i + 1)));
//            } else if (refreshIds) {
//                msg.setId(msg.getId());
//            }
//        }
//    }

    @Override
    public void encodeBegin(final FacesContext context) throws IOException
    {
        if (!isRendered()) {
            return;
        }

        super.encodeBegin(context);

        InputContainerElements elements = scan(getFacet(UIComponent.COMPOSITE_FACET_NAME), null, context);
        // assignIds(elements, context);
        wire(elements, context);

        getAttributes().put(getElementsAttributeName(), elements);

        if (elements.hasValidationError()) {
            getAttributes().put(getInvalidAttributeName(), true);
        } else {
            getAttributes().put(getInvalidAttributeName(), false);
        }

        // set the required attribute, but only if the user didn't already assign it
        if (!getAttributes().containsKey(getRequiredAttributeName()) && elements.hasRequiredInput()) {
            getAttributes().put(getRequiredAttributeName(), true);
        }

        /*
         * for some reason, Mojarra is not filling Attribute Map with "label" key if label attr has an EL value, so I added a
         * labelHasEmptyValue to guarantee that there was no label setted.
         */
        if (getValueExpression(getLabelAttributeName()) == null && (!getAttributes().containsKey(getLabelAttributeName()) || labelHasEmptyValue(elements))) {
            getAttributes().put(getLabelAttributeName(), generateLabel(elements, context));
        }

        if (Boolean.TRUE.equals(getAttributes().get(getEncloseAttributeName()))) {
            startContainerElement(context);
        }
    }

    @Override
    public void encodeEnd(final FacesContext context) throws IOException
    {
        if (!isRendered()) {
            return;
        }

        super.encodeEnd(context);

        if (Boolean.TRUE.equals(getAttributes().get(getEncloseAttributeName()))) {
            endContainerElement(context);
        }
    }

    public String getContainerElementName()
    {
        return "div";
    }

//    public String getDefaultInputId()
//    {
//        return "input";
//    }
//
//    public String getDefaultLabelId()
//    {
//        return "label";
//    }
//
//    public String getDefaultMessageId()
//    {
//        return "message";
//    }

    /**
     * The name of the auto-generated composite component attribute that holds the elements in this input container. The
     * elements include the label, a list of inputs and a cooresponding list of messages.
     */
    public String getElementsAttributeName()
    {
        return "elements";
    }

    /**
     * The name of the composite component attribute that holds a boolean indicating whether the component template should be
     * enclosed in an HTML element, so that it be referenced from JavaScript.
     */
    public String getEncloseAttributeName()
    {
        return "enclose";
    }

    @Override
    public String getFamily()
    {
        return UINamingContainer.COMPONENT_FAMILY;
    }

    /**
     * The name of the auto-generated composite component attribute that holds a boolean indicating whether the the template
     * contains an invalid input.
     */
    public String getInvalidAttributeName()
    {
        return "invalid";
    }

    /**
     * The name of the composite component attribute that holds the string label for this set of inputs. If the label attribute
     * is not provided, one will be generated from the id of the composite component or, if the id is defaulted, the name of the
     * property bound to the first input.
     */
    public String getLabelAttributeName()
    {
        return "label";
    }

    /**
     * The name of the auto-generated composite component attribute that holds a boolean indicating whether the template
     * contains a required input.
     */
    public String getRequiredAttributeName()
    {
        return "required";
    }

    protected void endContainerElement(final FacesContext context) throws IOException
    {
        context.getResponseWriter().endElement(getContainerElementName());
    }

    protected String generateLabel(final InputContainerElements elements, final FacesContext context)
    {
        String name = getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX) ? elements.getPropertyName(context) : getId();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Get the default Bean Validation Validator to read the contraints for a property.
     */
    private Validator getDefaultValidator(final FacesContext context)
    {
        if (!beanValidationPresent) {
            return null;
        }

        ValidatorFactory validatorFactory;
        Object cachedObject = context.getExternalContext().getApplicationMap().get(BeanValidator.VALIDATOR_FACTORY_KEY);
        if (cachedObject instanceof ValidatorFactory) {
            validatorFactory = (ValidatorFactory) cachedObject;
        } else {
            try {
                validatorFactory = Validation.buildDefaultValidatorFactory();
            } catch (ValidationException e) {
                throw new FacesException("Could not build a default Bean Validator factory", e);
            }
            context.getExternalContext().getApplicationMap().put(BeanValidator.VALIDATOR_FACTORY_KEY, validatorFactory);
        }
        return validatorFactory.getValidator();
    }

    private boolean isClassPresent(final String fqcn)
    {
        try {
            if (Thread.currentThread().getContextClassLoader() != null) {
                return Thread.currentThread().getContextClassLoader().loadClass(fqcn) != null;
            } else {
                return Class.forName(fqcn) != null;
            }
        } catch (ClassNotFoundException e) {
            return false;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    private boolean labelHasEmptyValue(InputContainerElements elements)
    {
        return !(elements.getLabel() == null || elements.getLabel().getValue() == null) && (elements.getLabel().getValue().toString().trim().equals(":")
            || elements.getLabel().getValue().toString().trim().equals(""));
    }

    /**
     * Walk the component tree branch built by the composite component and locate the input container elements.
     *
     * @return a composite object of the input container elements
     */
    protected InputContainerElements scan(final UIComponent component, InputContainerElements elements, final FacesContext context)
    {
        if (elements == null) {
            elements = new InputContainerElements();
        }

        // NOTE we need to walk the tree ignoring rendered attribute because it's condition
        // could be based on what we discover
        if ((elements.getLabel() == null) && (component instanceof HtmlOutputLabel)) {
            elements.setLabel((HtmlOutputLabel) component);
        } else if (component instanceof EditableValueHolder) {
            elements.registerInput((EditableValueHolder) component, getDefaultValidator(context), context);
        } else if (component instanceof UIMessage) {
            elements.registerMessage((UIMessage) component);
        }
        // may need to walk smarter to ensure "element of least suprise"
        for (UIComponent child : component.getChildren()) {
            scan(child, elements, context);
        }

        return elements;
    }

    protected void startContainerElement(final FacesContext context) throws IOException
    {
        context.getResponseWriter().startElement(getContainerElementName(), this);
        String style = (getAttributes().get("style") != null ? getAttributes().get("style").toString().trim() : null);
        if (style != null && style.length() > 0) {
            context.getResponseWriter().writeAttribute(HTML_STYLE_ATTR_NAME, style, HTML_STYLE_ATTR_NAME);
        }
        String styleClass = (getAttributes().get("styleClass") != null ? getAttributes().get("styleClass").toString().trim() : null);
        if (styleClass != null && styleClass.length() > 0) {
            context.getResponseWriter().writeAttribute(HTML_CLASS_ATTR_NAME, styleClass, HTML_CLASS_ATTR_NAME);
        }
        context.getResponseWriter().writeAttribute(HTML_ID_ATTR_NAME, getClientId(context), HTML_ID_ATTR_NAME);
    }

    /**
     * Wire the label and messages to the input(s)
     */
    protected void wire(final InputContainerElements elements, final FacesContext context)
    {
        elements.wire(context);
    }

// -------------------------- INNER CLASSES --------------------------

    public static class InputContainerElements {
// ------------------------------ FIELDS ------------------------------

        private final List<EditableValueHolder> inputs = new ArrayList<EditableValueHolder>();

        private HtmlOutputLabel label;

        private final List<UIMessage> messages = new ArrayList<UIMessage>();

        private String propertyName;

        private boolean requiredInput = false;

        private boolean validationError = false;

// --------------------- GETTER / SETTER METHODS ---------------------

        public List<EditableValueHolder> getInputs()
        {
            return inputs;
        }

        public HtmlOutputLabel getLabel()
        {
            return label;
        }

        public void setLabel(final HtmlOutputLabel label)
        {
            this.label = label;
        }

        public List<UIMessage> getMessages()
        {
            return messages;
        }

        public boolean hasRequiredInput()
        {
            return requiredInput;
        }

        public boolean hasValidationError()
        {
            return validationError;
        }

// -------------------------- OTHER METHODS --------------------------

        public String getPropertyName(final FacesContext context)
        {
            if (propertyName != null) {
                return propertyName;
            }

            if (inputs.size() == 0) {
                return null;
            }

            propertyName = (String) new ValueExpressionAnalyzer(((UIComponent) inputs.get(0)).getValueExpression("value")).getValueReference(
                context.getELContext()).getProperty();
            return propertyName;
        }

        public void registerInput(final EditableValueHolder input, final Validator validator, final FacesContext context)
        {
            inputs.add(input);
            if (input.isRequired() || isRequiredByConstraint(input, validator, context)) {
                requiredInput = true;
            }
            if (!input.isValid()) {
                validationError = true;
            } else if (!validationError) {
                // optimization to avoid loop if already flagged
                Iterator<FacesMessage> it = context.getMessages(((UIComponent) input).getClientId(context));
                while (it.hasNext()) {
                    if (it.next().getSeverity().compareTo(FacesMessage.SEVERITY_WARN) >= 0) {
                        validationError = true;
                        break;
                    }
                }
            }
        }

        public void registerMessage(final UIMessage message)
        {
            messages.add(message);
        }

        public void wire(final FacesContext context)
        {
            int numInputs = inputs.size();
            if (numInputs > 0) {
                if (label != null) {
                    label.setFor(((UIComponent) inputs.get(0)).getClientId(context));
                }
                for (int i = 0, len = messages.size(); i < len; i++) {
                    if (i < numInputs) {
                        messages.get(i).setFor(((UIComponent) inputs.get(i)).getClientId(context));
                    }
                }
            }
        }

        private boolean isRequiredByConstraint(final EditableValueHolder input, final Validator validator, final FacesContext context)
        {
            if (validator == null) {
                return false;
            }

            // NOTE believe it or not, getValueReference on ValueExpression is broken, so we have to do it ourselves
            ValueExpression valueExpression = ((UIComponent) input).getValueExpression("value");
            if (valueExpression != null) {
                ValueExpressionAnalyzer valueExpressionAnalyzer = new ValueExpressionAnalyzer(valueExpression);
                ValueReference vref = valueExpressionAnalyzer.getValueReference(context.getELContext());
                if (vref != null) { // valueExpressionAnalyzer can return a null value. The condition prevents a NPE
                    BeanDescriptor constraintsForClass = validator.getConstraintsForClass(vref.getBase().getClass());
                    PropertyDescriptor d = constraintsForClass.getConstraintsForProperty((String) vref.getProperty());
                    return (d != null) && d.hasConstraints();
                }
            }
            return false;
        }
    }
}