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
 * Sebastien Rius
 ******************************************************************************/

package org.eclipse.yasson.serializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import javax.json.bind.config.PropertyOrderStrategy;

import org.eclipse.yasson.internal.model.ReverseTreeMap;
import org.eclipse.yasson.serializers.model.AnnotatedWithSerializerType;
import org.eclipse.yasson.serializers.model.Author;
import org.eclipse.yasson.serializers.model.Box;
import org.eclipse.yasson.serializers.model.BoxWithAnnotations;
import org.eclipse.yasson.serializers.model.Crate;
import org.eclipse.yasson.serializers.model.CrateDeserializer;
import org.eclipse.yasson.serializers.model.CrateDeserializerWithConversion;
import org.eclipse.yasson.serializers.model.CrateInner;
import org.eclipse.yasson.serializers.model.CrateJsonObjectDeserializer;
import org.eclipse.yasson.serializers.model.CrateSerializer;
import org.eclipse.yasson.serializers.model.CrateSerializerWithConversion;
import org.eclipse.yasson.serializers.model.GenerifiedPersistentBag;
import org.eclipse.yasson.serializers.model.NumberDeserializer;
import org.eclipse.yasson.serializers.model.NumberSerializer;
import org.eclipse.yasson.serializers.model.PersistentBag;
import org.eclipse.yasson.serializers.model.PersistentBagSerializer;
import org.eclipse.yasson.serializers.model.PojoWithExtendedList;
import org.eclipse.yasson.serializers.model.RecursiveDeserializer;
import org.eclipse.yasson.serializers.model.RecursiveSerializer;
import org.eclipse.yasson.serializers.model.SimpleAnnotatedSerializedArrayContainer;
import org.eclipse.yasson.serializers.model.SimpleContainer;
import org.eclipse.yasson.serializers.model.StringWrapper;
import org.eclipse.yasson.serializers.model.SupertypeSerializerPojo;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Roman Grigoriadi
 */
public class SerializersTest {

    @Test
    public void testClassLevelAnnotation() {
        Crate crate = new Crate();
        crate.crateBigDec = BigDecimal.TEN;
        crate.crateStr = "crateStr";

        crate.annotatedType = new AnnotatedWithSerializerType();
        crate.annotatedType.value = "abc";
        crate.annotatedTypeOverriddenOnProperty = new AnnotatedWithSerializerType();
        crate.annotatedTypeOverriddenOnProperty.value = "def";
        final Jsonb jsonb = JsonbBuilder.create();
        String expected = "{\"annotatedType\":{\"valueField\":\"replaced value\"},\"annotatedTypeOverriddenOnProperty\":{\"valueField\":\"overridden value\"},\"crateBigDec\":10,\"crate_str\":\"crateStr\"}";

        assertEquals(expected, jsonb.toJson(crate));

        Crate result = jsonb.fromJson(expected, Crate.class);
        assertEquals("replaced value", result.annotatedType.value);
        assertEquals("overridden value", result.annotatedTypeOverriddenOnProperty.value);

    }

    /**
     * Tests JSONB deserialization of arbitrary type invoked from a Deserializer.
     */
    @Test
    public void testDeserializerDeserializationByType() {
        JsonbConfig config = new JsonbConfig().withDeserializers(new CrateDeserializer());
        Jsonb jsonb = JsonbBuilder.create(config);

        Box box = createPojoWithDates();

        String expected = "{\"boxStr\":\"Box string\",\"crate\":{\"crateInner\":{\"crateInnerBigDec\":10,\"crate_inner_str\":\"Single inner\",\"date\":\"14.05.2015 || 11:10:01\"},\"crateInnerList\":[{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 0\"},{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 1\"}],\"date\":\"2015-05-14T11:10:01\"},\"secondBoxStr\":\"Second box string\"}";

        Box result = jsonb.fromJson(expected, Box.class);

        //deserialized by deserializationContext.deserialize(Class c)
        assertEquals(box.crate.crateInner.crateInnerBigDec, result.crate.crateInner.crateInnerBigDec);
        assertEquals(box.crate.crateInner.crateInnerStr, result.crate.crateInner.crateInnerStr);

        assertEquals("List inner 0", result.crate.crateInnerList.get(0).crateInnerStr);
        assertEquals("List inner 1", result.crate.crateInnerList.get(1).crateInnerStr);

        //set by deserializer statically
        assertEquals(new BigDecimal("123"), result.crate.crateBigDec);
        assertEquals("abc", result.crate.crateStr);

    }

