import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PostListComponent } from '../post-list/post-list.component';

@Component({
    selector: 'app-post-page',
    standalone: true,
    imports: [CommonModule, PostListComponent],
    templateUrl: './post-page.component.html',
    styleUrls: ['./post-page.component.scss']
})
export class PostPageComponent {

}