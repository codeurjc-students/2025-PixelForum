import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Post } from '../../models/post.model';
import { Topic } from '../../models/topic.model';
import { PostService } from '../../services/post.service';
import { TopicService } from '../../services/topic.service';
import { Router } from '@angular/router';

@Component({
	selector: 'app-create-post',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule],
	templateUrl: './create-post.component.html',
	styleUrls: ['./create-post.component.scss']
})
export class CreatePostComponent implements OnInit {
	createPostForm!: FormGroup;
	topics: Topic[] = [];
	selectedImages: File[] = [];
	previewUrls: string[] = [];
	isLoading = false;
	errorMessage: string | null = null;
	successMessage: string | null = null;

	constructor(
		private fb: FormBuilder,
		private postService: PostService,
		private topicService: TopicService,
		private router: Router
	) { }

	ngOnInit(): void {
		this.initializeForm();
		this.loadTopics();
	}

	private initializeForm(): void {
		this.createPostForm = this.fb.group({
			title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(300)]],
			content: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(5000)]],
			topics: ['', Validators.required]
		});
	}

	private loadTopics(): void {
		this.topicService.getAll().subscribe({
			next: (data) => {
				this.topics = data;
			},
			error: (err) => {
				console.error('Error loading topics:', err);
				this.errorMessage = 'Error loading topics';
			}
		});
	}

	onImageSelected(event: Event): void {
		const input = event.target as HTMLInputElement;
		if (input.files) {
			const files = Array.from(input.files);

			// Validate maximum number of images
			if (this.selectedImages.length + files.length > 5) {
				this.errorMessage = 'Máximo 5 imágenes permitidas';
				return;
			}

			// Validate file types
			files.forEach((file) => {
				if (!file.type.startsWith('image/')) {
					this.errorMessage = 'Only image files are allowed';
					return;
				}

				this.selectedImages.push(file);

				// Create preview URL
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
	}

	removeImage(index: number): void {
		this.selectedImages.splice(index, 1);
		this.previewUrls.splice(index, 1);
	}

	selectTopic(event: Event): void {
		const selectElement = event.target as HTMLSelectElement;
		const topicId = selectElement.value ? parseInt(selectElement.value, 10) : '';
		this.createPostForm.patchValue({
			topics: topicId
		});
	}

	async createPost(): Promise<void> {
		if (!this.createPostForm.valid) {
			this.errorMessage = 'Please fill in all required fields correctly';
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
					const uploadResponse = await this.postService
						.uploadImages(this.selectedImages)
						.toPromise();

					if (uploadResponse?.urls) {
						imageUrls = uploadResponse.urls;
					}
				} catch (uploadError) {
					console.error('Error uploading images:', uploadError);
					this.errorMessage = 'Error uploading images';
					this.isLoading = false;
					return;
				}
			}

			// Create post
			const formValue = this.createPostForm.value;
			const selectedTopicId = formValue.topics;

			const newPost: Post = {
				title: formValue.title,
				content: formValue.content,
				topic: {
					id: formValue.topics,
					name: ''
				},
				images: imageUrls
			};

			this.postService.create(newPost).subscribe({
				next: (createdPost) => {
					this.successMessage = 'Post created successfully!';
					this.createPostForm.reset();
					this.selectedImages = [];
					this.previewUrls = [];
					this.isLoading = false;

					// Clear success message after 3 seconds
					setTimeout(() => {
						this.successMessage = null;
						this.router.navigate(['/posts']);
					}, 3000);
				},
				error: (err) => {
					console.error('Error creating post:', err);
					this.errorMessage = 'Error creating post';
					this.isLoading = false;
				}
			});
		} catch (error) {
			console.error('Error:', error);
			this.errorMessage = 'An unexpected error occurred';
			this.isLoading = false;
		}
	}

	get titleLength(): number {
		return this.createPostForm.get('title')?.value?.length || 0;
	}

	get contentLength(): number {
		return this.createPostForm.get('content')?.value?.length || 0;
	}
}