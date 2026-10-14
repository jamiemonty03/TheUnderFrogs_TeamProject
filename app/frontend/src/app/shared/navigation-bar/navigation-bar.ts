import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../services/auth';

@Component({
  imports: [RouterLink, RouterLinkActive, MatButtonModule, MatMenuModule],
  selector: 'app-navigation-bar',
  styleUrl: './navigation-bar.scss',
  templateUrl: './navigation-bar.html',
})
export class NavigationBar {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  isMenuOpen = false;

  readonly username = computed(() => this.auth.currentUser()?.username ?? '');
  readonly initial = computed(() => this.username().charAt(0).toUpperCase());
  readonly role = computed(() =>
    this.auth.currentUser()?.roles.includes('ADMIN') ? 'Admin' : 'Trader',
  );

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu(): void {
    this.isMenuOpen = false;
  }

  logout(): void {
    this.closeMenu();
    this.auth.logout().subscribe({
      complete: () => void this.router.navigate(['/login'], { replaceUrl: true }),
    });
  }
}
