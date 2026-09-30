import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { UsersService } from '../../core/users.service';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { UsuarioResponse } from '../../core/models';
import { Icon } from '../../shared/icon';
import { ConfirmDialog } from '../../shared/confirm-dialog';
@Component({
  selector: 'iw-users-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './users-list.html',
})
export class UsersList {
  private readonly users = inject(UsersService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);
  readonly notice = inject(NoticeService);
  readonly records = signal<UsuarioResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busyId = signal<number | null>(null);
  readonly query = signal('');
  readonly role = signal('');
  readonly status = signal('');
  readonly page = signal(0);
  readonly pageSize = 6;
  readonly filtered = computed(() =>
    this.records().filter(
      (user) =>
        (!this.role() || user.rol === this.role()) &&
        (!this.status() || user.estado === Number(this.status())) &&
        `${user.nomUsuario} ${user.correo}`
          .toLocaleLowerCase()
          .includes(this.query().trim().toLocaleLowerCase()),
    ),
  );
  readonly visible = computed(() =>
    this.filtered().slice(this.page() * this.pageSize, (this.page() + 1) * this.pageSize),
  );
  readonly pages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize)));
  readonly admins = computed(
    () => this.records().filter((user) => user.estado === 1 && user.rol === 'Administrador').length,
  );
  readonly active = computed(() => this.records().filter((user) => user.estado === 1).length);
  readonly rangeEnd = computed(() =>
    Math.min((this.page() + 1) * this.pageSize, this.filtered().length),
  );
  constructor() {
    void this.load();
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.records.set(await this.users.list());
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'No se pudieron cargar los usuarios.',
      );
    } finally {
      this.loading.set(false);
    }
  }
  search(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }
  filterRole(value: string): void {
    this.role.set(value);
    this.page.set(0);
  }
  filterStatus(value: string): void {
    this.status.set(value);
    this.page.set(0);
  }
  reset(): void {
    this.query.set('');
    this.role.set('');
    this.status.set('');
    this.page.set(0);
  }
  initials(name: string): string {
    return name
      .split(' ')
      .slice(0, 2)
      .map((word) => word[0])
      .join('');
  }
  async deactivate(user: UsuarioResponse): Promise<void> {
    const own = user.idUsuario === this.auth.session()?.idUsuario;
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Desactivar a ${user.nomUsuario}?`,
            message: `Esta persona perderá el acceso al sistema. Su información y el historial de sus operaciones se conservarán.${own ? ' Estás desactivando tu propia cuenta; se cerrará tu sesión.' : ''}`,
            action: 'Desactivar usuario',
            danger: true,
          },
        })
        .afterClosed(),
    );
    if (!confirmed) return;
    this.busyId.set(user.idUsuario);
    this.error.set('');
    this.notice.clear();
    try {
      await this.users.deactivate(user.idUsuario);
      if (own) {
        this.auth.logout();
        return;
      }
      this.records.update((users) =>
        users.map((record) =>
          record.idUsuario === user.idUsuario ? { ...record, estado: 0 } : record,
        ),
      );
      this.afterStateChange(user.idUsuario);
      this.notice.show(
        `${user.nomUsuario} fue desactivado. La cuenta permanece en el listado y puede reactivarse.`,
      );
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo desactivar al usuario.');
    } finally {
      this.busyId.set(null);
    }
  }
  async activate(user: UsuarioResponse): Promise<void> {
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Reactivar a ${user.nomUsuario}?`,
            message:
              'Esta persona podrá iniciar sesión nuevamente con sus credenciales y el rol que tiene asignado.',
            action: 'Reactivar usuario',
          },
        })
        .afterClosed(),
    );
    if (!confirmed) return;
    this.busyId.set(user.idUsuario);
    this.error.set('');
    this.notice.clear();
    try {
      await this.users.activate(user.idUsuario);
      this.records.update((users) =>
        users.map((record) =>
          record.idUsuario === user.idUsuario ? { ...record, estado: 1 } : record,
        ),
      );
      this.afterStateChange(user.idUsuario);
      this.notice.show(`${user.nomUsuario} fue reactivado y ya puede iniciar sesión.`);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo reactivar al usuario.');
    } finally {
      this.busyId.set(null);
    }
  }
  private afterStateChange(id: number): void {
    if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    requestAnimationFrame(() => {
      const action = document.querySelector<HTMLElement>(`[data-state-action="${id}"]`);
      (action ?? document.getElementById('user-status'))?.focus();
    });
  }
}
