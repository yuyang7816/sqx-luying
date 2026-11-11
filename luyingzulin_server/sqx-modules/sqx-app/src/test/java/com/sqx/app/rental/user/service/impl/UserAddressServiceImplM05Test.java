package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.rental.user.domain.bo.UserAddressSaveBo;
import com.sqx.app.rental.user.domain.entity.UserAddress;
import com.sqx.app.rental.user.domain.vo.UserAddressVo;
import com.sqx.app.rental.user.mapper.UserAddressMapper;
import com.sqx.common.core.exception.BizException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * M05：地址保存与删除边界单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplM05Test {

    @Mock
    private UserAddressMapper userAddressMapper;

    @InjectMocks
    private UserAddressServiceImpl userAddressService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                UserAddress.class);
    }

    @Test
    void saveShouldCreateDefaultAddressWhenFirstOne() {
        UserAddressSaveBo saveBo = new UserAddressSaveBo();
        saveBo.setContactName("张露营");
        saveBo.setContactPhone("13812348888");
        saveBo.setProvince("陕西省");
        saveBo.setCity("西安市");
        saveBo.setDistrict("雁塔区");
        saveBo.setDetailAddress("科技路 198 号");

        when(userAddressMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(1L);
            return 1;
        }).when(userAddressMapper).insert(any(UserAddress.class));
        when(userAddressMapper.selectOne(any())).thenAnswer(invocation -> {
            UserAddress address = new UserAddress();
            address.setId(1L);
            address.setUserId(1L);
            address.setContactName("张露营");
            address.setContactPhone("13812348888");
            address.setProvince("陕西省");
            address.setCity("西安市");
            address.setDistrict("雁塔区");
            address.setDetailAddress("科技路 198 号");
            address.setIsDefault(1);
            return address;
        });

        UserAddressVo vo = userAddressService.save(1L, saveBo);

        assertEquals(1, vo.getIsDefault());
        assertTrue(vo.getFullAddress().contains("科技路 198 号"));
    }

    @Test
    void saveNewDefaultShouldClearPreviousDefaultWhenMultipleExist() {
        UserAddressSaveBo saveBo = new UserAddressSaveBo();
        saveBo.setContactName("李四");
        saveBo.setContactPhone("13912341234");
        saveBo.setProvince("陕西省");
        saveBo.setCity("西安市");
        saveBo.setDistrict("雁塔区");
        saveBo.setDetailAddress("高新路 1 号");
        saveBo.setIsDefault(true);

        when(userAddressMapper.selectCount(any())).thenReturn(2L);
        doAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(2L);
            return 1;
        }).when(userAddressMapper).insert(any(UserAddress.class));
        when(userAddressMapper.selectOne(any())).thenAnswer(invocation -> {
            UserAddress address = new UserAddress();
            address.setId(2L);
            address.setUserId(1L);
            address.setContactName("李四");
            address.setContactPhone("13912341234");
            address.setProvince("陕西省");
            address.setCity("西安市");
            address.setDistrict("雁塔区");
            address.setDetailAddress("高新路 1 号");
            address.setIsDefault(1);
            return address;
        });

        UserAddressVo vo = userAddressService.save(1L, saveBo);

        assertEquals(1, vo.getIsDefault());
        verify(userAddressMapper).update(isNull(), any());
    }

    @Test
    void setDefaultShouldEnsureSingleDefaultAddress() {
        UserAddress address = new UserAddress();
        address.setId(3L);
        address.setUserId(1L);
        address.setIsDefault(0);

        when(userAddressMapper.selectOne(any())).thenReturn(address);

        userAddressService.setDefault(1L, 3L);

        verify(userAddressMapper, times(2)).update(isNull(), any());
    }

    @Test
    void deleteShouldRejectWhenOnlyOneAddressLeft() {
        UserAddress address = new UserAddress();
        address.setId(5L);
        address.setUserId(1L);
        address.setIsDefault(1);

        when(userAddressMapper.selectOne(any())).thenReturn(address);
        when(userAddressMapper.selectCount(any())).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> userAddressService.delete(1L, 5L));
        assertTrue(ex.getMessage().contains("至少保留一个"));
    }
}
