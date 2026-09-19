import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth'; // <-- Apunta a auth.ts

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  correo = '';
  contrasena = '';
  errorMessage = '';
  loading = false;

  onLogin(): void {
    if (!this.correo || !this.contrasena) {
      this.errorMessage = 'Debe completar todos los campos';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.login({ correo: this.correo, contrasena: this.contrasena }).subscribe({
      next: (res) => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
        // En el próximo paso haremos la redirección al dashboard
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Credenciales incorrectas o error de servidor';
      }
    });
  }
}