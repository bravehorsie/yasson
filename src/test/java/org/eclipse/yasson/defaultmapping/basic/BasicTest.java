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
import org.junit.Test;

import javax.json.bind.Jsonb;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Default mapping primitives tests.
 *
 * @author Dmitry Kornilov
 */
public class BasicTest {

    private static final int[] masks = {
            0x80,
            0x40,
            0x20,
            0x10,
            0x08,
            0x04,
            0x02,
            0x01};

    @Test
    public void testOutOfRangeNumbers() {
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 308; i++) {
            sb.append("0");
        }

        BigDecimal bd = new BigDecimal(sb.toString());
        testBigDec(bd);

        sb.append("0");

        bd = new BigDecimal(sb.toString());
        testBigDec(bd);

        sb = new StringBuilder("0.");
        for (int i = 0; i < 309; i++) {
            sb.append("0");
        }

        bd = new BigDecimal(sb.toString());
        testBigDec(bd);

        sb.append("1");

        bd = new BigDecimal(sb.toString());
        testBigDec(bd);

        sb = new StringBuilder("0.");
        for (int i = 0; i < 350; i++) {
            sb.append("0");
        }
        sb.append("1");

        bd = new BigDecimal(sb.toString());
        testBigDec(bd);
    }

    private void testBigDec(BigDecimal bd) {
        System.out.println("bd = " + bd);
        System.out.println("bd.doubleValue() = " + bd.doubleValue());
        System.out.println("isIEEE754(bd) = " + isIEEE754(bd));
    }

    private boolean isIEEE754(BigDecimal bigDecimal) {
        if (bigDecimal.stripTrailingZeros().unscaledValue().bitLength() > 53) {
            return false;
        }
        return bigDecimal.doubleValue() != Double.POSITIVE_INFINITY
                && bigDecimal.doubleValue() != Double.NEGATIVE_INFINITY;
    }


    @Test
    public void testBigDecToFloat() {
        BigDecimal bd = new BigDecimal("39999999999999996");
        System.out.println("bd = " + bd);
        System.out.printf("dubleValue() = %.2000f\n", bd.doubleValue());
        System.out.printf("BD from double value = %.2000f\n", new BigDecimal(bd.doubleValue()));
        System.out.printf("value = %.2000f\n", bd.floatValue());


        //000000000100000110001001001101110100101111000110101001
        //111011110101110101111010111111101011111111110111111101


    }


    @Test
    public void testParse() {
        System.out.println("10 % 8 = " + 10 % 8);
        System.out.println("4%8 = " + 4 % 8);
        System.out.println("8%8\\ = " + 8 % 8);
        System.out.println("1%8 = " + 1 % 8);

        String expBytes = "1000" + "0000" + "011";
        String mantissaByes = "1001" + "1001" + "1001" + "1001" +
                "1001" + "1001" + "1001" + "1001" +
                "1001" + "1001" + "1001" + "1001" +
                "1001" + "1";
        String bytes = "0" + expBytes + mantissaByes;
        BigDecimal exponent = parseExponent(bytes);
        System.out.println("exponent = " + exponent);
        BigDecimal mantissa = parseMantissa(bytes);
        System.out.println("mantissa = " + mantissa);
        BigDecimal expExpanded = new BigDecimal(binaryPow(exponent.intValue()));
        System.out.println("expExpanded = " + expExpanded);
        BigDecimal result = mantissa.multiply(expExpanded);
        System.out.println("result = " + result);
    }

    private BigDecimal parseExponent(String bytes) {
        long exponent = -1023;
        for (int i = 0; i < 11; i++) {
            int position = 11;
            char bit = bytes.charAt(position - i);
            if (bit == '1') {
                exponent = exponent + binaryPow(i);
            }

        }
        return new BigDecimal(exponent);
    }

    private BigDecimal parseMantissa(String bytes) {
        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < 53; i++) {
            int positionBias = 12;
            int position = positionBias + i;
            result = result.add(binaryNegativePow(i + 1));
        }
        return result;
    }


    private BigDecimal binaryNegativePow(int exp) {
        BigDecimal two = new BigDecimal("2");
        BigDecimal result = BigDecimal.ONE;
        for (int i = 1; i <= exp; i++) {
            result = result.divide(two);
        }
        return result;
    }

    private long binaryPow(int exp) {
        long result = 1;
        for (int i = 1; i <= exp; i++) {
            result = result << 1;
        }
        return result;
    }

    private byte[] assembleIEEE754binary64bit(long exponent, long mantissa) {
        long result = 0;
        //move 11bits of exponent after the signed bit
        result = result | (exponent << (52));
        //move mantissa right for the sign bit and exponent
        result = result | (mantissa >>> 12);
        //sign bit and exponent (12bit) are now written.
        return longToBytes(result);
    }

    private long mergeIntegralAndFloatingBits(byte[] integral, byte[] floating, long floatingPointPosition) {
        long integralBits = bytesToLong(integral);
        long mantissaBits = bytesToLong(floating);
        long mantissa = 0;
        //TODO: fix floating point when integral is 0.
        //move integral by its length (stored in floatingPointPosition) + drop first 1.
        mantissa = integralBits << (64 - (floatingPointPosition));
        mantissa = mantissa | (mantissaBits >>> floatingPointPosition);
        return mantissa;
    }

    @Test
    public void testConvert() {
        /*StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 308; i++) {
            sb.append("0");
        }

        BigDecimal bd = new BigDecimal(sb.toString());*/
//        BigDecimal bd = new BigDecimal("3.9999999999999996");
        BigDecimal bd = new BigDecimal("1.1");
        System.out.println("bd = " + bd);
        byte[] fractionBits = getFractionBytes(bd);
        byte[] integralBits = getIntegralBytes(bd);
        BigInteger integralPart = bd.toBigInteger();
        //todo no support for 0.xxx
        int binaryFloatingPosition = getMSBposition(integralPart.longValue());
        long exponent = (binaryFloatingPosition + 1023);
        System.out.println("binaryFloatingPosition = " + binaryFloatingPosition);
        System.out.println("binary exponent = " + (binaryFloatingPosition + 1023));

        byte[] first64fraction = new byte[8];
        System.arraycopy(fractionBits, 0, first64fraction, 0, 8);
        long mantissa = mergeIntegralAndFloatingBits(integralBits, first64fraction, binaryFloatingPosition);


        byte[] ieee754 = assembleIEEE754binary64bit(exponent, mantissa);
        System.out.print("IEEE754A = ");
        System.out.println(printBytesStartingLeft(ieee754, 64));

        StringBuilder result = new StringBuilder("IEEE754C = ");
        result.append("0");
//        result.append("-");
        result.append(printBytes(getIntegralBytes(exponent), 11));
//        result.append("-");
        result.append(printBytes(integralBits, binaryFloatingPosition));
//        result.append("-");
        result.append(printBytesStartingLeft(fractionBits, 53 - binaryFloatingPosition - 1));

        System.out.println(result);

        long value = Double.doubleToLongBits(bd.doubleValue());
        System.out.println("IEEE754D = " + toBinaryString(getIntegralBytesByBuffer(value)));
        double parsed = Double.longBitsToDouble(value);
        System.out.println("Double.longBitsToDouble(value) = " + parsed);

        long valueFromCustom = bytesToLong(ieee754);
        parsed = Double.longBitsToDouble(valueFromCustom);
        System.out.println("valueFromCustom = " + parsed);
        bd.doubleValue();


    }


    private byte[] getFractionBytes(BigDecimal value) {
        byte[] bytes = new byte[16];
        long fractionPart = value.remainder(BigDecimal.ONE).movePointRight(value.scale()).toBigInteger().longValue();
        long scale = value.scale();
        long limit = 1;
        for (int i = 1; i <= scale; i++) {
            limit = limit * 10;
        }
        System.out.println("fractionPart = " + fractionPart);
        System.out.println("scale = " + scale);
        System.out.println("limit = " + limit);
        StringBuilder bits = new StringBuilder();
        Set<Long> existing = new HashSet<>();
        long doubleValue = fractionPart << 1;
        int bitPosition = 0;
        int bytePosition = 0;
        //it may happen than 1 is printed to 53th bit following all zeroes, than we need to move 53 more
        for (int i = 0; i < 128; i++, bitPosition++) {
            if (bitPosition == 8) {
                bitPosition = 0;
                bytePosition++;
            }
//            System.out.print("doubleValue = " + doubleValue);
            if (existing.contains(doubleValue)) {
//                System.out.print(" <= loop");
            } else {
                existing.add(doubleValue);
            }

            if (doubleValue > limit) {
                bytes[bytePosition] |= masks[bitPosition];
                bits.append(1);
                doubleValue = doubleValue - limit;
//                System.out.println(" Bit [" + i + "]: " + 1);
            } else {
                bits.append(0);
//                System.out.println(" Bit [" + i + "]: " + 0);
            }
            doubleValue = doubleValue << 1;
        }
        return bytes;
    }

    private byte[] getIntegralBytes(BigDecimal bigDecimal) {
        Long integralPart = bigDecimal.toBigInteger().longValue();
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (integralPart & 0xFF);
            integralPart >>= 8;
        }
        return result;
    }

    private byte[] getIntegralBytes(Long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private byte[] getIntegralBytesByBuffer(Long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    private int getMSBposition(Long n) {
        int ndx = 0;
        while (1 < n) {
            n = (n >> 1);
            ndx++;
        }
        return ndx;
    }

    public static String toBinaryString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            StringBuilder sb = new StringBuilder("00000000");
            for (int bit = 0; bit < 8; bit++) {
                if (((b >> bit) & 1) > 0) {
                    sb.setCharAt(7 - bit, '1');
                }
            }
            result.append(sb.toString());
        }
        return result.toString();
    }


    private String printBytes(byte[] bytes, int count) {
        String allBits = toBinaryString(bytes);
        return allBits.substring(allBits.length() - count);
    }
    private String printBytesStartingLeft(byte[] bytes, int count) {
        String allBits = toBinaryString(bytes);
        return allBits.substring(0, count);
    }

    private int bitAt(byte b, int position) {
        return (b >> position) & 0x01;
    }

    private byte[] longToBytes(Long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }


    private byte[] cutLeadingZeroes(byte[] bytes) {
        int count;
        return null;
//        for (int i=0; i<)
    }

    @Test
    public void testMarshallEscapedString() {
        final Jsonb jsonb = (new JsonBindingBuilder()).build();
        assertEquals("[\" \\\\ \\\" / \\f\\b\\r\\n\\t 9\"]", jsonb.toJson(new String[]{" \\ \" / \f\b\r\n\t \u0039"}));
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
        final String val = jsonb.toJson(new Object());
        assertEquals("{}", val);
    }

}
