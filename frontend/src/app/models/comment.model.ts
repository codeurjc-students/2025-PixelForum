import { User } from "./user.model";

export interface Comment {
    id?: number;
    content: string;
    createdAt?: string;
    updatedAt?: string;
    author?: User;
    parentComment?: Comment;
    likes?: number;
    hasUserLiked?: boolean;
    post?: number;
}