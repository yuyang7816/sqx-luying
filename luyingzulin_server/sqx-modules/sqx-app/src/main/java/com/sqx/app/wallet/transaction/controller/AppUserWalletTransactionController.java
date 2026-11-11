package com.sqx.app.wallet.transaction.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.wallet.transaction.domain.bo.UserWalletTransactionInfoBo;
import com.sqx.app.wallet.transaction.domain.bo.UserWalletTransactionPageBo;
import com.sqx.app.wallet.transaction.domain.vo.UserWalletTransactionVo;
import com.sqx.app.wallet.transaction.service.UserWalletTransactionService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户钱包交易记录表 前端控制器
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-13 15:12:03
 */
@Tag(name = "用户端-用户钱包交易记录表", description = "用户端-用户钱包交易记录表")
@RestController
@RequestMapping("/app/userWalletTransaction")
public class AppUserWalletTransactionController {

    @Resource
    private UserWalletTransactionService userWalletTransactionService;

    /**
     * 列表
     */
    @Login
    @Operation(summary = "列表")
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserWalletTransactionVo.class)))
    @PostMapping("page")
    public Result<IPage<UserWalletTransactionVo>> page(@RequestAttribute("userId") Long userId,
                                                       @RequestBody @Validated UserWalletTransactionPageBo userWalletTransactionPageBo) {
        userWalletTransactionPageBo.setUserId(userId);
        IPage<UserWalletTransactionVo> voIPage = userWalletTransactionService.pageInfo(userWalletTransactionPageBo);
        return Result.success(voIPage);
    }

    /**
     * 信息
     */
    @Login
    @Operation(summary = "信息")
    @ApiResponse(content = @Content(schema = @Schema(implementation = UserWalletTransactionVo.class)))
    @PostMapping("info")
    public Result<UserWalletTransactionVo> info(@RequestAttribute("userId") Long userId,
                                                @RequestBody @Validated UserWalletTransactionInfoBo userWalletTransactionInfoBo) {
        UserWalletTransactionVo vo = userWalletTransactionService.info(userWalletTransactionInfoBo);
        if (vo != null && vo.getUserId() != null && !userId.equals(vo.getUserId())) {
            return Result.failed("无权查看该流水");
        }
        return Result.success(vo);
    }
}
