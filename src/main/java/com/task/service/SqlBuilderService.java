package com.task.service;

import java.util.List;


public interface SqlBuilderService {

  String getCreateTableSql(String tableName);

  List<String> getFillTableSql(String tableName);

  String getUniqueColumnCheck(String tableName, String columnName);

  String updateTableStructure(String tableName, List<String> newColumnsList);

}
