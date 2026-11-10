package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
public class MainController {

    // =====================================================
    // UPLOAD SETTINGS (used by the Android app)
    // =====================================================

    // Folder where uploaded files are saved.
    private static final Path UPLOAD_DIR = Paths.get("uploads");


    // =====================================================
    // FILE UPLOAD
    //   POST /upload  (multipart/form-data)
    //   field "file"    : the file (required)
    //   field "library" : HttpURLConnection | OkHttp | Retrofit | Volley (optional)
    // =====================================================

    @PostMapping("/upload")
    public ResponseEntity<FileUpload> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "library", defaultValue = "Unknown") String library
    ) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new FileUpload(false, "No file received.", null, 0, library)
            );
        }

        try {
            Files.createDirectories(UPLOAD_DIR);

            // Clean the file name to prevent path traversal.
            String original = file.getOriginalFilename() == null
                    ? "file"
                    : Paths.get(file.getOriginalFilename()).getFileName().toString();
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedName = System.currentTimeMillis() + "-" + safeName;

            Files.copy(
                    file.getInputStream(),
                    UPLOAD_DIR.resolve(storedName),
                    StandardCopyOption.REPLACE_EXISTING
            );

            return ResponseEntity.ok(
                    new FileUpload(true, "File uploaded successfully", original, file.getSize(), library)
            );

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new FileUpload(false, "Could not save file: " + e.getMessage(), null, 0, library)
            );
        }
    }



    // =====================================================
    // YOUR EXISTING ENDPOINTS (unchanged)
    // =====================================================

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