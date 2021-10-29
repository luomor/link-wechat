package com.linkwechat.wecom.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.linkwechat.common.annotation.Excel;
import com.linkwechat.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 群发消息列对象 we_group_message_list
 *
 * @author ruoyi
 * @date 2021-10-19
 */
@ApiModel
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("we_group_message_list")
public class WeGroupMessageList extends BaseEntity {

    /**
     * 主键id
     */
    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 企业群发消息的id
     */
    @ApiModelProperty("企业群发消息的id")
    @Excel(name = "企业群发消息的id")
    private String msgId;

    /**
     * 群发任务的类型，默认为single，表示发送给客户，group表示发送给客户群
     */
    @ApiModelProperty("群发任务的类型，默认为single，表示发送给客户，group表示发送给客户群")
    @Excel(name = "群发任务的类型，默认为single，表示发送给客户，group表示发送给客户群")
    private String chatType;

    /**
     * 群发消息创建者userid
     */
    @ApiModelProperty("群发消息创建者userid")
    @Excel(name = "群发消息创建者userid")
    private String userId;

    /**
     * 发送时间
     */
    @ApiModelProperty("发送时间")
    @Excel(name = "发送时间", width = 30, dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /**
     * 群发消息创建来源。0：企业 1：个人
     */
    @ApiModelProperty("群发消息创建来源。0：企业 1：个人")
    @Excel(name = "群发消息创建来源。0：企业 1：个人")
    private Integer createType;

    /**
     * 是否定时任务 0 常规 1 定时发送
     */
    @ApiModelProperty("是否定时任务 0 常规 1 定时发送")
    @Excel(name = "是否定时任务 0 常规 1 定时发送")
    private Integer isTask;
}
