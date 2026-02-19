import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Post } from '../../models/post.model';
import { Topic } from '../../models/topic.model';
import { PostService } from '../../services/post.service';
import { TopicService } from '../../services/topic.service';
import { ImageService } from '../../services/image.service';

@Component({
	selector: 'app-create-post',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule],
	templateUrl: './create-post.component.html',
	styleUrls: ['./create-post.component.scss']
})
export class CreatePostComponent implements OnInit {
	@ViewChild('formContainer') formContainer!: ElementRef<HTMLDivElement>;
	@ViewChild('errorMessageRef') errorMessageRef!: ElementRef<HTMLDivElement>;
	createPostForm!: FormGroup;
	allTopics: Topic[] = [];
	filteredTopics: Topic[] = [];
	selectedImages: File[] = [];
	previewUrls: string[] = [];
	isLoading = false;
	errorMessage: string | null = null;
	successMessage: string | null = null;
	showTopicDropdown = false;
	selectedTopic: Topic | null = null;
	isDragOver = false;
	private readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

	constructor(
		private fb: FormBuilder,
		private postService: PostService,
		private topicService: TopicService,
		private imageService: ImageService
	) { }

	ngOnInit(): void {
		this.initializeForm();
		this.loadTopics();
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
			},
			error: (err) => {
				console.error('Error loading topics:', err);
				this.setError('Error loading topics');
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
		if (this.selectedImages.length + files.length > 10) {
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
				this.setError(`Each image must be smaller than 10 MB`);
				return;
			}

			this.selectedImages.push(file);

			const reader = new FileReader();
			reader.onload = (e) => {
				if (e.target?.result) {
					this.previewUrls.push(e.target.result as string);
				}
			};
			reader.readAsDataURL(file);
		});

		this.errorMessage = null;
	}


	removeImage(index: number): void {
		this.selectedImages.splice(index, 1);
		this.previewUrls.splice(index, 1);
	}

	async createPost(): Promise<void> {
		if (!this.createPostForm.valid || !this.selectedTopic) {
			this.setError('Please fill in all required fields correctly');
			return;
		}

		this.isLoading = true;
		this.errorMessage = null;
		this.successMessage = null;

		try {
			let imageUrls: string[] = [];

			// Upload images if any are selected
			if (this.selectedImages.length > 0) {
				try {
					const uploadResponse = await this.imageService
						.uploadImages(this.selectedImages, 'posts')
						.toPromise();

					if (uploadResponse?.urls && uploadResponse.urls.length > 0) {
						imageUrls = uploadResponse.urls;
					}
				} catch (uploadError) {
					console.error('Error uploading images:', uploadError);
					this.isLoading = false;
					this.setError('Error uploading images');
					return;
				}
			}

			// Create post
			const newPost: Post = {
				title: this.createPostForm.get('title')?.value,
				content: this.createPostForm.get('content')?.value,
				topic: this.selectedTopic
			};

			// Only add images if there are any
			if (imageUrls.length > 0) {
				newPost.images = imageUrls;
			}

			this.postService.create(newPost).subscribe({
				next: (createdPost) => {
					this.successMessage = 'Post created successfully!';
					this.createPostForm.reset();
					this.selectedImages = [];
					this.previewUrls = [];
					this.selectedTopic = null;
					this.isLoading = false;

					// Clear success message after 3 seconds
					setTimeout(() => {
						this.successMessage = null;
					}, 3000);
				},
				error: (err) => {
					console.error('Error creating post:', err);
					this.isLoading = false;
					this.setError('Error creating post');
				}
			});
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

}