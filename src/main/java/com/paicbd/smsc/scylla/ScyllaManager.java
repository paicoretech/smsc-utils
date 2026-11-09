package com.paicbd.smsc.scylla;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessageRegister;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.MsisdnUtils;
import com.paicore.sba.stack.openapi.model.UeSmsContextData;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.RegEventState;
import com.paicbd.smsc.utils.RegistrationType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@Slf4j
public class ScyllaManager {
    private static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";
    private static final String INSERT_INTO = "INSERT INTO ";
    private static final String COUNTRY_CODE_COLUMN = "country_code";
    private static final String CREATE_QUERY = "CREATE TABLE IF NOT EXISTS %s (id TEXT PRIMARY KEY, data TEXT);";
    private static final String INSERT_QUERY = "INSERT INTO %s (id, data) VALUES (?, ?);";
    private static final String INSERT_QUERY_WITH_TTL = "INSERT INTO %s (id, data) VALUES (?, ?) USING TTL %s;";
    private static final String DELETE_QUERY = "DELETE FROM %s WHERE id = ?";
    private static final String DELETE_QUERY_IF_EXISTS = "DELETE FROM %s WHERE id = ? IF EXISTS";
    private static final String SELECT_QUERY_BY_ID = "SELECT data FROM %s WHERE id = ?";
    private static final String SELECT_ALL_QUERY = "SELECT data FROM %s";
    private static final String SELECT_ALL_WITH_LIMIT_QUERY = "SELECT data FROM %s LIMIT ?";

    private static final String CREATE_DND_ENTRIES_TABLE = "CREATE TABLE IF NOT EXISTS dnd_entries (parent_id int, key text, dnd_type text, msisdn text, PRIMARY KEY (dnd_type, msisdn));";
    private static final String INSERT_DND_ENTRY = "INSERT INTO dnd_entries (parent_id, key, dnd_type, msisdn) VALUES (?, ?, ?, ?);";
    private static final String SELECT_DND_BY_PARENT_ID = "SELECT dnd_type, msisdn FROM dnd_entries WHERE parent_id = ? ALLOW FILTERING;";
    private static final String DELETE_DND_BY_KEY = "DELETE FROM dnd_entries WHERE dnd_type = ? AND msisdn = ?;";
    private static final String SELECT_DND_BY_TYPE_AND_MSISDN = "SELECT * FROM dnd_entries WHERE dnd_type = ? AND msisdn = ?";
    private static final String SELECT_DND_EXISTS_BY_PARENT_ID = "SELECT parent_id FROM dnd_entries WHERE parent_id = ? LIMIT 1 ALLOW FILTERING;";

    private static final String CREATE_HOME_ROUTING_CC_MCC_MNC_TABLE = "CREATE TABLE IF NOT EXISTS home_routing_cc_mcc_mnc (ss7_network_id TEXT, country_code TEXT, mcc_mnc TEXT, smsc TEXT, PRIMARY KEY (ss7_network_id, country_code, mcc_mnc));";
    private static final String INSERT_HOME_ROUTING = "INSERT INTO home_routing_cc_mcc_mnc (ss7_network_id, country_code, mcc_mnc, smsc) VALUES (?, ?, ?, ?) IF NOT EXISTS;";
    private static final String SELECT_HOME_ROUTING_BY_NETWORK_AND_COUNTRY = "SELECT ss7_network_id, country_code, mcc_mnc, smsc FROM home_routing_cc_mcc_mnc WHERE ss7_network_id = ? AND country_code = ?;";
    private static final String DELETE_HOME_ROUTING_BY_NETWORK_COUNTRY_AND_MCC = "DELETE FROM home_routing_cc_mcc_mnc WHERE ss7_network_id = ? AND country_code = ? AND mcc_mnc = ?;";
    private static final String DELETE_HOME_ROUTING_BY_NETWORK = "DELETE FROM home_routing_cc_mcc_mnc WHERE ss7_network_id = ?;";

    private static final String CREATE_PLMN_PREFIX_TABLE = CREATE_TABLE_IF_NOT_EXISTS
            + ScyllaTablesConstants.PLMN_PREFIX_TABLE
            + " (prefix text PRIMARY KEY, mcc text, mnc text, country text, country_code text, country_iso text);";
    private static final String UPSERT_PLMN_PREFIX = INSERT_INTO
            + ScyllaTablesConstants.PLMN_PREFIX_TABLE
            + " (prefix, mcc, mnc, country, country_code, country_iso) VALUES (?, ?, ?, ?, ?, ?);";
    private static final String SELECT_PLMN_BY_PREFIX = "SELECT prefix, mcc, mnc, country, country_code, country_iso FROM "
            + ScyllaTablesConstants.PLMN_PREFIX_TABLE + " WHERE prefix = ?;";
    private static final String SELECT_ANY_PLMN_PREFIX = "SELECT prefix FROM "
            + ScyllaTablesConstants.PLMN_PREFIX_TABLE + " LIMIT 1;";
    private static final String DELETE_PLMN_PREFIX = "DELETE FROM "
            + ScyllaTablesConstants.PLMN_PREFIX_TABLE + " WHERE prefix = ?;";
    private static final String CREATE_PLMN_RANGE_TABLE = CREATE_TABLE_IF_NOT_EXISTS
            + ScyllaTablesConstants.PLMN_RANGE_TABLE
            + " (lookup_bucket text, sn_range_start bigint, sn_range_stop bigint, prefix text, mcc text, "
            + "mnc text, country text, country_code text, country_iso text, "
            + "PRIMARY KEY ((lookup_bucket), sn_range_start)) "
            + "WITH CLUSTERING ORDER BY (sn_range_start DESC);";
    private static final String UPSERT_PLMN_RANGE = INSERT_INTO
            + ScyllaTablesConstants.PLMN_RANGE_TABLE
            + " (lookup_bucket, sn_range_start, sn_range_stop, prefix, mcc, mnc, country, country_code, country_iso) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String SELECT_PLMN_RANGE = "SELECT lookup_bucket, sn_range_start, sn_range_stop, "
            + "prefix, mcc, mnc, country, country_code, country_iso FROM "
            + ScyllaTablesConstants.PLMN_RANGE_TABLE
            + " WHERE lookup_bucket = ? AND sn_range_start <= ? LIMIT 1;";
    private static final String SELECT_ANY_PLMN_RANGE = "SELECT lookup_bucket FROM "
            + ScyllaTablesConstants.PLMN_RANGE_TABLE + " LIMIT 1;";
    private static final String DELETE_PLMN_RANGE = "DELETE FROM "
            + ScyllaTablesConstants.PLMN_RANGE_TABLE
            + " WHERE lookup_bucket = ? AND sn_range_start = ?;";
    private static final int MAX_PLMN_PREFIX_LENGTH = 15;

