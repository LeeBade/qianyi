package com.qianyi.core.model.task;


import lombok.Data;

import java.util.Map;

/**
 * RPC协议配置
 *
 * @author TianJunQi
 * @since 2026-06-18
 */
@Data
public class RpcAction {
    private String protocol;
    // 参数随协议变化，使用 Map 承载
    private Map<String, Object> params = Map.of();
}
