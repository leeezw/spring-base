// dict API
import request from './index.js';

export const pageDict = (params) => request.get('/dict/page', { params });
export const getDict = (id) => request.get(`/dict/${id}`);
export const createDict = (data) => request.post('/dict', data);
export const updateDict = (data) => request.put('/dict', data);
export const deleteDict = (id) => request.delete(`/dict/${id}`);
