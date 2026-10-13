import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  imports: [RouterLink, RouterLinkActive, MatButtonModule],
  selector: 'app-navigation-bar',
  styleUrl: './navigation-bar.scss',
  templateUrl: './navigation-bar.html',
})
export class NavigationBar {
  isMenuOpen = false;

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu(): void {
    this.isMenuOpen = false;
  }
}
