package com.groupon.maven.plugin.json.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Consumer;

public class JsonTreeWalker {

  public void walkTree(JsonNode root, Consumer<ObjectNode> consumer) {
    walker(null, root, consumer);
  }

  private void walker(String nodename, JsonNode node, Consumer<ObjectNode> consumer) {
    String nameToPrint = nodename != null ? nodename : "must_be_root";
    if (node.isObject()) {
      consumer.accept((ObjectNode) node);
      node.fields().forEachRemaining(e -> walker(e.getKey(), e.getValue(), consumer));
    } else if (node.isArray()) {
      node.elements().forEachRemaining(n -> walker("array item of '" + nameToPrint + "'", n, consumer));
    }
  }
}