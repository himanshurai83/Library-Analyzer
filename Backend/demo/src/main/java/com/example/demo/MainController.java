package com.example.demo;

import org.springframework.web.bind.annotation.*;

@RestController
public class MainController {
    @GetMapping("/info")
    public String getInfo() {
        return "Response From the Backend";
    }

    @PostMapping("/info")
    public String postInfo(){
        return "Returning Response From /info using POST method";
    }

    @PutMapping("/info")
    public String putInfo(){return "Putting /info";}

    @DeleteMapping("/info")
    public String deleteInfo(){return "Deleting /info";}

    @GetMapping("/student")
    public Student getStudent() {
        return new Student("Himashu",20);
    }

    @PostMapping("/student")
    public String postStudent(){
        return "Returning Response from /student using POST method";
    }

    @PutMapping("/student")
    public String putStudent(){
        return "Putting /student";
    }

    @DeleteMapping("/student")
    public String deleteStudent(){
        return "Deleting /student";
    }

    @GetMapping("/students")
    public String getStudents(){return "Returning multiple Students";}

    @PostMapping("/students")
    public String postStudents(){return "Posting multiple Students";}

    @PutMapping("/students")
    public String putStudents(){return "Putting multiple Students";}

    @DeleteMapping("/students")
    public String deleteStudents(){return "Deleting multiple Students";}

    @GetMapping("/students/{id}")
    public String getStudentById(@PathVariable int id){return "Returning Student with ID: " + id;}

    @PostMapping("/students/{id}")
    public String postStudentById(@PathVariable int id){return "Posting Student with ID: " + id;}

    @PutMapping("/students/{id}")
    public String putStudentById(@PathVariable int id){return "Puttting Student with ID: " + id;}

    @DeleteMapping("/students/{id}")
    public String deleteStudentById(@PathVariable int id){return "Deleting Student with ID: " + id;}

    @GetMapping("/users")
    public String getUsers(){return "Getting users Detail";}

    @PostMapping("/users")
    public String postUsers(){return "Posting User Detail";}

    @PutMapping("/users")
    public String putUsers(){return "Putting User Detail";}

    @DeleteMapping("/users")
    public String deleteUsers(){return "Deleting User Detail";}

}
