package com.ganesh.EV_Project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Demo {

    @GetMapping("/")
    public String test(){
        return "Hello World";
    }
}
