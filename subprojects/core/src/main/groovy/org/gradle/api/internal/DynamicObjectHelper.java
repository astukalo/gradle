/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal;

import groovy.lang.MissingPropertyException;
import org.gradle.api.DynamicExtension;
import org.gradle.api.internal.plugins.DefaultConvention;
import org.gradle.api.plugins.Convention;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicObjectHelper extends CompositeDynamicObject {

    public static final String ADHOC_EXTENSION_NAME = "ext";

    public enum Location {
        BeforeConvention, AfterConvention
    }

    private final Object delegate;
    private final AbstractDynamicObject dynamicDelegate;
    private DynamicObject parent;
    private Convention convention;
    private DynamicObject beforeConvention;
    private DynamicObject afterConvention;
    private DynamicExtension dynamicExtension;
    private DynamicObject dynamicExtensionDynamicObject;

    /**
     * This variant will internally create a convention that is not fully featured, so should be avoided.
     *
     * Use one of the other variants wherever possible.
     *
     * @param delegate The delegate
     * @see org.gradle.api.internal.plugins.DefaultConvention#DefaultConvention()
     */
    public DynamicObjectHelper(Object delegate) {
        this(delegate, new BeanDynamicObject(delegate), new DefaultConvention());
    }

    public DynamicObjectHelper(Object delegate, Instantiator instantiator) {
        this(delegate, new BeanDynamicObject(delegate), new DefaultConvention(instantiator));
    }

    public DynamicObjectHelper(Object delegate, AbstractDynamicObject dynamicDelegate, Instantiator instantiator) {
        this(delegate, dynamicDelegate, new DefaultConvention(instantiator));
    }

    public DynamicObjectHelper(Object delegate, AbstractDynamicObject dynamicDelegate, Convention convention) {
        this.delegate = delegate;
        this.dynamicDelegate = dynamicDelegate;
        this.convention = convention;
        this.dynamicExtension = new DefaultDynamicExtension();
        this.dynamicExtensionDynamicObject = new AddOnDemandDynamicExtensionDynamicObjectAdapter(delegate, dynamicExtension);

        // Expose the dynamic storage as an extension on the delegate
        getConvention().add(ADHOC_EXTENSION_NAME, dynamicExtension);

        updateDelegates();
    }

    private void updateDelegates() {
        List<DynamicObject> delegates = new ArrayList<DynamicObject>();
        delegates.add(dynamicDelegate);
        delegates.add(dynamicExtensionDynamicObject);
        if (beforeConvention != null) {
            delegates.add(beforeConvention);
        }
        if (convention != null) {
            delegates.add(convention.getExtensionsAsDynamicObject());
        }
        if (afterConvention != null) {
            delegates.add(afterConvention);
        }
        if (parent != null) {
            delegates.add(parent);
        }
        setObjects(delegates.toArray(new DynamicObject[delegates.size()]));

        delegates.remove(parent);
        delegates.add(dynamicExtensionDynamicObject);
        setObjectsForUpdate(delegates.toArray(new DynamicObject[delegates.size()]));
    }

    protected String getDisplayName() {
        return dynamicDelegate.getDisplayName();
    }

    public DynamicExtension getDynamicExtension() {
        return dynamicExtension;
    }

    public void addProperties(Map<String, ?> properties) {
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            dynamicExtension.add(entry.getKey(), entry.getValue());
        }
    }

    public DynamicObject getParent() {
        return parent;
    }

    public void setParent(DynamicObject parent) {
        this.parent = parent;
        updateDelegates();
    }

    public Convention getConvention() {
        return convention;
    }

    public void addObject(DynamicObject object, Location location) {
        switch (location) {
            case BeforeConvention:
                beforeConvention = object;
                break;
            case AfterConvention:
                afterConvention = object;
        }
        updateDelegates();
    }

    /**
     * Returns the inheritable properties and methods of this object.
     *
     * @return an object containing the inheritable properties and methods of this object.
     */
    public DynamicObject getInheritable() {
        return new InheritedDynamicObject();
    }

    private DynamicObjectHelper snapshotInheritable() {
        AbstractDynamicObject emptyBean = new AbstractDynamicObject() {
            @Override
            protected String getDisplayName() {
                return dynamicDelegate.getDisplayName();
            }
        };

        DynamicObjectHelper helper = new DynamicObjectHelper(emptyBean);

        helper.parent = parent;
        helper.convention = convention;
        helper.dynamicExtension = dynamicExtension;
        helper.dynamicExtensionDynamicObject = dynamicExtensionDynamicObject;
        if (beforeConvention != null) {
            helper.beforeConvention = beforeConvention;
        }
        helper.updateDelegates();
        return helper;
    }

    private class InheritedDynamicObject implements DynamicObject {
        public void setProperty(String name, Object value) {
            throw new MissingPropertyException(String.format("Could not find property '%s' inherited from %s.", name,
                    dynamicDelegate.getDisplayName()));
        }

        public boolean hasProperty(String name) {
            return snapshotInheritable().hasProperty(name);
        }

        public Object getProperty(String name) {
            return snapshotInheritable().getProperty(name);
        }

        public Map<String, Object> getProperties() {
            return snapshotInheritable().getProperties();
        }

        public boolean hasMethod(String name, Object... arguments) {
            return snapshotInheritable().hasMethod(name, arguments);
        }

        public Object invokeMethod(String name, Object... arguments) {
            return snapshotInheritable().invokeMethod(name, arguments);
        }
    }

    public static DynamicObject asDynamicObject(Object object) {
        if (object instanceof DynamicObject) {
            return (DynamicObject)object;
        } else if (object instanceof DynamicObjectAware) {
            return ((DynamicObjectAware) object).getAsDynamicObject();
        } else {
            return new BeanDynamicObject(object);
        }
    }
}

