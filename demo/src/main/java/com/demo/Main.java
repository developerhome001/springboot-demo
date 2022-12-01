package com.demo;


import com.keray.common.annotation.ApiResult;
import com.keray.common.file.OssPlugins;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.FileNotFoundException;

@SpringBootApplication
@RestController
public class Main {

    public static void main(String[] args) {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "200");
        SpringApplication.run(Main.class, args);
    }


    @ApiResult
    @GetMapping("/check-health")
    public Object checkHealth() {
        return "ok";
    }

}