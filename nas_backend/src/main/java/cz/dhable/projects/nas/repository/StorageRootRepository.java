package cz.dhable.projects.nas.repository;

import cz.dhable.projects.nas.model.entity.StorageRoot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorageRootRepository extends JpaRepository<StorageRoot, Long> {
    Optional<StorageRoot> findByActiveTrue();
    boolean existsByBasePath(String basePath);
}
