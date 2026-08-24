export interface RoomType {id:number;name:string;bedType:string;capacity:number;description:string;unitPrice:string;estimatedTotalAmount:string}
export interface Availability {hotel:{id:number;name:string;address:string};checkInDate:string;checkOutDate:string;nightCount:number;roomCount:number;roomTypes:RoomType[]}
export interface BookingRequest {roomTypeId:number;checkInDate:string;checkOutDate:string;roomCount:number;guestName:string;guestPhone:string}
export interface Order {orderNo:string;hotelName:string;roomTypeName:string;bedType:string;guestName:string;guestPhoneMasked:string;checkInDate:string;checkOutDate:string;roomCount:number;nightCount:number;unitPrice:string;totalAmount:string;status:'BOOKED'|'CHECKED_IN'|'CHECKED_OUT'|'CANCELLED';createdAt:string;checkedInAt:string|null;checkedInByName?:string|null}
export interface Session {authenticated:boolean;staff?:{displayName:string}|null;csrf?:{headerName:string;token:string}}
export interface Page<T>{items:T[];page:number;size:number;totalElements:number;totalPages:number}
export interface ApiError {code:string;message:string;traceId:string;fieldErrors?:Array<{field:string;message:string}>}
