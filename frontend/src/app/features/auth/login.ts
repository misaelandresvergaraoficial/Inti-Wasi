import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth.service';
import { Icon } from '../../shared/icon';
import { fieldError, focusFirstInvalid } from '../../shared/form-errors';
import { ThemeService } from '../../core/theme.service';
@Component({
  selector: 'iw-login',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, Icon],
  templateUrl: './login.html',
})
export class Login {
  readonly theme = inject(ThemeService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly formElement = viewChild<ElementRef<HTMLFormElement>>('loginForm');
  readonly form = inject(FormBuilder).nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
  });
  readonly busy = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly expired = this.route.snapshot.queryParamMap.get('sesion') === 'caducada';
  readonly accountChanged = this.route.snapshot.queryParamMap.get('cuenta') === 'actualizada';
  readonly errorText = fieldError;
  async submit(): Promise<void> {
    if (this.busy()) return;
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focusFirstInvalid(this.formElement()!.nativeElement);
      return;
    }
    this.busy.set(true);
    try {
      const value = this.form.getRawValue();
      await this.auth.login(value.correo, value.contrasena);
      await this.router.navigate([this.auth.isAdmin() ? '/usuarios' : '/inicio']);
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'No se pudo iniciar sesión. Inténtalo de nuevo.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
