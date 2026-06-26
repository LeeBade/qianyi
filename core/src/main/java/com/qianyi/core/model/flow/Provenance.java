package com.qianyi.core.model.flow;

import lombok.Data;

/**
* Flow资产溯源信息
*
* @author TianJunQi
* @since 2026-06-16
  */

@Data
public class Provenance {

    private String authoredBy;

    private String patchId;

    private String matterId;
}
