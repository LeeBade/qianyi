package com.qianyi.core.model.matter;

import lombok.Data;

/**
* HUMAN节点责任人描述符与自然人的绑定
*
* @author TianJunQi
* @since 2026-06-16
  */

@Data
public class AssigneeBinding {

    private String descriptor;

    private String resolvedId;
}
