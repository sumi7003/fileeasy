import axios from 'axios';

const request = axios.create({
  baseURL: '/api/v1', // Proxy configured in vite.config.ts
  timeout: 600000, // 延长到 10 分钟以支持大文件上传
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // Handle global errors here (e.g. toast notification)
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

export default request;

