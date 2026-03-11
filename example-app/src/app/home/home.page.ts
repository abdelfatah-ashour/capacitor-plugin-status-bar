import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonItem, IonList, IonInput, IonToggle, IonLabel, IonSegment, IonSegmentButton, IonItemGroup, IonItemDivider } from '@ionic/angular/standalone';
import { FormsModule } from '@angular/forms';
import { StatusBar, Style, StatusBarColor, StatusBarAnimation} from "capacitor-plugin-status-bar";
import {SafeAreaInsets} from "capacitor-plugin-status-bar";
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
  customColor = signal<StatusBarColor>("#FF5733");
  overlaysWebView = signal(true);
  animated = signal(true);
  safeAreaInsets = signal<SafeAreaInsets>({ top: 0, bottom: 0, left: 0, right: 0 });
  private router = inject(Router);

  async applyStyle() {
    await StatusBar.setStyle({ style: this.style(), color: this.style() === Style.CUSTOM ? this.color() : undefined });
  }

  async show() {
    await StatusBar.show({ animated: this.animated() });
  }

  async hideInFade() {
    await StatusBar.hide({ animation: StatusBarAnimation.FADE });
  }

  async hideInSlide() {
    await StatusBar.hide({ animation: StatusBarAnimation.SLIDE });
  }

  async setOverlay() {
    await StatusBar.setOverlaysWebView({ value: this.overlaysWebView() });
  }

  async getSafeAreaInsets() {
    this.safeAreaInsets.set(await StatusBar.getSafeAreaInsets());
  }

  async applyCustomColor() {
    await StatusBar.setStyle({ style: Style.CUSTOM, color: this.customColor() });
  }

  async setQuickColor(color: StatusBarColor) {
    this.customColor.set(color);
    await this.applyCustomColor();
  }

  navigateToChat() {
    this.router.navigate(['/chat']);
  }
}
