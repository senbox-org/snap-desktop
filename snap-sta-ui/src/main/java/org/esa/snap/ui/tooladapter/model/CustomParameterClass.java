/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.snap.ui.tooladapter.model;

import org.esa.snap.core.gpf.operators.tooladapter.ToolAdapterConstants;

import java.io.File;

/**
 * Enum-like class holding the possible types of operator parameters
 *
 * @author Ramona Manda
 * @author Cosmin Cara
 */
public class CustomParameterClass {

    private Class<?> aClass;
    private String typeMask;

    /**
     * Represents a template file that is supposed to be executed before the actual tool execution
     */
    public static final CustomParameterClass BeforeTemplateFileClass = new CustomParameterClass(File.class, ToolAdapterConstants.TEMPLATE_BEFORE_MASK);
    /**
     * Represents a template file that is supposed to be executed after the actual tool execution
     */
    public static final CustomParameterClass AfterTemplateFileClass = new CustomParameterClass(File.class, ToolAdapterConstants.TEMPLATE_AFTER_MASK);
    /**
     * Represents a the template file of the actual tool execution
     */
    public static final CustomParameterClass TemplateFileClass = new CustomParameterClass(File.class, ToolAdapterConstants.TEMPLATE_PARAM_MASK);
    /**
     * Represents a file parameter
     */
    public static final CustomParameterClass RegularFileClass = new CustomParameterClass(File.class, ToolAdapterConstants.REGULAR_PARAM_MASK);
    /**
     * Represents a file list parameter
     */
    public static final CustomParameterClass FileListClass = new CustomParameterClass(File[].class, ToolAdapterConstants.REGULAR_PARAM_MASK);
    /**
     * Represents a string/text parameter
     */
    public static final CustomParameterClass StringClass = new CustomParameterClass(String.class, ToolAdapterConstants.REGULAR_PARAM_MASK);
    /**
     * Represents an integer parameter
     */
    public static final CustomParameterClass IntegerClass = new CustomParameterClass(Integer.class, ToolAdapterConstants.REGULAR_PARAM_MASK);
    /**
     * Represents a string list parameter
     */
    public static final CustomParameterClass ListClass = new CustomParameterClass(String[].class, ToolAdapterConstants.REGULAR_PARAM_MASK);
    /**
     * Represents a boolean parameter
     */
    public static final CustomParameterClass BooleanClass = new CustomParameterClass(Boolean.class, ToolAdapterConstants.REGULAR_PARAM_MASK);
    /**
     * Represents a float parameter
     */
    public static final CustomParameterClass FloatClass = new CustomParameterClass(Float.class, ToolAdapterConstants.REGULAR_PARAM_MASK);

    private CustomParameterClass(Class<?> aClass, String typeMask) {
        this.aClass = aClass;
        this.typeMask = typeMask;
    }

    /**
     * @return  The Java class of the parameter
     */
    public Class<?> getParameterClass() {
        return aClass;
    }

    /**
     * Checks if the parameter is a template parameter
     */
    public boolean isTemplateParameter() {
        return typeMask.equals(ToolAdapterConstants.TEMPLATE_PARAM_MASK);
    }
    /**
     * Checks if the parameter is a template-before parameter
     */
    public boolean isTemplateBefore() {
        return typeMask.equals(ToolAdapterConstants.TEMPLATE_BEFORE_MASK);
    }
    /**
     * Checks if the parameter is a template-after parameter
     */
    public boolean isTemplateAfter() {
        return typeMask.equals(ToolAdapterConstants.TEMPLATE_AFTER_MASK);
    }
    /**
     * Checks if the parameter is a regular parameter
     */
    public boolean isParameter() {
        return typeMask.equals(ToolAdapterConstants.REGULAR_PARAM_MASK);
    }

    /**
     * Returns the type mask of the parameter
     */
    public String getTypeMask() { return this.typeMask; }

    /**
     * Returns the CustomParameterClass instance matching the given type mask
     */
    public static CustomParameterClass getObject(Class<?> aClass, String typeMask) {
        CustomParameterClass result = matchClass(TemplateFileClass, aClass, typeMask);
        if (result == null) {
            result = matchClass(BeforeTemplateFileClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(AfterTemplateFileClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(RegularFileClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(StringClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(IntegerClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(ListClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(BooleanClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(FloatClass, aClass, typeMask);
        }
        if (result == null) {
            result = matchClass(FileListClass, aClass, typeMask);
        }
        return result;
    }

    private static CustomParameterClass matchClass(CustomParameterClass paramClass, Class<?> aClass, String typeMask){
        return (paramClass.getParameterClass().equals(aClass) && paramClass.typeMask.equals(typeMask)) ?
            paramClass : null;
    }

}
