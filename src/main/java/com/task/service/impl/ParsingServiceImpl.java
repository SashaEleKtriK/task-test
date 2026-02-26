package com.task.service.impl;

import com.task.exception.XmlParseException;
import com.task.service.ParsingService;
import groovy.xml.XmlSlurper;
import groovy.xml.slurpersupport.GPathResult;
import groovy.xml.slurpersupport.Node;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ParsingServiceImpl implements ParsingService {

  @Value("${xml.url}")
  private String xmlUrl;

  @Value("${xml.connection.request-method}")
  private String requestMethod;

  @Value("${xml.connection.connect-timeout}")
  private Integer connectTimeout;

  @Value("${xml.connection.read-timeout}")
  private Integer readTimeout;

  @Value("${xml.connection.user-agent}")
  private String userAgent;

  private final Map<String, Map<String, Map<String, String>>> xmlData = new LinkedHashMap<>();

  private boolean isParsed = false;

  @Override
  public void parseXml() throws Exception {
    log.info("--Запуск парсинга XML...");
    if (!xmlData.isEmpty()) {
      xmlData.clear();
    }
    String xmlString;
    try {
      xmlString = downloadXmlAsString();
    } catch (Exception e) {
      throw new XmlParseException("Ошибка парсинга", e);
    }
    XmlSlurper slurper = new XmlSlurper();
    GPathResult parsedXml = slurper.parseText(xmlString);
    Iterator<?> nodes = parsedXml.childNodes();
    findTables(nodes, "");
    xmlData.forEach(
        (tableName, rows) -> log.debug("Таблица '{}': {} строк", tableName, rows.size()));
    isParsed = true;
    log.info("--Парсинг окончен");
  }

  @Override
  public Map<String, Map<String, Map<String, String>>> getParsedXml() {
    parseIfNecessary();
    return xmlData;
  }

  @Override
  public List<String> getTableColumns(String tableName) {
    parseIfNecessary();
    Map<String, Map<String, String>> table = xmlData.get(tableName);
    if (table == null) {
      log.error("Не существует таблицы {}. Доступные таблицы: {}", tableName, xmlData.keySet());
      throw new RuntimeException("Не существующее название таблицы");
    }
    return table.values().stream().flatMap(x -> x.keySet().stream()).distinct()
        .toList();
  }

  private void parseIfNecessary() {
    if (!isParsed) {
      try {
        parseXml();
      } catch (Exception e) {
        log.error("Ошибка парсинга xml: ", e);
      }
    }
  }

  private void findTables(Iterator<?> nodes, String path) {
    while (nodes.hasNext()) {
      Object child = nodes.next();
      if (child instanceof Node element) {
        String nodeName = element.name();
        String currentPath = path.isEmpty() ? nodeName : path + "." + nodeName;
        if (isTable(element.childNodes())) {
          Map<String, Map<String, Map<String, String>>> tableData = extractTableData(element);
          if (!tableData.isEmpty()) {
            xmlData.putAll(tableData);
            log.debug("Найдена таблица: {} ({} строк)", currentPath, tableData.size());
          }
        }
        findTables(element.childNodes(), currentPath);
      }
    }
  }

  private String downloadXmlAsString() throws Exception {
    HttpURLConnection connection;
    URL url = new URL(xmlUrl);
    connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(requestMethod);
    connection.setConnectTimeout(connectTimeout);
    connection.setReadTimeout(readTimeout);
    connection.setRequestProperty("User-Agent", userAgent);

    StringBuilder content = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append("\n");
      }
    }
    String result = content.toString().replaceFirst("<!DOCTYPE[^>]*>", "");
    log.debug("Первые 500 символов XML:\n{}", result.substring(0, Math.min(500, result.length())));
    return result;
  }

  private boolean isTable(Iterator<?> children) {
    int childCount = 0;
    Set<String> rows = new HashSet<>();
    while (children.hasNext()) {
      Object child = children.next();
      if (child instanceof Node) {
        childCount++;
        rows.add(((Node) child).name());
      }
    }
    if (rows.size() != 1) {
      return false;
    }
    if (childCount <= 0) {
      return false;
    }
    return true;
  }

  private Map<String, Map<String, Map<String, String>>> extractTableData(Node node) {
    String tableName = node.name();
    Map<String, Map<String, Map<String, String>>> result = new HashMap<>();
    Iterator<?> rows = node.childNodes();
    Map<String, Map<String, String>> rawMap = new HashMap<>();
    while (rows.hasNext()) {
      Object rowNode = rows.next();
      if (rowNode instanceof Node) {
        Iterator<?> columns = ((Node) rowNode).childNodes();
        Map<String, String> columnValueMap = new HashMap<>();
        while (columns.hasNext()) {
          Object column = columns.next();
          if (column instanceof Node) {
            columnValueMap.put(((Node) column).name(), ((Node) column).text());
          }
        }
        if (!((Node) rowNode).text().isEmpty()) {
          columnValueMap.put("text", ((Node) rowNode).text());
        }
        try {
          Map<String, String> attrs = getAttributes((Node) rowNode);
          columnValueMap.putAll(attrs);
          String id = attrs.get("id");
          rawMap.put(id, columnValueMap);
        } catch (Exception e) {
          throw new XmlParseException("Ошибка парсинга Xml", e);
        }
      }
    }
    result.put(tableName, rawMap);
    return result;
  }

  private Map<String, String> getAttributes(Node element) {
    Map<String, String> attributes = new LinkedHashMap<>();
    Map attrMap = element.attributes();
    if (attrMap != null && !attrMap.isEmpty()) {
      for (Object entryObj : attrMap.entrySet()) {
        Map.Entry entry = (Map.Entry) entryObj;
        attributes.put((String) entry.getKey(), (String) entry.getValue());
      }
    } else {
      log.debug("Элемент {} не является NodeChild, это: {}", element.name(),
          element.getClass().getSimpleName());
    }

    return attributes;
  }
}

