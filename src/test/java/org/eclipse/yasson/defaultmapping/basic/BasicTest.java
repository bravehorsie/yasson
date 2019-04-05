/*******************************************************************************
 * Copyright (c) 2015, 2018 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Dmitry Kornilov - initial implementation
 ******************************************************************************/
package org.eclipse.yasson.defaultmapping.basic;

import org.eclipse.yasson.internal.JsonBindingBuilder;
import org.eclipse.yasson.internal.serializer.BigNumberUtil;
import org.junit.Test;

import javax.json.bind.Jsonb;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Default mapping primitives tests.
 *
 * @author Dmitry Kornilov
 */
public class BasicTest {

    @Test
    public void bigDecTest() {
        StringBuilder bigDecValue = new StringBuilder("0.");
        for (int i = 0; i < 323; i++) {
            bigDecValue.append("0");
        }
        bigDecValue.append("9007199254740991");
        BigDecimal value = new BigDecimal(bigDecValue.toString());
        System.out.println("BigNumberUtil.isIEEE754 = " + BigNumberUtil.isIEEE754(value));

        DecimalFormat format = new DecimalFormat("#");
        format.setMaximumFractionDigits(2000);

        System.out.println("bigDec = " + format.format(value));
        System.out.println("double = " + format.format(value.doubleValue()));
        System.out.println("double = " + format.format(Double.valueOf(bigDecValue.toString())));
        System.out.println("======");
        System.out.printf("value = %.2000f\n", value);
        System.out.printf("value = %.2000f\n", value.doubleValue());
        System.out.printf("value = %.2000f\n", Double.valueOf(bigDecValue.toString()));
        System.out.println("===");
        System.out.println("value.unscaledValue() = " + value.unscaledValue());
        System.out.println("value.precision() = " + value.precision());
        //bit value of number without scale
        int unscaledBits = value.unscaledValue().abs().bitLength();
        System.out.println("unscaledBits = " + unscaledBits);
        //bit value of scaled number
        int nonFractionBits = value.toBigInteger().bitLength();
        System.out.println("nonFractionBits = " + nonFractionBits);

        System.out.println("value.scale() = " + value.scale());
        System.out.println("value.scale().bitLength() = " + new BigInteger(String.valueOf(value.scale())).bitLength());
        System.out.println("-----");

    }

    @Test
    public void testBigDecConversion() {
        BigDecimal addition = new BigDecimal(".0001");
        BigDecimal value = BigDecimal.ZERO;
        for (int i = 0; i < 10000; i++) {
            value = value.add(addition);
            Double d = value.doubleValue();
            if (!value.equals(new BigDecimal(d))) {
                throw new RuntimeException("Value: "+value+" double: "+d+" new BigDec: "+new BigDecimal(d));
            }
        }
    }

    @Test
    public void testConvert() {
        BigDecimal bd = new BigDecimal("15.0045634325");
        convert(bd);
    }

    private byte[] convert(BigDecimal bigDecimal) {
        long decimalPart = bigDecimal.remainder(BigDecimal.ONE).movePointRight(bigDecimal.scale()).toBigInteger().longValue();
        byte[] decimalBytes = longToBytes(decimalPart);
        System.out.println("decimalBytes:" + Arrays.toString(decimalBytes));
        printByte(decimalBytes);
        return decimalBytes;
    }

    private void printByte(byte[] bytes) {
        for (byte b : bytes) {
            System.out.print(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0') + " ");
        }
    }

    private int bitAt(byte b, int position) {
        return (b >> position) & 0x01;
    }

    private byte[] longToBytes(Long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    private byte[] cutLeadingZeroes(byte[] bytes) {
        int count;
        return null;
//        for (int i=0; i<)
    }

    @Test
    public void testMarshallEscapedString() {
        final Jsonb jsonb = (new JsonBindingBuilder()).build();
        assertEquals("[\" \\\\ \\\" / \\f\\b\\r\\n\\t 9\"]", jsonb.toJson(new String[] {" \\ \" / \f\b\r\n\t \u0039"}));
    }

    @Test
    public void testMarshallWriter() {
        final Jsonb jsonb = (new JsonBindingBuilder()).build();
        Writer writer = new StringWriter();
        jsonb.toJson(new Long[]{5L}, writer);
        assertEquals("[5]", writer.toString());
    }

    @Test
    public void testMarshallOutputStream() throws IOException {
        final Jsonb jsonb = (new JsonBindingBuilder()).build();

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            jsonb.toJson(new Long[]{5L}, baos);
            assertEquals("[5]", baos.toString("UTF-8"));
        }
    }

    @Test
    public void testObjectSerialization() {
        Jsonb jsonb = (new JsonBindingBuilder()).build();
        final String val =  jsonb.toJson(new Object());
        assertEquals("{}", val);
    }

}
