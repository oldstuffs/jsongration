/*
 * MIT License
 *
 * Copyright (c) 2020 Hasan Demirtaş
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions from the Software.
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
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import java.io.File;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.FileConfiguration;
import org.simpleyaml.exceptions.InvalidConfigurationException;

/**
 * a JSON implementation for {@link FileConfiguration}.
 */
public final class JsonConfiguration extends FileConfiguration {

  /**
   * the blank configuration file.
   */
  private static final String BLANK_CONFIG = "{}\n";

  /**
   * loads the given file and converts into the json configuration instance.
   *
   * @param file the file to load.
   *
   * @return the json configuration instance.
   */
  @NotNull
  public static JsonConfiguration loadConfiguration(@NotNull final File file) {
    return JsonConfiguration.loadConfiguration(new JsonConfiguration(), file);
  }

  /**
   * loads the given file and returns the given json configuration.
   *
   * @param config the configuration to load.
   * @param file the file to load.
   *
   * @return the given config instance.
   */
  @NotNull
  private static JsonConfiguration loadConfiguration(@NotNull final JsonConfiguration config,
                                                     @NotNull final File file) {
    try {
      config.load(file);
    } catch (final IOException | InvalidConfigurationException exception) {
      throw new IllegalStateException(exception);
    }
    return config;
  }

  @NotNull
  @Override
  public String saveToString() {
    final String dump = Helper.mapAsJsonObject(this.getValues(false))
      .toString(WriterConfig.PRETTY_PRINT);
    if (dump.equals(JsonConfiguration.BLANK_CONFIG)) {
      return "";
    }
    return dump;
  }

  @Override
  public void loadFromString(@NotNull final String contents) {
    if (contents.isEmpty()) {
      return;
    }
    final JsonValue parse = Json.parse(contents);
    if (!parse.isObject()) {
      return;
    }
    Helper.convertMapToSection(parse.asObject(), this);
  }

  @NotNull
  @Override
  public JsonConfigurationOptions options() {
    if (this.options == null) {
      this.options = new JsonConfigurationOptions(this);
    }
    return (JsonConfigurationOptions) this.options;
  }

  @Override
  protected String buildHeader() {
    return "";
  }
}
