import axios from 'axios'; import type {ApiError} from '../types';
export const client=axios.create({baseURL:'/api/v1',withCredentials:true,headers:{'Content-Type':'application/json'}});
let csrf:{headerName:string;token:string}|null=null;
export function setCsrf(value:{headerName:string;token:string}){csrf=value;}
client.interceptors.request.use(config=>{if(csrf&&config.method&&['post','put','patch','delete'].includes(config.method))config.headers.set(csrf.headerName,csrf.token);return config;});
export function errorMessage(error:unknown){if(axios.isAxiosError<ApiError>(error)){if(error.response)return error.response.data?.message??`请求失败（${error.response.status}）`;return '无法连接服务器，请确认后端已启动';}return '操作失败，请稍后重试';}
