package com.collab.persistenceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PersistenceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersistenceServiceApplication.class, args);
	}

}
