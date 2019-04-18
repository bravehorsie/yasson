/*******************************************************************************
 * Copyright (c) 2016, 2018 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Roman Grigoriadi
 * David Kral
 ******************************************************************************/

package org.eclipse.yasson.defaultmapping.basic;

import org.eclipse.yasson.TestTypeToken;
import org.eclipse.yasson.defaultmapping.basic.model.BigDecimalInNumber;
import org.eclipse.yasson.defaultmapping.generics.model.ScalarValueWrapper;
import org.eclipse.yasson.internal.serializer.BigNumberUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Roman Grigoriadi
 */
public class NumberTest {

    private Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void testBigDecimal() {
        assertTrue(BigNumberUtil.isIEEE754(BigDecimal.TEN));
        //mantissa bit length 53
        assertTrue(BigNumberUtil.isIEEE754(new BigDecimal("0.1000000000000001")));
        //mantissa bit length 54
        assertFalse(BigNumberUtil.isIEEE754(new BigDecimal("0.10000000000000001")));
        //mantissa bit length 1 exponent in range
        assertTrue(BigNumberUtil.isIEEE754(new BigDecimal("0.0000000000000000000000001")));

        // Sign: "0"
        // Exponent: "11111111110"
        // Mantissa: "00000000000000000000000000000000000000000000000000001"
        byte[] bits = new byte[8];
        bits[0] = (byte)0x7f;
        bits[1] = (byte)0xe0;
        bits[7] = (byte)0x01;

        Double parsedDouble = ByteBuffer.wrap(bits).getDouble();
        BigDecimal value = new BigDecimal(parsedDouble.toString());
        //all in range now
        assertTrue(BigNumberUtil.isIEEE754(value));
        //move exponent out of range (mantissa is OK)
        value = value.multiply(new BigDecimal("10"));
        assertFalse(BigNumberUtil.isIEEE754(value));

        // Sign: "0"
        // Exponent: "10000000000"
        // Mantissa: "11111111111111111111111111111111111111111111111111111"
        bits = new byte[8];
        bits[0] = (byte)0x40;
        bits[1] = (byte)0x0f;
        bits[2] = (byte)0xff;
        bits[3] = (byte)0xff;
        bits[4] = (byte)0xff;
        bits[5] = (byte)0xff;
        bits[6] = (byte)0xff;
        bits[7] = (byte)0xff;

        parsedDouble = ByteBuffer.wrap(bits).getDouble();
        System.out.println("parsedDouble = " + parsedDouble);
        value = new BigDecimal(parsedDouble.toString());

        System.out.println("value.fraction bits = " + value.remainder(BigDecimal.ONE).unscaledValue().bitLength());
        System.out.println("BI bits = " + value.toBigInteger().bitLength());
        System.out.println("value.unscaledValue().bitLength() = " + value.unscaledValue().bitLength());
        System.out.println("value = " + value);

        System.out.println(printBytes(bits, "value bits: "));
        //all in range now

        assertTrue(BigNumberUtil.isIEEE754(value));
        //move mantissa out of range (exponent OK)
        value = value.add(BigDecimal.ONE);
        assertFalse(BigNumberUtil.isIEEE754(value));


        //largest unscaled value allowed by 53bit mantissa
        assertTrue(BigNumberUtil.isIEEE754(new BigDecimal("9007199254740991")));
        //54bit mantissa
        assertFalse(BigNumberUtil.isIEEE754(new BigDecimal("9007199254740992")));

        //same for unsigned
        assertTrue(BigNumberUtil.isIEEE754(new BigDecimal("-9007199254740991")));
        assertFalse(BigNumberUtil.isIEEE754(new BigDecimal("-9007199254740992")));
    }

    @Test
    public void testBigInteger() {
        assertTrue(BigNumberUtil.isIEEE754(new BigInteger("9007199254740991")));
        assertFalse(BigNumberUtil.isIEEE754(new BigInteger("9007199254740992")));

        assertTrue(BigNumberUtil.isIEEE754(new BigInteger("-9007199254740991")));
        assertFalse(BigNumberUtil.isIEEE754(new BigInteger("-9007199254740992")));
    }

