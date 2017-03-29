package cn.eastseven;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by dongqi on 17/3/29.
 */
public interface ProductRepository extends MongoRepository<Product, String> {
}
