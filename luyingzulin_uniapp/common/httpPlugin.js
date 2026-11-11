import {
	get,
	getT,
	post,
	postT,
	postJson,
	resolveBusinessResponse,
	isBusinessSuccess
} from './httpRequest.js';

const httpPlugin = {
	install(app) {
		app.config.globalProperties.$http = {
			get,
			post,
			postT,
			postJson,
			getT,
			resolveBusinessResponse,
			isBusinessSuccess
		};
	}
};

export default httpPlugin;