    @Test
    public void testFromIEEE754() {

        // Sign: "0"
        // Exponent: "11111111110"
        // Mantissa: "00000000000000000000000000000000000000000000000000001"
        byte[] bits = new byte[8];
        bits[0] = (byte)0x7f;
        bits[1] = (byte)0xe0;
        bits[2] = (byte)0x00;
        bits[3] = (byte)0x00;
        bits[4] = (byte)0x00;
        bits[5] = (byte)0x00;
        bits[6] = (byte)0x00;
        bits[7] = (byte)0x01;

        System.out.println(printBytes(bits, "bytes"));
        Double aDouble = ByteBuffer.wrap(bits).getDouble();
        System.out.println("aDouble = " + aDouble);
        BigDecimal value = new BigDecimal(aDouble.toString());

        assertTrue(BigNumberUtil.isIEEE754(value));
        value = value.multiply(new BigDecimal("10"));
        assertFalse(BigNumberUtil.isIEEE754(value));


        System.out.println("new BigDecimal(\"2\").pow(1017) = " + new BigDecimal("2").pow(1017));
    }

    private String printBytes(byte[] bytes, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        for (byte b : bytes) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return sb.toString();
    }

    @Test
    public void testSerializeFloat() {
        final String json = jsonb.toJson(0.35f);
        Assert.assertEquals("0.35", json);

        Float result = jsonb.fromJson("0.35", Float.class);
        Assert.assertEquals((Float) .35f, result);
    }

    @Test
    public void testBigDecimalMarshalling() {
        String jsonString = jsonb.toJson(new BigDecimal("0.10000000000000001"));
        Assert.assertEquals("\"0.10000000000000001\"", jsonString);

        jsonString = jsonb.toJson(new BigDecimal("0.1000000000000001"));
        Assert.assertEquals("0.1000000000000001", jsonString);

        BigDecimal result = jsonb.fromJson("0.10000000000000001", BigDecimal.class);
        Assert.assertEquals(new BigDecimal("0.10000000000000001"), result);

        result = jsonb.fromJson("\"0.100000000000000001\"", BigDecimal.class);
        Assert.assertEquals(new BigDecimal("0.100000000000000001"), result);
    }

    @Test
    public void testBigDecimalIEEE748() {
        String jsonString = jsonb.toJson(new BigDecimal("9007199254740991"));
        Assert.assertEquals("9007199254740991", jsonString);

        jsonString = jsonb.toJson(new BigDecimal("9007199254740992"));
        Assert.assertEquals("\"9007199254740992\"", jsonString);

        jsonString = jsonb.toJson(new BigDecimal("9007199254740991.1"));
        Assert.assertEquals("\"9007199254740991.1\"", jsonString);

        jsonString = jsonb.toJson(new BigDecimal(new BigInteger("1"), -400));
        Assert.assertEquals("\"" + new BigDecimal(new BigInteger("1"), -400) + "\"", jsonString);
    }

    @Test
    public void testBigIntegerIEEE748() {
        String jsonString = jsonb.toJson(new BigInteger("9007199254740991"));
        Assert.assertEquals("9007199254740991", jsonString);

        jsonString = jsonb.toJson(new BigInteger("9007199254740992"));
        Assert.assertEquals("\"9007199254740992\"", jsonString);
    }

    @Test
    public void testBigDecimalInNumber() {
        BigDecimalInNumber testValueQuoted = new BigDecimalInNumber() {{setBigDecValue(new BigDecimal("9007199254740992"));}};
        BigDecimalInNumber testValueUnQuoted = new BigDecimalInNumber() {{setBigDecValue(new BigDecimal("9007199254740991"));}};
        String jsonString = jsonb.toJson(testValueQuoted);
        Assert.assertEquals("{\"bigDecValue\":\"9007199254740992\"}", jsonString);

        jsonString = jsonb.toJson(testValueUnQuoted);
        Assert.assertEquals("{\"bigDecValue\":9007199254740991}", jsonString);

        BigDecimalInNumber result = jsonb.fromJson("{\"bigDecValue\":\"9007199254740992\"}", BigDecimalInNumber.class);
        Assert.assertEquals(testValueQuoted.getBigDecValue(), result.getBigDecValue());

        result = jsonb.fromJson("{\"bigDecValue\":9007199254740991}", BigDecimalInNumber.class);
        Assert.assertEquals(testValueUnQuoted.getBigDecValue(), result.getBigDecValue());
    }

