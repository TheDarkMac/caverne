package com.devikapps.caverne;

import org.springframework.boot.SpringApplication;

public class TestCaverneApplication {

	public static void main(String[] args) {
		SpringApplication.from(CaverneApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
