/**
 * Copyright (c) 2015 SQUARESPACE, Inc.
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

package com.squarespace.template.plugins.platform;

import static com.squarespace.template.GeneralUtils.executeTemplate;
import static com.squarespace.template.GeneralUtils.getOrDefault;
import static com.squarespace.template.GeneralUtils.loadResource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squarespace.cldr.CLDR;
import com.squarespace.template.Arguments;
import com.squarespace.template.BaseFormatter;
import com.squarespace.template.CodeException;
import com.squarespace.template.CodeExecuteException;
import com.squarespace.template.Compiler;
import com.squarespace.template.Constants;
import com.squarespace.template.Context;
import com.squarespace.template.Formatter;
import com.squarespace.template.FormatterRegistry;
import com.squarespace.template.GeneralUtils;
import com.squarespace.template.Instruction;
import com.squarespace.template.JsonUtils;
import com.squarespace.template.StringView;
import com.squarespace.template.SymbolTable;
import com.squarespace.template.Variable;
import com.squarespace.template.Variables;
import com.squarespace.template.plugins.PluginUtils;
import com.squarespace.template.plugins.platform.enums.ProductType;


/**
 * Extracted from Commons library at commit ab4ba7a6f2b872a31cb6449ae9e96f5f5b30f471
 */
public class CommerceFormatters implements FormatterRegistry {

  @Override
  public void registerFormatters(SymbolTable<StringView, Formatter> table) {
    table.add(new AddToCartButtonFormatter());
    table.add(new BookkeeperMoneyFormatter());
    table.add(new CartQuantityFormatter());
    table.add(new CartSubtotalFormatter());
    table.add(new CartUrlFormatter());
    table.add(new FromPriceFormatter());
    table.add(new MoneyCamelFormatter());
    table.add(new MoneyDashFormatter());
    table.add(new MoneyStringFormatter());
    table.add(new NormalPriceFormatter());
    table.add(new PercentageFormatter());
    table.add(new ProductCheckoutFormatter());
    table.add(new ProductPriceFormatter());
    table.add(new ProductQuickViewFormatter());
    table.add(new ProductStatusFormatter());
    table.add(new QuantityInputFormatter());
    table.add(new SalePriceFormatter());
    table.add(new SummaryFormFieldFormatter());
    table.add(new VariantDescriptorFormatter());
    table.add(new VariantsSelectFormatter());
  }

  protected static class AddToCartButtonFormatter extends BaseFormatter {

    private Instruction template;

    public AddToCartButtonFormatter() {
      super("add-to-cart-btn", false);
    }

    @Override
    public void initialize(Compiler compiler) throws CodeException {
      String source = loadResource(CommerceFormatters.class, "add-to-cart-btn.html");
      this.template = compiler.compile(source).code();
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      var.set(executeTemplate(ctx, template, var.node(), false));
    }
  }

  protected static class CartQuantityFormatter extends BaseFormatter {

    public CartQuantityFormatter() {
      super("cart-quantity", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      int count = 0;
      JsonNode entriesNode = var.node().path("entries");
      for (int i = 0; i < entriesNode.size(); i++) {
        count += entriesNode.get(i).get("quantity").intValue();
      }

      StringBuilder buf = new StringBuilder();
      buf.append("<span class=\"sqs-cart-quantity\">").append(count).append("</span>");
      var.set(buf);
    }
  }

  /**
   * @deprecated this formatter is not used internally anymore. Remove it when all external usage is cleared.
   */
  @Deprecated
  protected static class CartSubtotalFormatter extends BaseFormatter {

    public CartSubtotalFormatter() {
      super("cart-subtotal", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      double subtotalCents = var.node().path("subtotalCents").doubleValue();

      StringBuilder buf = new StringBuilder();
      buf.append("<span class=\"sqs-cart-subtotal\">");
      CommerceUtils.writeLegacyMoneyString(subtotalCents, buf);
      buf.append("</span>");
      var.set(buf);
    }
  }

  protected static class CartUrlFormatter extends BaseFormatter {

    public CartUrlFormatter() {
      super("cart-url", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      variables.first().set("/cart");
    }
  }

  /**
   * @deprecated this formatter is not used internally anymore. Remove it when all external usage is cleared.
   */
  @Deprecated
  protected static class FromPriceFormatter extends BaseFormatter {

    public FromPriceFormatter() {
      super("from-price", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable item = variables.first();
      JsonNode moneyNode = CommerceUtils.getFromPriceMoneyNode(item.node());
      double legacyPrice = CommerceUtils.getLegacyPriceFromMoneyNode(moneyNode);
      item.set(legacyPrice);
    }
  };

