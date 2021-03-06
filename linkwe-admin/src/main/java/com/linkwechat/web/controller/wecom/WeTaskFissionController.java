package com.linkwechat.web.controller.wecom;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
//import com.alibaba.nacos.common.utils.CollectionUtils;
import com.google.common.collect.Lists;
import com.linkwechat.common.annotation.Log;
import com.linkwechat.common.config.CosConfig;
import com.linkwechat.common.constant.HttpStatus;
import com.linkwechat.common.core.controller.BaseController;
import com.linkwechat.common.core.domain.AjaxResult;
import com.linkwechat.common.core.page.TableDataInfo;
import com.linkwechat.common.enums.BusinessType;
import com.linkwechat.common.exception.wecom.WeComException;
import com.linkwechat.common.utils.DateUtils;
import com.linkwechat.common.utils.StringUtils;
import com.linkwechat.common.utils.file.FileUploadUtils;
import com.linkwechat.common.utils.poi.ExcelUtil;
import com.linkwechat.framework.web.domain.server.SysFile;
import com.linkwechat.framework.web.service.FileService;
import com.linkwechat.wecom.domain.WeCustomer;
import com.linkwechat.wecom.domain.WeTaskFission;
import com.linkwechat.wecom.domain.dto.WeChatUserDTO;
import com.linkwechat.wecom.domain.dto.WeTaskFissionPosterDTO;
import com.linkwechat.wecom.domain.query.WeTaskFissionStatisticQO;
import com.linkwechat.wecom.domain.vo.WeTaskFissionProgressVO;
import com.linkwechat.wecom.domain.vo.WeTaskFissionStatisticVO;
import com.linkwechat.wecom.domain.vo.WeTaskFissionTotalProgressVO;
import com.linkwechat.wecom.service.IWeTaskFissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;


/**
 * ?????????Controller
 *
 * @author leejoker
 * @date 2021-01-20
 */
@Api("?????????Controller")
@RestController
@RequestMapping("/wecom/fission")
public class WeTaskFissionController extends BaseController {

    @Autowired
    private IWeTaskFissionService weTaskFissionService;


    @Autowired
    private FileService fileService;

    /**
     * ?????????????????????
     */
    @ApiOperation(value = "?????????????????????", httpMethod = "GET")
    //    @PreAuthorize("@ss.hasPermi('wecom:fission:list')")
    @GetMapping("/list")
    public TableDataInfo<List<WeTaskFission>> list(WeTaskFission weTaskFission){
        startPage();
        List<WeTaskFission> list = weTaskFissionService.selectWeTaskFissionList(weTaskFission);
        return getDataTable(list);
    }

    /**
     * ??????????????????
     */
    @ApiOperation(value = "??????????????????", httpMethod = "GET")
    //  @PreAuthorize("@ss.hasPermi('wecom:fission:stat')")
    @GetMapping("/stat")
    public AjaxResult<WeTaskFissionStatisticVO> statistics(WeTaskFissionStatisticQO weTaskFissionStatisticQO) throws ParseException {
        Date st;
        Date et = DateUtils.getNowDate();
        if (weTaskFissionStatisticQO.getSeven()) {
            st = DateUtils.addDays(et, -7);
        } else if (weTaskFissionStatisticQO.getThirty()) {
            st = DateUtils.addDays(et, -30);
        } else {
            if (StringUtils.isNotBlank(weTaskFissionStatisticQO.getBeginTime()) ^ StringUtils.isNotBlank(weTaskFissionStatisticQO.getEndTime())) {
                throw new WeComException("?????????????????????????????????");
            }
            st = DateUtils.parseDate(weTaskFissionStatisticQO.getBeginTime() + " 00:00:00", "yyyy-MM-dd HH:mm:ss");
            et = DateUtils.parseDate(weTaskFissionStatisticQO.getEndTime() + " 23:59:59", "yyyy-MM-dd HH:mm:ss");
        }
        WeTaskFissionStatisticVO vo = weTaskFissionService.taskFissionStatistic(weTaskFissionStatisticQO.getTaskFissionId(), st, et);
        return AjaxResult.success(vo);
    }

