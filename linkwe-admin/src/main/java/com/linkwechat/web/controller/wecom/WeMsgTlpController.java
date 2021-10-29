package com.linkwechat.web.controller.wecom;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linkwechat.common.annotation.Log;
import com.linkwechat.common.core.controller.BaseController;
import com.linkwechat.common.core.domain.AjaxResult;
import com.linkwechat.common.core.page.TableDataInfo;
import com.linkwechat.common.enums.BusinessType;
import com.linkwechat.common.utils.StringUtils;
import com.linkwechat.wecom.domain.WeMsgTlp;
import com.linkwechat.wecom.service.IWeMsgTlpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 欢迎语模板Controller
 * 
 * @author ruoyi
 * @date 2020-10-04
 */
@RestController
@RequestMapping("/wecom/tlp")
public class WeMsgTlpController extends BaseController
{
    @Autowired
    private IWeMsgTlpService weMsgTlpService;



    /**
     * 查询欢迎语模板列表
     */
    //   @PreAuthorize("@ss.hasPermi('wecom:tlp:list')")
    @GetMapping("/list")
    public TableDataInfo list(WeMsgTlp weMsgTlp)
    {
        startPage();
        return getDataTable(weMsgTlpService.list(
                new LambdaQueryWrapper<WeMsgTlp>()
                        .like(StringUtils.isNotEmpty(weMsgTlp.getWelcomeMsg()),WeMsgTlp::getWelcomeMsg,weMsgTlp.getWelcomeMsg())
                        .eq(weMsgTlp.getWelcomeMsgTplType() !=null,WeMsgTlp::getWelcomeMsgTplType,weMsgTlp.getWelcomeMsgTplType())
                        .like(StringUtils.isNotEmpty(weMsgTlp.getUserIds()),WeMsgTlp::getUserIds,weMsgTlp.getUserIds())
        ));

    }



    /**
     * 新增欢迎语模板
     */
    //  @PreAuthorize("@ss.hasPermi('wecom:tlp:add')")
    @Log(title = "新增欢迎语模板", businessType = BusinessType.INSERT)
    @PostMapping(value = "/addorUpdate")
    public AjaxResult addorUpdate(@RequestBody WeMsgTlp weMsgTlp)
    {

        weMsgTlpService.addorUpdate(weMsgTlp);
        return AjaxResult.success();
    }


    /**
     * 删除欢迎语模板
     */
    //   @PreAuthorize("@ss.hasPermi('wecom:tlp:remove')")
    @Log(title = "删除欢迎语模板", businessType = BusinessType.DELETE)
	@DeleteMapping("/remove/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        weMsgTlpService.removeByIds(CollectionUtil.toList(ids));

        return AjaxResult.success();
    }
}
