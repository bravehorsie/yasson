/*******************************************************************************
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.yasson.serializers.model;

import java.util.List;

public class PojoWithExtendedList {

    private List<Number> numbers;

    private List<Number> anotherNumbers;

    public List<Number> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Number> numbers) {
        this.numbers = numbers;
    }

    public List<Number> getAnotherNumbers() {
        return anotherNumbers;
    }

    public void setAnotherNumbers(List<Number> anotherNumbers) {
        this.anotherNumbers = anotherNumbers;
    }
}
