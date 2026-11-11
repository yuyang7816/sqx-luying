package com.sqx.app.referral.withdraw.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordPageBo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordSaveBo;
import com.sqx.app.referral.withdraw.domain.vo.WithdrawRecordVo;
import com.sqx.app.referral.withdraw.service.WithdrawRecordService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 提现记录 前端控制器
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-05 10:10:32
 */
@Tag(name = "用户端-提现记录", description = "用户端-提现记录")
@RestController
@RequestMapping("/app/withdrawRecord")
public class AppWithdrawRecordController {

    @Resource
    private WithdrawRecordService withdrawRecordService;

    /**
     * 保存
     */
    @Login
    @Operation(summary = "发起提现")
    @PostMapping("save")
    public Result<Void> save(@RequestAttribute Long userId, @RequestBody @Validated WithdrawRecordSaveBo withdrawRecordSaveBo) {
        withdrawRecordSaveBo.setUserId(userId);
        withdrawRecordService.saveInfo(withdrawRecordSaveBo);
        return Result.success();
    }

    /**
     * 列表
     */
    @Login
    @Operation(summary = "列表")
    @PostMapping("page")
    public Result<IPage<WithdrawRecordVo>> page(@RequestAttribute Long userId, @RequestBody @Validated WithdrawRecordPageBo withdrawRecordPageBo) {
        withdrawRecordPageBo.setUserId(userId);
        IPage<WithdrawRecordVo> voIPage = withdrawRecordService.pageInfo(withdrawRecordPageBo);
        return Result.success(voIPage);
    }
}
