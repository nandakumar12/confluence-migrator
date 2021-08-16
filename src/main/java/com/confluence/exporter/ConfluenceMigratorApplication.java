package com.confluence.exporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
@ComponentScan("com.confluence")
public class ConfluenceMigratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfluenceMigratorApplication.class, args);

	}

}
