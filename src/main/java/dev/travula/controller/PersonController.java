package dev.travula.controller;

import dev.travula.exceptions.FileDownloadException;
import dev.travula.exceptions.FileEmptyException;
import dev.travula.exceptions.FileUploadException;
import dev.travula.model.Person;
import dev.travula.repo.PersonRepository;
import dev.travula.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping
@Validated
public class PersonController {

    private final PersonRepository personRepository;
    private final FileService fileService;



    private static final String UPLOAD_DIR = "src/main/resources/static/images/";

    @GetMapping
    public String people(Model model) throws FileDownloadException, IOException {

        List<Person> people = new ArrayList<>();
        personRepository.findAll().forEach(person -> {
            if (person.getImgUrl() != null && !person.getImgUrl().isEmpty()) {
                try {
                    person.setImageFile(fileService.downloadFile(person.getImgUrl()));
                    person.setImgUrl( "/images/" +person.getImgUrl());
                    people.add(person);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        model.addAttribute("people", people);
        return "people";
    }

    @GetMapping("/people")
    public String newPerson(Model model) {
        model.addAttribute("person", new Person());
        return "person_add";
    }

    @PostMapping("/people")
    public String addPerson(@ModelAttribute("person") @Valid Person person,
                            @RequestParam("imageFile") MultipartFile multipartFile,
                            Model model) throws FileEmptyException, IOException, FileUploadException {
        if (multipartFile.isEmpty()){
            throw new FileEmptyException("File is empty. Cannot save an empty file");
        }

        boolean isValidFile = isValidFile(multipartFile);
        List<String> allowedFileExtensions = new ArrayList<>(Arrays.asList("png", "jpg"));

        if (isValidFile && allowedFileExtensions.contains(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))){
            String fileName = fileService.uploadFile(multipartFile);
            person.setImgUrl(fileName);
            personRepository.save(person);

        } else {
            model.addAttribute("error", "Error occurred while adding person. Please try again.");
            return "error";
        }

        return "redirect:/";
    }

    @GetMapping("/people/edit/{id}")
    public String editPerson(@PathVariable Long id, Model model) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid person Id:" + id));
        model.addAttribute("person", person);
        return "person_edit";
    }

    @PostMapping("/people/edit/{id}")
    public String editPerson(@PathVariable Long id, @ModelAttribute("person") @Valid Person person,
                             @RequestParam("imageFile") MultipartFile multipartFile,
                             Model model) throws IOException, FileUploadException {

        if (multipartFile.isEmpty()){
            model.addAttribute("error", "File is empty. Cannot save an empty file");
            return "error";
        }

        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid person Id:" + id));

        boolean isValidFile = isValidFile(multipartFile);
        List<String> allowedFileExtensions = new ArrayList<>(Arrays.asList("png", "jpg"));

        if (isValidFile && allowedFileExtensions.contains(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))){
            fileService.delete(existingPerson.getImgUrl());

            String fileName = fileService.uploadFile(multipartFile);

            existingPerson.setName(person.getName());
            existingPerson.setAddress(person.getAddress());
            existingPerson.setImgUrl(fileName);
            personRepository.save(existingPerson);

        } else {
            model.addAttribute("error", "Error occurred while adding person. Please try again.");
            return "error";
        }

        return "redirect:/";
    }

    @GetMapping("/people/delete/{id}")
    public String deletePerson(@PathVariable Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid person Id:" + id));

        personRepository.deleteById(id);

        if (person.getImgUrl() != null && !person.getImgUrl().isEmpty()) {
            fileService.delete(person.getImgUrl());
        }

        return "redirect:/";
    }

    @GetMapping("/images/{fileName}")
    public ResponseEntity<InputStreamResource> viewFile(@PathVariable String fileName){
        var s3Object = fileService.getFile(fileName);
        var content = s3Object.getObjectContent();

        // Determine the content type
        String contentType;
        if (fileName.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if (fileName.endsWith(".gif")) {
            contentType = MediaType.IMAGE_GIF_VALUE;
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // Default binary type
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(new InputStreamResource(content));
    }

    private void saveImage(Person person, MultipartFile imageFile) throws IOException {
        if (!imageFile.isEmpty()) {
            byte[] bytes = imageFile.getBytes();
            String fileName = UUID.randomUUID().toString() + "-" + imageFile.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.write(path, bytes);
            person.setImgUrl("/images/" + fileName); // Set the relative path
        }
    }

    private void deleteImage(String imgUrl) {
        String filename = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
        File file = new File(UPLOAD_DIR + filename);
        if (file.exists()) {
            file.delete();
        }
    }

    /*private void updatePerson(Person existingPerson, Person updatedPerson) {
        existingPerson.setName(updatedPerson.getName());
        existingPerson.setAddress(updatedPerson.getAddress());

        // Delete the old image file
        if (existingPerson.getImgUrl() != null && !existingPerson.getImgUrl().isEmpty()) {
            deleteImage(existingPerson.getImgUrl());
        }

        // Save the new image file
        if (updatedPerson.getImageFile() != null && !updatedPerson.getImageFile().isEmpty()) {
            try {
                saveImage(existingPerson, updatedPerson.getImageFile());
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception properly
            }
        }
    }*/

    private boolean isValidFile(MultipartFile multipartFile){
        if (Objects.isNull(multipartFile.getOriginalFilename())){
            return false;
        }
        return !multipartFile.getOriginalFilename().trim().isEmpty();
    }

}