  /**
   * @deprecated this formatter is not used internally anymore. Remove it when all external usage is cleared.
   */
  @Deprecated
  protected abstract static class MoneyBaseFormatter extends BaseFormatter {

    public MoneyBaseFormatter(String identifier) {
      super(identifier, false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      double value = var.node().asDouble();
      var.set(PluginUtils.formatMoney(value, Locale.US));
    }
  }

  /**
   * @deprecated this formatter is not used internally anymore. Remove it when all external usage is cleared.
   */
  @Deprecated
  protected static class MoneyCamelFormatter extends MoneyBaseFormatter {

    public MoneyCamelFormatter() {
      super("moneyFormat");
    }

  }

  /**
   * @deprecated this formatter is not used internally anymore. Remove it when all external usage is cleared.
   */
  @Deprecated
  protected static class MoneyDashFormatter extends MoneyBaseFormatter {

    public MoneyDashFormatter() {
      super("money-format");
    }
  }

  protected static class BookkeeperMoneyFormatter extends BaseFormatter {

    public BookkeeperMoneyFormatter() {
      super("bookkeeper-money-format", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      double value = var.node().asDouble();
      var.set(PlatformUtils.formatBookkeeperMoney(value, Locale.US));
    }
  }

  /**
   * @deprecated this formatter is not used internally anymore. Remove it when all external usage is cleared.
   */
  @Deprecated
  protected static class MoneyStringFormatter extends BaseFormatter {

    public MoneyStringFormatter() {
      super("money-string", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      double value = var.node().asDouble();
      StringBuilder buf = new StringBuilder();
      CommerceUtils.writeLegacyMoneyString(value, buf);
      var.set(buf);
    }
  }

  protected static class PercentageFormatter extends BaseFormatter {

    public PercentageFormatter() {
      super("percentage-format", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      double value = var.node().asDouble();
      StringBuilder buf = new StringBuilder();
      boolean trim = args.count() > 0 && args.first().equals("trim");
      String formatted = PlatformUtils.formatPercentage(value, trim, Locale.US);
      buf.append(formatted);
      var.set(buf);
    }
  }

  /**
   * @deprecated this formatter is not used internally anymore. Remove it when all external usage is cleared.
   */
  @Deprecated
  protected static class NormalPriceFormatter extends BaseFormatter {

    public NormalPriceFormatter() {
      super("normal-price", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable item = variables.first();
      JsonNode moneyNode = CommerceUtils.getNormalPriceMoneyNode(item.node());
      double legacyPrice = CommerceUtils.getLegacyPriceFromMoneyNode(moneyNode);
      item.set(legacyPrice);
    }
  }

  protected static class ProductCheckoutFormatter extends BaseFormatter {

    private static final String SOURCE = "{@|variants-select}{@|quantity-input}{@|add-to-cart-btn}";

    private Instruction template;

    public ProductCheckoutFormatter() {
      super("product-checkout", false);
    }

    @Override
    public void initialize(Compiler compiler) throws CodeException {
      this.template = compiler.compile(SOURCE).code();
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      var.set(executeTemplate(ctx, template, var.node(), false));
    }
  }

  protected static class ProductPriceFormatter extends BaseFormatter {

    private Instruction template;

    public ProductPriceFormatter() {
      super("product-price", false);
    }

    @Override
    public void initialize(Compiler compiler) throws CodeException {
        String source = loadResource(CommerceFormatters.class, "product-price.html");
        this.template = compiler.compile(source.trim()).code();
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      JsonNode node = var.node();
      StringBuilder buf = new StringBuilder();

      ObjectNode obj = JsonUtils.createObjectNode();
      if (CommerceUtils.getProductType(node) != ProductType.UNDEFINED) {
        if (CommerceUtils.hasVariedPrices(node)) {
          String fromText = ctx.resolve(Constants.PRODUCT_PRICE_FROM_TEXT_KEY).asText();
          obj.put("fromText", StringUtils.defaultIfEmpty(fromText, "from {fromPrice}"));
          obj.put("formattedFromPrice", getMoneyString(CommerceUtils.getFromPriceMoneyNode(node), ctx));
        }
        if (CommerceUtils.isOnSale(node)) {
          obj.put("formattedSalePrice", getMoneyString(CommerceUtils.getSalePriceMoneyNode(node), ctx));
        }
        obj.put("formattedNormalPrice", getMoneyString(CommerceUtils.getNormalPriceMoneyNode(node), ctx));
      }
      JsonNode priceInfo = executeTemplate(ctx, template, obj, true);
      buf.append(priceInfo.asText());
      var.set(buf);
    }

