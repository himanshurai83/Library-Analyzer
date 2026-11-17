package com.example.demo;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
    //   field "library" : HttpURLConnection | OkHttp | Retrofit | Volley | Ktor (optional)
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
    // FILE DOWNLOAD (used by the Android "Download File" page)
    //   GET /files                     -> list of files in the "uploads" folder
    //                                     [{ "name": "...", "size": 123 }, ...]
    //   GET /download/{name}           -> the file itself
    //   GET /download/{name}?library=X -> same ("library" is only used in error messages)
    // =====================================================

    @GetMapping("/files")
    public List<Map<String, Object>> listFiles() throws IOException {

        if (!Files.isDirectory(UPLOAD_DIR)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(UPLOAD_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    // Stored names start with a timestamp, so this shows the newest first.
                    .sorted(Comparator.reverseOrder())
                    .map(p -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", p.getFileName().toString());
                        try {
                            item.put("size", Files.size(p));
                        } catch (IOException e) {
                            item.put("size", -1);
                        }
                        return item;
                    })
                    .collect(Collectors.toList());
        }
    }

    @GetMapping("/download/{name:.+}")
    public ResponseEntity<?> downloadFile(
            @PathVariable("name") String name,
            @RequestParam(value = "library", defaultValue = "Unknown") String library
    ) throws IOException {

        Path baseDir = UPLOAD_DIR.toAbsolutePath().normalize();
        Path file = baseDir.resolve(name).normalize();

        // Block "../" tricks: the file must be inside the uploads folder.
        if (!file.startsWith(baseDir) || !Files.isRegularFile(file)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FileDownload(false, "File not found", name, 0, library));
        }

        Resource resource = new UrlResource(file.toUri());

        return ResponseEntity.ok()
                .contentLength(Files.size(file))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name).build().toString())
                .body(resource);
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