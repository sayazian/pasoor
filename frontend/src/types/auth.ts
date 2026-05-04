export interface CurrentUser {
  id: string;
  name: string;
  email: string;
  avatarUrl: string | null;
  preferredTheme: 'CLASSIC_GREEN_FELT' | 'MODERN_LIGHT_TABLE' | 'PERSIAN_TILE' | 'DARK_CARD_ROOM';
}

