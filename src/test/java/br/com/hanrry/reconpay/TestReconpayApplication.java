package br.com.hanrry.reconpay;

import org.springframework.boot.SpringApplication;

public class TestReconpayApplication {

	public static void main(String[] args) {
		SpringApplication.from(StarterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