    /**
     * Tests JSONB serialization of arbitrary type invoked from a Serializer.
     */
    @Test
    public void testSerializerSerializationOfType() {
        JsonbConfig config = new JsonbConfig().withSerializers(new CrateSerializer());
        Jsonb jsonb = JsonbBuilder.create(config);
        String expected = "{\"boxStr\":\"Box string\",\"crate\":{\"crateStr\":\"REPLACED crate str\",\"crateInner\":{\"crateInnerBigDec\":10,\"crate_inner_str\":\"Single inner\"},\"crateInnerList\":[{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 0\"},{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 1\"}],\"crateBigDec\":54321},\"secondBoxStr\":\"Second box string\"}";
        Box pojo = createPojo();

        assertEquals(expected, jsonb.toJson(pojo));

        Box result = jsonb.fromJson(expected, Box.class);
        assertEquals(new BigDecimal("54321"), result.crate.crateBigDec);
        //result.crate.crateStr is mapped to crate_str by jsonb property
        assertNull(result.crate.crateStr);
        assertEquals(pojo.crate.crateInner.crateInnerStr, result.crate.crateInner.crateInnerStr);
        assertEquals(pojo.crate.crateInner.crateInnerBigDec, result.crate.crateInner.crateInnerBigDec);
    }

    /**
     * Tests jsonb type conversion, including property customization.
     */
    @Test
    public void testDeserializersUsingConversion() {
        JsonbConfig config = new JsonbConfig().withDeserializers(new CrateDeserializerWithConversion());
        Jsonb jsonb = JsonbBuilder.create(config);

        String json = "{\"boxStr\":\"Box string\",\"crate\":{\"date-converted\":\"2015-05-14T11:10:01\",\"crateStr\":\"REPLACED crate str\",\"crateInner\":{\"crateInnerBigDec\":10,\"crate_inner_str\":\"Single inner\",\"date\":\"14.05.2015 || 11:10:01\"},\"crateBigDec\":54321},\"secondBoxStr\":\"Second box string\"}";
        Box result = jsonb.fromJson(json, Box.class);
        final Date expected = getExpectedDate();
        assertEquals(expected, result.crate.date);
        assertEquals("Box string", result.boxStr);
        assertEquals("Second box string", result.secondBoxStr);
    }

    @Test
    public void testCrateJsonObjectDeserializer() {
        JsonbConfig config = new JsonbConfig().withDeserializers(new CrateJsonObjectDeserializer());
        Jsonb jsonb = JsonbBuilder.create(config);
        String expected = "{\"boxStr\":\"Box string\",\"crate\":{\"date-converted\":\"2015-05-14T11:10:01\",\"crateStr\":\"REPLACED crate str\",\"crateInner\":{\"crateInnerBigDec\":10,\"crateInnerStr\":\"Single inner\"},\"crateInnerList\":[{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 0\"},{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 1\"}],\"crateBigDec\":54321},\"secondBoxStr\":\"Second box string\"}";
        Box result = jsonb.fromJson(expected, Box.class);
        assertEquals(new BigDecimal("54321"), result.crate.crateBigDec);
        assertEquals("REPLACED crate str", result.crate.crateStr);
        assertEquals("Single inner", result.crate.crateInner.crateInnerStr);
        assertEquals(BigDecimal.TEN, result.crate.crateInner.crateInnerBigDec);
    }

    private Date getExpectedDate() {
        return new Calendar.Builder().setDate(2015, 4, 14).setTimeOfDay(11, 10, 1).setTimeZone(TimeZone.getTimeZone("Z")).build().getTime();
    }

    @Test
    public void testSerializationUsingConversion() {
        JsonbConfig config = new JsonbConfig().withSerializers(new CrateSerializerWithConversion());
        Jsonb jsonb = JsonbBuilder.create(config);

        String json = "{\"boxStr\":\"Box string\",\"crate\":{\"crateStr\":\"REPLACED crate str\",\"crateInner\":{\"crateInnerBigDec\":10,\"crate_inner_str\":\"Single inner\",\"date\":\"14.05.2015 || 11:10:01\"},\"crateInnerList\":[{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 0\"},{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 1\"}],\"crateBigDec\":54321,\"date-converted\":\"2015-05-14T11:10:01Z[UTC]\"},\"secondBoxStr\":\"Second box string\"}";
        assertEquals(json, jsonb.toJson(createPojoWithDates()));
    }