    private static final String SELECT_BEARER_TOKEN_BLACKLIST = "SELECT jti FROM " + ScyllaTablesConstants.BEARER_TOKEN_BLACKLIST_TABLE + " WHERE jti = ?";
    private static final String CREATE_BEARER_TOKEN_BLACKLIST_TABLE = CREATE_TABLE_IF_NOT_EXISTS + ScyllaTablesConstants.BEARER_TOKEN_BLACKLIST_TABLE + " (jti text PRIMARY KEY);";
    private static final String INSERT_BEARER_TOKEN_BLACKLIST = INSERT_INTO + ScyllaTablesConstants.BEARER_TOKEN_BLACKLIST_TABLE + " (jti) VALUES (?) USING TTL ?";

    private static final String INSERT_SMPP_MESSAGE_PARTS = "INSERT INTO %s (id, timestamp, data) VALUES (?, currentTimestamp(), ?) USING TTL %s;";
    private static final String DELETE_SMPP_MESSAGE_PARTS_BY_ID = "DELETE FROM %s WHERE id = ?;";

    private static final String CREATE_SIP_REGISTRATION_STATE_TABLE = "CREATE TABLE IF NOT EXISTS %s (msisdn text PRIMARY KEY, aor text, imsi text, registration_type text, reg_event_state text, gsm_scf_address text, support_sbi boolean, expires int, updated_at timestamp);";
    private static final String CREATE_SIP_REG_AOR_INDEX = "CREATE INDEX IF NOT EXISTS sip_reg_aor_idx ON %s (aor);";
    private static final String SELECT_SIP_REG_BY_MSISDN = "SELECT msisdn FROM %s WHERE msisdn = ?;";
    private static final String SELECT_SIP_REG_EVENT_BY_MSISDN = "SELECT msisdn, aor, expires, gsm_scf_address, imsi, reg_event_state, registration_type, support_sbi FROM %s WHERE msisdn = ?;";
    private static final String SELECT_SIP_REG_BY_AOR = "SELECT msisdn, aor FROM %s WHERE aor = ?;";
    private static final String SELECT_SIP_REG_EVENT_BY_AOR = "SELECT msisdn, aor, expires, gsm_scf_address, imsi, reg_event_state, registration_type, support_sbi FROM %s WHERE aor = ?;";
    private static final String UPSERT_SIP_REG = "INSERT INTO %s (msisdn, aor, imsi, registration_type, reg_event_state, gsm_scf_address, support_sbi, expires, updated_at)VALUES (?, ?, ?, ?, ?, ?, ?, ?, toTimestamp(now()));";
    private static final String UPDATE_SIP_REG_EVENT_STATE = "UPDATE %s SET reg_event_state = ?, updated_at = toTimestamp(now()) WHERE msisdn = ?;";
    private static final String UPDATE_SIP_REG_EVENT_STATE_AND_AOR = "UPDATE %s SET reg_event_state = ?, aor = ?, updated_at = toTimestamp(now()) WHERE msisdn = ?;";
    private static final String UPSERT_SIP_REG_WITH_TTL = "INSERT INTO %s (msisdn, aor, imsi, registration_type, reg_event_state, gsm_scf_address, support_sbi, expires, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, toTimestamp(now())) USING TTL ?;";
    private static final String UPDATE_SIP_REG_EVENT_STATE_WITH_TTL = "UPDATE %s USING TTL ? SET reg_event_state = ?, updated_at = toTimestamp(now()) WHERE msisdn = ?;";
    private static final String DELETE_SIP_REG_BY_MSISDN = "DELETE FROM %s WHERE msisdn = ?;";
    //SMSF
    private static final String INSERT_SMSF_MESSAGE_PARTS = "INSERT INTO %s (id, timestamp, data) VALUES (?, currentTimestamp(), ?) USING TTL %s;";
    private static final String SELECT_SMSF_MESSAGE_PARTS_BY_ID = "SELECT data FROM %s WHERE id = ?;";
    private static final String DELETE_SMSF_MESSAGE_PARTS_BY_ID = "DELETE FROM %s WHERE id = ?;";
    private static final String CREATE_SMSF_UE_SMS_CONTEXT_TABLE = "CREATE TABLE IF NOT EXISTS %s (supi text PRIMARY KEY, gpsi text, data text, reg_event_state text, updated_at timestamp);";
    private static final String UPDATE_SMSF_REG_EVENT_STATE = "UPDATE %s SET reg_event_state = ? WHERE supi = ?;";
    private static final String SELECT_SMSF_UE_SMS_CONTEXT_WITH_STATE_BY_GPSI = "SELECT data, reg_event_state FROM %s WHERE gpsi = ?;";
    private static final String CREATE_SMSF_UE_SMS_CONTEXT_GPSI_INDEX = "CREATE INDEX IF NOT EXISTS smsf_ue_sms_context_gpsi_idx ON %s (gpsi);";
    private static final String UPSERT_SMSF_UE_SMS_CONTEXT = "INSERT INTO %s (supi, gpsi, data, reg_event_state, updated_at) VALUES (?, ?, ?, ?, toTimestamp(now()));";
    private static final String SELECT_SMSF_UE_SMS_CONTEXT_BY_SUPI = "SELECT data FROM %s WHERE supi = ?;";
    private static final String SELECT_SMSF_UE_SMS_CONTEXT_SUPI_BY_GPSI = "SELECT supi FROM %s WHERE gpsi = ?;";
    private static final String SELECT_SMSF_UE_SMS_CONTEXT_GPSI_BY_SUPI = "SELECT gpsi FROM %s WHERE supi = ?;";
    private static final String SELECT_SMSF_UE_SMS_CONTEXT_KEY_BY_SUPI = "SELECT supi FROM %s WHERE supi = ?;";
    private static final String SELECT_SMSF_UE_SMS_CONTEXT_KEY_BY_GPSI = "SELECT gpsi FROM %s WHERE gpsi = ?;";
    private static final String DELETE_SMSF_UE_SMS_CONTEXT_BY_SUPI = "DELETE FROM %s WHERE supi = ?;";
    private static final String KEY_SPACE = "smsc";
    private static final String MSISDN = "msisdn";
    private static final String REGISTRATION_TYPE = "registration_type";
    private static final String GSM_SCF_ADDRESS = "gsm_scf_address";
    private static final String IMSI = "imsi";
    private static final String SUPPORT_SBI = "support_sbi";
    private static final String AOR = "aor";
    private static final String REG_EVENT_STATE = "reg_event_state";

