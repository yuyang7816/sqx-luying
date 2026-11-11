/** 服务端地址 VO → 页面展示结构 */
import { resolveAvatarUrl } from '@/common/constants/avatar';

export const mapAddressVoToUi = (vo = {}) => ({
	id: vo.id,
	name: vo.contactName || '',
	phone: vo.contactPhone || '',
	province: vo.province || '',
	city: vo.city || '',
	district: vo.district || '',
	detailAddress: vo.detailAddress || '',
	address: vo.fullAddress || [vo.province, vo.city, vo.district, vo.detailAddress].filter(Boolean).join(''),
	default: Number(vo.isDefault) === 1
});

/** 页面表单 → 服务端保存 BO */
export const mapAddressUiToSaveBo = (form = {}) => ({
	id: form.id ? Number(form.id) : undefined,
	contactName: String(form.name || '').trim(),
	contactPhone: String(form.phone || '').trim(),
	province: String(form.province || '').trim(),
	city: String(form.city || '').trim(),
	district: String(form.district || '').trim(),
	detailAddress: String(form.detailAddress || form.address || '').trim(),
	isDefault: form.default === true
});

/** 收藏 VO → 列表卡片 */
export const mapFavoriteVoToUi = (vo = {}) => ({
	id: vo.goodsId,
	favoriteId: vo.favoriteId,
	goodsNo: vo.goodsNo || '',
	name: vo.goodsName || '',
	image: vo.coverImage || '/static/logo.png',
	price: Number(vo.rentPriceDay || 0).toFixed(2),
	deposit: Number(vo.depositAmount || 0).toFixed(2),
	goodsStatus: vo.goodsStatus,
	offShelf: Number(vo.goodsStatus) !== 1,
	favoriteTime: vo.favoriteTime || ''
});

/** 用户资料 VO → 资料页表单 */
export const mapAppUserInfoToUi = (vo = {}) => ({
	userId: vo.userId || 0,
	userName: vo.userName || '',
	avatar: resolveAvatarUrl(vo.avatar),
	sex: vo.sex || 0,
	age: vo.age ? String(vo.age) : '',
	phoneMasked: vo.phoneMasked || '',
	memberLabel: Number(vo.isVip) === 1 ? 'VIP会员' : (vo.userTypeLabel || '普通会员'),
	invitationCode: vo.invitationCode || '',
	platform: vo.platform || '',
	createTime: vo.createTime || ''
});

/** 资料页表单 → 更新 BO */
export const mapProfileUiToUpdateBo = (form = {}) => ({
	userName: String(form.userName || '').trim(),
	avatar: String(form.avatar || '').trim(),
	sex: form.sex ? Number(form.sex) : undefined,
	age: form.age ? Number(form.age) : undefined
});