    @Test
    public void testAnnotations() {
        final Jsonb jsonb = JsonbBuilder.create();
        BoxWithAnnotations box = new BoxWithAnnotations();
        box.boxStr = "Box string";
        box.secondBoxStr = "Second box string";
        box.crate = new Crate();
        box.crate.date = getExpectedDate();
        box.crate.crateInner = createCrateInner("Single inner");

        box.crate.crateInnerList = new ArrayList<>();
        box.crate.crateInnerList.add(createCrateInner("List inner 0"));
        box.crate.crateInnerList.add(createCrateInner("List inner 1"));

        String expected = "{\"boxStr\":\"Box string\",\"crate\":{\"crateStr\":\"REPLACED crate str\",\"crateInner\":{\"crateInnerBigDec\":10,\"crate_inner_str\":\"Single inner\"},\"crateInnerList\":[{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 0\"},{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 1\"}],\"crateBigDec\":54321,\"date-converted\":\"2015-05-14T11:10:01Z[UTC]\"},\"secondBoxStr\":\"Second box string\"}";

        assertEquals(expected, jsonb.toJson(box));

        BoxWithAnnotations result = jsonb.fromJson(expected, BoxWithAnnotations.class);

        //deserialized by deserializationContext.deserialize(Class c)
        assertEquals(box.crate.crateInner.crateInnerBigDec, result.crate.crateInner.crateInnerBigDec);
        assertEquals(box.crate.crateInner.crateInnerStr, result.crate.crateInner.crateInnerStr);

        assertEquals(2L, result.crate.crateInnerList.size());
        assertEquals("List inner 0", result.crate.crateInnerList.get(0).crateInnerStr);
        assertEquals("List inner 1", result.crate.crateInnerList.get(1).crateInnerStr);

        //set by deserializer statically
        assertEquals(new BigDecimal("123"), result.crate.crateBigDec);
        assertEquals("abc", result.crate.crateStr);
    }

    @Test
    public void testAnnotationsOverride() {
        JsonbConfig config = new JsonbConfig().withDeserializers(new CrateJsonObjectDeserializer()).withSerializers(new CrateSerializer());
        Jsonb jsonb = JsonbBuilder.create(config);

        BoxWithAnnotations box = new BoxWithAnnotations();
        box.boxStr = "Box string";
        box.secondBoxStr = "Second box string";
        box.crate = new Crate();
        box.crate.crateInner = createCrateInner("Single inner");
        box.crate.date = getExpectedDate();

        box.crate.crateInnerList = new ArrayList<>();
        box.crate.crateInnerList.add(createCrateInner("List inner 0"));
        box.crate.crateInnerList.add(createCrateInner("List inner 1"));

        String expected = "{\"boxStr\":\"Box string\",\"crate\":{\"crateStr\":\"REPLACED crate str\",\"crateInner\":{\"crateInnerBigDec\":10,\"crate_inner_str\":\"Single inner\"},\"crateInnerList\":[{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 0\"},{\"crateInnerBigDec\":10,\"crate_inner_str\":\"List inner 1\"}],\"crateBigDec\":54321,\"date-converted\":\"2015-05-14T11:10:01Z[UTC]\"},\"secondBoxStr\":\"Second box string\"}";

        assertEquals(expected, jsonb.toJson(box));

        BoxWithAnnotations result = jsonb.fromJson(expected, BoxWithAnnotations.class);

        //deserialized by deserializationContext.deserialize(Class c)
        assertEquals(box.crate.crateInner.crateInnerBigDec, result.crate.crateInner.crateInnerBigDec);
        assertEquals(box.crate.crateInner.crateInnerStr, result.crate.crateInner.crateInnerStr);

        assertEquals(2L, result.crate.crateInnerList.size());
        assertEquals("List inner 0", result.crate.crateInnerList.get(0).crateInnerStr);
        assertEquals("List inner 1", result.crate.crateInnerList.get(1).crateInnerStr);

        //set by deserializer statically
        assertEquals(new BigDecimal("123"), result.crate.crateBigDec);
        assertEquals("abc", result.crate.crateStr);
    }

    @Test
    public void testStringField() {
        Jsonb jsonb = JsonbBuilder.create();
        StringWrapper pojo = new StringWrapper();
        pojo.strField = "abc";
        final String result = jsonb.toJson(pojo);
    }

