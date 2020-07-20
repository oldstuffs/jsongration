/*
 * MIT License
 *
 * Copyright (c) 2020 Hasan Demirtaş
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.portlek.jsongration;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
class Helper {

    public void convertMapToSection(@NotNull final JsonObject object,
                                    @NotNull final ConfigurationSection section) {
        convertMapToSection(Helper.jsonObjectAsMap(object), section);
    }

    private void convertMapToSection(@NotNull final Map<?, ?> input,
                                     @NotNull final ConfigurationSection section) {
        final Map<String, Object> result = deserialize(input);
        for (final Map.Entry<?, ?> entry : result.entrySet()) {
            final String key = entry.getKey().toString();
            final Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                convertMapToSection((Map<?, ?>) value, section.createSection(key));
            } else {
                section.set(key, value);
            }
        }
    }

    @NotNull
    private Collection<Object> deserialize(@NotNull final Iterable<?> input) {
        final Collection<Object> objects = new ArrayList<>();
        input.forEach(o -> {
            if (o instanceof Map) {
                objects.add(deserialize((Map<?, ?>) o));
            } else if (o instanceof List<?>) {
                objects.add(deserialize((Iterable<?>) o));
            } else {
                objects.add(o);
            }
        });
        return objects;
    }

    @NotNull
    private Map<String, Object> deserialize(@NotNull final Map<?, ?> input) {
        return input.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> Objects.toString(entry.getKey()),
                entry -> {
                    final Object value = entry.getValue();
                    if (value instanceof Map<?, ?>) {
                        return deserialize((Map<?, ?>) value);
                    }
                    if (value instanceof Iterable<?>) {
                        return deserialize((Iterable<?>) value);
                    }
                    if (value instanceof Stream<?>) {
                        return deserialize(((Stream<?>) value).collect(Collectors.toList()));
                    }
                    return value;
                }));
    }

    @NotNull
    public JsonObject mapAsJsonObject(@NotNull final Map<?, ?> map) {
        final JsonObject object = new JsonObject();
        map.forEach((key, value) ->
            Helper.objectAsJsonValue(value).ifPresent(jsonValue ->
                object.add(String.valueOf(key), jsonValue)));
        return object;
    }

    @NotNull
    private Map<String, Object> jsonObjectAsMap(@NotNull final JsonObject object) {
        final Map<String, Object> map = new HashMap<>();
        object.forEach(member ->
            Helper.jsonValueAsObject(member.getValue()).ifPresent(o ->
                map.put(member.getName(), o)));
        return map;
    }

    @NotNull
    private Optional<Object> jsonValueAsObject(@NotNull final JsonValue value) {
        @Nullable final Object object;
        if (value.isBoolean()) {
            object = value.asBoolean();
        } else if (value.isNumber()) {
            object = parseNumber(value);
        } else if (value.isString()) {
            object = value.asString();
        } else if (value.isArray()) {
            object = Helper.jsonArrayAsList(value.asArray());
        } else if (value.isObject()) {
            object = Helper.jsonObjectAsMap(value.asObject());
        } else {
            object = null;
        }
        return Optional.ofNullable(object);
    }

    @NotNull
    private List<Object> jsonArrayAsList(@NotNull final JsonArray array) {
        final List<Object> list = new ArrayList<>(array.size());
        array.forEach(element ->
            Helper.jsonValueAsObject(element).ifPresent(list::add));
        return list;
    }

    @NotNull
    private Optional<JsonValue> objectAsJsonValue(@NotNull final Object object) {
        @Nullable final JsonValue value;
        if (object instanceof Boolean) {
            value = Json.value(((boolean) object));
        } else if (object instanceof Integer) {
            value = Json.value(((int) object));
        } else if (object instanceof Long) {
            value = Json.value(((long) object));
        } else if (object instanceof Float) {
            value = Json.value(((float) object));
        } else if (object instanceof Double) {
            value = Json.value(object);
        } else if (object instanceof String) {
            value = Json.value((String) object);
        } else if (object instanceof Iterable<?>) {
            value = Helper.collectionAsJsonArray((Iterable<?>) object);
        } else if (object instanceof Map<?, ?>) {
            value = Helper.mapAsJsonObject((Map<?, ?>) object);
        } else if (object instanceof ConfigurationSection) {
            value = Helper.mapAsJsonObject(((ConfigurationSection) object).getValues(false));
        } else {
            value = null;
        }
        return Optional.ofNullable(value);
    }

    @NotNull
    private JsonArray collectionAsJsonArray(@NotNull final Iterable<?> collection) {
        final JsonArray array = new JsonArray();
        collection.forEach(o ->
            Helper.objectAsJsonValue(o).ifPresent(array::add));
        return array;
    }

    @Nullable
    public Object parseNumber(@NotNull final JsonValue number) {
        try {
            return number.asInt();
        } catch (final NumberFormatException e) {
            try {
                return number.asLong();
            } catch (final NumberFormatException e1) {
                try {
                    return number.asDouble();
                } catch (final NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

}
