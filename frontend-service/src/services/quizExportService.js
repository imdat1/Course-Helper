import apiClient from './apiClient';

// Start generating a similar quiz for given course & source XML file
export const startSimilarQuizExport = async (courseId, fileId) => {
	return await apiClient.post(`/courses/${courseId}/quizzes/${fileId}/generate-similar`);
};

// Download exported quiz XML by export task id
export const downloadExportedQuiz = async (courseId, taskId) => {
	// Use raw response to handle blob
	const resp = await apiClient.get(`/courses/${courseId}/quizzes/exports/${taskId}/download`, {
		responseType: 'blob'
	});
	return resp;
};

export const getQuizExportTasks = async (courseId, fileId) => {
	return await apiClient.get(`/courses/${courseId}/quizzes/${fileId}/exports`);
};