    /**
     * ?????????????????????
     */
    @ApiOperation(value = "?????????????????????", httpMethod = "GET")
    //    @PreAuthorize("@ss.hasPermi('wecom:fission:export')")
    @Log(title = "?????????", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(WeTaskFission weTaskFission) {
        List<WeTaskFission> list = weTaskFissionService.selectWeTaskFissionList(weTaskFission);
        ExcelUtil<WeTaskFission> util = new ExcelUtil<WeTaskFission>(WeTaskFission.class);
        return util.exportExcel(list, "fission");
    }

    /**
     * ???????????????????????????
     */
    @ApiOperation(value = "???????????????????????????", httpMethod = "GET")
    //    @PreAuthorize("@ss.hasPermi('wecom:fission:query')")
    @GetMapping(value = "/getInfo/{id}")
    public AjaxResult<WeTaskFission> getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(weTaskFissionService.selectWeTaskFissionById(id));
    }

    /**
     * ???????????????
     */
    @ApiOperation(value = "???????????????", httpMethod = "POST")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:add')")
    @Log(title = "?????????", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody WeTaskFission weTaskFission) {
        weTaskFissionService.insertWeTaskFission(weTaskFission);
        return AjaxResult.success();
    }

    /**
     * ???????????????
     */
    @ApiOperation(value = "???????????????", httpMethod = "POST")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:edit')")
    @Log(title = "?????????", businessType = BusinessType.INSERT)
    @PutMapping("/edit")
    public AjaxResult edit(@RequestBody WeTaskFission weTaskFission) {
        if (ObjectUtils.isEmpty(weTaskFission.getId())) {
            return AjaxResult.error("??????id??????");
        }
        WeTaskFission fissionTask = weTaskFissionService.selectWeTaskFissionById(weTaskFission.getId());
        if (ObjectUtils.isEmpty(fissionTask)) {
            return AjaxResult.error("???????????????");
        }
        CopyOptions options = CopyOptions.create();
        options.setIgnoreNullValue(true);
        BeanUtil.copyProperties(weTaskFission, fissionTask, options);
        if (CollectionUtil.isNotEmpty(weTaskFission.getTaskFissionStaffs())) {
            fissionTask.setTaskFissionStaffs(weTaskFission.getTaskFissionStaffs());
        }
        if (CollectionUtil.isNotEmpty(weTaskFission.getTaskFissionWeGroups())) {
            fissionTask.setTaskFissionWeGroups(weTaskFission.getTaskFissionWeGroups());
        }
        Long id = weTaskFissionService.updateWeTaskFission(fissionTask);
        JSONObject json = new JSONObject();
        json.put("id", id);
        return AjaxResult.success(json.toJSONString());
    }

    /**
     * ???????????????
     */
    @ApiOperation(value = "???????????????", httpMethod = "DELETE")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:remove')")
    @Log(title = "?????????", businessType = BusinessType.DELETE)
    @DeleteMapping("/delete/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(weTaskFissionService.deleteWeTaskFissionByIds(ids));
    }

    /**
     * ??????????????????
     */
    /*@ApiOperation(value = "??????????????????", httpMethod = "GET")
    @Log(title = "??????????????????", businessType = BusinessType.OTHER)
    @GetMapping("/send/{id}")
    public AjaxResult send(@PathVariable Long id) throws Exception {
        weTaskFissionService.sendWeTaskFission(id);
        return AjaxResult.success();
    }*/

    /**
     * ???????????????????????????
     */
    @ApiOperation(value = "???????????????????????????", httpMethod = "POST")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:complete')")
    @Log(title = "???????????????????????????", businessType = BusinessType.OTHER)
    @PostMapping("/complete/{id}/records/{recordId}")
    public AjaxResult completeRecord(@PathVariable("id") Long id,
                                     @PathVariable("recordId") Long recordId,
                                     @RequestBody WeChatUserDTO weChatUserDTO) {
        WeTaskFission taskFission = weTaskFissionService.selectWeTaskFissionById(id);
        if (taskFission == null) {
            return AjaxResult.error(HttpStatus.NOT_FOUND, "???????????????");
        }
        weTaskFissionService.completeFissionRecord(id, recordId, weChatUserDTO);
        return AjaxResult.success("????????????", taskFission.getFissQrcode());
    }