    private static String getMoneyString(JsonNode moneyNode, Context ctx) {
      if (useCLDRMode(ctx)) {
        BigDecimal amount = CommerceUtils.getAmountFromMoneyNode(moneyNode);
        String currencyCode = CommerceUtils.getCurrencyFromMoneyNode(moneyNode);
        CLDR.Locale locale = ctx.cldrLocale();
        return CommerceUtils.getCLDRMoneyString(amount, currencyCode, locale);
      } else {
        double legacyAmount = CommerceUtils.getLegacyPriceFromMoneyNode(moneyNode);
        StringBuilder buf = new StringBuilder();
        CommerceUtils.writeLegacyMoneyString(legacyAmount, buf);
        return buf.toString();
      }
    }

    private static boolean useCLDRMode(Context ctx) {
      return GeneralUtils.isTruthy(ctx.resolve(Constants.CLDR_MONEYFORMAT_KEY));
    }
  }

  protected static class ProductQuickViewFormatter extends BaseFormatter {

    public ProductQuickViewFormatter() {
      super("product-quick-view", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      JsonNode node = var.node();
      String id = node.path("id").asText();
      String group = args.isEmpty() ? "" : args.first();

      // check to see if the group is a key that lives in the context or higher up
      JsonNode groupNode = node.path(group);
      if (!groupNode.isMissingNode()) {
        group = groupNode.asText();
      } else {
        groupNode = ctx.resolve(group);
        if (!groupNode.isMissingNode()) {
          group = groupNode.asText();
        }
      }

      StringBuilder buf = new StringBuilder();
      buf.append("<span class=\"sqs-product-quick-view-button\" data-id=\"").append(id);
      buf.append("\" data-group=\"").append(group).append("\">");
      String text = ctx.resolve(Constants.PRODUCT_QUICK_VIEW_TEXT_KEY).asText();
      buf.append(StringUtils.defaultIfEmpty(text, "Quick View"));
      buf.append("</span>");
      var.set(buf);
    }
  }

  protected static class ProductStatusFormatter extends BaseFormatter {

    public ProductStatusFormatter() {
      super("product-status", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      JsonNode node = var.node();
      StringBuilder buf = new StringBuilder();
      if (CommerceUtils.isSoldOut(node)) {
        String text = ctx.resolve(Constants.PRODUCT_SOLD_OUT_TEXT_KEY).asText();
        buf.append("<div class=\"product-mark sold-out\">");
        buf.append(StringUtils.defaultIfEmpty(text, "sold out"));
        buf.append("</div>");
        var.set(buf);
      } else if (CommerceUtils.isOnSale(node)) {
        String text = ctx.resolve(Constants.PRODUCT_SALE_TEXT_KEY).asText();
        buf.append("<div class=\"product-mark sale\">");
        buf.append(StringUtils.defaultIfEmpty(text, "sale"));
        buf.append("</div>");
        var.set(buf);
      } else {
        var.setMissing();
      }
    }
  }

  protected static class QuantityInputFormatter extends BaseFormatter {

    private Instruction template;

    public QuantityInputFormatter() {
      super("quantity-input", false);
    }

    @Override
    public void initialize(Compiler compiler) throws CodeException {
      String source = loadResource(CommerceFormatters.class, "quantity-input.html");
      this.template = compiler.compile(source).code();
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      JsonNode node = var.node();
      ProductType type = CommerceUtils.getProductType(node);

      boolean multipleQuantityAllowed = ProductType.PHYSICAL.equals(type)
          || (ProductType.SERVICE.equals(type)
             && CommerceUtils.isMultipleQuantityAllowedForServices(ctx.resolve("websiteSettings")));
      boolean hideQuantityInput = !multipleQuantityAllowed || CommerceUtils.getTotalStockRemaining(node) <= 1;

      if (hideQuantityInput) {
        var.setMissing();
        return;
      }
      var.set(executeTemplate(ctx, template, var.node(), false));
    }
  }

  protected static class SalePriceFormatter extends BaseFormatter {

    public SalePriceFormatter() {
      super("sale-price", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      JsonNode moneyNode = CommerceUtils.getSalePriceMoneyNode(var.node());
      double legacyPrice = CommerceUtils.getLegacyPriceFromMoneyNode(moneyNode);
      var.set(legacyPrice);
    }
  }

  protected static class VariantDescriptorFormatter extends BaseFormatter {

    public VariantDescriptorFormatter() {
      super("variant-descriptor", false);
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      StringBuilder buf = new StringBuilder();
      CommerceUtils.writeVariantFormat(var.node(), buf);
      var.set(buf);
    }
  }

