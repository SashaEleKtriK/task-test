package com.task.service.impl;

import com.task.service.DatabaseService;
import com.task.service.MainService;
import com.task.service.ParsingService;
import com.task.service.SqlBuilderService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainServiceImpl implements MainService {

  private final ParsingService parsingService;
  private final SqlBuilderService sqlBuilder;
  private final DatabaseService databaseService;

  @Override
  public ArrayList<String> getTableNames() {
    Map<String, Map<String, Map<String, String>>> map = parsingService.getParsedXml();
    return new ArrayList<>(map.keySet());
  }

  @Override
  public String getTableDDL(String tableName) {
    String sqlDdl = sqlBuilder.getCreateTableSql(tableName);
    databaseService.createTableBySql(sqlDdl, tableName);
    return sqlDdl;
  }

  @Override
  public void update() throws Exception {
    parsingService.parseXml();
    List<String> dbTables = databaseService.getAllTableNames();
    for (String tableName : dbTables) {
      requestForTableUpdate(tableName);
    }
  }

  @Override
  public void update(String tableName) throws Exception {
    parsingService.parseXml();
    requestForTableUpdate(tableName);
  }

  @Override
  public ArrayList<String> getColumnNames(String tableName) {
    List<String> dbTables = databaseService.getAllTableNames();
    if (dbTables.contains(tableName)) {
      return (ArrayList<String>) databaseService.getColumnNames(tableName);
    } else {
      log.error(
          "В базе данных не существует таблицы {}. Доступные таблицы {}", tableName, dbTables);
      throw new RuntimeException("Несуществующее название таблицы");
    }
  }

  @Override
  public boolean isColumnId(String tableName, String columnName) {
    return databaseService.checkColumnUnique(
        sqlBuilder.getUniqueColumnCheck(tableName, columnName));
  }

  @Override
  public String getDDLChange(String tableName) {
    List<String> actualColumns = parsingService.getTableColumns(tableName);
    List<String> databaseColumns = databaseService.getColumnNames(tableName);
    List<String> newColumns = new ArrayList<>();
    for (String actualColumn : actualColumns) {
      if (!databaseColumns.contains(actualColumn.toLowerCase())) {
        newColumns.add(actualColumn);
      }
    }
    if (newColumns.isEmpty()) {
      return null;
    } else {
      log.info("Колонок для добавления {} шт. : {}", newColumns.size(), newColumns);
      String result = sqlBuilder.updateTableStructure(tableName, newColumns);
      databaseService.updateTableBySql(result, tableName);
      return result;
    }
  }

  private void requestForTableUpdate(String tableName) {
    if (parsingService.getParsedXml().containsKey(tableName)) {
      if (databaseService.getAllTableNames().contains(tableName)) {
        if (new HashSet<>(databaseService.getColumnNames(tableName)).containsAll(
            parsingService.getTableColumns(tableName).stream().map(String::toLowerCase).toList())) {
          List<String> sqlDml = sqlBuilder.getFillTableSql(tableName);
          databaseService.appendTableBySql(sqlDml, tableName);
        } else {
          throw new RuntimeException("Структура данных изменилась в таблице " + tableName);
        }
      } else {
        log.error(
            "В базе данных не существует таблицы {}, которая существует в xml. Для создания таблицы воспользуйтесь командой: create {}",
            tableName, tableName);
      }
    } else {
      log.error("xml не содержит таблицы с таким названием {}. Доступные таблицы: {}", tableName,
          parsingService.getParsedXml().keySet());
    }
  }
}
