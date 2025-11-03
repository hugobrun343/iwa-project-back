package com.iwaproject.announcement.repositories;

import com.iwaproject.announcement.entities.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    /**
     * Find public images by announcement id.
     *
     * @param id the id of the announcement
     * @return list of images
     */
    List<Image> findByAnnouncementIdAndIsPrivateFalse(Long id);

    /**
     * Find all images by announcement id.
     *
     * @param id the id of the announcement
     * @return list of images
     */
    List<Image> findByAnnouncementId(Long id);


}
