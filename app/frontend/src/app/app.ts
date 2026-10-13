import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavigationBar } from './shared/navigation-bar/navigation-bar';

@Component({
  imports: [NavigationBar, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {}
