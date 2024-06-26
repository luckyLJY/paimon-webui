/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.web.api.action.context;

import org.apache.paimon.web.api.exception.ActionException;

import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The AbstractActionContext provides a default implementation for getActionArgs. Its concrete
 * subclasses only need to annotate the parameter fields of the action with the {@link ActionConf}
 * annotation.
 */
@SuperBuilder
public abstract class AbstractActionContext implements ActionContext {

    private String actionPath;

    @Override
    public List<String> getArguments() {
        Class<?> clazz = this.getClass();
        List<String> args = new ArrayList<>();
        args.add(name());
        addArgument(args, clazz, this);
        return args;
    }

    private void addArgument(List<String> args, Class<?> clazz, Object obj) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        addArgument(args, clazz.getSuperclass(), obj);
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            ActionConf actionConf = declaredField.getAnnotation(ActionConf.class);
            if (actionConf == null) {
                continue;
            }
            String confKey = actionConf.value();
            Object confValue = null;
            try {
                declaredField.setAccessible(true);
                confValue = declaredField.get(obj);
            } catch (IllegalArgumentException | IllegalAccessException ignore) {

            }
            boolean nullable = declaredField.getAnnotation(Nullable.class) != null;
            if (!nullable && confValue == null) {
                throw new ActionException(confKey + " can not be null");
            }
            if (nullable && confValue == null) {
                continue;
            }
            if (confValue instanceof List) {
                ActionContextUtil.addConfList(args, confKey, (List<?>) confValue);
            } else {
                ActionContextUtil.addConf(args, confKey, String.valueOf(confValue));
            }
        }
    }

    public String getJarPath() {
        return actionPath;
    }
}