    @Test
    public void testContainerSerializer() {
        Jsonb jsonb = JsonbBuilder.create();

        SimpleAnnotatedSerializedArrayContainer container = new SimpleAnnotatedSerializedArrayContainer();
        SimpleContainer instance1 = new SimpleContainer();
        instance1.setInstance("Test String 1");
        SimpleContainer instance2 = new SimpleContainer();
        instance2.setInstance("Test String 2");
        container.setArrayInstance(new SimpleContainer[] {instance1, instance2});

        container.setListInstance(new ArrayList<>());
        container.getListInstance().add(new SimpleContainer("Test List 1"));
        container.getListInstance().add(new SimpleContainer("Test List 2"));

        String jsonString = jsonb.toJson(container);
        Assert.assertEquals("{\"arrayInstance\":[{\"instance\":\"Test String 1\"},{\"instance\":\"Test String 2\"}],\"listInstance\":[{\"instance\":\"Test List 1\"},{\"instance\":\"Test List 2\"}]}", jsonString);

        SimpleAnnotatedSerializedArrayContainer unmarshalledObject = jsonb.fromJson("{\"arrayInstance\":[{\"instance\":\"Test String 1\"},{\"instance\":\"Test String 2\"}],\"listInstance\":[{\"instance\":\"Test List 1\"},{\"instance\":\"Test List 2\"}]}", SimpleAnnotatedSerializedArrayContainer.class);

        Assert.assertEquals("Test String 1", unmarshalledObject.getArrayInstance()[0].getInstance());
        Assert.assertEquals("Test String 2", unmarshalledObject.getArrayInstance()[1].getInstance());

        Assert.assertEquals("Test List 1", unmarshalledObject.getListInstance().get(0).getInstance());
        Assert.assertEquals("Test List 2", unmarshalledObject.getListInstance().get(1).getInstance());
    }

