import { Component, OnInit } from '@angular/core';
import { Keyboard, KeyboardResize } from '@capacitor/keyboard';
import { IonApp, IonRouterOutlet } from '@ionic/angular/standalone';
import { CapacitorStatusBar } from 'capacitor-plugin-status-bar';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  imports: [IonApp, IonRouterOutlet],
})
export class AppComponent implements OnInit {

  ngOnInit(): void {
    Keyboard.setResizeMode({mode : KeyboardResize.Body});
    CapacitorStatusBar.setOverlaysWebView({value : true});
  }

}
