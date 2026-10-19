package com.example.financemgmt.service;

import com.example.financemgmt.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DatabaseBackupService {
    private static final String HEADER = "-- Ledgerly Finance Management database backup v1";
    private static final String DATA_PREFIX = "-- finance-data-base64:";
    private static final int MAX_BACKUP_BYTES = 50 * 1024 * 1024;
    private static final List<TableSpec> TABLES = List.of(
            table("customers",
                    col("id", ValueType.LONG), col("name", ValueType.STRING),
                    col("normalized_name", ValueType.STRING), col("mobile_number", ValueType.STRING),
                    col("address", ValueType.STRING), col("created_at", ValueType.TIMESTAMP),
                    col("status", ValueType.STRING)),
            table("cash_flow",
                    col("id", ValueType.LONG), col("investment_amount", ValueType.DECIMAL),
                    col("financial_balance", ValueType.DECIMAL), col("profit_amount", ValueType.DECIMAL),
                    col("collection_amount", ValueType.DECIMAL)),
            table("loans",
                    col("id", ValueType.STRING), col("customer_id", ValueType.LONG),
                    col("loan_type", ValueType.STRING), col("principal_amount", ValueType.DECIMAL),
                    col("interest_rate", ValueType.DECIMAL), col("repayment_frequency", ValueType.STRING),
                    col("weekly_day", ValueType.STRING), col("start_date", ValueType.DATE),
                    col("installment_amount", ValueType.DECIMAL), col("installment_count", ValueType.INTEGER),
                    col("next_installment_date", ValueType.DATE)),
            table("financial_transactions",
                    col("id", ValueType.LONG), col("transaction_type", ValueType.STRING),
                    col("amount", ValueType.DECIMAL), col("transaction_date", ValueType.DATE),
                    col("loan_id", ValueType.STRING), col("created_at", ValueType.TIMESTAMP)),
            table("cash_transfers",
                    col("id", ValueType.LONG), col("source", ValueType.STRING),
                    col("amount", ValueType.DECIMAL), col("transfer_date", ValueType.DATE),
                    col("created_at", ValueType.TIMESTAMP))
    );
    private static final List<String> DELETE_ORDER = List.of(
            "cash_transfers", "financial_transactions", "loans", "customers", "cash_flow");
    private static final List<String> IDENTITY_TABLES = List.of(
            "customers", "financial_transactions", "cash_transfers");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z_]+");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DatabaseBackupService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] exportSql() {
        try {
            Map<String, List<List<Object>>> snapshot = new LinkedHashMap<>();
            for (TableSpec table : TABLES) {
                String columns = table.columns().stream().map(ColumnSpec::name)
                        .collect(java.util.stream.Collectors.joining(", "));
                List<Map<String, Object>> records = jdbcTemplate.queryForList(
                        "SELECT " + columns + " FROM finance_mgmt." + table.name()
                                + " ORDER BY " + table.columns().getFirst().name());
                List<List<Object>> rows = records.stream()
                        .map(record -> table.columns().stream()
                                .map(column -> jsonValue(record.get(column.name())))
                                .toList())
                        .toList();
                snapshot.put(table.name(), rows);
            }

            String payload = objectMapper.writeValueAsString(Map.of("version", 1, "tables", snapshot));
            StringBuilder sql = new StringBuilder(HEADER).append('\n')
                    .append(DATA_PREFIX)
                    .append(Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)))
                    .append("\n\nBEGIN;\n");

            for (TableSpec table : TABLES) {
                List<List<Object>> rows = snapshot.get(table.name());
                if (rows.isEmpty()) continue;
                String columns = table.columns().stream().map(ColumnSpec::name)
                        .collect(java.util.stream.Collectors.joining(", "));
                for (List<Object> row : rows) {
                    sql.append("INSERT INTO finance_mgmt.").append(table.name())
                            .append(" (").append(columns).append(") VALUES (")
                            .append(renderValues(table, row)).append(");\n");
                }
            }

            for (String tableName : IDENTITY_TABLES) {
                long nextId = nextIdentityValue(tableName);
                sql.append("ALTER TABLE finance_mgmt.").append(tableName)
                        .append(" ALTER COLUMN id RESTART WITH ").append(nextId).append(";\n");
            }
            sql.append("COMMIT;\n");
            byte[] backup = sql.toString().getBytes(StandardCharsets.UTF_8);
            if (backup.length > MAX_BACKUP_BYTES) {
                throw new IllegalStateException("Database backup exceeds the 50 MB import limit");
            }
            return backup;
        } catch (IOException e) {
            throw new IllegalStateException("Could not create database backup", e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void importSql(byte[] backup) {
        if (backup.length == 0 || backup.length > MAX_BACKUP_BYTES) {
            throw new BadRequestException("The backup file is empty or exceeds the 50 MB limit");
        }
        Map<String, List<Object[]>> rowsByTable = parseBackup(backup);
        List<Object[]> cashFlowRows = rowsByTable.get("cash_flow");
        if (cashFlowRows.size() != 1 || !Long.valueOf(1L).equals(cashFlowRows.getFirst()[0])) {
            throw new BadRequestException("The backup must contain the finance cash-flow account with ID 1");
        }

        for (String tableName : DELETE_ORDER) {
            jdbcTemplate.update("DELETE FROM finance_mgmt." + tableName);
        }
        for (TableSpec table : TABLES) {
            List<Object[]> rows = rowsByTable.get(table.name());
            if (rows.isEmpty()) continue;
            String columns = table.columns().stream().map(ColumnSpec::name)
                    .collect(java.util.stream.Collectors.joining(", "));
            String placeholders = String.join(", ", java.util.Collections.nCopies(table.columns().size(), "?"));
            jdbcTemplate.batchUpdate("INSERT INTO finance_mgmt." + table.name()
                            + " (" + columns + ") VALUES (" + placeholders + ")",
                    rows);
        }
        for (String tableName : IDENTITY_TABLES) {
            jdbcTemplate.execute("ALTER TABLE finance_mgmt." + tableName
                    + " ALTER COLUMN id RESTART WITH " + nextIdentityValue(tableName));
        }
    }

    private Map<String, List<Object[]>> parseBackup(byte[] backup) {
        String sql = new String(backup, StandardCharsets.UTF_8);
        String[] lines = sql.split("\\R", 3);
        if (lines.length < 2 || !HEADER.equals(lines[0].trim()) || !lines[1].startsWith(DATA_PREFIX)) {
            throw new BadRequestException("This is not a Finance Management SQL backup created by this application");
        }
        try {
            byte[] json = Base64.getDecoder().decode(lines[1].substring(DATA_PREFIX.length()).trim());
            JsonNode root = objectMapper.readTree(json);
            if (root == null || root.path("version").asInt(-1) != 1 || !root.path("tables").isObject()) {
                throw new BadRequestException("The backup format is invalid or unsupported");
            }
            JsonNode tables = root.path("tables");
            if (tables.size() != TABLES.size()) {
                throw new BadRequestException("The backup does not contain all required application tables");
            }
            Map<String, List<Object[]>> result = new LinkedHashMap<>();
            for (TableSpec table : TABLES) {
                JsonNode rows = tables.get(table.name());
                if (rows == null || !rows.isArray()) {
                    throw new BadRequestException("The backup is missing table data for " + table.name());
                }
                List<Object[]> parsedRows = new ArrayList<>(rows.size());
                for (JsonNode row : rows) {
                    if (!row.isArray() || row.size() != table.columns().size()) {
                        throw new BadRequestException("The backup contains an invalid row for " + table.name());
                    }
                    Object[] values = new Object[table.columns().size()];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = parseValue(row.get(i), table.columns().get(i), table.name());
                    }
                    parsedRows.add(values);
                }
                result.put(table.name(), parsedRows);
            }
            return result;
        } catch (IllegalArgumentException | IOException e) {
            throw new BadRequestException("The SQL backup data is invalid or corrupted");
        }
    }

    private Object parseValue(JsonNode value, ColumnSpec column, String table) {
        if (value == null || value.isNull()) return null;
        try {
            return switch (column.type()) {
                case STRING -> {
                    if (!value.isTextual()) throw new IllegalArgumentException();
                    yield value.textValue();
                }
                case LONG -> value.isIntegralNumber() ? value.longValue() : Long.parseLong(value.textValue());
                case INTEGER -> value.isIntegralNumber() ? value.intValue() : Integer.parseInt(value.textValue());
                case DECIMAL -> new BigDecimal(value.isNumber() ? value.asText() : value.textValue());
                case DATE -> Date.valueOf(LocalDate.parse(value.textValue()));
                case TIMESTAMP -> Timestamp.valueOf(LocalDateTime.parse(value.textValue()));
            };
        } catch (RuntimeException e) {
            throw new BadRequestException("The backup contains an invalid " + column.name() + " value in " + table);
        }
    }

    private String renderValues(TableSpec table, List<Object> row) {
        List<String> values = new ArrayList<>(row.size());
        for (int i = 0; i < row.size(); i++) {
            Object value = row.get(i);
            if (value == null) {
                values.add("NULL");
            } else if (table.columns().get(i).type() == ValueType.LONG
                    || table.columns().get(i).type() == ValueType.INTEGER
                    || table.columns().get(i).type() == ValueType.DECIMAL) {
                values.add(value.toString());
            } else {
                values.add("'" + value.toString().replace("'", "''") + "'");
            }
        }
        return String.join(", ", values);
    }

    private long nextIdentityValue(String tableName) {
        if (!SAFE_IDENTIFIER.matcher(tableName).matches() || !IDENTITY_TABLES.contains(tableName)) {
            throw new IllegalArgumentException("Unsupported identity table");
        }
        Long maxId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM finance_mgmt." + tableName, Long.class);
        return maxId == null ? 1L : Math.addExact(maxId, 1L);
    }

    private Object jsonValue(Object value) {
        if (value instanceof BigDecimal decimal) return decimal.toPlainString();
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime().toString();
        if (value instanceof Date date) return date.toLocalDate().toString();
        return value;
    }

    private static TableSpec table(String name, ColumnSpec... columns) {
        return new TableSpec(name, List.of(columns));
    }

    private static ColumnSpec col(String name, ValueType type) {
        return new ColumnSpec(name, type);
    }

    private record TableSpec(String name, List<ColumnSpec> columns) {}
    private record ColumnSpec(String name, ValueType type) {}
    private enum ValueType { STRING, LONG, INTEGER, DECIMAL, DATE, TIMESTAMP }
}
