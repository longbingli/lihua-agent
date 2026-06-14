package com.bingli.lihuaAgent.model.enums;

import org.apache.commons.lang3.ObjectUtils;

public enum AiAttachmentTypeEnum {

    IMAGE("image"),
    TEXT("text"),
    PDF("pdf");

    private final String value;

    AiAttachmentTypeEnum(String value) {
        this.value = value;
    }

    public static AiAttachmentTypeEnum getByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (AiAttachmentTypeEnum typeEnum : values()) {
            if (typeEnum.value.equalsIgnoreCase(value)) {
                return typeEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }
}
