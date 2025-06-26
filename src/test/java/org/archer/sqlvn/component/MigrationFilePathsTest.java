package org.archer.sqlvn.component;

import java.io.IOException;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class MigrationFilePathsTest {

	@Autowired
	MigrationFilePaths MigrationFilePaths;
	@Test
	public void test() throws IOException {
		MigrationFilePaths.printAllMigrationFilePaths();
	}

}
