import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonItem, IonList, IonInput, IonToggle, IonLabel, IonSegment, IonSegmentButton, IonItemGroup, IonItemDivider, IonFooter } from '@ionic/angular/standalone';
import { FormsModule } from '@angular/forms';
import { CapacitorStatusBar, Style, StatusBarColor } from "capacitor-plugin-status-bar";
import {SafeAreaInsets} from "capacitor-plugin-status-bar";
import { JsonPipe } from '@angular/common';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonItem, IonList, IonInput, IonToggle, IonLabel, IonSegment, IonSegmentButton, IonItemGroup, IonItemDivider, FormsModule, JsonPipe, IonFooter],
})
export class HomePage implements OnInit {
  private router = inject(Router);

  style = signal<Style>(Style.LIGHT);
  color = signal<StatusBarColor>("#800080");
  customColor = signal<StatusBarColor>("#FF5733");
  overlaysWebView = signal(true);
  animated = signal(true);
  safeAreaInsets = signal<SafeAreaInsets>({ top: 0, bottom: 0, left: 0, right: 0 });

  // Expose enum to template
  readonly Style = Style;

  ngOnInit(): void {
    CapacitorStatusBar.setOverlaysWebView({value : true});
    this.setInsets();
  }

  private async setInsets() {
    this.safeAreaInsets.set(await CapacitorStatusBar.getSafeAreaInsets());
    const insets = await CapacitorStatusBar.getSafeAreaInsets()
    console.log("🚀 ~ HomePage ~ setInsets ~ insets:", insets)
    document.documentElement.style.setProperty('--safe-area-inset-top', `${insets.top}px`);
    document.documentElement.style.setProperty('--safe-area-inset-bottom', `${insets.bottom}px`);
    document.documentElement.style.setProperty('--safe-area-inset-left', `${insets.left}px`);
    document.documentElement.style.setProperty('--safe-area-inset-right', `${insets.right}px`);
  }

  async applyStyle() {
    if(this.style() === Style.CUSTOM) {
      await CapacitorStatusBar.setStyle({ style: Style.CUSTOM, color: this.color() });
    } else {
      await CapacitorStatusBar.setStyle({ style: this.style() });
    }
  }

  async show() {
    await CapacitorStatusBar.show();
  }

  async hide() {
    await CapacitorStatusBar.hide();
  }

  async setOverlay() {
    await CapacitorStatusBar.setOverlaysWebView({ value: this.overlaysWebView() });
  }

  async showNavigationBar() {
    await CapacitorStatusBar.showNavigationBar();
  }

  async hideNavigationBar() {
    await CapacitorStatusBar.hideNavigationBar();
  }

  async getSafeAreaInsets() {
    this.safeAreaInsets.set(await CapacitorStatusBar.getSafeAreaInsets());
  }

  async applyCustomColor() {
    await CapacitorStatusBar.setStyle({ style: Style.CUSTOM, color: this.customColor() });
  }

  async setQuickColor(color: StatusBarColor) {
    this.customColor.set(color);
    await this.applyCustomColor();
  }

  navigateToChat() {
    this.router.navigate(['/chat']);
  }
}