    /**
     * Tests avoiding StackOverflowError.
     */
    @Test
    public void testRecursiveSerializer() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withSerializers(new RecursiveSerializer()).withDeserializers(new RecursiveDeserializer()));

        Box box = new Box();
        box.boxStr = "Box to serialize";
        try {
            jsonb.toJson(box);
            fail();
        } catch (JsonbException ex) {
        }

        try {
            jsonb.fromJson("{\"boxStr\":\"Box to deserialize\"}", Box.class);
            fail();
        } catch (StackOverflowError error){
        }
    }

    @Test
    public void testAuthor() {
        Author author = new Author("Sarah", "Connor");
        Jsonb jsonb = JsonbBuilder.create();
        String expected = "{\"firstName\":\"S\",\"lastName\":\"Connor\"}";
        String json = jsonb.toJson(author);

        Assert.assertEquals(expected, json);

        Author result = jsonb.fromJson(expected, Author.class);
        Assert.assertEquals("John", result.getFirstName());
        Assert.assertEquals("Connor", result.getLastName());
    }

    @Test
    public void testSupertypeSerializer() {
        Jsonb jsonb = JsonbBuilder.create(
                new JsonbConfig().withSerializers(new NumberSerializer())
                        .withDeserializers(new NumberDeserializer()));
        SupertypeSerializerPojo pojo = new SupertypeSerializerPojo();
        pojo.setNumberInteger(10);
        pojo.setAnotherNumberInteger(11);
        Assert.assertEquals("{\"anotherNumberInteger\":\"12\",\"numberInteger\":\"11\"}", jsonb.toJson(pojo));

        pojo = jsonb.fromJson("{\"anotherNumberInteger\":\"12\",\"numberInteger\":\"11\"}", SupertypeSerializerPojo.class);
        Assert.assertEquals(Integer.valueOf(10), pojo.getNumberInteger());
        Assert.assertEquals(Integer.valueOf(11), pojo.getAnotherNumberInteger());
    }
    
    @Test
    public void testObjectDerializerWithLexOrderStrategy() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL));
        Object pojo = jsonb.fromJson("{\"first\":{},\"third\":{},\"second\":{\"second\":2,\"first\":1}}", Object.class);
        Assert.assertTrue("Pojo is not of type TreeMap", pojo instanceof TreeMap);
        @SuppressWarnings("unchecked")
        SortedMap<String, Object> pojoAsMap = (SortedMap<String, Object>) pojo;
        Assert.assertTrue("Pojo inner object is not of type TreeMap", pojoAsMap.get("second") instanceof TreeMap);
        Assert.assertEquals("{\"first\":{},\"second\":{\"first\":1,\"second\":2},\"third\":{}}", jsonb.toJson(pojo));
    }
    
    @Test
    public void testObjectDerializerWithReverseOrderStrategy() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.REVERSE));
        Object pojo = jsonb.fromJson("{\"first\":{},\"second\":{\"first\":1,\"second\":2},\"third\":{}}", Object.class);
        Assert.assertTrue("Pojo is not of type ReverseTreeMap", pojo instanceof ReverseTreeMap);
        @SuppressWarnings("unchecked")
        SortedMap<String, Object> pojoAsMap = (SortedMap<String, Object>) pojo;
        Assert.assertTrue("Pojo inner object is not of type TreeMap", pojoAsMap.get("second") instanceof TreeMap);
        Assert.assertEquals("{\"third\":{},\"second\":{\"second\":2,\"first\":1},\"first\":{}}", jsonb.toJson(pojo));
    }

    @Test
    public void testObjectDerializerWithAnyOrNoneOrderStrategy() {
        String json = "{\"first\":{},\"second\":{\"first\":1,\"second\":2},\"third\":{}}";
        // ANY
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.ANY));
        Object pojo = jsonb.fromJson(json, Object.class);
        Assert.assertTrue("Pojo is not of type HashMap with \"ANY\" strategy", pojo instanceof HashMap);
        // none
        jsonb = JsonbBuilder.create(new JsonbConfig());
        pojo = jsonb.fromJson(json, Object.class);
        Assert.assertTrue("Pojo is not of type HashMap with no strategy", pojo instanceof HashMap);
    }

    @Test
    public void testSortedMapDerializer() {
        String json = "{\"first\":1,\"third\":3,\"second\":2}";

        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.ANY));
        SortedMap<?, ?> pojo = jsonb.fromJson(json, SortedMap.class);
        Assert.assertTrue("Pojo is not of type TreeMap with \"ANY\" strategy", pojo instanceof TreeMap);

        jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL));
        pojo = jsonb.fromJson(json, SortedMap.class);
        Assert.assertTrue("Pojo is not of type TreeMap with no strategy", pojo instanceof TreeMap);
        Assert.assertEquals("{\"first\":1,\"second\":2,\"third\":3}", jsonb.toJson(pojo));

        jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.REVERSE));
        pojo = jsonb.fromJson(json, SortedMap.class);
        Assert.assertTrue("Pojo is not of type ReverseTreeMap with no strategy", pojo instanceof ReverseTreeMap);
        Assert.assertEquals("{\"third\":3,\"second\":2,\"first\":1}", jsonb.toJson(pojo));

        jsonb = JsonbBuilder.create(new JsonbConfig());
        pojo = jsonb.fromJson(json, SortedMap.class);
        Assert.assertTrue("Pojo is not of type TreeMap with no strategy", pojo instanceof TreeMap);
        Assert.assertEquals("{\"first\":1,\"second\":2,\"third\":3}", jsonb.toJson(pojo));
    }

    @Test
    public void testExtendedListPojo() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PojoWithExtendedList pojo = new PojoWithExtendedList();
        PersistentBag integerBag = new PersistentBag();
        integerBag.add(1);
        integerBag.add(2);
        Method setNumbers = PojoWithExtendedList.class.getMethod("setNumbers", List.class);
        setNumbers.invoke(pojo, integerBag);

        GenerifiedPersistentBag<Number> anotherBag = new GenerifiedPersistentBag<>();
        anotherBag.add(3);
        anotherBag.add(4);
        pojo.setAnotherNumbers(anotherBag);

        JsonbConfig config = new JsonbConfig().withSerializers(new PersistentBagSerializer());
        Jsonb jsonb = JsonbBuilder.create(config);

        Assert.assertEquals("{\"anotherNumbers\":[\"val: 3\",\"val: 4\"],\"numbers\":[\"val: 1\",\"val: 2\"]}",
                jsonb.toJson(pojo));
    }

    private Box createPojoWithDates() {
        Date date = getExpectedDate();
        Box box = createPojo();
        box.crate.date = date;
        box.crate.crateInner.date = date;
        return box;
    }

    private Box createPojo() {
        Box box = new Box();
        box.boxStr = "Box string";
        box.crate = new Crate();
        box.secondBoxStr = "Second box string";


        box.crate.crateInner = createCrateInner("Single inner");

        box.crate.crateInnerList = new ArrayList<>();
        box.crate.crateInnerList.add(createCrateInner("List inner 0"));
        box.crate.crateInnerList.add(createCrateInner("List inner 1"));

        return box;
    }

    private CrateInner createCrateInner(String name) {
        final CrateInner crateInner = new CrateInner();
        crateInner.crateInnerStr = name;
        crateInner.crateInnerBigDec = BigDecimal.TEN;
        return crateInner;
    }


}
