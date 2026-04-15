package org.example.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class TaskDTO {
    private String title;
    private String description;
    private Date date;
    private String status;
}