    private final String nodes;
    private final String datacenter;
    private final String username;
    private final String password;
    private final CqlSession session;


    public ScyllaManager(String nodes, String datacenter, String username, String password, boolean initDatabase) {
        this.nodes = nodes;
        this.datacenter = datacenter;
        this.username = username;
        this.password = password;
        List<InetSocketAddress> addresses = new ArrayList<>();
        String[] splitNodes = nodes.split(",");
        for (String node : splitNodes) {
            String[] hostPortNode = node.split(":");
            if (hostPortNode.length != 2) {
                throw new IllegalArgumentException("Invalid node format: " + node);
            }
            addresses.add(new InetSocketAddress(hostPortNode[0], Integer.parseInt(hostPortNode[1])));
            log.info("Connecting to Scylla node: {}:{}", hostPortNode[0], hostPortNode[1]);
        }
        DriverConfigLoader loader = DriverConfigLoader.programmaticBuilder()
                .withString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER, datacenter)
                .withString(DefaultDriverOption.REQUEST_TIMEOUT, "5 seconds")
                .withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, 32)
                .withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, 10)
                .withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS, 8192)
                .build();

        CqlSessionBuilder sessionBuilder = CqlSession.builder()
                .addContactPoints(addresses)
                .withConfigLoader(loader)
                .withKeyspace(KEY_SPACE);
        if (username.isEmpty() && password.isEmpty()) {
            log.info("Connecting to Scylla without authentication");

        } else {
            log.info("Connecting to Scylla with authentication");
            sessionBuilder.withAuthCredentials(username, password);
        }
        this.session = sessionBuilder.build();
        if (initDatabase) {
            log.info("Initializing Scylla database, but at this moment it is not supported");
        }
    }

    public boolean keyspaceExists() {
        SimpleStatement stmt = SimpleStatement.builder("SELECT keyspace_name FROM system_schema.keyspaces WHERE keyspace_name = ?")
                .addPositionalValue(KEY_SPACE)
                .build();
        Row row = session.execute(stmt).one();
        return row != null;
    }

    public void createTable(String table) {
        String query = String.format(CREATE_QUERY, table);
        SimpleStatement tableCreator = SimpleStatement.builder(query).build();
        session.execute(tableCreator);
    }

    public void createDndEntriesTable() {
        session.execute(CREATE_DND_ENTRIES_TABLE);
    }

    public void createHomeRoutingTable() {
        session.execute(CREATE_HOME_ROUTING_CC_MCC_MNC_TABLE);
    }

    public void createPlmnPrefixTable() {
        session.execute(CREATE_PLMN_PREFIX_TABLE);
    }

    public void createPlmnRangeTable() {
        session.execute(CREATE_PLMN_RANGE_TABLE);
    }

    public boolean isPlmnPrefixTableEmpty() {
        return Objects.isNull(session.execute(SELECT_ANY_PLMN_PREFIX).one());
    }

    public boolean isPlmnRangeTableEmpty() {
        return Objects.isNull(session.execute(SELECT_ANY_PLMN_RANGE).one());
    }

    public void insertIntoTable(String table, String id, String data) {
        String query = String.format(INSERT_QUERY, table);
        SimpleStatement insertToTable = SimpleStatement.builder(query).addPositionalValues(id, data).build();
        session.execute(insertToTable);
    }

    public void insertIntoTable(String table, String id, String data, int ttlSeconds) {
        if (ttlSeconds <= 0) {
            this.insertIntoTable(table, id, data);
            return;
        }
        String query = String.format(INSERT_QUERY_WITH_TTL, table, ttlSeconds);
        SimpleStatement insertToTable = SimpleStatement.builder(query).addPositionalValues(id, data).build();
        session.execute(insertToTable);
    }

    public void insertIntoDndEntries(int parentId, String key, String dndType, String msisdn) {
        SimpleStatement insert = SimpleStatement.builder(INSERT_DND_ENTRY)
                .addPositionalValues(parentId, key, dndType, msisdn)
                .build();
        session.execute(insert);
    }

    public boolean deleteFromTableIfExists(String table, String id) {
        String query = String.format(DELETE_QUERY_IF_EXISTS, table);
        SimpleStatement deleteFromTable = SimpleStatement.builder(query).addPositionalValues(id).build();
        return session.execute(deleteFromTable).wasApplied();
    }

    public void deleteFromTable(String table, String id) {
        String query = String.format(DELETE_QUERY, table);
        SimpleStatement deleteFromTable = SimpleStatement.builder(query).addPositionalValues(id).build();
        session.execute(deleteFromTable);
    }

    public void deleteDndEntriesByParentId(int parentId) {
        List<Row> entries = getDndEntriesByParentId(parentId);
        for (Row row : entries) {
            String dndType = row.getString("dnd_type");
            String msisdn = row.getString(MSISDN);
            deleteDndEntry(dndType, msisdn);
        }
    }

    public void deleteDndEntry(String dndType, String msisdn) {
        SimpleStatement delete = SimpleStatement.builder(DELETE_DND_BY_KEY)
                .addPositionalValues(dndType, msisdn)
                .build();
        session.execute(delete);
    }

    public String selectFromTable(String table, String id) {
        String query = String.format(SELECT_QUERY_BY_ID, table);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(id).build();
        Row result = session.execute(selectFromTable).one();
        if (Objects.isNull(result)) {
            return null;
        }
        return result.getString("data");
    }

    public String selectFromTableWithDelete(String table, String id) {
        String data = this.selectFromTable(table, id);
        this.deleteFromTable(table, id);
        return data;
    }

    public List<String> selectAllFromTable(String table) {
        String query = String.format(SELECT_ALL_QUERY, table);
        SimpleStatement selectAllFromTable = SimpleStatement.builder(query).build();
        List<Row> rows = session.execute(selectAllFromTable).all();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> row.getString("data")).toList();
    }

    public List<String> selectAllFromTableWithLimit(String table, int limit) {
        String query = String.format(SELECT_ALL_WITH_LIMIT_QUERY, table);
        SimpleStatement selectAllFromTable = SimpleStatement.builder(query).addPositionalValues(limit).build();
        return this.getListOfString(selectAllFromTable);
    }

    public boolean existsInTableById(String table, String id) {
        String query = String.format(SELECT_QUERY_BY_ID, table);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(id).build();
        Row result = session.execute(selectFromTable).one();
        return Objects.nonNull(result);
    }

    public boolean isJtiBlacklisted(String jti) {
        try {
            PreparedStatement ps = session.prepare(SELECT_BEARER_TOKEN_BLACKLIST);
            ResultSet rs = session.execute(ps.bind(jti));
            return rs.one() != null;
        } catch (Exception e) {
            log.error("Failed to check blacklist for JTI {}: {}", jti, e.getMessage(), e);
            return false; // fail open — don't block traffic if ScyllaDB is temporarily down
        }
    }

    public void insertIntoPendingDlrTable(int networkId, String messageId, String data) {
        String query = String.format("""
                INSERT INTO %s (network_id, message_id, timestamp, data)
                VALUES (?, ?, now(), ?);
                """, ScyllaTablesConstants.SMPP_PENDING_DLR_TABLE);
        SimpleStatement insertIntoPendingDlrTable = SimpleStatement.builder(query).addPositionalValues(networkId, messageId, data).build();
        session.execute(insertIntoPendingDlrTable);
    }

    public List<String> selectAllPendingDlrByNetworkId(int networkId) {
        String query = String.format("""
                SELECT data FROM %s WHERE network_id = ?;
                """, ScyllaTablesConstants.SMPP_PENDING_DLR_TABLE);
        SimpleStatement selectAllPendingDlr = SimpleStatement.builder(query).addPositionalValues(networkId).build();
        return this.getListOfString(selectAllPendingDlr);
    }

    public void deletePendingDlrByNetworkId(int networkId) {
        String deleteQuery = String.format("""
                DELETE FROM %s WHERE network_id = ?;
                """, ScyllaTablesConstants.SMPP_PENDING_DLR_TABLE);
        SimpleStatement delete = SimpleStatement.builder(deleteQuery).addPositionalValues(networkId).build();
        session.execute(delete);
    }

    public void insertIntoRetriesTable(long sendTime, String data) {
        String query = String.format("""
                INSERT INTO %s (timestamp, send_time, data)
                VALUES (now(), ?, ?);
                """, ScyllaTablesConstants.RETRIES_TABLE);
        SimpleStatement insertIntoRetries = SimpleStatement.builder(query).addPositionalValues(sendTime, data).build();
        session.execute(insertIntoRetries);
    }

    public List<String> selectAllMessageRetriesBySendTime(long sendTime) {
        String query = String.format("""
                SELECT data FROM %s WHERE send_time = ?;
                """, ScyllaTablesConstants.RETRIES_TABLE);
        SimpleStatement selectMessagesToRetries = SimpleStatement.builder(query).addPositionalValues(sendTime).build();
        return this.getListOfString(selectMessagesToRetries);
    }

    public void deleteAllMessageRetriesBySendTime(long sendTime) {
        String deleteQuery = String.format("""
                DELETE FROM %s WHERE send_time = ?;
                """, ScyllaTablesConstants.RETRIES_TABLE);
        SimpleStatement delete = SimpleStatement.builder(deleteQuery).addPositionalValues(sendTime).build();
        session.execute(delete);
    }

    private List<String> getListOfString(SimpleStatement simpleStatement) {
        List<Row> rows = session.execute(simpleStatement).all();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> row.getString("data")).toList();
    }

    public List<Row> getDndEntriesByParentId(int parentId) {
        SimpleStatement select = SimpleStatement.builder(SELECT_DND_BY_PARENT_ID)
                .addPositionalValue(parentId)
                .build();
        return session.execute(select).all();
    }

    public boolean dndEntriesExistByParentId(int parentId) {
        SimpleStatement select = SimpleStatement.builder(SELECT_DND_EXISTS_BY_PARENT_ID)
                .addPositionalValue(parentId)
                .build();
        return session.execute(select).one() != null;
    }

    public boolean isInDndWithExactKey(String dndType, String msisdn, String keyParam) {
        SimpleStatement stmt = SimpleStatement.builder(SELECT_DND_BY_TYPE_AND_MSISDN)
                .addPositionalValues(dndType, msisdn)
                .build();

        Row result = session.execute(stmt).one();
        if (result == null) {
            return false;
        }

        String dbKey = result.getString("key");
        return keyParam.equals(dbKey);
    }

    public void insertIntoErrorNetworks(String key, String messageId, String data) {
        String query = String.format("""
                INSERT INTO %s (key, message_id, data)
                VALUES (?, ?, ?);
                """, ScyllaTablesConstants.ERROR_NETWORK_TABLE);
        SimpleStatement insertIntoErrorNetwork = SimpleStatement.builder(query).addPositionalValues(key, messageId, data).build();
        session.execute(insertIntoErrorNetwork);
    }

    public void deleteErrorNetworksByKeyAndMessageId(String key, String messageId) {
        String deleteQuery = String.format("""
                DELETE FROM %s WHERE key = ? AND message_id = ?;
                """, ScyllaTablesConstants.ERROR_NETWORK_TABLE);
        SimpleStatement delete = SimpleStatement.builder(deleteQuery).addPositionalValues(key, messageId).build();
        session.execute(delete);
    }

    public List<String> getMessageFromErrorNetworksByKeyAndMessageId(String key, String messageId) {
        String query = String.format("""
                        SELECT data FROM %s WHERE key = ? AND message_id = ?;
                """, ScyllaTablesConstants.ERROR_NETWORK_TABLE);
        SimpleStatement selectAllFromTable = SimpleStatement.builder(query).addPositionalValues(key, messageId).build();
        return this.getListOfString(selectAllFromTable);
    }

    public List<String> getMessageFromErrorNetworksByKey(String key) {
        String query = String.format("""
                    SELECT data FROM %s WHERE key = ?;
                """, ScyllaTablesConstants.ERROR_NETWORK_TABLE);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(key).build();
        return this.getListOfString(selectFromTable);
    }

    public void insertIntoMessageParts(String key, String data, int ttl) {
        String query = String.format(INSERT_QUERY_WITH_TTL, ScyllaTablesConstants.MESSAGE_PARTS_TABLE, ttl);
        SimpleStatement insertToTable = SimpleStatement.builder(query).addPositionalValues(key, data).build();
        session.execute(insertToTable);
    }

    public String getMessagePartsByKey(String key) {
        String query = String.format("""
                    SELECT data FROM %s WHERE id = ?;
                """, ScyllaTablesConstants.MESSAGE_PARTS_TABLE);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(key).build();
        var listResult = this.getListOfString(selectFromTable);
        return (listResult.isEmpty()) ? null : listResult.getFirst();
    }

    public String getSmppMessagePartsByKey(String key) {
        String query = String.format("""
                SELECT data FROM %s WHERE id = ?;
            """, ScyllaTablesConstants.SMPP_MESSAGE_PARTS_TABLE);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(key).build();
        Row result = session.execute(selectFromTable).one();
        if (Objects.isNull(result)) {
            return null;
        }
        return result.getString("data");
    }

    public void insertIntoSmppMessageParts(String key, String data, int ttl) {
        String query = String.format(INSERT_SMPP_MESSAGE_PARTS, ScyllaTablesConstants.SMPP_MESSAGE_PARTS_TABLE, ttl);
        SimpleStatement insertToTable = SimpleStatement.builder(query).addPositionalValues(key, data).build();
        session.execute(insertToTable);
    }

    public void deleteSmppMessagePartsByKey(String key) {
        String query = String.format(DELETE_SMPP_MESSAGE_PARTS_BY_ID, ScyllaTablesConstants.SMPP_MESSAGE_PARTS_TABLE);
        SimpleStatement deleteFromTable = SimpleStatement.builder(query)
                .addPositionalValues(key)
                .build();
        session.execute(deleteFromTable);
    }

    public void insertHomeRouting(String ss7NetworkId, String countryCode, String mccMnc, String smsc) {
        SimpleStatement insert = SimpleStatement.builder(INSERT_HOME_ROUTING)
                .addPositionalValues(ss7NetworkId, countryCode, mccMnc, smsc)
                .build();
        session.execute(insert);
    }

    public List<UtilsRecords.CcMccMnc> getHomeRoutingByNetworkIdAndCountryCode(String ss7NetworkId, String countryCode) {
        return session.execute(
                        SimpleStatement.builder(SELECT_HOME_ROUTING_BY_NETWORK_AND_COUNTRY)
                                .addPositionalValues(ss7NetworkId, countryCode)
                                .build()
                )
                .all().stream()
                .map(row -> new UtilsRecords.CcMccMnc(
                        row.getString(COUNTRY_CODE_COLUMN),
                        row.getString("mcc_mnc"),
                        row.getString("smsc")
                ))
                .toList();
    }

    public void deleteHomeRoutingByNetworkCountryAndMcc(String ss7NetworkId, String countryCode, String mccMnc) {
        SimpleStatement delete = SimpleStatement.builder(DELETE_HOME_ROUTING_BY_NETWORK_COUNTRY_AND_MCC)
                .addPositionalValues(ss7NetworkId, countryCode, mccMnc)
                .build();
        session.execute(delete);
    }

    public void deleteHomeRoutingByNetworkId(String ss7NetworkId) {
        SimpleStatement delete = SimpleStatement.builder(DELETE_HOME_ROUTING_BY_NETWORK)
                .addPositionalValues(ss7NetworkId)
                .build();
        session.execute(delete);
    }

    public void upsertPlmnPrefix(UtilsRecords.PlmnPrefix plmnPrefix) {
        Objects.requireNonNull(plmnPrefix, "PLMN prefix cannot be null");
        if (hasRange(plmnPrefix)) {
            upsertPlmnRange(plmnPrefix);
            return;
        }
        SimpleStatement upsert = SimpleStatement.builder(UPSERT_PLMN_PREFIX)
                .addPositionalValues(
                        plmnPrefix.prefix(),
                        plmnPrefix.mcc(),
                        plmnPrefix.mnc(),
                        plmnPrefix.country(),
                        plmnPrefix.countryCode(),
                        plmnPrefix.countryIso()
                )
                .build();
        session.execute(upsert);
    }

    private void upsertPlmnRange(UtilsRecords.PlmnPrefix plmnPrefix) {
        if (!hasRange(plmnPrefix)) {
            return;
        }
        long rangeStart = Long.parseLong(plmnPrefix.snRangeStart());
        long rangeStop = Long.parseLong(plmnPrefix.snRangeStop());
        SimpleStatement upsert = SimpleStatement.builder(UPSERT_PLMN_RANGE)
                .addPositionalValues(
                        rangeBucket(plmnPrefix.snRangeStart()),
                        rangeStart,
                        rangeStop,
                        plmnPrefix.prefix(),
                        plmnPrefix.mcc(),
                        plmnPrefix.mnc(),
                        plmnPrefix.country(),
                        plmnPrefix.countryCode(),
                        plmnPrefix.countryIso()
                )
                .build();
        session.execute(upsert);
    }

    public void deletePlmnPrefix(String prefix) {
        if (Objects.isNull(prefix) || prefix.isBlank()) {
            return;
        }
        SimpleStatement delete = SimpleStatement.builder(DELETE_PLMN_PREFIX)
                .addPositionalValue(prefix)
                .build();
        session.execute(delete);
    }

    public void deletePlmnRange(String snRangeStart) {
        if (Objects.isNull(snRangeStart) || snRangeStart.isBlank()) {
            return;
        }
        SimpleStatement delete = SimpleStatement.builder(DELETE_PLMN_RANGE)
                .addPositionalValues(rangeBucket(snRangeStart), Long.parseLong(snRangeStart))
                .build();
        session.execute(delete);
    }

    public UtilsRecords.PlmnPrefix getPlmnByPrefix(String prefix) {
        if (Objects.isNull(prefix) || prefix.isBlank()) {
            return null;
        }
        Row row = session.execute(
                SimpleStatement.builder(SELECT_PLMN_BY_PREFIX)
                        .addPositionalValue(prefix)
                        .build()
        ).one();
        if (Objects.isNull(row)) {
            return null;
        }
        return new UtilsRecords.PlmnPrefix(
                row.getString("prefix"),
                row.getString("mcc"),
                row.getString("mnc"),
                row.getString("country"),
                row.getString(COUNTRY_CODE_COLUMN),
                row.getString("country_iso")
        );
    }

    public UtilsRecords.PlmnPrefix findPlmnByMsisdn(String msisdn) {
        String normalizedMsisdn = normalizeMsisdn(msisdn);
        if (Objects.isNull(normalizedMsisdn)) {
            return null;
        }
        UtilsRecords.PlmnPrefix rangeMatch = findPlmnByRange(normalizedMsisdn);
        if (Objects.nonNull(rangeMatch)) {
            return rangeMatch;
        }
        String prefixCandidate = normalizedMsisdn.substring(
                0, Math.min(normalizedMsisdn.length(), MAX_PLMN_PREFIX_LENGTH));
        for (int length = prefixCandidate.length(); length > 0; length--) {
            UtilsRecords.PlmnPrefix match = getPlmnByPrefix(prefixCandidate.substring(0, length));
            if (Objects.nonNull(match)) {
                return match;
            }
        }
        return null;
    }

    public UtilsRecords.PlmnPrefix findPlmnByRange(String msisdn) {
        String normalizedMsisdn = normalizeMsisdn(msisdn);
        if (Objects.isNull(normalizedMsisdn) || normalizedMsisdn.length() > MAX_PLMN_PREFIX_LENGTH) {
            return null;
        }
        long numericMsisdn;
        try {
            numericMsisdn = Long.parseLong(normalizedMsisdn);
        } catch (NumberFormatException exception) {
            return null;
        }
        Row row = session.execute(
                SimpleStatement.builder(SELECT_PLMN_RANGE)
                        .addPositionalValues(rangeBucket(normalizedMsisdn), numericMsisdn)
                        .build()
        ).one();
        if (Objects.isNull(row)) {
            return null;
        }
        long rangeStop = row.getLong("sn_range_stop");
        if (numericMsisdn > rangeStop) {
            return null;
        }
        return new UtilsRecords.PlmnPrefix(
                row.getString("prefix"),
                row.getString("mcc"),
                row.getString("mnc"),
                row.getString("country"),
                row.getString(COUNTRY_CODE_COLUMN),
                row.getString("country_iso"),
                String.valueOf(row.getLong("sn_range_start")),
                String.valueOf(rangeStop)
        );
    }

    private boolean hasRange(UtilsRecords.PlmnPrefix plmnPrefix) {
        return Objects.nonNull(plmnPrefix.snRangeStart()) && !plmnPrefix.snRangeStart().isBlank()
                && Objects.nonNull(plmnPrefix.snRangeStop()) && !plmnPrefix.snRangeStop().isBlank();
    }

    private String rangeBucket(String value) {
        return value.length() + ":" + value.charAt(0);
    }

    private String normalizeMsisdn(String msisdn) {
        if (Objects.isNull(msisdn) || msisdn.isBlank()) {
            return null;
        }
        StringBuilder normalized = new StringBuilder(msisdn.length());
        for (char character : msisdn.toCharArray()) {
            if (character >= '0' && character <= '9') {
                normalized.append(character);
            } else if (character != '+' && character != '-' && !Character.isWhitespace(character)) {
                return null;
            }
        }
        return normalized.isEmpty() ? null : normalized.toString();
    }

    public void insertIntoHomeRoutingCorrelationValue(String key, String data, int ttl) {
        String query = String.format("""
                INSERT INTO %s (correlation_id, data)
                VALUES (?, ?) USING TTL %s;
                """, ScyllaTablesConstants.HOME_ROUTING_CORRELATION_VALUE_TABLE, ttl);
        SimpleStatement insertToTable = SimpleStatement.builder(query).addPositionalValues(key, data).build();
        session.execute(insertToTable);
    }

    public String getHomeRoutingCorrelationValue(String key) {
        String query = String.format("""
                    SELECT data FROM %s WHERE correlation_id = ?;
                """, ScyllaTablesConstants.HOME_ROUTING_CORRELATION_VALUE_TABLE);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(key).build();
        var listResult = this.getListOfString(selectFromTable);
        return (listResult.isEmpty()) ? null : listResult.getFirst();
    }

    public void insertInPendingMessagesTable(String topic, List<String> batchData) {
        String query = String.format("""
                    INSERT INTO %s (topic, message_id, created_at, data)
                    VALUES (?, ?, toTimestamp(now()), ?);
                """, ScyllaTablesConstants.PENDING_MESSAGES_TABLE);

        PreparedStatement insertStmt = session.prepare(query);
        BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
        for (String msg : batchData) {
            var messageEvent = Converter.stringToObject(msg, MessageEvent.class);
            assert messageEvent != null;
            BoundStatement bound = insertStmt.bind(
                    topic,
                    messageEvent.getMessageId(),
                    msg
            );
            batch.addStatement(bound);
        }
        session.execute(batch.build());
    }


    public List<String> getPendingMessagesByTopic(String topic) {
        String query = String.format("""
                    SELECT data FROM %s WHERE topic = ? ;
                """, ScyllaTablesConstants.PENDING_MESSAGES_TABLE);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(topic).build();
        return this.getListOfString(selectFromTable);
    }


    public void deletePendingMessagesByTopic(String topic) {
        String query = String.format("""
                    DELETE FROM %s WHERE topic = ?  ;
                """, ScyllaTablesConstants.PENDING_MESSAGES_TABLE);
        SimpleStatement delete = SimpleStatement.builder(query).addPositionalValues(topic).build();
        session.execute(delete);
    }

    public void createSipRegistrationStateTable() {
        String query = String.format(CREATE_SIP_REGISTRATION_STATE_TABLE, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE);
        session.execute(SimpleStatement.newInstance(query));
    }

    public void createSipRegistrationAorIndex() {
        String query = String.format(CREATE_SIP_REG_AOR_INDEX, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE);
        session.execute(SimpleStatement.newInstance(query));
    }

    public boolean existsSipRegistrationStateByMsisdn(String msisdn) {
        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SIP_REG_BY_MSISDN, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE)
                ).addPositionalValue(msisdn).build()
        ).one();

        return row != null;
    }

    public MessageRegister getSipRegistrationByMsisdn(String msisdn) {
        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SIP_REG_EVENT_BY_MSISDN, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE)
                ).addPositionalValue(msisdn).build()
        ).one();

        return toMessageRegister(row);
    }

    public boolean existsSipRegistrationStateByAor(String aor) {
        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SIP_REG_BY_AOR, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE)
                ).addPositionalValue(aor).build()
        ).one();

        return row != null;
    }

    public MessageRegister getSipRegistrationByAor(String aor) {
        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SIP_REG_EVENT_BY_AOR, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE)
                ).addPositionalValue(aor).build()
        ).one();

        return toMessageRegister(row);
    }

    private MessageRegister toMessageRegister(Row row) {
        if (Objects.isNull(row)) return null;

        return MessageRegister.builder()
                .msisdn(row.getString(MSISDN))
                .aor(row.getString(AOR))
                .imsi(row.getString(IMSI))
                .registrationType(RegistrationType.valueOf(row.getString(REGISTRATION_TYPE)))
                .regEventState(RegEventState.valueOf(row.getString(REG_EVENT_STATE)))
                .gsmScfAddress(row.getString(GSM_SCF_ADDRESS))
                .supportSbi(!row.isNull(SUPPORT_SBI) && row.getBoolean(SUPPORT_SBI))
                .build();
    }

    // Kept as a compatibility API for modules that persist SIP registration state.
    @SuppressWarnings("java:S107")
    public void upsertSipRegistrationState(
            String msisdn, String aor, String imsi, String registrationType,  String regEventState, String gsmScfAddress, boolean supportSbi, int expires
    ) {
        session.execute(
                SimpleStatement.builder(
                        String.format(UPSERT_SIP_REG, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE)
                ).addPositionalValues(
                        msisdn, aor, imsi, registrationType, regEventState, gsmScfAddress, supportSbi, expires
                ).build()
        );
    }

    public void updateSipRegEventState(String msisdn, String regEventState, String aor) {
        if (Objects.isNull(msisdn) || msisdn.isBlank()) return;
        if (Objects.isNull(regEventState) || regEventState.isBlank()) return;

        String table = ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE;

        if (Objects.nonNull(aor)) {
            session.execute(
                    SimpleStatement.builder(String.format(UPDATE_SIP_REG_EVENT_STATE_AND_AOR, table))
                            .addPositionalValues(regEventState, aor, msisdn)
                            .build()
            );
            return;
        }

        session.execute(
                SimpleStatement.builder(String.format(UPDATE_SIP_REG_EVENT_STATE, table))
                        .addPositionalValues(regEventState, msisdn)
                        .build()
        );
    }

    public void createBearerTokenBlacklistTable() {
        session.execute(CREATE_BEARER_TOKEN_BLACKLIST_TABLE);
    }

    public void insertIntoBearerTokenBlacklist(String jti, int ttl) {
        SimpleStatement stmt = SimpleStatement.newInstance(INSERT_BEARER_TOKEN_BLACKLIST, jti, ttl);
        session.execute(stmt);
    }

    public void deleteSipRegistrationStateByMsisdn(String msisdn) {
        session.execute(
                SimpleStatement.builder(
                        String.format(DELETE_SIP_REG_BY_MSISDN, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE)
                ).addPositionalValue(msisdn).build()
        );
    }

    // Kept as a compatibility API for modules that persist SIP registration state with TTL.
    @SuppressWarnings("java:S107")
    public void upsertSipRegistrationStateWithTtl(
            String msisdn,
            String aor,
            String imsi,
            String registrationType,
            String regEventState,
            String gsmScfAddress,
            boolean supportSbi,
            int expires,
            int ttlSeconds) {
        if (Objects.isNull(msisdn) || msisdn.isBlank()) {
            log.warn("Skipping SIP registration state upsert because msisdn is empty");
            return;
        }

        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("TTL must be greater than zero for SIP registration state");
        }

        session.execute(
                SimpleStatement.builder(
                                String.format(
                                        UPSERT_SIP_REG_WITH_TTL,
                                        ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE))
                        .addPositionalValues(
                                msisdn,
                                aor,
                                imsi,
                                registrationType,
                                regEventState,
                                gsmScfAddress,
                                supportSbi,
                                expires,
                                ttlSeconds)
                        .build());
    }

    public boolean promoteSipRegistrationStateToActive(String msisdn, String aor) {
        if (Objects.isNull(msisdn) || msisdn.isBlank()) {
            log.warn("Skipping SIP registration state promotion because msisdn is empty");
            return false;
        }

        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SIP_REG_EVENT_BY_MSISDN, ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE)
                ).addPositionalValue(msisdn).build()
        ).one();

        if (Objects.isNull(row)) {
            log.warn("Skipping SIP registration state promotion because row was not found msisdn={}", msisdn);
            return false;
        }

        String aorToPersist = Objects.nonNull(aor) && !aor.isBlank()
                ? aor
                : row.getString(AOR);

        upsertSipRegistrationState(
                row.getString(MSISDN),
                aorToPersist,
                row.getString(IMSI),
                row.getString(REGISTRATION_TYPE),
                RegEventState.ACTIVE.name(),
                row.getString(GSM_SCF_ADDRESS),
                !row.isNull(SUPPORT_SBI) && row.getBoolean(SUPPORT_SBI),
                row.isNull("expires") ? 0 : row.getInt("expires")
        );

        return true;
    }

    public void createSmsfUeSmsContextTables() {
        session.execute(SimpleStatement.newInstance(String.format(CREATE_QUERY, ScyllaTablesConstants.SMSF_SUBMIT_SM_RESULT_TABLE)));

        session.execute(SimpleStatement.newInstance(String.format(CREATE_SMSF_UE_SMS_CONTEXT_TABLE, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)));

        session.execute(SimpleStatement.newInstance(String.format(CREATE_SMSF_UE_SMS_CONTEXT_GPSI_INDEX, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)));
    }

    public void updateSmsfRegEventState(String supi, String regEventState) {
        if (isBlank(supi) || isBlank(regEventState)) {
            return;
        }

        session.execute(
                SimpleStatement.builder(String.format(UPDATE_SMSF_REG_EVENT_STATE, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValues(regEventState, supi).build()
        );
    }

    public UeSmsContextData getActiveSmsfUeSmsContextByGpsi(String msisdn) {
        if (isBlank(msisdn)) {
            return null;
        }

        String gpsi = MsisdnUtils.normalize(msisdn);
        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SMSF_UE_SMS_CONTEXT_WITH_STATE_BY_GPSI, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValue(gpsi).build()
        ).one();

        if (Objects.isNull(row) || !this.isActiveState(row.getString(REG_EVENT_STATE))) {
            return null;
        }

        return Converter.stringToObject(row.getString("data"), UeSmsContextData.class);
    }

    private boolean isActiveState(String regEventState) {
        return isBlank(regEventState) || RegEventState.ACTIVE.name().equals(regEventState);
    }

    public void upsertSmsfUeSmsContext(UeSmsContextData ueSmsContextData) {
        if (Objects.isNull(ueSmsContextData)) {
            throw new IllegalArgumentException("UE SMS context data is required");
        }

        if (isBlank(ueSmsContextData.getSupi())) {
            throw new IllegalArgumentException("supi is required");
        }

        String data = Converter.valueAsString(ueSmsContextData);

        session.execute(
                SimpleStatement.builder(String.format(UPSERT_SMSF_UE_SMS_CONTEXT, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValues(
                        ueSmsContextData.getSupi(),
                        ueSmsContextData.getGpsi(),
                        data,
                        RegEventState.ACTIVE.name()).build()
        );
    }

    public UeSmsContextData getSmsfUeSmsContextBySupi(String supi) {
        if (isBlank(supi)) {
            return null;
        }

        Row row = session.execute(
                SimpleStatement.builder(String.format(SELECT_SMSF_UE_SMS_CONTEXT_BY_SUPI, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValue(supi).build()
        ).one();

        if (Objects.isNull(row)) {
            return null;
        }

        return Converter.stringToObject(row.getString("data"), UeSmsContextData.class);
    }

    public String getSmsfUeSmsContextSupiByGpsi(String msisdn) {
        if (isBlank(msisdn)) {
            return null;
        }

        String gpsi = MsisdnUtils.normalize(msisdn);
        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SMSF_UE_SMS_CONTEXT_SUPI_BY_GPSI, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValue(gpsi).build()
        ).one();

        if (Objects.isNull(row)) {
            return null;
        }

        return row.getString("supi");
    }

    public String getSmsfUeSmsContextGpsiBySupi(String supi) {
        if (isBlank(supi)) {
            return null;
        }

        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SMSF_UE_SMS_CONTEXT_GPSI_BY_SUPI, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValue(supi).build()
        ).one();

        if (Objects.isNull(row)) {
            return null;
        }

        return row.getString("gpsi");
    }

    public boolean existsSmsfUeSmsContextBySupi(String supi) {
        if (isBlank(supi)) {
            return false;
        }

        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SMSF_UE_SMS_CONTEXT_KEY_BY_SUPI, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValue(supi).build()
        ).one();

        return Objects.nonNull(row);
    }

    public boolean existsSmsfUeSmsContextByGpsi(String gpsi) {
        if (isBlank(gpsi)) {
            return false;
        }

        Row row = session.execute(
                SimpleStatement.builder(
                        String.format(SELECT_SMSF_UE_SMS_CONTEXT_KEY_BY_GPSI, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValue(gpsi).build()
        ).one();

        return Objects.nonNull(row);
    }

    public void deleteSmsfUeSmsContextBySupi(String supi) {
        if (isBlank(supi)) {
            return;
        }

        session.execute(
                SimpleStatement.builder(
                        String.format(DELETE_SMSF_UE_SMS_CONTEXT_BY_SUPI, ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE)
                ).addPositionalValue(supi).build()
        );
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    public String getSmsfMessagePartsByKey(String key) {
        String query = String.format(SELECT_SMSF_MESSAGE_PARTS_BY_ID, ScyllaTablesConstants.SMSF_MESSAGE_PARTS_TABLE);
        SimpleStatement selectFromTable = SimpleStatement.builder(query).addPositionalValues(key).build();
        Row result = session.execute(selectFromTable).one();
        if (Objects.isNull(result)) {
            return null;
        }
        return result.getString("data");
    }

    public void insertIntoSmsfMessageParts(String key, String data, int ttl) {
        String query = String.format(INSERT_SMSF_MESSAGE_PARTS, ScyllaTablesConstants.SMSF_MESSAGE_PARTS_TABLE, ttl);
        SimpleStatement insertToTable = SimpleStatement.builder(query).addPositionalValues(key, data).build();
        session.execute(insertToTable);
    }

    public void deleteSmsfMessagePartsByKey(String key) {
        String query = String.format(DELETE_SMSF_MESSAGE_PARTS_BY_ID, ScyllaTablesConstants.SMSF_MESSAGE_PARTS_TABLE);
        SimpleStatement deleteFromTable = SimpleStatement.builder(query)
                .addPositionalValues(key)
                .build();
        session.execute(deleteFromTable);
    }

}
