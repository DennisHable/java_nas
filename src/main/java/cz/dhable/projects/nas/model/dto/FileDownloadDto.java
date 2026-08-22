package cz.dhable.projects.nas.model.dto;

public class FileDownloadDto {
    private final String originalName;
    private final String contentType;

    public FileDownloadDto(String originalName, String contentType) {
        this.originalName = originalName;
        this.contentType = contentType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }
}
