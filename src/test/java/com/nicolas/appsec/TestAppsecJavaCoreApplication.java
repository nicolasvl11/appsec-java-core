package com.nicolas.appsec;

import org.springframework.boot.SpringApplication;

public class TestAppsecJavaCoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(AppsecJavaCoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
