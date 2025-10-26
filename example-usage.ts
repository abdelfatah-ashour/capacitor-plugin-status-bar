// Example usage of the new setStatusBarColor method

import { CAPStatusBar } from './src';

// Example: Set status bar to a red color
async function setRedStatusBar() {
  await CAPStatusBar.setStatusBarColor({
    color: '#FF0000' // Red color
  });
}

// Example: Set status bar to a blue color with alpha (semi-transparent)
async function setSemiTransparentBlueStatusBar() {
  await CAPStatusBar.setStatusBarColor({
    color: '#0066FF80' // Blue color with 50% opacity
  });
}

// Example: Set status bar to a light color (will automatically use dark content)
async function setLightStatusBar() {
  await CAPStatusBar.setStatusBarColor({
    color: '#F0F0F0' // Light gray - will show dark content
  });
}

// Example: Set status bar to a dark color (will automatically use light content)
async function setDarkStatusBar() {
  await CAPStatusBar.setStatusBarColor({
    color: '#333333' // Dark gray - will show light content
  });
}

// Comparison with existing methods:
async function compareWithExistingMethods() {
  // Old way: Setting style with color (affects both appearance and color)
  await CAPStatusBar.setStyle({
    style: 'CUSTOM' as any,
    color: '#FF0000'
  });

  // New way: Just change the color (automatically determines best appearance)
  await CAPStatusBar.setStatusBarColor({
    color: '#FF0000'
  });

  // You can still set background separately
  await CAPStatusBar.setBackground({
    color: '#FFFFFF'
  });
}