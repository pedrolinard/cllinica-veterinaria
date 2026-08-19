package com.vetclinic.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VetClinicApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VetClinicApiApplication.class, args);
    }
}
