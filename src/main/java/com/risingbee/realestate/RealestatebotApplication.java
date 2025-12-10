package com.risingbee.realestate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;



@ComponentScan(basePackages = {
        "com.risingbee.realestate",        // your core package
        "com.risingbee.realestate.automation" // automation + all subpackages
})
@ComponentScan("com.risingbee.realestate")
@SpringBootApplication
public class RealestatebotApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealestatebotApplication.class, args);
	}

}
