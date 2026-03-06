export interface User {
	id: number;
	username: string;
	email?: string;
	roles: ('USER' | 'ADMIN')[];
}