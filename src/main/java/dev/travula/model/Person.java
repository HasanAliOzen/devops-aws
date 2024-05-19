package dev.travula.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.File;

@Entity
@Table(name = "person")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "img_url")
    private String imgUrl;

    @Transient
    private Object imageFile; // Add this field

    private static final String UPLOAD_DIR = "src/main/resources/static/images/";

    @PreRemove
    public void deleteImageFile() {
        if (this.imgUrl != null && !this.imgUrl.isEmpty()) {
            String filename = this.imgUrl.substring(this.imgUrl.lastIndexOf('/') + 1);
            File file = new File(UPLOAD_DIR + filename);
            if (file.exists()) {
                file.delete();
            }
        }
    }

}
