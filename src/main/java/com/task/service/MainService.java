package com.task.service;

import java.util.ArrayList;


public interface MainService {

  /*
  Возвращает названия таблиц из XML(currency, categories, offers)
  @return ArrayList
*/
  ArrayList<String> getTableNames() throws Exception;

/*
  Создает sql для создания таблиц динамически из XML
  @param
  String tableName
  @return
  String Sql DDL request
*/
  String getTableDDL(String tableName);

/*
  обновляет данные в таблицах бд на основе Id, если поменялась структура выдает exception
*/
void update() throws Exception;

/*
  обновляет данные в таблицах бд, если поменялась структура выдает exception
  @param
  String tableName
*/
  void update(String tableName) throws Exception;

  //наименование столбцов таблицы (динамически)
  ArrayList<String> getColumnNames(String tableName);

  //true если столбец не имеет повторяющихся значений
  boolean isColumnId(String tableName, String columnName);

  //изменения таблицы, допустимо только добавление новых столбцов
  String getDDLChange(String tableName);
}
