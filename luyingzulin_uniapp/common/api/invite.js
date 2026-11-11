import { resolveAvatarUrl } from '@/common/constants/avatar';
import { get, resolveBusinessResponse } from '@/common/httpRequest';
import { buildInviteRegisterPath, buildInviteRegisterUrl } from '@/common/auth';
import { fetchInvitePosterConfigRemote } from '@/common/api/content';
import { resolvePageData } from '@/common/utils/pageData';

/** 分销汇总数据 */
export async function fetchReferralData() {
	return get('app/referral/userCommission/referralData').then(resolveBusinessResponse);
}

/** 佣金账户信息 */
export async function fetchReferralCommissionInfo() {
	return get('app/referral/userCommission/info').then(resolveBusinessResponse);
}

/** 邀请用户列表 */
export async function fetchReferralUserList(params = {}) {
	return get('app/referral/userCommission/referralUserList', {
		page: params.page || 1,
		limit: params.limit || 20
	}).then(resolveBusinessResponse).then(resolvePageData);
}

/** 收益奖励列表 */
export async function fetchReferralRewardList(params = {}) {
	return get('app/referral/userCommission/referralList', {
		page: params.page || 1,
		limit: params.limit || 20
	}).then(resolveBusinessResponse).then(resolvePageData);
}

/** 当前用户邀请资料 */
export async function fetchInviteProfile() {
	const session = uni.getStorageSync('camp-auth-session');
	const user = session?.userInfo || {};
	const inviteCode = user.inviteCode || '';
	const posterConfig = await fetchInvitePosterConfigRemote();
	return {
		userId: user.userId,
		nickname: user.nickname,
		inviteCode,
		invitePath: inviteCode ? buildInviteRegisterPath(inviteCode) : '',
		inviteUrl: inviteCode ? buildInviteRegisterUrl(inviteCode) : '',
		inviteRewardText: '好友消费后可获得分销佣金'
	};
}

/** 团队数据概览 */
export async function fetchTeamOverview() {
	const data = await get('app/referral/userCommission/referralTeamOverview').then(resolveBusinessResponse);
	return {
		level1Count: Number(data?.level1Count || 0),
		level2Count: Number(data?.level2Count || 0),
		totalInvite: Number(data?.level1Count || 0) + Number(data?.level2Count || 0),
		totalCommission: Number(data?.totalCommission || 0),
		level1Commission: Number(data?.level1Commission || 0),
		level2Commission: Number(data?.level2Commission || 0),
		level1RatePercent: Number(data?.level1RatePercent || 0),
		level2RatePercent: Number(data?.level2RatePercent || 0)
	};
}

/** 团队成员列表（按层级） */
export async function fetchTeamMembers(level = 1, params = {}) {
	const pageVo = await get('app/referral/userCommission/referralTeamUserList', {
		level,
		page: params.page || 1,
		limit: params.limit || 50
	}).then(resolveBusinessResponse);
	const { list } = resolvePageData(pageVo);
	return (list || []).map((item, index) => ({
		id: item.userId || index,
		name: item.nickname || '用户',
		avatar: resolveAvatarUrl(item.avatar),
		time: formatTeamTime(item.createTime)
	}));
}

function formatTeamTime(value) {
	if (!value) return '--';
	return String(value).replace('T', ' ').slice(0, 10);
}

/** 邀请记录 */
export async function fetchInviteRecords() {
	const { list } = await fetchReferralUserList({ page: 1, limit: 50 });
	return (list || []).map((item) => ({
		name: item.userName || item.nickname,
		time: item.createTime,
		status: item.statusText || '已注册',
		rewardAmount: item.amount || 0
	}));
}

/** 佣金记录 */
export async function fetchCommissionRecords(params = {}) {
	const { list } = await fetchReferralRewardList(params);
	return (list || []).map((item, index) => ({
		id: index,
		name: item.userName || '下级用户',
		avatar: resolveAvatarUrl(item.avatar),
		amount: item.amount,
		commissionAmount: item.amount,
		time: formatTeamTime(item.createTime),
		level: String(item.title || '').includes('二级') ? 2 : 1,
		settleStatus: '已到账'
	}));
}

/** 邀请统计 */
export async function fetchInviteStats() {
	const data = await fetchReferralData();
	return {
		totalInvite: Number(data?.num || 0),
		convertedCount: 0,
		totalReward: Number(data?.commission || 0),
		totalCommission: Number(data?.commission || 0)
	};
}
