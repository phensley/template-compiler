/**
 * Copyright (c) 2017 SQUARESPACE, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squarespace.template.plugins.platform.i18n;

import java.math.BigDecimal;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.JsonNode;
import com.squarespace.cldr.MessageArg;
import com.squarespace.cldr.MessageFormat;
import com.squarespace.template.Context;
import com.squarespace.template.Frame;
import com.squarespace.template.GeneralUtils;


/**
 * Argument for MessageFormat allowing a formatter to resolve
 * the argument's value on demand.
 */
public class MsgArg implements MessageArg {

  protected final Object[] name;
  protected Context ctx;
  protected JsonNode node;
  protected JsonNode currencyNode;
  protected boolean castFailed;
  protected String value;
  protected String currencyCode;
  protected ZoneId timeZone;
  protected BigDecimal number;

  public MsgArg(Object[] name) {
    this.name = name;
  }

  public void reset() {
    this.node = null;
    this.currencyNode = null;
    this.castFailed = false;
    this.value = null;
    this.currencyCode = null;
    this.timeZone = null;
    this.number = null;
  }

  /**
   * Sets the context used to resolve the argument's value.
   */
  public void setContext(Context ctx) {
    this.ctx = ctx;
  }

  @Override
  public boolean resolve() {
    if (this.node == null) {
      // Since the message string is the node on the current stack frame, the
      // variable reference '@' will point to it, instead of the parent scope.
      // Skip the current frame so we avoid trying to resolve variables against
      // the message string.
      Frame parent = ctx.frame().parent();
      this.node = ctx.resolve(name, parent == null ? ctx.frame() : parent);

      // Peek to see if this argument is a Money
      JsonNode decimal = node.path("decimalValue");
      if (!decimal.isMissingNode()) {
        this.node = decimal;
        this.currencyNode = node.path("currencyCode");
      }
    }
    return true;
  }

  @Override
  public String currency() {
    if (this.currencyCode == null && this.currencyNode != null) {
      this.currencyCode = this.currencyNode.asText();
    }
    return this.currencyCode;
  }

  /**
   * TODO: underlying cldr message format supports a per-argument timezone,
   * but we currently have no standardized time object which associates
   * a timezone with an instant. Once that exists we can obtain the zoneId
   * in resolve() above.
   */
  @Override
  public ZoneId timeZone() {
    return this.timeZone;
  }

  @Override
  public String asString() {
    if (this.value == null) {
      this.value = node == null ? "" : node.asText();
    }
    return this.value;
  }

  @Override
  public BigDecimal asBigDecimal() {
    if (!castFailed) {
      this.number = node == null ? null : GeneralUtils.nodeToBigDecimal(node);
      if (number == null) {
        castFailed = true;
      }
    }
    return number;
  }

  @Override
  public long asLong() {
    if (this.value == null) {
      this.value = node == null ? "0" : node.asText();
    }
    return value == null ? 0 : MessageFormat.toLong(value, 0, value.length());
  }

}