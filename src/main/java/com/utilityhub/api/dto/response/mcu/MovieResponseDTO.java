package com.utilityhub.api.dto.response.mcu;
import java.sql.Date;
import com.utilityhub.api.db.entity.mcu.WatchStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponseDTO {
    private String globalId;
    private String title;
    private Date premiereDate;
    private Integer runtime;
    private String synopsis;
    private WatchStatus status;
    private String s3Url;
    private ContentType type;
}
