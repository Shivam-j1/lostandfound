package com.campus.lostandfound;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByTitleContainingIgnoreCaseOrLocationContainingIgnoreCase(String title, String location);
    List<Item> findByTypeIgnoreCaseAndTitleContainingIgnoreCaseOrTypeIgnoreCaseAndLocationContainingIgnoreCase(
            String titleType, String title, String locationType, String location);
    List<Item> findByTypeIgnoreCase(String type);
    long countByTypeIgnoreCase(String type);
    long countByStatus(String status);
}
