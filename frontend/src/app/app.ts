import { Component, signal } from '@angular/core';
import { PostListComponent } from './components/post-list/post-list.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
  imports: [PostListComponent],
})
export class App {
}
