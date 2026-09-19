import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html'
})
export class LayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  correoUsuario = this.authService.getCorreo() || 'Usuario';
  rolUsuario = this.authService.getRol() || 'Operador';

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}