import { TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';
import { ImageService } from '../image.service';
import { AuthService } from '../auth.service';

jasmine.DEFAULT_TIMEOUT_INTERVAL = 30000;
jasmine.getEnv().configure({ random: false });

describe('ImageService - Integration Tests', () => {

    let service: ImageService;
    let authService: AuthService;

    beforeEach((done: DoneFn) => {
        TestBed.configureTestingModule({
            imports: [HttpClientModule],
            providers: [ImageService, AuthService]
        });

        service = TestBed.inject(ImageService);
        authService = TestBed.inject(AuthService);

        authService.login('martin', 'user').subscribe({
            next: () => done(),
            error: err => {
                fail('Login failed: ' + err.message);
                done();
            }
        });
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should upload image and return ids', (done: DoneFn) => {
        const file = new File(['test'], 'test.png', { type: 'image/png' });

        service.uploadImages([file]).subscribe({
            next: (response) => {
                expect(response).toBeTruthy();
                expect(response.length).toBeGreaterThan(0);
                expect(typeof response[0]).toBe('number');
                done();
            },
            error: (err) => {
                fail('Upload failed: ' + err.message);
                done();
            }
        });
    });

    it('should delete uploaded image', (done: DoneFn) => {
        const file = new File(['test'], 'test.png', { type: 'image/png' });

        service.uploadImages([file]).subscribe({
            next: (response) => {
                const imageId = response[0];

                service.deleteImage(Number(imageId)).subscribe({
                    next: () => {
                        service.deleteImage(Number(imageId)).subscribe({
                            next: () => {
                                fail('Image should not exist anymore');
                                done();
                            },
                            error: (err) => {
                                expect(err.status).toBe(500);
                                done();
                            }
                        });
                    }
                });
            }
        });
    });

});