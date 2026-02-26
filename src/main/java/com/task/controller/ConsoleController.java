package com.task.controller;

import com.task.service.MainService;
import com.task.service.ParsingService;
import java.util.List;
import java.util.Scanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleController implements CommandLineRunner {

  private final ParsingService parserService;

  private final MainService mainService;

  @Override
  public void run(String... args) throws Exception {
    startInteractiveMode();
  }

  private void startInteractiveMode() throws Exception {
    printWelcomeBanner();
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.print("\n❯ ");
      String input = scanner.nextLine().trim();

      if (input.isEmpty()) {
        continue;
      }

      if (executeCommand(input)) {
        break;
      }
    }

    log.info("До свидания!");
  }

  private boolean executeCommand(String input) {
    String[] parts = input.split("\\s+", 3);
    String commandStr = parts[0].toLowerCase();
    String args = parts.length > 1 ? parts[1] : "";
    String args2 = parts.length > 2 ? parts[2] : "";
    Command command = Command.fromString(commandStr);

    switch (command) {
      case HELP -> printHelp();
      case EXIT -> {
        return true;
      }
      case UPDATE_ALL -> update();
      case UPDATE -> update(args);
      case PARSE -> parseXml();
      case TABLES -> showTables();
      case CREATE -> createTable(args);
      case COLUMNS -> getColumnsName(args);
      case RESTRUCTURE -> updateStructure(args);
      case UNIQUE -> checkUnique(args, args2);
      default -> log.warn("Неизвестная команда: '{}'. Введите 'help' для справки.", commandStr);
    }

    return false;
  }

  private void updateStructure(String tableName) {
    String sql = mainService.getDDLChange(tableName);
    if (sql == null) {
      log.info("Обновление структуры не требуется");
    } else {
      log.info("Сформированный запрос {}", sql);
    }
  }

  private void checkUnique(String tableName, String columnName) {
    try {
      boolean result = mainService.isColumnId(tableName, columnName);
      log.info("Результат проверки стобца {} таблицы {} на уникальность значений - {}", columnName,
          tableName, result);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private void getColumnsName(String tableName) {
    try {
      List<String> result = mainService.getColumnNames(tableName);
      log.info(result.toString());
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private void update() {
    try {
      mainService.update();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private void update(String tableName) {
    try {
      mainService.update(tableName);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private void parseXml() {
    try {
      parserService.parseXml();
    } catch (Exception e) {
      log.error("Ошибка парсинга xml: ", e);
    }

  }

  private void showTables() {
    try {
      List<String> result = mainService.getTableNames();
      StringBuilder msg = new StringBuilder();
      msg.append("Названия таблиц: ");
      for (String name : result) {
        msg.append(name).append("; ");
      }
      log.info(msg.toString());
    } catch (Exception e) {
      log.error(e.getMessage());
    }

  }

  private void createTable(String tableName) {
    String sql;
    try {
      sql = mainService.getTableDDL(tableName);
      log.info("Запрос на создание таблицы: {}", sql);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private void printHelp() {
    log.info("Доступные команды:");
    log.info("{} {}", Command.HELP.getCommand(), Command.HELP.getDescription());
    log.info("{} {}", Command.TABLES.getCommand(), Command.TABLES.getDescription());
    log.info("{} {}", Command.CREATE.getCommand(), Command.CREATE.getDescription());
    log.info("{} {}", Command.UPDATE_ALL.getCommand(), Command.UPDATE_ALL.getDescription());
    log.info("{} {}", Command.UPDATE.getCommand(), Command.UPDATE.getDescription());
    log.info("{} {}", Command.COLUMNS.getCommand(), Command.COLUMNS.getDescription());
    log.info("{} {}", Command.UNIQUE.getCommand(), Command.UNIQUE.getDescription());
    log.info("{} {}", Command.RESTRUCTURE.getCommand(), Command.RESTRUCTURE.getDescription());
    log.info("{} {}", Command.PARSE.getCommand(), Command.PARSE.getDescription());
    log.info("{} {}", Command.EXIT.getCommand(), Command.EXIT.getDescription());
  }

  private void printWelcomeBanner() {
    log.info("╔══════════════════════════════════════╗");
    log.info("║    XML Processor Console v0.1        ║");
    log.info("║    Введите 'help' для справки        ║");
    log.info("╚══════════════════════════════════════╝");
  }

}
