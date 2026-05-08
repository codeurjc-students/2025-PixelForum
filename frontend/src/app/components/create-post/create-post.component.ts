import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Post } from '../../models/post.model';
import { Topic } from '../../models/topic.model';
import { PostService } from '../../services/post.service';
import { TopicService } from '../../services/topic.service';
import { ImageService } from '../../services/image.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs/internal/firstValueFrom';
import { ErrorService } from '../../services/error.service';
import { AuthService } from '../../services/auth.service';

@Component({
	selector: 'app-create-post',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule],
	templateUrl: './create-post.component.html',
	styleUrls: ['./create-post.component.scss']
})
export class CreatePostComponent implements OnInit {
	@ViewChild('errorMessageRef') errorMessageRef!: ElementRef<HTMLDivElement>;
	createPostForm!: FormGroup;
	allTopics: Topic[] = [];
	filteredTopics: Topic[] = [];
	selectedImages: File[] = []; // Files selected for upload
	previewImages: string[] = []; // Existing image URLs and new image previews
	isLoading = false;
	errorMessage: string | null = null;
	showTopicDropdown = false;
	selectedTopic: Topic | null = null;
	isDragOver = false;

	isEditMode = false;
	postId: number | null = null;
	existingPost: Post | null = null;
	isLoadingPost = false;
	existingImageIds: string[] = []; // IDs of existing images for edit mode

	private readonly MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

	constructor(
		private fb: FormBuilder,
		private postService: PostService,
		private topicService: TopicService,
		private imageService: ImageService,
		private authService: AuthService,
		private errorService: ErrorService,
		private route: ActivatedRoute,
		private router: Router,
		private snackBar: MatSnackBar
	) { }

	ngOnInit(): void {
		this.initializeForm();
		this.loadTopics();

		this.route.params.subscribe(params => {
			this.postId = params['id'];
			if (this.postId) {
				this.isEditMode = true;
				this.loadPostForEdit();
			}
		});
	}

	private initializeForm(): void {
		this.createPostForm = this.fb.group({
			title: ['', [Validators.required, Validators.maxLength(300)]],
			content: [null, [Validators.maxLength(5000)]],
			topicSearch: ['']
		});
	}

	private loadTopics(): void {
		this.topicService.getAll().subscribe({
			next: (data) => {
				this.allTopics = data;
				this.filteredTopics = data;
			}
		});
	}

	get isTopicInvalid(): boolean {
		const control = this.createPostForm.get('topicSearch');
		return (
			!this.selectedTopic &&
			!!control?.touched &&
			!this.showTopicDropdown
		);
	}

	onTopicSearchChange(event: Event): void {
		const input = event.target as HTMLInputElement;
		const searchTerm = input.value.toLowerCase();

		if (searchTerm) {
			this.filteredTopics = this.allTopics.filter(topic =>
				topic.name.toLowerCase().includes(searchTerm)
			);
			this.showTopicDropdown = true;
		} else {
			this.filteredTopics = this.allTopics;
			this.showTopicDropdown = this.allTopics.length > 0;
		}
	}

	onTopicFocus(): void {
		const searchTerm = this.createPostForm.get('topicSearch')?.value?.toLowerCase() || '';

		if (searchTerm) {
			this.filteredTopics = this.allTopics.filter(topic =>
				topic.name.toLowerCase().includes(searchTerm)
			);
		} else if (this.selectedTopic) {
			const term = this.selectedTopic.name.toLowerCase();
			this.filteredTopics = this.allTopics.filter(topic =>
				topic.name.toLowerCase().includes(term)
			);
		} else {
			this.filteredTopics = this.allTopics;
		}

		this.showTopicDropdown = true;
	}

	onTopicBlur(): void {
		setTimeout(() => {
			this.showTopicDropdown = false;
			this.createPostForm.get('topicSearch')?.markAsTouched();
		}, 150);
	}

	selectTopic(topic: Topic): void {
		this.selectedTopic = topic;
		this.createPostForm.patchValue({
			topicSearch: topic.name
		});
		this.createPostForm.get('topicSearch')?.markAsUntouched();
		this.showTopicDropdown = false;
	}

	clearSelectedTopic(): void {
		this.selectedTopic = null;
		this.createPostForm.patchValue({
			topicSearch: ''
		});
		this.filteredTopics = this.allTopics;
	}

	onDragOver(event: DragEvent): void {
		event.preventDefault();
		event.stopPropagation();
		this.isDragOver = true;
	}

	onDragLeave(event: DragEvent): void {
		event.preventDefault();
		event.stopPropagation();
		this.isDragOver = false;
	}

	onDrop(event: DragEvent): void {
		event.preventDefault();
		event.stopPropagation();
		this.isDragOver = false;

		if (event.dataTransfer?.files) {
			this.handleFiles(Array.from(event.dataTransfer.files));
		}
	}

