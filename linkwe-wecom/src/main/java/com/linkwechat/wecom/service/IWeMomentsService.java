package com.linkwechat.wecom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linkwechat.wecom.domain.WeMoments;
import com.linkwechat.wecom.mapper.WeMomentsMapper;

import java.util.List;

public interface IWeMomentsService extends IService<WeMoments> {

    List<WeMoments> findMoments(WeMoments weMoments);

    void addOrUpdateMoments(WeMoments weMoments);

    void synchPersonMoments(Integer filterType);

    void synchEnterpriseMoments(Integer filterType);
}
