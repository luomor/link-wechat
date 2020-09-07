package com.linkwechat.wecom.service.impl;

import java.util.List;

import com.linkwechat.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.linkwechat.wecom.mapper.WeTagMapper;
import com.linkwechat.wecom.domain.WeTag;
import com.linkwechat.wecom.service.IWeTagService;

/**
 * 企业微信标签Service业务层处理
 * 
 * @author ruoyi
 * @date 2020-09-07
 */
@Service
public class WeTagServiceImpl implements IWeTagService 
{
    @Autowired
    private WeTagMapper weTagMapper;

    /**
     * 查询企业微信标签
     * 
     * @param id 企业微信标签ID
     * @return 企业微信标签
     */
    @Override
    public WeTag selectWeTagById(Long id)
    {
        return weTagMapper.selectWeTagById(id);
    }

    /**
     * 查询企业微信标签列表
     * 
     * @param weTag 企业微信标签
     * @return 企业微信标签
     */
    @Override
    public List<WeTag> selectWeTagList(WeTag weTag)
    {
        return weTagMapper.selectWeTagList(weTag);
    }

    /**
     * 新增企业微信标签
     * 
     * @param weTag 企业微信标签
     * @return 结果
     */
    @Override
    public int insertWeTag(WeTag weTag)
    {
        weTag.setCreateTime(DateUtils.getNowDate());
        return weTagMapper.insertWeTag(weTag);
    }

    /**
     * 修改企业微信标签
     * 
     * @param weTag 企业微信标签
     * @return 结果
     */
    @Override
    public int updateWeTag(WeTag weTag)
    {
        return weTagMapper.updateWeTag(weTag);
    }

    /**
     * 批量删除企业微信标签
     * 
     * @param ids 需要删除的企业微信标签ID
     * @return 结果
     */
    @Override
    public int deleteWeTagByIds(Long[] ids)
    {
        return weTagMapper.deleteWeTagByIds(ids);
    }

    /**
     * 删除企业微信标签信息
     * 
     * @param id 企业微信标签ID
     * @return 结果
     */
    @Override
    public int deleteWeTagById(Long id)
    {
        return weTagMapper.deleteWeTagById(id);
    }
}
