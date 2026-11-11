/** 用户未上传头像时的默认展示图 */
export const DEFAULT_AVATAR_URL = 'https://tu.mianyazuju.com/2026/09/01/eed55c6a-a41e-4a4a-b61a-d29d1ded313a.png';

/** 解析用户头像地址，空值时回退默认头像 */
export const resolveAvatarUrl = (avatar) => {
	const value = String(avatar || '').trim();
	return value || DEFAULT_AVATAR_URL;
};
