package com.task.service;

import java.util.List;

public interface DatabaseService {

  void createTableBySql(String sql, String tableName);

  void updateTableBySql(String sql, String tableName);

  void appendTableBySql(List<String> sqlList, String tableName);

  List<String> getAllTableNames();

  List<String> getColumnNames(String tableName);

  boolean checkColumnUnique(String sql);

}
