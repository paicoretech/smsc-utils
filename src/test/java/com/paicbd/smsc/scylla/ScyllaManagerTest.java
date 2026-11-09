package com.paicbd.smsc.scylla;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.MsisdnUtils;
import com.paicore.sba.stack.openapi.model.AccessType;
import com.paicore.sba.stack.openapi.model.RatType;
import com.paicore.sba.stack.openapi.model.UeSmsContextData;
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
import static com.paicbd.smsc.scylla.ScyllaTablesConstants.SMSF_SUBMIT_SM_RESULT_TABLE;
import static com.paicbd.smsc.scylla.ScyllaTablesConstants.SIP_REGISTRATION_STATE_TABLE;
import static com.paicbd.smsc.scylla.ScyllaTablesConstants.SMPP_SUBMIT_SM_RESULT_TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.paicbd.smsc.scylla.ScyllaTablesConstants.SMSF_UE_SMS_CONTEXT_TABLE;

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
                                false,
                                "",
                                "",
                                ""
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
                                false,
                                "",
                                "",
                                ""
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

    private UeSmsContextData getUeSmsContextData() {
        return new UeSmsContextData()
                .supi("imsi-460000000000001")
                .gpsi("msisdn-8613915900000")
                .pei("imei-123456789012345")
                .accessType(AccessType._3_GPP_ACCESS)
                .amfId(UUID.fromString("54804518-e30d-11e8-9f32-f2801f1b9fd1"))
                .ratType(RatType.NR)
                .ueTimeZone("+08:00");
    }

    private String getUeSmsContextDataJson() {
        return """
                {
                  "supi": "imsi-460000000000001",
                  "gpsi": "msisdn-8613915900000",
                  "pei": "imei-123456789012345",
                  "accessType": "3GPP_ACCESS",
                  "amfId": "54804518-e30d-11e8-9f32-f2801f1b9fd1",
                  "ratType": "NR",
                  "ueTimeZone": "+08:00"
                }
                """;
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
    void deleteFromTableIfExistsReportsWhoWonTheRow() {
        CqlSession session = mock(CqlSession.class);
        ResultSet applied = mock(ResultSet.class);
        when(applied.wasApplied()).thenReturn(true);
        ResultSet notApplied = mock(ResultSet.class);
        when(notApplied.wasApplied()).thenReturn(false);
        when(session.execute(any(SimpleStatement.class))).thenReturn(applied, notApplied);

        ScyllaManager scyllaManager = newManagerWithSession(session);

        assertTrue(scyllaManager.deleteFromTableIfExists(MESSAGE_PARTS_TABLE, "id-1"));
        assertFalse(scyllaManager.deleteFromTableIfExists(MESSAGE_PARTS_TABLE, "id-1"));

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(2)).execute(captor.capture());
        assertThat(captor.getValue().getQuery())
                .contains("DELETE FROM " + MESSAGE_PARTS_TABLE)
                .contains("WHERE id = ?")
                .contains("IF EXISTS");
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
        String insertQuery = statements.getFirst().getQuery();
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
    void smsfMessageParts() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        String smsfMessageJson = """
                {   "text": "This is part 1 of a multi-part MO SMS from the SMSF pipeline",
                }
                """;
        when(row.getString("data")).thenReturn(smsfMessageJson);

        ResultSet insertResultSet = mock(ResultSet.class);
        ResultSet selectResultSet = mock(ResultSet.class);
        when(selectResultSet.one()).thenReturn(row);

        when(session.execute(any(SimpleStatement.class)))
                .thenReturn(insertResultSet)
                .thenReturn(selectResultSet);

        ScyllaManager manager = newManagerWithSession(session);
        String expectedKey = "imsi-460000203924736|242";
        int ttl = 30;
        manager.insertIntoSmsfMessageParts(expectedKey, smsfMessageJson, ttl);

        String retrievedData = manager.getSmsfMessagePartsByKey(expectedKey);
        assertEquals(smsfMessageJson, retrievedData);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(2)).execute(captor.capture());

        List<SimpleStatement> statements = captor.getAllValues();
        String insertQuery = statements.getFirst().getQuery();
        assertTrue(insertQuery.contains("smsf_message_parts"), "INSERT should use smsf_message_parts table");
        assertTrue(insertQuery.contains("timestamp"), "INSERT should include timestamp field");
        assertTrue(insertQuery.contains("currentTimestamp()"), "INSERT should use currentTimestamp() for timestamp");
        assertTrue(insertQuery.contains("USING TTL"), "INSERT should include TTL");
    }

    @Test
    void smsfMessagePartsVerifiesTtl() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        String messageData = """
                {
                    "text": "Concat SMS test message",
                }
                """;

        int ttl = 30;
        manager.insertIntoSmsfMessageParts("imsi-460000203924736|207", messageData, ttl);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String query = stmt.getQuery();

        assertTrue(query.contains("USING TTL"), "Query should include TTL");
        assertTrue(query.contains(String.valueOf(ttl)), "Query should include the TTL value");
        assertTrue(query.contains("smsf_message_parts"), "Query should use smsf_message_parts table");
        assertTrue(query.contains("timestamp"), "Query should include timestamp field");
        assertTrue(query.contains("currentTimestamp()"), "Query should use currentTimestamp() for timestamp");
    }

    @Test
    void getSmsfMessagePartsByKeyReturnsNullWhenNoRow() {
        CqlSession session = mock(CqlSession.class);
        ResultSet selectResultSet = mock(ResultSet.class);
        when(selectResultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(selectResultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.getSmsfMessagePartsByKey("missing-key"));

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());
        String query = captor.getValue().getQuery();
        assertTrue(query.contains("SELECT data FROM smsf_message_parts"),
                "Should select from smsf_message_parts table");
        assertTrue(query.contains("WHERE id = ?"), "Should filter by id");
    }

    @Test
    void deleteSmsfMessagePartsByKey() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        String key = "imsi-460000203924736|242";

        manager.deleteSmsfMessagePartsByKey(key);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String query = stmt.getQuery();

        assertTrue(query.contains("DELETE FROM smsf_message_parts"),
                "Should delete from smsf_message_parts table");
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
    void dndEntriesExistByParentIdWhenRowFound() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(mock(Row.class));
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        assertTrue(newManagerWithSession(session).dndEntriesExistByParentId(55));
    }

    @Test
    void dndEntriesExistByParentIdWhenNoRowFound() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        assertFalse(newManagerWithSession(session).dndEntriesExistByParentId(55));
    }

    @Test
    void dndEntriesExistByParentIdUsesLimitedFilteringQuery() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        newManagerWithSession(session).dndEntriesExistByParentId(77);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session).execute(captor.capture());
        SimpleStatement statement = captor.getValue();
        assertTrue(statement.getQuery().contains("FROM dnd_entries"));
        assertTrue(statement.getQuery().contains("WHERE parent_id = ?"));
        assertTrue(statement.getQuery().contains("LIMIT 1"));
        assertTrue(statement.getQuery().contains("ALLOW FILTERING"));
        assertEquals(77, statement.getPositionalValues().get(0));
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
    void createPlmnPrefixTableTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        manager.createPlmnPrefixTable();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(session).execute(captor.capture());
        assertThat(captor.getValue())
                .contains("CREATE TABLE IF NOT EXISTS " + ScyllaTablesConstants.PLMN_PREFIX_TABLE)
                .contains("prefix text PRIMARY KEY")
                .contains("mcc text")
                .contains("mnc text")
                .contains("country_code text")
                .contains("country_iso text");
    }

    @Test
    void createPlmnRangeTableTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        manager.createPlmnRangeTable();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(session).execute(captor.capture());
        assertThat(captor.getValue())
                .contains("CREATE TABLE IF NOT EXISTS " + ScyllaTablesConstants.PLMN_RANGE_TABLE)
                .contains("PRIMARY KEY ((lookup_bucket), sn_range_start)")
                .contains("CLUSTERING ORDER BY (sn_range_start DESC)");
    }

    @Test
    void isPlmnPrefixTableEmptyReturnsTrueWhenNoRowsExist() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(session.execute(anyString())).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(null);
        ScyllaManager manager = newManagerWithSession(session);

        assertTrue(manager.isPlmnPrefixTableEmpty());

        verify(session).execute("SELECT prefix FROM " + ScyllaTablesConstants.PLMN_PREFIX_TABLE + " LIMIT 1;");
    }

    @Test
    void isPlmnPrefixTableEmptyReturnsFalseWhenOneRowExists() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(session.execute(anyString())).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(mock(Row.class));
        ScyllaManager manager = newManagerWithSession(session);

        assertFalse(manager.isPlmnPrefixTableEmpty());
    }

    @Test
    void upsertPlmnPrefixTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);
        UtilsRecords.PlmnPrefix plmnPrefix = new UtilsRecords.PlmnPrefix(
                "50578", "710", "01", "Nicaragua", "505", "NIC");

        manager.upsertPlmnPrefix(plmnPrefix);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session).execute(captor.capture());
        assertThat(captor.getValue().getQuery())
                .contains("INSERT INTO " + ScyllaTablesConstants.PLMN_PREFIX_TABLE);
        assertThat(captor.getValue().getPositionalValues())
                .containsExactly("50578", "710", "01", "Nicaragua", "505", "NIC");
    }

    @Test
    void upsertPlmnPrefixStoresConfiguredRangeOnlyInRangeTable() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);
        UtilsRecords.PlmnPrefix plmnPrefix = new UtilsRecords.PlmnPrefix(
                "5055849", "710", "01", "Nicaragua", "505", "NIC",
                "50558490000", "50558499999");

        manager.upsertPlmnPrefix(plmnPrefix);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session).execute(captor.capture());
        SimpleStatement rangeInsert = captor.getValue();
        assertThat(rangeInsert.getQuery()).contains("INSERT INTO " + ScyllaTablesConstants.PLMN_RANGE_TABLE);
        assertThat(rangeInsert.getQuery()).doesNotContain("INSERT INTO " + ScyllaTablesConstants.PLMN_PREFIX_TABLE);
        assertThat(rangeInsert.getPositionalValues()).containsExactly(
                "11:5", 50558490000L, 50558499999L, "5055849", "710", "01",
                "Nicaragua", "505", "NIC");
    }

    @Test
    void deletePlmnPrefixTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        manager.deletePlmnPrefix("50578");

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session).execute(captor.capture());
        assertThat(captor.getValue().getQuery())
                .contains("DELETE FROM " + ScyllaTablesConstants.PLMN_PREFIX_TABLE)
                .contains("WHERE prefix = ?");
        assertThat(captor.getValue().getPositionalValues()).containsExactly("50578");
    }

    @Test
    void getPlmnByPrefixTest() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);
        when(row.getString("prefix")).thenReturn("50578");
        when(row.getString("mcc")).thenReturn("710");
        when(row.getString("mnc")).thenReturn("01");
        when(row.getString("country")).thenReturn("Nicaragua");
        when(row.getString("country_code")).thenReturn("505");
        when(row.getString("country_iso")).thenReturn("NIC");
        when(resultSet.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        ScyllaManager manager = newManagerWithSession(session);

        UtilsRecords.PlmnPrefix result = manager.getPlmnByPrefix("50578");

        assertNotNull(result);
        assertEquals("50578", result.prefix());
        assertEquals("710", result.mcc());
        assertEquals("01", result.mnc());
        assertEquals("Nicaragua", result.country());
    }

    @Test
    void findPlmnByMsisdnUsesLongestPrefixMatch() {
        CqlSession session = mock(CqlSession.class);
        ResultSet miss = mock(ResultSet.class);
        ResultSet hit = mock(ResultSet.class);
        Row row = mock(Row.class);
        when(miss.one()).thenReturn(null);
        when(hit.one()).thenReturn(row);
        when(row.getString("prefix")).thenReturn("50578");
        when(row.getString("mcc")).thenReturn("710");
        when(row.getString("mnc")).thenReturn("01");
        when(row.getString("country")).thenReturn("Nicaragua");
        when(row.getString("country_code")).thenReturn("505");
        when(row.getString("country_iso")).thenReturn("NIC");
        when(session.execute(any(SimpleStatement.class))).thenReturn(miss, miss, miss, hit);
        ScyllaManager manager = newManagerWithSession(session);

        UtilsRecords.PlmnPrefix result = manager.findPlmnByMsisdn("+505-7890");

        assertNotNull(result);
        assertEquals("50578", result.prefix());
        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(4)).execute(captor.capture());
        assertThat(captor.getAllValues().stream().skip(1)
                .map(statement -> statement.getPositionalValues().getFirst())
                .toList())
                .containsExactly("5057890", "505789", "50578");
    }

    @Test
    void findPlmnByMsisdnFallsBackFromRangeToLongestPrefix() {
        CqlSession session = mock(CqlSession.class);
        ResultSet miss = mock(ResultSet.class);
        ResultSet hit = mock(ResultSet.class);
        Row row = mock(Row.class);
        when(miss.one()).thenReturn(null);
        when(hit.one()).thenReturn(row);
        when(row.getString("prefix")).thenReturn("50578901234");
        when(row.getString("mcc")).thenReturn("710");
        when(row.getString("mnc")).thenReturn("01");
        when(row.getString("country")).thenReturn("Nicaragua");
        when(row.getString("country_code")).thenReturn("505");
        when(row.getString("country_iso")).thenReturn("NIC");
        when(session.execute(any(SimpleStatement.class))).thenReturn(miss, hit);
        ScyllaManager manager = newManagerWithSession(session);

        UtilsRecords.PlmnPrefix result = manager.findPlmnByMsisdn("50578901234");

        assertNotNull(result);
        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(2)).execute(captor.capture());
        assertThat(captor.getAllValues().get(1).getPositionalValues()).containsExactly("50578901234");
    }

    @Test
    void findPlmnByMsisdnReturnsRangeMatchWithSingleQuery() {
        CqlSession session = mock(CqlSession.class);
        ResultSet hit = mock(ResultSet.class);
        Row row = mock(Row.class);
        when(hit.one()).thenReturn(row);
        when(row.getString("prefix")).thenReturn("5055849");
        when(row.getString("mcc")).thenReturn("710");
        when(row.getString("mnc")).thenReturn("01");
        when(row.getString("country")).thenReturn("Nicaragua");
        when(row.getString("country_code")).thenReturn("505");
        when(row.getString("country_iso")).thenReturn("NIC");
        when(row.getLong("sn_range_start")).thenReturn(50558490000L);
        when(row.getLong("sn_range_stop")).thenReturn(50558499999L);
        when(session.execute(any(SimpleStatement.class))).thenReturn(hit);
        ScyllaManager manager = newManagerWithSession(session);

        UtilsRecords.PlmnPrefix result = manager.findPlmnByMsisdn("50558491234");

        assertNotNull(result);
        assertEquals("5055849", result.prefix());
        assertEquals("50558490000", result.snRangeStart());
        assertEquals("50558499999", result.snRangeStop());
        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session).execute(captor.capture());
        assertThat(captor.getValue().getPositionalValues()).containsExactly("11:5", 50558491234L);
    }

    @Test
    void findPlmnByMsisdnFallsBackWhenClosestRangeDoesNotContainNumber() {
        CqlSession session = mock(CqlSession.class);
        ResultSet rangeResult = mock(ResultSet.class);
        ResultSet prefixResult = mock(ResultSet.class);
        Row rangeRow = mock(Row.class);
        Row prefixRow = mock(Row.class);
        when(rangeResult.one()).thenReturn(rangeRow);
        when(prefixResult.one()).thenReturn(prefixRow);
        when(rangeRow.getLong("sn_range_stop")).thenReturn(50558499999L);
        when(prefixRow.getString("prefix")).thenReturn("50558501234");
        when(prefixRow.getString("mcc")).thenReturn("710");
        when(prefixRow.getString("mnc")).thenReturn("01");
        when(session.execute(any(SimpleStatement.class))).thenReturn(rangeResult, prefixResult);
        ScyllaManager manager = newManagerWithSession(session);

        UtilsRecords.PlmnPrefix result = manager.findPlmnByMsisdn("50558501234");

        assertNotNull(result);
        assertEquals("50558501234", result.prefix());
        verify(session, times(2)).execute(any(SimpleStatement.class));
    }

    @Test
    void findPlmnByMsisdnSkipsInvalidValue() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.findPlmnByMsisdn("505ABC"));
        verify(session, times(0)).execute(any(SimpleStatement.class));
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


    @Test
    void isJtiBlacklistedReturnsTrueWhenRowPresent() {
        CqlSession session = mock(CqlSession.class);
        com.datastax.oss.driver.api.core.cql.PreparedStatement ps = mock(com.datastax.oss.driver.api.core.cql.PreparedStatement.class);
        com.datastax.oss.driver.api.core.cql.BoundStatement bs = mock(com.datastax.oss.driver.api.core.cql.BoundStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(session.prepare(any(String.class))).thenReturn(ps);
        when(ps.bind("jti-123")).thenReturn(bs);
        when(rs.one()).thenReturn(mock(Row.class));
        when(session.execute(bs)).thenReturn(rs);

        ScyllaManager manager = newManagerWithSession(session);
        assertTrue(manager.isJtiBlacklisted("jti-123"));
    }

    @Test
    void isJtiBlacklistedReturnsFalseWhenNoRow() {
        CqlSession session = mock(CqlSession.class);
        com.datastax.oss.driver.api.core.cql.PreparedStatement ps = mock(com.datastax.oss.driver.api.core.cql.PreparedStatement.class);
        com.datastax.oss.driver.api.core.cql.BoundStatement bs = mock(com.datastax.oss.driver.api.core.cql.BoundStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(session.prepare(any(String.class))).thenReturn(ps);
        when(ps.bind("jti-123")).thenReturn(bs);
        when(rs.one()).thenReturn(null);
        when(session.execute(bs)).thenReturn(rs);

        ScyllaManager manager = newManagerWithSession(session);
        assertFalse(manager.isJtiBlacklisted("jti-123"));
    }

    @Test
    void isJtiBlacklistedReturnsFalseOnException() {
        CqlSession session = mock(CqlSession.class);
        when(session.prepare(any(String.class))).thenThrow(new RuntimeException("DB down"));

        ScyllaManager manager = newManagerWithSession(session);
        assertFalse(manager.isJtiBlacklisted("jti-123"));
    }

    @Test
    void createBearerTokenBlacklistTableTest() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);
        manager.createBearerTokenBlacklistTable();
        verify(session, times(1)).execute(any(String.class));
    }

    @Test
    void insertIntoBearerTokenBlacklistTest() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));
        ScyllaManager manager = newManagerWithSession(session);
        manager.insertIntoBearerTokenBlacklist("jti-test-123", 3600);
        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());
        SimpleStatement stmt = captor.getValue();
        String cql = stmt.getQuery();
        assertThat(cql).contains("bearer_token_blacklist").contains("USING TTL");
        List<Object> values = stmt.getPositionalValues();
        assertThat(values).containsExactly("jti-test-123", 3600);
    }

    @Test
    void upsertSipRegistrationStateWithTtlSendsInsertUsingTtlWithExpectedValues() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.upsertSipRegistrationStateWithTtl("50588888888", "sip:user@ims.local", "123456789012345", "INITIAL_REGISTRATION", "PENDING", "gsm-scf.ims.local", true, 3600, 3900);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        String cql = stmt.getQuery();

        assertThat(cql).contains("INSERT INTO " + SIP_REGISTRATION_STATE_TABLE).contains("USING TTL ?").contains("updated_at");
        assertThat(stmt.getPositionalValues()).containsExactly("50588888888", "sip:user@ims.local", "123456789012345", "INITIAL_REGISTRATION", "PENDING", "gsm-scf.ims.local", true, 3600, 3900);
    }

    @Test
    void upsertSipRegistrationStateWithTtlShouldSkipWhenMsisdnIsBlank() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        manager.upsertSipRegistrationStateWithTtl(" ", "sip:user@ims.local", "123456789012345", "INITIAL_REGISTRATION", "PENDING", "gsm-scf.ims.local", true, 3600, 3900);

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void upsertSipRegistrationStateWithTtlShouldSkipWhenMsisdnIsNull() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        manager.upsertSipRegistrationStateWithTtl(null, "sip:user@ims.local", "123456789012345", "INITIAL_REGISTRATION", "PENDING", "gsm-scf.ims.local", true, 3600, 3900);

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void upsertSipRegistrationStateWithTtlShouldThrowExceptionWhenTtlIsInvalid() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        assertThrows(IllegalArgumentException.class, () -> manager.upsertSipRegistrationStateWithTtl("50588888888", "sip:user@ims.local", "123456789012345", "INITIAL_REGISTRATION", "PENDING", "gsm-scf.ims.local", true, 3600, 0));

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void promoteSipRegistrationStateToActiveShouldSkipWhenMsisdnIsBlank() {
        CqlSession session = mock(CqlSession.class);

        ScyllaManager manager = newManagerWithSession(session);

        boolean promoted = manager.promoteSipRegistrationStateToActive("   ", "sip:new-user@ims.local");

        assertFalse(promoted);
        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void promoteSipRegistrationStateToActiveShouldSkipWhenMsisdnIsNull() {
        CqlSession session = mock(CqlSession.class);

        ScyllaManager manager = newManagerWithSession(session);

        boolean promoted = manager.promoteSipRegistrationStateToActive(null, "sip:new-user@ims.local");

        assertFalse(promoted);
        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void promoteSipRegistrationStateToActiveShouldReturnFalseWhenRowDoesNotExist() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        boolean promoted = manager.promoteSipRegistrationStateToActive("50586604983", "sip:new-user@ims.local");

        assertFalse(promoted);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement selectStatement = captor.getValue();

        assertThat(selectStatement.getQuery())
                .contains("SELECT msisdn, aor, expires, gsm_scf_address, imsi, reg_event_state, registration_type, support_sbi")
                .contains("FROM " + SIP_REGISTRATION_STATE_TABLE)
                .contains("WHERE msisdn = ?");

        assertThat(selectStatement.getPositionalValues())
                .containsExactly("50586604983");
    }

    @Test
    void promoteSipRegistrationStateToActiveShouldInsertPermanentActiveRecordUsingNotifyAor() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("msisdn")).thenReturn("50586604983");
        when(row.getString("imsi")).thenReturn("748010123456789");
        when(row.getString("registration_type")).thenReturn("INITIAL_REGISTRATION");
        when(row.getString("gsm_scf_address")).thenReturn("505880000011");
        when(row.isNull("support_sbi")).thenReturn(false);
        when(row.getBoolean("support_sbi")).thenReturn(true);
        when(row.isNull("expires")).thenReturn(false);
        when(row.getInt("expires")).thenReturn(240);

        when(resultSet.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        boolean promoted = manager.promoteSipRegistrationStateToActive("50586604983", "sip:new-user@ims.local");

        assertTrue(promoted);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(2)).execute(captor.capture());

        List<SimpleStatement> statements = captor.getAllValues();

        SimpleStatement selectStatement = statements.get(0);
        assertThat(selectStatement.getQuery())
                .contains("SELECT msisdn, aor, expires, gsm_scf_address, imsi, reg_event_state, registration_type, support_sbi")
                .contains("FROM " + SIP_REGISTRATION_STATE_TABLE)
                .contains("WHERE msisdn = ?");
        assertThat(selectStatement.getPositionalValues())
                .containsExactly("50586604983");

        SimpleStatement insertStatement = statements.get(1);
        assertThat(insertStatement.getQuery())
                .contains("INSERT INTO " + SIP_REGISTRATION_STATE_TABLE)
                .contains("reg_event_state")
                .doesNotContain("USING TTL");

        assertThat(insertStatement.getPositionalValues())
                .containsExactly(
                        "50586604983",
                        "sip:new-user@ims.local",
                        "748010123456789",
                        "INITIAL_REGISTRATION",
                        RegEventState.ACTIVE.name(),
                        "505880000011",
                        true,
                        240
                );
    }

    @Test
    void promoteSipRegistrationStateToActiveShouldKeepExistingAorWhenNotifyAorIsBlank() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("msisdn")).thenReturn("50586604983");
        when(row.getString("aor")).thenReturn("sip:existing-user@ims.local");
        when(row.getString("imsi")).thenReturn("748010123456789");
        when(row.getString("registration_type")).thenReturn("RE_REGISTRATION");
        when(row.getString("gsm_scf_address")).thenReturn("505880000011");
        when(row.isNull("support_sbi")).thenReturn(false);
        when(row.getBoolean("support_sbi")).thenReturn(false);
        when(row.isNull("expires")).thenReturn(false);
        when(row.getInt("expires")).thenReturn(3600);

        when(resultSet.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        boolean promoted = manager.promoteSipRegistrationStateToActive("50586604983", "   ");

        assertTrue(promoted);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(2)).execute(captor.capture());

        SimpleStatement insertStatement = captor.getAllValues().get(1);

        assertThat(insertStatement.getQuery())
                .contains("INSERT INTO " + SIP_REGISTRATION_STATE_TABLE)
                .doesNotContain("USING TTL");

        assertThat(insertStatement.getPositionalValues())
                .containsExactly(
                        "50586604983",
                        "sip:existing-user@ims.local",
                        "748010123456789",
                        "RE_REGISTRATION",
                        RegEventState.ACTIVE.name(),
                        "505880000011",
                        false,
                        3600
                );
    }

    @Test
    void promoteSipRegistrationStateToActiveShouldUseSafeDefaultsWhenNullableColumnsAreNull() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("msisdn")).thenReturn("50586604983");
        when(row.getString("aor")).thenReturn("sip:existing-user@ims.local");
        when(row.getString("imsi")).thenReturn(null);
        when(row.getString("registration_type")).thenReturn("INITIAL_REGISTRATION");
        when(row.getString("gsm_scf_address")).thenReturn("505880000011");
        when(row.isNull("support_sbi")).thenReturn(true);
        when(row.isNull("expires")).thenReturn(true);

        when(resultSet.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        boolean promoted = manager.promoteSipRegistrationStateToActive("50586604983", null);

        assertTrue(promoted);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(2)).execute(captor.capture());

        SimpleStatement insertStatement = captor.getAllValues().get(1);

        assertThat(insertStatement.getQuery())
                .contains("INSERT INTO " + SIP_REGISTRATION_STATE_TABLE)
                .doesNotContain("USING TTL");

        assertThat(insertStatement.getPositionalValues())
                .containsExactly(
                        "50586604983",
                        "sip:existing-user@ims.local",
                        null,
                        "INITIAL_REGISTRATION",
                        RegEventState.ACTIVE.name(),
                        "505880000011",
                        false,
                        0
                );
    }
    @Test
    void createSmsfUeSmsContextTablesTest() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        manager.createSmsfUeSmsContextTables();

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(3)).execute(captor.capture());

        List<SimpleStatement> statements = captor.getAllValues();

        String createSubmitResultTableCql = statements.get(0).getQuery();
        assertThat(createSubmitResultTableCql).contains("CREATE TABLE IF NOT EXISTS")
                .contains(SMSF_SUBMIT_SM_RESULT_TABLE)
                .contains("id TEXT PRIMARY KEY")
                .contains("data TEXT");

        String createTableCql = statements.get(1).getQuery();
        assertThat(createTableCql).contains("CREATE TABLE IF NOT EXISTS")
                .contains(SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("supi text PRIMARY KEY")
                .contains("gpsi text")
                .contains("data text")
                .contains("reg_event_state text")
                .contains("updated_at timestamp");

        String createIndexCql = statements.get(2).getQuery();
        assertThat(createIndexCql).contains("CREATE INDEX IF NOT EXISTS")
                .contains("smsf_ue_sms_context_gpsi_idx")
                .contains("ON " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("(gpsi)");
    }

    @Test
    void upsertSmsfUeSmsContextSendsExpectedStatement() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);
        UeSmsContextData ueSmsContextData = getUeSmsContextData();

        manager.upsertSmsfUeSmsContext(ueSmsContextData);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();

        assertThat(stmt.getQuery())
                .contains("INSERT INTO " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("supi")
                .contains("gpsi")
                .contains("data")
                .contains("reg_event_state")
                .contains("updated_at")
                .contains("toTimestamp(now())");

        List<Object> values = stmt.getPositionalValues();

        assertThat(values).hasSize(4);
        assertThat(values.get(0)).isEqualTo("imsi-460000000000001");
        assertThat(values.get(1)).isEqualTo("msisdn-8613915900000");
        assertThat(values.get(3)).isEqualTo(RegEventState.ACTIVE.name());
        assertThat(values.get(2).toString())
                .contains("imsi-460000000000001")
                .contains("msisdn-8613915900000")
                .contains("imei-123456789012345")
                .contains("3GPP_ACCESS")
                .contains("NR");
    }

    @Test
    void updateSmsfRegEventStateMarksTheSubscriberWithoutAnyExpiry() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.updateSmsfRegEventState("imsi-460000000000001", RegEventState.INACTIVE.name());

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();

        // The mark has no lifetime of its own: it lasts until a new subscription arrives.
        assertThat(stmt.getQuery())
                .contains("UPDATE " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("reg_event_state = ?")
                .doesNotContain("USING TTL")
                .doesNotContain("expires");

        List<Object> values = stmt.getPositionalValues();
        assertThat(values).hasSize(2);
        assertThat(values.get(0)).isEqualTo(RegEventState.INACTIVE.name());
        assertThat(values.get(1)).isEqualTo("imsi-460000000000001");
    }

    @Test
    void upsertSmsfUeSmsContextAllowsNullOptionalFields() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        UeSmsContextData ueSmsContextData = new UeSmsContextData().supi("imsi-460000000000001");

        manager.upsertSmsfUeSmsContext(ueSmsContextData);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        List<Object> values = stmt.getPositionalValues();

        assertThat(stmt.getQuery())
                .contains("INSERT INTO " + SMSF_UE_SMS_CONTEXT_TABLE);

        assertThat(values).hasSize(4);
        assertThat(values.get(0)).isEqualTo("imsi-460000000000001");
        assertThat(values.get(1)).isNull();
        assertThat(values.get(2).toString()).contains("imsi-460000000000001");
        assertThat(values.get(3)).isEqualTo(RegEventState.ACTIVE.name());
    }

    @Test
    void upsertSmsfUeSmsContextThrowsWhenDataIsNull() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> manager.upsertSmsfUeSmsContext(null)
        );

        assertEquals("UE SMS context data is required", exception.getMessage());
        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void upsertSmsfUeSmsContextThrowsWhenSupiIsBlank() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        UeSmsContextData ueSmsContextData = new UeSmsContextData()
                .supi("")
                .gpsi("msisdn-8613915900000");

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> manager.upsertSmsfUeSmsContext(ueSmsContextData)
        );

        assertEquals("supi is required", exception.getMessage());
        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void getSmsfUeSmsContextBySupiReturnsData() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("data")).thenReturn(getUeSmsContextDataJson());
        when(resultSet.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        UeSmsContextData data = manager.getSmsfUeSmsContextBySupi("imsi-460000000000001");

        assertThat(data).isNotNull();
        assertThat(data.getSupi()).isEqualTo("imsi-460000000000001");
        assertThat(data.getGpsi()).isEqualTo("msisdn-8613915900000");
        assertThat(MsisdnUtils.extract(data.getGpsi())).isEqualTo("8613915900000");
        assertThat(data.getPei()).isEqualTo("imei-123456789012345");
        assertThat(data.getAccessType()).isEqualTo(AccessType._3_GPP_ACCESS);
        assertThat(data.getRatType()).isEqualTo(RatType.NR);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        assertThat(stmt.getQuery())
                .contains("SELECT data")
                .contains("FROM " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("WHERE supi = ?");
        assertThat(stmt.getPositionalValues()).containsExactly("imsi-460000000000001");
    }

    @Test
    void getSmsfUeSmsContextBySupiReturnsNullWhenNoRowFound() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.getSmsfUeSmsContextBySupi("imsi-460000000000001"));
    }

    @Test
    void getSmsfUeSmsContextBySupiReturnsNullWhenSupiIsBlank() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.getSmsfUeSmsContextBySupi(""));

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void getSmsfUeSmsContextSupiByGpsiReturnsSupi() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("supi")).thenReturn("imsi-460000000000001");
        when(resultSet.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        String supi = manager.getSmsfUeSmsContextSupiByGpsi("8613915900000");

        assertEquals("imsi-460000000000001", supi);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        assertThat(stmt.getQuery())
                .contains("SELECT supi")
                .contains("FROM " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("WHERE gpsi = ?");
        assertThat(stmt.getPositionalValues()).containsExactly("msisdn-8613915900000");
    }

    @Test
    void getSmsfUeSmsContextSupiByGpsiReturnsNullWhenNoRowFound() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.getSmsfUeSmsContextSupiByGpsi("8613915900000"));
    }

    @Test
    void getSmsfUeSmsContextSupiByGpsiReturnsNullWhenGpsiIsBlank() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.getSmsfUeSmsContextSupiByGpsi(""));

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void getSmsfUeSmsContextGpsiBySupiReturnsGpsi() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        when(row.getString("gpsi")).thenReturn("8613915900000");
        when(resultSet.one()).thenReturn(row);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        String gpsi = manager.getSmsfUeSmsContextGpsiBySupi("imsi-460000000000001");

        assertEquals("8613915900000", gpsi);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        assertThat(stmt.getQuery())
                .contains("SELECT gpsi")
                .contains("FROM " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("WHERE supi = ?");
        assertThat(stmt.getPositionalValues()).containsExactly("imsi-460000000000001");
    }

    @Test
    void getSmsfUeSmsContextGpsiBySupiReturnsNullWhenNoRowFound() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.getSmsfUeSmsContextGpsiBySupi("imsi-460000000000001"));
    }

    @Test
    void getSmsfUeSmsContextGpsiBySupiReturnsNullWhenSupiIsBlank() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        assertNull(manager.getSmsfUeSmsContextGpsiBySupi(""));

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void existsSmsfUeSmsContextBySupiWhenRowPresentReturnsTrue() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(mock(Row.class));
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertTrue(manager.existsSmsfUeSmsContextBySupi("imsi-460000000000001"));

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        assertThat(stmt.getQuery())
                .contains("SELECT supi")
                .contains("FROM " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("WHERE supi = ?");
        assertThat(stmt.getPositionalValues()).containsExactly("imsi-460000000000001");
    }

    @Test
    void existsSmsfUeSmsContextBySupiWhenNoRowFoundReturnsFalse() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertFalse(manager.existsSmsfUeSmsContextBySupi("imsi-460000000000001"));
    }

    @Test
    void existsSmsfUeSmsContextBySupiWhenSupiIsBlankReturnsFalse() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        assertFalse(manager.existsSmsfUeSmsContextBySupi(""));

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void existsSmsfUeSmsContextByGpsiWhenRowPresentReturnsTrue() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(mock(Row.class));
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertTrue(manager.existsSmsfUeSmsContextByGpsi("8613915900000"));

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();
        assertThat(stmt.getQuery())
                .contains("SELECT gpsi")
                .contains("FROM " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("WHERE gpsi = ?");
        assertThat(stmt.getPositionalValues()).containsExactly("8613915900000");
    }

    @Test
    void existsSmsfUeSmsContextByGpsiWhenNoRowFoundReturnsFalse() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.one()).thenReturn(null);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        ScyllaManager manager = newManagerWithSession(session);

        assertFalse(manager.existsSmsfUeSmsContextByGpsi("8613915900000"));
    }

    @Test
    void existsSmsfUeSmsContextByGpsiWhenGpsiIsBlankReturnsFalse() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        assertFalse(manager.existsSmsfUeSmsContextByGpsi(""));

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void deleteSmsfUeSmsContextBySupiSendsDeleteWithExpectedValue() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.deleteSmsfUeSmsContextBySupi("imsi-460000000000001");

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();

        assertThat(stmt.getQuery())
                .contains("DELETE FROM " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("WHERE supi = ?");
        assertThat(stmt.getPositionalValues()).containsExactly("imsi-460000000000001");
    }

    @Test
    void deleteSmsfUeSmsContextBySupiDoesNothingWhenSupiIsBlank() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        manager.deleteSmsfUeSmsContextBySupi("");

        verify(session, times(0)).execute(any(SimpleStatement.class));
    }

    @Test
    void updateSmsfRegEventStateIgnoresBlankArguments() {
        CqlSession session = mock(CqlSession.class);
        ScyllaManager manager = newManagerWithSession(session);

        manager.updateSmsfRegEventState("", RegEventState.INACTIVE.name());
        manager.updateSmsfRegEventState(null, RegEventState.INACTIVE.name());
        manager.updateSmsfRegEventState("imsi-460000000000001", null);
        manager.updateSmsfRegEventState("imsi-460000000000001", "");

        verify(session, never()).execute(any(SimpleStatement.class));
    }

    @Test
    void updateSmsfRegEventStateWritesActiveWithoutTtl() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.updateSmsfRegEventState("imsi-460000000000001", RegEventState.ACTIVE.name());

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();

        assertThat(stmt.getQuery())
                .contains("UPDATE " + SMSF_UE_SMS_CONTEXT_TABLE)
                .contains("SET reg_event_state")
                .doesNotContain("USING TTL");

        assertThat(stmt.getPositionalValues())
                .containsExactly(RegEventState.ACTIVE.name(), "imsi-460000000000001");
    }


    @Test
    void getActiveSmsfUeSmsContextByGpsiReadsTheContextAndTheStateInOneQuery() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getString("reg_event_state")).thenReturn(null);
        when(row.getString("data")).thenReturn(Converter.valueAsString(getUeSmsContextData()));

        ScyllaManager manager = newManagerWithSession(session);

        assertThat(manager.getActiveSmsfUeSmsContextByGpsi("50550001234")).isNotNull();

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        assertThat(captor.getValue().getQuery())
                .contains("SELECT data, reg_event_state")
                .contains("WHERE gpsi = ?");
    }

    @Test
    void getActiveSmsfUeSmsContextByGpsiHonoursAnExplicitActiveState() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getString("reg_event_state")).thenReturn(RegEventState.ACTIVE.name());
        when(row.getString("data")).thenReturn(Converter.valueAsString(getUeSmsContextData()));

        ScyllaManager manager = newManagerWithSession(session);

        assertThat(manager.getActiveSmsfUeSmsContextByGpsi("50550001234")).isNotNull();
    }

    @Test
    void getActiveSmsfUeSmsContextByGpsiReturnsNullWhenInactive() {
        CqlSession session = mock(CqlSession.class);
        Row row = mock(Row.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getString("reg_event_state")).thenReturn(RegEventState.INACTIVE.name());

        ScyllaManager manager = newManagerWithSession(session);

        assertThat(manager.getActiveSmsfUeSmsContextByGpsi("50550001234")).isNull();
    }

    @Test
    void getActiveSmsfUeSmsContextByGpsiReturnsNullWhenThereIsNoRow() {
        CqlSession session = mock(CqlSession.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(null);

        ScyllaManager manager = newManagerWithSession(session);

        assertThat(manager.getActiveSmsfUeSmsContextByGpsi("50550001234")).isNull();
        assertThat(manager.getActiveSmsfUeSmsContextByGpsi("")).isNull();
    }


    @Test
    void insertIntoTableAppliesTheTtlWhenGiven() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.insertIntoTable(SMSF_SUBMIT_SM_RESULT_TABLE, "id-1", "{}", 300);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        SimpleStatement stmt = captor.getValue();

        assertThat(stmt.getQuery())
                .contains("INSERT INTO " + SMSF_SUBMIT_SM_RESULT_TABLE)
                .contains("USING TTL 300");
        assertThat(stmt.getPositionalValues()).containsExactly("id-1", "{}");
    }

    @Test
    void insertIntoTableFallsBackToNoTtlWhenNotPositive() {
        CqlSession session = mock(CqlSession.class);
        when(session.execute(any(SimpleStatement.class))).thenReturn(mock(ResultSet.class));

        ScyllaManager manager = newManagerWithSession(session);

        manager.insertIntoTable(SMSF_SUBMIT_SM_RESULT_TABLE, "id-1", "{}", 0);

        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, times(1)).execute(captor.capture());

        assertThat(captor.getValue().getQuery()).doesNotContain("USING TTL");
    }
}
