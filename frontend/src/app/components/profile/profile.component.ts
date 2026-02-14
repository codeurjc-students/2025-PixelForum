import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent {

  username: string | null = null;

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
      this.authService.me().subscribe({
      next: user => this.username = user.username,
      error: () => this.username = null
    });
  }

}