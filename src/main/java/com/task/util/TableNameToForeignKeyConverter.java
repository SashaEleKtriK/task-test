package com.task.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableNameToForeignKeyConverter {

  private static final Map<String, String> EXCEPTIONS;

  private static final List<Rule> RULES;

  private static final Logger log = LoggerFactory.getLogger(TableNameToForeignKeyConverter.class);

  static {
    RULES = new ArrayList<>();
    RULES.add(new Rule(Pattern.compile("(?i)ies$"), "y"));
    RULES.add(new Rule(Pattern.compile("(?i)ses$"), "s"));
    RULES.add(new Rule(Pattern.compile("(?i)ves$"), "f"));
    RULES.add(new Rule(Pattern.compile("(?i)oes$"), "o"));
    RULES.add(new Rule(Pattern.compile("(?i)ches$"), "ch"));
    RULES.add(new Rule(Pattern.compile("(?i)shes$"), "sh"));
    RULES.add(new Rule(Pattern.compile("(?i)xes$"), "x"));
    RULES.add(new Rule(Pattern.compile("(?i)zes$"), "z"));
    RULES.add(new Rule(Pattern.compile("(?i)s$"), ""));

    EXCEPTIONS = Map.ofEntries(
        Map.entry("children", "child"),
        Map.entry("people", "person"),
        Map.entry("men", "man"),
        Map.entry("women", "woman"),
        Map.entry("feet", "foot"),
        Map.entry("teeth", "tooth"),
        Map.entry("geese", "goose"),
        Map.entry("mice", "mouse"),
        Map.entry("data", "datum"),
        Map.entry("criteria", "criterion"),
        Map.entry("phenomena", "phenomenon"),
        Map.entry("oxen", "ox"),
        Map.entry("cacti", "cactus"),
        Map.entry("foci", "focus"),
        Map.entry("nuclei", "nucleus"),
        Map.entry("syllabi", "syllabus"),
        Map.entry("analyses", "analysis"),
        Map.entry("theses", "thesis"),
        Map.entry("crises", "crisis"),
        Map.entry("appendices", "appendix"),
        Map.entry("indices", "index")
    );
  }

  public static String getColumnName(String tableName) {
    String lowerTableName = tableName.toLowerCase();
    String exception = EXCEPTIONS.get(lowerTableName);
    if (exception != null) {
      return exception + "Id";
    }
    for (Rule rule : RULES) {
      if (rule.pattern.matcher(tableName).find()) {
        String result = tableName.replaceAll(rule.pattern.pattern(), rule.replacement);
        log.debug("Применено правило: {} -> {}", tableName, result);
        return result + "Id";

      }
    }
    return null;
  }

  private static class Rule {

    final Pattern pattern;
    final String replacement;

    Rule(Pattern pattern, String replacement) {
      this.pattern = pattern;
      this.replacement = replacement;
    }
  }

}
