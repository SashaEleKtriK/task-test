package com.task.service;

import java.util.List;
import java.util.Map;

public interface ParsingService {

  void parseXml() throws Exception;

  Map<String, Map<String, Map<String, String>>> getParsedXml();

  List<String> getTableColumns(String tableName);

}
