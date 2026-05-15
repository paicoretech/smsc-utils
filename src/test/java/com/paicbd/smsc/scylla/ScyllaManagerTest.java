package com.paicbd.smsc.scylla;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.RegEventState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static com.paicbd.smsc.scylla.ScyllaTablesConstants.MESSAGE_PARTS_TABLE;
import static com.paicbd.smsc.scylla.ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE;
import static com.paicbd.smsc.scylla.ScyllaTablesConstants.SMPP_SUBMIT_SM_RESULT_TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScyllaManagerTest {

    static Stream<Arguments> keyspaceExistsParams() {
        return Stream.of(Arguments.of(true, true));
    }

    static Stream<Arguments> selectAllParams() {
        return Stream.of(
                Arguments.of(List.of(
                        new UtilsRecords.SubmitSmResponseEvent(
                                "hash-id",
                                "1234",
                                "systemId",
                                "submitSmId",
                                "submitSmServerId",
                                "SMPP",
                                1,
                                "SP",
                                "",
                                0,
                                0,
                                "1233456",
                                11,
                                false,
                                new HashMap<>(),
                                false
                        ).toString(),
                        new UtilsRecords.SubmitSmResponseEvent(
                                "hash-id",
                                "7891",
                                "systemId",
                                "submitSmId",
                                "submitSmServerId",
                                "HTTP",
                                10,
                                "SP",
                                "",
                                0,
                                0,
                                "1233456",
                                0,
                                false,
                                new HashMap<>(),
                                false
                        ).toString()
                )),
                Arguments.of(List.of())
        );
    }


    static Stream<Arguments> existsByIdParams() {
        return Stream.of(Arguments.of(true), Arguments.of(false));
    }

    static Stream<Arguments> dndKeyParams() {
        return Stream.of(
                Arguments.of("K-1", "K-1", true),
                Arguments.of("K-1", "K-2", false),
                Arguments.of(null, "K-1", false)
        );
    }

    private ScyllaManager newManagerWithSession(CqlSession session) {
        try (org.mockito.MockedStatic<CqlSession> mocked = org.mockito.Mockito.mockStatic(CqlSession.class)) {
            CqlSessionBuilder builder = org.mockito.Mockito.mock(CqlSessionBuilder.class, Mockito.RETURNS_SELF);
            mocked.when(CqlSession::builder).thenReturn(builder);
            org.mockito.Mockito.when(builder.addContactPoints(org.mockito.ArgumentMatchers.anyList())).thenReturn(builder);
            org.mockito.Mockito.when(builder.withConfigLoader(org.mockito.ArgumentMatchers.any())).thenReturn(builder);
            org.mockito.Mockito.when(builder.withKeyspace(org.mockito.ArgumentMatchers.anyString())).thenReturn(builder);
            org.mockito.Mockito.when(builder.build()).thenReturn(session);
            return new ScyllaManager("127.0.0.1:9042", "dc1", "", "", false);
        }
    }

    @ParameterizedTest
    @MethodSource("keyspaceExistsParams")
    void keyspaceExists(boolean rowPresentInDb, boolean expectedExist) {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(rowPresentInDb ? mock(Row.class) : null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager scyllamanager = newManagerWithSession(session);
        assertEquals(expectedExist, scyllamanager.keyspaceExists());
    }

    @ParameterizedTest
    @MethodSource("selectAllParams")
    void selectAllFromTable(List<String> expectedValues) {
        CqlSession session = mock(CqlSession.class);
        List<Row> rows = expectedValues.stream().map(s -> {
            Row row = mock(Row.class);
            when(row.getString("data")).thenReturn(s);
            return row;
        }).toList();
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.all()).thenReturn(rows);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager scyllaManager = newManagerWithSession(session);
        assertEquals(expectedValues, scyllaManager.selectAllFromTable(SMPP_SUBMIT_SM_RESULT_TABLE));
    }

    @ParameterizedTest
    @MethodSource("existsByIdParams")
    void existsInTableById(boolean rowFound) {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(rowFound ? mock(Row.class) : null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager scyllaManager = newManagerWithSession(session);
        assertEquals(rowFound, scyllaManager.existsInTableById(MESSAGE_PARTS_TABLE, "ID"));
    }

    @ParameterizedTest
    @MethodSource("dndKeyParams")
    void isInDndWithExactKey(String dbKey, String probeKey, boolean expected) {
        CqlSession session = mock(CqlSession.class);
        Row row = dbKey == null ? null : mock(Row.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        if (row != null) when(row.getString("key")).thenReturn(dbKey);

        ScyllaManager scyllaManager = newManagerWithSession(session);
        assertEquals(expected, scyllaManager.isInDndWithExactKey("NETWORK", "999", probeKey));
    }

    @Test
    void insertSelectDelete() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        when(row.getString("data")).thenReturn("testData");
        ResultSet setHit = mock(ResultSet.class);
        when(setHit.one()).thenReturn(row);
        ResultSet setMiss = mock(ResultSet.class);
        when(setMiss.one()).thenReturn(null);

        when(session.execute(any(SimpleStatement.class)))
                .thenReturn(mock(ResultSet.class))
                .thenReturn(setHit)
                .thenReturn(mock(ResultSet.class))
                .thenReturn(setMiss);

        ScyllaManager scyllaManager = newManagerWithSession(session);
        scyllaManager.insertIntoTable(MESSAGE_PARTS_TABLE, "id", "testData");
        assertEquals("testData", scyllaManager.selectFromTable(MESSAGE_PARTS_TABLE, "id"));
        scyllaManager.deleteFromTable(MESSAGE_PARTS_TABLE, "id");
        assertNull(scyllaManager.selectFromTable(MESSAGE_PARTS_TABLE, "id"));
    }

    @Test
    void insertIntoTableNullValues() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));
        ScyllaManager scyllaManager = newManagerWithSession(session);
        assertDoesNotThrow(() -> scyllaManager.insertIntoTable(MESSAGE_PARTS_TABLE, null, null));
        verify(session, times(1)).execute(any(SimpleStatement.class));
    }

    @Test
    void deleteFromTableNullId() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));
        ScyllaManager scyllaManager = newManagerWithSession(session);
        assertDoesNotThrow(() -> scyllaManager.deleteFromTable(MESSAGE_PARTS_TABLE, null));
        verify(session, times(1)).execute(any(SimpleStatement.class));
    }

    @Test
    void selectFromTableNull() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        ScyllaManager scyllaManager = newManagerWithSession(session);
        assertNull(scyllaManager.selectFromTable(MESSAGE_PARTS_TABLE, null));
    }

    @Test
    void existsInTableById() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        ScyllaManager scyllaManager = newManagerWithSession(session);
        assertFalse(scyllaManager.existsInTableById(MESSAGE_PARTS_TABLE, null));
    }

    @Test
    void createTable() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));
        newManagerWithSession(session).createTable(MESSAGE_PARTS_TABLE);
        verify(session).execute(any(SimpleStatement.class));
    }

    @Test
    void insertIntoDndEntries() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));
        newManagerWithSession(session).insertIntoDndEntries(10, "K", "TYPE", "999");
        verify(session).execute(any(SimpleStatement.class));
    }

    @Test
    void selectFromTableWithDelete() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        when(row.getString("data")).thenReturn("V");
        ResultSet set = mock(ResultSet.class);
        when(set.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(set, mock(ResultSet.class));
        assertEquals("V", newManagerWithSession(session).selectFromTableWithDelete(MESSAGE_PARTS_TABLE, "id"));
    }

    @Test
    void selectAllFromTableWithLimit() {
        CqlSession session = mock(CqlSession.class);
        Row row1 = mock(Row.class);
        when(row1.getString("data")).thenReturn("a");
        Row row2 = mock(Row.class);
        when(row2.getString("data")).thenReturn("b");
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.all()).thenReturn(List.of(row1, row2));
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        assertEquals(List.of("a", "b"), newManagerWithSession(session).selectAllFromTableWithLimit(MESSAGE_PARTS_TABLE, 2));
    }

    @Test
    void pendingDlr() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        when(row.getString("data")).thenReturn("D");
        ResultSet set = mock(ResultSet.class);
        when(set.all()).thenReturn(List.of(row));
        when(session.execute(any(SimpleStatement.class)))
                .thenReturn(mock(ResultSet.class))
                .thenReturn(set)
                .thenReturn(mock(ResultSet.class));
        ScyllaManager manager = newManagerWithSession(session);
        manager.insertIntoPendingDlrTable(1, "m1", "D");
        assertEquals(List.of("D"), manager.selectAllPendingDlrByNetworkId(1));
        manager.deletePendingDlrByNetworkId(1);
    }

    @Test
    void retries() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        when(row.getString("data")).thenReturn("R");
        ResultSet set = mock(ResultSet.class);
        when(set.all()).thenReturn(List.of(row));
        when(session.execute(any(SimpleStatement.class)))
                .thenReturn(mock(ResultSet.class))
                .thenReturn(set)
                .thenReturn(mock(ResultSet.class));
        ScyllaManager manager = newManagerWithSession(session);
        manager.insertIntoRetriesTable(123L, "R");
        assertEquals(List.of("R"), manager.selectAllMessageRetriesBySendTime(123L));
        manager.deleteAllMessageRetriesBySendTime(123L);
    }

    @Test
    void errorNetworks() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        when(row.getString("data")).thenReturn("E");
        ResultSet byKeyMsg = mock(ResultSet.class);
        when(byKeyMsg.all()).thenReturn(List.of(row));
        ResultSet byKey = mock(ResultSet.class);
        when(byKey.all()).thenReturn(List.of(row));
        when(session.execute(any(SimpleStatement.class)))
                .thenReturn(mock(ResultSet.class))
                .thenReturn(byKeyMsg)
                .thenReturn(byKey)
                .thenReturn(mock(ResultSet.class));
        ScyllaManager manager = newManagerWithSession(session);
        manager.insertIntoErrorNetworks("K", "M1", "E");
        assertEquals(List.of("E"), manager.getMessageFromErrorNetworksByKeyAndMessageId("K", "M1"));
        assertEquals(List.of("E"), manager.getMessageFromErrorNetworksByKey("K"));
        manager.deleteErrorNetworksByKeyAndMessageId("K", "M1");
    }

    @Test
    void messageParts() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        when(row.getString("data")).thenReturn("P");
        ResultSet set = mock(ResultSet.class);
        when(set.all()).thenReturn(List.of(row));
        when(session.execute(any(SimpleStatement.class)))
                .thenReturn(mock(ResultSet.class))
                .thenReturn(set)
                .thenReturn(mock(ResultSet.class));
        ScyllaManager manager = newManagerWithSession(session);
        manager.insertIntoMessageParts("K", "P", 60);
        assertEquals("P", manager.getMessagePartsByKey("K"));
    }

    @Test
    void smppMessageParts() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        String smppMessageJson = """
                {   "text": "This is part 1 of a multi-part SMS message that exceeds the standard 160 character limit",
                }
                """;
        when(row.getString("data")).thenReturn(smppMessageJson);

        ResultSet insertResultSet = mock(ResultSet.class);
        ResultSet selectResultSet = mock(ResultSet.class);
        when(selectResultSet.one()).thenReturn(row);

        when(session.execute(any(SimpleStatement.class)))
                .thenReturn(insertResultSet)
                .thenReturn(selectResultSet);

        ScyllaManager manager = newManagerWithSession(session);
        String expectedKey = "test_system12345";
        int ttl = 30;
        manager.insertIntoSmppMessageParts(expectedKey, smppMessageJson, ttl);

        String retrievedData = manager.getSmppMessagePartsByKey(expectedKey);
        assertEquals(smppMessageJson, retrievedData);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(2)).execute(captor.capture());

        List<SimpleStatement> statements = captor.getAllValues();
        String insertQuery = statements.get(0).getQuery();
        assertTrue(insertQuery.contains("smpp_message_parts"), "INSERT should use smpp_message_parts table");
        assertTrue(insertQuery.contains("timestamp"), "INSERT should include timestamp field");
        assertTrue(insertQuery.contains("currentTimestamp()"), "INSERT should use now() for timestamp");
        assertTrue(insertQuery.contains("currentTimestamp()"), "INSERT should use currentTimestamp() for timestamp");
        assertTrue(insertQuery.contains("USING TTL"), "INSERT should include TTL");
    }

    @Test
    void smppMessagePartsVerifiesTtl() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        String messageData = """
                {
                    "text": "Test message",
                }
                """;

        int ttl = 30;
        manager.insertIntoSmppMessageParts("provider_0167890", messageData, ttl);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String query = stmt.getQuery();

        assertTrue(query.contains("USING TTL"), "Query should include TTL");
        assertTrue(query.contains(String.valueOf(ttl)), "Query should include the TTL value");
        assertTrue(query.contains("smpp_message_parts"), "Query should use smpp_message_parts table");
        assertTrue(query.contains("timestamp"), "Query should include timestamp field");
        assertTrue(query.contains("currentTimestamp()"), "Query should use currentTimestamp() for timestamp");
    }

    @Test
    void deleteSmppMessagePartsByKey() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        String key = "test_system12345";

        manager.deleteSmppMessagePartsByKey(key);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String query = stmt.getQuery();

        assertTrue(query.contains("DELETE FROM smpp_message_parts"),
                "Should delete from smpp_message_parts table");
        assertTrue(query.contains("WHERE id = ?"),
                "Should delete by id (which deletes all timestamp entries for that id)");
    }

    @Test
    void getDndEntriesByParentId() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.all()).thenReturn(List.of(mock(Row.class), mock(Row.class)));
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        assertEquals(2, newManagerWithSession(session).getDndEntriesByParentId(55).size());
    }


    @Test
    void deleteDndEntriesByParentId() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.all()).thenReturn(List.of());
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        ScyllaManager manager = newManagerWithSession(session);
        manager.deleteDndEntriesByParentId(123);
        verify(session, times(1)).execute(any(SimpleStatement.class));
    }

    @Test
    void createHomeRoutingTableTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);
        manager.createHomeRoutingTable();
        verify(session, times(1)).execute(any(String.class));
    }

    @Test
    void insertHomeRoutingTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);
        assertDoesNotThrow(() -> manager.insertHomeRouting("1", "505", "71001", "smsc1"));
        verify(session, times(1)).execute(any(SimpleStatement.class));
    }

    @Test
    void getHomeRoutingByNetworkIdAndCountryCodeTest() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("country_code")).thenReturn("505");
        when(row.getString("mcc_mnc")).thenReturn("71030");

        when(resultSet.all()).thenReturn(List.of(row));
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        var out = manager.getHomeRoutingByNetworkIdAndCountryCode("1", "505");

        assertEquals(1, out.size());
        var rec = out.getFirst();
        assertEquals("505", rec.countryCode());
        assertEquals("71030", rec.mccMnc());
    }

    @Test
    void deleteHomeRoutingByNetworkCountryAndMccTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        String ss7NetworkId = "1";
        String countryCode = "505";
        String mccMnc = "71002";

        assertDoesNotThrow(() ->
                manager.deleteHomeRoutingByNetworkCountryAndMcc(ss7NetworkId, countryCode, mccMnc)
        );

        ArgumentCaptor<SimpleStatement> stmtCaptor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(stmtCaptor.capture());

        SimpleStatement sent = stmtCaptor.getValue();
        String cql = sent.getQuery();

        assertThat(cql).contains("DELETE FROM home_routing_cc_mcc_mnc");

        List<Object> values = sent.getPositionalValues();
        assertThat(values).containsExactly(ss7NetworkId, countryCode, mccMnc);
    }


    @Test
    void deleteHomeRoutingByNetworkIdTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);
        assertDoesNotThrow(() -> manager.deleteHomeRoutingByNetworkId("1"));
        verify(session, times(1)).execute(any(SimpleStatement.class));
    }

    @Test
    void createSipRegistrationStateTableTest() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        manager.createSipRegistrationStateTable();

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String cql = stmt.getQuery();

        assertThat(cql).contains("CREATE TABLE IF NOT EXISTS")
            .contains(SIP_REGISTRATION_STATE_TABLE)
            .contains("msisdn text PRIMARY KEY")
            .contains("registration_type text")
            .contains("reg_event_state text")
            .contains("updated_at timestamp");
    }

    @Test
    void createSipRegistrationAorIndexTest() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        manager.createSipRegistrationAorIndex();

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session).execute(captor.capture());

        String cql = captor.getValue().getQuery();

        assertThat(cql)
                .contains("CREATE INDEX IF NOT EXISTS")
                .contains("sip_reg_aor_idx")
                .contains("ON " + SIP_REGISTRATION_STATE_TABLE)
                .contains("(aor)");
    }

    @Test
    void getSipRegistrationByMsisdnReturnsAor() {
        CqlSession session = mock(CqlSession.class);
        ResultSet rs = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("msisdn")).thenReturn("50588888888");
        when(row.getString("aor")).thenReturn("sip:user@ims.local");
        when(row.getString("imsi")).thenReturn("123");
        when(row.getString("registration_type")).thenReturn("INITIAL_REGISTRATION");
        when(row.getString("reg_event_state")).thenReturn("ACTIVE");
        when(row.getString("gsm_scf_address")).thenReturn("gsm-scf");
        when(row.isNull("support_sbi")).thenReturn(false);
        when(row.getBoolean("support_sbi")).thenReturn(true);

        when(rs.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(rs);

        ScyllaManager manager = newManagerWithSession(session);

        var reg = manager.getSipRegistrationByMsisdn("50588888888");

        assertThat(reg).isNotNull();
        assertThat(reg.getMsisdn()).isEqualTo("50588888888");
        assertThat(reg.getAor()).isEqualTo("sip:user@ims.local");
        assertThat(reg.getRegEventState()).isEqualTo(RegEventState.ACTIVE);
    }

    @Test
    void existsSipRegistrationStateByMsisdnWhenRowPresentReturnsTrue() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(mock(Row.class));
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        boolean exists = manager.existsSipRegistrationStateByMsisdn("50588888888");
        assertTrue(exists);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String cql = stmt.getQuery();

        assertThat(cql).contains("SELECT msisdn")
            .contains("FROM " + SIP_REGISTRATION_STATE_TABLE)
            .contains("WHERE msisdn = ?");

        List<Object> values = stmt.getPositionalValues();
        assertThat(values).containsExactly("50588888888");
    }

    @Test
    void existsSipRegistrationStateByMsisdnWhenNoRowReturnsFalse() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        boolean exists = manager.existsSipRegistrationStateByMsisdn("50500000000");
        assertFalse(exists);
    }

    @Test
    void upsertSipRegistrationStateSendsInsertWithExpectedValues() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.upsertSipRegistrationState(
                "50588888888",
                "sip:user@ims.local",
                "123456789012345",
                "INITIAL_REGISTRATION",
                "PENDING",
                "gsm-scf.ims.local",
                true,
                3600
        );

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String cql = stmt.getQuery();

        assertThat(cql).contains("INSERT INTO " + SIP_REGISTRATION_STATE_TABLE)
            .contains("msisdn")
            .contains("aor")
            .contains("registration_type")
            .contains("reg_event_state")
            .contains("updated_at")
            .doesNotContain("USING TTL");

        List<Object> values = stmt.getPositionalValues();
        assertThat(values).containsExactly(
                "50588888888",
                "sip:user@ims.local",
                "123456789012345",
                "INITIAL_REGISTRATION",
                "PENDING",
                "gsm-scf.ims.local",
                true,
                3600
        );
    }

    @Test
    void updateSipRegEventStateSendsUpdateWithExpectedValues() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.updateSipRegEventState("50588888888", "ACTIVE", null);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String cql = stmt.getQuery();

        assertThat(cql)
                .contains("UPDATE " + SIP_REGISTRATION_STATE_TABLE)
                .contains("SET reg_event_state = ?")
                .contains("updated_at = toTimestamp(now())")
                .contains("WHERE msisdn = ?");

        List<Object> values = stmt.getPositionalValues();
        assertThat(values).containsExactly("ACTIVE", "50588888888");
    }

    @Test
    void deleteSipRegistrationStateByMsisdnSendsDeleteWithExpectedValue() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        manager.deleteSipRegistrationStateByMsisdn("50588888888");

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String cql = stmt.getQuery();

        assertThat(cql).contains("DELETE FROM " + SIP_REGISTRATION_STATE_TABLE)
            .contains("WHERE msisdn = ?");

        List<Object> values = stmt.getPositionalValues();
        assertThat(values).containsExactly("50588888888");
    }

    @Test
    void existsSipRegistrationStateByAorReturnsTrue() {
        CqlSession session = mock(CqlSession.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.one()).thenReturn(mock(Row.class));
        when(session.execute(any(SimpleStatement.class))).thenReturn(rs);

        ScyllaManager manager = newManagerWithSession(session);

        assertTrue(manager.existsSipRegistrationStateByAor("sip:user@ims.local"));

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        assertThat(stmt.getQuery())
                .contains("WHERE aor = ?");
        assertThat(stmt.getPositionalValues())
                .containsExactly("sip:user@ims.local");
    }

    @Test
    void getSipRegistrationByAorReturnsRegistration() {
        CqlSession session = mock(CqlSession.class);
        ResultSet rs = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("msisdn")).thenReturn("50588888888");
        when(row.getString("aor")).thenReturn("sip:user@ims.local");
        when(row.getString("imsi")).thenReturn("123456789012345");
        when(row.getString("registration_type")).thenReturn("INITIAL_REGISTRATION");
        when(row.getString("reg_event_state")).thenReturn("ACTIVE");
        when(row.getString("gsm_scf_address")).thenReturn("gsm-scf");
        when(row.isNull("support_sbi")).thenReturn(false);
        when(row.getBoolean("support_sbi")).thenReturn(true);

        when(rs.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(rs);

        ScyllaManager manager = newManagerWithSession(session);

        var reg = manager.getSipRegistrationByAor("sip:user@ims.local");

        assertThat(reg).isNotNull();
        assertThat(reg.getMsisdn()).isEqualTo("50588888888");
        assertThat(reg.getAor()).isEqualTo("sip:user@ims.local");
    }

}
