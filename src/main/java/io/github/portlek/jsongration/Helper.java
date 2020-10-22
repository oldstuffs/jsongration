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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simpleyaml.configuration.ConfigurationSection;

/**
 * a class that helps to conversion processes.
 */
final class Helper {

  /**
   * ctor.
   */
  private Helper() {
  }

  /**
   * converts the given configuration section into a {@link Map}.
   *
   * @param object the json object to convert.
   * @param section the section to convert.
   */
  static void convertMapToSection(@NotNull final JsonObject object,
                                  @NotNull final ConfigurationSection section) {
    Helper.convertMapToSection(Helper.jsonObjectAsMap(object), section);
  }

  /**
   * converts a {@link Map} into a {@link JsonObject}.
   *
   * @param map the map to convert.
   *
   * @return a json object instance.
   */
  @NotNull
  static JsonObject mapAsJsonObject(@NotNull final Map<?, ?> map) {
    final JsonObject object = new JsonObject();
    map.forEach((key, value) ->
      Helper.objectAsJsonValue(value).ifPresent(jsonValue ->
        object.add(String.valueOf(key), jsonValue)));
    return object;
  }

  /**
   * parses the given json value into a number.
   *
   * @param number the number to parse.
   *
   * @return a number instance.
   */
  @Nullable
  private static Object parseNumber(@NotNull final JsonValue number) {
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

  /**
   * converts the given configuration section into a {@link Map}.
   *
   * @param input the input to convert.
   * @param section the section to convert.
   */
  private static void convertMapToSection(@NotNull final Map<?, ?> input,
                                          @NotNull final ConfigurationSection section) {
    final Map<String, Object> result = Helper.deserialize(input);
    for (final Map.Entry<?, ?> entry : result.entrySet()) {
      final String key = entry.getKey().toString();
      final Object value = entry.getValue();
      if (value instanceof Map<?, ?>) {
        Helper.convertMapToSection((Map<?, ?>) value, section.createSection(key));
      } else {
        section.set(key, value);
      }
    }
  }

  /**
   * deserializes the given input.
   *
   * @param input the input to deserialize.
   *
   * @return deserialized input.
   */
  @NotNull
  private static Collection<Object> deserialize(@NotNull final Iterable<?> input) {
    final Collection<Object> objects = new ArrayList<>();
    input.forEach(o -> {
      if (o instanceof Map) {
        objects.add(Helper.deserialize((Map<?, ?>) o));
      } else if (o instanceof List<?>) {
        objects.add(Helper.deserialize((Iterable<?>) o));
      } else {
        objects.add(o);
      }
    });
    return objects;
  }

  /**
   * deserialized the given input.
   *
   * @param input the input to deserialize.
   *
   * @return deserialized input.
   */
  @NotNull
  private static Map<String, Object> deserialize(@NotNull final Map<?, ?> input) {
    return input.entrySet().stream()
      .collect(Collectors.toMap(
        entry -> Objects.toString(entry.getKey()),
        entry -> {
          final Object value = entry.getValue();
          if (value instanceof Map<?, ?>) {
            return Helper.deserialize((Map<?, ?>) value);
          }
          if (value instanceof Iterable<?>) {
            return Helper.deserialize((Iterable<?>) value);
          }
          if (value instanceof Stream<?>) {
            return Helper.deserialize(((Stream<?>) value).collect(Collectors.toList()));
          }
          return value;
        }));
  }

  /**
   * converts the given json object into a {@link Map}
   *
   * @param object the json object to convert.
   *
   * @return a converted {@link Map} instance.
   */
  @NotNull
  private static Map<String, Object> jsonObjectAsMap(@NotNull final JsonObject object) {
    final Map<String, Object> map = new HashMap<>();
    object.forEach(member ->
      Helper.jsonValueAsObject(member.getValue()).ifPresent(o ->
        map.put(member.getName(), o)));
    return map;
  }

  /**
   * converts the given json value into the real object.
   *
   * @param value the json value to convert.
   *
   * @return a real object instance.
   */
  @NotNull
  private static Optional<Object> jsonValueAsObject(@NotNull final JsonValue value) {
    @Nullable final Object object;
    if (value.isBoolean()) {
      object = value.asBoolean();
    } else if (value.isNumber()) {
      object = Helper.parseNumber(value);
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

  /**
   * converts the given json array into a {@link List}
   *
   * @param array the json array to convert.
   *
   * @return a {@link List} instance.
   */
  @NotNull
  private static List<Object> jsonArrayAsList(@NotNull final JsonArray array) {
    final List<Object> list = new ArrayList<>(array.size());
    array.forEach(element ->
      Helper.jsonValueAsObject(element).ifPresent(list::add));
    return list;
  }

  /**
   * converts the given object into a {@link JsonValue}.
   *
   * @param object the object to convert.
   *
   * @return a {@link JsonValue} instance.
   */
  @NotNull
  private static Optional<JsonValue> objectAsJsonValue(@NotNull final Object object) {
    @Nullable final JsonValue value;
    if (object instanceof Boolean) {
      value = Json.value((boolean) object);
    } else if (object instanceof Integer) {
      value = Json.value((int) object);
    } else if (object instanceof Long) {
      value = Json.value((long) object);
    } else if (object instanceof Float) {
      value = Json.value((float) object);
    } else if (object instanceof Double) {
      value = Json.value((double) object);
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

  /**
   * converts the given iterable into a {@link JsonArray}.
   *
   * @param iterable the iterable to convert.
   *
   * @return a {@link JsonArray} instance.
   */
  @NotNull
  private static JsonArray collectionAsJsonArray(@NotNull final Iterable<?> iterable) {
    final JsonArray array = new JsonArray();
    iterable.forEach(o ->
      Helper.objectAsJsonValue(o).ifPresent(array::add));
    return array;
  }
}
