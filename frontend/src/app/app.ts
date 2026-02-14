import { Component, signal } from '@angular/core';
import { HeaderComponent } from './components/header/header.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
  imports: [RouterOutlet, HeaderComponent, SidebarComponent],
})
export class App {
  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.authService.checkAuth().subscribe();
  }

}
