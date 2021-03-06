package com.linkwechat.wecom.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.linkwechat.common.constant.WeConstans;
import com.linkwechat.common.core.domain.entity.WeCorpAccount;
import com.linkwechat.common.enums.CommunityTaskType;
import com.linkwechat.common.enums.MessageType;
import com.linkwechat.common.exception.CustomException;
import com.linkwechat.common.exception.wecom.WeComException;
import com.linkwechat.common.utils.StringUtils;
import com.linkwechat.wecom.client.WeMessagePushClient;
import com.linkwechat.wecom.domain.*;
import com.linkwechat.wecom.domain.dto.WeMessagePushDto;
import com.linkwechat.wecom.domain.dto.message.*;
import com.linkwechat.wecom.domain.query.WeAddGroupMessageQuery;
import com.linkwechat.wecom.domain.vo.SenderInfo;
import com.linkwechat.wecom.domain.vo.WeCommunityTaskEmplVo;
import com.linkwechat.wecom.domain.vo.WePresTagGroupTaskVo;
import com.linkwechat.wecom.domain.vo.WePresTagTaskListVO;
import com.linkwechat.wecom.mapper.*;
import com.linkwechat.wecom.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WePresTagGroupTaskServiceImpl extends ServiceImpl<WePresTagGroupTaskMapper, WePresTagGroupTask> implements IWePresTagGroupTaskService {

    @Autowired
    WePresTagGroupTaskStatMapper taskStatMapper;

    @Autowired
    WePresTagGroupTaskScopeMapper taskScopeMapper;

    @Autowired
    WePresTagGroupTaskTagMapper taskTagMapper;

    @Autowired
    WeMessagePushClient messagePushClient;

    @Autowired
    WeGroupCodeMapper groupCodeMapper;

    @Autowired
    IWeCorpAccountService corpAccountService;

    @Autowired
    IWeGroupMessageTemplateService iWeGroupMessageTemplateService;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(WePresTagGroupTask task) {
        int rows = baseMapper.insert(task);
        if (rows > 0) {
            // ??????????????????
            if (CollectionUtil.isNotEmpty(task.getTagList())) {
                List<WePresTagGroupTaskTag> taskTagList = task.getTagList()
                        .stream()
                        .map(id -> {
                            WePresTagGroupTaskTag taskTag = new WePresTagGroupTaskTag();
                            taskTag.setTaskId(task.getTaskId());
                            taskTag.setTagId(id);
                            taskTag.setCreateBy(task.getCreateBy());
                            return taskTag;
                        })
                        .collect(Collectors.toList());
                taskTagMapper.batchBindsTaskTags(taskTagList);
            }

            // ??????????????????
            if (CollectionUtil.isNotEmpty(task.getScopeList())) {
                List<WePresTagGroupTaskScope> wePresTagGroupTaskScopeList = task.getScopeList()
                        .stream()
                        .map(id -> {
                            WePresTagGroupTaskScope sc = new WePresTagGroupTaskScope();
                            sc.setTaskId(task.getTaskId());
                            sc.setWeUserId(id);
                            sc.setCreateBy(task.getCreateBy());
                            return sc;
                        })
                        .collect(Collectors.toList());
                taskScopeMapper.batchBindsTaskScopes(wePresTagGroupTaskScopeList);
            }
        }
        return rows;
    }


    @Override
    public List<WePresTagTaskListVO> selectTaskList(String taskName, Integer sendType, String createBy, String beginTime, String endTime) {
        List<WePresTagTaskListVO> list = baseMapper.selectListVO(taskName, sendType, createBy, beginTime, endTime);
        list.forEach(i -> {
            if (StringUtils.isNotEmpty(i.getTagListStr())) {
                i.setTagList(Arrays.asList(i.getTagListStr().split(",")));
            } else {
                i.setTagList(new ArrayList<>());
            }
        });
        return list;
    }

    @Override
    public WePresTagGroupTaskVo getTaskById(Long taskId) {
        return baseMapper.selectTaskById(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRemoveTaskByIds(Long[] idList) {
        List<Long> ids = Arrays.asList(idList);

        // ?????????????????????
        LambdaQueryWrapper<WePresTagGroupTaskTag> tagQueryWrapper = new LambdaQueryWrapper<>();
        tagQueryWrapper.in(WePresTagGroupTaskTag::getTaskId, ids);
        taskTagMapper.delete(tagQueryWrapper);
        // ?????????????????????
        LambdaQueryWrapper<WePresTagGroupTaskScope> scopeQueryWrapper = new LambdaQueryWrapper<>();
        scopeQueryWrapper.in(WePresTagGroupTaskScope::getTaskId, ids);
        taskScopeMapper.delete(scopeQueryWrapper);
        // ?????????????????????
        LambdaQueryWrapper<WePresTagGroupTaskStat> statQueryWrapper = new LambdaQueryWrapper<>();
        statQueryWrapper.in(WePresTagGroupTaskStat::getTaskId, ids);
        taskStatMapper.delete(statQueryWrapper);

        // ????????????task
        LambdaQueryWrapper<WePresTagGroupTask> taskQueryWrapper = new LambdaQueryWrapper<>();
        taskQueryWrapper.in(WePresTagGroupTask::getTaskId, ids);

        return baseMapper.delete(taskQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTask(WePresTagGroupTask task) {

        if (isNameOccupied(task)) {
            throw new CustomException("??????????????????");
        }
        int rows = baseMapper.updateById(task);
        if (rows > 0) {
            // ???????????? - ??????????????????
            LambdaUpdateWrapper<WePresTagGroupTaskTag> taskTagQueryWrapper = new LambdaUpdateWrapper<>();
            taskTagQueryWrapper
                    .eq(WePresTagGroupTaskTag::getTaskId, task.getTaskId())
                    .set(WePresTagGroupTaskTag::getDelFlag, 1);
            taskTagMapper.update(null, taskTagQueryWrapper);
            // ???????????? - ??????????????????
            List<String> tagIdList = task.getTagList();
            if (CollectionUtil.isNotEmpty(tagIdList)) {
                List<WePresTagGroupTaskTag> wePresTagGroupTaskTagList = tagIdList
                        .stream()
                        .map(id -> {
                            WePresTagGroupTaskTag taskTag = new WePresTagGroupTaskTag();
                            taskTag.setTaskId(task.getTaskId());
                            taskTag.setTagId(id);
                            taskTag.setCreateBy(task.getCreateBy());
                            taskTag.setCreateTime(new Date());
                            return taskTag;
                        })
                        .collect(Collectors.toList());
                taskTagMapper.batchBindsTaskTags(wePresTagGroupTaskTagList);
            }

            // ?????????????????????????????????
            LambdaUpdateWrapper<WePresTagGroupTaskScope> scopeQueryWrapper = new LambdaUpdateWrapper<>();
            scopeQueryWrapper.eq(WePresTagGroupTaskScope::getTaskId, task.getTaskId()).set(WePresTagGroupTaskScope::getDelFlag, 1);
            taskScopeMapper.update(null, scopeQueryWrapper);

            // ???????????????????????????
            List<String> userIdList = task.getScopeList();
            if (CollectionUtil.isNotEmpty(userIdList)) {
                List<WePresTagGroupTaskScope> wePresTagGroupTaskScopeList = userIdList
                        .stream()
                        .map(id -> {
                            WePresTagGroupTaskScope sc = new WePresTagGroupTaskScope();
                            sc.setTaskId(task.getTaskId());
                            sc.setWeUserId(id);
                            sc.setCreateBy(task.getCreateBy());
                            return sc;
                        })
                        .collect(Collectors.toList());
                taskScopeMapper.batchBindsTaskScopes(wePresTagGroupTaskScopeList);
            }
        }
        return rows;
    }


    /**
     * ??????????????????????????????
     * @param task
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskAndSendMsg(WePresTagGroupTask task) throws WeComException {
        try {
            this.updateTask(task);
            this.sendMessage(task);
        }catch (WeComException e){
            throw e;
        }

    }

    @Override
    public List<WePresTagGroupTaskStat> getTaskStat(Long id, String customerName, Integer isInGroup, Integer isSent, Integer sendType) {

        WePresTagGroupTaskStat query = new WePresTagGroupTaskStat();
        query.setTaskId(id);
        query.setCustomerName(customerName);
        query.setInGroup(isInGroup);
        query.setSent(isSent);

        // ??????????????????
        if (sendType.equals(0)) {
            return taskStatMapper.cropSendResultList(query);
        }
        // ??????????????????
        else {
            return taskStatMapper.singleSendResultList(query);
        }
    }

    @Override
    public List<WeCommunityTaskEmplVo> getScopeListByTaskId(Long taskId) {
        return taskScopeMapper.getScopeListByTaskId(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateFollowerTaskStatus(Long taskId, String followerId) {
        // ???????????????????????????????????????1
        LambdaUpdateWrapper<WePresTagGroupTaskStat> statLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        statLambdaUpdateWrapper
                .eq(WePresTagGroupTaskStat::getTaskId, taskId)
                .in(WePresTagGroupTaskStat::getUserId, followerId)
                .set(WePresTagGroupTaskStat::getSent, 1);
        return taskStatMapper.update(null, statLambdaUpdateWrapper);
    }

    @Override
    public boolean isNameOccupied(WePresTagGroupTask task) {
        Long currentId = Optional.ofNullable(task.getTaskId()).orElse(-1L);
        LambdaQueryWrapper<WePresTagGroupTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WePresTagGroupTask::getDelFlag, 0).eq(WePresTagGroupTask::getTaskName, task.getTaskName());
        List<WePresTagGroupTask> queryRes = baseMapper.selectList(queryWrapper);
        return !queryRes.isEmpty() && !currentId.equals(queryRes.get(0).getTaskId());
    }

    @Override
    public void sendMessage(WePresTagGroupTask task) throws  WeComException{
        Integer sendType = task.getSendType();
        try {
            // ??????????????????
            if (sendType.equals(0)) {

                List<SenderInfo> senderInfoList = baseMapper.selectSenderInfo(task.getTaskId());
                if (StringUtils.isEmpty(senderInfoList)) {
                    throw new WeComException("??????????????????????????????????????????");
                }
                String codeUrl = groupCodeMapper.selectById(task.getGroupCodeId()).getCodeUrl();
//                WeMediaDto media = materialService.uploadTemporaryMaterial(codeUrl, MediaType.IMAGE.getMediaType(), "????????????");
                List<WeMessageTemplate> attachmentList = Lists.newArrayList();
                WeMessageTemplate template = new WeMessageTemplate();
                template.setMsgType(MessageType.IMAGE.getMessageType());
//                template.setMediaId(media.getMedia_id());
                template.setMediaId(codeUrl);
                attachmentList.add(template);

                List<WeAddGroupMessageQuery.SenderInfo> senderList = senderInfoList.stream()
                        .map(sender -> {
                            WeAddGroupMessageQuery.SenderInfo info = new WeAddGroupMessageQuery.SenderInfo();
                            info.setUserId(sender.getUserId());
                            info.setCustomerList(Arrays.asList(sender.getCustomerList().split(",")));
                            return info;
                        }).collect(Collectors.toList());
                WeAddGroupMessageQuery query = new WeAddGroupMessageQuery();
                query.setContent(task.getWelcomeMsg());
                query.setSenderList(senderList);
                query.setChatType(1);
                query.setIsTask(0);
                query.setSource(2);
                query.setAttachmentsList(attachmentList);

                iWeGroupMessageTemplateService.addGroupMsgTemplate(query);
                task.setMessageTemplateId(query.getId());
                baseMapper.updateById(task);
            }
            // ??????????????????
            else {
                List<WeCustomer> externalIds = baseMapper.selectTaskExternalIds(task.getTaskId());
                List<String> followerIds = selectFollowerIdByTaskId(task.getTaskId());
                if (StringUtils.isEmpty(followerIds)) {
                    throw new WeComException("?????????????????????????????????????????????????????????");
                }

                // ?????????????????????
                LambdaUpdateWrapper<WePresTagGroupTaskStat> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(WePresTagGroupTaskStat::getTaskId, task.getTaskId()).set(WePresTagGroupTaskStat::getDelFlag, 1);
                taskStatMapper.update(null, updateWrapper);

                // ????????????????????????
                List<WePresTagGroupTaskStat> statList = externalIds.stream()
                        .map(i -> {
                            WePresTagGroupTaskStat stat = new WePresTagGroupTaskStat();
                            stat.setTaskId(task.getTaskId());
                            stat.setSent(0);
                            stat.setUserId(i.getFirstUserId());
                            stat.setExternalUserId(i.getExternalUserid());
                            stat.setCreateBy(task.getCreateBy());
                            return stat;
                        })
                        .collect(Collectors.toList());
                taskStatMapper.batchSave(statList);

                WeMessagePushDto pushDto = new WeMessagePushDto();
                // ??????toUser??????
                pushDto.setTouser(String.join("|", followerIds));

                // ??????agentId
                WeCorpAccount validWeCorpAccount = corpAccountService.findValidWeCorpAccount();
                String agentId = validWeCorpAccount.getAgentId();
                String corpId = validWeCorpAccount.getCorpId();
                if (StringUtils.isEmpty(agentId)) throw new CustomException("?????????????????????????????????agentId, ?????????????????????");
                pushDto.setAgentid(Integer.valueOf(agentId));
                // ??????????????????
                TextMessageDto text = new TextMessageDto();
                String REDIRECT_URI = URLEncoder.encode(String.format("%s?corpId=%s&agentId=%s&type=%s", validWeCorpAccount.getSopTagRedirectUrl(), corpId, agentId, CommunityTaskType.TAG.getType()));
                String context = String.format(WeConstans.MSG_TEMPLATE, validWeCorpAccount.getAuthorizeUrl(), corpId, REDIRECT_URI);
                text.setContent(context);
                pushDto.setText(text);
                pushDto.setMsgtype("text");

                // ??????????????????
                messagePushClient.sendMessageToUser(pushDto, agentId);
            }
        } catch (Exception e) {
            log.error("============> ????????????????????????????????????, ????????????: {}", task);
            log.error("????????????: {}", e.getMessage());
            throw new WeComException(e.getMessage());
        }
    }

    @Override
    public List<String> selectExternalIds(Long taskId) {
        List<WeCustomer> customers = baseMapper.selectTaskExternalIds(taskId);
        return customers.stream().map(WeCustomer::getExternalUserid).collect(Collectors.toList());
    }

    @Override
    public List<String> selectFollowerIdByTaskId(Long taskId) {
        return baseMapper.selectTaskFollowerIds(taskId);
    }

    @Override
    public List<WePresTagGroupTaskVo> getFollowerTaskList(String emplId, Integer isDone) {
        return baseMapper.getTaskListByFollowerId(emplId, isDone);
    }

    private List<String> selectExternalIdsByFollowerId(Long taskId, String followerId) {
        return baseMapper.selectTaskExternalByFollowerId(taskId, followerId);
    }


}
