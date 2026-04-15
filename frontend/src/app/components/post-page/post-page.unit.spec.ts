import { ComponentFixture, TestBed } from "@angular/core/testing";
import { PostPageComponent } from "./post-page.component";
import { HttpClientTestingModule } from "@angular/common/http/testing";

describe('PostPageComponent - Unit Tests', () => {

    let component: PostPageComponent;
    let fixture: ComponentFixture<PostPageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PostPageComponent, HttpClientTestingModule]
        }).compileComponents();

        fixture = TestBed.createComponent(PostPageComponent);
        component = fixture.componentInstance;
    });

    it('should create component', () => {
        expect(component).toBeTruthy();
    });
});