package cn.eastseven;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by dongqi on 17/3/29.
 */
@Data
@Builder
@Document
public class Product {

    @Id
    private String id;

    private String name;

    private String image;

    private String url;
}
