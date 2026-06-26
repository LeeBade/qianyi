package com.qianyi.core.model.schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* JSON Schema Draft-07 核心结构体，后续可按需扩展 $ref / items / additionalProperties 等关键字
*
* @author TianJunQi
* @since 2026-06-16
  */
@Data
public class JsonSchema {

    private String type;
    /**
     * 字段名称，面向业务人员，可选
     */
    private String title;

    /**
     * 字段描述，面向业务人员，可选
     */
    private String description;

    private List<String> required = List.of();

    private Map<String, JsonSchema> properties = Map.of();

    @JsonAnySetter
    @Getter(AccessLevel.NONE)
    private Map<String, Object> extensions = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return extensions;
    }
}
