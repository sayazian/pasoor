export interface CurrentUser {
  id: string;
  name: string;
  email: string;
  avatarUrl: string | null;
  preferredTheme: PreferredTheme;
}

export type PreferredTheme = 'CLASSIC_GREEN_FELT' | 'MODERN_LIGHT_TABLE' | 'PERSIAN_TILE' | 'DARK_CARD_ROOM';