	onImageSelected(event: Event): void {
		const input = event.target as HTMLInputElement;
		if (input.files) {
			this.handleFiles(Array.from(input.files));
			input.value = '';
		}
	}

	private handleFiles(files: File[]): void {
		if (this.previewImages.length + files.length > 10) {
			this.setError('You can only upload up to 10 images');
			return;
		}

		files.forEach((file) => {
			// Validate type
			if (!file.type.startsWith('image/')) {
				this.setError('Only image files are allowed');
				return;
			}

			// Validate size
			if (file.size > this.MAX_FILE_SIZE) {
				this.setError('Each image must be smaller than 5 MB');
				return;
			}

			this.selectedImages.push(file);

			const reader = new FileReader();
			reader.onload = (e) => {
				if (e.target?.result) {
					this.previewImages.push(e.target.result as string);
				}
			};
			reader.readAsDataURL(file);
		});

		this.errorMessage = null;
	}


	removeImage(index: number): void {
		// If it's an existing image remove from existing list
		if (!this.previewImages[index].startsWith('data:')) {
			this.existingImageIds.splice(index, 1);
		} else { // If it's a new image remove from selected images
			this.selectedImages.splice(index - this.existingImageIds.length, 1);
		}
		this.previewImages.splice(index, 1);
	}

	async createPost(): Promise<void> {
		if (!this.createPostForm.valid || !this.selectedTopic) {
			this.setError('Please fill in all required fields correctly');
			return;
		}

		this.isLoading = true;
		this.errorMessage = null;

		try {
			let newImageIds: string[] = [];

			// Upload images if any are selected
			if (this.selectedImages.length > 0) {
				try {
					const uploadResponse = await firstValueFrom(this.imageService
						.uploadImages(this.selectedImages));

					if (uploadResponse && uploadResponse.length > 0) {
						newImageIds = uploadResponse;
					}
				} catch (uploadError) {
					console.error('Error uploading images:', uploadError);
					this.isLoading = false;
					this.setError('Error uploading images');
					return;
				}
			}

			const allImageIds = [...this.existingImageIds, ...newImageIds];

			// Create post
			const postData: Post = {
				title: this.createPostForm.get('title')?.value,
				content: this.createPostForm.get('content')?.value || null,
				topic: this.selectedTopic,
				images: allImageIds.length > 0 ? allImageIds : undefined
			};

			if (this.isEditMode && this.postId) {
				const postToUpdate: Post = {
					...this.existingPost,
					...postData,
					id: this.postId
				};

				this.postService.update(postToUpdate).subscribe({
					next: () => {
						this.isLoading = false;
						this.snackBar.open('Post updated successfully', 'Close', {
							duration: 3000
						});
						this.router.navigate(['/posts', this.postId]);
					}
				});
			} else {
				this.postService.create(postData).subscribe({
					next: (createdPost) => {
						this.isLoading = false;
						this.snackBar.open('Post created successfully', 'Close', {
							duration: 3000
						});
						this.router.navigate(['/posts', createdPost.id]);
					}
				});
			}
		} catch (error) {
			console.error('Error:', error);
			this.isLoading = false;
			this.setError('An unexpected error occurred');
		}
	}

	get titleLength(): number {
		return this.createPostForm.get('title')?.value?.length || 0;
	}

	get contentLength(): number {
		return this.createPostForm.get('content')?.value?.length || 0;
	}

	setError(message: string): void {
		this.errorMessage = message;

		// Wait a tick for angular to render
		setTimeout(() => {
			if (this.errorMessageRef) {
				this.errorMessageRef.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
			}
		}, 0);
	}

	private loadPostForEdit(): void {
		this.isLoadingPost = true;

		this.postService.getById(this.postId!).subscribe({
			next: (post) => {
				this.authService.checkAuth().subscribe({
					next: (user) => {
						if (!user) {
							this.router.navigate(['/login']);
							return;
						}

						const isOwner = post.author?.id === user.id;
						const isAdmin = user.roles.includes('ADMIN');

						if (!isOwner && !isAdmin) {
							this.errorService.setError(403, 'Forbidden', "You don't have permission to edit this post");
							this.router.navigate(['/error']);
							return;
						}

						this.existingPost = post;
						this.populateFormWithPost(post);
						this.isLoadingPost = false;
					}
				});
			}
		});
	}

	private populateFormWithPost(post: Post): void {
		this.createPostForm.patchValue({
			title: post.title,
			content: post.content || '',
			topicSearch: post.topic?.name || ''
		});

		if (post.topic) {
			this.selectedTopic = post.topic;
		}

		if (post.images && post.images.length > 0) {
			this.previewImages = post.images.map(id => `/api/v1/images/${id}`);
			this.existingImageIds = [...post.images];
		}
	}

	onCancel(): void {
		this.router.navigate(['/posts']);
	}
}