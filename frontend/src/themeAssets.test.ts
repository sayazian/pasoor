import { describe, expect, it } from 'vitest';
import { themeAssetUrls } from './themeAssets';

describe('theme assets', () => {
  it('keeps the card-back and page-border picture paths stable', () => {
    expect([...themeAssetUrls].sort()).toEqual([
      '/assets/themes/classic-card-back.svg',
      '/assets/themes/classic-page-border.svg',
      '/assets/themes/dark-card-back.svg',
      '/assets/themes/dark-page-border.svg',
      '/assets/themes/modern-card-back.svg',
      '/assets/themes/persian-card-back.svg',
      '/assets/themes/persian-page-border.svg'
    ]);
  });
});
