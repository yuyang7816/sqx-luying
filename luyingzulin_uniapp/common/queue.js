export const getWxAppid = () => {
	return 'wxbf1429781694727b';
};
export const getInvitation = () => {
	return uni.getStorageSync('publicRelation');
};
export const getAppDownUrl = () => {
	return uni.getStorageSync('appurl');
};
export const publicYuMing = () => {
	return 'https://peizhen.xianmaxiong.com';
};