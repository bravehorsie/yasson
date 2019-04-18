/*******************************************************************************
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * David Kral
 ******************************************************************************/

package org.eclipse.yasson.internal.serializer;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility class for checking of IEEE-754 standard in big numbers
 *
 * @author David Kral
 */
public class BigNumberUtil {

    // Max bit length for unscaled values
    private static final int MAX_BIT_LENGTH = 53;

    // Max decimal unscaled value (2**53)-1
    private static final long MAX_UNSCALED_VALUE = 9007199254740991L;

    // Min decimal unscaled value -(2**53)+1
    private static final long MIN_UNSCALED_VALUE = -9007199254740991L;

    // -1022 is the lowest range of the exponent
    // more https://en.wikipedia.org/wiki/Exponent_bias
    private static final int MIN_SCALE_VALUE = -1022;

    // 1023 is the highest range of the exponent
    // more https://en.wikipedia.org/wiki/Exponent_bias
    private static final int MAX_SCALE_VALUE = 1023;

    /**
     * Checks whether the value of {@link BigDecimal} matches format IEEE-754
     *
     * @param value value which is going to be checked
     * @return true if value matches format IEEE-754
     */
    public static boolean isIEEE754(BigDecimal value) {
        if (value.abs().stripTrailingZeros().unscaledValue().bitLength() > MAX_BIT_LENGTH) {
            return false;
        }
        //Exponent overlaps 11bit
        return value.doubleValue() != Double.POSITIVE_INFINITY
                && value.doubleValue() != Double.NEGATIVE_INFINITY;
    }

    /**
     * Checks whether the value of {@link BigInteger} matches format IEEE-754
     *
     * @param value value which is going to be checked
     * @return true if value doesn't loose precision after conversion to double precision IEEE-754
     */
    public static boolean isIEEE754(BigInteger value) {
        // Number whose bit length is less or equal to 53 is considered as non IEEE 754-2008 binary64 compliant
        return isIEEE754(value.longValue());
    }

    /**
     * Checks whether the value of {@link Long} looses precision after converting to double precision IEEE754.
     *
     * @param value value which is going to be checked
     * @return true if value doesn't loose precision after conversion to double precision IEEE-754
     */
    public static boolean isIEEE754(Long value) {
        return value >= MIN_UNSCALED_VALUE && value <= MAX_UNSCALED_VALUE;
    }



}
