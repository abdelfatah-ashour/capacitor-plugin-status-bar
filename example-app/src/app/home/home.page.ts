import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonItem, IonList, IonInput, IonToggle, IonLabel, IonSegment, IonSegmentButton, IonItemGroup, IonItemDivider } from '@ionic/angular/standalone';
import { FormsModule } from '@angular/forms';
import { CAPStatusBar, Style, StatusBarColor, StatusBarAnimation} from "cap-status-bar";
import {SafeAreaInsets} from "cap-status-bar";
import { JsonPipe } from '@angular/common';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonItem, IonList, IonInput, IonToggle, IonLabel, IonSegment, IonSegmentButton, IonItemGroup, IonItemDivider, FormsModule, JsonPipe],
})
export class HomePage {
  // Expose enum to template
  readonly Style = Style;

  style = signal<Style>(Style.LIGHT);
  color = signal<StatusBarColor>("#800080");
  statusBarColor = signal<StatusBarColor>("#FF5733");
  overlaysWebView = signal(true);
  animated = signal(true);
  safeAreaInsets = signal<SafeAreaInsets>({ top: 0, bottom: 0, left: 0, right: 0 });
  private router = inject(Router);

  async applyStyle() {
    await CAPStatusBar.setStyle({ style: this.style(), color: this.style() === Style.CUSTOM ? this.color() : undefined });
  }

  async show() {
    await CAPStatusBar.show({ animated: this.animated() });
  }

  async hideInFade() {
    await CAPStatusBar.hide({ animation: StatusBarAnimation.FADE });
  }

  async hideInSlide() {
    await CAPStatusBar.hide({ animation: StatusBarAnimation.SLIDE });
  }

  async setOverlay() {
    await CAPStatusBar.setOverlaysWebView({ value: this.overlaysWebView() });
  }

  async getSafeAreaInsets() {
    this.safeAreaInsets.set(await CAPStatusBar.getSafeAreaInsets());
  }

  async setStatusBarColor() {
    await CAPStatusBar.setStatusBarColor({ color: this.statusBarColor() });
  }

  async setQuickColor(color: StatusBarColor) {
    this.statusBarColor.set(color);
    await this.setStatusBarColor();
  }

  navigateToChat() {
    this.router.navigate(['/chat']);
  }
}