    @Test
    public void testBigDecimalWrappedMarshalling() {
        String jsonString = jsonb.toJson(new ScalarValueWrapper<>(new BigDecimal("0.1000000000000001")));
        Assert.assertEquals("{\"value\":0.1000000000000001}", jsonString);

        jsonString = jsonb.toJson(new ScalarValueWrapper<>(new BigDecimal("0.10000000000000001")));
        Assert.assertEquals("{\"value\":\"0.10000000000000001\"}", jsonString);

        ScalarValueWrapper<BigDecimal> result = jsonb.fromJson("{\"value\":0.1000000000000001}", new TestTypeToken<ScalarValueWrapper<BigDecimal>>(){}.getType());
        Assert.assertEquals(new BigDecimal("0.1000000000000001"), result.getValue());

        result = jsonb.fromJson("{\"value\":\"0.10000000000000001\"}", new TestTypeToken<ScalarValueWrapper<BigDecimal>>(){}.getType());
        Assert.assertEquals(new BigDecimal("0.10000000000000001"), result.getValue());
    }

    @Test
    public void testBigDecimalCastedToNumber() {
        String jsonString = jsonb.toJson(new Object() { public Number number = new BigDecimal("0.10000000000000001"); });
        Assert.assertEquals("{\"number\":\"0.10000000000000001\"}", jsonString);

        jsonString = jsonb.toJson(new Object() { public Number number = new BigDecimal("0.1000000000000001"); });
        Assert.assertEquals("{\"number\":0.1000000000000001}", jsonString);
    }

    @Test
    public void testLongIEEE748() {

        // 9007199254740991L
        Long maxJsSafeValue = Double.valueOf(Math.pow(2, 53)).longValue() - 1;
        Long upperJsUnsafeValue = maxJsSafeValue + 1;

        String json = jsonb.toJson(maxJsSafeValue);
        Assert.assertEquals("9007199254740991", json);
        Long deserialized = jsonb.fromJson(json, Long.class);
        Assert.assertEquals(Long.valueOf("9007199254740991"), deserialized);

        json = jsonb.toJson(upperJsUnsafeValue);
        Assert.assertEquals("\"9007199254740992\"", json);
        deserialized = jsonb.fromJson(json, Long.class);
        Assert.assertEquals(Long.valueOf("9007199254740992"), deserialized);


        Long minJsSafeValue = Math.negateExact(maxJsSafeValue);
        Long lowerJsUnsafeValue = minJsSafeValue - 1;

        json = jsonb.toJson(minJsSafeValue);
        Assert.assertEquals("-9007199254740991", json);
        deserialized = jsonb.fromJson(json, Long.class);
        Assert.assertEquals(Long.valueOf("-9007199254740991"), deserialized);

        json = jsonb.toJson(lowerJsUnsafeValue);
        Assert.assertEquals("\"-9007199254740992\"", json);
        deserialized = jsonb.fromJson(json, Long.class);
        Assert.assertEquals(Long.valueOf("-9007199254740992"), deserialized);
    }

    /**
     * Tests that JSON-P RI itself does no big number (out of IEEE 754 quotation).
     * This is why it is now must be done in Yasson to match the JSONB spec.
     */
    @Test
    public void testJsonpBigNumber() {
        StringWriter w = new StringWriter();
        JsonGenerator generator = Json.createGenerator(w);

        Long maxJsSafeValue = Double.valueOf(Math.pow(2, 53)).longValue() - 1;
        Long upperJsUnsafeValue = Long.MAX_VALUE;

        generator.writeStartObject();
        generator.write("safeLongValue", maxJsSafeValue);
        generator.write("unsafeLongValue", upperJsUnsafeValue);
        generator.write("safeBigDecimalValue", BigDecimal.TEN);
        generator.write("unsafeBigDecimalValue", BigDecimal.valueOf(upperJsUnsafeValue));
        generator.writeEnd();
        generator.close();

        Assert.assertEquals("{" +
                        "\"safeLongValue\":9007199254740991," +
                        "\"unsafeLongValue\":9223372036854775807," +
                        "\"safeBigDecimalValue\":10," +
                        "\"unsafeBigDecimalValue\":9223372036854775807}",
                w.toString());


        w = new StringWriter();
        JsonWriter writer = Json.createWriter(w);


        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("safeLongValue", maxJsSafeValue);
        objectBuilder.add("unsafeLongValue", upperJsUnsafeValue);
        objectBuilder.add("safeBigDecimalValue", BigDecimal.valueOf(maxJsSafeValue));
        objectBuilder.add("unsafeBigDecimalValue", BigDecimal.valueOf(upperJsUnsafeValue));
        JsonObject build = objectBuilder.build();
        writer.write(build);
        writer.close();

        Assert.assertEquals("{" +
                "\"safeLongValue\":9007199254740991," +
                "\"unsafeLongValue\":9223372036854775807," +
                "\"safeBigDecimalValue\":9007199254740991," +
                "\"unsafeBigDecimalValue\":9223372036854775807}", w.toString());

    }

}
