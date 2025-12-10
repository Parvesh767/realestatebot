package com.risingbee.realestate.automation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDbRunner implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            log.info("🌟 DB Connected Successfully → {}", conn.getMetaData().getURL());
        } catch (Exception e) {
            log.error("❌ DB Connection Failed", e);
        }
    }
}
