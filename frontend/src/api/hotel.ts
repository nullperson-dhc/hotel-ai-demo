import {client,setCsrf} from './client'; import type {Availability,BookingRequest,Order,Page,Session} from '../types';
let sessionRequest:Promise<Session>|null=null;
export async function availability(params:{checkInDate:string;checkOutDate:string;roomCount:number}){return (await client.get<Availability>('/availability',{params})).data;}
export async function createBooking(data:BookingRequest,key:string){return (await client.post<Order>('/bookings',data,{headers:{'Idempotency-Key':key}})).data;}
export async function guestOrder(orderNo:string,guestPhone:string){return (await client.post<Order>('/bookings/query',{orderNo,guestPhone})).data;}
export async function session(){if(!sessionRequest)sessionRequest=client.get<Session>('/staff/session').then(response=>{const value=response.data;if(value.csrf)setCsrf(value.csrf);return value;}).finally(()=>{sessionRequest=null;});return sessionRequest;}
export async function login(username:string,password:string){await session();const value=(await client.post<Session>('/staff/session',{username,password})).data;if(value.csrf)setCsrf(value.csrf);return value;}
export async function logout(){await client.delete('/staff/session');}
export async function staffOrders(params:{orderNo?:string;guestPhone?:string}){return (await client.get<Page<Order>>('/staff/bookings',{params})).data;}
export async function checkIn(orderNo:string){await session();return (await client.post<Order>(`/staff/bookings/${encodeURIComponent(orderNo)}/check-in`)).data;}
