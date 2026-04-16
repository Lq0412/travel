package com.lq.travel.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSchemaInitializerTest {

    private ProductSchemaInitializer initializer;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        initializer = new ProductSchemaInitializer();
        jdbcTemplate = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(initializer, "jdbcTemplate", jdbcTemplate);
        doNothing().when(jdbcTemplate).execute(anyString());
    }

    @Test
    void runShouldAvoidMysqlUnsupportedIfNotExistsAlterSyntax() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString()))
                .thenReturn(1);

        initializer.run();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        List<String> executedSql = sqlCaptor.getAllValues();
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("CREATE TABLE IF NOT EXISTS `product`")));
        assertFalse(executedSql.stream().anyMatch(sql -> sql.contains("ADD COLUMN IF NOT EXISTS")));
    }

    @Test
    void runShouldBackfillMissingColumnsUsingPlainAlterTable() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), eq("address"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), eq("latitude"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), eq("longitude"))).thenReturn(0);

        initializer.run();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        List<String> executedSql = sqlCaptor.getAllValues();
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("ADD COLUMN latitude decimal(10,7)")));
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("ADD COLUMN longitude decimal(10,7)")));
        assertFalse(executedSql.stream().anyMatch(sql -> sql.contains("ADD COLUMN address")));
    }
}
