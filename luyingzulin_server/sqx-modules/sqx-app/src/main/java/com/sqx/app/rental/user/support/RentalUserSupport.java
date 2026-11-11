package com.sqx.app.rental.user.support;

import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.user.support.UserAvatarSupport;
import com.sqx.app.rental.user.domain.entity.UserAddress;
import com.sqx.app.rental.user.domain.vo.AdminUserListVo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;
import com.sqx.app.rental.user.domain.vo.UserAddressVo;
import org.apache.commons.lang3.StringUtils;

/**
 * 用户账户 VO 转换工具
 */
public final class RentalUserSupport {

    private RentalUserSupport() {
    }

    public static String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String buildFullAddress(String province, String city, String district, String detail) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, province);
        appendPart(builder, city);
        appendPart(builder, district);
        appendPart(builder, detail);
        return builder.toString();
    }

    private static void appendPart(StringBuilder builder, String part) {
        if (StringUtils.isBlank(part)) {
            return;
        }
        builder.append(part.trim());
    }

    public static AppUserInfoVo toAppUserInfoVo(UserEntity entity) {
        AppUserInfoVo vo = new AppUserInfoVo();
        vo.setUserId(entity.getUserId());
        vo.setUserName(entity.getUserName());
        vo.setAvatar(UserAvatarSupport.resolveAvatar(entity.getAvatar()));
        vo.setSex(entity.getSex());
        vo.setAge(entity.getAge());
        vo.setPhoneMasked(maskPhone(entity.getPhone()));
        vo.setUserType(entity.getUserType());
        vo.setUserTypeLabel(resolveUserTypeLabel(entity.getUserType()));
        vo.setIsVip(resolveIsVip(entity.getUserType()));
        vo.setInvitationCode(entity.getInvitationCode());
        vo.setPlatform(entity.getPlatform());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    public static AdminUserListVo toAdminUserListVo(UserEntity entity) {
        AdminUserListVo vo = new AdminUserListVo();
        vo.setUserId(entity.getUserId());
        vo.setUserName(entity.getUserName());
        vo.setPhone(entity.getPhone());
        vo.setPhoneMasked(maskPhone(entity.getPhone()));
        vo.setAvatar(UserAvatarSupport.resolveAvatar(entity.getAvatar()));
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(resolveStatusLabel(entity.getStatus()));
        vo.setUserType(entity.getUserType());
        vo.setUserTypeLabel(resolveUserTypeLabel(entity.getUserType()));
        vo.setIsVip(resolveIsVip(entity.getUserType()));
        vo.setPlatform(entity.getPlatform());
        vo.setInvitationCode(entity.getInvitationCode());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /** 管理端用户详情（含明文手机号） */
    public static AppUserInfoVo toAdminAppUserInfoVo(UserEntity entity) {
        AppUserInfoVo vo = toAppUserInfoVo(entity);
        vo.setPhone(entity.getPhone());
        vo.setSuperiorInvitationCode(entity.getSuperiorInvitationCode());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    public static UserAddressVo toAddressVo(UserAddress address) {
        UserAddressVo vo = new UserAddressVo();
        vo.setId(address.getId());
        vo.setContactName(address.getContactName());
        vo.setContactPhone(address.getContactPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setFullAddress(buildFullAddress(
                address.getProvince(), address.getCity(), address.getDistrict(), address.getDetailAddress()));
        vo.setIsDefault(address.getIsDefault());
        vo.setCreateTime(address.getCreateTime());
        return vo;
    }

    public static String resolveUserTypeLabel(Integer userType) {
        if (userType == null) {
            return "普通用户";
        }
        if (Integer.valueOf(2).equals(userType)) {
            return "VIP用户";
        }
        return "普通用户";
    }

    public static Integer resolveIsVip(Integer userType) {
        return Integer.valueOf(2).equals(userType) ? 1 : 0;
    }

    public static String resolveStatusLabel(Integer status) {
        if (Integer.valueOf(2).equals(status)) {
            return "禁用";
        }
        return "正常";
    }
}
