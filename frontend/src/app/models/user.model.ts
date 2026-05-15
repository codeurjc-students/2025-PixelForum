export interface User {
	id: number;
	username: string;
	email?: string;
	createdAt?: string;
	bio?: string;
	avatar?: string;
	roles: ('USER' | 'ADMIN')[];
}