    /**
     * ???????????????????????????
     */
    @ApiOperation(value = "???????????????????????????", httpMethod = "POST")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:poster')")
    @Log(title = "???????????????????????????", businessType = BusinessType.OTHER)
    @PostMapping("/poster")
    public AjaxResult<JSONObject> posterGenerate(@RequestBody WeTaskFissionPosterDTO weTaskFissionPosterDTO) {
        String posterUrl = weTaskFissionService.fissionPosterGenerate(weTaskFissionPosterDTO);
        JSONObject json = new JSONObject();
        json.put("posterUrl", posterUrl);
        return AjaxResult.success(json);
    }

    /**
     * ??????????????????
     */
    //   @PreAuthorize("@ss.hasPermi('wechat:fission:upload')")
    @Log(title = "??????????????????", businessType = BusinessType.OTHER)
    @PostMapping("/upload")
    @ApiOperation(value = "??????????????????", httpMethod = "POST")
    public AjaxResult<JSONObject> upload(@RequestParam(value = "file") MultipartFile file) throws Exception {

        SysFile sysFile
                = fileService.upload(file);
//        String url = FileUploadUtils.upload2Cos(file, cosConfig);
        JSONObject json = new JSONObject();
        json.put("rewardImageUrl", sysFile.getImgUrlPrefix()+sysFile.getFileName());
        return AjaxResult.success(json);
    }


    /**
     * ????????????id??????????????????????????????
     */
    @ApiOperation(value = "????????????id??????????????????????????????", httpMethod = "GET")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:getCustomerListById')")
    @Log(title = "????????????id??????????????????????????????", businessType = BusinessType.OTHER)
    @GetMapping("/getCustomerListById/{id}")
    public AjaxResult<List<WeCustomer>> getCustomerListById(@ApiParam("??????id") @PathVariable("id") String id) {
        return AjaxResult.success(weTaskFissionService.getCustomerListById(null, id));
    }


    /**
     * ???????????????????????????????????????
     */
    @ApiOperation(value = "???????????????????????????????????????", httpMethod = "GET")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:getCustomerProgress')")
    @Log(title = "???????????????????????????????????????", businessType = BusinessType.OTHER)
    @GetMapping("/{id}/progress/{unionId}")
    public AjaxResult<WeTaskFissionProgressVO> getCustomerProgress(@ApiParam("??????id") @PathVariable("id") Long id
            , @PathVariable("unionId") @ApiParam("??????id") String unionId) {
        WeTaskFission weTaskFission = weTaskFissionService.selectWeTaskFissionById(id);
        if (weTaskFission != null) {
            return AjaxResult.success(weTaskFissionService.getCustomerTaskProgress(weTaskFission, unionId));
        } else {
            throw new WeComException("???????????????");
        }
    }

    /**
     * ?????????????????????????????????????????????
     */
    @ApiOperation(value = "?????????????????????????????????????????????", httpMethod = "GET")
    //   @PreAuthorize("@ss.hasPermi('wecom:fission:getCustomerProgress')")
    @Log(title = "?????????????????????????????????????????????", businessType = BusinessType.OTHER)
    @GetMapping("/{id}/progress")
    public AjaxResult<List<WeTaskFissionTotalProgressVO>> getAllCustomerProgress(@ApiParam("??????id") @PathVariable("id") Long id) {
        WeTaskFission weTaskFission = weTaskFissionService.selectWeTaskFissionById(id);
        List<WeTaskFissionTotalProgressVO> list = Lists.newArrayList();
        if (weTaskFission != null) {
            List<WeCustomer> customers = weTaskFissionService.getCustomerListById(null, String.valueOf(id));
            if (StringUtils.isNotEmpty(customers)) {
                customers.stream().filter(Objects::nonNull).forEach(customer -> {
                    WeTaskFissionTotalProgressVO vo = new WeTaskFissionTotalProgressVO();
                    vo.setCustomer(customer);
                    vo.setProgress(weTaskFissionService.getCustomerTaskProgress(weTaskFission, customer.getUnionid()));
                    list.add(vo);
                });
            }
            return AjaxResult.success(list);
        } else {
            throw new WeComException("???????????????");
        }
    }
}
