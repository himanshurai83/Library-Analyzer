package com.example.demo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/student")
    public Student hello() {
        return new Student("Himashu",20);
    }
    @GetMapping("/info")
    public String info() {
        return "Response From the Backend";
    }
}
