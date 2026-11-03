package com.omnitruth.tenant.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TenantManagementForOmniTruthAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(TenantManagementForOmniTruthAppApplication.class, args);
	}

}
