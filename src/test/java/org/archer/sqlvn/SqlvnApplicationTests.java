package org.archer.sqlvn;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
class SqlvnApplicationTests {

	@Autowired
	private ConfigurableApplicationContext ctx;
	
	@Test
	void contextLoads() {
        
        Flyway flyway = ctx.getBean(Flyway.class);
        Configuration configuration = flyway.getConfiguration();
        
        log.debug("123456");
        log.debug(configuration.getBaselineDescription());
	}

}
