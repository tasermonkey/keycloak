/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.util;

import java.util.Objects;

/**
 * @author pedroigor
 */
public class HtmlUtils {

    /**
     * <p>Escapes the value for a HTML element attribute.</p>
     *
     * @param value
     * @return
     */
    public static String escapeAttribute(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char chr = value.charAt(i);

            if (chr != '\'' && chr != '"' && chr != '<' && chr != '>' && chr != '/') {
                escaped.append(chr);
            }
        }

        return escaped.toString();
    }

    public static String escapeHtml(String value) {
        if ( value == null ) return null;
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char chr = value.charAt(i);

            if ( chr == '&' ) {
                escaped.append("&amp;");
            } else if ( chr == '<' ) {
                escaped.append("&lt;");
            } else if ( chr == '>' ) {
                escaped.append("&gt;");
            } else {
                escaped.append(chr);
            }
        }
        return escaped.toString();
    }

    public static String[] escapeHtml(Object ... values) {
        if ( values == null ) return null;
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length;i++) {
            strings[i] = escapeHtml(Objects.toString(values[i], null));
        }
        return strings;
    }
}
