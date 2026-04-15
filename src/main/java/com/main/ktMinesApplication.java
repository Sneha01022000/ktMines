package com.main;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@ComponentScan({"com.molcon"})
@Slf4j
@EnableAsync
@OpenAPIDefinition(
		info = @Info(
				title = "KTMINES-APIs",
				version = "1.0.0",
				description = "These APIs provides endpoints for KTMINES-APIs."

		)
)
public class ktMinesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ktMinesApplication.class, args);
	}

}
