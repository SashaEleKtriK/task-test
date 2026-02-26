package com.task.service.impl;

import com.task.service.DatabaseService;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseServiceImpl implements DatabaseService {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void createTableBySql(String sql, String tableName) {
    try {
      log.debug("Выполнение SQL:\n{}", sql);
      jdbcTemplate.execute(sql);
      log.info("Таблица успешно создана");
    } catch (Exception e) {
      log.error("Ошибка создания таблицы: {}", e.getMessage());
    }
  }

  @Override
  public void updateTableBySql(String sql, String tableName) {
    try {
      log.debug("Выполнение SQL:\n{}", sql);
      jdbcTemplate.execute(sql);
      log.info("Таблица успешно обновлена");
    } catch (Exception e) {
      log.error("Ошибка обновления таблицы: {}", e.getMessage());
    }
  }

  @Override
  public void appendTableBySql(List<String> sqlList, String tableName) {
    int problems = 0;
    int success = 0;
    for (String sql : sqlList) {
      try {
        log.debug("Выполнение SQL:\n{}", sql);
        jdbcTemplate.execute(sql);
        success++;
      } catch (Exception e) {
        log.debug("Ошибка заполнения таблицы: {}", e.getMessage());
        problems++;
      }
    }
    log.info(
        "Таблица {} успешно заполнена. Общее количество строк {}. Количество добавленных/обновленных строк {}. Количество ошибок: {}",
        tableName, sqlList.size(), success, problems);

  }

  @Override
  public List<String> getAllTableNames() {
    return jdbcTemplate.execute((ConnectionCallback<List<String>>) (connection) -> {
      DatabaseMetaData metaData = connection.getMetaData();
      List<String> tables = new ArrayList<>();

      try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
        while (rs.next()) {
          String tableName = rs.getString("TABLE_NAME");
          tables.add(tableName);
        }
      }
      return tables;
    });
  }

  @Override
  public List<String> getColumnNames(String tableName) {
    return jdbcTemplate.execute((ConnectionCallback<List<String>>) (connection) -> {
      DatabaseMetaData metaData = connection.getMetaData();
      List<String> columns = new ArrayList<>();

      try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
        while (rs.next()) {
          String columnName = rs.getString("COLUMN_NAME");
          columns.add(columnName);
        }
      }
      return columns;
    });
  }

  @Override
  public boolean checkColumnUnique(String sql) {
    try {
      List<Map<String, Object>> duplicates = jdbcTemplate.queryForList(sql);
      return duplicates.isEmpty();
    } catch (Exception e) {
      throw new RuntimeException(
          "Ошибка при проверке уникальности. Убедитесь что таблица и столбец существуют.", e);
    }
  }
}
