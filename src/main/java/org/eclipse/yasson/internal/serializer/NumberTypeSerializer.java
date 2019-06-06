/*******************************************************************************
 * Copyright (c) 2016, 2019 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Roman Grigoriadi
 ******************************************************************************/

package org.eclipse.yasson.internal.serializer;

import org.eclipse.yasson.internal.Marshaller;
import org.eclipse.yasson.internal.model.customization.Customization;

import javax.json.stream.JsonGenerator;
import java.math.BigDecimal;

/**
 * Serializer for {@link Number} type.
 * 
 * @author David Kral
 */
public class NumberTypeSerializer extends AbstractValueTypeSerializer<Number> {

    /**
     * Creates a new instance.
     *
     * @param customization Model customization.
     */
    public NumberTypeSerializer(Customization customization) {
        super(customization);
    }

    @Override
    protected void serialize(Number obj, JsonGenerator generator, Marshaller marshaller) {
        BigDecimal bigDecimalValue = new BigDecimal(String.valueOf(obj));
        generator.write(bigDecimalValue);
    }
}
