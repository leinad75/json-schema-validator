package io.github.leinad75.maven.plugin.json.util;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.BiConsumer;

public class JsonTreeWalker {

  public void walkTree(JsonNode root, BiConsumer<String, ObjectNode> consumer) {
    walker(null, root, consumer);
  }

  private void walker(String nodename, JsonNode node, BiConsumer<String, ObjectNode> consumer) {
    String nameToPrint = nodename != null ? nodename : "ROOT";
    if (node.isObject()) {
      consumer.accept(nameToPrint, (ObjectNode) node);
      node.fields().forEachRemaining(e -> walker((nodename != null ? nodename : "") + "/" + e.getKey(), e.getValue(), consumer));
    } else if (node.isArray()) {
      node.elements().forEachRemaining(n -> walker("array item of '" + nameToPrint + "'", n, consumer));
    }
  }
}