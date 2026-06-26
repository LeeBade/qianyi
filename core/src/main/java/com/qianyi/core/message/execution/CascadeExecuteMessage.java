package com.qianyi.core.message.execution;


import lombok.Data;

/**
 * Runtime级联触发消息体，承载节点触发意图
 * Controller投递，Runtime消费
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@Data
public class CascadeExecuteMessage {

    private String matterId;

    private String nodeId;

    /**
     * 触发意图，决定幂等守卫的状态校验条件
     */
    private TriggerType triggerType;
}
