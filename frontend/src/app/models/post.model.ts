import { Topic } from "./topic.model";
import { User } from "./user.model";

export interface Post {
	id?: number;
	title: string;
	content: string;
	createdAt?: string;
	updatedAt?: string;
	images?: string[];
	author?: User;
	topics: Topic[];
	likes?: number;
	usersThatLiked?: number[];
}