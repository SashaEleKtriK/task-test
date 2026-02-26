package com.task.service.impl;

import com.task.service.ParsingService;
import com.task.service.SqlBuilderService;
import com.task.util.TableNameToForeignKeyConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SqlBuilderServiceImpl implements SqlBuilderService {

  private final ParsingService parsingService;

  @Override
  public String getCreateTableSql(String tableName) {
    List<String> columns = parsingService.getTableColumns(tableName);
    Map<String, String> foreignKeys = new HashMap<>();
    Map<String, String> potentialForeignColumns = parsingService.getParsedXml().keySet().stream()
        .collect(Collectors.toMap(TableNameToForeignKeyConverter::getColumnName, x -> x));
    for (String column : columns) {
      if (potentialForeignColumns.containsKey(column)) {
        foreignKeys.put(column, potentialForeignColumns.get(column));
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
    boolean isFirst = true;
    for (String column : columns) {
      if (isFirst) {
        isFirst = false;
      } else {
        sb.append(",\n");
      }
      sb.append("    ").append(column).append(" ").append(determineSqlType(column));
      if (column.equals("id")) {
        sb.append(" PRIMARY KEY");
      } else if (column.equals("vendorCode")) {
        sb.append(" UNIQUE NOT NULL");
      }
    }
    sb.append(");\n");

    if (!foreignKeys.isEmpty()) {
      for (Object entryObj : foreignKeys.entrySet()) {
        Map.Entry entry = (Map.Entry) entryObj;
        sb.append("ALTER TABLE ").append(tableName).append(" ADD CONSTRAINT ").append("fk_")
            .append(tableName).append("_").append(entry.getKey()).append(" FOREIGN KEY (")
            .append(entry.getKey()).append(") REFERENCES ").append(entry.getValue())
            .append("(id);\n");
      }
    }

    return sb.toString();
  }

  @Override
  public List<String> getFillTableSql(String tableName) {
    List<String> allRawsInsertList = new ArrayList<>();
    Map<String, Map<String, String>> table = parsingService.getParsedXml().get(tableName);
    for (Map<String, String> raws : table.values()) {
      if (tableName.equals("offers") && !raws.containsKey("vendorCode")) {
        continue;
      }
      boolean isFirst = true;
      StringBuilder mainSb = new StringBuilder();
      StringBuilder columnsSb = new StringBuilder();
      StringBuilder valuesSb = new StringBuilder();
      List<String> columns = new ArrayList<>();
      mainSb.append("INSERT INTO ").append(tableName).append(" ");
      for (Object entryObj : raws.entrySet()) {
        if (isFirst) {
          isFirst = false;
          columnsSb.append("(");
          valuesSb.append("(");
        } else {
          columnsSb.append(", ");
          valuesSb.append(", ");
        }
        Map.Entry entry = (Map.Entry) entryObj;
        columnsSb.append(entry.getKey());
        String sqlType = determineSqlType((String) entry.getKey());
        if (sqlType.contains("VARCHAR") || sqlType.contains("TEXT")) {
          valuesSb.append("'").append(entry.getValue()).append("'");
        } else {
          valuesSb.append(entry.getValue());
        }
        if (!entry.getKey().equals("id")) {
          columns.add((String) entry.getKey());
        }
      }
      columnsSb.append(")");
      valuesSb.append(")");
      mainSb.append(columnsSb).append(" VALUES ").append(valuesSb)
          .append("\n ON CONFLICT (id) DO UPDATE SET \n");
      isFirst = true;
      for (String column : columns) {
        if (isFirst) {
          isFirst = false;
        } else {
          mainSb.append(", \n");
        }
        mainSb.append(column).append(" = EXCLUDED.").append(column);
      }
      mainSb.append(";");
      allRawsInsertList.add(mainSb.toString());
    }
    return allRawsInsertList;
  }

  @Override
  public String getUniqueColumnCheck(String tableName, String columnName) {
    return "SELECT " + columnName + ",\n    COUNT(*) as duplicate_count\nFROM " + tableName
        + "\nGROUP BY " + columnName + "\nHAVING COUNT(*) > 1;";
  }

  @Override
  public String updateTableStructure(String tableName, List<String> newColumnsList) {
    StringBuilder sqlSb = new StringBuilder().append("ALTER TABLE ").append(tableName);
    boolean isFirst = true;
    for (String newColumn : newColumnsList) {
      if (isFirst) {
        isFirst = false;
      } else {

        sqlSb.append(",");
      }
      sqlSb.append("\nADD ").append(newColumn).append(" ").append(determineSqlType(newColumn));
    }
    sqlSb.append(";");
    return sqlSb.toString();
  }

  private String determineSqlType(String columnName) {
    String lower = columnName.toLowerCase();

    if (lower.contains("id") || lower.contains("code")) {
      return "VARCHAR(50)";
    }

    if (lower.contains("price") || lower.contains("sum") || lower.contains("amount")
        || lower.contains("rate")) {
      return "NUMERIC(15,2)";
    }

    if (lower.contains("count") || lower.contains("stock") || lower.contains("quantity")) {
      return "INTEGER";
    }

    if (lower.contains("available") || lower.contains("active") || lower.contains("enabled")) {
      return "BOOLEAN";
    }

    if (lower.contains("description") || lower.contains("text")) {
      return "TEXT";
    }

    return "VARCHAR(255)";
  }

}
