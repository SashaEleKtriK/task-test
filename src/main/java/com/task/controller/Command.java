package com.task.controller;

import lombok.Getter;

@Getter
public enum Command {
  HELP("help", "- Показать справку"),
  TABLES("tables", "Показать список таблиц"),
  CREATE("create", "<table name> - Создать таблицу в базе данных"),
  UPDATE_ALL("update-all", "- Обновить/заполнить информацию в созданных таблицах"),
  UPDATE("update", "<table name> - Обновить/заполнить информацию в таблице"),
  COLUMNS("columns", "<table name> - Получить названия колонок таблицы в БД"),
  UNIQUE("unique", "<table name> <column name> - Проверка уникальности значений в колонке таблицы"),
  RESTRUCTURE("restructure", "<table name> - Добавить новые столбцы в структуру таблицы"),
  PARSE("parse", "- Распарсить XML файл"),
  EXIT("exit", "- Выход из программы"),
  UNKNOWN("", "Неизвестная команда");

  private final String command;
  private final String description;

  Command(String command, String description) {
    this.command = command;
    this.description = description;
  }

  public static Command fromString(String input) {
    if (input == null || input.isEmpty()) {
      return UNKNOWN;
    }

    String lowerInput = input.toLowerCase().trim();

    for (Command cmd : values()) {
      if (cmd.command.equals(lowerInput)) {
        return cmd;
      }
    }
    return UNKNOWN;
  }
}
