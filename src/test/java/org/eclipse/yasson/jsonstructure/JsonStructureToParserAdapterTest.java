package org.eclipse.yasson.jsonstructure;

import org.eclipse.yasson.TestTypeToken;
import org.eclipse.yasson.YassonJsonb;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.JsonbBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JsonStructureToParserAdapterTest {

    private final YassonJsonb jsonb = (YassonJsonb) JsonbBuilder.create();

    @Test
    public void testBasicJsonObject() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("stringProperty", "value 1");
        objectBuilder.add("bigDecimalProperty", new BigDecimal("1.1"));
        objectBuilder.add("longProperty", 10L);
        JsonObject jsonObject = objectBuilder.build();
        Pojo result = jsonb.fromJson(jsonObject, Pojo.class);
        assertEquals("value 1", result.getStringProperty());
        assertEquals(new BigDecimal("1.1"), result.getBigDecimalProperty());
        assertEquals(Long.valueOf(10), result.getLongProperty());
    }

    @Test
    public void testNullValues() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.addNull("stringProperty");
        objectBuilder.addNull("bigDecimalProperty");
        objectBuilder.add("longProperty", 10L);
        JsonObject jsonObject = objectBuilder.build();
        Pojo result = jsonb.fromJson(jsonObject, Pojo.class);
        assertNull(result.getStringProperty());
        assertNull(result.getBigDecimalProperty());
        assertEquals(Long.valueOf(10), result.getLongProperty());
    }

    @Test
    public void testInnerJsonObjectWrappedWithProperties() {
        JsonObjectBuilder innerBuilder = Json.createObjectBuilder();
        innerBuilder.add("innerFirst", "Inner value 1");
        innerBuilder.add("innerSecond", "Inner value 2");

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        objectBuilder.add("stringProperty", "value 1");
        objectBuilder.add("inner", innerBuilder.build());
        objectBuilder.add("bigDecimalProperty", new BigDecimal("1.1"));
        objectBuilder.add("longProperty", 10L);
        JsonObject jsonObject = objectBuilder.build();
        Pojo result = jsonb.fromJson(jsonObject, Pojo.class);

        assertEquals("value 1", result.getStringProperty());
        assertEquals(new BigDecimal("1.1"), result.getBigDecimalProperty());
        assertEquals(Long.valueOf(10), result.getLongProperty());
        assertEquals("Inner value 1", result.getInner().getInnerFirst());
        assertEquals("Inner value 2", result.getInner().getInnerSecond());
    }

    @Test
    public void testInnerJsonObjectAtEndProperty() {
        JsonObjectBuilder innerBuilder = Json.createObjectBuilder();
        innerBuilder.add("innerFirst", "Inner value 1");
        innerBuilder.add("innerSecond", "Inner value 2");

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        objectBuilder.add("stringProperty", "value 1");
        objectBuilder.add("bigDecimalProperty", new BigDecimal("1.1"));
        objectBuilder.add("longProperty", 10L);
        objectBuilder.add("inner", innerBuilder.build());

        JsonObject jsonObject = objectBuilder.build();
        Pojo result = jsonb.fromJson(jsonObject, Pojo.class);

        assertEquals("value 1", result.getStringProperty());
        assertEquals(new BigDecimal("1.1"), result.getBigDecimalProperty());
        assertEquals(Long.valueOf(10), result.getLongProperty());
        assertEquals("Inner value 1", result.getInner().getInnerFirst());
        assertEquals("Inner value 2", result.getInner().getInnerSecond());

    }

    @Test
    public void testEmptyJsonObject() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonObject jsonObject = objectBuilder.build();
        Pojo result = jsonb.fromJson(jsonObject, Pojo.class);
        assertNull(result.getStringProperty());
        assertNull(result.getBigDecimalProperty());
        assertNull(result.getLongProperty());
    }

    @Test
    public void testEmptyInnerJsonObject() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        JsonObjectBuilder innerBuilder = Json.createObjectBuilder();
        JsonObject innerObject = innerBuilder.build();

        objectBuilder.add("inner", innerObject);

        JsonObject jsonObject = objectBuilder.build();

        Pojo result = jsonb.fromJson(jsonObject, Pojo.class);
        assertNull(result.getStringProperty());
        assertNull(result.getBigDecimalProperty());
        assertNull(result.getLongProperty());

        assertNotNull(result.getInner());
        assertNull(result.getInner().getInnerFirst());
        assertNull(result.getInner().getInnerSecond());
    }

    @Test
    public void testSimpleArray() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        arrayBuilder.add(BigDecimal.TEN).add("String value").addNull();
        JsonArray jsonArray = arrayBuilder.build();
        List result = jsonb.fromJson(jsonArray, ArrayList.class);
        assertEquals(3, result.size());
        assertEquals(BigDecimal.TEN, result.get(0));
        assertEquals("String value", result.get(1));
        assertNull(result.get(2));
    }

    @Test
    public void testArraysInsideObject() {
        JsonArrayBuilder bigDecBuilder = Json.createArrayBuilder();
        JsonArrayBuilder strBuilder = Json.createArrayBuilder();
        JsonArrayBuilder blnBuilder = Json.createArrayBuilder();

        bigDecBuilder.add(BigDecimal.TEN);
        strBuilder.add("String value 1");
        blnBuilder.add(Boolean.TRUE);

        JsonObjectBuilder pojoBuilder = Json.createObjectBuilder();
        pojoBuilder.add("strings", strBuilder.build());
        pojoBuilder.add("bigDecimals", bigDecBuilder.build());
        pojoBuilder.add("booleans", blnBuilder.build());

        JsonObject jsonObject = pojoBuilder.build();
        Pojo pojo = jsonb.fromJson(jsonObject, Pojo.class);

        assertEquals(1, pojo.getBigDecimals().size());
        assertEquals(1, pojo.getStrings().size());
        assertEquals(1, pojo.getBooleans().size());
    }

    @Test
    public void testNestedArrays() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        JsonArrayBuilder innerArrBuilder = Json.createArrayBuilder();
        innerArrBuilder.add("first").add("second");
        arrayBuilder.add(BigDecimal.TEN);
        arrayBuilder.add(innerArrBuilder.build());

        JsonArray jsonArray = arrayBuilder.build();

        ArrayList result = jsonb.fromJson(jsonArray, ArrayList.class);
        assertEquals(2, result.size());
        assertEquals(BigDecimal.TEN, result.get(0));
        assertTrue(result.get(1) instanceof List);
        List inner = (List) result.get(1);
        assertEquals(2, inner.size());
        assertEquals("first", inner.get(0));
        assertEquals("second", inner.get(1));
    }

    @Test
    public void testObjectsNestedInArrays() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("stringProperty", "value 1");
        objectBuilder.add("bigDecimalProperty", new BigDecimal("1.1"));
        objectBuilder.add("longProperty", 10L);

        JsonArrayBuilder innerArrayBuilder = Json.createArrayBuilder();
        innerArrayBuilder.add("String value 1");
        objectBuilder.add("strings", innerArrayBuilder.build());

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        arrayBuilder.add(objectBuilder.build());

        JsonArray rootArray = arrayBuilder.build();

        List<Object> result = jsonb.fromJson(rootArray, new TestTypeToken<List<Pojo>>(){}.getType());
        assertTrue(result.get(0) instanceof Pojo);
        Pojo pojo = (Pojo) result.get(0);
        assertNotNull(pojo);
        assertEquals("value 1", pojo.getStringProperty());
        assertEquals(new BigDecimal("1.1"), pojo.getBigDecimalProperty());
        assertEquals(Long.valueOf(10), pojo.getLongProperty());
        assertNotNull(pojo.getStrings());
        assertEquals(1, pojo.getStrings().size());
        assertEquals("String value 1", pojo.getStrings().get(0));
    }

    @Test
    public void testObjectsNestedInArraysRaw() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("stringProperty", "value 1");
        objectBuilder.add("bigDecimalProperty", new BigDecimal("1.1"));
        objectBuilder.add("longProperty", 10L);

        JsonArrayBuilder innerArrayBuilder = Json.createArrayBuilder();
        innerArrayBuilder.add("String value 1");

        objectBuilder.add("strings", innerArrayBuilder.build());

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        arrayBuilder.add(10L);
        arrayBuilder.add(objectBuilder.build());
        arrayBuilder.add("10");

        JsonArray rootArray = arrayBuilder.build();

        List<Object> result = jsonb.fromJson(rootArray, new TestTypeToken<List<Object>>(){}.getType());
        assertEquals(new BigDecimal("10"), result.get(0));
        assertTrue(result.get(1) instanceof Map);
        Map pojo = (Map) result.get(1);
        assertNotNull(pojo);
        assertEquals("value 1", pojo.get("stringProperty"));
        assertEquals(new BigDecimal("1.1"), pojo.get("bigDecimalProperty"));
        assertEquals(new BigDecimal(10), pojo.get("longProperty"));
        assertTrue(pojo.get("strings") instanceof List);
        List strings = (List) pojo.get("strings");
        assertNotNull(strings);
        assertEquals(1, strings.size());
        assertEquals("String value 1", strings.get(0));
    }


}
