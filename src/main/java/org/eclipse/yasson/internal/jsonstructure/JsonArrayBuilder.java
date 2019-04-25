/*******************************************************************************
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 ******************************************************************************/
package org.eclipse.yasson.internal.jsonstructure;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Builds {@link JsonArray}. Delegates to {@link javax.json.JsonArrayBuilder}.
 */
public class JsonArrayBuilder extends JsonStructureBuilder {

    private final javax.json.JsonArrayBuilder arrayBuilder;

    public JsonArrayBuilder() {
        this.arrayBuilder = Json.createArrayBuilder();
    }

    @Override
    public JsonArray build() {
        return arrayBuilder.build();
    }

    @Override
    public void write(JsonValue value) {
        arrayBuilder.add(value);
    }

    @Override
    public void write(String value) {
        arrayBuilder.add(value);
    }

    @Override
    public void write(BigDecimal value) {
        arrayBuilder.add(value);
    }

    @Override
    public void write(BigInteger value) {
        arrayBuilder.add(value);
    }

    @Override
    public void write(int value) {
        arrayBuilder.add(value);
    }

    @Override
    public void write(long value) {
        arrayBuilder.add(value);
    }

    @Override
    public void write(double value) {
        arrayBuilder.add(value);
    }

    @Override
    public void write(boolean value) {
        arrayBuilder.add(value);
    }

    @Override
    public void writeNull() {
        arrayBuilder.addNull();
    }

    @Override
    public void put(JsonStructure structure) {
        arrayBuilder.add(structure);
    }
}
