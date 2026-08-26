package cz.dhable.projects.nas.model.dto;

import java.util.List;

public class FolderTreeDto {
    private final Long id; // id akutální složky
    private final String name; // název aktuální složky
    private final List<FolderTreeDto> children; // podsložky vnořené v této složce

    public FolderTreeDto(Long id, String name, List<FolderTreeDto> children) {
        this.id = id;
        this.name = name;
        this.children = children;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<FolderTreeDto> getChildren() {
        return children;
    }
}
