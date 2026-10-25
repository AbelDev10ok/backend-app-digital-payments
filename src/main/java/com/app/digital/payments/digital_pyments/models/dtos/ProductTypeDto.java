package com.app.digital.payments.digital_pyments.models.dtos;

public class ProductTypeDto {
    private Long id;
    private String name;
    private String icon;
    private Long parentId;
    private String parentName;
    private String slug;

    // Constructores, getters y setters
    public ProductTypeDto() {}

    public ProductTypeDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public ProductTypeDto(Long id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public ProductTypeDto(Long id, String name, String icon, Long parentId, String parentName, String slug) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.parentId = parentId;
        this.parentName = parentName;
        this.slug = slug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