  protected static class VariantsSelectFormatter extends BaseFormatter {

    private Instruction template;

    public VariantsSelectFormatter() {
      super("variants-select", false);
    }

    @Override
    public void initialize(Compiler compiler) throws CodeException {
      String source = loadResource(CommerceFormatters.class, "variants-select.html");
      this.template = compiler.compile(source).code();
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      JsonNode node = var.node();

      ArrayNode options = CommerceUtils.getItemVariantOptions(node);
      if (options.size() == 0) {
        // Don't bother executing the template of nothing would be emitted.
        var.setMissing();
        return;
      }

      ObjectNode obj = JsonUtils.createObjectNode();
      obj.set("item", node);
      obj.set("options", options);
      var.set(executeTemplate(ctx, template, obj, false));
    }
  }

  protected static class SummaryFormFieldFormatter extends BaseFormatter {

    private static final String[] TEMPLATES = new String[] {
      "address", "checkbox", "date", "likert", "name", "phone", "time"
    };

    private final Map<String, Instruction> templateMap = new HashMap<>();

    public SummaryFormFieldFormatter() {
      super("summary-form-field", false);
    }

    @Override
    public void initialize(Compiler compiler) throws CodeException {
      for (String type : TEMPLATES) {
        String source = loadResource(CommerceFormatters.class, "summary-form-field-" + type + ".html");
        Instruction code = compiler.compile(source.trim()).code();
        templateMap.put(type, code);
      }
    }

    @Override
    public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
      Variable var = variables.first();
      JsonNode field = var.node();
      String type = field.path("type").asText();
      Instruction code = templateMap.get(type);
      JsonNode value = null;
      if (code == null) {
        value = field.path("value");
      } else {
        JsonNode node = field;
        if (type.equals("likert")) {
          Map<String, String> answerMap = buildAnswerMap(ctx.resolve("localizedStrings"));
          node = convertLikert(field.path("values"), answerMap);
        }
        value = executeTemplate(ctx, code, node, true);
      }

      // Assemble the HTML form wrapper containing the rendered value.
      StringBuilder buf = new StringBuilder();
      buf.append("<div style=\"font-size:11px; margin-top:3px\">\n");
      buf.append("  <span style=\"font-weight:bold;\">");
      buf.append(field.path("rawTitle").asText());
      buf.append(":</span> ");
      if (GeneralUtils.isTruthy(value)) {
        buf.append(value.asText());
      } else {
        String text = ctx.resolve(Constants.PRODUCT_SUMMARY_FORM_NO_ANSWER_TEXT_KEY).asText();
        buf.append(StringUtils.defaultIfEmpty(text, "N/A"));
      }
      buf.append("\n</div>");

      var.set(buf);
    }

    private static JsonNode convertLikert(JsonNode values, Map<String, String> answerMap) {
      ArrayNode result = JsonUtils.createArrayNode();
      Iterator<Entry<String, JsonNode>> likertFields = values.fields();
      while (likertFields.hasNext()) {
        Entry<String, JsonNode> likertField = likertFields.next();
        String answer = likertField.getValue().asText();
        ObjectNode node = JsonUtils.createObjectNode();
        node.put("question", likertField.getKey());
        node.put("answer", getOrDefault(answerMap, answer, answerMap.get("0")));
        result.add(node);
      }
      return result;
    }

    private static final String KEY_PREFIX = "productAnswerMap";
    private static final String KEY_STRONGLY_DISAGREE = KEY_PREFIX + "StronglyDisagree";
    private static final String KEY_DISAGREE = KEY_PREFIX + "Disagree";
    private static final String KEY_NEUTRAL = KEY_PREFIX + "Neutral";
    private static final String KEY_AGREE = KEY_PREFIX + "Agree";
    private static final String KEY_STRONGLY_AGREE = KEY_PREFIX + "StronglyAgree";

    private Map<String, String> buildAnswerMap(JsonNode strings) {
      Map<String, String> map = new HashMap<>();
      map.put("-2", GeneralUtils.localizeOrDefault(strings, KEY_STRONGLY_DISAGREE, "Strongly Disagree"));
      map.put("-1", GeneralUtils.localizeOrDefault(strings, KEY_DISAGREE, "Disagree"));
      map.put("0", GeneralUtils.localizeOrDefault(strings, KEY_NEUTRAL, "Neutral"));
      map.put("1", GeneralUtils.localizeOrDefault(strings, KEY_AGREE, "Agree"));
      map.put("2", GeneralUtils.localizeOrDefault(strings, KEY_STRONGLY_AGREE, "Strongly Agree"));
      return map;
    }
  }

}
