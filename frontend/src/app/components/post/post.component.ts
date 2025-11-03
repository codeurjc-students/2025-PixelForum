import { Component, Input } from "@angular/core";
import { Post } from "../../models/post.model";
import { CommonModule, DatePipe } from '@angular/common';

@Component({
  selector: 'app-post',
  standalone: true,
  templateUrl: './post.component.html',
  styleUrls: ['./post.component.scss'],
  imports: [
    CommonModule,
    DatePipe]
})

export class PostComponent {
  @Input() post!: Post;}